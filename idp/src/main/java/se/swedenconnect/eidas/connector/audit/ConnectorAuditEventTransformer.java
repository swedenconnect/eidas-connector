/*
 * Copyright 2017-2026 Sweden Connect
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

import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.util.StringUtils;
import se.swedenconnect.eidas.connector.audit.data.ConnectorAuditData;
import se.swedenconnect.eidas.connector.audit.data.EidasAuthnRequestAuditData;
import se.swedenconnect.eidas.connector.audit.data.EuMetadataChangeAuditData;
import se.swedenconnect.eidas.connector.audit.data.EuPeerCountryAuditData;
import se.swedenconnect.eidas.connector.audit.data.IdmConsentAuditData;
import se.swedenconnect.eidas.connector.audit.data.IdmErrorAuditData;
import se.swedenconnect.eidas.connector.audit.data.IdmRecordAuditData;
import se.swedenconnect.eidas.connector.audit.data.ProcessingErrorAuditData;
import se.swedenconnect.eidas.connector.audit.data.SamlAssertionAuditData;
import se.swedenconnect.eidas.connector.audit.data.SamlResponseAuditData;
import se.swedenconnect.eidas.connector.audit.data.SignatureConsentAuditData;
import se.swedenconnect.eidas.connector.events.AbstractConnectorAuthnEvent;
import se.swedenconnect.eidas.connector.events.BeforeEidasAuthenticationEvent;
import se.swedenconnect.eidas.connector.events.ErrorEidasResponseEvent;
import se.swedenconnect.eidas.connector.events.EuMetadataEvent;
import se.swedenconnect.eidas.connector.events.IdentityMatchingConsentEvent;
import se.swedenconnect.eidas.connector.events.IdentityMatchingErrorEvent;
import se.swedenconnect.eidas.connector.events.IdentityMatchingRecordEvent;
import se.swedenconnect.eidas.connector.events.ResponseProcessingErrorEvent;
import se.swedenconnect.eidas.connector.events.SignatureConsentEvent;
import se.swedenconnect.eidas.connector.events.SuccessEidasResponseEvent;
import se.swedenconnect.spring.audit.AuditEvent;
import se.swedenconnect.spring.audit.AuditEventBuilder;
import se.swedenconnect.spring.audit.AuditEventContext;
import se.swedenconnect.spring.audit.transform.EventTransformer;
import se.swedenconnect.spring.audit.value.AuditValue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Transforms the events of the connector into audit events. The type, the principal and the data of each audit event
 * are those that earlier versions of the connector produced, see {@link ConnectorAuditEvents}. The common structure of
 * spring-audit-support, such as the application name and the correlation ID, is added.
 *
 * @author Martin Lindström
 */
public class ConnectorAuditEventTransformer implements EventTransformer {

  /** Logger instance. */
  private static final Logger log = LoggerFactory.getLogger(ConnectorAuditEventTransformer.class);

  /** The principal of system events. */
  public static final String DEFAULT_PRINCIPAL = "eidas-connector";

  /** Used when the SP or the ID of the authentication request is not known. */
  public static final String UNKNOWN = "unknown";

  /** The events that are transformed. */
  private static final List<Class<? extends ApplicationEvent>> SUPPORTED_EVENTS = List.of(
      EuMetadataEvent.class, BeforeEidasAuthenticationEvent.class, SuccessEidasResponseEvent.class,
      ErrorEidasResponseEvent.class, ResponseProcessingErrorEvent.class, SignatureConsentEvent.class,
      IdentityMatchingConsentEvent.class, IdentityMatchingRecordEvent.class, IdentityMatchingErrorEvent.class,
      EidasSuccessResponseAuditEvent.class);

  /**
   * Supports the events of the connector. An EU metadata event or an event about the authentication request sent to
   * the foreign country is only supported if it gives any audit data.
   */
  @Override
  public boolean supports(final ApplicationEvent event) {
    return switch (event) {
      case final EuMetadataEvent e -> EuMetadataChangeAuditData.of(e.getEuMetadataUpdateData()) != null;
      case final BeforeEidasAuthenticationEvent e -> EidasAuthnRequestAuditData.of(e) != null;
      default -> SUPPORTED_EVENTS.stream().anyMatch(c -> c.isInstance(event));
    };
  }

  /** {@inheritDoc} */
  @Override
  public AuditEvent transform(final ApplicationEvent event, final AuditEventContext context)
      throws UnsupportedOperationException {

    final AuditEvent auditEvent = switch (event) {
      case final EuMetadataEvent e -> systemEvent(context, ConnectorAuditEvents.CONNECTOR_AUDIT_EU_METADATA_CHANGED,
          e.getTimestamp(), EuMetadataChangeAuditData.of(e.getEuMetadataUpdateData()));
      case final BeforeEidasAuthenticationEvent e -> authnEvent(context,
          ConnectorAuditEvents.CONNECTOR_BEFORE_SAML_REQUEST, e, EidasAuthnRequestAuditData.of(e));
      case final SuccessEidasResponseEvent e -> authnEvent(context, ConnectorAuditEvents.CONNECTOR_SUCCESS_RESPONSE, e,
          SamlResponseAuditData.of(e.getResponse()),
          SamlAssertionAuditData.of(e.getAssertion(), isEncrypted(e.getResponse())),
          EuPeerCountryAuditData.of(e.getCountry()));
      case final ErrorEidasResponseEvent e -> authnEvent(context, ConnectorAuditEvents.CONNECTOR_ERROR_RESPONSE, e,
          SamlResponseAuditData.of(e.getResponse()));
      case final ResponseProcessingErrorEvent e -> authnEvent(context, ConnectorAuditEvents.CONNECTOR_PROCESSING_ERROR,
          e, ProcessingErrorAuditData.of(e), SamlResponseAuditData.of(e.getResponse()));
      case final SignatureConsentEvent e -> authnEvent(context,
          ConnectorAuditEvents.CONNECTOR_SIGNATURE_CONSENT_RESULT, e, SignatureConsentAuditData.of(e));
      case final IdentityMatchingConsentEvent e -> authnEvent(context,
          ConnectorAuditEvents.CONNECTOR_IDM_CONSENT_RESULT, e, IdmConsentAuditData.of(e));
      case final IdentityMatchingRecordEvent e -> authnEvent(context, ConnectorAuditEvents.CONNECTOR_IDM_RECORD, e,
          IdmRecordAuditData.of(e));
      case final IdentityMatchingErrorEvent e -> authnEvent(context, ConnectorAuditEvents.CONNECTOR_IDM_ERROR, e,
          IdmErrorAuditData.of(e));
      case final EidasSuccessResponseAuditEvent e -> authnEvent(context,
          ConnectorAuditEvents.EIDAS_AUDIT_SUCCESSFUL_RESPONSE, e.getTimestamp(), e.getSpEntityId(),
          e.getResponse().getInResponseTo(),
          SamlResponseAuditData.of(e.getResponse()),
          SamlAssertionAuditData.of(e.getAssertion(), isEncrypted(e.getResponse())),
          EuPeerCountryAuditData.of(e.getAssertion()));
      default -> throw new UnsupportedOperationException(
          "Unsupported event type: " + event.getClass().getName());
    };

    log.info("Publishing audit event: type='{}', timestamp='{}', principal='{}'",
        auditEvent.getType(), auditEvent.getTimestamp(), auditEvent.getPrincipal());

    return auditEvent;
  }

  /**
   * Creates an audit event that concerns user authentication.
   *
   * @param context the audit event context
   * @param type the type of audit event
   * @param event the connector event
   * @param data audit data
   * @return an {@link AuditEvent}
   */
  private static AuditEvent authnEvent(final AuditEventContext context, final String type,
      final AbstractConnectorAuthnEvent event, final ConnectorAuditData... data) {
    return authnEvent(context, type, event.getTimestamp(), event.getOriginalSpId(),
        event.getOriginalAuthnRequestId(), data);
  }

  /**
   * Creates an audit event that concerns user authentication. The data holds {@code sp-entity-id} and
   * {@code authn-request-id}, followed by the supplied data elements.
   *
   * @param context the audit event context
   * @param type the type of audit event
   * @param timestamp the timestamp (in millis since epoch)
   * @param spEntityId the entityID of the requesting SP
   * @param authnRequestId the ID of the {@code AuthnRequest}
   * @param data audit data
   * @return an {@link AuditEvent}
   */
  private static AuditEvent authnEvent(final AuditEventContext context, final String type, final long timestamp,
      final String spEntityId, final String authnRequestId, final ConnectorAuditData... data) {

    final String sp = StringUtils.hasText(spEntityId) ? spEntityId : UNKNOWN;
    final AuditEventBuilder builder = AuditEventBuilder.builder(context)
        .type(type)
        .timestamp(Instant.ofEpochMilli(timestamp))
        .principal(sp)
        .dataField(new AuditValue<>("sp-entity-id", sp))
        .dataField(new AuditValue<>("authn-request-id",
            StringUtils.hasText(authnRequestId) ? authnRequestId : UNKNOWN));
    addData(builder, data);
    return builder.build();
  }

  /**
   * Creates a system audit event.
   *
   * @param context the audit event context
   * @param type the type of audit event
   * @param timestamp the timestamp (in millis since epoch)
   * @param data audit data
   * @return an {@link AuditEvent}
   */
  private static AuditEvent systemEvent(final AuditEventContext context, final String type, final long timestamp,
      final ConnectorAuditData... data) {
    final AuditEventBuilder builder = AuditEventBuilder.builder(context)
        .type(type)
        .timestamp(Instant.ofEpochMilli(timestamp))
        .principal(DEFAULT_PRINCIPAL);
    addData(builder, data);
    return builder.build();
  }

  /**
   * Adds the data elements that are set to the audit event.
   *
   * @param builder the builder
   * @param data the data elements
   */
  private static void addData(final AuditEventBuilder builder, final ConnectorAuditData... data) {
    if (data != null) {
      for (final ConnectorAuditData d : data) {
        Optional.ofNullable(d).map(ConnectorAuditData::toAuditValue).ifPresent(builder::dataField);
      }
    }
  }

  /**
   * Tells whether the response holds an encrypted assertion.
   *
   * @param response the response, may be {@code null}
   * @return whether the assertion is encrypted
   */
  private static boolean isEncrypted(final Response response) {
    return Optional.ofNullable(response)
        .map(Response::getEncryptedAssertions)
        .filter(l -> !l.isEmpty())
        .isPresent();
  }

}
