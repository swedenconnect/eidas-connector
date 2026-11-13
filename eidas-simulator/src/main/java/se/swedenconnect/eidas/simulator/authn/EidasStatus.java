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
package se.swedenconnect.eidas.simulator.authn;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * The status of an error response: a top-level status code, an optional second-level status code and an optional
 * status message.
 *
 * @param statusCode the top-level status code
 * @param subStatusCode the second-level status code, or {@code null}
 * @param message the status message, or {@code null}
 * @author Martin Lindström
 */
public record EidasStatus(@NonNull String statusCode, @Nullable String subStatusCode, @Nullable String message) {

  /**
   * Constructor.
   *
   * @param statusCode the top-level status code
   * @param subStatusCode the second-level status code, or {@code null}
   * @param message the status message, or {@code null}
   */
  public EidasStatus {
    Objects.requireNonNull(statusCode, "statusCode must not be null");
  }

}
