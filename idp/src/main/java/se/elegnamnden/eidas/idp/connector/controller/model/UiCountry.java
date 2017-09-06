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
package se.elegnamnden.eidas.idp.connector.controller.model;

import se.elegnamnden.eidas.idp.connector.config.EuropeCountry;

/**
 * Represents a country when used in views.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class UiCountry implements Comparable<UiCountry> {

  private String code;
  private String name;

  public UiCountry(String code) {
    this(code, null);
  }

  public UiCountry(String code, String name) {
    this.code = code;
    this.name = name;
    if (this.name == null) {
      try {
        this.name = EuropeCountry.valueOf(code).getShortEnglishName();
      }
      catch (Exception ex) {
        this.name = code + " Test Country";
      }
    }
  }
  
  public String getCode() {
    return this.code;
  }

  public String getName() {
    return this.name;
  }

  public boolean isRealCountry() {
    for (EuropeCountry e : EuropeCountry.values()) {
      if (e.getIsoCode().equals(this.code)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int compareTo(UiCountry otherCountry) {
    return this.name.compareTo(otherCountry.name);
  }

}
