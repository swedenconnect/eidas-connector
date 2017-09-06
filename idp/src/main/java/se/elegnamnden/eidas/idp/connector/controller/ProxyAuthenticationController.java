/*
 * The eidas-connector project is the implementation of the Swedish eIDAS 
 * connector built on top of the Shibboleth IdP.
 *
 * More details on <https://github.com/elegnamnden/eidas-connector> 
 * Copyright (C) 2017 E-legitimationsnämnden
 * 
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.elegnamnden.eidas.idp.connector.controller;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import net.shibboleth.idp.authn.ExternalAuthenticationException;
import se.elegnamnden.eidas.idp.connector.config.CountryConfig;
import se.litsec.shibboleth.idp.authn.controller.AbstractExternalAuthenticationController;

/**
 * The main controller for the Shibboleth external authentication flow implementing proxy functionality against the
 * eIDAS framework.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class ProxyAuthenticationController extends AbstractExternalAuthenticationController implements InitializingBean {

  /** The name of the cookie that holds the selected country. */
  public static final String DEFAULT_SELECTED_COUNTRY_COOKIE_NAME = "selectedCountry";

  /** Symbolic name for the action parameter value of "cancel". */
  public static final String ACTION_CANCEL = "cancel";

  /** Symbolic name for the action parameter value of "authenticate". */
  public static final String ACTION_AUTHENTICATE = "authenticate";

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(ProxyAuthenticationController.class);

  /** The name of the authenticator. */
  private String authenticatorName;

  /**
   * The name for the cookie that stores the selected country. Defaults to
   * {@value #DEFAULT_SELECTED_COUNTRY_COOKIE_NAME}.
   */
  private String selectedCountryCookieName;

  /** Configuration of countries. */
  private CountryConfig countryConfig;

  /** {@inheritDoc} */
  @Override
  protected ModelAndView doExternalAuthentication(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String key,
      ProfileRequestContext<?, ?> profileRequestContext) throws ExternalAuthenticationException, IOException {

    // Selected country from earlier session.
    //
    final String selectedCountry = this.getSelectedCountry(httpRequest);

    // The first step is to prompt the user for which country to direct to.
    // If the request is made by a signature service and the selected country is known, we skip
    // the "select country" view.
    //
    if (this.isSignatureServicePeer(profileRequestContext) && selectedCountry != null) {
      log.info("Request is from a signature service. Will default to previously selected country: '{}'", selectedCountry);
    }

    ModelAndView modelAndView = new ModelAndView("country-select");
    modelAndView.addObject("authenticationKey", key);
    modelAndView.addObject("countries", this.countryConfig.getUiContries("en"));  // TODO: get hold of the current locale
    if (selectedCountry != null) {
      modelAndView.addObject("selectedCountry", selectedCountry);
    }

    return modelAndView;
  }

  @RequestMapping(value = "/extauth/proxyauth", method = RequestMethod.POST)
  public ModelAndView processAuthentication(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
      @RequestParam("authenticationKey") String key,
      @RequestParam("action") String action,
      @RequestParam("selectedCountry") String selectedCountry) throws ExternalAuthenticationException, IOException {

    if (ACTION_CANCEL.equals(action)) {
      log.info("User cancelled country selection - aborting authentication");
      this.cancel(httpRequest, httpResponse, key);
      return null;
    }

    log.debug("User selected country '{}'", selectedCountry);
    this.saveSelectedCountry(httpResponse, selectedCountry);

    return null;
  }

  /**
   * Returns the selected country by inspecting the "selected country" cookie.
   * 
   * @param httpRequest
   *          the HTTP request
   * @return the selected country, or {@code null} if this information is not available
   */
  private String getSelectedCountry(HttpServletRequest httpRequest) {
    Cookie[] cookies = httpRequest.getCookies();
    return cookies != null ? Arrays.asList(cookies)
      .stream()
      .filter(c -> this.selectedCountryCookieName.equals(c.getName()))
      .map(c -> c.getValue())
      .findFirst()
      .orElse(null) : null;
  }

  /**
   * Saves the selected country in a user cookie.
   * 
   * @param httpResponse
   *          the HTTP response
   * @param selectedCountry
   *          the selected country
   */
  private void saveSelectedCountry(HttpServletResponse httpResponse, String selectedCountry) {
    Cookie cookie = new Cookie(this.selectedCountryCookieName, selectedCountry);
    cookie.setPath("/idp/");
    httpResponse.addCookie(cookie);
  }

  /** {@inheritDoc} */
  @Override
  public String getAuthenticatorName() {
    return this.authenticatorName;
  }

  /**
   * Assigns the name of this authenticator.
   * 
   * @param authenticatorName
   *          the name
   */
  public void setAuthenticatorName(String authenticatorName) {
    this.authenticatorName = authenticatorName;
  }

  /**
   * Assigns the name that should be used for the cookie that stores the selected country. Defaults to
   * {@value #DEFAULT_SELECTED_COUNTRY_COOKIE_NAME}.
   * 
   * @param selectedCountryCookieName
   *          the cookie name
   */
  public void setSelectedCountryCookieName(String selectedCountryCookieName) {
    this.selectedCountryCookieName = selectedCountryCookieName;
  }

  /**
   * Assigns the country configuration.
   * 
   * @param countryConfig
   *          country configuration
   */
  public void setCountryConfig(CountryConfig countryConfig) {
    this.countryConfig = countryConfig;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    super.afterPropertiesSet();
    Assert.hasText(this.authenticatorName, "The property 'authenticatorName' must be assigned");
    Assert.notNull(this.countryConfig, "The property 'countryConfig' must be assigned");
    if (!StringUtils.hasText(this.selectedCountryCookieName)) {
      this.selectedCountryCookieName = DEFAULT_SELECTED_COUNTRY_COOKIE_NAME;
      log.debug("Name of cookie that holds selected country was not given - defaulting to {}", this.selectedCountryCookieName);
    }
  }

}
