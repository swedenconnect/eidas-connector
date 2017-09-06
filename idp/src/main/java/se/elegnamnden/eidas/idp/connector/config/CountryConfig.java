/*
 * The eidas-connector project is the implementation of the Swedish eIDAS 
 * connector built on top of the Shibboleth IdP.
 *
 * More details on <https://github.com/elegnamnden/eidas-connector> 
 * Copyright (C) 2017 E-legitimationsnämnden
 * 
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.elegnamnden.eidas.idp.connector.config;

import java.util.List;
import java.util.stream.Collectors;

import se.elegnamnden.eidas.idp.connector.controller.model.UiCountry;

/**
 * Temporary bean for providing which countries to support.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class CountryConfig {

  private List<CountryName> countries;

  public List<CountryName> getCountries() {
    return this.countries;
  }

  public List<UiCountry> getUiContries(String locale) {
    return this.countries.stream().map(c -> c.toUiCountry(locale)).sorted().collect(Collectors.toList());
  }
  
  public void setCountries(List<CountryName> countries) {
    this.countries = countries;
  }

}
