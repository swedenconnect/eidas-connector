/*
 * Copyright 2017-2023 Sweden Connect
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

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a country when used in views.
 *
 * @author Martin Lindström
 */
public class UiCountry implements Comparable<UiCountry> {

  @Getter
  private final String code;

  @Getter
  private final String name;

  @Getter
  private boolean realCountry = true;

  @Getter
  @Setter
  private boolean disabled = false;

  public UiCountry(final String code, final String name) {
    this(code, name, true);
  }

  public UiCountry(final String code, final String name, final boolean realCountry) {
    this.code = code;
    this.name = name;
    this.realCountry = realCountry;
  }
  
  public String getFlag() {
    if (this.realCountry) {
      return this.code.toLowerCase();
    }
    else {
      return "eu";
    }
  }

  @Override
  public int compareTo(final UiCountry otherCountry) {
    return this.name.compareTo(otherCountry.name);
  }

}
