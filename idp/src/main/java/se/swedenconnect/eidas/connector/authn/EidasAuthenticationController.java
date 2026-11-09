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

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;
import se.swedenconnect.eidas.connector.authn.EidasCountryHandler.SelectableCountry;
import se.swedenconnect.eidas.connector.authn.ui.EidasUiModelFactory;
import se.swedenconnect.eidas.connector.authn.ui.IdmUiModelFactory;
import se.swedenconnect.eidas.connector.authn.ui.SignUiModelFactory;
import se.swedenconnect.eidas.connector.authn.ui.UiLanguageHandler;
import se.swedenconnect.eidas.connector.config.CookieGenerator;
import se.swedenconnect.eidas.connector.events.BeforeCountrySelectionEvent;
import se.swedenconnect.eidas.connector.events.BeforeCountrySelectionEvent.NoDisplayReason;
import se.swedenconnect.eidas.connector.events.IdentityMatchingConsentEvent;
import se.swedenconnect.eidas.connector.events.SignatureConsentEvent;
import se.swedenconnect.opensaml.saml2.request.RequestHttpObject;
import se.swedenconnect.spring.authnserver.audit.AuthnEventPublisher;
import se.swedenconnect.spring.authnserver.authentication.provider.UserAuthenticationInputToken;
import se.swedenconnect.spring.authnserver.authentication.provider.redirect.AbstractAuthenticationController;
import se.swedenconnect.spring.authnserver.authentication.provider.redirect.RedirectForAuthenticationToken;
import se.swedenconnect.spring.authnserver.error.CommonUnrecoverableError;
import se.swedenconnect.spring.authnserver.error.UnrecoverableErrorException;
import se.swedenconnect.spring.authnserver.saml.error.SamlErrorStatusException;

import java.util.Optional;

/**
 * The authentication controller.
 * <p>
 * Every page of the controller carries the identifier of the authentication in progress in the
 * {@value RedirectForAuthenticationToken#AUTHN_ID_PARAMETER} parameter. The response from the foreign country does
 * not carry it, so it is added to that request by the {@link EidasAuthnFlowFilter}.
 * </p>
 *
 * @author Martin Lindström
 */
@Controller
public class EidasAuthenticationController extends AbstractAuthenticationController<EidasAuthenticationProvider> {

  /** Logger instance. */
  private static final Logger log = LoggerFactory.getLogger(EidasAuthenticationController.class);

  /** The path for receiving eIDAS assertions. */
  public static final @NonNull String ASSERTION_CONSUMER_PATH = "/extauth/saml2/post";

  /** Symbolic name for the action parameter value of "ok". */
  public static final @NonNull String ACTION_OK = "ok";

  /** Symbolic name for the action parameter value of "cancel". */
  public static final @NonNull String ACTION_CANCEL = "cancel";

  /** The name of the model attribute holding the identifier of the authentication. */
  public static final @NonNull String AUTHN_ID_MODEL_ATTRIBUTE = "authnId";

  /** The authentication provider. */
  private final @NonNull EidasAuthenticationProvider provider;

  /** The UI language handler. */
  private final @NonNull UiLanguageHandler uiLanguageHandler;

  /** Factory for UI model. */
  private final @NonNull EidasUiModelFactory eidasUiModelFactory;

  /** Factory for sign consent UI model. */
  private final @NonNull SignUiModelFactory signUiModelFactory;

  /** Factory for IdM consent UI model. */
  private final @NonNull IdmUiModelFactory idmUiModelFactory;

  /** Cookie generator for saving selected country. */
  private final @NonNull CookieGenerator selectedCountryCookieGenerator;

  /** Session cookie generator for saving selected country. */
  private final @NonNull CookieGenerator selectedCountrySessionCookieGenerator;

  /** Session cookie generator for IdM consents. */
  private final @NonNull CookieGenerator idmConsentCookieGenerator;

  /** Cookie generator for controlling whether the IdM banner should be hidden. */
  private final @NonNull CookieGenerator idmHideBannerCookieGenerator;

  /** For assisting us in selecting possible countries (to display). */
  private final @NonNull EidasCountryHandler countryHandler;

  /** The event publisher. */
  private final @NonNull ApplicationEventPublisher eventPublisher;

  /** For auditing errors that cannot be reported to the Swedish SP. */
  private final @NonNull AuthnEventPublisher authnEventPublisher;

  /**
   * Constructor.
   *
   * @param provider the authentication provider
   * @param uiLanguageHandler the UI language handler
   * @param eidasUiModelFactory factory for UI model
   * @param signUiModelFactory factory for sign consent UI model
   * @param idmUiModelFactory factory for IdM consent UI model
   * @param selectedCountryCookieGenerator cookie generator for saving selected country
   * @param selectedCountrySessionCookieGenerator session cookie generator for saving selected country
   * @param idmConsentCookieGenerator session cookie generator for IdM consents
   * @param idmHideBannerCookieGenerator cookie generator for controlling whether the IdM banner should be hidden
   * @param countryHandler for assisting us in selecting possible countries (to display)
   * @param eventPublisher the event publisher
   */
  public EidasAuthenticationController(final @NonNull EidasAuthenticationProvider provider,
      final @NonNull UiLanguageHandler uiLanguageHandler,
      final @NonNull EidasUiModelFactory eidasUiModelFactory,
      final @NonNull SignUiModelFactory signUiModelFactory,
      final @NonNull IdmUiModelFactory idmUiModelFactory,
      @Qualifier("selectedCountryCookieGenerator") final @NonNull CookieGenerator selectedCountryCookieGenerator,
      @Qualifier("selectedCountrySessionCookieGenerator")
      final @NonNull CookieGenerator selectedCountrySessionCookieGenerator,
      @Qualifier("idmConsentSessionCookieGenerator") final @NonNull CookieGenerator idmConsentCookieGenerator,
      @Qualifier("idmHideBannerCookieGenerator") final @NonNull CookieGenerator idmHideBannerCookieGenerator,
      final @NonNull EidasCountryHandler countryHandler,
      final @NonNull ApplicationEventPublisher eventPublisher) {
    this.provider = provider;
    this.uiLanguageHandler = uiLanguageHandler;
    this.eidasUiModelFactory = eidasUiModelFactory;
    this.signUiModelFactory = signUiModelFactory;
    this.idmUiModelFactory = idmUiModelFactory;
    this.selectedCountryCookieGenerator = selectedCountryCookieGenerator;
    this.selectedCountrySessionCookieGenerator = selectedCountrySessionCookieGenerator;
    this.idmConsentCookieGenerator = idmConsentCookieGenerator;
    this.idmHideBannerCookieGenerator = idmHideBannerCookieGenerator;
    this.countryHandler = countryHandler;
    this.eventPublisher = eventPublisher;
    this.authnEventPublisher = new AuthnEventPublisher(eventPublisher);
  }

  /** {@inheritDoc} */
  @Override
  protected @NonNull EidasAuthenticationProvider getProvider() {
    return this.provider;
  }

  /**
   * The entry point for the external authentication process.
   *
   * @param request the HTTP servlet request
   * @param response the HTTP servlet response
   * @param language optional parameter holding the selected language
   * @return a {@link ModelAndView}
   */
  @GetMapping(EidasAuthenticationProvider.AUTHN_PATH)
  public @NonNull ModelAndView authenticate(final @NonNull HttpServletRequest request,
      final @NonNull HttpServletResponse response,
      @RequestParam(name = "lang", required = false) final @Nullable String language) {

    final RedirectForAuthenticationToken redirectToken = this.getInputToken(request);
    final UserAuthenticationInputToken token = redirectToken.getAuthnInputToken();

    try {
      if (language != null) {
        this.uiLanguageHandler.setUiLanguage(request, response, language);
      }
      // If this is a call with the language parameter set it means that we have already processed the request,
      // and we shouldn't signal any events (or log stuff). That will be confusing.
      //
      final boolean signalEvent = language == null;

      // Selected country from this session.
      //
      final String selectedCountry = Optional.ofNullable(WebUtils.getCookie(
              request, this.selectedCountrySessionCookieGenerator.getName()))
          .map(Cookie::getValue)
          .orElse(null);

      if (signalEvent) {
        log.debug("Selected country from this session: {} [{}]",
            Optional.ofNullable(selectedCountry).orElse("none"), token.getLogString());
      }

      // The first step is to prompt the user for which country to direct to.

      // If the request is made by a signature service and the selected country is known, we skip
      // the "select country" view.
      //
      if (SignServiceSsoVoter.isSignatureService(token.getAuthnRequirements()) && selectedCountry != null) {
        if (signalEvent) {
          log.info("Request is from a signature service. Will default to previously selected country: '{}' [{}]",
              selectedCountry, token.getLogString());

          this.eventPublisher.publishEvent(new BeforeCountrySelectionEvent(
              token, selectedCountry, NoDisplayReason.SIGN_SERVICE_COUNTRY_FROM_SESSION));
        }

        return this.initiateAuthentication(request, response, selectedCountry);
      }

      // Get a listing of all countries to display ...
      //
      final EidasCountryHandler.SelectableCountries selectableCountries =
          this.countryHandler.getSelectableCountries(token);

      if (!selectableCountries.displaySelection()) {
        // If request contained only one country, we skip the country selection ...
        //
        if (signalEvent) {
          log.info("Request contains requested country '{}'. Will not display country selection [{}]",
              selectableCountries.countries().getFirst().country(), token.getLogString());
          this.eventPublisher.publishEvent(new BeforeCountrySelectionEvent(token,
              selectableCountries.countries().getFirst().country(), NoDisplayReason.FROM_AUTHN_REQUEST));
        }

        return this.initiateAuthentication(request, response, selectableCountries.countries().getFirst().country());
      }

      final ModelAndView modelAndView = new ModelAndView("country-select");
      modelAndView.addObject(AUTHN_ID_MODEL_ATTRIBUTE, redirectToken.getAuthnId());
      modelAndView.addObject("pingFlag", this.provider.isPingRequest(token));
      modelAndView.addObject("languages", this.uiLanguageHandler.getOtherLanguages());
      modelAndView.addObject("ui", this.eidasUiModelFactory.createUiModel(
          request, token, selectableCountries.countries()));

      if (signalEvent) {
        this.eventPublisher.publishEvent(new BeforeCountrySelectionEvent(token, selectableCountries.countries().stream()
            .filter(SelectableCountry::canAuthenticate)
            .map(SelectableCountry::country)
            .toList()));
      }

      return modelAndView;
    }
    catch (final SamlErrorStatusException e) {
      return this.completeWithError(request, e);
    }
  }

  /**
   * Controller method that initiates the authentication against the foreign Proxy Service ending with an authentication
   * being sent.
   *
   * @param httpRequest the request
   * @param httpResponse the response
   * @param selectedCountry the country code for the selected country
   * @return a model and view for POST or redirect of the request
   */
  @PostMapping(value = EidasAuthenticationProvider.AUTHN_PATH + "/proxyauth")
  public @NonNull ModelAndView initiateAuthentication(
      final @NonNull HttpServletRequest httpRequest, final @NonNull HttpServletResponse httpResponse,
      @RequestParam(name = "selectedCountry") final @NonNull String selectedCountry) {

    final RedirectForAuthenticationToken redirectToken = this.getInputToken(httpRequest);
    final UserAuthenticationInputToken inputToken = redirectToken.getAuthnInputToken();

    if (ACTION_CANCEL.equals(selectedCountry)) {
      log.info("User cancelled country selection - aborting authentication [{}]", inputToken.getLogString());
      return this.cancel(httpRequest);
    }

    try {
      log.debug("User selected country '{}' [{}]", selectedCountry, inputToken.getLogString());

      // Generate AuthnRequest
      //
      final RequestHttpObject<AuthnRequest> authnRequest = this.getProvider().generateAuthnRequest(
          httpRequest, selectedCountry, redirectToken);

      // Save the country as "selected" ...
      //
      this.selectedCountryCookieGenerator.addCookie(selectedCountry, httpResponse);
      this.selectedCountrySessionCookieGenerator.addCookie(selectedCountry, httpResponse);

      // POST or redirect ...
      //
      if (SAMLConstants.POST_METHOD.equals(authnRequest.getMethod())) {
        final ModelAndView modelAndView = new ModelAndView("post-request");
        modelAndView.addObject("action", authnRequest.getSendUrl());
        modelAndView.addAllObjects(authnRequest.getRequestParameters());
        return modelAndView;
      }
      else {
        return new ModelAndView("redirect:" + authnRequest.getSendUrl());
      }
    }
    catch (final SamlErrorStatusException e) {
      return this.completeWithError(httpRequest, e);
    }
  }

  /**
   * Receives the SAML response from the foreign IdP.
   *
   * <p>
   * The {@code SAMLResponse} parameter is required, but we set it as optional so that Spring does not report an error.
   * We want to set the error ourselves.
   * </p>
   *
   * @param httpRequest the servlet request
   * @param httpResponse the servlet response
   * @param samlResponse the SAML response
   * @param relayState optional relay state
   * @return a {@link ModelAndView}
   */
  @PostMapping(value = ASSERTION_CONSUMER_PATH)
  public @NonNull ModelAndView foreignResponse(
      final @NonNull HttpServletRequest httpRequest, final @NonNull HttpServletResponse httpResponse,
      @RequestParam(name = "SAMLResponse", required = false) final @Nullable String samlResponse,
      @RequestParam(name = "RelayState", required = false) final @Nullable String relayState) {

    log.debug("Received SAML response [client-ip-address='{}']", httpRequest.getRemoteAddr());

    final UserAuthenticationInputToken inputToken = this.getInputToken(httpRequest).getAuthnInputToken();
    try {
      // Process the response ...
      //
      final EidasAuthenticationToken token =
          this.getProvider().processSamlResponse(httpRequest, inputToken, samlResponse, relayState);

      // OK, now we have an authentication token. Time to complete the authentication ...
      //
      return this.completeAuthentication(httpRequest, httpResponse, token, false, false);
    }
    catch (final SamlErrorStatusException e) {
      return this.completeWithError(httpRequest, e);
    }
  }

  /**
   * Helper method that assists us in completing an authentication. Given an {@link EidasAuthenticationToken} the method
   * will perform the final (optional) steps in the authentication including consents from the user.
   *
   * @param httpRequest the HTTP servlet request
   * @param httpResponse the HTTP servlet response
   * @param token the eIDAS authentication token
   * @param idmProcessed if the IdM-step has been completed
   * @param signConsentProcessed if the Sign consent step has been completed
   * @return a {@link ModelAndView} instance
   */
  private @NonNull ModelAndView completeAuthentication(final @NonNull HttpServletRequest httpRequest,
      final @NonNull HttpServletResponse httpResponse, final @NonNull EidasAuthenticationToken token,
      final boolean idmProcessed, final boolean signConsentProcessed) {

    final UserAuthenticationInputToken inputToken = this.getInputToken(httpRequest).getAuthnInputToken();
    final boolean signatureService = SignServiceSsoVoter.isSignatureService(inputToken.getAuthnRequirements());

    if (!idmProcessed) {
      if (this.getProvider().isIdmActive()) {

        // Before we check whether the user has an IdM record, we check if the IdM consent cookie has been set.
        // If so, it means that we have already checked the IdM status in this session (and possibly prompted
        // for the consent).
        // We only check the cookie if this is a signature request since the legal requirements state that the
        // user needs to be prompted for consent every time he/she authenticates.
        //
        final IdmSessionState idmState = signatureService
            ? Optional.ofNullable(WebUtils.getCookie(httpRequest, this.idmConsentCookieGenerator.getName()))
            .map(Cookie::getValue)
            .map(IdmSessionState::fromString)
            .orElse(null)
            : null;
        if (idmState != null) {
          if (idmState == IdmSessionState.NO_RECORD) {
            log.debug("IdM record has already been checked for '{}' in the current session - skipping ... [{}]",
                token.getPrincipal(), token.getLogString());
            return this.completeAuthentication(httpRequest, httpResponse, token, true, false);
          }
          else {
            final String consent = idmState == IdmSessionState.GAVE_CONSENT ? ACTION_OK : ACTION_CANCEL;
            log.debug("User '{}' has already been prompted for Identity Matching consent ({}) [{}]",
                token.getPrincipal(), idmState.getValue(), token.getLogString());

            this.eventPublisher.publishEvent(new IdentityMatchingConsentEvent(inputToken, token,
                idmState == IdmSessionState.GAVE_CONSENT));

            this.getProvider().saveEidasAuthenticationToken(httpRequest, token);
            return this.idmConsentResult(httpRequest, httpResponse, consent);
          }
        }

        if (this.getProvider().hasIdmRecord(token, inputToken)) {
          // If the user has an IdM record, we need to ask for his or hers consent to read it ...
          //
          this.getProvider().saveEidasAuthenticationToken(httpRequest, token);
          return this.idmConsentPage(httpRequest, httpResponse, null);
        }
        else {
          this.idmConsentCookieGenerator.addCookie(IdmSessionState.NO_RECORD.getValue(), httpResponse);
          this.idmHideBannerCookieGenerator.addCookie("false", httpResponse);
        }
      }
    }
    else {
      log.trace("IdM feature is not active - no IdM record will be queried");
    }

    if (!signConsentProcessed) {
      if (signatureService) {
        log.debug("User '{}' has performed authentication given a request from a SignService. "
            + "Directing to signature consent dialogue ... [{}]", token.getPrincipal(), token.getLogString());

        this.getProvider().saveEidasAuthenticationToken(httpRequest, token);
        return this.signConsentPage(httpRequest, httpResponse, null);
      }
    }

    // OK, we are done communicating with the user. As a final step, we assert any potential
    // requirements on the authenticating/signing user.
    //
    try {
      this.getProvider().assertPrincipalSelection(inputToken, token);
    }
    catch (final SamlErrorStatusException e) {
      return this.completeWithError(httpRequest, e);
    }

    return this.complete(httpRequest, token);
  }

  /**
   * Delivers the Identity Matching consent page.
   *
   * @param httpRequest the HttpServletRequest object
   * @param httpResponse the HttpServletResponse object
   * @param language the language parameter (optional)
   * @return a ModelAndView object representing the IdM consent page
   */
  @GetMapping(EidasAuthenticationProvider.AUTHN_PATH + "/idmconsent")
  public @NonNull ModelAndView idmConsentPage(
      final @NonNull HttpServletRequest httpRequest, final @NonNull HttpServletResponse httpResponse,
      @RequestParam(name = "lang", required = false) final @Nullable String language) {

    if (language != null) {
      this.uiLanguageHandler.setUiLanguage(httpRequest, httpResponse, language);
    }
    final RedirectForAuthenticationToken redirectToken = this.getInputToken(httpRequest);

    final ModelAndView modelAndView = new ModelAndView("idm-consent");
    modelAndView.addObject(AUTHN_ID_MODEL_ATTRIBUTE, redirectToken.getAuthnId());
    modelAndView.addObject("languages", this.uiLanguageHandler.getOtherLanguages());
    modelAndView.addObject("idmConsent", this.idmUiModelFactory.createUiModel(redirectToken.getAuthnInputToken()));

    return modelAndView;
  }

  /**
   * Receives the results from the IdM consent page.
   *
   * @param httpRequest the HTTP servlet request
   * @param httpResponse the HTTP servlet response
   * @param action the result (ok or cancel)
   * @return a {@link ModelAndView}
   */
  @PostMapping(EidasAuthenticationProvider.AUTHN_PATH + "/idmresult")
  public @NonNull ModelAndView idmConsentResult(
      final @NonNull HttpServletRequest httpRequest, final @NonNull HttpServletResponse httpResponse,
      @RequestParam(name = "action") final @NonNull String action) {

    final UserAuthenticationInputToken inputToken = this.getInputToken(httpRequest).getAuthnInputToken();
    final EidasAuthenticationToken token = this.getProvider().getEidasAuthenticationToken(httpRequest);

    if (ACTION_OK.equals(action)) {
      log.debug("User '{}' consented to getting IdM record [{}]", token.getPrincipal(), token.getLogString());
      this.eventPublisher.publishEvent(new IdentityMatchingConsentEvent(inputToken, token, true));
      this.idmConsentCookieGenerator.addCookie(IdmSessionState.GAVE_CONSENT.getValue(), httpResponse);
      this.idmHideBannerCookieGenerator.addCookie("true", httpResponse);

      this.getProvider().obtainIdmRecord(token, inputToken);
    }
    else if (ACTION_CANCEL.equals(action)) {
      log.debug("User '{}' did not consent to getting IdM record [{}]", token.getPrincipal(), token.getLogString());
      this.eventPublisher.publishEvent(new IdentityMatchingConsentEvent(inputToken, token, false));
      this.idmConsentCookieGenerator.addCookie(IdmSessionState.DENIED_CONSENT.getValue(), httpResponse);
      this.idmHideBannerCookieGenerator.addCookie("false", httpResponse);
    }
    else {
      log.info("Unknown action parameter {} [{}]", action, token.getLogString());
      return this.idmConsentPage(httpRequest, httpResponse, null);
    }
    return this.completeAuthentication(httpRequest, httpResponse, token, true, false);
  }

  /**
   * Delivers the sign consent page.
   *
   * @param httpRequest the HttpServletRequest object
   * @param httpResponse the HttpServletResponse object
   * @param language the language parameter (optional)
   * @return a ModelAndView object representing the sign consent page
   */
  @GetMapping(EidasAuthenticationProvider.AUTHN_PATH + "/consent")
  public @NonNull ModelAndView signConsentPage(
      final @NonNull HttpServletRequest httpRequest, final @NonNull HttpServletResponse httpResponse,
      @RequestParam(name = "lang", required = false) final @Nullable String language) {

    if (language != null) {
      this.uiLanguageHandler.setUiLanguage(httpRequest, httpResponse, language);
    }
    final RedirectForAuthenticationToken redirectToken = this.getInputToken(httpRequest);

    try {
      final ModelAndView modelAndView = new ModelAndView("sign-consent");
      modelAndView.addObject(AUTHN_ID_MODEL_ATTRIBUTE, redirectToken.getAuthnId());
      modelAndView.addObject("languages", this.uiLanguageHandler.getOtherLanguages());
      modelAndView.addObject("signMessageConsent", this.signUiModelFactory.createUiModel(
          redirectToken.getAuthnInputToken(), this.getProvider().getEidasAuthenticationToken(httpRequest)));

      return modelAndView;
    }
    catch (final SamlErrorStatusException e) {
      return this.completeWithError(httpRequest, e);
    }
  }

  /**
   * Receives the results from the sign consent page.
   *
   * @param httpRequest the HTTP servlet request
   * @param httpResponse the HTTP servlet response
   * @param action the result (ok or cancel)
   * @return a {@link ModelAndView}
   */
  @PostMapping(EidasAuthenticationProvider.AUTHN_PATH + "/signed")
  public @NonNull ModelAndView signConsentResult(
      final @NonNull HttpServletRequest httpRequest, final @NonNull HttpServletResponse httpResponse,
      @RequestParam(name = "action") final @NonNull String action) {

    final UserAuthenticationInputToken inputToken = this.getInputToken(httpRequest).getAuthnInputToken();
    final EidasAuthenticationToken token = this.getProvider().getEidasAuthenticationToken(httpRequest);

    if (ACTION_OK.equals(action)) {
      log.debug("User consented to signature [{}]", inputToken.getLogString());

      token.setSignatureConsented(true);
      this.eventPublisher.publishEvent(new SignatureConsentEvent(inputToken, token, true));

      return this.completeAuthentication(httpRequest, httpResponse, token, true, true);
    }
    else if (ACTION_CANCEL.equals(action)) {
      log.debug("User did not consent to signing [{}]", inputToken.getLogString());
      this.eventPublisher.publishEvent(new SignatureConsentEvent(inputToken, token, false));

      return this.completeWithError(httpRequest, SamlErrors.errorWithMessageCode(StatusCode.RESPONDER,
          SamlErrors.CANCEL_STATUS_CODE, SamlErrors.REJECT_SIGNATURE_MESSAGE_CODE,
          "User did not consent to signature"));
    }
    else {
      log.info("Unknown action parameter {} [{}]", action, inputToken.getLogString());
      return this.signConsentPage(httpRequest, httpResponse, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected @NonNull ModelAndView complete(final @NonNull HttpServletRequest request,
      final @NonNull Authentication authentication) {
    this.getProvider().removeEidasAuthenticationToken(request);
    return super.complete(request, authentication);
  }

  /**
   * Completes the authentication with a SAML error that is reported to the Swedish SP.
   *
   * @param request the HTTP servlet request
   * @param error the error
   * @return a redirect to the resume path
   */
  protected @NonNull ModelAndView completeWithError(final @NonNull HttpServletRequest request,
      final @NonNull SamlErrorStatusException error) {
    return this.complete(request, new EidasAuthenticationErrorToken(error));
  }

  /** {@inheritDoc} */
  @Override
  protected @NonNull ModelAndView cancel(final @NonNull HttpServletRequest request) {
    this.getProvider().removeEidasAuthenticationToken(request);
    return super.cancel(request);
  }

  /**
   * Handles errors that cannot be reported to the Swedish SP, and unexpected exceptions.
   *
   * @param request the HTTP servlet request
   * @param e the exception
   * @return a {@link ModelAndView}
   */
  @ExceptionHandler(Exception.class)
  public @NonNull ModelAndView handleException(final @NonNull HttpServletRequest request, final @NonNull Exception e) {

    if (e instanceof final UnrecoverableErrorException unrecoverable) {
      log.info("Error that cannot be reported to the Service Provider - {}", e.getMessage());
      this.authnEventPublisher.publishUnrecoverableError(request, unrecoverable);

      final ModelAndView modelAndView = new ModelAndView("error/idp");
      modelAndView.addObject("idpErrorMessageCode", unrecoverable.getMessageCode());
      return modelAndView;
    }
    else {
      log.error("Caught unexpected exception - {}", e.getMessage(), e);
      this.authnEventPublisher.publishUnrecoverableError(request,
          new UnrecoverableErrorException(CommonUnrecoverableError.INTERNAL, e.getMessage(), e));

      final ModelAndView modelAndView = new ModelAndView("error");
      modelAndView.addObject("message", CommonUnrecoverableError.INTERNAL.getDescription());
      return modelAndView;
    }
  }

  /**
   * For storing the IdM state in a cookie.
   */
  private enum IdmSessionState {
    NO_RECORD("no-record"),
    DENIED_CONSENT("denied-consent"),
    GAVE_CONSENT("gave-consent");

    public @NonNull String getValue() {
      return this.value;
    }

    public static @Nullable IdmSessionState fromString(final @Nullable String value) {
      for (final IdmSessionState state : IdmSessionState.values()) {
        if (state.getValue().equals(value)) {
          return state;
        }
      }
      return null;
    }

    private final @NonNull String value;

    IdmSessionState(final @NonNull String value) {
      this.value = value;
    }
  }

}
