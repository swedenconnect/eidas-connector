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

import net.shibboleth.shared.xml.SerializeSupport;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AuthenticatingAuthority;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Issuer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class EidasAuthenticationToken extends AbstractAuthenticationToken {

  /** Logger instance. */
  private static final Logger log = LoggerFactory.getLogger(EidasAuthenticationToken.class);

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The ID for the response. */
  private final @NonNull String responseId;

  /** The InResponseTo attribute of the response. */
  private final @NonNull String inResponseTo;

  /** The issue instant of the response. */
  private final @NonNull Instant responseIssueInstant;

  /** The SAML assertion. */
  private final @NonNull SerializableOpenSamlObject<Assertion> assertion;

  /** The generic attributes that are released to the Swedish Service Provider. */
  private final @NonNull List<GenericAttribute<?>> attributes = new ArrayList<>();

  /** The corresponding request. */
  private final @NonNull EidasAuthnRequest authnRequest;

  /** Was the signature consented by the user? */
  private boolean signatureConsented = false;

  /** The Swedish eID authentication context class reference. */
  private @Nullable String swedishEidAuthnContextClassRef;

  /** For logging. */
  private final @NonNull String logString;

  /** The eIDAS person identifier of the user. */
  private final @NonNull String principal;

  /**
   * Constructor.
   *
   * @param result result of response processing
   * @param authnRequest the eIDAS authentication request
   * @param inputToken the input token (used for logging)
   */
  public EidasAuthenticationToken(final @NonNull ResponseProcessingResult result,
      final @NonNull EidasAuthnRequest authnRequest, final @NonNull UserAuthenticationInputToken inputToken) {
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
   * Gets the ID for the response.
   *
   * @return the response ID
   */
  public @NonNull String getResponseId() {
    return this.responseId;
  }

  /**
   * Gets the InResponseTo attribute of the response.
   *
   * @return the InResponseTo attribute
   */
  public @NonNull String getInResponseTo() {
    return this.inResponseTo;
  }

  /**
   * Gets the issue instant of the response.
   *
   * @return the response issue instant
   */
  public @NonNull Instant getResponseIssueInstant() {
    return this.responseIssueInstant;
  }

  /**
   * Gets the corresponding request.
   *
   * @return the eIDAS authentication request
   */
  public @NonNull EidasAuthnRequest getAuthnRequest() {
    return this.authnRequest;
  }

  /**
   * Tells whether the signature was consented by the user.
   *
   * @return {@code true} if the signature was consented, and {@code false} otherwise
   */
  public boolean isSignatureConsented() {
    return this.signatureConsented;
  }

  /**
   * Assigns whether the signature was consented by the user.
   *
   * @param signatureConsented whether the signature was consented
   */
  public void setSignatureConsented(final boolean signatureConsented) {
    this.signatureConsented = signatureConsented;
  }

  /**
   * Gets the Swedish eID authentication context class reference.
   *
   * @return the Swedish eID authentication context class reference
   */
  public @Nullable String getSwedishEidAuthnContextClassRef() {
    return this.swedishEidAuthnContextClassRef;
  }

  /**
   * Assigns the Swedish eID authentication context class reference.
   *
   * @param swedishEidAuthnContextClassRef the Swedish eID authentication context class reference
   */
  public void setSwedishEidAuthnContextClassRef(final @Nullable String swedishEidAuthnContextClassRef) {
    this.swedishEidAuthnContextClassRef = swedishEidAuthnContextClassRef;
  }

  /**
   * Gets the string used for logging.
   *
   * @return the log string
   */
  public @NonNull String getLogString() {
    return this.logString;
  }

  /**
   * Gets the SAML {@link Assertion}.
   *
   * @return an {@link Assertion}
   */
  public @NonNull Assertion getAssertion() {
    return this.assertion.get();
  }

  /**
   * Gets the SAML {@link Assertion} in Base64 encoding.
   *
   * @return the Base64-encoded {@link Assertion}
   */
  public @Nullable String getAssertionBase64() {
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
  public @NonNull List<Attribute> getEidasAttributes() {
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
  public @NonNull List<GenericAttribute<?>> getAttributes() {
    return Collections.unmodifiableList(this.attributes);
  }

  /**
   * Adds a generic attribute that is released to the Swedish Service Provider. An attribute with the same identifier
   * is replaced.
   *
   * @param attribute the attribute to add
   */
  public void addAttribute(final @Nullable GenericAttribute<?> attribute) {
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
  public @Nullable GenericAttribute<?> getAttribute(final @NonNull String identifier) {
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
  public @NonNull Instant getAuthnInstant() {
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
  public @Nullable String getAuthnContextClassRef() {
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
  public @NonNull List<String> getAuthenticatingAuthorities() {

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
  public @NonNull Object getPrincipal() {
    return this.principal;
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull Object getCredentials() {
    return this.getAssertion();
  }

}
