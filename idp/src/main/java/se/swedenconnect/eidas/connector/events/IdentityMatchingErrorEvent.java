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

import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.eidas.connector.authn.EidasAuthenticationToken;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;

import java.io.Serial;

/**
 * Event used to signal errors for communications with the Identity Matching service.
 *
 * @author Martin Lindström
 */
public class IdentityMatchingErrorEvent extends AbstractConnectorAuthnEvent {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The eIDAS authentication token. */
  private final EidasAuthenticationToken eidasToken;

  /** The error raised during the IdM operation. */
  private final Exception error;

  /**
   * Constructor.
   *
   * @param token the authentication input token
   * @param eidasToken the eIDAS authentication token
   * @param error the error raised during the IdM operation
   */
  public IdentityMatchingErrorEvent(final Saml2UserAuthenticationInputToken token,
      final EidasAuthenticationToken eidasToken, final Exception error) {
    super(token);
    this.eidasToken = eidasToken;
    this.error = error;
  }

  /**
   * Gets the eIDAS authentication token.
   *
   * @return the {@link EidasAuthenticationToken}
   */
  public EidasAuthenticationToken getEidasToken() {
    return this.eidasToken;
  }

  /**
   * Gets the error raised during the IdM operation.
   * @return an {@link Exception}
   */
  public Exception getError() {
    return this.error;
  }

}
