/*
 * Copyright 2017-2025 Sweden Connect
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
package se.swedenconnect.eidas.connector.authn.ui;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.eidas.connector.config.UiConfigurationProperties.Language;

/**
 * Handler for which language the UI uses based on user preferences.
 *
 * @author Martin Lindström
 */
@Slf4j
public class UiLanguageHandler {

  /** Supported languages. */
  private final List<Language> languages;

  /**
   * Constructor.
   *
   * @param languages the supported languages
   */
  public UiLanguageHandler(final List<Language> languages) {
    this.languages = Optional.ofNullable(languages)
        .filter(l -> !l.isEmpty())
        .orElseThrow(() -> new IllegalArgumentException("languages must not be null or empty"));
  }

  /**
   * Returns a list of languages to display as selectable in the UI. The method will not include the language for the
   * currently used language.
   *
   * @return a list of language model objects
   */
  public List<Language> getOtherLanguages() {
    final Locale locale = LocaleContextHolder.getLocale();

    return this.languages.stream()
        .filter(lang -> !lang.getTag().equals(locale.getLanguage()))
        .collect(Collectors.toList());
  }

  /**
   * Updates the UI language for the current user/session.
   *
   * @param request the HTTP request
   * @param response the HTTP response
   * @param language the language tag
   */
  public void setUiLanguage(
      final HttpServletRequest request, final HttpServletResponse response, final String language) {

    try {
      final Locale locale = Locale.forLanguageTag(language);

      final LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
      if (localeResolver != null) {
        localeResolver.setLocale(request, response, locale);
      }
      LocaleContextHolder.setLocale(locale);
    }
    catch (final Exception e) {
      log.error("Failed to save selected UI language", e);
    }
  }

}
