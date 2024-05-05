/*
 * Copyright 2017-2024 Sweden Connect
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
import lombok.extern.slf4j.Slf4j;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.StatusCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
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
import se.swedenconnect.eidas.connector.events.SignatureConsentEvent;
import se.swedenconnect.opensaml.saml2.request.RequestHttpObject;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;
import se.swedenconnect.spring.saml.idp.authentication.provider.external.AbstractAuthenticationController;
import se.swedenconnect.spring.saml.idp.authnrequest.Saml2AuthnRequestAuthenticationToken;
import se.swedenconnect.spring.saml.idp.error.Saml2ErrorStatusException;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * The authentication controller.
 *
 * @author Martin Lindström
 */
@Controller
@Slf4j
public class EidasAuthenticationController extends AbstractAuthenticationController<EidasAuthenticationProvider> {

  /** The path for receiving eIDAS assertions. */
  public static final String ASSERTION_CONSUMER_PATH = "/extauth/saml2/post";

  /** Symbolic name for the action parameter value of "ok". */
  public static final String ACTION_OK = "ok";

  /** Symbolic name for the action parameter value of "cancel". */
  public static final String ACTION_CANCEL = "cancel";

  /** The authentication provider. */
  private final EidasAuthenticationProvider provider;

  /** The UI language handler. */
  private final UiLanguageHandler uiLanguageHandler;

  /** Factory for UI model. */
  private final EidasUiModelFactory eidasUiModelFactory;

  /** Factory for sign consent UI model. */
  private final SignUiModelFactory signUiModelFactory;

  /** Factory for IdM consent UI model. */
  private final IdmUiModelFactory idmUiModelFactory;

  /** Cookie generator for saving selected country. */
  private final CookieGenerator selectedCountryCookieGenerator;

  /** Session cookie generator for saving selected country. */
  private final CookieGenerator selectedCountrySessionCookieGenerator;

  /** Session cookie generator for IdM consents. */
  private final CookieGenerator idmConsentCookieGenerator;

  /** For assisting us in selecting possible countries (to display). */
  private final EidasCountryHandler countryHandler;

  /** The event publisher. */
  private final ApplicationEventPublisher eventPublisher;

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
   * @param countryHandler for assisting us in selecting possible countries (to display)
   * @param eventPublisher the event publisher
   */
  public EidasAuthenticationController(final EidasAuthenticationProvider provider,
      final UiLanguageHandler uiLanguageHandler,
      final EidasUiModelFactory eidasUiModelFactory,
      final SignUiModelFactory signUiModelFactory,
      final IdmUiModelFactory idmUiModelFactory,
      @Qualifier("selectedCountryCookieGenerator") final CookieGenerator selectedCountryCookieGenerator,
      @Qualifier("selectedCountrySessionCookieGenerator") final CookieGenerator selectedCountrySessionCookieGenerator,
      @Qualifier("idmConsentSessionCookieGenerator") final CookieGenerator idmConsentCookieGenerator,
      final EidasCountryHandler countryHandler,
      final ApplicationEventPublisher eventPublisher) {
    this.provider = provider;
    this.uiLanguageHandler = uiLanguageHandler;
    this.eidasUiModelFactory = eidasUiModelFactory;
    this.signUiModelFactory = signUiModelFactory;
    this.idmUiModelFactory = idmUiModelFactory;
    this.selectedCountryCookieGenerator = selectedCountryCookieGenerator;
    this.selectedCountrySessionCookieGenerator = selectedCountrySessionCookieGenerator;
    this.idmConsentCookieGenerator = idmConsentCookieGenerator;
    this.countryHandler = countryHandler;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Tells whether the request is made from a signature service.
   */
  private static final Predicate<Saml2UserAuthenticationInputToken> isSignatureService =
      t -> Optional.ofNullable(t.getAuthnRequestToken())
          .map(Saml2AuthnRequestAuthenticationToken::isSignatureServicePeer)
          .orElse(false);

  /** {@inheritDoc} */
  @Override
  protected EidasAuthenticationProvider getProvider() {
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
  public ModelAndView authenticate(final HttpServletRequest request, final HttpServletResponse response,
      @RequestParam(name = "lang", required = false) final String language) {

    try {
      final Saml2UserAuthenticationInputToken token = this.getInputToken(request).getAuthnInputToken();

      if (language != null) {
        this.uiLanguageHandler.setUiLanguage(request, response, language);
      }
      final boolean signalEvent = language == null;

      // Selected country from this session.
      //
      final String selectedCountry = Optional.ofNullable(WebUtils.getCookie(
              request, this.selectedCountrySessionCookieGenerator.getName()))
          .map(Cookie::getValue)
          .orElse(null);
      log.debug("Selected country from this session: {}", Optional.ofNullable(selectedCountry).orElse("none"));

      // The first step is to prompt the user for which country to direct to.

      // If the request is made by a signature service and the selected country is known, we skip
      // the "select country" view.
      //
      if (isSignatureService.test(token) && selectedCountry != null) {
        log.info("Request is from a signature service. Will default to previously selected country: '{}'",
            selectedCountry);

        if (signalEvent) {
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
          this.eventPublisher.publishEvent(new BeforeCountrySelectionEvent(token,
              selectableCountries.countries().get(0).country(), NoDisplayReason.FROM_AUTHN_REQUEST));
        }

        return this.initiateAuthentication(request, response, selectableCountries.countries().get(0).country());
      }

      final ModelAndView modelAndView = new ModelAndView("country-select");
      modelAndView.addObject("pingFlag", this.provider.isPingRequest(token));
      modelAndView.addObject("languages", this.uiLanguageHandler.getOtherLanguages());
      modelAndView.addObject("ui", this.eidasUiModelFactory.createUiModel(token, selectableCountries.countries()));

      if (signalEvent) {
        this.eventPublisher.publishEvent(new BeforeCountrySelectionEvent(token, selectableCountries.countries().stream()
            .filter(SelectableCountry::canAuthenticate)
            .map(SelectableCountry::country)
            .toList()));
      }

      return modelAndView;
    }
    catch (final Saml2ErrorStatusException e) {
      return this.complete(request, e);
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
  public ModelAndView initiateAuthentication(
      final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
      @RequestParam(name = "selectedCountry") final String selectedCountry) {

    if (ACTION_CANCEL.equals(selectedCountry)) {
      log.info("User cancelled country selection - aborting authentication");
      return this.cancel(httpRequest);
    }

    try {
      log.debug("User selected country '{}'", selectedCountry);

      // Generate AuthnRequest
      //
      final RequestHttpObject<AuthnRequest> authnRequest = this.getProvider().generateAuthnRequest(
          httpRequest, selectedCountry, this.getInputToken(httpRequest).getAuthnInputToken());

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
    catch (final Saml2ErrorStatusException e) {
      return this.complete(httpRequest, e);
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
  public ModelAndView foreignResponse(
      final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
      @RequestParam(name = "SAMLResponse", required = false) final String samlResponse,
      @RequestParam(name = "RelayState", required = false) final String relayState) {

    log.debug("Received SAML response [client-ip-address='{}']", httpRequest.getRemoteAddr());

    try {
      // Process the response ...
      //
      final EidasAuthenticationToken token =
          this.getProvider().processSamlResponse(httpRequest, samlResponse, relayState);

      // OK, now we have an authentication token. Time to complete the authentication ...
      //
      return this.completeAuthentication(httpRequest, httpResponse, token, false, false);
    }
    catch (final Saml2ErrorStatusException e) {
      return this.complete(httpRequest, e);
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
  private ModelAndView completeAuthentication(final HttpServletRequest httpRequest,
      final HttpServletResponse httpResponse, final EidasAuthenticationToken token,
      final boolean idmProcessed, final boolean signConsentProcessed) {

    final Saml2UserAuthenticationInputToken inputToken = this.getInputToken(httpRequest).getAuthnInputToken();

    if (!idmProcessed) {
      if (this.getProvider().isIdmActive()) {

        // Before we check whether the user has an IdM record, we check if the IdM consent cookie has been set.
        // If so, it means that we have already checked the IdM status in this session (and possibly prompted
        // for the consent).
        // We only check the cookie if this is a signature request since the legal requirements state that the
        // user needs to be prompted for consent every time he/she authenticates.
        //
        final IdmSessionState idmState = isSignatureService.test(inputToken)
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
            return this.idmConsentResult(httpRequest, httpResponse, consent);
          }
        }

        if (this.getProvider().hasIdmRecord(token)) {
          // If the user has an IdM record, we need to ask for his or hers consent to read it ...
          //
          this.getProvider().saveEidasAuthenticationToken(httpRequest, token);
          return this.idmConsentPage(httpRequest, httpResponse, null);
        }
        // else: User does not have a record ...
      }
    }
    else {
      log.trace("IdM feature is not active - no IdM record will be queried");
    }

    if (!signConsentProcessed) {
      if (isSignatureService.test(inputToken)) {
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
    catch (final Saml2ErrorStatusException e) {
      return this.complete(httpRequest, e);
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
  public ModelAndView idmConsentPage(
      final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
      @RequestParam(name = "lang", required = false) final String language) {

    if (language != null) {
      this.uiLanguageHandler.setUiLanguage(httpRequest, httpResponse, language);
    }
    final ModelAndView modelAndView = new ModelAndView("idm-consent");
    modelAndView.addObject("languages", this.uiLanguageHandler.getOtherLanguages());
    modelAndView.addObject("idmConsent",
        this.idmUiModelFactory.createUiModel(this.getInputToken(httpRequest).getAuthnInputToken()));

    return modelAndView;
  }

  /**
   * Receives the results from the sign consent page.
   *
   * @param httpRequest the HTTP servlet request
   * @param httpResponse the HTTP servlet response
   * @param action the result (ok or cancel)
   * @return a {@link ModelAndView}
   */
  @PostMapping(EidasAuthenticationProvider.AUTHN_PATH + "/idmresult")
  public ModelAndView idmConsentResult(
      final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
      @RequestParam(name = "action") final String action) {

    final EidasAuthenticationToken token = this.getProvider().getEidasAuthenticationToken(httpRequest);

    if (ACTION_OK.equals(action)) {
      log.debug("User '{}' consented to getting IdM record [{}]", token.getPrincipal(), token.getLogString());
      this.getProvider().obtainIdMRecord(token);
    }
    else if (ACTION_CANCEL.equals(action)) {
      log.debug("User '{}' did not consent to getting IdM record [{}]", token.getPrincipal(), token.getLogString());
      // TODO: audit
    }
    else {
      log.warn("Unknown action parameter {}", action);
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
  public ModelAndView signConsentPage(
      final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
      @RequestParam(name = "lang", required = false) final String language) {

    if (language != null) {
      this.uiLanguageHandler.setUiLanguage(httpRequest, httpResponse, language);
    }

    final ModelAndView modelAndView = new ModelAndView("sign-consent");
    modelAndView.addObject("languages", this.uiLanguageHandler.getOtherLanguages());
    modelAndView.addObject("signMessageConsent", this.signUiModelFactory.createUiModel(
        this.getInputToken(httpRequest).getAuthnInputToken(),
        this.getProvider().getEidasAuthenticationToken(httpRequest)));

    return modelAndView;
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
  public ModelAndView signConsentResult(
      final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
      @RequestParam(name = "action") final String action) {

    final EidasAuthenticationToken token = this.getProvider().getEidasAuthenticationToken(httpRequest);
    final Saml2UserAuthenticationInputToken inputToken = this.getInputToken(httpRequest).getAuthnInputToken();

    if (ACTION_OK.equals(action)) {
      log.debug("User consented to signature [{}]", inputToken.getLogString());

      token.setSignatureConsented(true);
      this.eventPublisher.publishEvent(new SignatureConsentEvent(inputToken, token, true));

      return this.completeAuthentication(httpRequest, httpResponse, token, true, true);
    }
    else if (ACTION_CANCEL.equals(action)) {
      log.debug("User did not consent to signing [{}]", inputToken.getLogString());
      this.eventPublisher.publishEvent(new SignatureConsentEvent(inputToken, token, false));

      // TODO: clean up session

      final String msg = "User did not consent to signature";
      return this.complete(httpRequest,
          new Saml2ErrorStatusException(StatusCode.RESPONDER, "http://id.elegnamnden.se/status/1.0/cancel",
              "idp.error.status.reject-signature", msg, msg));
    }
    else {
      log.warn("Unknown action parameter {}", action);
      return this.signConsentPage(httpRequest, httpResponse, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected ModelAndView complete(final HttpServletRequest request, final Authentication authentication) {
    this.getProvider().removeEidasAuthenticationToken(request);
    return super.complete(request, authentication);
  }

  /** {@inheritDoc} */
  @Override
  protected ModelAndView complete(final HttpServletRequest request, final Saml2ErrorStatusException error) {
    this.getProvider().removeEidasAuthenticationToken(request);
    return super.complete(request, error);
  }

  /** {@inheritDoc} */
  @Override
  protected ModelAndView cancel(final HttpServletRequest request) {
    this.getProvider().removeEidasAuthenticationToken(request);
    return super.cancel(request);
  }

  /**
   * For storing the IdM state in a cookie.
   */
  private enum IdmSessionState {
    NO_RECORD("no-record"),
    DENIED_CONSENT("denied-consent"),
    GAVE_CONSENT("gave-consent");

    public String getValue() {
      return this.value;
    }

    public static IdmSessionState fromString(final String value) {
      for (final IdmSessionState state : IdmSessionState.values()) {
        if (state.value.equals(value)) {
          return state;
        }
      }
      return null;
    }

    private final String value;

    IdmSessionState(final String value) {
      this.value = value;
    }
  }

}
