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

import se.swedenconnect.eidas.connector.authn.EidasAuthenticationToken;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;

/**
 * Event for the Identity Matching consent.
 *
 * @author Martin Lindström
 */
public class IdentityMatchingConsentEvent extends AbstractConnectorAuthnEvent {

  /** The authentication token. */
  private final EidasAuthenticationToken eidasToken;

  /** Whether the user consented to sharing IdM record. */
  private final boolean consented;

  /**
   * Constructor.
   *
   * @param inputToken the authentication input token
   * @param eidasToken the eIDAS authentication token
   * @param consented whether the user consented to sharing IdM record
   */
  public IdentityMatchingConsentEvent(
      final Saml2UserAuthenticationInputToken inputToken, final EidasAuthenticationToken eidasToken,
      final boolean consented) {
    super(inputToken);
    this.eidasToken = eidasToken;
    this.consented = false;
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
   * Returns whether the user has consented to sharing the IdM record.
   *
   * @return {@code true} if the user has consented, {@code false} otherwise
   */
  public boolean isConsented() {
    return this.consented;
  }
}
