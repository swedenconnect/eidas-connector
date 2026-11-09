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
package se.swedenconnect.eidas.connector.authn.ui;

import org.jspecify.annotations.NonNull;

/**
 * Represents a country when used in views.
 *
 * @author Martin Lindström
 */
public class UiCountry implements Comparable<UiCountry> {

  private final @NonNull String code;

  private final @NonNull String name;

  private final boolean realCountry;

  private boolean disabled = false;

  public UiCountry(final @NonNull String code, final @NonNull String name) {
    this(code, name, true);
  }

  public UiCountry(final @NonNull String code, final @NonNull String name, final boolean realCountry) {
    this.code = code;
    this.name = name;
    this.realCountry = realCountry;
  }

  /**
   * Gets the country code.
   *
   * @return the country code
   */
  public @NonNull String getCode() {
    return this.code;
  }

  /**
   * Gets the country display name.
   *
   * @return the country name
   */
  public @NonNull String getName() {
    return this.name;
  }

  /**
   * Tells whether this is a real country.
   *
   * @return {@code true} if this is a real country, and {@code false} otherwise
   */
  public boolean isRealCountry() {
    return this.realCountry;
  }

  /**
   * Tells whether the country is disabled.
   *
   * @return {@code true} if the country is disabled, and {@code false} otherwise
   */
  public boolean isDisabled() {
    return this.disabled;
  }

  /**
   * Assigns whether the country is disabled.
   *
   * @param disabled whether the country is disabled
   */
  public void setDisabled(final boolean disabled) {
    this.disabled = disabled;
  }

  public @NonNull String getFlag() {
    if (this.realCountry) {
      return this.code.toLowerCase();
    }
    else {
      return "eu";
    }
  }

  @Override
  public int compareTo(final @NonNull UiCountry otherCountry) {
    return this.name.compareTo(otherCountry.name);
  }

}
