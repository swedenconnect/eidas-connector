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
package se.swedenconnect.eidas.connector.config;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.util.CookieGenerator;

import se.swedenconnect.eidas.connector.authn.ui.EidasUiModelFactory;
import se.swedenconnect.eidas.connector.authn.ui.UiLanguageHandler;
import se.swedenconnect.eidas.connector.config.UiConfigurationProperties.Language;

/**
 * Web MVC configuration.
 * 
 * @author Martin Lindström
 */
@Configuration
@EnableConfigurationProperties(UiConfigurationProperties.class)
public class WebMvcConfiguration implements WebMvcConfigurer {

  /** Set cookie permanently (one year). */
  private static final int FOREVER = 60 * 60 * 24 * 365;

  /** UI settings. */
  private final UiConfigurationProperties ui;

  /** The message source. */
  private final MessageSource messageSource;

  /**
   * Constructor.
   * 
   * @param ui the UI configuration
   * @param messageSource the Spring {@link MessageSource}
   */
  public WebMvcConfiguration(final UiConfigurationProperties ui, final MessageSource messageSource) {
    this.ui = Objects.requireNonNull(ui, "ui must not be null");
    this.messageSource = Objects.requireNonNull(messageSource, "messageSource must not be null");
  }

  /**
   * Gets the {@link UiLanguageHandler}
   * 
   * @return the {@link UiLanguageHandler}
   */
  @Bean
  UiLanguageHandler uiLanguageHandler() {
    return new UiLanguageHandler(this.ui.getLanguages());
  }

  /**
   * Creates a bean holding the UI languages.
   * 
   * @return the UI languages
   */
  @Bean
  List<Language> languages() {
    return this.ui.getLanguages();
  }

  /**
   * Creates a {@link LocaleResolver} for resolving which language to use in the UI.
   * 
   * @param contextPath the servlet context path
   * @return a {@link LocaleResolver}
   */
  @Bean
  LocaleResolver localeResolver(@Value("${server.servlet.context-path}") String contextPath) {
    CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setDefaultLocale(new Locale("en"));
    resolver.setCookiePath(contextPath);
    resolver.setCookieMaxAge(31536000);
    return resolver;
  }

  /**
   * Creates a {@link LocaleChangeInterceptor} for changing the locale based on a request parameter name.
   * 
   * @return a {@link LocaleChangeInterceptor}
   */
  @Bean
  LocaleChangeInterceptor localeChangeInterceptor() {
    LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
    interceptor.setParamName("lang");
    return interceptor;
  }

  /**
   * Gets a {@link CookieGenerator} for the selected country.
   * 
   * @return {@link CookieGenerator}
   */
  @Bean("selectedCountryCookieGenerator")
  CookieGenerator selectedCountryCookieGenerator() {
    final CookieGenerator cookieGenerator = new CookieGenerator();
    cookieGenerator.setCookieName(this.ui.getSelectedCountryCookie().getName());
    if (this.ui.getSelectedCountryCookie().getDomain() != null) {
      cookieGenerator.setCookieDomain(this.ui.getSelectedCountryCookie().getDomain());
    }
    if (this.ui.getSelectedCountryCookie().getPath() != null) {
      cookieGenerator.setCookiePath(this.ui.getSelectedCountryCookie().getPath());
    }
    cookieGenerator.setCookieHttpOnly(true);
    cookieGenerator.setCookieSecure(true);
    cookieGenerator.setCookieMaxAge(FOREVER);

    return cookieGenerator;
  }

  /**
   * Gets a {@link CookieGenerator} for the selected country during session.
   * 
   * @return {@link CookieGenerator}
   */
  @Bean("selectedCountrySessionCookieGenerator")
  CookieGenerator selectedCountrySessionCookieGenerator() {
    final CookieGenerator cookieGenerator = new CookieGenerator();
    cookieGenerator.setCookieName(this.ui.getSelectedCountrySessionCookie().getName());
    if (this.ui.getSelectedCountrySessionCookie().getDomain() != null) {
      cookieGenerator.setCookieDomain(this.ui.getSelectedCountrySessionCookie().getDomain());
    }
    if (this.ui.getSelectedCountrySessionCookie().getPath() != null) {
      cookieGenerator.setCookiePath(this.ui.getSelectedCountrySessionCookie().getPath());
    }
    cookieGenerator.setCookieHttpOnly(true);
    cookieGenerator.setCookieSecure(true);
    cookieGenerator.setCookieMaxAge(-1);

    return cookieGenerator;
  }

  /**
   * Gets the {@link EidasUiModelFactory} bean
   * 
   * @param uiLanguageHandler the language handler
   * @return a {@link EidasUiModelFactory}
   */
  @Bean
  EidasUiModelFactory eidasUiModelFactory(final UiLanguageHandler uiLanguageHandler) {
    return new EidasUiModelFactory(uiLanguageHandler, this.messageSource,
        this.ui.getIdm().isActive() ? this.ui.getIdm().getServiceUrl() : null,
        this.ui.getAccessibilityUrl());
  }

  /**
   * Adds the configured {@link LocaleChangeInterceptor}.
   */
  @Override
  public void addInterceptors(final InterceptorRegistry registry) {
    registry.addInterceptor(this.localeChangeInterceptor());
  }

}
