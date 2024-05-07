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
package se.swedenconnect.eidas.connector.prid.generator;

import java.io.Serial;

/**
 * Exception class for PRID generation errors.
 */
public class PridGeneratorException extends Exception {

  /** For serializing. */
  @Serial
  private static final long serialVersionUID = 8619422156944522349L;

  /**
   * Constructor taking the error message as a parameter.
   *
   * @param message the error message
   */
  public PridGeneratorException(final String message) {
    super(message);
  }

  /**
   * Constructor taking the error message and the cause as parameters.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public PridGeneratorException(final String message, final Throwable cause) {
    super(message, cause);
  }

}
