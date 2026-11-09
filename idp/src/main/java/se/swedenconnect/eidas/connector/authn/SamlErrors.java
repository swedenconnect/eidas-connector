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
package se.swedenconnect.eidas.connector.authn;

import org.opensaml.saml.saml2.core.StatusCode;
import se.swedenconnect.spring.authnserver.saml.error.SamlErrorStatus;
import se.swedenconnect.spring.authnserver.saml.error.SamlErrorStatusException;

/**
 * Factory methods for the SAML errors that the connector reports to the Swedish Service Provider.
 * <p>
 * The status message of an error response is resolved from a message code. When the code cannot be resolved, the
 * description of the error is used as status message.
 * </p>
 *
 * @author Martin Lindström
 */
public final class SamlErrors {

  /** A message code that is never resolved, so that the description of the error becomes the status message. */
  public static final String NO_MESSAGE_CODE = "connector.error.status.description";

  /** The message code for a requested authentication context that cannot be met. */
  public static final String NO_AUTHN_CONTEXT_MESSAGE_CODE = "authn-server.error.no-authn-context";

  /** The message code for a sign message that cannot be displayed. */
  public static final String SIGN_MESSAGE_MESSAGE_CODE = "authn-server.error.saml.sign-message";

  /** The message code for a signature that the user did not consent to. */
  public static final String REJECT_SIGNATURE_MESSAGE_CODE = "idp.error.status.reject-signature";

  /** The Sweden Connect status code for a cancelled operation. */
  public static final String CANCEL_STATUS_CODE = "http://id.elegnamnden.se/status/1.0/cancel";

  /**
   * Creates an error whose status message is the description.
   *
   * @param statusCode the main status code
   * @param subStatusCode the subordinate status code
   * @param description the description, used as status message
   * @return a {@link SamlErrorStatusException}
   */
  public static SamlErrorStatusException error(
      final String statusCode, final String subStatusCode, final String description) {
    return new SamlErrorStatusException(
        new SamlErrorStatus(statusCode, subStatusCode), NO_MESSAGE_CODE, description);
  }

  /**
   * Creates an error whose status message is the description.
   *
   * @param statusCode the main status code
   * @param subStatusCode the subordinate status code
   * @param description the description, used as status message
   * @param cause the cause of the error
   * @return a {@link SamlErrorStatusException}
   */
  public static SamlErrorStatusException error(
      final String statusCode, final String subStatusCode, final String description, final Throwable cause) {
    return new SamlErrorStatusException(
        new SamlErrorStatus(statusCode, subStatusCode), NO_MESSAGE_CODE, description, cause);
  }

  /**
   * Creates an error with a status message that is resolved from a message code.
   *
   * @param statusCode the main status code
   * @param subStatusCode the subordinate status code
   * @param messageCode the message code for the status message
   * @param description the description of the error
   * @return a {@link SamlErrorStatusException}
   */
  public static SamlErrorStatusException errorWithMessageCode(final String statusCode, final String subStatusCode,
      final String messageCode, final String description) {
    return new SamlErrorStatusException(new SamlErrorStatus(statusCode, subStatusCode), messageCode, description);
  }

  /**
   * Creates the {@code Requester} / {@code NoAuthnContext} error.
   *
   * @param description the description of the error
   * @return a {@link SamlErrorStatusException}
   */
  public static SamlErrorStatusException noAuthnContext(final String description) {
    return errorWithMessageCode(StatusCode.REQUESTER, StatusCode.NO_AUTHN_CONTEXT,
        NO_AUTHN_CONTEXT_MESSAGE_CODE, description);
  }

  // Hidden constructor
  private SamlErrors() {
  }

}
