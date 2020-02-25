/*
 * Copyright 2017-2020 Sweden Connect
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
package se.elegnamnden.eidas.idp.connector.sp;

/**
 * Exception class for the SAML response processor.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class ResponseProcessingException extends Exception {

  /** For serializing. */
  private static final long serialVersionUID = 6421954607769255094L;

  /** The ID for the response that was processed. */
  private final String responseID;

  /**
   * Constructor.
   * 
   * @param responseID
   *          the ID for the response that was processed
   * @param message
   *          the error message
   */
  public ResponseProcessingException(final String responseID, final String message) {
    super(message);
    this.responseID = responseID;
  }

  /**
   * Constructor.
   * 
   * @param responseID
   *          the ID for the response that was processed
   * @param message
   *          the error message
   * @param cause
   *          the cause of the error
   */
  public ResponseProcessingException(final String responseID, final String message, final Throwable cause) {
    super(message, cause);
    this.responseID = responseID;
  }

  /**
   * Gets the response ID.
   * 
   * @return the response ID
   */
  public String getResponseID() {
    return this.responseID;
  }

}
