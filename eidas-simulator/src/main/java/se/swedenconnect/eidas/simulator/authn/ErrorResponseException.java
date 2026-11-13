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
import java.util.Objects;

/**
 * Thrown when an authentication request is answered with an error response. It is only used once the connector
 * and its assertion consumer service are known.
 *
 * @author Martin Lindström
 */
public class ErrorResponseException extends Exception {

  @Serial
  private static final long serialVersionUID = 1L;

  /** The status of the error response. */
  private final transient @NonNull EidasStatus status;

  /** Where the error response is sent. */
  private final transient @NonNull ResponseTarget target;

  /**
   * Constructor.
   *
   * @param status the status of the error response
   * @param target where the error response is sent
   */
  public ErrorResponseException(final @NonNull EidasStatus status, final @NonNull ResponseTarget target) {
    super(status.message());
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.target = Objects.requireNonNull(target, "target must not be null");
  }

  /**
   * Gets the status of the error response.
   *
   * @return the status
   */
  public @NonNull EidasStatus getStatus() {
    return this.status;
  }

  /**
   * Gets where the error response is sent.
   *
   * @return the response target
   */
  public @NonNull ResponseTarget getTarget() {
    return this.target;
  }

}
