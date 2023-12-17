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

import java.util.Objects;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.eidas.connector.audit.data.EuMetadataChangeAuditData;
import se.swedenconnect.eidas.connector.events.EuMetadataEvent;

/**
 * The eIDAS Connector Audit event publisher. The component listens for connector events and translates them into audit
 * events.
 *
 * @author Martin Lindström
 */
@Component
@Slf4j
public class ConnectorAuditPublisher {

  /** The system event publisher. */
  private final ApplicationEventPublisher publisher;

  /**
   * Constructor.
   *
   * @param publisher the system event publisher
   */
  public ConnectorAuditPublisher(final ApplicationEventPublisher publisher) {
    this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
  }

  /**
   * Handles {@link EuMetadataEvent}s and translates them into an
   * {@value ConnectorAuditEvents#CONNECTOR_AUDIT_EU_METADATA_CHANGED} event containing
   * {@link EuMetadataChangeAuditData} audit data.
   *
   * @param euMetadataEvent the event
   */
  @EventListener
  public void onEuMetadataEvent(final EuMetadataEvent euMetadataEvent) {
    final EuMetadataChangeAuditData auditData = EuMetadataChangeAuditData.of(euMetadataEvent.getEuMetadataUpdateData());
    if (auditData != null) {
      ConnectorAuditEvent auditEvent = new ConnectorAuditEvent(
          ConnectorAuditEvents.CONNECTOR_AUDIT_EU_METADATA_CHANGED, euMetadataEvent.getTimestamp(), auditData);

      log.info("Publishing audit event: {}", auditEvent);

      this.publish(auditEvent);
    }
  }

  /**
   * Publishes the {@link AuditEvent}.
   *
   * @param auditEvent the event to publish
   */
  private void publish(final ConnectorAuditEvent auditEvent) {
    this.publisher.publishEvent(new AuditApplicationEvent(auditEvent));
  }

}
