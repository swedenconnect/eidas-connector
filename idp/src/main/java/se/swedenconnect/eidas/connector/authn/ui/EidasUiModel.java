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
package se.swedenconnect.eidas.connector.authn.ui;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model object for the eiDAS Connector "choose country" UI view.
 * 
 * @author Martin Lindström
 */
@Data
public class EidasUiModel {

  /**
   * The SP UI info.
   */
  private SpInfo spInfo;

  /**
   * Identity Matching info.
   */
  private IdmInfo idm;

  /**
   * The countries to display.
   */
  private List<UiCountry> countries;

  /**
   * The accessibility report URL.
   */
  private String accessibilityUrl;

  /**
   * Predicate that tells whether we have any "disabled" countries.
   * 
   * @return if at least one country is "disabled", i.e., can't be used for authentication, {@code true} is returned
   */
  public boolean hasDisabledCountry() {
    if (this.countries == null) {
      return false;
    }
    return this.countries.stream().anyMatch(c -> c.isDisabled());
  }

  /**
   * SP UI Info.
   */
  @Data
  public static class SpInfo {

    /**
     * The SP display name.
     */
    private String displayName;

    /**
     * The SP logotype URL.
     */
    private String logoUrl;
  }

  /**
   * UI info for Identity Matching.
   */
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class IdmInfo {

    /**
     * Is the IDM-feature active?
     */
    private boolean active;

    /**
     * The URL to the IdM service.
     */
    private String serviceUrl;

  }

}
