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
package se.swedenconnect.eidas.connector.audit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.spring.saml.idp.audit.Saml2AuditEvent;
import se.swedenconnect.spring.saml.idp.audit.data.Saml2AuditData;

import java.io.Serial;
import java.util.Optional;

/**
 * Audit event for events that concern user authentication.
 *
 * @author Martin Lindström
 */
@JsonInclude(Include.NON_EMPTY)
public class ConnectorAuthnAuditEvent extends ConnectorAuditEvent {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /**
   * Constructor.
   *
   * @param type the type of audit event
   * @param timestamp the timestamp (in millis since epoch)
   * @param spEntityId the entityID of the requesting SP
   * @param authnRequestId the ID of the {@code AuthnRequest}
   * @param data audit data
   */
  public ConnectorAuthnAuditEvent(final String type, final long timestamp, final String spEntityId,
      final String authnRequestId, final Saml2AuditData... data) {
    super(type, timestamp,
        Optional.ofNullable(spEntityId).orElse(Saml2AuditEvent.UNKNOWN_SP),
        buildData(spEntityId, authnRequestId, data));
  }

  /**
   * Gets a string suitable to include in log entries. It does not dump the entire audit data that can contain sensible
   * data (that should not be present in proceess logs).
   *
   * @return a log string
   */
  @Override
  @JsonIgnore
  public String getLogString() {
    return String.format("type='%s', timestamp='%s', sp-entity-id='%s', authn-request-id='%s'",
        this.getType(), this.getTimestamp(), this.getData().get("sp-entity-id"),
        this.getData().get("authn-request-id"));
  }

}
