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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import se.elegnamnden.eidas.idp.connector.controller.model.SpInfo;
import se.elegnamnden.eidas.idp.connector.controller.model.UiCountry;

/**
 * Handler class for taking care of the select country view.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class CountrySelectionHandler implements InitializingBean {

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(CountrySelectionHandler.class);

  /** The default locale. */
  public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

  /** The name of the cookie that holds the selected country in between sessions. */
  public static final String DEFAULT_SELECTED_COUNTRY_COOKIE_NAME = "selectedCountry";

  /** The name of the cookie that holds the selected country for the current session. */
  public static final String DEFAULT_SELECTED_SESSION_COUNTRY_COOKIE_NAME = "selectedCountrySession";

  /** The Spring message source holding localized UI message strings. */
  private MessageSource messageSource;

  /** Fallback languages to be used in the currently selected language can not be used. */
  private List<String> fallbackLanguages;

  /**
   * The name for the cookie that stores the selected country. Defaults to
   * {@value #DEFAULT_SELECTED_COUNTRY_COOKIE_NAME}.
   */
  private String selectedCountryCookieName;

  /**
   * The name for the cookie that stores the selected country for the current session. Defaults to
   * {@value #DEFAULT_SELECTED_COUNTRY_COOKIE_NAME}.
   */
  private String selectedSessionCountryCookieName;

  /**
   * Returns the selected country by inspecting the "selected country" cookies
   * 
   * @param httpRequest
   *          the HTTP request
   * @param session
   *          should the value that is stored for the session only be returned, or the persistent value?
   * @return the selected country, or {@code null} if this information is not available
   */
  public String getSelectedCountry(HttpServletRequest httpRequest, boolean session) {
    return this.getCookieValue(httpRequest, session ? this.selectedSessionCountryCookieName : this.selectedCountryCookieName);
  }

  /**
   * Saves the selected country in a user cookie.
   * 
   * @param httpResponse
   *          the HTTP response
   * @param selectedCountry
   *          the selected country
   */
  public void saveSelectedCountry(HttpServletResponse httpResponse, String selectedCountry) {
    Cookie cookie = new Cookie(this.selectedCountryCookieName, selectedCountry);
    cookie.setPath("/idp");
    cookie.setHttpOnly(true);
    cookie.setMaxAge(Integer.MAX_VALUE);
    httpResponse.addCookie(cookie);
    cookie = new Cookie(this.selectedSessionCountryCookieName, selectedCountry);
    cookie.setPath("/idp");
    cookie.setHttpOnly(true);
    cookie.setMaxAge(-1);
    httpResponse.addCookie(cookie);
  }

  /**
   * Returns the cookie value for the named cookie.
   * 
   * @param httpRequest
   *          the HTTP request
   * @param cookieName
   *          the cookie name
   * @return the cookie value, or {@code null} if the cookie is not found
   */
  private String getCookieValue(HttpServletRequest httpRequest, String cookieName) {
    Cookie[] cookies = httpRequest.getCookies();
    return cookies != null ? Arrays.asList(cookies)
      .stream()
      .filter(c -> cookieName.equals(c.getName()))
      .map(Cookie::getValue)
      .findFirst()
      .orElse(null) : null;
  }

  /**
   * Returns a {@link SpInfo} model object for the given metadata.
   * 
   * @param metadata
   *          the SP metadata
   * @return a {@code SpInfo} object
   */
  public SpInfo getSpInfo(EntityDescriptor metadata) {
    Locale locale = LocaleContextHolder.getLocale();
    return SpInfoHandler.buildSpInfo(metadata, locale.getLanguage(), this.fallbackLanguages);
  }

  /**
   * Returns a list of country objects for displaying in the list of possible countries in the UI. The list is sorted
   * according to the current locale.
   * 
   * @param isoCodes
   *          a list of language codes
   * @return a sorted list of country objects
   */
  public List<UiCountry> getSelectableCountries(List<String> isoCodes) {

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
   * {@value #DEFAULT_SELECTED_SESSION_COUNTRY_COOKIE_NAME}.
   * 
   * @param selectedCountryCookieName
   *          the cookie name
   */
  public void setSelectedCountryCookieName(String selectedCountryCookieName) {
    this.selectedCountryCookieName = selectedCountryCookieName;
  }

  /**
   * Assigns the name that should be used for the cookie that stores the selected country for the current session.
   * Defaults to {@value #DEFAULT_SELECTED_SESSION_COUNTRY_COOKIE_NAME}.
   * 
   * @param selectedSessionCountryCookieName
   *          the cookie name
   */
  public void setSelectedSessionCountryCookieName(String selectedSessionCountryCookieName) {
    this.selectedSessionCountryCookieName = selectedSessionCountryCookieName;
  }

  /**
   * Assigns the fallback languages to be used in the currently selected language can not be used.
   * 
   * @param fallbackLanguages
   *          a list of country codes
   */
  public void setFallbackLanguages(List<String> fallbackLanguages) {
    this.fallbackLanguages = fallbackLanguages;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(this.messageSource, "The property 'messageSource' must be assigned");
    Assert.notNull(this.fallbackLanguages, "The property 'fallbackLanguages' must be assigned");
    if (!StringUtils.hasText(this.selectedCountryCookieName)) {
      this.selectedCountryCookieName = DEFAULT_SELECTED_COUNTRY_COOKIE_NAME;
      log.debug("Name of cookie that holds selected country was not given - defaulting to {}", this.selectedCountryCookieName);
    }
    if (!StringUtils.hasText(this.selectedSessionCountryCookieName)) {
      this.selectedSessionCountryCookieName = DEFAULT_SELECTED_SESSION_COUNTRY_COOKIE_NAME;
      log.debug("Name of cookie that holds selected country for session was not given - defaulting to {}",
        this.selectedSessionCountryCookieName);
    }
  }

}
