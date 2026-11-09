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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.util.StringUtils;
import se.swedenconnect.eidas.connector.attributes.EidasAttributeConverter;
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
import se.swedenconnect.eidas.connector.idp.SignMessageProcessor;
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
import se.swedenconnect.spring.authnserver.attributes.AttributeIdentifiers;
import se.swedenconnect.spring.authnserver.attributes.GenericAttribute;
import se.swedenconnect.spring.authnserver.attributes.GenericRequestedAttribute;
import se.swedenconnect.spring.authnserver.attributes.SwedishIdentityNumbers;
import se.swedenconnect.spring.authnserver.authentication.AuthenticatedUser;
import se.swedenconnect.spring.authnserver.authentication.UserAuthentication;
import se.swedenconnect.spring.authnserver.authentication.provider.UserAuthenticationInputToken;
import se.swedenconnect.spring.authnserver.authentication.provider.redirect.AbstractUserRedirectAuthenticationProvider;
import se.swedenconnect.spring.authnserver.authentication.provider.redirect.RedirectForAuthenticationToken;
import se.swedenconnect.spring.authnserver.authentication.provider.redirect.ResumedAuthenticationToken;
import se.swedenconnect.spring.authnserver.error.AuthenticationErrorException;
import se.swedenconnect.spring.authnserver.error.CommonUnrecoverableError;
import se.swedenconnect.spring.authnserver.error.UnrecoverableErrorException;
import se.swedenconnect.spring.authnserver.message.GenericSignMessage;
import se.swedenconnect.spring.authnserver.saml.error.SamlErrorStatusException;
import se.swedenconnect.spring.authnserver.sso.RequestedAttributesSsoVoter;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The {@link AuthenticationProvider} handling the authentication of the user against the foreign eIDAS countries.
 * <p>
 * The user is sent to the pages of the connector, see {@link EidasAuthenticationController}, where the country is
 * selected and the authentication at the foreign country is made.
 * </p>
 *
 * @author Martin Lindström
 */
public class EidasAuthenticationProvider extends AbstractUserRedirectAuthenticationProvider {

  /** Logger instance. */
  private static final Logger log = LoggerFactory.getLogger(EidasAuthenticationProvider.class);

  /** The authentication path, i.e., where the SAML engine should direct the user for authentication. */
  public static final String AUTHN_PATH = "/extauth";

  /** The path where we redirect the user to leave the control back to the SAML engine. */
  public static final String RESUME_PATH = "/resume";

  /** Special purpose AuthnContext Class Ref for eIDAS test. */
  public static final String EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF = "http://eidas.europa.eu/LoA/test";

  /** The session key where we store the eIDAS AuthnRequest. */
  public static final String EIDAS_AUTHNREQUEST_SESSION_KEY = EidasAuthnRequest.class.getName();

  /** The session key prefix for where we store the eIDAS authentication token. */
  public static final String EIDAS_AUTHNTOKEN_SESSION_KEY = EidasAuthenticationToken.class.getName();

  /** The event publisher. */
  private final ApplicationEventPublisher eventPublisher;

  /** SAML SP metadata. */
  private final EntityDescriptor spMetadata;

  /** For generating AuthnRequests. */
  private final EidasAuthnRequestGenerator authnRequestGenerator;

  /** The processor handling the SAML responses received from the foreign eIDAS proxy services. */
  private final ResponseProcessor responseProcessor;

  /** For converting eIDAS attributes into the attributes released to the Swedish SP. */
  private final EidasAttributeConverter attributeConverter;

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
   * @param attributeConverter for converting eIDAS attributes into the attributes released to the Swedish SP
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
      final EidasAttributeConverter attributeConverter,
      final PridService pridService,
      final IdmClient idmClient,
      final List<String> supportedLoas,
      final List<String> entityCategories,
      final List<String> pingWhitelist) {

    super(AUTHN_PATH, RESUME_PATH);

    // The connector lets a previous authentication be reused also when another set of attributes is requested,
    // and never for a signature service ...
    //
    this.getSsoVoters().removeIf(RequestedAttributesSsoVoter.class::isInstance);
    this.getSsoVoters().add(new SignServiceSsoVoter());
    this.getSsoVoters().add(new EidasSsoVoter());

    this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    this.spMetadata = Objects.requireNonNull(spMetadata, "spMetadata must not be null");
    this.authnRequestGenerator =
        Objects.requireNonNull(authnRequestGenerator, "authnRequestGenerator must not be null");
    this.responseProcessor = Objects.requireNonNull(responseProcessor, "responseProcessor must not be null");
    this.samlResponseUrl = String.format("%s%s", Objects.requireNonNull(baseUrl, "baseUrl must not be null"),
        EidasAuthenticationController.ASSERTION_CONSUMER_PATH);
    this.metadataProvider = Objects.requireNonNull(metadataProvider, "metadataProvider must not be null");
    this.attributeConverter = Objects.requireNonNull(attributeConverter, "attributeConverter must not be null");
    this.pridService = Objects.requireNonNull(pridService, "pridService must not be null");
    this.idmClient = Objects.requireNonNull(idmClient, "idmClient must not be null");
    this.supportedLoas = Collections.unmodifiableList(
        Objects.requireNonNull(supportedLoas, "supportedLoas must not be null"));
    this.entityCategories = Collections.unmodifiableList(
        Objects.requireNonNull(entityCategories, "entityCategories must not be null"));
    this.pingWhitelist = Optional.ofNullable(pingWhitelist).orElseGet(Collections::emptyList);
  }

  /**
   * Refuses a sign message that the connector cannot display before the user is sent to the pages of the connector.
   */
  @Override
  protected Authentication authenticate(final UserAuthenticationInputToken token,
      final List<String> authnContextUris) throws AuthenticationErrorException {
    SignMessageProcessor.assertSupported(token.getAuthnRequirements().getSignMessage());
    return super.authenticate(token, authnContextUris);
  }

  /**
   * The method will be called when the user comes back to the resume path after the authentication at the foreign
   * country.
   */
  @Override
  protected UserAuthentication createUserAuthentication(final ResumedAuthenticationToken token)
      throws AuthenticationErrorException {

    if (token.getAuthnToken() instanceof final EidasAuthenticationErrorToken errorToken) {
      // An authentication that ends in an error must not let the next request through ...
      //
      clearSessionAuthentication(token.getServletRequest());
      throw errorToken.getError();
    }
    final EidasAuthenticationToken eidasToken = (EidasAuthenticationToken) Objects.requireNonNull(
        token.getAuthnToken(), "No authentication token available");

    // Build the authenticated user ...
    //
    final List<GenericAttribute<?>> attributes = new ArrayList<>(eidasToken.getAttributes());
    attributes.add(new GenericAttribute<>(AttributeIdentifiers.AUTHENTICATION_PROVIDER,
        eidasToken.getAuthenticatingAuthorities().stream().map(Serializable.class::cast).toList()));

    final AuthenticatedUser user = new AuthenticatedUser(
        attributes,
        AttributeIdentifiers.PRID,
        eidasToken.getSwedishEidAuthnContextClassRef(),
        eidasToken.getAuthnInstant(),
        Optional.ofNullable(token.getServletRequest()).map(HttpServletRequest::getRemoteAddr).orElse("unknown"));

    final GenericSignMessage signMessage = token.getAuthnInputToken().getAuthnRequirements().getSignMessage();
    if (eidasToken.isSignatureConsented() && signMessage != null) {
      user.setSignMessageDisplayed(true, signMessage.getMessages().getFirst().language());
    }

    final UserAuthentication userAuth = new UserAuthentication(user);
    userAuth.setReuseForSso(!eidasToken.isSignatureConsented());

    return userAuth;
  }

  /**
   * Processes a SAML response received from the foreign IdP.
   *
   * @param httpRequest the HTTP servlet request
   * @param inputToken the input token for the authentication
   * @param samlResponse the SAML response
   * @param relayState the RelayState variable
   * @return an {@link EidasAuthenticationToken}
   * @throws SamlErrorStatusException for errors that are reported to the Swedish SP
   * @throws UnrecoverableErrorException if there is no matching authentication request in the session
   */
  public EidasAuthenticationToken processSamlResponse(final HttpServletRequest httpRequest,
      final UserAuthenticationInputToken inputToken, final String samlResponse, final String relayState)
      throws SamlErrorStatusException, UnrecoverableErrorException {

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

      final EidasAuthenticationToken eidasToken = new EidasAuthenticationToken(result, eidasAuthnRequest, inputToken);

      // Signal event ...
      //
      this.eventPublisher.publishEvent(new SuccessEidasResponseEvent(inputToken,
          result.getResponse(), result.getAssertion(), eidasAuthnRequest.getCountry()));

      // Assert that the authentication context URI is valid and map it to a Swedish URI ...
      //
      try {
        final String eidasAuthnContextUri = eidasToken.getAuthnContextClassRef();
        AuthnContextClassRefMapper.assertReturnedAuthnContextUri(eidasAuthnContextUri,
            eidasToken.getAuthnRequest().getAuthnRequest().getRequestedAuthnContext());

        eidasToken.setSwedishEidAuthnContextClassRef(AuthnContextClassRefMapper.calculateReturnAuthnContextUri(
            eidasAuthnContextUri, inputToken.getAuthnRequirements().getAuthnContextRequirements()));
      }
      catch (final SamlErrorStatusException e) {
        this.eventPublisher.publishEvent(new ResponseProcessingErrorEvent(inputToken, e.getMessage()));
        throw e;
      }

      // Map eIDAS attributes to the attributes released to the Swedish SP ...
      //
      this.attributeConverter.toGenericAttributes(eidasToken.getEidasAttributes())
          .forEach(eidasToken::addAttribute);

      // Add country attribute ...
      //
      eidasToken.addAttribute(
          GenericAttribute.of(AttributeIdentifiers.COUNTRY, eidasToken.getAuthnRequest().getCountry()));

      // Add the ID of the assertion as a transactionIdentifier attribute ...
      //
      eidasToken.addAttribute(
          GenericAttribute.of(AttributeIdentifiers.TRANSACTION_IDENTIFIER, eidasToken.getAssertion().getID()));

      // Add the entire assertion as a Base64 encoded blob to the authServerSignature attribute ...
      //
      Optional.ofNullable(eidasToken.getAssertionBase64())
          .ifPresent(a -> eidasToken.addAttribute(
              GenericAttribute.of(AttributeIdentifiers.AUTHENTICATION_EVIDENCE, a)));

      // Invoke the PRID service to resolve the eIDAS person identifier to a Swedish
      // PRID attribute ...
      //
      try {
        final PridResult pridResult = this.pridService.generatePrid(
            (String) eidasToken.getPrincipal(), eidasToken.getAuthnRequest().getCountry());

        eidasToken.addAttribute(GenericAttribute.of(AttributeIdentifiers.PRID, pridResult.prid()));
        eidasToken.addAttribute(
            GenericAttribute.of(AttributeIdentifiers.PRID_PERSISTENCE, pridResult.pridPersistence()));
      }
      catch (final IllegalArgumentException | PridGeneratorException | CountryPolicyNotFoundException e) {
        log.error("Failed to generate PRID attribute from ID '{}' ({}) - {}",
            eidasToken.getPrincipal(), eidasToken.getAuthnRequest().getCountry(),
            e.getMessage(), e);

        this.eventPublisher.publishEvent(new ResponseProcessingErrorEvent(inputToken, e.getMessage()));

        throw SamlErrors.error(StatusCode.RESPONDER, StatusCode.AUTHN_FAILED, "Failed to create PRID attribute", e);
      }

      return eidasToken;
    }
    catch (final ResponseStatusErrorException e) {
      log.info("Received non successful status: {}", e.getMessage());

      // Signal event ...
      //
      this.eventPublisher.publishEvent(new ErrorEidasResponseEvent(inputToken, e.getResponse()));

      // Let's update the status message - It may be in any language, so we add
      // a text telling that the failure was received from the foreign service.
      //
      final Status status = e.getStatus();
      throw SamlErrors.error(
          Optional.ofNullable(status.getStatusCode()).map(StatusCode::getValue).orElse(StatusCode.RESPONDER),
          Optional.ofNullable(status.getStatusCode()).map(StatusCode::getStatusCode).map(StatusCode::getValue)
              .orElse(StatusCode.AUTHN_FAILED),
          Optional.ofNullable(status.getStatusMessage())
              .map(StatusMessage::getValue)
              .map("Failure received from foreign service: %s"::formatted)
              .orElse("Failure received from foreign service"),
          e);
    }
    catch (final ResponseProcessingException e) {
      log.info("Error while processing eIDAS response - {}", e.getMessage(), e);

      this.eventPublisher.publishEvent(new ResponseProcessingErrorEvent(inputToken, e.getMessage(), e.getResponse()));

      if (e instanceof final EidasResponseValidationException eidasError) {
        throw eidasError.getError();
      }
      else {
        final String msg = "Validation failure of response from foreign service: %s".formatted(e.getMessage());
        throw SamlErrors.error(StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, msg, e);
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
        return EidasAuthenticationProvider.this.samlResponseUrl;
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
   * attributes. This is only done for signature services.
   *
   * @param inputToken the input token
   * @param token the eIDAS authentication token
   * @throws SamlErrorStatusException for assertion errors
   */
  public void assertPrincipalSelection(
      final UserAuthenticationInputToken inputToken, final EidasAuthenticationToken token)
      throws SamlErrorStatusException {

    // Only assert for signing user ...
    //
    if (!SignServiceSsoVoter.isSignatureService(inputToken.getAuthnRequirements())) {
      return;
    }

    try {
      for (final GenericRequestedAttribute requested : inputToken.getAuthnRequirements().getRequestedAttributes()) {
        if (requested.getRequestedValues().isEmpty()) {
          continue;
        }
        final String psValue = String.valueOf(requested.getRequestedValues().getFirst());
        final String id = requested.getIdentifier();

        if (AttributeIdentifiers.PERSONAL_IDENTITY_NUMBER.equals(id)
            || AttributeIdentifiers.COORDINATION_NUMBER.equals(id)
            || AttributeIdentifiers.MAPPED_PERSONAL_IDENTITY_NUMBER.equals(id)
            || AttributeIdentifiers.MAPPED_COORDINATION_NUMBER.equals(id)) {

          final List<String> values = new ArrayList<>();
          Optional.ofNullable(token.getAttribute(AttributeIdentifiers.MAPPED_PERSONAL_IDENTITY_NUMBER))
              .ifPresent(a -> values.addAll(a.getStringValues()));
          Optional.ofNullable(token.getAttribute(AttributeIdentifiers.MAPPED_COORDINATION_NUMBER))
              .ifPresent(a -> values.addAll(a.getStringValues()));

          checkPrincipalSelectionValue(id, psValue,
              se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_PERSONAL_IDENTITY_NUMBER,
              values);
        }
        else if (AttributeIdentifiers.PRID.equals(id)) {
          checkPrincipalSelectionValue(id, psValue,
              se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_PRID,
              Optional.ofNullable(token.getAttribute(AttributeIdentifiers.PRID))
                  .map(GenericAttribute::getStringValues)
                  .orElse(null));
        }
        else if (AttributeIdentifiers.EIDAS_PERSON_IDENTIFIER.equals(id)) {
          checkPrincipalSelectionValue(id, psValue,
              se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_EIDAS_PERSON_IDENTIFIER,
              Optional.ofNullable(token.getAttribute(AttributeIdentifiers.EIDAS_PERSON_IDENTIFIER))
                  .map(GenericAttribute::getStringValues)
                  .orElse(null));
        }
      }
    }
    catch (final SamlErrorStatusException e) {
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
  public boolean hasIdmRecord(final EidasAuthenticationToken token, final UserAuthenticationInputToken inputToken) {
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
   * @param inputToken the input token
   */
  public void obtainIdmRecord(final EidasAuthenticationToken token, final UserAuthenticationInputToken inputToken) {
    try {
      final IdmRecord idmRecord = this.idmClient.getRecord(token);

      log.info("Received IdM record for user '{}': swedish-id:'{}', binding:{} [{}]",
          token.getPrincipal(), idmRecord.getSwedishIdentity(), idmRecord.getBinding(), token.getLogString());

      token.addAttribute(GenericAttribute.of(
          SwedishIdentityNumbers.isCoordinationNumber(idmRecord.getSwedishIdentity())
              ? AttributeIdentifiers.MAPPED_COORDINATION_NUMBER
              : AttributeIdentifiers.MAPPED_PERSONAL_IDENTITY_NUMBER,
          idmRecord.getSwedishIdentity()));
      token.addAttribute(GenericAttribute.of(AttributeIdentifiers.IDENTITY_BINDING, idmRecord.getBinding()));

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
   * @throws SamlErrorStatusException for mismatches
   */
  private static void checkPrincipalSelectionValue(
      final String psName, final String psValue, final String friendlyName, final List<String> values)
      throws SamlErrorStatusException {

    if (values == null || values.isEmpty()) {
      return;
    }
    if (!values.contains(psValue)) {
      log.info("PrincipalSelection for attribute '{}' specified value '{}', but authenticated value was {}",
          psName, psValue, values);
      throw SamlErrors.error(StatusCode.RESPONDER, StatusCode.UNKNOWN_PRINCIPAL,
          "%s attribute from authentication does not match %s attribute from request"
              .formatted(friendlyName, friendlyName));
    }
  }

  /**
   * Generates an authentication request for the given country.
   *
   * @param httpServletRequest the HTTP servlet request
   * @param country the recipient country
   * @param token the redirect token of the authentication
   * @return an {@code AuthnRequest}
   * @throws SamlErrorStatusException for errors generating the request
   */
  public RequestHttpObject<AuthnRequest> generateAuthnRequest(final HttpServletRequest httpServletRequest,
      final String country, final RedirectForAuthenticationToken token) throws SamlErrorStatusException {

    final UserAuthenticationInputToken inputToken = token.getAuthnInputToken();

    final CountryMetadata countryMetadata = this.metadataProvider.getCountry(country);
    if (countryMetadata == null) {
      final String msg = "No services available for selected country";
      log.error("No metadata found for country '{}'", country);
      throw SamlErrors.error(StatusCode.RESPONDER, StatusCode.NO_AVAILABLE_IDP, msg);
    }

    try {
      final String relayState = Optional.ofNullable(SamlRequestData.getRelayState(inputToken))
          .filter(StringUtils::hasText)
          .orElseGet(() -> UUID.randomUUID().toString());

      final RequestHttpObject<AuthnRequest> authnRequest =
          this.authnRequestGenerator.generateAuthnRequest(countryMetadata, inputToken, relayState);

      // Add event ...
      //
      this.eventPublisher.publishEvent(new BeforeEidasAuthenticationEvent(inputToken,
          countryMetadata.getCountryCode(), authnRequest.getRequest(), relayState, authnRequest.getMethod()));

      // Save request in session ...
      //
      httpServletRequest.getSession().setAttribute(EIDAS_AUTHNREQUEST_SESSION_KEY,
          new EidasAuthnRequest(authnRequest.getRequest(), relayState,
              SamlRequestData.getAuthnRequest(inputToken).getID(), countryMetadata.getCountryCode(),
              token.getAuthnId()));

      return authnRequest;
    }
    catch (final RequestGenerationException e) {
      log.error("Failed to generate AuthnRequest for '{}' - {} [{}]", country, e.getMessage(),
          inputToken.getLogString(), e);
      throw SamlErrors.errorWithMessageCode(StatusCode.RESPONDER, StatusCode.AUTHN_FAILED,
          "authn-server.error.authn-failed", "Failed to generate AuthnRequest");
    }
  }

  /**
   * Supports {@link EidasAuthenticationToken} and {@link EidasAuthenticationErrorToken}.
   */
  @Override
  public boolean supportsUserAuthenticationToken(final Authentication authentication) {
    return authentication instanceof EidasAuthenticationToken
        || authentication instanceof EidasAuthenticationErrorToken;
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
  protected List<String> filterRequestedAuthnContextUris(final UserAuthenticationInputToken token) {
    final List<String> filtered = super.filterRequestedAuthnContextUris(token);
    if (this.pingWhitelist.isEmpty() || !this.pingWhitelist.contains(token.getRequester().identifier())) {
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
  public boolean isPingRequest(final UserAuthenticationInputToken token) {
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
    httpRequest.getSession().setAttribute(getTokenSessionKey(httpRequest), token);
  }

  /**
   * Removes the {@link EidasAuthenticationToken} from session storage.
   *
   * @param httpRequest the HTTP servlet request
   */
  public void removeEidasAuthenticationToken(final HttpServletRequest httpRequest) {
    final HttpSession session = httpRequest.getSession(false);
    if (session != null) {
      session.removeAttribute(getTokenSessionKey(httpRequest));
    }
  }

  /**
   * Gets the {@link EidasAuthenticationToken} from session storage.
   *
   * @param httpRequest the HTTP servlet request
   * @return an {@link EidasAuthenticationToken}
   * @throws UnrecoverableErrorException for session errors
   */
  public EidasAuthenticationToken getEidasAuthenticationToken(final HttpServletRequest httpRequest)
      throws UnrecoverableErrorException {

    final HttpSession session = httpRequest.getSession();
    final EidasAuthenticationToken token =
        (EidasAuthenticationToken) session.getAttribute(getTokenSessionKey(httpRequest));
    if (token == null) {
      throw new UnrecoverableErrorException(CommonUnrecoverableError.INVALID_SESSION,
          "Session error - no authentication request available");
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
   * @throws UnrecoverableErrorException for session errors
   */
  private EidasAuthnRequest getEidasAuthnRequest(final HttpServletRequest httpRequest)
      throws UnrecoverableErrorException {

    final HttpSession session = httpRequest.getSession();
    final EidasAuthnRequest authnRequest = (EidasAuthnRequest) session.getAttribute(EIDAS_AUTHNREQUEST_SESSION_KEY);
    if (authnRequest == null) {
      throw new UnrecoverableErrorException(CommonUnrecoverableError.INVALID_SESSION,
          "Received SAML response, but no authentication requests exists in session");
    }

    // Remove the authn request from the session ...
    session.removeAttribute(EIDAS_AUTHNREQUEST_SESSION_KEY);

    return authnRequest;
  }

  /**
   * Gets the session key for the {@link EidasAuthenticationToken} of the authentication that the request concerns.
   *
   * @param httpRequest the HTTP servlet request
   * @return the session key
   */
  private static String getTokenSessionKey(final HttpServletRequest httpRequest) {
    return "%s.%s".formatted(EIDAS_AUTHNTOKEN_SESSION_KEY,
        Optional.ofNullable(RedirectForAuthenticationToken.getAuthnId(httpRequest)).orElse(""));
  }

  /**
   * Removes the authentication that is saved in the session for single sign-on.
   *
   * @param httpRequest the HTTP servlet request, may be {@code null}
   */
  private static void clearSessionAuthentication(final HttpServletRequest httpRequest) {
    SecurityContextHolder.clearContext();
    Optional.ofNullable(httpRequest)
        .map(r -> r.getSession(false))
        .ifPresent(s -> s.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
  }

}
