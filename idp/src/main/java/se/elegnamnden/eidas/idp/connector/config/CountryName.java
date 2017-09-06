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

import java.util.Map;

import org.springframework.util.Assert;

import se.elegnamnden.eidas.idp.connector.controller.model.UiCountry;

/**
 * Model class for representing a country name in different languages.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class CountryName {

  /** The two-letter id of the country, e.g., "SE". */
  private String isoCode;

  /**
   * A mapping between language and the country name.
   */
  private Map<String, String> nameMap;

  /**
   * Constructor assigning the country ID (two letter string).
   * 
   * @param isoCode
   */
  public CountryName(String isoCode) {
    Assert.hasText(isoCode, "isoCode must not be null");
    this.isoCode = isoCode.toUpperCase();
  }

  /**
   * Returns the two letter country ID, e.g., "SE".
   * 
   * @return two letter country ID
   */
  public String getIsoCode() {
    return this.isoCode;
  }

  /**
   * Returns the mapping between languages (according to Java Locale) and country names.
   * 
   * @return mapping between languages and country names
   */
  public Map<String, String> getNameMap() {
    return this.nameMap;
  }

  /**
   * Assigns the mapping between languages (according to Java Locale) and country names.
   * 
   * @param nameMap
   *          locale to name mapping
   */
  public void setNameMap(Map<String, String> nameMap) {
    this.nameMap = nameMap;
  }

  /**
   * Returns a {@link UiCountry} instance for the given locale
   * 
   * @param locale
   *          the locale
   * @return a {@link UiCountry} instance
   */
  public UiCountry toUiCountry(String locale) {
    return new UiCountry(this.isoCode, this.nameMap.get(locale));
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    if (this.nameMap != null) {
      for (Map.Entry<String, String> e : this.nameMap.entrySet()) {
        if (sb.length() > 0) {
          sb.append(",");
          sb.append(e.getKey()).append("=").append(e.getValue());
        }
      }
    }

    return String.format("%s [%s]", this.isoCode, sb.toString());
  }

}
