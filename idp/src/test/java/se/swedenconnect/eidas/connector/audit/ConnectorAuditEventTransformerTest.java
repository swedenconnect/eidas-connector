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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.opensaml.saml.saml2.core.AuthnRequest;
import se.swedenconnect.eidas.connector.OpenSamlTestBase;
import se.swedenconnect.eidas.connector.authn.EidasAuthenticationToken;
import se.swedenconnect.eidas.connector.events.BeforeEidasAuthenticationEvent;
import se.swedenconnect.eidas.connector.events.EuMetadataEvent;
import se.swedenconnect.eidas.connector.events.IdentityMatchingConsentEvent;
import se.swedenconnect.eidas.connector.events.ResponseProcessingErrorEvent;
import se.swedenconnect.opensaml.saml2.core.build.AuthnRequestBuilder;
import se.swedenconnect.spring.audit.AuditEvent;
import se.swedenconnect.spring.audit.AuditEventContext;
import se.swedenconnect.spring.audit.repository.JsonAuditEventMapper;
import se.swedenconnect.spring.audit.support.ApplicationName;
import se.swedenconnect.spring.audit.support.ApplicationVersion;
import se.swedenconnect.spring.audit.tracing.CorrelationID;
import se.swedenconnect.spring.audit.tracing.TraceID;
import se.swedenconnect.spring.authnserver.authentication.AuthenticationProtocol;
import se.swedenconnect.spring.authnserver.authentication.Requester;
import se.swedenconnect.spring.authnserver.authentication.provider.UserAuthenticationInputToken;
import se.swedenconnect.spring.authnserver.saml.authentication.SamlAuthenticationRequirements;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConnectorAuditEventTransformer}. The type and the data of each event, field names included, are
 * those of earlier versions of the connector.
 *
 * @author Martin Lindström
 */
class ConnectorAuditEventTransformerTest extends OpenSamlTestBase {

  private static final String SP = "https://sp.example.com";

  private final ConnectorAuditEventTransformer transformer = new ConnectorAuditEventTransformer();

  private final JsonAuditEventMapper mapper = new JsonAuditEventMapper(JsonMapper.builder().build());

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @Test
  void testProcessingError() {
    final ResponseProcessingErrorEvent event = new ResponseProcessingErrorEvent(inputToken(), "Bad assertion");
    assertThat(this.transformer.supports(event)).isTrue();

    final JsonNode json = this.toJson(this.transformer.transform(event, context()));
    assertThat(json.get("type").asString()).isEqualTo(ConnectorAuditEvents.CONNECTOR_PROCESSING_ERROR);
    assertThat(json.get("principal").asString()).isEqualTo(SP);
    assertThat(json.get("correlation_id").asString()).isEqualTo("correlation");
    assertThat(json.get("application").get("name").asString()).isEqualTo("eidas-connector");

    final JsonNode data = json.get("data");
    assertThat(data.get("sp-entity-id").asString()).isEqualTo(SP);
    assertThat(data.get("authn-request-id").asString()).isEqualTo("_request");
    assertThat(data.get("processing-error").get("error-message").asString()).isEqualTo("Bad assertion");
    assertThat(data.has("saml-response")).isFalse();
  }

  @Test
  void testBeforeSamlRequest() {
    final AuthnRequest authnRequest = AuthnRequestBuilder.builder()
        .id("_eidas")
        .issuer("https://connector.example.com/metadata/sp")
        .destination("https://idp.example.de/sso")
        .build();
    final BeforeEidasAuthenticationEvent event =
        new BeforeEidasAuthenticationEvent(inputToken(), "DE", authnRequest, "relay", "POST");
    assertThat(this.transformer.supports(event)).isTrue();

    final JsonNode data = this.toJson(this.transformer.transform(event, context())).get("data");
    final JsonNode request = data.get("eidas-authn-request");
    assertThat(request.get("country").asString()).isEqualTo("DE");
    assertThat(request.get("authn-request-id").asString()).isEqualTo("_eidas");
    assertThat(request.get("relay-state").asString()).isEqualTo("relay");
    assertThat(request.get("destination-url").asString()).isEqualTo("https://idp.example.de/sso");
    assertThat(request.get("method").asString()).isEqualTo("POST");
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void testIdmConsent(final boolean consented) {
    final EidasAuthenticationToken eidasToken = Mockito.mock(EidasAuthenticationToken.class);
    Mockito.when(eidasToken.getPrincipal()).thenReturn("DE/SE/12345");
    final IdentityMatchingConsentEvent event = new IdentityMatchingConsentEvent(inputToken(), eidasToken, consented);
    assertThat(this.transformer.supports(event)).isTrue();

    final JsonNode json = this.toJson(this.transformer.transform(event, context()));
    assertThat(json.get("type").asString()).isEqualTo(ConnectorAuditEvents.CONNECTOR_IDM_CONSENT_RESULT);
    final JsonNode data = json.get("data").get("idm-consent-result");
    assertThat(data.get("idm-consented").asBoolean()).isEqualTo(consented);
    assertThat(data.get("user").asString()).isEqualTo("DE/SE/12345");
  }

  @Test
  void testEuMetadataChange() {
    final EuMetadataEvent event = new EuMetadataEvent(Instant.now(), List.of("FR"), List.of("DE"));
    assertThat(this.transformer.supports(event)).isTrue();

    final JsonNode json = this.toJson(this.transformer.transform(event, context()));
    assertThat(json.get("type").asString()).isEqualTo(ConnectorAuditEvents.CONNECTOR_AUDIT_EU_METADATA_CHANGED);
    assertThat(json.get("principal").asString()).isEqualTo(ConnectorAuditEventTransformer.DEFAULT_PRINCIPAL);
    final JsonNode change = json.get("data").get("eu-metadata-change");
    assertThat(change.get("removed-countries").get(0).asString()).isEqualTo("FR");
    assertThat(change.get("added-countries").get(0).asString()).isEqualTo("DE");
  }

  @Test
  void testEuMetadataWithoutChangeIsNotAudited() {
    assertThat(this.transformer.supports(new EuMetadataEvent(Instant.now(), List.of(), List.of()))).isFalse();
  }

  private JsonNode toJson(final AuditEvent event) {
    return this.jsonMapper.readTree(this.mapper.write(event));
  }

  private static UserAuthenticationInputToken inputToken() {
    return new UserAuthenticationInputToken(new SamlAuthenticationRequirements(),
        new Requester(AuthenticationProtocol.SAML, SP), "_request", null);
  }

  private static AuditEventContext context() {
    return new AuditEventContext() {
      @Override
      public ApplicationName getApplicationName() {
        return new ApplicationName("eidas-connector");
      }

      @Override
      public ApplicationVersion getApplicationVersion() {
        return null;
      }

      @Override
      public CorrelationID getCorrelationId() {
        return CorrelationID.of("correlation");
      }

      @Override
      public TraceID getTraceId() {
        return null;
      }

      @Override
      public String getPrincipal() {
        return null;
      }
    };
  }

}
