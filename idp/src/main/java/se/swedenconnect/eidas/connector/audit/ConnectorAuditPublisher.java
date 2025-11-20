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

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import se.swedenconnect.eidas.connector.audit.data.EidasAuthnRequestAuditData;
import se.swedenconnect.eidas.connector.audit.data.EuMetadataChangeAuditData;
import se.swedenconnect.eidas.connector.audit.data.EuPeerCountryAuditData;
import se.swedenconnect.eidas.connector.audit.data.IdmConsentAuditData;
import se.swedenconnect.eidas.connector.audit.data.IdmErrorAuditData;
import se.swedenconnect.eidas.connector.audit.data.IdmRecordAuditData;
import se.swedenconnect.eidas.connector.audit.data.ProcessingErrorAuditData;
import se.swedenconnect.eidas.connector.audit.data.SignatureConsentAuditData;
import se.swedenconnect.eidas.connector.events.BeforeEidasAuthenticationEvent;
import se.swedenconnect.eidas.connector.events.ErrorEidasResponseEvent;
import se.swedenconnect.eidas.connector.events.EuMetadataEvent;
import se.swedenconnect.eidas.connector.events.IdentityMatchingConsentEvent;
import se.swedenconnect.eidas.connector.events.IdentityMatchingErrorEvent;
import se.swedenconnect.eidas.connector.events.IdentityMatchingRecordEvent;
import se.swedenconnect.eidas.connector.events.ResponseProcessingErrorEvent;
import se.swedenconnect.eidas.connector.events.SignatureConsentEvent;
import se.swedenconnect.eidas.connector.events.SuccessEidasResponseEvent;
import se.swedenconnect.spring.saml.idp.audit.data.Saml2AssertionAuditData;
import se.swedenconnect.spring.saml.idp.audit.data.Saml2ResponseAuditData;
import se.swedenconnect.spring.saml.idp.events.Saml2SuccessResponseEvent;

import java.util.Objects;
import java.util.Optional;

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
   * Handles {@link EuMetadataEvent}s and translates them into a
   * {@value ConnectorAuditEvents#CONNECTOR_AUDIT_EU_METADATA_CHANGED} event containing
   * {@link EuMetadataChangeAuditData} audit data.
   *
   * @param euMetadataEvent the event
   */
  @EventListener
  public void onEuMetadataEvent(final EuMetadataEvent euMetadataEvent) {
    final EuMetadataChangeAuditData auditData = EuMetadataChangeAuditData.of(euMetadataEvent.getEuMetadataUpdateData());
    if (auditData != null) {
      final ConnectorAuditEvent auditEvent = new ConnectorAuditEvent(
          ConnectorAuditEvents.CONNECTOR_AUDIT_EU_METADATA_CHANGED, euMetadataEvent.getTimestamp(), null, auditData);

      log.info("Publishing audit event: {}", auditEvent);

      this.publish(auditEvent);
    }
  }

  /**
   * Handles {@link BeforeEidasAuthenticationEvent}s and translates them into a
   * {@value ConnectorAuditEvents#CONNECTOR_BEFORE_SAML_REQUEST} event containing {@link EidasAuthnRequestAuditData}
   * audit data.
   *
   * @param event the event
   */
  @EventListener
  public void onBeforeEidasAuthenticationEvent(final BeforeEidasAuthenticationEvent event) {
    final EidasAuthnRequestAuditData auditData = EidasAuthnRequestAuditData.of(event);
    if (auditData != null) {
      final ConnectorAuditEvent auditEvent = new ConnectorAuthnAuditEvent(
          ConnectorAuditEvents.CONNECTOR_BEFORE_SAML_REQUEST, event.getTimestamp(),
          event.getOriginalSpId(), event.getOriginalAuthnRequestId(),
          auditData);

      log.info("Publishing audit event: {}", auditEvent);

      this.publish(auditEvent);
    }
  }

  /**
   * Handles {@link SuccessEidasResponseEvent}s and translates them into a
   * {@value ConnectorAuditEvents#CONNECTOR_SUCCESS_RESPONSE} event containing {@link Saml2ResponseAuditData} and
   * {@link Saml2AssertionAuditData}.
   *
   * @param event the event
   */
  @EventListener
  public void onSuccessEidasResponseEvent(final SuccessEidasResponseEvent event) {
    final ConnectorAuditEvent auditEvent =
        new ConnectorAuthnAuditEvent(ConnectorAuditEvents.CONNECTOR_SUCCESS_RESPONSE, event.getTimestamp(),
            event.getOriginalSpId(), event.getOriginalAuthnRequestId(),
            Saml2ResponseAuditData.of(event.getResponse()),
            Saml2AssertionAuditData.of(event.getAssertion(), Optional.ofNullable(event.getResponse())
                .map(Response::getEncryptedAssertions)
                .filter(l -> !l.isEmpty())
                .isPresent()),
            EuPeerCountryAuditData.of(event.getCountry()));

    log.info("Publishing audit event: {}", auditEvent.getLogString());

    this.publish(auditEvent);
  }

  @EventListener
  public void onSuccessResponseEvent(@Nonnull final Saml2SuccessResponseEvent event) {

    final Assertion assertion = event.getAssertion();

    final ConnectorAuthnAuditEvent auditEvent =
        new ConnectorAuthnAuditEvent(ConnectorAuditEvents.EIDAS_AUDIT_SUCCESSFUL_RESPONSE, event.getTimestamp(),
            event.getSpEntityId(), Optional.ofNullable(event.getResponse()).map(Response::getInResponseTo).orElse(null),
            Saml2ResponseAuditData.of(event.getResponse()),
            Saml2AssertionAuditData.of(assertion, Optional.ofNullable(event.getResponse())
                .map(Response::getEncryptedAssertions)
                .filter(l -> !l.isEmpty())
                .isPresent()),
            EuPeerCountryAuditData.of(assertion));

    log.info("Publishing audit event: {}", auditEvent.getLogString());

    this.publish(auditEvent);
  }

  /**
   * Handles {@link ErrorEidasResponseEvent}s and translates them into a
   * {@value ConnectorAuditEvents#CONNECTOR_ERROR_RESPONSE} event containing {@link Saml2ResponseAuditData}.
   *
   * @param event the event
   */
  @EventListener
  public void onErrorEidasResponseEvent(final ErrorEidasResponseEvent event) {
    final ConnectorAuditEvent auditEvent =
        new ConnectorAuthnAuditEvent(ConnectorAuditEvents.CONNECTOR_ERROR_RESPONSE, event.getTimestamp(),
            event.getOriginalSpId(), event.getOriginalAuthnRequestId(),
            Saml2ResponseAuditData.of(event.getResponse()));

    log.info("Publishing audit event: {}", auditEvent.getLogString());

    this.publish(auditEvent);
  }

  /**
   * Handles {@link ResponseProcessingErrorEvent}s and translates them into a
   * {@value ConnectorAuditEvents#CONNECTOR_PROCESSING_ERROR} event containing a {@link ProcessingErrorAuditData} and
   * optionally a {@link Saml2ResponseAuditData}.
   *
   * @param event the event
   */
  @EventListener
  public void onResponseProcessingErrorEvent(final ResponseProcessingErrorEvent event) {
    final ConnectorAuditEvent auditEvent =
        new ConnectorAuthnAuditEvent(ConnectorAuditEvents.CONNECTOR_PROCESSING_ERROR, event.getTimestamp(),
            event.getOriginalSpId(), event.getOriginalAuthnRequestId(),
            ProcessingErrorAuditData.of(event),
            Saml2ResponseAuditData.of(event.getResponse()));

    log.info("Publishing audit event: {}", auditEvent.getLogString());

    this.publish(auditEvent);
  }

  /**
   * Handles {@link SignatureConsentEvent}s and translates them into a
   * {@value ConnectorAuditEvents#CONNECTOR_SIGNATURE_CONSENT_RESULT} event containing
   * {@link SignatureConsentAuditData}.
   *
   * @param event the event
   */
  @EventListener
  public void onSignatureConsentEvent(final SignatureConsentEvent event) {
    final ConnectorAuditEvent auditEvent =
        new ConnectorAuthnAuditEvent(ConnectorAuditEvents.CONNECTOR_SIGNATURE_CONSENT_RESULT, event.getTimestamp(),
            event.getOriginalSpId(), event.getOriginalAuthnRequestId(),
            SignatureConsentAuditData.of(event));

    log.info("Publishing audit event: {}", auditEvent.getLogString());

    this.publish(auditEvent);
  }

  /**
   * Handles {@link IdentityMatchingConsentEvent}s and translates them into a
   * {@value ConnectorAuditEvents#CONNECTOR_IDM_CONSENT_RESULT} event containing {@link IdmConsentAuditData}.
   *
   * @param event the event
   */
  @EventListener
  public void onIdmConsentEvent(final IdentityMatchingConsentEvent event) {
    final ConnectorAuditEvent auditEvent =
        new ConnectorAuthnAuditEvent(ConnectorAuditEvents.CONNECTOR_IDM_CONSENT_RESULT, event.getTimestamp(),
            event.getOriginalSpId(), event.getOriginalAuthnRequestId(),
            IdmConsentAuditData.of(event));

    log.info("Publishing audit event: {}", auditEvent.getLogString());

    this.publish(auditEvent);
  }

  /**
   * Handles {@link IdentityMatchingRecordEvent}s and translates them into a
   * {@value ConnectorAuditEvents#CONNECTOR_IDM_RECORD} event containing {@link IdmRecordAuditData}.
   *
   * @param event the event
   */
  @EventListener
  public void onIdmRecordEvent(final IdentityMatchingRecordEvent event) {
    final ConnectorAuditEvent auditEvent =
        new ConnectorAuthnAuditEvent(ConnectorAuditEvents.CONNECTOR_IDM_RECORD, event.getTimestamp(),
            event.getOriginalSpId(), event.getOriginalAuthnRequestId(),
            IdmRecordAuditData.of(event));

    log.info("Publishing audit event: {}", auditEvent.getLogString());

    this.publish(auditEvent);
  }

  /**
   * Handles {@link IdentityMatchingConsentEvent}s and translates them into a
   * {@value ConnectorAuditEvents#CONNECTOR_IDM_ERROR} event containing {@link IdmErrorAuditData}.
   *
   * @param event the event
   */
  @EventListener
  public void onIdmErrorEvent(final IdentityMatchingErrorEvent event) {
    final ConnectorAuditEvent auditEvent =
        new ConnectorAuthnAuditEvent(ConnectorAuditEvents.CONNECTOR_IDM_ERROR, event.getTimestamp(),
            event.getOriginalSpId(), event.getOriginalAuthnRequestId(),
            IdmErrorAuditData.of(event));

    log.info("Publishing audit event: {}", auditEvent.getLogString());

    this.publish(auditEvent);
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
