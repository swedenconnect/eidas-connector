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
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import net.shibboleth.idp.authn.ExternalAuthenticationException;
import se.elegnamnden.eidas.idp.connector.controller.model.UiCountry;
import se.elegnamnden.eidas.metadataconfig.MetadataConfig;
import se.litsec.shibboleth.idp.authn.controller.AbstractExternalAuthenticationController;

/**
 * The main controller for the Shibboleth external authentication flow implementing proxy functionality against the
 * eIDAS framework.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
@Controller
public class ProxyAuthenticationController extends AbstractExternalAuthenticationController implements InitializingBean {

  /** The name of the cookie that holds the selected country. */
  public static final String DEFAULT_SELECTED_COUNTRY_COOKIE_NAME = "selectedCountry";

  /** Symbolic name for the action parameter value of "cancel". */
  public static final String ACTION_CANCEL = "cancel";

  /** Symbolic name for the action parameter value of "authenticate". */
  public static final String ACTION_AUTHENTICATE = "authenticate";

  /** The default locale. */
  public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(ProxyAuthenticationController.class);

  /** The name of the authenticator. */
  private String authenticatorName;

  /** Configurator for eIDAS metadata. */
  private MetadataConfig metadataConfig;

  /** The Spring message source holding localized UI message strings. */
  private MessageSource messageSource;

  /**
   * The name for the cookie that stores the selected country. Defaults to
   * {@value #DEFAULT_SELECTED_COUNTRY_COOKIE_NAME}.
   */
  private String selectedCountryCookieName;

  /** {@inheritDoc} */
  @Override
  protected ModelAndView doExternalAuthentication(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String key,
      ProfileRequestContext<?, ?> profileRequestContext) throws ExternalAuthenticationException, IOException {
    
    // Selected country from earlier session.
    //
    final String selectedCountry = this.getSelectedCountry(httpRequest);
    log.debug("Selected country from previous session: {}", selectedCountry != null ? selectedCountry : "none");

    // The first step is to prompt the user for which country to direct to.
    // If the request is made by a signature service and the selected country is known, we skip
    // the "select country" view.
    //
    if (this.isSignatureServicePeer(profileRequestContext) && selectedCountry != null) {
      log.info("Request is from a signature service. Will default to previously selected country: '{}'", selectedCountry);
      return this.processAuthentication(httpRequest, httpResponse, ACTION_AUTHENTICATE, selectedCountry);
    }

    ModelAndView modelAndView = new ModelAndView("country-select");
    modelAndView.addObject("authenticationKey", key);
    modelAndView.addObject("countries", this.getSelectableCountries());

    if (selectedCountry != null) {
      modelAndView.addObject("selectedCountry", selectedCountry);
    }
    
//    LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(httpRequest);
//    localeResolver.setLocale(httpRequest, httpResponse, Locale.forLanguageTag("en"));
    
    return modelAndView;
  }

  @RequestMapping(value = "/extauth/proxyauth", method = RequestMethod.POST)
  public ModelAndView processAuthentication(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
      @RequestParam("action") String action,
      @RequestParam("selectedCountry") String selectedCountry) throws ExternalAuthenticationException, IOException {

    if (ACTION_CANCEL.equals(action)) {
      log.info("User cancelled country selection - aborting authentication");
      this.cancel(httpRequest, httpResponse);
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

  /**
   * Returns a list of country objects for displaying in the list of possible countries in the UI. The list is sorted
   * according to the current locale.
   * 
   * @return a sorted list of country objects
   */
  private List<UiCountry> getSelectableCountries() {

    // Ask the metadata configurator about which countries that have an eIDAS Proxy service registered.
    //
    List<String> isoCodes = Arrays.asList("AT", "CZ", "EE", "FR", "DE", "IS", "NL", "NO", "XX"); // this.metadataConfig.getProxyServiceCountryList();
    // Arrays.asList("AT", "CZ", "EE", "FR", "DE", "IS", "NL", "NO", "XX")
    

    // We want to avoid locale based sorting in the view, so we'll provide a sorted list based on each
    // country's localized display name.
    //
    Locale locale = LocaleContextHolder.getLocale();

    List<UiCountry> countries = new ArrayList<>();
    for (String code : isoCodes) {
      String displayName = null;
      try {
        displayName = this.messageSource.getMessage("connector.ui.country." + code.toUpperCase(), null, locale);
      }
      catch (NoSuchMessageException e) {
        // Maybe, there is no mapping for the given locale. Try the default locale also.
        if (!locale.equals(DEFAULT_LOCALE)) {
          try {
            displayName = this.messageSource.getMessage("connector.ui.country." + code.toUpperCase(), null, DEFAULT_LOCALE);
          }
          catch (NoSuchMessageException e2) {
          }
        }
      }

      if (displayName != null) {      
        countries.add(new UiCountry(code, displayName));
      }
      else {
        // A fake country for test...
        displayName = this.messageSource.getMessage("connector.ui.country.TEST", new Object[] { code }, code + "Test Country", locale);
        countries.add(new UiCountry(code, displayName, false));
      }
    }

    Collator collator = Collator.getInstance(locale);

    countries.sort(new Comparator<UiCountry>() {

      @Override
      public int compare(UiCountry o1, UiCountry o2) {
        if (!o1.isRealCountry()) {
          return 1;
        }
        if (!o2.isRealCountry()) {
          return -1;
        }
        return collator.compare(o1.getName(), o2.getName());
      }
    });

    return countries;
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
   * Assigns the configurator object holding information about the eIDAS metadata.
   * 
   * @param metadataConfig
   *          configurator object
   */
  public void setMetadataConfig(MetadataConfig metadataConfig) {
    this.metadataConfig = metadataConfig;
  }

  /**
   * Assigns the message source holding the localized UI messages used.
   * 
   * @param messageSource
   *          Spring message source
   */
  public void setMessageSource(MessageSource messageSource) {
    this.messageSource = messageSource;
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

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    super.afterPropertiesSet();
    Assert.hasText(this.authenticatorName, "The property 'authenticatorName' must be assigned");
    Assert.notNull(this.metadataConfig, "The property 'metadataConfig' must be assigned");
    Assert.notNull(this.messageSource, "The property 'messageSource' must be assigned");
    if (!StringUtils.hasText(this.selectedCountryCookieName)) {
      this.selectedCountryCookieName = DEFAULT_SELECTED_COUNTRY_COOKIE_NAME;
      log.debug("Name of cookie that holds selected country was not given - defaulting to {}", this.selectedCountryCookieName);
    }
    
    // TMP code since the metadata config is not set up as it should be
    this.metadataConfig.recache();
  }

}
