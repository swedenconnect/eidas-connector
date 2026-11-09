/*
 * Copyright 2017-2026 Sweden Connect
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
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AuthenticatingAuthority;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Issuer;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.eidas.connector.attributes.EidasAttributeConverter;
import se.swedenconnect.eidas.connector.authn.sp.EidasAuthnRequest;
import se.swedenconnect.opensaml.common.utils.SerializableOpenSamlObject;
import se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants;
import se.swedenconnect.opensaml.saml2.response.ResponseProcessingResult;
import se.swedenconnect.spring.authnserver.attributes.GenericAttribute;
import se.swedenconnect.spring.authnserver.authentication.provider.UserAuthenticationInputToken;

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
 * <p>
 * The token holds the eIDAS assertion, and the generic attributes that are released to the Swedish Service Provider.
 * </p>
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

  /** The generic attributes that are released to the Swedish Service Provider. */
  private final List<GenericAttribute<?>> attributes = new ArrayList<>();

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

  /** The eIDAS person identifier of the user. */
  private final String principal;

  /**
   * Constructor.
   *
   * @param result result of response processing
   * @param authnRequest the eIDAS authentication request
   * @param inputToken the input token (used for logging)
   */
  public EidasAuthenticationToken(final ResponseProcessingResult result, final EidasAuthnRequest authnRequest,
      final UserAuthenticationInputToken inputToken) {
    super(Collections.emptyList());
    this.logString = Objects.requireNonNull(inputToken, "inputToken must not be null").getLogString();
    Objects.requireNonNull(result, "result must not be null");
    this.responseId = result.getResponseId();
    this.inResponseTo = result.getInResponseTo();
    this.responseIssueInstant = result.getIssueInstant();
    this.assertion = new SerializableOpenSamlObject<>(result.getAssertion());
    this.authnRequest = Objects.requireNonNull(authnRequest, "authnRequest must not be null");
    this.principal = this.getEidasAttributes().stream()
        .filter(a -> AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME.equals(a.getName()))
        .map(EidasAttributeConverter::getValue)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse("unknown");
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
   * Gets the eIDAS attributes of the assertion.
   *
   * @return a list of eIDAS attributes
   */
  public List<Attribute> getEidasAttributes() {
    return Optional.ofNullable(this.assertion.get())
        .map(Assertion::getAttributeStatements)
        .map(statements -> statements.stream()
            .map(AttributeStatement::getAttributes)
            .flatMap(List::stream)
            .toList())
        .orElse(Collections.emptyList());
  }

  /**
   * Returns a read-only list of the generic attributes that are released to the Swedish Service Provider.
   *
   * @return attribute list
   */
  public List<GenericAttribute<?>> getAttributes() {
    return Collections.unmodifiableList(this.attributes);
  }

  /**
   * Adds a generic attribute that is released to the Swedish Service Provider. An attribute with the same identifier
   * is replaced.
   *
   * @param attribute the attribute to add
   */
  public void addAttribute(final GenericAttribute<?> attribute) {
    if (attribute != null) {
      this.attributes.removeIf(a -> a.getIdentifier().equals(attribute.getIdentifier()));
      this.attributes.add(attribute);
    }
  }

  /**
   * Gets the generic attribute with the given identifier.
   *
   * @param identifier the attribute identifier
   * @return the {@link GenericAttribute} or {@code null} if not available
   */
  public GenericAttribute<?> getAttribute(final String identifier) {
    return this.attributes.stream()
        .filter(a -> Objects.equals(identifier, a.getIdentifier()))
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

  /**
   * Returns a list of "authenticating authorities". This list will always start with the URI for the country, followed
   * by the IdP entityID.
   *
   * @return a list of authenticating authorities
   */
  public List<String> getAuthenticatingAuthorities() {

    final List<String> authorities = new ArrayList<>();

    // Always add the country URI first ...
    authorities.add(EidasCountryHandler.COUNTRY_URI_PREFIX + this.authnRequest.getCountry().toLowerCase());

    // Next, add the IdP from the foreign country ...
    Optional.ofNullable(this.assertion.get())
        .map(Assertion::getIssuer)
        .map(Issuer::getValue)
        .ifPresent(authorities::add);

    // And, if the assertion contained any Authenticating Authority elements, add those as well.
    //
    final List<AuthenticatingAuthority> a = Optional.ofNullable(this.assertion.get())
        .map(Assertion::getAuthnStatements)
        .filter(Predicate.not(List::isEmpty))
        .map(List::getFirst)
        .map(AuthnStatement::getAuthnContext)
        .map(AuthnContext::getAuthenticatingAuthorities)
        .orElse(Collections.emptyList());

    a.stream().map(AuthenticatingAuthority::getURI).forEach(authorities::add);

    return authorities;
  }

  /**
   * Gets the eIDAS person identifier of the user.
   */
  @Override
  public Object getPrincipal() {
    return this.principal;
  }

  /** {@inheritDoc} */
  @Override
  public Object getCredentials() {
    return this.getAssertion();
  }

}
