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

import java.util.Optional;
import java.util.function.Predicate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.CookieGenerator;
import org.springframework.web.util.WebUtils;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.eidas.connector.authn.ui.EidasUiModelFactory;
import se.swedenconnect.eidas.connector.authn.ui.UiLanguageHandler;
import se.swedenconnect.opensaml.saml2.request.RequestHttpObject;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;
import se.swedenconnect.spring.saml.idp.authentication.provider.external.AbstractAuthenticationController;
import se.swedenconnect.spring.saml.idp.authnrequest.Saml2AuthnRequestAuthenticationToken;
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
      @RequestParam(name = "language", required = false) String language) {

    try {
      final Saml2UserAuthenticationInputToken token = this.getInputToken(request).getAuthnInputToken();

      if (language != null) {
        this.uiLanguageHandler.setUiLanguage(request, response, language);
      }

      // Selected country from this session.
      //
      final String selectedCountry = Optional.ofNullable(WebUtils.getCookie(
          request, this.selectedCountrySessionCookieGenerator.getCookieName()))
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
        return this.initiateAuthentication(request, response, selectedCountry);
      }

      // Get a listing of all countries to display ...
      //
      final EidasCountryHandler.SelectableCountries selectableCountries = this.countryHandler.getSelectableCountries(token);

      if (!selectableCountries.displaySelection()) {
        // If request contained only one country, we skip the country selection ...
        return this.initiateAuthentication(request, response, selectableCountries.countries().get(0).country());
      }

      final ModelAndView modelAndView = new ModelAndView("country-select");
      modelAndView.addObject("pingFlag", this.provider.isPingRequest(token));
      modelAndView.addObject("languages", this.uiLanguageHandler.getOtherLanguages());
      modelAndView.addObject("ui", this.eidasUiModelFactory.createUiModel(token, selectableCountries.countries()));

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
          selectedCountry, this.getInputToken(httpRequest).getAuthnInputToken());

      // Save the country as "selected" ...
      //
      this.selectedCountryCookieGenerator.addCookie(httpResponse, selectedCountry);
      this.selectedCountrySessionCookieGenerator.addCookie(httpResponse, selectedCountry);

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


}
