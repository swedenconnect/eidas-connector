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
package se.swedenconnect.eidas.connector.authn.ui;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;

import se.swedenconnect.eidas.connector.authn.EidasCountryHandler.SelectableCountry;
import se.swedenconnect.eidas.connector.config.UiConfigurationProperties.Language;
import se.swedenconnect.spring.saml.idp.authentication.Saml2ServiceProviderUiInfo;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;

/**
 * Factory bean for creating {@link EidasUiModel} objects.
 *
 * @author Martin Lindström
 */
public class EidasUiModelFactory {

  private final UiLanguageHandler languageHandler;

  private final MessageSource messageSource;

  private final String idmServiceUrl;

  private final String accessibilityUrl;

  public EidasUiModelFactory(final UiLanguageHandler languageHandler,
      final MessageSource messageSource, final String idmServiceUrl,
      final String accessibilityUrl) {
    this.languageHandler = languageHandler;
    this.messageSource = messageSource;
    this.idmServiceUrl = idmServiceUrl;
    this.accessibilityUrl = accessibilityUrl;
  }

  public EidasUiModel createUiModel(final Saml2UserAuthenticationInputToken token,
      final List<SelectableCountry> selectableCountries) {

    final Locale locale = LocaleContextHolder.getLocale();

    final EidasUiModel uiModel = new EidasUiModel();
    uiModel.setSpInfo(this.createSpInfo(token, locale));

    if (this.idmServiceUrl != null) {
      uiModel.setIdm(new EidasUiModel.IdmInfo(true, this.idmServiceUrl));
    }
    else {
      uiModel.setIdm(new EidasUiModel.IdmInfo(false, null));
    }

    uiModel.setCountries(this.getUiCountries(selectableCountries, locale));

    uiModel.setAccessibilityUrl(this.accessibilityUrl);

    return uiModel;
  }

  private EidasUiModel.SpInfo createSpInfo(final Saml2UserAuthenticationInputToken token, final Locale locale) {
    final Saml2ServiceProviderUiInfo spUiInfo = token.getUiInfo();
    final EidasUiModel.SpInfo model = new EidasUiModel.SpInfo();

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
      if (logo.getHeight() > 80 && logo.getHeight() < 120) {
        return true;
      }
      return false;
    };
  }

  private List<UiCountry> getUiCountries(final List<SelectableCountry> countries, final Locale locale) {

    if (countries == null || countries.isEmpty()) {
      return Collections.emptyList();
    }

    final List<UiCountry> uiCountries = new ArrayList<>();
    for (final SelectableCountry c : countries) {
      String displayName = null;
      try {
        displayName = this.messageSource.getMessage("connector.ui.country." + c.country(), null, locale);
      }
      catch (final NoSuchMessageException e) {
        // Maybe, there is no mapping for the given locale. Try the other languages ...
        for (final Language lang : this.languageHandler.getOtherLanguages()) {
          try {
            displayName =
                this.messageSource.getMessage("connector.ui.country." + c.country(), null, new Locale(lang.getTag()));
            if (displayName != null) {
              break;
            }
          }
          catch (final NoSuchMessageException e2) {
          }
        }
      }

      UiCountry uiCountry = null;
      if (displayName != null) {
        uiCountry = new UiCountry(c.country(), displayName);
      }
      else {
        // A fake country for test...
        displayName = this.messageSource.getMessage("connector.ui.country.TEST", new Object[] { c.country() },
            c.country() + "Test Country", locale);
        uiCountry = new UiCountry(c.country(), displayName, false);
      }
      uiCountry.setDisabled(!c.canAuthenticate());
      uiCountries.add(uiCountry);
    }

    final Collator collator = Collator.getInstance(locale);

    uiCountries.sort((o1, o2) -> {
      if (!o1.isRealCountry()) {
        return 1;
      }
      if (!o2.isRealCountry()) {
        return -1;
      }
      return collator.compare(o1.getName(), o2.getName());
    });

    return uiCountries;
  }

}
