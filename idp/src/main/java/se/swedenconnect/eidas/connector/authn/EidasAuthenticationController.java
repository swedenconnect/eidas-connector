/*
 * Copyright 2023-2024 Sweden Connect
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

import java.util.Optional;
import java.util.function.Predicate;

import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.eidas.connector.authn.EidasCountryHandler.SelectableCountry;
import se.swedenconnect.eidas.connector.authn.ui.EidasUiModelFactory;
import se.swedenconnect.eidas.connector.authn.ui.SignUiModelFactory;
import se.swedenconnect.eidas.connector.authn.ui.UiLanguageHandler;
import se.swedenconnect.eidas.connector.config.CookieGenerator;
import se.swedenconnect.eidas.connector.events.BeforeCountrySelectionEvent;
import se.swedenconnect.eidas.connector.events.BeforeCountrySelectionEvent.NoDisplayReason;
import se.swedenconnect.opensaml.saml2.request.RequestHttpObject;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;
import se.swedenconnect.spring.saml.idp.authentication.provider.external.AbstractAuthenticationController;
import se.swedenconnect.spring.saml.idp.authnrequest.Saml2AuthnRequestAuthenticationToken;
import se.swedenconnect.spring.saml.idp.error.Saml2ErrorStatus;
import se.swedenconnect.spring.saml.idp.error.Saml2ErrorStatusException;

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

  /** Symbolic name for the action parameter value of "cancel". */
  public static final String ACTION_CANCEL = "cancel";

  /** The authentication provider. */
  @Autowired
  @Setter
  private EidasAuthenticationProvider provider;

  /** The UI language handler. */
  @Autowired
  @Setter
  private UiLanguageHandler uiLanguageHandler;

  /** Factory for UI model. */
  @Autowired
  @Setter
  private EidasUiModelFactory eidasUiModelFactory;

  /** Factory for sign consent UI model. */
  @Autowired
  @Setter
  private SignUiModelFactory signUiModelFactory;

  /** Cookie generator for saving selected country. */
  @Autowired
  @Qualifier("selectedCountryCookieGenerator")
  @Setter
  private CookieGenerator selectedCountryCookieGenerator;

  /** Session cookie generator for saving selected country. */
  @Autowired
  @Qualifier("selectedCountrySessionCookieGenerator")
  @Setter
  private CookieGenerator selectedCountrySessionCookieGenerator;

  /** For assisting us in selecting possible countries (to display). */
  @Autowired
  @Setter
  private EidasCountryHandler countryHandler;

  /** The event publisher. */
  @Autowired
  @Setter
  private ApplicationEventPublisher eventPublisher;

  /**
   * Tells whether the request is made from a signature service.
   */
  private static Predicate<Saml2UserAuthenticationInputToken> isSignatureService =
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
      @RequestParam(name = "lang", required = false) String language) {

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
      log.debug("Selected country from this session: {}", Optional.ofNullable(selectedCountry).orElseGet(() -> "none"));

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
      RequestHttpObject<AuthnRequest> authnRequest = this.getProvider().generateAuthnRequest(
          httpRequest, selectedCountry, this.getInputToken(httpRequest).getAuthnInputToken());

      // Save the country as "selected" ...
      //
      this.selectedCountryCookieGenerator.addCookie(selectedCountry, httpResponse);
      this.selectedCountrySessionCookieGenerator.addCookie(selectedCountry, httpResponse);

      // POST or redirect ...
      //
      if (SAMLConstants.POST_METHOD.equals(authnRequest.getMethod())) {
        ModelAndView modelAndView = new ModelAndView("post-request");
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

      // TODO: IDM

      // Is this a sign request? If so, display the sign consent page ...
      //
      final Saml2UserAuthenticationInputToken inputToken = this.getInputToken(httpRequest).getAuthnInputToken();
      if (inputToken.getAuthnRequestToken().isSignatureServicePeer()) {
        this.getProvider().saveEidasAuthenticationToken(httpRequest, token);
        return this.signConsentPage(httpRequest, httpResponse, null);
      }
      else {
        return this.complete(httpRequest, token);
      }
    }
    catch (final Saml2ErrorStatusException e) {
      return this.complete(httpRequest, e);
    }
  }

  @GetMapping(EidasAuthenticationProvider.AUTHN_PATH + "/consent")
  public ModelAndView signConsentPage(
      final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
      @RequestParam(name = "lang", required = false) String language) {

    if (language != null) {
      this.uiLanguageHandler.setUiLanguage(httpRequest, httpResponse, language);
    }

    final ModelAndView modelAndView = new ModelAndView("sign-consent");
    modelAndView.addObject("languages", this.uiLanguageHandler.getOtherLanguages());
    modelAndView.addObject("signMessageConsent", this.signUiModelFactory.createUiModel(
        this.getInputToken(httpRequest).getAuthnInputToken(),
        this.getProvider().getEidasAuthenticationToken(httpRequest, false)));

    return modelAndView;
  }

  @PostMapping(EidasAuthenticationProvider.AUTHN_PATH + "/signed")
  public ModelAndView signConsentResult(
      final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
      @RequestParam(name = "action") String action) {

    if ("ok".equals(action)) {
      final EidasAuthenticationToken token = this.getProvider().getEidasAuthenticationToken(httpRequest, true);
      token.setSignatureConsented(true);
      return this.complete(httpRequest, token);
    }
    else if ("cancel".equals(action)) {
      return this.complete(httpRequest,
          new Saml2ErrorStatusException(Saml2ErrorStatus.CANCEL, "User did not consent to signature"));
    }
    else {
      log.warn("Unknown action parameter {}", action);
      return this.signConsentPage(httpRequest, httpResponse, null);
    }
  }

  @PostMapping(value = EidasAuthenticationProvider.AUTHN_PATH + "/complete")
  public ModelAndView completeAuthentication(
      final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) {

    return null;
  }

}
