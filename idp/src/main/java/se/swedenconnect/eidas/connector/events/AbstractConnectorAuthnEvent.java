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

import java.io.Serial;
import java.util.Objects;
import java.util.Optional;

import org.opensaml.saml.saml2.core.AuthnRequest;

import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.spring.saml.idp.audit.Saml2AuditEvent;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;
import se.swedenconnect.spring.saml.idp.authnrequest.Saml2AuthnRequestAuthenticationToken;
import se.swedenconnect.spring.saml.idp.events.AbstractSaml2IdpEvent;

/**
 * Abstract base class for eIDAS authentication events.
 *
 * @author Martin Lindström
 */
public class AbstractConnectorAuthnEvent extends AbstractSaml2IdpEvent {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /**
   * Constructor.
   *
   * @param token the authentication input token
   */
  public AbstractConnectorAuthnEvent(final Saml2UserAuthenticationInputToken token) {
    super(Objects.requireNonNull(token, "token must not be null"));
  }

  /**
   * Gets the input token.
   *
   * @return the {@link Saml2UserAuthenticationInputToken}
   */
  public final Saml2UserAuthenticationInputToken getToken() {
    return (Saml2UserAuthenticationInputToken) this.getSource();
  }

  /**
   * Gets the original SP entityID.
   *
   * @return the entityID of the Swedish SP that requested authentication
   */
  public final String getOriginalSpId() {
    return Optional.ofNullable(this.getToken().getAuthnRequestToken())
        .map(Saml2AuthnRequestAuthenticationToken::getEntityId)
        .orElseGet(() -> Saml2AuditEvent.UNKNOWN_SP);
  }

  /**
   * Gets the ID of the original {@link AuthnRequest}, i.e., the request sent by the Swedish SP.
   *
   * @return the ID of the original {@link AuthnRequest}
   */
  public final String getOriginalAuthnRequestId() {
    return Optional.ofNullable(this.getToken().getAuthnRequestToken())
        .map(Saml2AuthnRequestAuthenticationToken::getAuthnRequest)
        .map(AuthnRequest::getID)
        .orElseGet(() -> Saml2AuditEvent.UNKNOWN_AUTHN_REQUEST_ID);
  }

}
