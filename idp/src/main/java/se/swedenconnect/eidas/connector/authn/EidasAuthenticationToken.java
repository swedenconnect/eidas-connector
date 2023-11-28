/*
 * Copyright 2023 Sweden Connect
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.eidas.connector.authn.sp.EidasAuthnRequest;
import se.swedenconnect.opensaml.common.utils.SerializableOpenSamlObject;
import se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants;
import se.swedenconnect.opensaml.saml2.response.ResponseProcessingResult;
import se.swedenconnect.spring.saml.idp.attributes.UserAttribute;
import se.swedenconnect.spring.saml.idp.attributes.eidas.EidasAttributeValue;

/**
 * An {@link Authentication} object representing a validated response from the foreign country.
 *
 * @author Martin Lindström
 */
@Slf4j
public class EidasAuthenticationToken extends AbstractAuthenticationToken {

  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The ID for the response. */
  private final String responseId;

  /** The InResponseTo attribute of the response. */
  private final String inResponseTo;

  /** The issue instant of the response. */
  private final Instant responseIssueInstant;

  /** The SAML assertion. */
  private final SerializableOpenSamlObject<Assertion> assertion;

  /** Attributes from the assertion. */
  private final List<UserAttribute> attributes;

  /** The corresponding request. */
  private final EidasAuthnRequest authnRequest;

  /**
   * Constructor.
   *
   * @param result result of response processing
   */
  public EidasAuthenticationToken(final ResponseProcessingResult result, final EidasAuthnRequest authnRequest) {
    super(Collections.emptyList());
    Objects.requireNonNull(result, "result must not be null");
    this.responseId = result.getResponseId();
    this.inResponseTo = result.getInResponseTo();
    this.responseIssueInstant = result.getIssueInstant();
    this.assertion = new SerializableOpenSamlObject<>(result.getAssertion());
    this.authnRequest = Objects.requireNonNull(authnRequest, "authnRequest must not be null");

    this.attributes = new ArrayList<>();
    result.getAttributes().stream().forEach(a -> {
      try {
        this.attributes.add(new UserAttribute(a));
      }
      catch (final Exception e) {
        log.warn("Received {} attribute that could not be processed", a.getName(), e);
      }
    });

  }

  /**
   * Gets the ID for the response.
   *
   * @return the ID for the response
   */
  public String getResponseId() {
    return this.responseId;
  }

  /**
   * Gets the InResponseTo attribute of the response.
   *
   * @return the InResponseTo attribute
   */
  public String getInResponseTo() {
    return this.inResponseTo;
  }

  /**
   * Gets the issue instant of the response message.
   *
   * @return the issue instant
   */
  public Instant getResponseIssueInstant() {
    return this.responseIssueInstant;
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
        .map(as -> as.get(0))
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
        .map(as -> as.get(0))
        .map(AuthnStatement::getAuthnContext)
        .map(AuthnContext::getAuthnContextClassRef)
        .map(AuthnContextClassRef::getURI)
        .orElse(null);
  }

  /**
   * Gets the authentication request object that corresponds to this token.
   *
   * @return an {@link EidasAuthnRequest}
   */
  public EidasAuthnRequest getAuthnRequest() {
    return this.authnRequest;
  }

  /** {@inheritDoc} */
  @Override
  public Object getPrincipal() {
    return Optional.ofNullable(this.getAttribute(AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME))
        .map(UserAttribute::getValues)
        .filter(Predicate.not(List::isEmpty))
        .map(v -> v.get(0))
        .map(EidasAttributeValue.class::cast)
        .map(EidasAttributeValue::getValueAsString)
        .orElseGet(() -> "unknown");
  }

  /** {@inheritDoc} */
  @Override
  public Object getCredentials() {
    return this.getAssertion();
  }

}
