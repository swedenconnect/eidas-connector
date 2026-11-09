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
package se.swedenconnect.eidas.connector.authn;

import org.opensaml.saml.saml2.core.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.swedenconnect.eidas.connector.authn.metadata.EuMetadataProvider;
import se.swedenconnect.spring.authnserver.attributes.AttributeIdentifiers;
import se.swedenconnect.spring.authnserver.attributes.GenericRequestedAttribute;
import se.swedenconnect.spring.authnserver.authentication.provider.UserAuthenticationInputToken;
import se.swedenconnect.spring.authnserver.saml.error.SamlErrorStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bean assisting us in selecting countries.
 *
 * @author Martin Lindström
 */
@Service
public class EidasCountryHandler {

  /** Logger instance. */
  private static final Logger log = LoggerFactory.getLogger(EidasCountryHandler.class);

  /** Prefix URI for country representation. */
  public static final String COUNTRY_URI_PREFIX = "http://id.swedenconnect.se/eidas/1.0/proxy-service/";

  /** The metadata provider. */
  private final EuMetadataProvider euMetadataProvider;

  /**
   * Constructor.
   *
   * @param euMetadataProvider the metadata provider
   */
  public EidasCountryHandler(final EuMetadataProvider euMetadataProvider) {
    this.euMetadataProvider = euMetadataProvider;
  }

  /**
   * Represents a "selectable country" where {@code canAuthenticate} tells whether the country can authenticate the call
   * (i.e., whether the requested/supported authn context match).
   */
  public record SelectableCountry(String country, boolean canAuthenticate) {
  }

  /**
   * Represents the selectable countries and a flag {@code displaySelection}. If this is set a dialogue with all countries
   * should be displayed. Otherwise, the {@code countries} will only contain one country (known in advance by the SP)
   * and we may skip the selection UI.
   */
  public record SelectableCountries(List<SelectableCountry> countries, boolean displaySelection) {
  }

  /**
   * Given the authentication request the method calculates which countries that are possible to choose from during
   * authentication.
   *
   * @param token the input token
   * @return a {@link SelectableCountries}
   * @throws SamlErrorStatusException for SAML error responses
   */
  public SelectableCountries getSelectableCountries(final UserAuthenticationInputToken token)
      throws SamlErrorStatusException {

    // First see if the authentication request pointed out specific country/countries.
    //
    final List<String> requestedCountries = getRequestedCountries(token);

    // If countries were requested, check that they appear in the EU metadata.
    //
    if (!requestedCountries.isEmpty()) {
      log.debug("AuthnRequest contains request for country/countries: {} [{}]", requestedCountries,
          token.getLogString());
      final List<SelectableCountry> filteredRequestedCountries = requestedCountries.stream()
          .filter(c -> {
            if (!this.euMetadataProvider.contains(c, true)) {
              log.info("Requested country {} does not appear in EU metadata [{}]", c, token.getLogString());
              return false;
            }
            return true;
          })
          .map(this.euMetadataProvider::getCountry)
          .map(c -> new SelectableCountry(c.getCountryCode(),
              c.canAuthenticate(token.getAuthnRequirements().getAuthnContextRequirements())))
          .toList();

      if (filteredRequestedCountries.isEmpty()) {
        final String msg = String.format("Country/countries %s not available for authentication", requestedCountries);
        log.info("{} [{}]", msg, token.getLogString());
        throw SamlErrors.error(StatusCode.REQUESTER, StatusCode.NO_AVAILABLE_IDP, msg);
      }
      else if (filteredRequestedCountries.size() == 1 && requestedCountries.size() == 1) {
        if (!filteredRequestedCountries.getFirst().canAuthenticate()) {
          final String msg = String.format("Can not send request to %s - it does not support requested authn context",
              filteredRequestedCountries.getFirst().country());
          log.info("{} [{}]", msg, token.getLogString());
          throw SamlErrors.error(StatusCode.REQUESTER, StatusCode.NO_AVAILABLE_IDP, msg);
        }
        return new SelectableCountries(filteredRequestedCountries, false);
      }
      return new SelectableCountries(filteredRequestedCountries, true);
    }

    // Else, show all possible countries.
    return new SelectableCountries(
        this.euMetadataProvider.getCountries().stream()
            .map(c -> new SelectableCountry(c.getCountryCode(),
                c.canAuthenticate(token.getAuthnRequirements().getAuthnContextRequirements())))
            .toList(),
        true);
  }

  /**
   * Checks the AuthnRequest to see if the SP has requested a specific country.
   *
   * @param token the input
   * @return a list of country codes (possibly empty)
   */
  public static List<String> getRequestedCountries(final UserAuthenticationInputToken token) {

    // First check PrincipalSelection extension. A country given there is a requested c attribute holding values.
    //
    final List<String> principalSelection = token.getAuthnRequirements().getRequestedAttributes().stream()
        .filter(a -> Objects.equals(AttributeIdentifiers.COUNTRY, a.getIdentifier()))
        .map(GenericRequestedAttribute::getRequestedValues)
        .flatMap(List::stream)
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .toList();
    if (!principalSelection.isEmpty()) {
      return principalSelection;
    }

    // Check scoping in AuthnRequest ...
    //
    final List<String> countries = new ArrayList<>();
    for (final String providerId : token.getAuthnRequirements().getRequestedAuthnProviders()) {
      if (providerId == null) {
        continue;
      }
      if (providerId.startsWith(COUNTRY_URI_PREFIX)) {
        final String country = providerId.substring(COUNTRY_URI_PREFIX.length());
        if (StringUtils.hasText(country)) {
          countries.add(country.toUpperCase());
        }
        else {
          log.warn("Bad value for IDPEntry ({}) in AuthnRequest [{}]", providerId, token.getLogString());
        }
      }
      else {
        log.info("Unrecognized IDPEntry ({}) in AuthnRequest [{}]", providerId, token.getLogString());
      }
    }
    return countries;
  }

}
