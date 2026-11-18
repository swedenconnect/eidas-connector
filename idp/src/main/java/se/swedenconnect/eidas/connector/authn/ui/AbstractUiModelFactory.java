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
package se.swedenconnect.eidas.connector.authn.ui;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.i18n.LocaleContextHolder;
import se.swedenconnect.eidas.connector.authn.RequesterLookup;
import se.swedenconnect.eidas.connector.authn.ui.BaseUiModel.SpInfo;
import se.swedenconnect.eidas.connector.config.UiConfigurationProperties.Language;
import se.swedenconnect.spring.authnserver.authentication.provider.UserAuthenticationInputToken;
import se.swedenconnect.spring.authnserver.registry.Logo;
import se.swedenconnect.spring.authnserver.registry.RequesterRecord;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Abstract factory bean for creating UI model objects.
 *
 * @param <T> the type of model objects created
 *
 * @author Martin Lindström
 */
public abstract class AbstractUiModelFactory<T extends BaseUiModel> {

  /** The UI language handler. */
  private final @NonNull UiLanguageHandler languageHandler;

  /** The accessibility URL. */
  private final @NonNull String accessibilityUrl;

  /** For getting the display name and logotype of the requester. */
  private final @NonNull RequesterLookup requesterLookup;

  /**
   * Constructor.
   *
   * @param languageHandler the UI language handler
   * @param accessibilityUrl the accessibility URL
   * @param requesterLookup for getting the display name and logotype of the requester
   */
  public AbstractUiModelFactory(final @NonNull UiLanguageHandler languageHandler,
      final @NonNull String accessibilityUrl, final @NonNull RequesterLookup requesterLookup) {
    this.languageHandler = Objects.requireNonNull(languageHandler, "languageHandler must not be null");
    this.accessibilityUrl = Objects.requireNonNull(accessibilityUrl, "accessibilityUrl must not be null");
    this.requesterLookup = Objects.requireNonNull(requesterLookup, "requesterLookup must not be null");
  }

  /**
   * Initializes the model object.
   *
   * @param model the object to initialize
   * @param token the input token
   */
  protected void initModel(final @NonNull T model, final @NonNull UserAuthenticationInputToken token) {
    model.setAccessibilityUrl(this.accessibilityUrl);
    model.setSpInfo(this.createSpInfo(token));
  }

  /**
   * Gets the {@link UiLanguageHandler}.
   *
   * @return the {@link UiLanguageHandler}
   */
  protected @NonNull UiLanguageHandler getUiLanguageHandler() {
    return this.languageHandler;
  }

  /**
   * Creates the display name and logotype of the requester, a SAML Service Provider or an OpenID Connect Relying
   * Party, from its record in the client registry.
   *
   * @param token the input token
   * @return a {@link SpInfo}
   */
  private @NonNull SpInfo createSpInfo(final @NonNull UserAuthenticationInputToken token) {

    final SpInfo model = new SpInfo();
    final RequesterRecord record = this.requesterLookup.getRecord(token);
    if (record == null) {
      return model;
    }
    final String language = LocaleContextHolder.getLocale().getLanguage();

    // First the display name ...
    String spDisplayName = record.getDisplayName(language);
    if (spDisplayName == null) {
      for (final Language lang : this.languageHandler.getOtherLanguages()) {
        spDisplayName = record.getDisplayName(lang.getTag());
        if (spDisplayName != null) {
          break;
        }
      }
    }
    model.setDisplayName(spDisplayName);

    // Now, get the logotype ...
    // Try to find something larger than 80px and less than 120px in the page language first
    //
    model.setLogoUrl(Optional.ofNullable(findLogo(record, hasLanguage(language).and(hasBestSize())))
        .or(() -> Optional.ofNullable(findLogo(record, hasBestSize())))
        .or(() -> Optional.ofNullable(findLogo(record, l -> true)))
        .map(Logo::url)
        .orElse(null));

    return model;
  }

  /**
   * Gets the first logotype of the requester that matches the predicate.
   *
   * @param record the requester record
   * @param predicate the predicate
   * @return a {@link Logo}, or {@code null}
   */
  private static @Nullable Logo findLogo(final @NonNull RequesterRecord record, final @NonNull Predicate<Logo> predicate) {
    return record.logos().stream().filter(predicate).findFirst().orElse(null);
  }

  /**
   * Predicate telling whether a logotype is for the given language.
   *
   * @param language the language
   * @return a predicate
   */
  private static @NonNull Predicate<Logo> hasLanguage(final @NonNull String language) {
    return logo -> logo.language() != null && language.equals(logo.getPrimaryLanguage());
  }

  /**
   * Predicate telling whether a logotype is higher than 80 and lower than 120 pixels.
   *
   * @return a predicate
   */
  private static @NonNull Predicate<Logo> hasBestSize() {
    return logo -> logo.height() != null && logo.height() > 80 && logo.height() < 120;
  }

}
