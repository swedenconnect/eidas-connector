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

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import org.jspecify.annotations.NonNull;
import org.opensaml.saml.saml2.core.StatusCode;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.spring.authnserver.oidc.error.OidcAuthenticationError;
import se.swedenconnect.spring.authnserver.saml.error.SamlAuthenticationError;
import se.swedenconnect.spring.authnserver.saml.error.SamlErrorStatus;

import java.io.Serial;
import java.util.Objects;

/**
 * The authentication errors of the connector that the built-in errors of the authentication server do not cover. Each
 * error is reported to a SAML Service Provider with its own status and to an OpenID Connect Relying Party with its own
 * error.
 * <p>
 * Most errors use the message code {@value #NO_MESSAGE_CODE}, which is never resolved. The SAML status message is then
 * the description of the {@link se.swedenconnect.spring.authnserver.error.AuthenticationErrorException}.
 * </p>
 *
 * @author Martin Lindström
 */
public final class EidasAuthenticationError implements SamlAuthenticationError, OidcAuthenticationError {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** A message code that is never resolved, so that the description of the error becomes the status message. */
  public static final @NonNull String NO_MESSAGE_CODE = "connector.error.status.description";

  /** No requested country is available for authentication. */
  public static final @NonNull EidasAuthenticationError NO_AVAILABLE_COUNTRY = new EidasAuthenticationError(
      "NO_AVAILABLE_COUNTRY", NO_MESSAGE_CODE, "No requested country is available for authentication",
      new SamlErrorStatus(StatusCode.REQUESTER, StatusCode.NO_AVAILABLE_IDP), OAuth2Error.ACCESS_DENIED);

  /** There is no metadata for the selected country. */
  public static final @NonNull EidasAuthenticationError NO_COUNTRY_METADATA = new EidasAuthenticationError(
      "NO_COUNTRY_METADATA", NO_MESSAGE_CODE, "No services available for selected country",
      new SamlErrorStatus(StatusCode.RESPONDER, StatusCode.NO_AVAILABLE_IDP), OAuth2Error.ACCESS_DENIED);

  /** The request asks for something that the connector does not support. */
  public static final @NonNull EidasAuthenticationError REQUEST_UNSUPPORTED = new EidasAuthenticationError(
      "REQUEST_UNSUPPORTED", NO_MESSAGE_CODE, "The request is not supported",
      new SamlErrorStatus(StatusCode.REQUESTER, StatusCode.REQUEST_UNSUPPORTED), OAuth2Error.INVALID_REQUEST);

  /** The sign message cannot be displayed. */
  public static final @NonNull EidasAuthenticationError SIGN_MESSAGE_NOT_SUPPORTED = new EidasAuthenticationError(
      "SIGN_MESSAGE_NOT_SUPPORTED", "authn-server.error.saml.sign-message", "The sign message cannot be displayed",
      new SamlErrorStatus(StatusCode.REQUESTER, StatusCode.REQUEST_UNSUPPORTED), OAuth2Error.INVALID_REQUEST);

  /** The request, or the response from the foreign country, is denied. */
  public static final @NonNull EidasAuthenticationError REQUEST_DENIED = new EidasAuthenticationError(
      "REQUEST_DENIED", NO_MESSAGE_CODE, "The request is denied",
      new SamlErrorStatus(StatusCode.REQUESTER, StatusCode.REQUEST_DENIED), OAuth2Error.ACCESS_DENIED);

  /** The foreign country returned an authentication context that was not requested. */
  public static final @NonNull EidasAuthenticationError INVALID_AUTHN_CONTEXT = new EidasAuthenticationError(
      "INVALID_AUTHN_CONTEXT", NO_MESSAGE_CODE, "Invalid authentication context received from foreign service",
      new SamlErrorStatus(StatusCode.RESPONDER, StatusCode.NO_AUTHN_CONTEXT), OAuth2Error.ACCESS_DENIED);

  /** The PRID of the user could not be calculated. */
  public static final @NonNull EidasAuthenticationError PRID_FAILURE = new EidasAuthenticationError(
      "PRID_FAILURE", NO_MESSAGE_CODE, "Failed to create PRID attribute",
      new SamlErrorStatus(StatusCode.RESPONDER, StatusCode.AUTHN_FAILED), OAuth2Error.ACCESS_DENIED);

  /** The user did not consent to the signature. */
  public static final @NonNull EidasAuthenticationError SIGNATURE_REJECTED = new EidasAuthenticationError(
      "SIGNATURE_REJECTED", "idp.error.status.reject-signature", "User did not consent to signature",
      new SamlErrorStatus(StatusCode.RESPONDER, SamlErrorStatus.CANCEL), OAuth2Error.ACCESS_DENIED);

  /** The name of the error, for logs. */
  private final @NonNull String name;

  /** The message code. */
  private final @NonNull String messageCode;

  /** The default message. */
  private final @NonNull String defaultMessage;

  /** The SAML status. */
  private final @NonNull SamlErrorStatus samlErrorStatus;

  /** The OpenID Connect error. */
  private final @NonNull ErrorObject oidcError;

  /**
   * Constructor.
   *
   * @param name the name of the error, for logs
   * @param messageCode the message code
   * @param defaultMessage the default message
   * @param samlErrorStatus the SAML status
   * @param oidcError the OpenID Connect error
   */
  private EidasAuthenticationError(final @NonNull String name, final @NonNull String messageCode,
      final @NonNull String defaultMessage, final @NonNull SamlErrorStatus samlErrorStatus,
      final @NonNull ErrorObject oidcError) {
    this.name = name;
    this.messageCode = messageCode;
    this.defaultMessage = defaultMessage;
    this.samlErrorStatus = samlErrorStatus;
    this.oidcError = oidcError;
  }

  /**
   * Creates the error for a failure status received from the foreign country. A SAML Service Provider gets the same
   * status, and an OpenID Connect Relying Party gets {@code access_denied}.
   *
   * @param statusCode the main status code from the foreign country
   * @param subStatusCode the subordinate status code from the foreign country
   * @return an {@link EidasAuthenticationError}
   */
  public static @NonNull EidasAuthenticationError foreignStatus(
      final @NonNull String statusCode, final @NonNull String subStatusCode) {
    return new EidasAuthenticationError("FOREIGN_STATUS", NO_MESSAGE_CODE, "Failure received from foreign service",
        new SamlErrorStatus(statusCode, subStatusCode), OAuth2Error.ACCESS_DENIED);
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull String getMessageCode() {
    return this.messageCode;
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull String getDefaultMessage() {
    return this.defaultMessage;
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull SamlErrorStatus getSamlErrorStatus() {
    return this.samlErrorStatus;
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull ErrorObject getOidcError() {
    return this.oidcError;
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final EidasAuthenticationError other)) {
      return false;
    }
    return this.name.equals(other.name) && this.messageCode.equals(other.messageCode)
        && this.samlErrorStatus.equals(other.samlErrorStatus)
        && this.oidcError.getCode().equals(other.oidcError.getCode());
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Objects.hash(this.name, this.messageCode, this.samlErrorStatus, this.oidcError.getCode());
  }

  /**
   * Returns the name and the SAML status of the error.
   */
  @Override
  public @NonNull String toString() {
    return "%s (%s / %s)".formatted(
        this.name, this.samlErrorStatus.statusCode(), this.samlErrorStatus.subStatusCode());
  }

}
