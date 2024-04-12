/*
 * Copyright 2023-2024 Sweden Connect
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
 * Base UI model class.
 *
 * @author Martin Lindström
 */
public class BaseUiModel {

  /**
   * The SP UI info.
   */
  @Getter
  @Setter
  private SpInfo spInfo;

  /**
   * The accessibility report URL.
   */
  @Getter
  @Setter
  private String accessibilityUrl;

  /**
   * SP UI Info.
   */
  public static class SpInfo {

    /**
     * The SP display name.
     */
    @Getter
    @Setter
    private String displayName;

    /**
     * The SP logotype URL.
     */
    @Getter
    @Setter
    private String logoUrl;
  }

}
