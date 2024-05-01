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
package se.swedenconnect.eidas.connector.authn.ui;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.context.i18n.LocaleContextHolder;

import se.swedenconnect.eidas.connector.authn.ui.BaseUiModel.SpInfo;
import se.swedenconnect.eidas.connector.config.UiConfigurationProperties.Language;
import se.swedenconnect.spring.saml.idp.authentication.Saml2ServiceProviderUiInfo;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;

/**
 * Abstract factory bean for creating UI model objects.
 *
 * @param <T> the type of model objects created
 *
 * @author Martin Lindström
 */
public abstract class AbstractUiModelFactory<T extends BaseUiModel> {

  /** The UI language handler. */
  private final UiLanguageHandler languageHandler;

  /** The accessibility URL. */
  private final String accessibilityUrl;

  /**
   * Constructor.
   *
   * @param languageHandler the UI language handler
   * @param accessibilityUrl the accessibility URL
   */
  public AbstractUiModelFactory(final UiLanguageHandler languageHandler, final String accessibilityUrl) {
    this.languageHandler = Objects.requireNonNull(languageHandler, "languageHandler must not be null");
    this.accessibilityUrl = Objects.requireNonNull(accessibilityUrl, "accessibilityUrl must not be null");
  }

  /**
   * Initializes the model object.
   *
   * @param model the object to initialize
   * @param token the SAML input token
   */
  protected void initModel(final T model, final Saml2UserAuthenticationInputToken token) {
    model.setAccessibilityUrl(this.accessibilityUrl);
    model.setSpInfo(this.createSpInfo(token));
  }

  /**
   * Gets the {@link UiLanguageHandler}.
   *
   * @return the {@link UiLanguageHandler}
   */
  protected UiLanguageHandler getUiLanguageHandler() {
    return this.languageHandler;
  }

  private SpInfo createSpInfo(final Saml2UserAuthenticationInputToken token) {

    final Locale locale = LocaleContextHolder.getLocale();

    final Saml2ServiceProviderUiInfo spUiInfo = token.getUiInfo();
    final SpInfo model = new EidasUiModel.SpInfo();

    // First the display name ...
    String spDisplayName = spUiInfo.getDisplayName(locale.getLanguage());
    if (spDisplayName == null) {
      for (final Language lang : this.languageHandler.getOtherLanguages()) {
        spDisplayName = spUiInfo.getDisplayNames().get(lang.getTag());
        if (spDisplayName != null) {
          break;
        }
      }
    }
    model.setDisplayName(spDisplayName);

    // Now, get the logotype ...
    // Try to find something larger than 80px and less than 120px first
    //
    model.setLogoUrl(Optional.ofNullable(spUiInfo.getLogotype(findBestSize(locale.getLanguage())))
        .map(Saml2ServiceProviderUiInfo.Logotype::getUrl)
        .orElseGet(() -> Optional.ofNullable(spUiInfo.getLogotype(findBestSize()))
            .map(Saml2ServiceProviderUiInfo.Logotype::getUrl)
            .orElseGet(() -> Optional.ofNullable(spUiInfo.getLogotype((l) -> true))
                .map(Saml2ServiceProviderUiInfo.Logotype::getUrl)
                .orElse(null))));

    return model;
  }

  private static Predicate<Saml2ServiceProviderUiInfo.Logotype> findBestSize(final String languageTag) {
    return (logo) -> {
      if (logo.getLanguage() == null) {
        return false;
      }
      return findBestSize().test(logo);
    };
  }

  private static Predicate<Saml2ServiceProviderUiInfo.Logotype> findBestSize() {
    return (logo) -> {
      if (logo.getHeight() == null) {
        return false;
      }
      return logo.getHeight() > 80 && logo.getHeight() < 120;
    };
  }

}
