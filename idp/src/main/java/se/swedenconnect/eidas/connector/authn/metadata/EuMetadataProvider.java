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
package se.swedenconnect.eidas.connector.authn.metadata;

import java.util.Collections;
import java.util.List;

import se.swedenconnect.opensaml.saml2.metadata.provider.MetadataProvider;

/**
 * Interface for working with EU metadata.
 *
 * @author Martin Lindström
 */
public interface EuMetadataProvider {

  /**
   * Gets the underlying metadata provider.
   *
   * @return a {@link MetadataProvider}
   */
  MetadataProvider getProvider();

  /**
   * Gets the metadata for a given country.
   *
   * @param countryCode the country code
   * @return the {@link CountryMetadata} of {@code null} if it does not appear in the metadata
   */
  CountryMetadata getCountry(final String countryCode);

  /**
   * Predicate that tells whether the supplied country is among to stored countries.
   *
   * @param countryCode the country code to check
   * @param hiddenOk is it OK if the country is "hidden from discovery"?
   * @return true if the country is among to stored countries
   */
  boolean contains(final String countryCode, final boolean hiddenOk);

  /**
   * Lists all countries that may be displayed on the connector country selection page.
   * <p>
   * If a country has its "hide-from-discovery" field set it is filtered from the resulting list.
   * </p>
   *
   * @return a list of countries
   */
  default List<CountryMetadata> getCountries() {
    return this.getCountries(Collections.emptyList());
  }

  /**
   * Lists all countries that may be displayed on the connector country selection page.
   * <p>
   * If a country has its "hide-from-discovery" field set it is filtered from the resulting list <b>unless</b> it is
   * explicitly given by the {@code requestedCountries} parameter.
   * </p>
   *
   * @param requestedCountries a (possibly empty) list of requested countries
   * @return a list of matching countries
   */
  List<CountryMetadata> getCountries(final List<String> requestedCountries);

}
