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
package se.elegnamnden.eidas.idp.connector.controller.model;

/**
 * Represents a country when used in views.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class UiCountry implements Comparable<UiCountry> {

  private String code;
  private String name;
  private boolean realCountry = true;
  private boolean inactive = false;

  public UiCountry(String code, String name) {
    this(code, name, true);
  }
  
  public UiCountry(String code, String name, boolean realCountry) {
    this.code = code;
    this.name = name;
    this.realCountry = realCountry;
  }
  
  public String getCode() {
    return this.code;
  }

  public String getName() {
    return this.name;
  }
  
  public boolean isRealCountry() {
    return this.realCountry;
  }

  public boolean isInactive() {
    return this.inactive;
  }

  public void setInactive(boolean inactive) {
    this.inactive = inactive;
  }

  @Override
  public int compareTo(UiCountry otherCountry) {
    return this.name.compareTo(otherCountry.name);
  }

}
