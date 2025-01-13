/*
 * Copyright 2017-2025 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.swedenconnect.eidas.connector.authn;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.shibboleth.shared.xml.SerializeSupport;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.eidas.connector.authn.sp.EidasAuthnRequest;
import se.swedenconnect.opensaml.common.utils.SerializableOpenSamlObject;
import se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants;
import se.swedenconnect.opensaml.saml2.response.ResponseProcessingResult;
import se.swedenconnect.spring.saml.idp.attributes.UserAttribute;
import se.swedenconnect.spring.saml.idp.attributes.eidas.EidasAttributeValue;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;

import java.io.ByteArrayOutputStream;
import java.io.Serial;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * An {@link Authentication} object representing a validated response from the foreign country.
 *
 * @author Martin Lindström
 */
@Slf4j
public class EidasAuthenticationToken extends AbstractAuthenticationToken {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The ID for the response. */
  @Getter
  private final String responseId;

  /** The InResponseTo attribute of the response. */
  @Getter
  private final String inResponseTo;

  /** The issue instant of the response. */
  @Getter
  private final Instant responseIssueInstant;

  /** The SAML assertion. */
  private final SerializableOpenSamlObject<Assertion> assertion;

  /** Attributes from the assertion. */
  private final List<UserAttribute> attributes;

  /** The corresponding request. */
  @Getter
  private final EidasAuthnRequest authnRequest;

  /** Was the signature consented by the user? */
  @Setter
  @Getter
  private boolean signatureConsented = false;

  /** The Swedish eID authentication context class reference. */
  @Getter
  @Setter
  private String swedishEidAuthnContextClassRef;

  /** For logging. */
  @Getter
  private final String logString;

  /** For caching the principal name. */
  private transient String principalCache;

  /**
   * Constructor.
   *
   * @param result result of response processing
   * @param authnRequest the eIDAS authentication request
   * @param inputToken the input token (used for logging)
   */
  public EidasAuthenticationToken(final ResponseProcessingResult result, final EidasAuthnRequest authnRequest,
      final Saml2UserAuthenticationInputToken inputToken) {
    super(Collections.emptyList());
    this.logString = Objects.requireNonNull(inputToken, "inputToken must not be null").getLogString();
    Objects.requireNonNull(result, "result must not be null");
    this.responseId = result.getResponseId();
    this.inResponseTo = result.getInResponseTo();
    this.responseIssueInstant = result.getIssueInstant();
    this.assertion = new SerializableOpenSamlObject<>(result.getAssertion());
    this.authnRequest = Objects.requireNonNull(authnRequest, "authnRequest must not be null");

    this.attributes = new ArrayList<>();
    result.getAttributes().forEach(a -> {
      try {
        this.attributes.add(new UserAttribute(a));
      }
      catch (final Exception e) {
        log.warn("Received {} attribute that could not be processed", a.getName(), e);
      }
    });

  }

  /**
   * Gets the SAML {@link Assertion}.
   *
   * @return an {@link Assertion}
   */
  public Assertion getAssertion() {
    return this.assertion.get();
  }

  /**
   * Gets the SAML {@link Assertion} in Base64 encoding.
   *
   * @return the Base64-encoded {@link Assertion}
   */
  @Nullable
  public String getAssertionBase64() {
    final Assertion assertion = this.getAssertion();
    if (assertion == null) {
      return null;
    }
    try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      SerializeSupport.writeNode(XMLObjectSupport.marshall(assertion), bos);
      return Base64.getEncoder().encodeToString(bos.toByteArray());
    }
    catch (final Exception e) {
      log.error("Failed to marshall OpenSAML object", e);
      return null;
    }
  }

  /**
   * Returns a read-only list of the attributes received.
   *
   * @return attribute list
   */
  public List<UserAttribute> getAttributes() {
    return Collections.unmodifiableList(this.attributes);
  }

  /**
   * Adds an attribute.
   *
   * @param attribute the attribute to add
   */
  public void addAttribute(final UserAttribute attribute) {
    if (attribute != null) {
      this.attributes.add(attribute);
    }
    // Clear cache
    this.principalCache = null;
  }

  /**
   * Gets the attribute matching the given attribute name.
   *
   * @param attributeName the attribute name
   * @return the {@link UserAttribute} or {@code null} if not available
   */
  public UserAttribute getAttribute(final String attributeName) {
    return this.attributes.stream()
        .filter(a -> Objects.equals(attributeName, a.getId()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Gets the authentication instant from the assertion.
   *
   * @return authentication instant
   */
  public Instant getAuthnInstant() {
    return Optional.ofNullable(this.assertion.get())
        .map(Assertion::getAuthnStatements)
        .filter(Predicate.not(List::isEmpty))
        .map(List::getFirst)
        .map(AuthnStatement::getAuthnInstant)
        .orElseGet(() -> this.assertion.get().getIssueInstant());
  }

  /**
   * Gets the authentication contect class reference URI from the assertion.
   *
   * @return the authn context class ref URI or {@code null} if it is not present in the assertion
   */
  public String getAuthnContextClassRef() {
    return Optional.ofNullable(this.assertion.get())
        .map(Assertion::getAuthnStatements)
        .filter(Predicate.not(List::isEmpty))
        .map(List::getFirst)
        .map(AuthnStatement::getAuthnContext)
        .map(AuthnContext::getAuthnContextClassRef)
        .map(AuthnContextClassRef::getURI)
        .orElse(null);
  }

  /** {@inheritDoc} */
  @Override
  public Object getPrincipal() {
    if (this.principalCache == null) {
      this.principalCache =
          Optional.ofNullable(this.getAttribute(AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME))
              .map(UserAttribute::getValues)
              .filter(Predicate.not(List::isEmpty))
              .map(List::getFirst)
              .map(EidasAttributeValue.class::cast)
              .map(EidasAttributeValue::getValueAsString)
              .orElse("unknown");
    }
    return this.principalCache;
  }

  /** {@inheritDoc} */
  @Override
  public Object getCredentials() {
    return this.getAssertion();
  }

}
