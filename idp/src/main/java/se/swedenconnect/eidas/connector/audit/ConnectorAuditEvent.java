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
package se.swedenconnect.eidas.connector.audit;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.spring.saml.idp.audit.Saml2AuditEvent;
import se.swedenconnect.spring.saml.idp.audit.data.Saml2AuditData;

/**
 * Audit event for creating event objects for the eIDAS Connector.
 *
 * @author Martin Lindström
 */
@JsonInclude(Include.NON_EMPTY)
public class ConnectorAuditEvent extends AuditEvent {

  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** Symbolic constant for the default principal. */
  public static final String DEFAULT_PRINCIPAL = "eidas-connector";

  /**
   * Constructor.
   *
   * @param type the type of audit event
   * @param timestamp the timestamp (in millis since epoch)
   * @param principal the principal for the event
   * @param data audit data
   */
  public ConnectorAuditEvent(
      final String type, final long timestamp, final String principal, final Saml2AuditData... data) {
    this(type, timestamp, principal, buildData(data));
  }

  /**
   * Constructor.
   *
   * @param type the type of audit event
   * @param timestamp the timestamp (in millis since epoch)
   * @param principal the principal for the event
   * @param data audit data
   */
  protected ConnectorAuditEvent(
      final String type, final long timestamp, final String principal, final Map<String, Object> data) {
    super(Instant.ofEpochMilli(timestamp),
        Optional.ofNullable(principal).orElseGet(() -> DEFAULT_PRINCIPAL), type, data);
  }

  /**
   * Builds a {@link Map} given the supplied audit data
   *
   * @param data audit data
   * @return a {@link Map} of audit data
   */
  protected static Map<String, Object> buildData(final Saml2AuditData... data) {
    final Map<String, Object> auditData = new HashMap<>();

    if (data != null) {
      for (final Saml2AuditData ad : data) {
        if (ad != null) {
          auditData.put(ad.getName(), ad);
        }
      }
    }
    return auditData;
  }

  /**
   * Builds a {@link Map} given the supplied audit data
   *
   * @param spEntityId the entityID of the requesting SP
   * @param authnRequestId the ID of the {@code AuthnRequest}
   * @param data audit data
   * @return a {@link Map} of audit data
   */
  protected static Map<String, Object> buildData(
      final String spEntityId, final String authnRequestId, final Saml2AuditData... data) {
    final Map<String, Object> auditData = new HashMap<>();

    auditData.put("sp-entity-id", StringUtils.hasText(spEntityId) ? spEntityId : Saml2AuditEvent.UNKNOWN_SP);
    auditData.put("authn-request-id", StringUtils.hasText(authnRequestId) ? authnRequestId : Saml2AuditEvent.UNKNOWN_AUTHN_REQUEST_ID);
    if (data != null) {
      for (final Saml2AuditData ad : data) {
        if (ad != null) {
          auditData.put(ad.getName(), ad);
        }
      }
    }

    return auditData;
  }

  /**
   * Gets a string suitable to include in log entries. It does not dump the entire audit data that can contain sensible
   * data (that should not be present in process logs).
   *
   * @return a log string
   */
  @JsonIgnore
  public String getLogString() {
    return String.format("type='%s', timestamp='%s', principal='%s', data=%s",
        this.getType(), this.getTimestamp(), this.getPrincipal(), this.getData());
  }

}
