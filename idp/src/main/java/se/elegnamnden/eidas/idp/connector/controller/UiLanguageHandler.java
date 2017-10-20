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

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.Assert;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import se.elegnamnden.eidas.idp.connector.controller.model.UiLanguage;

/**
 * Handler for which language the UI uses based on user preferences.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class UiLanguageHandler implements InitializingBean {

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(UiLanguageHandler.class);

  /** List of the languages we support. */
  private List<UiLanguage> languages;

  /**
   * Returns a list of languages to display as selectable in the UI. The method will not include the language for the
   * currently used language.
   * 
   * @return a list of language model objects
   */
  public List<UiLanguage> getUiLanguages() {
    Locale locale = LocaleContextHolder.getLocale();

    return this.languages.stream()
      .filter(lang -> !lang.getLanguageTag().equals(locale.getLanguage()))
      .collect(Collectors.toList());
  }

  /**
   * Updates the UI language for the current user.
   * 
   * @param request
   *          the HTTP request
   * @param response
   *          the HTTP response
   * @param language
   *          the language tag
   */
  public void setUiLanguage(HttpServletRequest request, HttpServletResponse response, String language) {

    try {
      Locale locale = Locale.forLanguageTag(language);

      LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
      if (localeResolver != null) {
        localeResolver.setLocale(request, response, locale);
      }
      LocaleContextHolder.setLocale(locale);
    }
    catch (Exception e) {
      log.error("Failed to save selected UI language", e);
    }
  }

  /**
   * Assigns the supported languages.
   * 
   * @param languages
   *          list of supported languages
   */
  public void setLanguages(List<UiLanguage> languages) {
    this.languages = languages;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notEmpty(this.languages, "The property 'languages' must be assigned");
  }

}
