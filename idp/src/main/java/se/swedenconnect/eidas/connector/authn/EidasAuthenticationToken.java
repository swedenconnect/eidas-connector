/*
 * Copyright 2023 Sweden Connect
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

import java.util.Collections;
import java.util.Objects;

import org.opensaml.saml.saml2.core.AuthnRequest;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;

import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.spring.saml.idp.utils.SerializableOpenSamlObject;

/**
 * An {@link Authentication} object representing the response from the foreign country.
 *
 * @author Martin Lindström
 */
public class EidasAuthenticationToken extends AbstractAuthenticationToken {

  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The Base64 encoded response message. */
  private final String response;

  /** The relay state. */
  private final String relayState;

  /** The AuthnRequest corresponding to this response. */
  private final SerializableOpenSamlObject<AuthnRequest> authnRequest;

  /**
   * Constructor.
   *
   * @param response the Base64 encoded response message
   * @param relayState the relay state (may be {@code null})
   * @param authnRequest the {@link AuthnRequest} corresponding to this response
   */
  public EidasAuthenticationToken(final String response, final String relayState, final AuthnRequest authnRequest) {
    super(Collections.emptyList());
    this.response = Objects.requireNonNull(response, "response must not be null");
    this.relayState = relayState;
    this.authnRequest = new SerializableOpenSamlObject<AuthnRequest>(
        Objects.requireNonNull(authnRequest, "authnRequest must not be null"));
  }

  /**
   * Gets the Base64 encoded response message
   *
   * @return the response message
   */
  public String getResponse() {
    return this.response;
  }

  /**
   * Gets the relay state.
   *
   * @return the relay state, or {@code null} if not available
   */
  public String getRelayState() {
    return this.relayState;
  }

  /**
   * Gets the {@link AuthnRequest} corresponding to this response.
   *
   * @return an {@link AuthnRequest}
   */
  public AuthnRequest getAuthnRequest() {
    return this.authnRequest.get();
  }

  /** {@inheritDoc} */
  @Override
  public Object getCredentials() {
    return this.getResponse();
  }

  /** {@inheritDoc} */
  @Override
  public Object getPrincipal() {
    return "saml-response";
  }

}
