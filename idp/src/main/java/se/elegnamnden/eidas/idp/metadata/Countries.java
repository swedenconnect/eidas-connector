/*
 * Copyright 2017-2019 Sweden Connect
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

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A representation of a country listing from the aggregated EU metadata.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class Countries {

  /** The countries. */
  private List<CountryEntry> countries;

  /**
   * Constructor.
   * 
   * @param countries the countries read from the metadata
   */
  public Countries(List<CountryEntry> countries) {
    Assert.notNull(countries, "countries must not be null");
    this.countries = countries;
  }

  /**
   * Lists all countries that may be displayed on the connector country selection page.
   * <p>
   * If a country has its "hide-from-discovery" field set it is filtered from the resulting list 
   * <b>unless</b> it is explicitly given by the {@code requestedCountries} parameter.
   * </p>
   * 
   * @param requestedCountries a (possibly empty) list of requested countries
   * @return a list of matching countries
   */
  public List<String> getCountries(final List<String> requestedCountries) {
    if (requestedCountries.isEmpty()) {
      return this.countries.stream().filter(c -> !c.isHideFromDiscovery()).map(CountryEntry::getCode).collect(Collectors.toList());
    }
    Predicate<CountryEntry> isRequested = c -> requestedCountries.stream().filter(r -> r.equalsIgnoreCase(c.getCode())).findFirst().isPresent();
    return this.countries.stream().filter(isRequested).map(CountryEntry::getCode).collect(Collectors.toList());
  }

  /**
   * Predicate that tells if there are no countries available.
   * 
   * @return {@code true} if no countries are available and {@code false} otherwise
   */
  public boolean isEmpty() {
    return this.countries.isEmpty();
  }

  /**
   * Represents a country entry.
   */
  @Data
  @AllArgsConstructor
  public static class CountryEntry implements Comparable<CountryEntry> {
    private String code;
    private boolean hideFromDiscovery = false;
    
    @Override
    public int compareTo(CountryEntry o) {
      return this.code.compareTo(o.getCode());
    }
  }

}
