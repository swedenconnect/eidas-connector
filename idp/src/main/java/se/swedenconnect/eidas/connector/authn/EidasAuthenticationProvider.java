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

import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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
import org.springframework.util.StringUtils;
import se.oidc.nimbus.claims.ScopeConstants;
import se.swedenconnect.eidas.connector.attributes.EidasAttributeConverter;
import se.swedenconnect.eidas.connector.authn.idm.IdmClient;
import se.swedenconnect.eidas.connector.authn.idm.IdmException;
import se.swedenconnect.eidas.connector.authn.idm.IdmRecord;
import se.swedenconnect.eidas.connector.authn.metadata.CountryMetadata;
import se.swedenconnect.eidas.connector.authn.metadata.EuMetadataProvider;
import se.swedenconnect.eidas.connector.authn.sp.AuthnContextClassRefMapper;
import se.swedenconnect.eidas.connector.authn.sp.EidasAuthnRequest;
import se.swedenconnect.eidas.connector.authn.sp.EidasAuthnRequestGenerator;
import se.swedenconnect.eidas.connector.events.BeforeEidasAuthenticationEvent;
import se.swedenconnect.eidas.connector.events.ErrorEidasResponseEvent;
import se.swedenconnect.eidas.connector.events.IdentityMatchingErrorEvent;
import se.swedenconnect.eidas.connector.events.IdentityMatchingRecordEvent;
import se.swedenconnect.eidas.connector.events.ResponseProcessingErrorEvent;
import se.swedenconnect.eidas.connector.events.SuccessEidasResponseEvent;
import se.swedenconnect.eidas.connector.idp.SignMessageProcessor;
import se.swedenconnect.eidas.connector.idp.UserMessageProcessor;
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
import se.swedenconnect.spring.authnserver.authentication.AuthenticationProtocol;
import se.swedenconnect.spring.authnserver.authentication.UserAuthentication;
import se.swedenconnect.spring.authnserver.authentication.provider.UserAuthenticationInputToken;
import se.swedenconnect.spring.authnserver.authentication.provider.redirect.AbstractUserRedirectAuthenticationProvider;
import se.swedenconnect.spring.authnserver.authentication.provider.redirect.RedirectForAuthenticationToken;
import se.swedenconnect.spring.authnserver.authentication.provider.redirect.ResumedAuthenticationToken;
import se.swedenconnect.spring.authnserver.error.AuthenticationError;
import se.swedenconnect.spring.authnserver.error.AuthenticationErrorException;
import se.swedenconnect.spring.authnserver.error.CommonUnrecoverableError;
import se.swedenconnect.spring.authnserver.error.UnrecoverableErrorException;
import se.swedenconnect.spring.authnserver.message.GenericSignMessage;
import se.swedenconnect.spring.authnserver.message.GenericUserMessage;
import se.swedenconnect.spring.authnserver.oidc.scope.BuiltInScopes;
import se.swedenconnect.spring.authnserver.saml.authnrequest.Saml2AuthnRequestData;
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
  public static final @NonNull String AUTHN_PATH = "/extauth";

  /** The path where we redirect the user to leave the control back to the SAML engine. */
  public static final @NonNull String RESUME_PATH = "/resume";

  /** Special purpose AuthnContext Class Ref for eIDAS test. */
  public static final @NonNull String EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF = "http://eidas.europa.eu/LoA/test";

  /** The session key where we store the eIDAS AuthnRequest. */
  public static final @NonNull String EIDAS_AUTHNREQUEST_SESSION_KEY = EidasAuthnRequest.class.getName();

  /** The session key prefix for where we store the eIDAS authentication token. */
  public static final @NonNull String EIDAS_AUTHNTOKEN_SESSION_KEY = EidasAuthenticationToken.class.getName();

  /**
   * The attributes that the connector delivers, which give the OpenID Connect claims that it supports. The attributes
   * from Identity Matching are added when it is active.
   */
  private static final @NonNull List<String> SUPPORTED_ATTRIBUTES = List.of(
      AttributeIdentifiers.EIDAS_PERSON_IDENTIFIER, AttributeIdentifiers.SURNAME, AttributeIdentifiers.GIVEN_NAME,
      AttributeIdentifiers.DATE_OF_BIRTH, AttributeIdentifiers.BIRTH_FAMILY_NAME, AttributeIdentifiers.BIRTH_GIVEN_NAME,
      AttributeIdentifiers.PLACE_OF_BIRTH, AttributeIdentifiers.PLACE_OF_BIRTH_COUNTRY,
      AttributeIdentifiers.PLACE_OF_BIRTH_LOCALITY, AttributeIdentifiers.COUNTRY_OF_CITIZENSHIP,
      AttributeIdentifiers.COUNTRY_OF_RESIDENCE, AttributeIdentifiers.EIDAS_ADDRESS_PO_BOX,
      AttributeIdentifiers.EIDAS_ADDRESS_LOCATOR_DESIGNATOR, AttributeIdentifiers.EIDAS_ADDRESS_LOCATOR_NAME,
      AttributeIdentifiers.EIDAS_ADDRESS_AREA, AttributeIdentifiers.EIDAS_ADDRESS_THOROUGHFARE,
      AttributeIdentifiers.EIDAS_ADDRESS_POST_NAME, AttributeIdentifiers.EIDAS_ADDRESS_ADMIN_UNIT_FIRST_LINE,
      AttributeIdentifiers.EIDAS_ADDRESS_ADMIN_UNIT_SECOND_LINE, AttributeIdentifiers.EIDAS_ADDRESS_POST_CODE,
      AttributeIdentifiers.TELEPHONE_NUMBER, AttributeIdentifiers.EMAIL, AttributeIdentifiers.GENDER,
      AttributeIdentifiers.EIDAS_COUNTRY, AttributeIdentifiers.PRID, AttributeIdentifiers.PRID_PERSISTENCE,
      AttributeIdentifiers.TRANSACTION_IDENTIFIER, AttributeIdentifiers.AUTHENTICATION_EVIDENCE,
      AttributeIdentifiers.AUTHENTICATION_PROVIDER);

  /** The attributes that Identity Matching gives. */
  private static final @NonNull List<String> IDM_ATTRIBUTES = List.of(
      AttributeIdentifiers.MAPPED_PERSONAL_IDENTITY_NUMBER, AttributeIdentifiers.MAPPED_COORDINATION_NUMBER,
      AttributeIdentifiers.IDENTITY_BINDING);

  /** The OpenID Connect scopes that the connector offers. The IdM scope is added when Identity Matching is active. */
  private static final @NonNull List<String> SUPPORTED_SCOPES = List.of(
      BuiltInScopes.EIDAS_NATURAL_PERSON_IDENTITY.getValue(), ScopeConstants.NATURAL_PERSON_INFO.getValue(),
      OIDCScopeValue.PROFILE.getValue(), ScopeConstants.SIGN_APPROVAL.getValue());

  /** The event publisher. */
  private final @NonNull ApplicationEventPublisher eventPublisher;

  /** SAML SP metadata. */
  private final @NonNull EntityDescriptor spMetadata;

  /** For generating AuthnRequests. */
  private final @NonNull EidasAuthnRequestGenerator authnRequestGenerator;

  /** The processor handling the SAML responses received from the foreign eIDAS proxy services. */
  private final @NonNull ResponseProcessor responseProcessor;

  /** For converting eIDAS attributes into the attributes released to the Swedish SP. */
  private final @NonNull EidasAttributeConverter attributeConverter;

  /** The URL where we receive SAML responses. */
  private final @NonNull String samlResponseUrl;

  /** The metadata provider. */
  private final @NonNull EuMetadataProvider metadataProvider;

  /** The PRID service bean. */
  private final @NonNull PridService pridService;

  /** The Identity Matching client. */
  private final @NonNull IdmClient idmClient;

  /** Looks up the requester in the client registry. */
  private final @NonNull RequesterLookup requesterLookup;

  /** Supported LoA URI:s. */
  private final @NonNull List<String> supportedLoas;

  /** The entity categories. */
  private final @NonNull List<String> entityCategories;

  /** Whitelisted SP:s that are allowed to send "ping" requests. */
  private final @NonNull List<String> pingWhitelist;

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
   * @param requesterLookup looks up the requester in the client registry
   * @param supportedLoas supported LoA URI:s
   * @param entityCategories the entity categories
   * @param pingWhitelist the whitelisted SP:s that are allowed to send ping requests
   */
  public EidasAuthenticationProvider(final @NonNull String baseUrl,
      final @NonNull ApplicationEventPublisher eventPublisher,
      final @NonNull EntityDescriptor spMetadata,
      final @NonNull EidasAuthnRequestGenerator authnRequestGenerator,
      final @NonNull ResponseProcessor responseProcessor,
      final @NonNull EuMetadataProvider metadataProvider,
      final @NonNull EidasAttributeConverter attributeConverter,
      final @NonNull PridService pridService,
      final @NonNull IdmClient idmClient,
      final @NonNull RequesterLookup requesterLookup,
      final @NonNull List<String> supportedLoas,
      final @NonNull List<String> entityCategories,
      final @Nullable List<String> pingWhitelist) {

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
    this.requesterLookup = Objects.requireNonNull(requesterLookup, "requesterLookup must not be null");
    this.supportedLoas = Collections.unmodifiableList(
        Objects.requireNonNull(supportedLoas, "supportedLoas must not be null"));
    this.entityCategories = Collections.unmodifiableList(
        Objects.requireNonNull(entityCategories, "entityCategories must not be null"));
    this.pingWhitelist = Optional.ofNullable(pingWhitelist).orElseGet(Collections::emptyList);
  }

  /**
   * Refuses a sign message that the connector cannot display, and a request that holds both a user message and a sign
   * message, before the user is sent to the pages of the connector.
   */
  @Override
  protected @NonNull Authentication authenticate(final @NonNull UserAuthenticationInputToken token,
      final @NonNull List<String> authnContextUris) throws AuthenticationErrorException {
    final GenericUserMessage userMessage = token.getAuthnRequirements().getUserMessage();
    final GenericSignMessage signMessage = token.getAuthnRequirements().getSignMessage();
    if (userMessage != null && signMessage != null) {
      log.info("Request holds both a user message and a sign message - rejecting [{}]", token.getLogString());
      throw new AuthenticationErrorException(EidasAuthenticationError.REQUEST_UNSUPPORTED,
          "A user message and a sign message must not both be given");
    }
    SignMessageProcessor.assertSupported(signMessage);
    if (userMessage != null && !UserMessageProcessor.isDisplayable(userMessage)) {
      log.info("UserMessage of type {} is not displayed [{}]", userMessage.getMimeType().getMimeType(),
          token.getLogString());
    }
    return super.authenticate(token, authnContextUris);
  }

  /**
   * The method will be called when the user comes back to the resume path after the authentication at the foreign
   * country.
   */
  @Override
  protected @NonNull UserAuthentication createUserAuthentication(final @NonNull ResumedAuthenticationToken token)
      throws AuthenticationErrorException {
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
   * @throws AuthenticationErrorException for errors that are reported to the requester
   * @throws UnrecoverableErrorException if there is no matching authentication request in the session
   */
  public @NonNull EidasAuthenticationToken processSamlResponse(final @NonNull HttpServletRequest httpRequest,
      final @NonNull UserAuthenticationInputToken inputToken, final @Nullable String samlResponse,
      final @Nullable String relayState)
      throws AuthenticationErrorException, UnrecoverableErrorException {

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
      catch (final AuthenticationErrorException e) {
        this.eventPublisher.publishEvent(new ResponseProcessingErrorEvent(inputToken, e.getMessage()));
        throw e;
      }

      // Map eIDAS attributes to the attributes released to the Swedish SP ...
      //
      this.attributeConverter.toGenericAttributes(eidasToken.getEidasAttributes())
          .forEach(eidasToken::addAttribute);

      // Add the country of the foreign eIDAS node. SAML releases it as the c attribute ...
      //
      eidasToken.addAttribute(
          GenericAttribute.of(AttributeIdentifiers.EIDAS_COUNTRY, eidasToken.getAuthnRequest().getCountry()));

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

        throw new AuthenticationErrorException(EidasAuthenticationError.PRID_FAILURE,
            "Failed to create PRID attribute", e);
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
      throw new AuthenticationErrorException(
          EidasAuthenticationError.foreignStatus(
              Optional.ofNullable(status.getStatusCode()).map(StatusCode::getValue).orElse(StatusCode.RESPONDER),
              Optional.ofNullable(status.getStatusCode()).map(StatusCode::getStatusCode).map(StatusCode::getValue)
                  .orElse(StatusCode.AUTHN_FAILED)),
          Optional.ofNullable(status.getStatusMessage())
              .map(StatusMessage::getValue)
              .map("Failure received from foreign service: %s"::formatted)
              .orElse("Failure received from foreign service"),
          e);
    }
    catch (final ResponseProcessingException e) {
      log.info("Error while processing eIDAS response - {}", e.getMessage(), e);

      this.eventPublisher.publishEvent(new ResponseProcessingErrorEvent(inputToken, e.getMessage(), e.getResponse()));

      final String msg = "Validation failure of response from foreign service: %s".formatted(e.getMessage());
      throw new AuthenticationErrorException(EidasAuthenticationError.REQUEST_DENIED, msg, e);
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
      public @Nullable AuthnRequest getAuthnRequest(final @NonNull String id) {
        return Optional.ofNullable(eidasAuthnRequest)
            .map(EidasAuthnRequest::getAuthnRequest)
            .filter(a -> Objects.equals(id, a.getID()))
            .orElse(null);
      }

      @Override
      public @Nullable String getRequestRelayState(final @NonNull String id) {
        return Optional.ofNullable(eidasAuthnRequest)
            .map(EidasAuthnRequest::getRelayState)
            .orElse(null);
      }

      @Override
      public @NonNull String getReceiveURL() {
        return EidasAuthenticationProvider.this.samlResponseUrl;
      }

      @Override
      public @NonNull Instant getReceiveInstant() {
        return Instant.now();
      }

      @Override
      public @NonNull String getClientIpAddress() {
        return httpServletRequest.getRemoteAddr();
      }

      @Override
      public @Nullable X509Certificate getClientCertificate() {
        return null;
      }

    };

  }

  /**
   * Asserts that the user matches the attribute values that the requester asked for. It is called before the user is
   * asked to consent to a signature, so that a sign message is never shown to another user than the one that the
   * requester asked for.
   * <p>
   * The requested values are compared with the values of the user. A requested personal identity number or
   * coordination number is compared with the number that Identity Matching gave. An attribute that the user does not
   * have is not compared. A mismatch fails the authentication if the values are essential, or if the requester is a
   * signature service. Otherwise it is logged.
   * </p>
   *
   * @param inputToken the input token
   * @param token the eIDAS authentication token
   * @throws AuthenticationErrorException {@link AuthenticationError#UNKNOWN_PRINCIPAL} if the user does not match
   */
  public void assertRequestedValues(
      final @NonNull UserAuthenticationInputToken inputToken, final @NonNull EidasAuthenticationToken token)
      throws AuthenticationErrorException {

    final boolean signatureService = SignServiceSsoVoter.isSignatureService(inputToken.getAuthnRequirements());

    for (final GenericRequestedAttribute requested : inputToken.getAuthnRequirements().getRequestedAttributes()) {
      if (requested.getRequestedValues().isEmpty()) {
        continue;
      }
      final String id = requested.getIdentifier();
      final List<Serializable> values = getUserValues(id, token);
      if (values.isEmpty() || !Collections.disjoint(values, requested.getRequestedValues())) {
        continue;
      }
      if (requested.isRequestedValuesEssential() || signatureService) {
        log.info("The authenticated user does not match the requested values of attribute '{}' [{}]",
            id, inputToken.getLogString());
        final String msg = "The authenticated user does not match the requested values of attribute '%s'"
            .formatted(id);
        this.eventPublisher.publishEvent(new ResponseProcessingErrorEvent(inputToken, msg));
        throw new AuthenticationErrorException(AuthenticationError.UNKNOWN_PRINCIPAL, msg);
      }
      log.info("The authenticated user does not match the voluntary requested values of attribute '{}' - "
          + "proceeding [{}]", id, inputToken.getLogString());
    }
  }

  /**
   * Gets the values of the user to compare with the requested values of an attribute. For a personal identity number
   * or a coordination number, these are the numbers that Identity Matching gave.
   *
   * @param id the attribute identifier
   * @param token the eIDAS authentication token
   * @return the values, empty if the user does not have the attribute
   */
  private static @NonNull List<Serializable> getUserValues(
      final @NonNull String id, final @NonNull EidasAuthenticationToken token) {
    final List<String> ids = AttributeIdentifiers.PERSONAL_IDENTITY_NUMBER.equals(id)
        || AttributeIdentifiers.COORDINATION_NUMBER.equals(id)
        ? List.of(AttributeIdentifiers.MAPPED_PERSONAL_IDENTITY_NUMBER, AttributeIdentifiers.MAPPED_COORDINATION_NUMBER)
        : List.of(id);
    final List<Serializable> values = new ArrayList<>();
    for (final String i : ids) {
      Optional.ofNullable(token.getAttribute(i)).ifPresent(a -> values.addAll(a.getValues()));
    }
    return values;
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
   * Tells whether the IdM record should be looked up for the request. For SAML, it always is. An OpenID Connect
   * Relying Party gets the IdM record only when it asks for the claims that it gives, or asks for a personal identity
   * number or coordination number holding values.
   *
   * @param token the input token
   * @return {@code true} if the IdM record should be looked up, and {@code false} otherwise
   */
  public boolean isIdmRequested(final @NonNull UserAuthenticationInputToken token) {
    if (token.getRequester().protocol() != AuthenticationProtocol.OIDC) {
      return true;
    }
    return token.getAuthnRequirements().getRequestedAttributes().stream()
        .anyMatch(a -> IDM_ATTRIBUTES.contains(a.getIdentifier())
            || (!a.getRequestedValues().isEmpty()
            && (AttributeIdentifiers.PERSONAL_IDENTITY_NUMBER.equals(a.getIdentifier())
            || AttributeIdentifiers.COORDINATION_NUMBER.equals(a.getIdentifier()))));
  }

  /**
   * Checks if the user identified by the authentication token has an IdM record.
   *
   * @param token the authentication token
   * @param inputToken the authentication input token
   * @return {@code true} if the user has a record and {@code false} otherwise
   */
  public boolean hasIdmRecord(final @NonNull EidasAuthenticationToken token,
      final @NonNull UserAuthenticationInputToken inputToken) {
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
  public void obtainIdmRecord(final @NonNull EidasAuthenticationToken token,
      final @NonNull UserAuthenticationInputToken inputToken) {
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
   * Generates an authentication request for the given country.
   *
   * @param httpServletRequest the HTTP servlet request
   * @param country the recipient country
   * @param token the redirect token of the authentication
   * @return an {@code AuthnRequest}
   * @throws AuthenticationErrorException for errors generating the request
   */
  public @NonNull RequestHttpObject<AuthnRequest> generateAuthnRequest(
      final @NonNull HttpServletRequest httpServletRequest, final @NonNull String country,
      final @NonNull RedirectForAuthenticationToken token) throws AuthenticationErrorException {

    final UserAuthenticationInputToken inputToken = token.getAuthnInputToken();

    final CountryMetadata countryMetadata = this.metadataProvider.getCountry(country);
    if (countryMetadata == null) {
      log.error("No metadata found for country '{}'", country);
      throw new AuthenticationErrorException(EidasAuthenticationError.NO_COUNTRY_METADATA,
          "No services available for selected country");
    }

    try {
      // A SAML request passes on the RelayState of the Swedish SP, and other requests get a random RelayState ...
      //
      final String relayState = Optional.ofNullable(
              inputToken.getProtocolRequestData() instanceof final Saml2AuthnRequestData samlData
                  ? samlData.responseAttributes().relayState()
                  : null)
          .filter(StringUtils::hasText)
          .orElseGet(() -> UUID.randomUUID().toString());

      final RequestHttpObject<AuthnRequest> authnRequest = this.authnRequestGenerator.generateAuthnRequest(
          countryMetadata, inputToken, relayState, this.requesterLookup.isPublicSector(inputToken));

      // Add event ...
      //
      this.eventPublisher.publishEvent(new BeforeEidasAuthenticationEvent(inputToken,
          countryMetadata.getCountryCode(), authnRequest.getRequest(), relayState, authnRequest.getMethod()));

      // Save request in session ...
      //
      httpServletRequest.getSession().setAttribute(EIDAS_AUTHNREQUEST_SESSION_KEY,
          new EidasAuthnRequest(authnRequest.getRequest(), relayState, inputToken.getRequestId(),
              countryMetadata.getCountryCode(), token.getAuthnId()));

      return authnRequest;
    }
    catch (final RequestGenerationException e) {
      log.error("Failed to generate AuthnRequest for '{}' - {} [{}]", country, e.getMessage(),
          inputToken.getLogString(), e);
      throw new AuthenticationErrorException(AuthenticationError.AUTHN_FAILED, "Failed to generate AuthnRequest", e);
    }
  }

  /**
   * Supports {@link EidasAuthenticationToken}.
   */
  @Override
  public boolean supportsUserAuthenticationToken(final @Nullable Authentication authentication) {
    return authentication instanceof EidasAuthenticationToken;
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull String getName() {
    return "eidas-authn";
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull List<String> getSupportedAuthnContextUris() {
    return this.supportedLoas;
  }

  /**
   * Special handling since we also may support the special URI {@code http://eidas.europa.eu/LoA/test}.
   */
  @Override
  protected @NonNull List<String> filterRequestedAuthnContextUris(final @NonNull UserAuthenticationInputToken token) {
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
  public boolean isPingRequest(final @NonNull UserAuthenticationInputToken token) {
    return token.getAuthnRequirements().getAuthnContextRequirements().contains(EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF);
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull List<String> getEntityCategories() {
    return this.entityCategories;
  }

  /**
   * Gets the attributes that the connector delivers. The attributes from Identity Matching are included when it is
   * active.
   */
  @Override
  public @NonNull List<String> getSupportedAttributes() {
    if (!this.isIdmActive()) {
      return SUPPORTED_ATTRIBUTES;
    }
    final List<String> attributes = new ArrayList<>(SUPPORTED_ATTRIBUTES);
    attributes.addAll(IDM_ATTRIBUTES);
    return attributes;
  }

  /**
   * Gets the OpenID Connect scopes that the connector offers. The {@code eidasSwedishIdentity} scope is included when
   * Identity Matching is active.
   */
  @Override
  public @NonNull List<String> getSupportedScopes() {
    if (!this.isIdmActive()) {
      return SUPPORTED_SCOPES;
    }
    final List<String> scopes = new ArrayList<>(SUPPORTED_SCOPES);
    scopes.add(BuiltInScopes.EIDAS_SWEDISH_IDENTITY.getValue());
    return scopes;
  }

  /**
   * Saves the {@link EidasAuthenticationToken} in session storage.
   *
   * @param httpRequest the HTTP servlet request
   * @param token the token to save
   */
  public void saveEidasAuthenticationToken(final @NonNull HttpServletRequest httpRequest,
      final @NonNull EidasAuthenticationToken token) {
    httpRequest.getSession().setAttribute(getTokenSessionKey(httpRequest), token);
  }

  /**
   * Removes the {@link EidasAuthenticationToken} from session storage.
   *
   * @param httpRequest the HTTP servlet request
   */
  public void removeEidasAuthenticationToken(final @NonNull HttpServletRequest httpRequest) {
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
  public @NonNull EidasAuthenticationToken getEidasAuthenticationToken(final @NonNull HttpServletRequest httpRequest)
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

}
