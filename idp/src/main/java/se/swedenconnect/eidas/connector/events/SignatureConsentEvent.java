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
 * An event that tells whether a user has consented or rejected a signature.
 *
 * @author Martin Lindström
 */
public class SignatureConsentEvent extends AbstractConnectorAuthnEvent {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The authentication token. */
  private final EidasAuthenticationToken eidasToken;

  /** Whether the signature was consented. */
  private final boolean signatureConsented;

  /**
   * Constructor.
   *
   * @param inputToken the authentication input token
   * @param eidasToken the eIDAS authentication token
   * @param signatureConsented whether the signature was consented by the user
   */
  public SignatureConsentEvent(final Saml2UserAuthenticationInputToken inputToken,
      final EidasAuthenticationToken eidasToken, final boolean signatureConsented) {
    super(inputToken);
    this.eidasToken = eidasToken;
    this.signatureConsented = signatureConsented;
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
   * Tells whether the signature was consented by the user.
   *
   * @return {@code true} if the signature was consented and {@code false} otherwise
   */
  public boolean isSignatureConsented() {
    return this.signatureConsented;
  }

}
