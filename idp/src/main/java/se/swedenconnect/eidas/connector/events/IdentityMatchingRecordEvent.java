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
import se.swedenconnect.eidas.connector.authn.idm.IdmRecord;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;

import java.io.Serial;

/**
 * Event that is signalled when an Identity Matching record has been obtained.
 * @author Martin Lindström
 */
public class IdentityMatchingRecordEvent extends AbstractConnectorAuthnEvent {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The eIDAS authentication token. */
  private final EidasAuthenticationToken eidasToken;

  /** The Identity Matching record obtained. */
  private final IdmRecord idmRecord;

  /**
   * Constructor.
   *
   * @param token the authentication input token
   * @param eidasToken the eIDAS authentication token
   * @param idmRecord the Identity Matching record
   */
  public IdentityMatchingRecordEvent(final Saml2UserAuthenticationInputToken token,
      final EidasAuthenticationToken eidasToken, final IdmRecord idmRecord) {
    super(token);
    this.eidasToken = eidasToken;
    this.idmRecord = idmRecord;
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
   * Returns the Identity Matching record associated with this event.
   *
   * @return the Identity Matching record
   */
  public IdmRecord getIdmRecord() {
    return this.idmRecord;
  }

}
