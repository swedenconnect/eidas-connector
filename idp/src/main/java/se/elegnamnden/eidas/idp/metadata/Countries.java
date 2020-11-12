/*
 * Copyright 2017-2020 Sweden Connect
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
package se.elegnamnden.eidas.idp.metadata;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

/**
 * A representation of a country listing from the aggregated EU metadata.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class Countries {

  /** The countries. */
  private List<Country> countries;

  /**
   * Constructor.
   * 
   * @param countries
   *          the countries read from the metadata
   */
  public Countries(List<Country> countries) {
    Assert.notNull(countries, "countries must not be null");
    this.countries = countries;
  }

  /**
   * Predicate that tells whether the supplied country is among to stored countries.
   * 
   * @param country
   *          the country code to check
   * @param hiddenOk
   *          is it OK if the country is "hidden from discovery"?
   * @return true if the country is among to stored countries
   */
  public boolean contains(final String country, final boolean hiddenOk) {
    return this.countries.stream()
      .filter(c -> country.equalsIgnoreCase(c.getCountryCode()))
      .filter(c -> (hiddenOk || !c.isHideFromDiscovery()))
      .findFirst()
      .isPresent();
  }

  /**
   * Lists all countries that may be displayed on the connector country selection page.
   * <p>
   * If a country has its "hide-from-discovery" field set it is filtered from the resulting list <b>unless</b> it is
   * explicitly given by the {@code requestedCountries} parameter.
   * </p>
   * 
   * @param requestedCountries
   *          a (possibly empty) list of requested countries
   * @return a list of matching countries
   */
  public List<Country> getCountries(final List<String> requestedCountries) {
    if (requestedCountries.isEmpty()) {
      return this.countries.stream().filter(c -> !c.isHideFromDiscovery()).collect(Collectors.toList());
    }
    Predicate<Country> isRequested = c -> requestedCountries.stream()
      .filter(r -> r.equalsIgnoreCase(c.getCountryCode()))
      .findFirst()
      .isPresent();
    return this.countries.stream().filter(isRequested).collect(Collectors.toList());
  }

  /**
   * Predicate that tells if there are no countries available.
   * 
   * @return true if no countries are available and false otherwise
   */
  public boolean isEmpty() {
    return this.countries.isEmpty();
  }

}
