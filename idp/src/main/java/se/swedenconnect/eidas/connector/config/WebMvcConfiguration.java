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
package se.swedenconnect.eidas.connector.config;

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
import se.swedenconnect.eidas.connector.authn.ui.EidasUiModelFactory;
import se.swedenconnect.eidas.connector.authn.ui.IdmUiModelFactory;
import se.swedenconnect.eidas.connector.authn.ui.SignUiModelFactory;
import se.swedenconnect.eidas.connector.authn.ui.UiLanguageHandler;
import se.swedenconnect.eidas.connector.config.UiConfigurationProperties.Language;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Web MVC configuration.
 *
 * @author Martin Lindström
 */
@Configuration
@EnableConfigurationProperties({ UiConfigurationProperties.class, ConnectorConfigurationProperties.class })
public class WebMvcConfiguration implements WebMvcConfigurer {

  /** UI settings. */
  private final UiConfigurationProperties ui;

  /** IdM settings. */
  private final IdmProperties idm;

  /** The message source. */
  private final MessageSource messageSource;

  /**
   * Constructor.
   *
   * @param ui the UI configuration
   * @param connector the Connector configuration
   * @param messageSource the Spring {@link MessageSource}
   */
  public WebMvcConfiguration(final UiConfigurationProperties ui,
      final ConnectorConfigurationProperties connector,
      final MessageSource messageSource) {
    this.ui = Objects.requireNonNull(ui, "ui must not be null");
    this.idm = connector.getIdm();
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
  @Bean("localeResolver")
  LocaleResolver localeResolver(@Value("${server.servlet.context-path}") final String contextPath) {
    final CookieLocaleResolver resolver = new CookieLocaleResolver();
    resolver.setDefaultLocale(new Locale("en"));
    resolver.setCookiePath(contextPath);
    resolver.setCookieMaxAge(Duration.ofDays(365));
    resolver.setCookieHttpOnly(true);
    resolver.setCookieSecure(true);
    return resolver;
  }

  /**
   * Creates a {@link LocaleChangeInterceptor} for changing the locale based on a request parameter name.
   *
   * @return a {@link LocaleChangeInterceptor}
   */
  @Bean
  LocaleChangeInterceptor localeChangeInterceptor() {
    final LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
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
    return new CookieGenerator(
        this.ui.getSelectedCountryCookie().getName(),
        this.ui.getSelectedCountryCookie().getDomain(),
        this.ui.getSelectedCountryCookie().getPath(),
        Duration.ofDays(365));
  }

  /**
   * Gets a {@link CookieGenerator} for the selected country during session.
   *
   * @return {@link CookieGenerator}
   */
  @Bean("selectedCountrySessionCookieGenerator")
  CookieGenerator selectedCountrySessionCookieGenerator() {
    return new CookieGenerator(
        this.ui.getSelectedCountrySessionCookie().getName(),
        this.ui.getSelectedCountrySessionCookie().getDomain(),
        this.ui.getSelectedCountrySessionCookie().getPath());
  }

  /**
   * Gets a {@link CookieGenerator} for the IdM consent during the session.
   *
   * @return {@link CookieGenerator}
   */
  @Bean("idmConsentSessionCookieGenerator")
  CookieGenerator idmConsentSessionCookieGenerator() {
    return new CookieGenerator(
        this.ui.getIdmConsentSessionCookie().getName(),
        this.ui.getIdmConsentSessionCookie().getDomain(),
        this.ui.getIdmConsentSessionCookie().getPath());
  }

  /**
   * Gets the {@link EidasUiModelFactory} bean
   *
   * @param uiLanguageHandler the language handler
   * @return a {@link EidasUiModelFactory}
   */
  @Bean
  EidasUiModelFactory eidasUiModelFactory(final UiLanguageHandler uiLanguageHandler) {
    return new EidasUiModelFactory(uiLanguageHandler, this.ui.getAccessibilityUrl(), this.messageSource,
        Optional.ofNullable(this.idm).map(IdmProperties::getServiceUrl).orElse(null));
  }

  /**
   * Gets the {@link SignUiModelFactory} bean.
   *
   * @param uiLanguageHandler the language handler
   * @return a {@link SignUiModelFactory}
   */
  @Bean
  SignUiModelFactory signUiModelFactory(final UiLanguageHandler uiLanguageHandler) {
    return new SignUiModelFactory(uiLanguageHandler, this.ui.getAccessibilityUrl());
  }

  /**
   * Gets the {@link IdmUiModelFactory} bean.
   *
   * @param uiLanguageHandler the language handler
   * @return an {@link IdmUiModelFactory}
   */
  @Bean
  IdmUiModelFactory idmUiModelFactory(final UiLanguageHandler uiLanguageHandler) {
    return new IdmUiModelFactory(uiLanguageHandler, this.ui.getAccessibilityUrl());
  }

  /**
   * Adds the configured {@link LocaleChangeInterceptor}.
   */
  @Override
  public void addInterceptors(final InterceptorRegistry registry) {
    registry.addInterceptor(this.localeChangeInterceptor());
  }

}
