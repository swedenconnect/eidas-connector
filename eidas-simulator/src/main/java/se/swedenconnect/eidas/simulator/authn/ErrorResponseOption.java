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
import org.opensaml.saml.saml2.core.StatusCode;

/**
 * The error responses that a tester can send instead of authenticating. The status codes are the SAML status codes
 * that eIDAS nodes use.
 *
 * @author Martin Lindström
 */
public enum ErrorResponseOption {

  /** The user cancelled the authentication. */
  CANCEL("The user cancelled the authentication", StatusCode.RESPONDER, StatusCode.REQUEST_DENIED,
      "User cancelled the authentication"),

  /** The authentication failed. */
  AUTHN_FAILED("The authentication failed", StatusCode.RESPONDER, StatusCode.AUTHN_FAILED,
      "Authentication failed"),

  /** The user did not consent to the release of the attributes. */
  NO_CONSENT("The user did not consent to the release of the attributes", StatusCode.REQUESTER,
      StatusCode.REQUEST_DENIED, "Citizen consent not given"),

  /** The requested level of assurance cannot be delivered. */
  NO_AUTHN_CONTEXT("The requested level of assurance cannot be delivered", StatusCode.RESPONDER,
      StatusCode.NO_AUTHN_CONTEXT, "Level of assurance not supported"),

  /** The user is not known by the identity provider. */
  UNKNOWN_PRINCIPAL("The user is not known", StatusCode.RESPONDER, StatusCode.UNKNOWN_PRINCIPAL,
      "Unknown principal"),

  /** A requested attribute cannot be delivered. */
  INVALID_ATTRIBUTE("A requested attribute cannot be delivered", StatusCode.REQUESTER,
      StatusCode.INVALID_ATTR_NAME_OR_VALUE, "Mandatory attribute not available"),

  /** The request is not supported. */
  REQUEST_UNSUPPORTED("The request is not supported", StatusCode.REQUESTER, StatusCode.REQUEST_UNSUPPORTED,
      "Request not supported"),

  /** An internal error in the identity provider. */
  INTERNAL_ERROR("Internal error in the eIDAS node", StatusCode.RESPONDER, null, "Internal error");

  /** The description shown to the tester. */
  private final @NonNull String description;

  /** The top-level status code. */
  private final @NonNull String statusCode;

  /** The second-level status code. */
  private final @Nullable String subStatusCode;

  /** The default status message. */
  private final @NonNull String defaultMessage;

  /**
   * Constructor.
   *
   * @param description the description shown to the tester
   * @param statusCode the top-level status code
   * @param subStatusCode the second-level status code, may be {@code null}
   * @param defaultMessage the default status message
   */
  ErrorResponseOption(final @NonNull String description, final @NonNull String statusCode,
      final @Nullable String subStatusCode, final @NonNull String defaultMessage) {
    this.description = description;
    this.statusCode = statusCode;
    this.subStatusCode = subStatusCode;
    this.defaultMessage = defaultMessage;
  }

  /**
   * Gets the description shown to the tester.
   *
   * @return the description
   */
  public @NonNull String getDescription() {
    return this.description;
  }

  /**
   * Gets the default status message.
   *
   * @return the message
   */
  public @NonNull String getDefaultMessage() {
    return this.defaultMessage;
  }

  /**
   * Gets a text with the status codes, for display.
   *
   * @return the status codes
   */
  public @NonNull String getStatusCodesText() {
    final String top = this.statusCode.substring(this.statusCode.lastIndexOf(':') + 1);
    return this.subStatusCode != null
        ? top + " / " + this.subStatusCode.substring(this.subStatusCode.lastIndexOf(':') + 1)
        : top;
  }

  /**
   * Creates the status of the error response.
   *
   * @param message the status message, or {@code null} for the default message
   * @return the status
   */
  public @NonNull EidasStatus toStatus(final @Nullable String message) {
    return new EidasStatus(this.statusCode, this.subStatusCode,
        message != null && !message.isBlank() ? message : this.defaultMessage);
  }

}
