/*
 * Copyright 2017-2025 Sweden Connect
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

import lombok.Data;

/**
 * Base UI model class.
 *
 * @author Martin Lindström
 */
@Data
public class BaseUiModel {

  /**
   * The SP UI info.
   */
  private SpInfo spInfo;

  /**
   * The accessibility report URL.
   */
  private String accessibilityUrl;

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

}
