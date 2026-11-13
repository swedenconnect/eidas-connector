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
package se.swedenconnect.eidas.simulator.authn;

import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.config.impl.DefaultSecurityConfigurationBootstrap;
import org.opensaml.xmlsec.crypto.XMLSigningUtil;
import org.opensaml.xmlsec.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xmlsec.keyinfo.KeyInfoCriterion;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedenconnect.eidas.simulator.country.SimulatedCountry;
import se.swedenconnect.eidas.simulator.metadata.ConnectorMetadataResolver;
import se.swedenconnect.opensaml.eidas.ext.NodeCountry;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttributes;
import se.swedenconnect.opensaml.eidas.ext.SPType;
import se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Processes an authentication request from the connector the way a production eIDAS node does.
 * <p>
 * Until the connector is identified, the request is trusted and its assertion consumer service established, a
 * failed check results in an {@link UnrecoverableRequestException}, and an error page is shown. After that, a failed
 * check results in an {@link ErrorResponseException}, and an error response is sent to the connector.
 * </p>
 *
 * @author Martin Lindström
 */
public class AuthnRequestProcessor {

  /** Logger. */
  private static final Logger log = LoggerFactory.getLogger(AuthnRequestProcessor.class);

  /** The entity category that tells that a country requires the RequesterID of non-public relying parties. */
  public static final @NonNull String REQUESTER_ID_ENTITY_CATEGORY =
      "http://eidas.europa.eu/entity-attributes/termsofaccess/requesterid";

  /** The order of the notified eIDAS levels of assurance. */
  private static final Map<String, Integer> NOTIFIED_LOA_ORDER = Map.of(
      "http://eidas.europa.eu/LoA/low", 1,
      "http://eidas.europa.eu/LoA/substantial", 2,
      "http://eidas.europa.eu/LoA/high", 3);

  /** The attributes that must be requested in a request for a natural person. */
  private static final List<String> MANDATORY_NATURAL_PERSON_ATTRIBUTES = List.of(
      AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME);

  /** The NameID formats in the order they are chosen when the request does not give one. */
  private static final List<String> NAME_ID_PREFERENCE =
      List.of(NameIDType.PERSISTENT, NameIDType.TRANSIENT, NameIDType.UNSPECIFIED);

  /** The connector metadata. */
  private final @NonNull ConnectorMetadataResolver connectorMetadataResolver;

  /** The maximum age of a request. */
  private final @NonNull Duration maxAge;

  /** The allowed clock skew. */
  private final @NonNull Duration clockSkew;

  /** Whether an unsupported NameID format in the request is answered with an error response. */
  private boolean strictNameIdPolicy;

  /** For resolving the credentials of the connector metadata. */
  private final KeyInfoCredentialResolver keyInfoCredentialResolver =
      DefaultSecurityConfigurationBootstrap.buildBasicInlineKeyInfoCredentialResolver();

  /** The clock. */
  private @NonNull Clock clock = Clock.systemUTC();

  /**
   * Constructor.
   *
   * @param connectorMetadataResolver the connector metadata
   * @param maxAge the maximum age of a request
   * @param clockSkew the allowed clock skew
   */
  public AuthnRequestProcessor(final @NonNull ConnectorMetadataResolver connectorMetadataResolver,
      final @NonNull Duration maxAge, final @NonNull Duration clockSkew) {
    this.connectorMetadataResolver =
        Objects.requireNonNull(connectorMetadataResolver, "connectorMetadataResolver must not be null");
    this.maxAge = Objects.requireNonNull(maxAge, "maxAge must not be null");
    this.clockSkew = Objects.requireNonNull(clockSkew, "clockSkew must not be null");
  }

  /**
   * Processes an authentication request received by a simulated country.
   *
   * @param country the simulated country
   * @param decoded the decoded request
   * @return the processed request
   * @throws UnrecoverableRequestException if the request cannot be answered with a response
   * @throws ErrorResponseException if the request is answered with an error response
   */
  public @NonNull ProcessedRequest process(final @NonNull SimulatedCountry country,
      final AuthnRequestDecoder.@NonNull DecodedRequest decoded)
      throws UnrecoverableRequestException, ErrorResponseException {

    final AuthnRequest request = decoded.authnRequest();

    // Checks before the connector and its assertion consumer service are known.
    //
    if (!country.isBindingSupported(decoded.binding())) {
      throw new UnrecoverableRequestException("%s does not support the binding %s"
          .formatted(country.getName(), decoded.binding()));
    }
    final String issuer = Optional.ofNullable(request.getIssuer()).map(i -> i.getValue()).orElse(null);
    if (issuer == null || issuer.isBlank()) {
      throw new UnrecoverableRequestException("The AuthnRequest has no Issuer");
    }
    final EntityDescriptor metadata = this.connectorMetadataResolver.getMetadata(issuer);
    if (metadata == null) {
      throw new UnrecoverableRequestException(
          "The issuer '%s' is not the connector, or the connector metadata is not available".formatted(issuer));
    }
    final SPSSODescriptor sp = metadata.getSPSSODescriptor(SAMLConstants.SAML20P_NS);
    if (sp == null) {
      throw new UnrecoverableRequestException("The metadata of '%s' has no SPSSODescriptor".formatted(issuer));
    }
    this.validateSignature(decoded, sp, issuer);

    if (request.getID() == null || request.getID().isBlank()) {
      throw new UnrecoverableRequestException("The AuthnRequest has no ID");
    }
    final String acsUrl = getAssertionConsumerServiceUrl(request, sp);
    final ResponseTarget target = new ResponseTarget(metadata, acsUrl, request.getID(), decoded.relayState());

    // Checks that are answered with an error response.
    //
    if (request.getVersion() == null || !SAMLVersion.VERSION_20.equals(request.getVersion())) {
      throw error(target, StatusCode.VERSION_MISMATCH, null, "Unsupported SAML version");
    }
    if (!country.getSsoUrl().equals(request.getDestination())) {
      throw error(target, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED,
          "Invalid Destination '%s'".formatted(request.getDestination()));
    }
    this.checkIssueInstant(request, target);

    if (Boolean.TRUE.equals(request.isPassive())) {
      throw error(target, StatusCode.RESPONDER, StatusCode.NO_PASSIVE, "IsPassive is not supported");
    }
    if (!Boolean.TRUE.equals(request.isForceAuthn())) {
      throw error(target, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, "ForceAuthn must be true");
    }

    final String spType = getSpType(request, metadata, target);

    final List<String> requesterIds = request.getScoping() != null
        ? request.getScoping().getRequesterIDs().stream().map(r -> r.getURI()).filter(Objects::nonNull).toList()
        : List.of();
    if ("private".equals(spType) && country.getSettings().getEntityCategories().contains(REQUESTER_ID_ENTITY_CATEGORY)
        && requesterIds.isEmpty()) {
      throw error(target, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED,
          "RequesterID is required for private sector relying parties");
    }

    final List<ProcessedRequest.RequestedAttribute> requestedAttributes =
        getRequestedAttributes(request, country, target);
    final String nameIdFormat = getNameIdFormat(request, country, target, this.strictNameIdPolicy);
    final List<String> allowedLoas = getAllowedLoas(request, country, target);

    final ProcessedRequest processed = new ProcessedRequest(UUID.randomUUID().toString(), country.getCode(), target,
        spType, requesterIds.isEmpty() ? null : requesterIds.getFirst(), request.getProviderName(),
        requestedAttributes, nameIdFormat, allowedLoas, getNodeCountry(sp));

    log.info("{}: Accepted AuthnRequest '{}' from '{}' [loa: {}, name-id: {}]", country.getCode(), request.getID(),
        issuer, allowedLoas, nameIdFormat);
    return processed;
  }

  /**
   * Validates the signature of the request against the signing keys of the connector metadata.
   *
   * @param decoded the decoded request
   * @param sp the SP role of the connector metadata
   * @param issuer the issuer, for logging
   * @throws UnrecoverableRequestException if the request is not signed or the signature is not valid
   */
  private void validateSignature(final AuthnRequestDecoder.@NonNull DecodedRequest decoded,
      final @NonNull SPSSODescriptor sp, final @NonNull String issuer) throws UnrecoverableRequestException {

    final List<Credential> credentials = this.getSigningCredentials(sp);
    if (credentials.isEmpty()) {
      throw new UnrecoverableRequestException("The metadata of '%s' has no signing key".formatted(issuer));
    }

    if (SAMLConstants.SAML2_REDIRECT_BINDING_URI.equals(decoded.binding())) {
      if (decoded.signedContent() == null || decoded.signature() == null || decoded.signatureAlgorithm() == null) {
        throw new UnrecoverableRequestException("The AuthnRequest is not signed");
      }
      for (final Credential credential : credentials) {
        try {
          if (XMLSigningUtil.verifyWithURI(credential, decoded.signatureAlgorithm(), decoded.signature(),
              decoded.signedContent())) {
            return;
          }
        }
        catch (final SecurityException e) {
          log.debug("Signature validation failed - {}", e.getMessage());
        }
      }
      throw new UnrecoverableRequestException("The signature of the AuthnRequest is not valid");
    }

    final AuthnRequest request = decoded.authnRequest();
    if (request.getSignature() == null) {
      throw new UnrecoverableRequestException("The AuthnRequest is not signed");
    }
    try {
      new SAMLSignatureProfileValidator().validate(request.getSignature());
    }
    catch (final SignatureException e) {
      throw new UnrecoverableRequestException("The signature of the AuthnRequest is not valid - " + e.getMessage(),
          e);
    }
    for (final Credential credential : credentials) {
      try {
        SignatureValidator.validate(request.getSignature(), credential);
        return;
      }
      catch (final SignatureException e) {
        log.debug("Signature validation failed - {}", e.getMessage());
      }
    }
    throw new UnrecoverableRequestException("The signature of the AuthnRequest is not valid");
  }

  /**
   * Gets the signing credentials of the connector metadata.
   *
   * @param sp the SP role
   * @return the credentials
   */
  private @NonNull List<Credential> getSigningCredentials(final @NonNull SPSSODescriptor sp) {
    final List<Credential> credentials = new ArrayList<>();
    for (final KeyDescriptor kd : sp.getKeyDescriptors()) {
      if (kd.getKeyInfo() == null || UsageType.ENCRYPTION == kd.getUse()) {
        continue;
      }
      try {
        this.keyInfoCredentialResolver.resolve(new CriteriaSet(new KeyInfoCriterion(kd.getKeyInfo())))
            .forEach(credentials::add);
      }
      catch (final ResolverException e) {
        log.debug("Failed to resolve credential from KeyInfo - {}", e.getMessage());
      }
    }
    return credentials;
  }

  /**
   * Gets the URL of the assertion consumer service that the response is sent to. Only the HTTP-POST binding is used
   * for responses.
   *
   * @param request the request
   * @param sp the SP role of the connector metadata
   * @return the URL
   * @throws UnrecoverableRequestException if no assertion consumer service can be established
   */
  private static @NonNull String getAssertionConsumerServiceUrl(final @NonNull AuthnRequest request,
      final @NonNull SPSSODescriptor sp) throws UnrecoverableRequestException {

    final List<AssertionConsumerService> postServices = sp.getAssertionConsumerServices().stream()
        .filter(a -> SAMLConstants.SAML2_POST_BINDING_URI.equals(a.getBinding()))
        .toList();

    if (request.getAssertionConsumerServiceURL() != null) {
      return postServices.stream()
          .map(AssertionConsumerService::getLocation)
          .filter(l -> request.getAssertionConsumerServiceURL().equals(l))
          .findFirst()
          .orElseThrow(() -> new UnrecoverableRequestException(
              "The AssertionConsumerServiceURL '%s' is not in the connector metadata"
                  .formatted(request.getAssertionConsumerServiceURL())));
    }
    if (request.getAssertionConsumerServiceIndex() != null) {
      return postServices.stream()
          .filter(a -> request.getAssertionConsumerServiceIndex().equals(a.getIndex()))
          .map(AssertionConsumerService::getLocation)
          .findFirst()
          .orElseThrow(() -> new UnrecoverableRequestException(
              "No assertion consumer service with index %d (HTTP-POST) in the connector metadata"
                  .formatted(request.getAssertionConsumerServiceIndex())));
    }
    return postServices.stream()
        .filter(a -> Boolean.TRUE.equals(a.isDefault()))
        .findFirst()
        .or(() -> postServices.stream().findFirst())
        .map(AssertionConsumerService::getLocation)
        .orElseThrow(() -> new UnrecoverableRequestException(
            "The connector metadata has no assertion consumer service for HTTP-POST"));
  }

  /**
   * Checks the age of the request.
   *
   * @param request the request
   * @param target the response target
   * @throws ErrorResponseException if the request is too old or issued in the future
   */
  private void checkIssueInstant(final @NonNull AuthnRequest request, final @NonNull ResponseTarget target)
      throws ErrorResponseException {
    final Instant issueInstant = request.getIssueInstant();
    if (issueInstant == null) {
      throw error(target, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, "The AuthnRequest has no IssueInstant");
    }
    final Instant now = this.clock.instant();
    if (issueInstant.isAfter(now.plus(this.clockSkew))) {
      throw error(target, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, "The AuthnRequest is issued in the future");
    }
    if (issueInstant.isBefore(now.minus(this.maxAge).minus(this.clockSkew))) {
      throw error(target, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, "The AuthnRequest is too old");
    }
  }

  /**
   * Gets the SP type, which is given either in the request or in the connector metadata, but not in both.
   *
   * @param request the request
   * @param metadata the connector metadata
   * @param target the response target
   * @return the SP type, {@code public} or {@code private}
   * @throws ErrorResponseException if the SP type is missing, or given both in the request and the metadata
   */
  private static @NonNull String getSpType(final @NonNull AuthnRequest request,
      final @NonNull EntityDescriptor metadata, final @NonNull ResponseTarget target) throws ErrorResponseException {

    final SPType requestSpType = findExtension(request.getExtensions(), SPType.class);
    SPType metadataSpType = findExtension(metadata.getExtensions(), SPType.class);
    if (metadataSpType == null) {
      final SPSSODescriptor sp = metadata.getSPSSODescriptor(SAMLConstants.SAML20P_NS);
      metadataSpType = sp != null ? findExtension(sp.getExtensions(), SPType.class) : null;
    }
    if (requestSpType != null && metadataSpType != null) {
      throw error(target, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED,
          "SPType is given in the connector metadata and must not be included in the AuthnRequest");
    }
    final SPType spType = requestSpType != null ? requestSpType : metadataSpType;
    if (spType == null || spType.getType() == null) {
      throw error(target, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, "No SPType in request or metadata");
    }
    return spType.getType().getValue();
  }

  /**
   * Gets the requested attributes.
   *
   * @param request the request
   * @param country the simulated country
   * @param target the response target
   * @return the requested attributes
   * @throws ErrorResponseException if the attributes cannot be served
   */
  private static @NonNull List<ProcessedRequest.RequestedAttribute> getRequestedAttributes(
      final @NonNull AuthnRequest request, final @NonNull SimulatedCountry country,
      final @NonNull ResponseTarget target) throws ErrorResponseException {

    final RequestedAttributes requested = findExtension(request.getExtensions(), RequestedAttributes.class);
    if (requested == null || requested.getRequestedAttributes().isEmpty()) {
      throw error(target, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, "No attributes requested");
    }
    final List<ProcessedRequest.RequestedAttribute> attributes = requested.getRequestedAttributes().stream()
        .filter(a -> a.getName() != null)
        .map(a -> new ProcessedRequest.RequestedAttribute(a.getName(), Boolean.TRUE.equals(a.isRequired())))
        .toList();

    final boolean naturalPerson = attributes.stream()
        .anyMatch(a -> a.name().startsWith(AttributeConstants.NATURAL_PERSON_PREFIX));
    final boolean legalPerson = attributes.stream()
        .anyMatch(a -> a.name().startsWith(AttributeConstants.LEGAL_PERSON_PREFIX));
    if (naturalPerson && legalPerson) {
      throw error(target, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED,
          "A request must not include both natural and legal person attributes");
    }

    for (final ProcessedRequest.RequestedAttribute attribute : attributes) {
      if (!attribute.required()) {
        // Requested attributes that the country does not support are ignored.
        continue;
      }
      if (!country.isAttributeSupported(attribute.name())) {
        throw error(target, StatusCode.RESPONDER, StatusCode.REQUEST_DENIED,
            "Required attribute %s is not supported".formatted(attribute.name()));
      }
      if (!attribute.name().startsWith(AttributeConstants.NATURAL_PERSON_PREFIX)
          || attribute.name().startsWith(AttributeConstants.REPRESENTATIVE_NATURAL_PERSON_PREFIX)) {
        throw error(target, StatusCode.RESPONDER, StatusCode.REQUEST_UNSUPPORTED,
            "Required attribute %s cannot be delivered, only natural persons are simulated"
                .formatted(attribute.name()));
      }
    }
    if (naturalPerson) {
      for (final String mandatory : MANDATORY_NATURAL_PERSON_ATTRIBUTES) {
        if (attributes.stream().noneMatch(a -> a.name().equals(mandatory))) {
          throw error(target, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED,
              "Mandatory attribute %s is not requested".formatted(mandatory));
        }
      }
    }
    return attributes;
  }

  /**
   * Gets the NameID format of the response. A requested format that the country does not support is answered with
   * an error response if the NameID policy is checked strictly, and otherwise replaced with a supported format.
   *
   * @param request the request
   * @param country the simulated country
   * @param target the response target
   * @param strictNameIdPolicy whether the NameID policy is checked strictly
   * @return the NameID format
   * @throws ErrorResponseException if the requested format is not supported and the check is strict
   */
  private static @NonNull String getNameIdFormat(final @NonNull AuthnRequest request,
      final @NonNull SimulatedCountry country, final @NonNull ResponseTarget target, final boolean strictNameIdPolicy)
      throws ErrorResponseException {

    final List<String> supported = country.getSettings().getNameIdFormats();
    final String requested = request.getNameIDPolicy() != null ? request.getNameIDPolicy().getFormat() : null;
    if (requested != null) {
      if (supported.contains(requested)) {
        return requested;
      }
      if (strictNameIdPolicy) {
        throw error(target, StatusCode.REQUESTER, StatusCode.INVALID_NAMEID_POLICY,
            "NameID format %s is not supported".formatted(requested));
      }
      log.debug("{}: NameID format {} is not supported, using a supported format", country.getCode(), requested);
    }
    return NAME_ID_PREFERENCE.stream()
        .filter(supported::contains)
        .findFirst()
        .orElse(NameIDType.UNSPECIFIED);
  }

  /**
   * Gets the levels of assurance that the response may have: the ones the country delivers that the request
   * allows, from the lowest to the highest.
   *
   * @param request the request
   * @param country the simulated country
   * @param target the response target
   * @return the levels of assurance
   * @throws ErrorResponseException if no level of assurance can be delivered
   */
  private static @NonNull List<String> getAllowedLoas(final @NonNull AuthnRequest request,
      final @NonNull SimulatedCountry country, final @NonNull ResponseTarget target) throws ErrorResponseException {

    final RequestedAuthnContext rac = request.getRequestedAuthnContext();
    if (rac == null || rac.getAuthnContextClassRefs().isEmpty()) {
      throw error(target, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, "No RequestedAuthnContext in request");
    }
    final List<String> requested = rac.getAuthnContextClassRefs().stream()
        .map(AuthnContextClassRef::getURI)
        .filter(Objects::nonNull)
        .toList();
    final List<String> supported = country.getSettings().getAssuranceCertifications();

    // The SAML default for Comparison is "exact".
    final AuthnContextComparisonTypeEnumeration comparison = Optional.ofNullable(rac.getComparison())
        .orElse(AuthnContextComparisonTypeEnumeration.EXACT);

    final List<String> allowed;
    if (comparison == AuthnContextComparisonTypeEnumeration.MINIMUM) {
      if (requested.size() != 1 || !NOTIFIED_LOA_ORDER.containsKey(requested.getFirst())) {
        throw error(target, StatusCode.REQUESTER, StatusCode.REQUEST_UNSUPPORTED,
            "Comparison minimum requires exactly one notified eIDAS level of assurance");
      }
      final int minimum = NOTIFIED_LOA_ORDER.get(requested.getFirst());
      allowed = supported.stream()
          .filter(l -> NOTIFIED_LOA_ORDER.getOrDefault(l, 0) >= minimum)
          .distinct()
          .toList();
    }
    else if (comparison == AuthnContextComparisonTypeEnumeration.EXACT) {
      allowed = supported.stream().filter(requested::contains).distinct().toList();
    }
    else {
      throw error(target, StatusCode.REQUESTER, StatusCode.REQUEST_UNSUPPORTED,
          "Comparison %s is not supported".formatted(comparison));
    }
    if (allowed.isEmpty()) {
      throw error(target, StatusCode.RESPONDER, StatusCode.NO_AUTHN_CONTEXT,
          "None of the requested levels of assurance %s can be delivered".formatted(requested));
    }
    return allowed.stream()
        .sorted(Comparator.comparingInt((String l) -> NOTIFIED_LOA_ORDER.getOrDefault(l, 0)))
        .toList();
  }

  /**
   * Gets the country code of the connector from its metadata. If not present, {@code SE} is assumed.
   *
   * @param sp the SP role of the connector metadata
   * @return the country code
   */
  private static @NonNull String getNodeCountry(final @NonNull SPSSODescriptor sp) {
    final NodeCountry nodeCountry = findExtension(sp.getExtensions(), NodeCountry.class);
    return nodeCountry != null && nodeCountry.getNodeCountry() != null ? nodeCountry.getNodeCountry() : "SE";
  }

  /**
   * Finds an extension element.
   *
   * @param extensions the extensions, may be {@code null}
   * @param type the type of the extension
   * @param <T> the type
   * @return the extension, or {@code null}
   */
  private static <T extends XMLObject> @Nullable T findExtension(
      final org.opensaml.saml.common.@Nullable SAMLObject extensions, final @NonNull Class<T> type) {
    if (extensions == null) {
      return null;
    }
    final List<XMLObject> children = extensions instanceof final Extensions e
        ? e.getUnknownXMLObjects()
        : extensions instanceof final org.opensaml.saml.saml2.core.Extensions e2
            ? e2.getUnknownXMLObjects()
            : List.of();
    return children.stream().filter(type::isInstance).map(type::cast).findFirst().orElse(null);
  }

  /**
   * Creates an error response exception.
   *
   * @param target the response target
   * @param statusCode the top-level status code
   * @param subStatusCode the second-level status code, or {@code null}
   * @param message the status message
   * @return the exception
   */
  private static @NonNull ErrorResponseException error(final @NonNull ResponseTarget target,
      final @NonNull String statusCode, final @Nullable String subStatusCode, final @NonNull String message) {
    log.info("Answering AuthnRequest '{}' with an error response - {}", target.inResponseTo(), message);
    return new ErrorResponseException(new EidasStatus(statusCode, subStatusCode, message), target);
  }

  /**
   * Assigns whether an unsupported NameID format in the request is answered with an error response
   * ({@code InvalidNameIDPolicy}). If {@code false}, which is the default, a format that the country supports is
   * used instead.
   *
   * @param strictNameIdPolicy whether the NameID policy is checked strictly
   */
  public void setStrictNameIdPolicy(final boolean strictNameIdPolicy) {
    this.strictNameIdPolicy = strictNameIdPolicy;
  }

  /**
   * Assigns the clock. For testing.
   *
   * @param clock the clock
   */
  void setClock(final @NonNull Clock clock) {
    this.clock = clock;
  }

}
