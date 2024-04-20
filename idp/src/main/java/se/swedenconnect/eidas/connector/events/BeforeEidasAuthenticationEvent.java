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

/**
 * An {@link ApplicationEvent} that is issued when the user has selected which country to authenticate at, but before
 * the authentication request is sent.
 *
 * @author Martin Lindström
 */
public class BeforeEidasAuthenticationEvent extends AbstractEidasConnectorEvent {

  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The receiving county. */
  private final String country;

  /** How the AuthnRequest was passed (GET or POST). */
  private final String method;

  /**
   * Constructor.
   *
   * @param country the receiving country
   * @param authnRequest the SAML {@link AuthnRequest}
   * @param method GET or POST
   */
  public BeforeEidasAuthenticationEvent(final String country, final AuthnRequest authnRequest, final String method) {
    super(new SerializableOpenSamlObject<AuthnRequest>(authnRequest));
    this.country = country;
    this.method = method;
  }

  /**
   * Gets the {@link AuthnRequest}.
   *
   * @return the {@link AuthnRequest}
   */
  @SuppressWarnings("unchecked")
  public AuthnRequest getAuthnRequest() {
    return ((SerializableOpenSamlObject<AuthnRequest>) this.getSource()).get();
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
