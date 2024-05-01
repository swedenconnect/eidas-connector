/*
 * Copyright 2017-2024 Sweden Connect
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
package se.swedenconnect.eidas.connector.authn.idm;

import se.swedenconnect.eidas.connector.ApplicationVersion;

import java.io.Serial;

/**
 * Exception class for errors communicating with the Identity Matching Query API.
 *
 * @author Martin Lindström
 */
public class IdmException extends Exception {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /**
   * Constructor setting the error message.
   *
   * @param message the error message
   */
  public IdmException(final String message) {
    super(message);
  }

  /**
   * Constructor setting the error message and the cause of the error.
   *
   * @param message the error message
   * @param cause the cause of the error
   */
  public IdmException(final String message, final Throwable cause) {
    super(message, cause);
  }

}
