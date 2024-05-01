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

import org.opensaml.saml.saml2.core.AuthnRequest;
import org.springframework.context.ApplicationEvent;

import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.opensaml.common.utils.SerializableOpenSamlObject;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;

import java.io.Serial;

/**
 * An {@link ApplicationEvent} that is issued when the user has selected which country to authenticate at, but before
 * the authentication request is sent.
 *
 * @author Martin Lindström
 */
public class BeforeEidasAuthenticationEvent extends AbstractConnectorAuthnEvent {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The receiving county. */
  private final String country;

  /** The authentication request. */
  private final SerializableOpenSamlObject<AuthnRequest> authnRequest;

  /** The relay state. */
  private final String relayState;

  /** How the AuthnRequest was passed (GET or POST). */
  private final String method;

  /**
   * Constructor.
   *
   * @param token the {@link Saml2UserAuthenticationInputToken}
   * @param country the receiving country
   * @param authnRequest the SAML {@link AuthnRequest}
   * @param relayState the RelayState variable
   * @param method GET or POST
   */
  public BeforeEidasAuthenticationEvent(final Saml2UserAuthenticationInputToken token,
      final String country, final AuthnRequest authnRequest, final String relayState, final String method) {
    super(token);
    this.authnRequest = new SerializableOpenSamlObject<>(authnRequest);
    this.relayState = relayState;
    this.country = country;
    this.method = method;
  }

  /**
   * Gets the {@link AuthnRequest}.
   *
   * @return the {@link AuthnRequest}
   */
  public AuthnRequest getAuthnRequest() {
    return this.authnRequest.get();
  }

  /**
   * Gets the RelayState.
   *
   * @return the RelayState
   */
  public String getRelayState() {
    return this.relayState;
  }

  /**
   * Gets the country.
   *
   * @return the country code
   */
  public String getCountry() {
    return this.country;
  }

  /**
   * Tells whether redirect or POST was used.
   *
   * @return the HTTP method
   */
  public String getMethod() {
    return this.method;
  }

}
