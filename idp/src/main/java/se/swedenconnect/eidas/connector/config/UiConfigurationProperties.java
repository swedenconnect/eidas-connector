/*
 * Copyright 2017-2026 Sweden Connect
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration properties for UI settings.
 *
 * @author Martin Lindström
 */
@ConfigurationProperties("ui")
public class UiConfigurationProperties implements InitializingBean {

  /** Default cookie name for the selected country cookie. */
  public static final @NonNull String DEFAULT_SELECTED_COUNTRY_COOKIE_NAME = "selectedCountry";

  /** Default cookie name for the selected country session cookie. */
  public static final @NonNull String DEFAULT_SELECTED_COUNTRY_SESSION_COOKIE_NAME = "selectedCountrySession";

  /** Default cookie name for storing the IdM consent session cookie. */
  public static final @NonNull String DEFAULT_IDM_CONSENT_COOKIE_NAME = "idmConsentSession";

  /** Default cookie name for the cookie that controls displaying of the IdM banner. */
  public static final @NonNull String DEFAULT_IDM_HIDE_BANNER_COOKIE_NAME = "idmHideBanner";

  @Value("${server.servlet.context-path:/}")
  private @Nullable String contextPath;

  @Value("${connector.domain:#{null}}")
  private @Nullable String domain;

  /**
   * The UI language settings.
   */
  private final @NonNull List<Language> languages = new ArrayList<>();

  /**
   * The cookie for storing the selected country (in between sessions).
   */
  @NestedConfigurationProperty
  private final @NonNull Cookie selectedCountryCookie = new Cookie();

  /**
   * The cookie for storing the selected country (during session).
   */
  @NestedConfigurationProperty
  private final @NonNull Cookie selectedCountrySessionCookie = new Cookie();

  /**
   * The cookie for remembering consent to access IdM record (during session).
   */
  @NestedConfigurationProperty
  private final @NonNull Cookie idmConsentSessionCookie = new Cookie();

  /**
   * The cookie that controls whether the IdM banner (at the country selection page) should be hidden.
   */
  @NestedConfigurationProperty
  private final @NonNull Cookie idmHideBannerCookie = new Cookie();

  /**
   * The accessibility report URL.
   */
  private @Nullable String accessibilityUrl;

  /**
   * Gets the UI language settings.
   *
   * @return the UI language settings
   */
  public @NonNull List<Language> getLanguages() {
    return this.languages;
  }

  /**
   * Gets the cookie for storing the selected country (in between sessions).
   *
   * @return the cookie for storing the selected country (in between sessions)
   */
  public @NonNull Cookie getSelectedCountryCookie() {
    return this.selectedCountryCookie;
  }

  /**
   * Gets the cookie for storing the selected country (during session).
   *
   * @return the cookie for storing the selected country (during session)
   */
  public @NonNull Cookie getSelectedCountrySessionCookie() {
    return this.selectedCountrySessionCookie;
  }

  /**
   * Gets the cookie for remembering consent to access IdM record (during session).
   *
   * @return the cookie for remembering consent to access IdM record (during session)
   */
  public @NonNull Cookie getIdmConsentSessionCookie() {
    return this.idmConsentSessionCookie;
  }

  /**
   * Gets the cookie that controls whether the IdM banner (at the country selection page) should be hidden.
   *
   * @return the cookie that controls whether the IdM banner (at the country selection page) should be hidden
   */
  public @NonNull Cookie getIdmHideBannerCookie() {
    return this.idmHideBannerCookie;
  }

  /**
   * Gets the accessibility report URL.
   *
   * @return the accessibility report URL
   */
  public @Nullable String getAccessibilityUrl() {
    return this.accessibilityUrl;
  }

  /**
   * Assigns the accessibility report URL.
   *
   * @param accessibilityUrl the accessibility report URL
   */
  public void setAccessibilityUrl(final @Nullable String accessibilityUrl) {
    this.accessibilityUrl = accessibilityUrl;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() {
    Assert.notEmpty(this.languages, "ui.languages must contain at least one language");
    for (final Language lang : this.languages) {
      lang.afterPropertiesSet();
    }
    if (!StringUtils.hasText(this.selectedCountryCookie.getName())) {
      this.selectedCountryCookie.setName(DEFAULT_SELECTED_COUNTRY_COOKIE_NAME);
    }
    this.checkPath(this.selectedCountryCookie);
    this.checkDomain(this.selectedCountryCookie);

    if (!StringUtils.hasText(this.selectedCountrySessionCookie.getName())) {
      this.selectedCountrySessionCookie.setName(DEFAULT_SELECTED_COUNTRY_SESSION_COOKIE_NAME);
    }
    this.checkPath(this.selectedCountrySessionCookie);
    this.checkDomain(this.selectedCountrySessionCookie);

    if (!StringUtils.hasText(this.idmConsentSessionCookie.getName())) {
      this.idmConsentSessionCookie.setName(DEFAULT_IDM_CONSENT_COOKIE_NAME);
    }
    this.checkPath(this.idmConsentSessionCookie);
    this.checkDomain(this.idmConsentSessionCookie);

    if (!StringUtils.hasText(this.idmHideBannerCookie.getName())) {
      this.idmHideBannerCookie.setName(DEFAULT_IDM_HIDE_BANNER_COOKIE_NAME);
    }
    this.checkPath(this.idmHideBannerCookie);
    this.checkDomain(this.idmHideBannerCookie);
  }

  private void checkPath(final Cookie cookie) {
    if (!StringUtils.hasText(cookie.getPath())) {
      cookie.setPath(this.contextPath);
    }
  }

  private void checkDomain(final Cookie cookie) {
    if (!StringUtils.hasText(cookie.getDomain())) {
      cookie.setDomain(this.domain);
    }
  }

  /**
   * UI language settings.
   */
  public static class Language implements InitializingBean {

    /**
     * The language tag.
     */
    private @Nullable String tag;

    /**
     * The text associated with the language tag, e.g. English.
     */
    private @Nullable String text;

    /**
     * Gets the language tag.
     *
     * @return the language tag
     */
    public @Nullable String getTag() {
      return this.tag;
    }

    /**
     * Assigns the language tag.
     *
     * @param tag the language tag
     */
    public void setTag(final @Nullable String tag) {
      this.tag = tag;
    }

    /**
     * Gets the text associated with the language tag.
     *
     * @return the text associated with the language tag
     */
    public @Nullable String getText() {
      return this.text;
    }

    /**
     * Assigns the text associated with the language tag.
     *
     * @param text the text associated with the language tag
     */
    public void setText(final @Nullable String text) {
      this.text = text;
    }

    /** {@inheritDoc} */
    @Override
    public void afterPropertiesSet() {
      Assert.hasText(this.tag, "Missing ui.languages[].tag");
      Assert.hasText(this.text, "Missing ui.languages[].text");
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final @Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || this.getClass() != o.getClass()) {
        return false;
      }
      final Language other = (Language) o;
      return Objects.equals(this.tag, other.tag) && Objects.equals(this.text, other.text);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return Objects.hash(this.tag, this.text);
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull String toString() {
      return "tag=" + this.tag + ", text=" + this.text;
    }

  }

  /**
   * Cookie settings.
   */
  public static class Cookie {

    /**
     * Cookie name.
     */
    private @Nullable String name;

    /**
     * Cookie domain.
     */
    private @Nullable String domain;

    /**
     * Cookie path.
     */
    private @Nullable String path;

    /**
     * Gets the cookie name.
     *
     * @return the cookie name
     */
    public @Nullable String getName() {
      return this.name;
    }

    /**
     * Assigns the cookie name.
     *
     * @param name the cookie name
     */
    public void setName(final @Nullable String name) {
      this.name = name;
    }

    /**
     * Gets the cookie domain.
     *
     * @return the cookie domain
     */
    public @Nullable String getDomain() {
      return this.domain;
    }

    /**
     * Assigns the cookie domain.
     *
     * @param domain the cookie domain
     */
    public void setDomain(final @Nullable String domain) {
      this.domain = domain;
    }

    /**
     * Gets the cookie path.
     *
     * @return the cookie path
     */
    public @Nullable String getPath() {
      return this.path;
    }

    /**
     * Assigns the cookie path.
     *
     * @param path the cookie path
     */
    public void setPath(final @Nullable String path) {
      this.path = path;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final @Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || this.getClass() != o.getClass()) {
        return false;
      }
      final Cookie other = (Cookie) o;
      return Objects.equals(this.name, other.name)
          && Objects.equals(this.domain, other.domain)
          && Objects.equals(this.path, other.path);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return Objects.hash(this.name, this.domain, this.path);
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull String toString() {
      return "name=" + this.name
          + ", domain=" + this.domain
          + ", path=" + this.path;
    }
  }

}
