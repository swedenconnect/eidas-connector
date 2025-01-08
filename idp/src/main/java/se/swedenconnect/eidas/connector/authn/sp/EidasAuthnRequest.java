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
package se.swedenconnect.eidas.connector.authn.sp;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import org.opensaml.saml.saml2.core.AuthnRequest;

import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.opensaml.common.utils.SerializableOpenSamlObject;

/**
 * For storing authentication requests in the user session.
 *
 * @author Martin Lindström
 */
public class EidasAuthnRequest implements Serializable {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The authentication request. */
  private final SerializableOpenSamlObject<AuthnRequest> authnRequest;

  /** The relay state. */
  private final String relayState;

  /** The AuthnRequest ID of the associated request (i.e., the request from the Swedish SP). */
  private final String associatedRequestId;

  /** The country code for the recipient country. */
  private final String country;

  /**
   * Constructor.
   *
   * @param authnRequest the authentication request
   * @param relayState the relay state (may be {@code null})
   * @param associatedRequestId the AuthnRequest ID of the associated request (i.e., the request from the Swedish SP)
   * @param country the country code for the recipient country
   */
  public EidasAuthnRequest(
      final AuthnRequest authnRequest, final String relayState, final String associatedRequestId,
      final String country) {
    this.authnRequest = new SerializableOpenSamlObject<>(
        Objects.requireNonNull(authnRequest, "authnRequest must not be null"));
    this.relayState = relayState;
    this.associatedRequestId = Objects.requireNonNull(associatedRequestId, "associatedRequestId must not be null");
    this.country = Objects.requireNonNull(country, "country must not be null");
  }

  /**
   * Gets the authentication request.
   *
   * @return the authentication request
   */
  public AuthnRequest getAuthnRequest() {
    return this.authnRequest.get();
  }

  /**
   * Gets the relay state.
   *
   * @return the relay state, or {@code null} if not set
   */
  public String getRelayState() {
    return this.relayState;
  }

  /**
   * Gets the AuthnRequest ID of the associated request (i.e., the request from the Swedish SP).
   *
   * @return associated AuthnRequest ID
   */
  public String getAssociatedRequestId() {
    return this.associatedRequestId;
  }

  /**
   * Gets the country code for the recipient country
   *
   * @return country code
   */
  public String getCountry() {
    return this.country;
  }

}
