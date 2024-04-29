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
package se.swedenconnect.eidas.connector.events;

import org.opensaml.saml.saml2.core.Response;

import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.opensaml.common.utils.SerializableOpenSamlObject;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;

import java.io.Serial;

/**
 * Event indicating that an error occured when processing an eIDAS response message.
 *
 * @author Martin Lindström
 */
public class ResponseProcessingErrorEvent extends AbstractConnectorAuthnEvent {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The error message. */
  private final String errorMsg;

  /** The SAML response */
  private final SerializableOpenSamlObject<Response> response;

  /**
   * Constructor.
   *
   * @param token the input token
   * @param errorMsg the error message
   */
  public ResponseProcessingErrorEvent(final Saml2UserAuthenticationInputToken token, final String errorMsg) {
    this(token, errorMsg, null);
  }

  /**
   * Constructor.
   *
   * @param token the authentication input token
   * @param errorMsg the error message
   * @param response the SAML response
   * @param assertion the SAML assertion
   */
  public ResponseProcessingErrorEvent(final Saml2UserAuthenticationInputToken token, final String errorMsg,
      final Response response) {
    super(token);
    this.errorMsg = errorMsg;
    this.response = response != null ? new SerializableOpenSamlObject<Response>(response) : null;
  }

  /**
   * Gets the error message.
   *
   * @return the error message
   */
  public String getErrorMsg() {
    return this.errorMsg;
  }

  /**
   * Gets the SAML response.
   *
   * @return the {@link Response} or {@code null} if it is not available
   */
  public Response getResponse() {
    return this.response.get();
  }

}
