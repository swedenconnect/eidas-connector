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
package se.swedenconnect.eidas.connector.audit.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.eidas.connector.events.IdentityMatchingConsentEvent;

import java.io.Serial;

/**
 * Audit data for Identity Matching consent results.
 *
 * @author Martin Lindström
 */
@Setter
@Getter
public class IdmConsentAuditData extends ConnectorAuditData {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** Tells whether the user consented to releasing his/her Identity Matching record to the connector. */
  @JsonProperty("idm-consented")
  private boolean idmConsented;

  /** The user identity (personal identifier) of the user that consented/rejected the IdM query. */
  @JsonProperty("user")
  private String user;

  /** {@inheritDoc} */
  @Override
  public String getName() {
    return "idm-consent-result";
  }

  /**
   * Creates a {@link IdmConsentAuditData} given a {@link IdentityMatchingConsentEvent}.
   *
   * @param event a {@link IdentityMatchingConsentEvent}
   * @return a {@link IdmConsentAuditData}
   */
  public static IdmConsentAuditData of(final IdentityMatchingConsentEvent event) {
    final IdmConsentAuditData data = new IdmConsentAuditData();
    data.setIdmConsented(event.isConsented());
    data.setUser((String) event.getEidasToken().getPrincipal());
    return data;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return String.format("idm-consented='%s', user='%s'", this.idmConsented, this.user);
  }

}
