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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import se.swedenconnect.eidas.attributes.AttributeMappingService;
import se.swedenconnect.eidas.connector.authn.idm.IdmClient;
import se.swedenconnect.eidas.connector.authn.idm.IdmException;
import se.swedenconnect.eidas.connector.authn.idm.IdmRecord;
import se.swedenconnect.eidas.connector.authn.metadata.CountryMetadata;
import se.swedenconnect.eidas.connector.authn.metadata.EuMetadataProvider;
import se.swedenconnect.eidas.connector.authn.sp.AuthnContextClassRefMapper;
import se.swedenconnect.eidas.connector.authn.sp.EidasAuthnRequest;
import se.swedenconnect.eidas.connector.authn.sp.EidasAuthnRequestGenerator;
import se.swedenconnect.eidas.connector.authn.sp.EidasResponseValidationException;
import se.swedenconnect.eidas.connector.events.BeforeEidasAuthenticationEvent;
import se.swedenconnect.eidas.connector.events.ErrorEidasResponseEvent;
import se.swedenconnect.eidas.connector.events.IdentityMatchingErrorEvent;
import se.swedenconnect.eidas.connector.events.IdentityMatchingRecordEvent;
import se.swedenconnect.eidas.connector.events.ResponseProcessingErrorEvent;
import se.swedenconnect.eidas.connector.events.SuccessEidasResponseEvent;
import se.swedenconnect.eidas.connector.prid.generator.PridGeneratorException;
import se.swedenconnect.eidas.connector.prid.service.CountryPolicyNotFoundException;
import se.swedenconnect.eidas.connector.prid.service.PridResult;
import se.swedenconnect.eidas.connector.prid.service.PridService;
import se.swedenconnect.opensaml.common.validation.CoreValidatorParameters;
import se.swedenconnect.opensaml.saml2.request.RequestGenerationException;
import se.swedenconnect.opensaml.saml2.request.RequestHttpObject;
import se.swedenconnect.opensaml.saml2.response.ResponseProcessingException;
import se.swedenconnect.opensaml.saml2.response.ResponseProcessingInput;
import se.swedenconnect.opensaml.saml2.response.ResponseProcessingResult;
import se.swedenconnect.opensaml.saml2.response.ResponseProcessor;
import se.swedenconnect.opensaml.saml2.response.ResponseStatusErrorException;
import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;
import se.swedenconnect.spring.saml.idp.attributes.UserAttribute;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthentication;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserDetails;
import se.swedenconnect.spring.saml.idp.authentication.provider.external.AbstractUserRedirectAuthenticationProvider;
import se.swedenconnect.spring.saml.idp.authentication.provider.external.RedirectForAuthenticationToken;
import se.swedenconnect.spring.saml.idp.authentication.provider.external.ResumedAuthenticationToken;
import se.swedenconnect.spring.saml.idp.error.Saml2ErrorStatus;
import se.swedenconnect.spring.saml.idp.error.Saml2ErrorStatusException;
import se.swedenconnect.spring.saml.idp.error.UnrecoverableSaml2IdpError;
import se.swedenconnect.spring.saml.idp.error.UnrecoverableSaml2IdpException;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The {@link AuthenticationProvider} handling the authentication of the user against the foreign eIDAS countries.
 *
 * @author Martin Lindström
 */
@Slf4j
public class EidasAuthenticationProvider extends AbstractUserRedirectAuthenticationProvider {

  /** The authentication path, i.e., where the SAML engine should direct the user for authentication. */
  public static final String AUTHN_PATH = "/extauth";

  /** The path where we redirect the user to leave the control back to the SAML engine. */
  public static final String RESUME_PATH = "/resume";

  /** Special purpose AuthnContext Class Ref for eIDAS test. */
  public static final String EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF = "http://eidas.europa.eu/LoA/test";

  /** The session key where we store the eIDAS AuthnRequest. */
  public static final String EIDAS_AUTHNREQUEST_SESSION_KEY = EidasAuthnRequest.class.getName();

  /** The session key where we store the eIDAS authentication token. */
  public static final String EIDAS_AUTHNTOKEN_SESSION_KEY = EidasAuthenticationToken.class.getName();

  /** The event publisher. */
  private final ApplicationEventPublisher eventPublisher;

  /** SAML SP metadata. */
  private final EntityDescriptor spMetadata;

  /** For generating AuthnRequests. */
  private final EidasAuthnRequestGenerator authnRequestGenerator;

  /** The processor handling the SAML responses received from the foreign eIDAS proxy services. */
  private final ResponseProcessor responseProcessor;

  /** Service for mapping eIDAS attributes to the corresponding Swedish eID attributes. */
  private final AttributeMappingService attributeMappingService;

  /** The URL where we receive SAML responses. */
  private final String samlResponseUrl;

  /** The metadata provider. */
  private final EuMetadataProvider metadataProvider;

  /** The PRID service bean. */
  private final PridService pridService;

  /** The Identity Matching client. */
  private final IdmClient idmClient;

  /** Supported LoA URI:s. */
  private final List<String> supportedLoas;

  /** The entity categories. */
  private final List<String> entityCategories;

  /** Whitelisted SP:s that are allowed to send "ping" requests. */
  private final List<String> pingWhitelist;

  /**
   * Constructor.
   *
   * @param baseUrl the application base URL
   * @param eventPublisher the event publisher
   * @param spMetadata SAML SP metadata
   * @param authnRequestGenerator for generating authentication requests
   * @param responseProcessor the processor handling the SAML responses received from the foreign eIDAS proxy
   *     services
   * @param metadataProvider the EU metadata provider
   * @param attributeMappingService attribute service
   * @param pridService the PRID service bean
   * @param idmClient the Identity Matching client
   * @param supportedLoas supported LoA URI:s
   * @param entityCategories the entity categories
   * @param pingWhitelist the whitelisted SP:s that are allowed to send ping requests
   */
  public EidasAuthenticationProvider(final String baseUrl,
      final ApplicationEventPublisher eventPublisher,
      final EntityDescriptor spMetadata,
      final EidasAuthnRequestGenerator authnRequestGenerator,
      final ResponseProcessor responseProcessor,
      final EuMetadataProvider metadataProvider,
      final AttributeMappingService attributeMappingService,
      final PridService pridService,
      final IdmClient idmClient,
      final List<String> supportedLoas,
      final List<String> entityCategories,
      final List<String> pingWhitelist) {

    super(AUTHN_PATH, RESUME_PATH);

    this.ssoVoters().add(new EidasSsoVoter());

    this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    this.spMetadata = Objects.requireNonNull(spMetadata, "spMetadata must not be null");
    this.authnRequestGenerator =
        Objects.requireNonNull(authnRequestGenerator, "authnRequestGenerator must not be null");
    this.responseProcessor = Objects.requireNonNull(responseProcessor, "responseProcessor must not be null");
    this.samlResponseUrl = String.format("%s%s", Objects.requireNonNull(baseUrl, "baseUrl must not be null"),
        EidasAuthenticationController.ASSERTION_CONSUMER_PATH);
    this.metadataProvider = Objects.requireNonNull(metadataProvider, "metadataProvider must not be null");
    this.attributeMappingService =
        Objects.requireNonNull(attributeMappingService, "attributeMappingService must not be null");
    this.pridService = Objects.requireNonNull(pridService, "pridService must not be null");
    this.idmClient = Objects.requireNonNull(idmClient, "idmClient must not be null");
    this.supportedLoas = Collections.unmodifiableList(
        Objects.requireNonNull(supportedLoas, "supportedLoas must not be null"));
    this.entityCategories = Collections.unmodifiableList(
        Objects.requireNonNull(entityCategories, "entityCategories must not be null"));
    this.pingWhitelist = Optional.ofNullable(pingWhitelist).orElseGet(Collections::emptyList);

    // "http://eidas.europa.eu/LoA/test"

  }

  /**
   * The method will be called when we have received the SAML response from the foreign IdP.
   */
  @Override
  public Saml2UserAuthentication resumeAuthentication(final ResumedAuthenticationToken token)
      throws Saml2ErrorStatusException {

    final EidasAuthenticationToken eidasToken = (EidasAuthenticationToken) token.getAuthnToken();

    // Build the user details for the authentication ...
    //
    final Saml2UserDetails userDetails = new Saml2UserDetails(
        eidasToken.getAttributes(),
        AttributeConstants.ATTRIBUTE_NAME_PRID,
        eidasToken.getSwedishEidAuthnContextClassRef(),
        eidasToken.getAuthnInstant(),
        token.getServletRequest().getRemoteAddr());
    userDetails.setSignMessageDisplayed(eidasToken.isSignatureConsented());

    final Saml2UserAuthentication userAuth = new Saml2UserAuthentication(userDetails);
    userAuth.setReuseAuthentication(!eidasToken.isSignatureConsented());

    return userAuth;
  }

  /**
   * Processes a SAML response received from the foreign IdP.
   *
   * @param httpRequest the HTTP servlet request
   * @param samlResponse the SAML response
   * @param relayState the RelayState variable
   * @return an {@link EidasAuthenticationToken}
   * @throws Saml2ErrorStatusException for errors
   */
  public EidasAuthenticationToken processSamlResponse(final HttpServletRequest httpRequest,
      final String samlResponse, final String relayState) throws Saml2ErrorStatusException {

    try {
      // First get hold of the corresponding authentication request ...
      //
      final EidasAuthnRequest eidasAuthnRequest = this.getEidasAuthnRequest(httpRequest);

      // Process the SAML response ...
      //
      final ValidationContext validationContext =
          new ValidationContext(Map.of(CoreValidatorParameters.SP_METADATA, this.spMetadata));
      final ResponseProcessingResult result =
          this.responseProcessor.processSamlResponse(samlResponse, relayState,
              this.buildResponseProcessingInput(httpRequest, eidasAuthnRequest), validationContext);

      log.debug("Successfully processed SAML response");

      final Saml2UserAuthenticationInputToken inputToken = this.getSamlInputToken(httpRequest);
      final EidasAuthenticationToken eidasToken = new EidasAuthenticationToken(result, eidasAuthnRequest, inputToken);

      // Signal event ...
      //
      this.eventPublisher.publishEvent(new SuccessEidasResponseEvent(inputToken,
          result.getResponse(), result.getAssertion()));

      // Assert that the authentication context URI is valid and map it to a Swedish URI ...
      //
      try {
        final String eidasAuthnContextUri = eidasToken.getAuthnContextClassRef();
        AuthnContextClassRefMapper.assertReturnedAuthnContextUri(eidasAuthnContextUri,
            eidasToken.getAuthnRequest().getAuthnRequest().getRequestedAuthnContext());

        eidasToken.setSwedishEidAuthnContextClassRef(AuthnContextClassRefMapper.calculateReturnAuthnContextUri(
            eidasAuthnContextUri, inputToken.getAuthnRequirements().getAuthnContextRequirements()));
      }
      catch (final Saml2ErrorStatusException e) {
        this.eventPublisher.publishEvent(new ResponseProcessingErrorEvent(inputToken, e.getMessage()));
        throw e;
      }

      // Map eIDAS attributes to Swedish eID attributes ...
      //
      this.attributeMappingService.toSwedishUserAttributes(eidasToken.getAttributes())
          .forEach(eidasToken::addAttribute);

      // Add country attribute ...
      //
      eidasToken.addAttribute(new UserAttribute(
          AttributeConstants.ATTRIBUTE_NAME_C,
          AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_C,
          eidasToken.getAuthnRequest().getCountry()));

      // Add the ID of the assertion as a transactionIdentifier attribute ...
      //
      eidasToken.addAttribute(new UserAttribute(
          AttributeConstants.ATTRIBUTE_NAME_TRANSACTION_IDENTIFIER,
          AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_TRANSACTION_IDENTIFIER,
          eidasToken.getAssertion().getID()));

      // Invoke the PRID service to resolve the eIDAS person identifier to a Swedish
      // PRID attribute ...
      //
      try {
        final PridResult pridResult = this.pridService.generatePrid(
            (String) eidasToken.getPrincipal(), eidasToken.getAuthnRequest().getCountry());

        eidasToken.addAttribute(new UserAttribute(
            AttributeConstants.ATTRIBUTE_NAME_PRID,
            AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_PRID,
            pridResult.prid()));

        eidasToken.addAttribute(new UserAttribute(
            AttributeConstants.ATTRIBUTE_NAME_PRID_PERSISTENCE,
            AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_PRID_PERSISTENCE,
            pridResult.pridPersistence()));
      }
      catch (final IllegalArgumentException | PridGeneratorException | CountryPolicyNotFoundException e) {
        log.error("Failed to generate PRID attribute from ID '{}' ({}) - {}",
            eidasToken.getPrincipal(), eidasToken.getAuthnRequest().getCountry(),
            e.getMessage(), e);

        this.eventPublisher.publishEvent(new ResponseProcessingErrorEvent(inputToken, e.getMessage()));

        throw new Saml2ErrorStatusException(StatusCode.RESPONDER, StatusCode.AUTHN_FAILED,
            null, "Failed to create PRID attribute", e.getMessage(), e);
      }

      return eidasToken;
    }
    catch (final ResponseStatusErrorException e) {
      log.info("Received non successful status: {}", e.getMessage());

      // Signal event ...
      //
      this.eventPublisher.publishEvent(
          new ErrorEidasResponseEvent(this.getSamlInputToken(httpRequest), e.getResponse()));

      // Let's update the status message - It may be in any language, so we add
      // a text telling that the failure was received from the foreign service.
      //
      final Status status = e.getStatus();
      throw new Saml2ErrorStatusException(
          Optional.ofNullable(status.getStatusCode()).map(StatusCode::getValue).orElse(StatusCode.RESPONDER),
          Optional.ofNullable(status.getStatusCode()).map(StatusCode::getStatusCode).map(StatusCode::getValue)
              .orElse(StatusCode.AUTHN_FAILED),
          null,
          Optional.ofNullable(status.getStatusMessage())
              .map(StatusMessage::getValue)
              .map("Failure received from foreign service: %s"::formatted)
              .orElse("Failure received from foreign service"),
          e);
    }
    catch (final ResponseProcessingException e) {
      log.info("Error while processing eIDAS response - {}", e.getMessage(), e);

      this.eventPublisher.publishEvent(new ResponseProcessingErrorEvent(
          this.getSamlInputToken(httpRequest), e.getMessage(), e.getResponse()));

      if (e instanceof final EidasResponseValidationException eidasError) {
        throw eidasError.getError();
      }
      else {
        final String msg = "Validation failure of response from foreign service: %s".formatted(e.getMessage());
        throw new Saml2ErrorStatusException(StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, null, msg, e);
      }
    }
  }

  /**
   * Builds a {@link ResponseProcessingInput} for use when processing a SAML response.
   *
   * @param httpServletRequest the HTTP servlet request
   * @param eidasAuthnRequest the request
   * @return a {@link ResponseProcessingInput}
   */
  private ResponseProcessingInput buildResponseProcessingInput(
      final HttpServletRequest httpServletRequest, final EidasAuthnRequest eidasAuthnRequest) {

    return new ResponseProcessingInput() {

      @Override
      public AuthnRequest getAuthnRequest(final String id) {
        return Optional.ofNullable(eidasAuthnRequest)
            .map(EidasAuthnRequest::getAuthnRequest)
            .filter(a -> Objects.equals(id, a.getID()))
            .orElse(null);
      }

      @Override
      public String getRequestRelayState(final String id) {
        return Optional.ofNullable(eidasAuthnRequest)
            .map(EidasAuthnRequest::getRelayState)
            .orElse(null);
      }

      @Override
      public String getReceiveURL() {
        return samlResponseUrl;
      }

      @Override
      public Instant getReceiveInstant() {
        return Instant.now();
      }

      @Override
      public String getClientIpAddress() {
        return httpServletRequest.getRemoteAddr();
      }

      @Override
      public X509Certificate getClientCertificate() {
        return null;
      }

    };

  }

  /**
   * Asserts that any identity attributes given as "principal selections" in the request is valid given authenticated
   * attributes.
   *
   * @param inputToken the SAML input token
   * @param token the eIDAS authentication token
   * @throws Saml2ErrorStatusException for assertion errors
   */
  public void assertPrincipalSelection(
      final Saml2UserAuthenticationInputToken inputToken, final EidasAuthenticationToken token)
      throws Saml2ErrorStatusException {

    // Only assert for signing user ...
    //
    if (!inputToken.getAuthnRequestToken().isSignatureServicePeer()) {
      return;
    }

    try {
      final Collection<UserAttribute> attributes = token.getAttributes();

      final Collection<UserAttribute> principalSelection =
          inputToken.getAuthnRequirements().getPrincipalSelectionAttributes();
      if (principalSelection.isEmpty()) {
        return;
      }
      // We assert the typical identity attributes.
      //
      for (final UserAttribute ps : principalSelection) {

        final String psValue = Optional.of(ps.getValues())
            .filter(Predicate.not(List::isEmpty))
            .map(v -> v.get(0))
            .map(String.class::cast)
            .orElse(null);
        if (psValue == null) {
          continue;
        }

        if (AttributeConstants.ATTRIBUTE_NAME_PERSONAL_IDENTITY_NUMBER.equals(ps.getId())
            || AttributeConstants.ATTRIBUTE_NAME_MAPPED_PERSONAL_IDENTITY_NUMBER.equals(ps.getId())) {

          checkPrincipalSelectionValue(ps.getId(), psValue,
              AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_PERSONAL_IDENTITY_NUMBER,
              attributes.stream()
                  .filter(a -> AttributeConstants.ATTRIBUTE_NAME_MAPPED_PERSONAL_IDENTITY_NUMBER.equals(a.getId()))
                  .map(UserAttribute::getStringValues)
                  .findFirst()
                  .orElse(null));
        }
        else if (AttributeConstants.ATTRIBUTE_NAME_PRID.equals(ps.getId())) {
          checkPrincipalSelectionValue(ps.getId(), psValue,
              AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_PRID,
              attributes.stream()
                  .filter(a -> AttributeConstants.ATTRIBUTE_NAME_PRID.equals(a.getId()))
                  .map(UserAttribute::getStringValues)
                  .findFirst()
                  .orElse(null));
        }
        else if (AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER.equals(ps.getId())) {
          checkPrincipalSelectionValue(ps.getId(), psValue,
              AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_EIDAS_PERSON_IDENTIFIER,
              attributes.stream()
                  .filter(a -> AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER.equals(a.getId()))
                  .map(UserAttribute::getStringValues)
                  .findFirst()
                  .orElse(null));
        }
      }
    }
    catch (final Saml2ErrorStatusException e) {
      this.eventPublisher.publishEvent(new ResponseProcessingErrorEvent(inputToken, e.getMessage()));
      throw e;
    }
  }

  /**
   * Tells whether the Identity Matching feature is active.
   *
   * @return {@code true} if IdM is active and {@code false} otherwise
   */
  public boolean isIdmActive() {
    return this.idmClient.isActive();
  }

  /**
   * Checks if the user identified by the authentication token has an IdM record.
   *
   * @param token the authentication token
   * @param inputToken the authentication input token
   * @return {@code true} if the user has a record and {@code false} otherwise
   */
  public boolean hasIdmRecord(final EidasAuthenticationToken token,
      final Saml2UserAuthenticationInputToken inputToken) {
    try {
      return this.idmClient.hasRecord(token);
    }
    catch (final IdmException e) {
      this.eventPublisher.publishEvent(new IdentityMatchingErrorEvent(inputToken, token, e));
      return false;
    }
  }

  /**
   * Gets the Identity Matching record for the given user and updates the supplied {@link EidasAuthenticationToken} with
   * the attributes found in the IdM record.
   *
   * @param token the token to update
   * @param inputToken the SAML input token
   */
  public void obtainIdmRecord(final EidasAuthenticationToken token,
      final Saml2UserAuthenticationInputToken inputToken) {
    try {
      final IdmRecord idmRecord = this.idmClient.getRecord(token);

      log.info("Received IdM record for user '{}': swedish-id:'{}', binding:{} [{}]",
          token.getPrincipal(), idmRecord.getSwedishIdentity(), idmRecord.getBinding(), token.getLogString());

      token.addAttribute(new UserAttribute(
          AttributeConstants.ATTRIBUTE_NAME_MAPPED_PERSONAL_IDENTITY_NUMBER,
          AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_MAPPED_PERSONAL_IDENTITY_NUMBER,
          idmRecord.getSwedishIdentity()));
      token.addAttribute(new UserAttribute(
          AttributeConstants.ATTRIBUTE_NAME_PERSONAL_IDENTITY_NUMBER_BINDING,
          AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_PERSONAL_IDENTITY_NUMBER_BINDING,
          idmRecord.getBinding()));

      this.eventPublisher.publishEvent(new IdentityMatchingRecordEvent(inputToken, token, idmRecord));
    }
    catch (final IdmException e) {
      log.error("Failed to obtain IdM record: {}", e.getMessage(), e);
      this.eventPublisher.publishEvent(new IdentityMatchingErrorEvent(inputToken, token, e));
    }
  }

  /**
   * Checks principal selection values against authenticated values.
   *
   * @param psName the principal selection attribute name
   * @param psValue the value from the request (principal selection)
   * @param friendlyName the friendly name (for logging)
   * @param values the authenticated values
   * @throws Saml2ErrorStatusException for mismatches
   */
  private static void checkPrincipalSelectionValue(
      final String psName, final String psValue, final String friendlyName, final List<String> values)
      throws Saml2ErrorStatusException {

    if (values == null || values.isEmpty()) {
      return;
    }
    if (!values.contains(psValue)) {
      final String msg = "PrincipalSelection for attribute '%s' specified value '%s', but authenticated value was %s"
          .formatted(psName, psValue, values);
      throw new Saml2ErrorStatusException(StatusCode.RESPONDER, StatusCode.UNKNOWN_PRINCIPAL, null,
          "%s attribute from authentication does not match %s attribute from request"
              .formatted(friendlyName, friendlyName),
          msg);
    }
  }

  /**
   * Generates an authentication request for the given country.
   *
   * @param httpServletRequest the HTTP servlet request
   * @param country the recipient country
   * @param token the input token
   * @return an {@code AuthnRequest}
   * @throws Saml2ErrorStatusException for errors generating the request
   */
  public RequestHttpObject<AuthnRequest> generateAuthnRequest(final HttpServletRequest httpServletRequest,
      final String country, final Saml2UserAuthenticationInputToken token) throws Saml2ErrorStatusException {

    final CountryMetadata countryMetadata = this.metadataProvider.getCountry(country);
    if (countryMetadata == null) {
      final String msg = "No services available for selected country";
      log.error("No metadata found for country '{}'", country);
      throw new Saml2ErrorStatusException(StatusCode.RESPONDER, StatusCode.NO_AVAILABLE_IDP, null, msg, msg);
    }

    try {
      final String relayState = Optional.ofNullable(token.getAuthnRequestToken().getRelayState())
          .filter(StringUtils::hasText)
          .orElseGet(() -> UUID.randomUUID().toString());

      final RequestHttpObject<AuthnRequest> authnRequest =
          this.authnRequestGenerator.generateAuthnRequest(countryMetadata, token, relayState);

      // Add event ...
      //
      this.eventPublisher.publishEvent(new BeforeEidasAuthenticationEvent(token, countryMetadata.getCountryCode(),
          authnRequest.getRequest(), relayState, authnRequest.getMethod()));

      // Save request in session ...
      //
      httpServletRequest.getSession().setAttribute(EIDAS_AUTHNREQUEST_SESSION_KEY,
          new EidasAuthnRequest(authnRequest.getRequest(), relayState,
              token.getAuthnRequestToken().getAuthnRequest().getID(), countryMetadata.getCountryCode()));

      return authnRequest;
    }
    catch (final RequestGenerationException e) {
      // TODO: logging
      // TODO: change
      throw new Saml2ErrorStatusException(Saml2ErrorStatus.AUTHN_FAILED, "Failed to generate AuthnRequest", e);
    }
  }

  /**
   * Supports {@link EidasAuthenticationToken}.
   */
  @Override
  public boolean supportsUserAuthenticationToken(final Authentication authentication) {
    return authentication instanceof EidasAuthenticationToken;
  }

  /** {@inheritDoc} */
  @Override
  public String getName() {
    return "eidas-authn";
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getSupportedAuthnContextUris() {
    return this.supportedLoas;
  }

  /**
   * Special handling since we also may support the special URI {@code http://eidas.europa.eu/LoA/test}.
   */
  @Override
  protected List<String> filterRequestedAuthnContextUris(final Saml2UserAuthenticationInputToken token) {
    final List<String> filtered = super.filterRequestedAuthnContextUris(token);
    if (this.pingWhitelist.isEmpty() || !this.pingWhitelist.contains(token.getAuthnRequestToken().getEntityId())) {
      return filtered;
    }
    if (token.getAuthnRequirements().getAuthnContextRequirements().contains(EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF)) {
      return List.of(EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF);
    }
    else {
      return filtered;
    }
  }

  /**
   * Tells wheter this is a eIDAS ping request.
   *
   * @param token the input token
   * @return {@code true} if it is a ping request and {@code false} otherwise
   */
  public boolean isPingRequest(final Saml2UserAuthenticationInputToken token) {
    return token.getAuthnRequirements().getAuthnContextRequirements().contains(EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF);
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getEntityCategories() {
    return this.entityCategories;
  }

  /**
   * Saves the {@link EidasAuthenticationToken} in session storage.
   *
   * @param httpRequest the HTTP servlet request
   * @param token the token to save
   */
  public void saveEidasAuthenticationToken(final HttpServletRequest httpRequest, final EidasAuthenticationToken token) {
    httpRequest.getSession().setAttribute(EIDAS_AUTHNTOKEN_SESSION_KEY, token);
  }

  public void removeEidasAuthenticationToken(final HttpServletRequest httpRequest) {
    final HttpSession session = httpRequest.getSession();
    session.removeAttribute(EIDAS_AUTHNTOKEN_SESSION_KEY);
  }

  /**
   * Gets the {@link EidasAuthenticationToken} from session storage.
   *
   * @param httpRequest the HTTP servlet request
   * @return an {@link EidasAuthenticationToken}
   * @throws UnrecoverableSaml2IdpException for session errors
   */
  public EidasAuthenticationToken getEidasAuthenticationToken(final HttpServletRequest httpRequest)
      throws UnrecoverableSaml2IdpException {

    final HttpSession session = httpRequest.getSession();
    final EidasAuthenticationToken token =
        (EidasAuthenticationToken) session.getAttribute(EIDAS_AUTHNTOKEN_SESSION_KEY);
    if (token == null) {
      final String msg = "Session error - no authentication request available";
      throw new UnrecoverableSaml2IdpException(UnrecoverableSaml2IdpError.INVALID_SESSION, msg,
          Optional.ofNullable(this.getTokenRepository().getExternalAuthenticationToken(httpRequest))
              .map(RedirectForAuthenticationToken::getAuthnInputToken)
              .orElse(null));
    }
    return token;
  }

  /**
   * Gets the authentication request that we saved in the session before we sent the authentication request.
   * <p>
   * Once read from the session the object will be removed from session storage.
   * </p>
   *
   * @param httpRequest the HTTP servlet request
   * @return an {@link EidasAuthnRequest}
   * @throws UnrecoverableSaml2IdpException for session errors
   */
  private EidasAuthnRequest getEidasAuthnRequest(final HttpServletRequest httpRequest)
      throws UnrecoverableSaml2IdpException {

    final HttpSession session = httpRequest.getSession();
    final EidasAuthnRequest authnRequest = (EidasAuthnRequest) session.getAttribute(EIDAS_AUTHNREQUEST_SESSION_KEY);
    if (authnRequest == null) {
      final String msg = "Received SAML response, but no authentication requests exists in session";
      throw new UnrecoverableSaml2IdpException(UnrecoverableSaml2IdpError.INVALID_SESSION, msg,
          Optional.ofNullable(this.getTokenRepository().getExternalAuthenticationToken(httpRequest))
              .map(RedirectForAuthenticationToken::getAuthnInputToken)
              .orElse(null));
    }

    // Remove the authn request from the session ...
    session.removeAttribute(EIDAS_AUTHNREQUEST_SESSION_KEY);

    return authnRequest;
  }

  /**
   * Gets the {@link Saml2UserAuthenticationInputToken} from the session.
   *
   * @param httpRequest the HTTP servlet request
   * @return the {@link Saml2UserAuthenticationInputToken}
   */
  private Saml2UserAuthenticationInputToken getSamlInputToken(final HttpServletRequest httpRequest) {
    return Optional.ofNullable(this.getTokenRepository().getExternalAuthenticationToken(httpRequest))
        .map(RedirectForAuthenticationToken::getAuthnInputToken)
        .orElseThrow(() -> new UnrecoverableSaml2IdpException(UnrecoverableSaml2IdpError.INVALID_SESSION,
            "No input token available", null));
  }

}
