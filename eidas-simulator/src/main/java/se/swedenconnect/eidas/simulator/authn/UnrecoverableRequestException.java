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

import java.io.Serial;

/**
 * Thrown when an authentication request cannot be answered with a response, because the connector or its assertion
 * consumer service cannot be established, or the request cannot be trusted. An error page is shown instead.
 *
 * @author Martin Lindström
 */
public class UnrecoverableRequestException extends Exception {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   *
   * @param message the error message
   */
  public UnrecoverableRequestException(final @NonNull String message) {
    super(message);
  }

  /**
   * Constructor.
   *
   * @param message the error message
   * @param cause the cause
   */
  public UnrecoverableRequestException(final @NonNull String message, final @NonNull Throwable cause) {
    super(message, cause);
  }

}
