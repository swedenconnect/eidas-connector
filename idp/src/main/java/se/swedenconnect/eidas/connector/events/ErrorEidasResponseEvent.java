/*
 * Copyright 2017-2025 Sweden Connect
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
import java.util.Objects;

/**
 * Event that is signalled when an error SAML response was received from the foreign IdP.
 *
 * @author Martin Lindström
 */
public class ErrorEidasResponseEvent extends AbstractConnectorAuthnEvent {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The SAML response */
  private final SerializableOpenSamlObject<Response> response;

  /**
   * Constructor.
   *
   * @param token the authentication input token
   * @param response the SAML response
   */
  public ErrorEidasResponseEvent(final Saml2UserAuthenticationInputToken token, final Response response) {
    super(token);
    this.response = new SerializableOpenSamlObject<>(
        Objects.requireNonNull(response, "response must not be null"));
  }

  /**
   * Gets the SAML response.
   *
   * @return the {@link Response}
   */
  public Response getResponse() {
    return this.response.get();
  }

}
