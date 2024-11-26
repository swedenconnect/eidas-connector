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

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.util.WebUtils;
import se.swedenconnect.eidas.connector.authn.EidasCountryHandler.SelectableCountry;
import se.swedenconnect.eidas.connector.config.CookieGenerator;
import se.swedenconnect.eidas.connector.config.UiConfigurationProperties.Language;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Factory bean for creating {@link EidasUiModel} objects.
 *
 * @author Martin Lindström
 */
public class EidasUiModelFactory extends AbstractUiModelFactory<EidasUiModel> {

  /** The message source. */
  private final MessageSource messageSource;

  /** The IdM Service URL. */
  private final String idmServiceUrl;

  /** Cookie generator for controlling whether the IdM banner should be hidden. */
  private final CookieGenerator idmHideBannerCookieGenerator;

  /**
   * Constructor.
   *
   * @param languageHandler the UI language handler
   * @param accessibilityUrl the accessibility URL
   * @param messageSource the message source
   * @param idmServiceUrl the IdM service URL
   */
  public EidasUiModelFactory(final UiLanguageHandler languageHandler, final String accessibilityUrl,
      final MessageSource messageSource, final String idmServiceUrl,
      final CookieGenerator idmHideBannerCookieGenerator) {
    super(languageHandler, accessibilityUrl);
    this.messageSource = messageSource;
    this.idmServiceUrl = idmServiceUrl;
    this.idmHideBannerCookieGenerator = idmHideBannerCookieGenerator;
  }

  /**
   * Creates an {@link EidasUiModel}.
   *
   * @param request the HTTP servlet request
   * @param token the SAML input token
   * @param selectableCountries available countries
   * @return an {@link EidasUiModel}
   */
  public EidasUiModel createUiModel(final HttpServletRequest request,
      final Saml2UserAuthenticationInputToken token,
      final List<SelectableCountry> selectableCountries) {

    final EidasUiModel uiModel = new EidasUiModel();
    this.initModel(uiModel, token);

    final Locale locale = LocaleContextHolder.getLocale();

    if (this.idmServiceUrl != null) {
      final boolean hideBanner =
          Optional.ofNullable(WebUtils.getCookie(request, this.idmHideBannerCookieGenerator.getName()))
              .map(Cookie::getValue)
              .map(Boolean::parseBoolean)
              .orElse(false);

      uiModel.setIdm(new EidasUiModel.IdmInfo(true, !hideBanner, this.idmServiceUrl));
    }
    else {
      uiModel.setIdm(new EidasUiModel.IdmInfo(false, false, null));
    }
    uiModel.setCountries(this.getUiCountries(selectableCountries, locale));

    return uiModel;
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
        for (final Language lang : this.getUiLanguageHandler().getOtherLanguages()) {
          try {
            displayName =
                this.messageSource.getMessage("connector.ui.country." + c.country(), null, Locale.of(lang.getTag()));
            break;
          }
          catch (final NoSuchMessageException ignored) {
          }
        }
      }

      final UiCountry uiCountry;
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
