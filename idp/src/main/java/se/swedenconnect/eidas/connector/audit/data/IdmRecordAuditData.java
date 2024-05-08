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
package se.swedenconnect.eidas.connector.audit.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.eidas.connector.events.IdentityMatchingRecordEvent;

import java.io.Serial;

/**
 * Audit data for when an Identity Matching record was used.
 *
 * @author Martin Lindström
 */
@Setter
@Getter
public class IdmRecordAuditData extends ConnectorAuditData {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The user identity (eIDAS personal identifier) of the user. */
  @JsonProperty("user")
  private String user;

  /** The user identity (eIDAS personal identifier) of the user. */
  @JsonProperty("swedish-id")
  private String swedishId;

  /** The unique ID for the Identity Matching record. */
  @JsonProperty("record-id")
  private String recordId;

  /** {@inheritDoc} */
  @Override
  public String getName() {
    return "idm-record";
  }

  /**
   * Creates an {@link IdmRecordAuditData} given an {@link IdentityMatchingRecordEvent}.
   *
   * @param event the event
   * @return an {@link IdmRecordAuditData} object
   */
  public static IdmRecordAuditData of(final IdentityMatchingRecordEvent event) {
    final IdmRecordAuditData auditData = new IdmRecordAuditData();
    auditData.setUser((String) event.getEidasToken().getPrincipal());
    auditData.setSwedishId(event.getIdmRecord().getSwedishIdentity());
    auditData.setRecordId(event.getIdmRecord().getId());
    return auditData;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "user='%s', swedish-id='%s', record-id='%s'".formatted(this.user, this.swedishId, this.recordId);
  }
}
