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
package se.swedenconnect.eidas.connector.config;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

/**
 * A singleton that can be used to find out whether the Connector is running in "development mode".
 *
 * @author Martin Lindström
 */
@Slf4j
public class DevelopmentMode {

  /** The singleton instance. */
  private static final DevelopmentMode INSTANCE = new DevelopmentMode();

  /** Whether development mode is active. */
  private Boolean active;

  /**
   * Tells whether "development mode" is active.
   *
   * @return {@code true} if development mode is active and {@code false} otherwise.
   */
  public static boolean isActive() {
    return Optional.ofNullable(INSTANCE.active).orElse(false);
  }

  /**
   * Initializes the development mode singleton.
   *
   * @param active whether development mode is active
   */
  public static void init(final boolean active) {
    if (INSTANCE.active != null) {
      throw new IllegalStateException("DevelopmentMode singleton has already been initialized");
    }
    log.info("Setting development-mode to {}", active);
    INSTANCE.active = active;
  }

  // Hidden constructor.
  private DevelopmentMode() {
  }

}
