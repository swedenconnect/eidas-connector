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
package se.swedenconnect.eidas.connector.audit;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.actuate.audit.AuditEvent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.eidas.connector.audit.data.ConnectorAuditData;

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
   * @param data audit data
   */
  public ConnectorAuditEvent(
      final String type, final long timestamp, final ConnectorAuditData... data) {
    this(type, timestamp, DEFAULT_PRINCIPAL, buildData(data));
  }

  /**
   * Constructor.
   *
   * @param type the type of audit event
   * @param timestamp the timestamp (in millis since epoch)
   * @param principal the principal for the event
   * @param data audit data
   */
  public ConnectorAuditEvent(
      final String type, final long timestamp, final String principal, final ConnectorAuditData... data) {
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
    super(
        Instant.ofEpochMilli(timestamp), type, Optional.ofNullable(principal).orElseGet(() -> DEFAULT_PRINCIPAL), data);
  }

  /**
   * Builds a {@link Map} given the supplied audit data
   *
   * @param data audit data
   * @return a {@link Map} of audit data
   */
  protected static Map<String, Object> buildData(final ConnectorAuditData... data) {
    final Map<String, Object> auditData = new HashMap<>();

    if (data != null) {
      for (final ConnectorAuditData sad : data) {
        auditData.put(sad.getName(), sad);
      }
    }
    return auditData;
  }

  /**
   * Gets a string suitable to include in log entries. It does not dump the entire audit data that can contain sensible
   * data (that should not be present in proceess logs).
   *
   * @return a log string
   */
  @JsonIgnore
  public String getLogString() {
    return String.format("type='%s', timestamp='%s', principal='%s', data=%s",
        this.getType(), this.getTimestamp(), this.getPrincipal(), this.getData());
  }

}
