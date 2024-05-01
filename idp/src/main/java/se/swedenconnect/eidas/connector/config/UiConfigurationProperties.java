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

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for UI settings.
 *
 * @author Martin Lindström
 */
@ConfigurationProperties("ui")
public class UiConfigurationProperties implements InitializingBean {

  /** Default cookie name for the selected country cookie. */
  public static final String DEFAULT_SELECTED_COUNTRY_COOKIE_NAME = "selectedCountry";

  /** Default cookie name for the selected country session cookie. */
  public static final String DEFAULT_SELECTED_COUNTRY_SESSION_COOKIE_NAME = "selectedCountrySession";

  /**
   * The UI language settings.
   */
  @Getter
  private final List<Language> languages = new ArrayList<>();

  /**
   * The cookie for storing the selected country (in between sessions).
   */
  @NestedConfigurationProperty
  @Getter
  private final Cookie selectedCountryCookie = new Cookie();

  /**
   * The cookie for storing the selected country (during session).
   */
  @NestedConfigurationProperty
  @Getter
  private final Cookie selectedCountrySessionCookie = new Cookie();

  /**
   * The accessibility report URL.
   */
  @Getter
  @Setter
  private String accessibilityUrl;

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
    if (!StringUtils.hasText(this.selectedCountrySessionCookie.getName())) {
      this.selectedCountrySessionCookie.setName(DEFAULT_SELECTED_COUNTRY_SESSION_COOKIE_NAME);
    }
  }

  /**
   * UI language settings.
   */
  @Data
  public static class Language implements InitializingBean {

    /**
     * The language tag.
     */
    private String tag;

    /**
     * The text associated with the language tag, e.g. English.
     */
    private String text;

    /** {@inheritDoc} */
    @Override
    public void afterPropertiesSet() {
      Assert.hasText(this.tag, "Missing ui.languages[].tag");
      Assert.hasText(this.text, "Missing ui.languages[].text");
    }

  }

  /**
   * Cookie settings.
   */
  @Data
  public static class Cookie {

    /**
     * Cookie name.
     */
    private String name;

    /**
     * Cookie domain.
     */
    private String domain;

    /**
     * Cookie path.
     */
    private String path;
  }

}
