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
package se.swedenconnect.eidas.connector;

import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AuthenticatingAuthority;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.MvcResult;
import se.swedenconnect.eidas.connector.audit.ConnectorAuditEvents;
import se.swedenconnect.eidas.connector.authn.ui.EidasUiModel;
import se.swedenconnect.eidas.connector.authn.ui.SignUiModel;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttribute;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttributes;
import se.swedenconnect.opensaml.sweid.saml2.authn.psc.PrincipalSelection;
import se.swedenconnect.opensaml.sweid.saml2.authn.psc.build.MatchValueBuilder;
import se.swedenconnect.opensaml.sweid.saml2.metadata.entitycategory.EntityCategoryConstants;
import se.swedenconnect.opensaml.sweid.saml2.signservice.dss.SignMessage;
import se.swedenconnect.opensaml.sweid.saml2.signservice.dss.SignMessageMimeTypeEnum;
import se.swedenconnect.opensaml.xmlsec.encryption.support.SAMLObjectDecrypter;
import se.swedenconnect.spring.authnserver.saml.attributes.SamlAttributeValues;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Runs an authentication through the connector: a Swedish SP sends an authentication request, the user selects a
 * country, the connector sends an eIDAS authentication request, the foreign country answers, and the connector posts a
 * SAML response to the Swedish SP.
 *
 * @author Martin Lindström
 */
class AuthenticationFlowTest extends AbstractAuthenticationFlowTest {

  @Test
  void testSuccessfulAuthentication() throws Exception {
    final Browser session = new Browser();

    // The Swedish SP sends an authentication request ...
    //
    final String authnPath = this.sendAuthnRequest(session);
    assertThat(authnPath).startsWith("/idp/extauth?authnId=");
    final String authnId = authnPath.substring(authnPath.indexOf('=') + 1);

    // The user is shown the country selection ...
    //
    final MvcResult countrySelect = this.mockMvc.perform(get(REQUEST_BASE_URL + "/extauth").contextPath("/idp")
        .param("authnId", authnId).with(session)).andDo(session).andReturn();
    assertThat(view(countrySelect).getViewName()).isEqualTo("country-select");
    assertThat(view(countrySelect).getModel()).containsEntry("authnId", authnId);
    assertThat(countrySelect.getResponse().getContentAsString()).doesNotContain("alt=\"Logo\"");
    assertThat(((EidasUiModel) view(countrySelect).getModel().get("ui")).getUserMessage()).isNull();
    assertThat(countrySelect.getResponse().getContentAsString()).doesNotContain("id=\"userMessage\"");

    // ... and selects a country, which gives an eIDAS authentication request ...
    //
    final AuthnRequest eidasRequest = this.selectCountry(session, authnId);

    final List<RequestedAttribute> requested = ((RequestedAttributes) eidasRequest.getExtensions()
        .getUnknownXMLObjects(RequestedAttributes.DEFAULT_ELEMENT_NAME).getFirst()).getRequestedAttributes();
    assertThat(requested.stream().map(RequestedAttribute::getName).toList()).containsExactly(
        "http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName",
        "http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName",
        "http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier",
        "http://eidas.europa.eu/attributes/naturalperson/DateOfBirth",
        "http://eidas.europa.eu/attributes/naturalperson/Gender",
        "http://eidas.europa.eu/attributes/naturalperson/PlaceOfBirth",
        "http://eidas.europa.eu/attributes/naturalperson/TownOfBirth",
        "http://eidas.europa.eu/attributes/naturalperson/CountryOfBirth",
        "http://e-justice.europa.eu/attributes/naturalperson/eJusticeNaturalPersonRole");
    assertThat(requested.stream().filter(RequestedAttribute::isRequired).map(RequestedAttribute::getName).toList())
        .containsExactly(
            "http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName",
            "http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName",
            "http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier",
            "http://eidas.europa.eu/attributes/naturalperson/DateOfBirth");

    // The foreign country answers ...
    //
    final MvcResult foreignResponse = this.mockMvc.perform(post(REQUEST_BASE_URL + "/extauth/saml2/post")
            .contextPath("/idp").with(session)
            .param("SAMLResponse", encode(this.eidasResponse(eidasRequest, successStatus(), true)))
            .param("RelayState", "sp-relay"))
        .andDo(session).andReturn();
    final String resumePath = foreignResponse.getResponse().getRedirectedUrl();
    assertThat(resumePath).isEqualTo("/idp/resume?authnId=" + authnId);

    // ... and the connector answers the Swedish SP.
    //
    final Response response = this.resume(session, resumePath);
    assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.SUCCESS);
    assertThat(response.getInResponseTo()).isNotNull();

    final Assertion assertion = new SAMLObjectDecrypter(SP_CREDENTIAL)
        .decrypt(response.getEncryptedAssertions().getFirst(), Assertion.class);

    final String prid = this.pridService.generatePrid(PERSON_IDENTIFIER, COUNTRY).prid();
    final NameID nameId = assertion.getSubject().getNameID();
    assertThat(nameId.getFormat()).isEqualTo(NameID.PERSISTENT);
    assertThat(nameId.getValue()).isEqualTo(saml2IdpNameId(prid));

    final AuthnStatement statement = assertion.getAuthnStatements().getFirst();
    assertThat(statement.getAuthnContext().getAuthnContextClassRef().getURI()).isEqualTo(LOA_SUB);
    assertThat(statement.getAuthnContext().getAuthenticatingAuthorities().stream()
        .map(AuthenticatingAuthority::getURI).toList())
        .containsExactly("http://id.swedenconnect.se/eidas/1.0/proxy-service/xa", FOREIGN_IDP);

    final Map<String, List<String>> attributes = assertion.getAttributeStatements().getFirst().getAttributes()
        .stream()
        .collect(Collectors.toMap(Attribute::getName, SamlAttributeValues::getStringValues));
    assertThat(attributes).containsOnly(
        Map.entry("urn:oid:2.5.4.4", List.of("Kula")),
        Map.entry("urn:oid:2.5.4.42", List.of("Kalle")),
        Map.entry("urn:oid:1.3.6.1.5.5.7.9.1", List.of("1969-03-17")),
        Map.entry("urn:oid:1.3.6.1.5.5.7.9.3", List.of("M")),
        Map.entry("urn:oid:1.3.6.1.5.5.7.9.2", List.of("Enköping, SE")),
        Map.entry("urn:oid:1.2.752.201.3.7", List.of(PERSON_IDENTIFIER)),
        Map.entry("urn:oid:1.2.752.201.3.4", List.of(prid)),
        Map.entry("urn:oid:2.5.4.6", List.of(COUNTRY)),
        Map.entry("http://eidas.europa.eu/attributes/naturalperson/TownOfBirth", List.of("Enköping")),
        Map.entry("http://eidas.europa.eu/attributes/naturalperson/CountryOfBirth", List.of("SE")),
        Map.entry("http://e-justice.europa.eu/attributes/naturalperson/eJusticeNaturalPersonRole",
            List.of("VIP1", "VIP2")));

    // The audit events of the connector belong to the same flow as those of the authentication server ...
    //
    final List<se.swedenconnect.spring.audit.AuditEvent> events = this.auditEventRepository.find(null, null, null)
        .stream()
        .filter(se.swedenconnect.spring.audit.AuditEvent.class::isInstance)
        .map(se.swedenconnect.spring.audit.AuditEvent.class::cast)
        .filter(e -> e.getCorrelationId() != null)
        .toList();
    final String correlationId = events.stream()
        .filter(e -> ConnectorAuditEvents.CONNECTOR_SUCCESS_RESPONSE.equals(e.getType()))
        .findFirst()
        .map(e -> e.getCorrelationId().getValue())
        .orElseThrow();
    final List<String> flowEvents = events.stream()
        .filter(e -> correlationId.equals(e.getCorrelationId().getValue()))
        .map(se.swedenconnect.spring.audit.AuditEvent::getType)
        .toList();
    assertThat(flowEvents).contains(
        "authn_request_received", "authn_request_accepted",
        ConnectorAuditEvents.CONNECTOR_BEFORE_SAML_REQUEST, ConnectorAuditEvents.CONNECTOR_SUCCESS_RESPONSE,
        "authn_user_authenticated", "authn_success_response", ConnectorAuditEvents.EIDAS_AUDIT_SUCCESSFUL_RESPONSE);
  }

  @Test
  void testCancelCountrySelection() throws Exception {
    final Browser session = new Browser();
    final String authnPath = this.sendAuthnRequest(session);
    final String authnId = authnPath.substring(authnPath.indexOf('=') + 1);

    final MvcResult result = this.mockMvc.perform(post(REQUEST_BASE_URL + "/extauth/proxyauth").contextPath("/idp")
        .param("authnId", authnId).param("selectedCountry", "cancel").with(session)).andDo(session).andReturn();

    final Response response = this.resume(session, result.getResponse().getRedirectedUrl());
    assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
    assertThat(response.getStatus().getStatusCode().getStatusCode().getValue())
        .isEqualTo("http://id.elegnamnden.se/status/1.0/cancel");
    assertThat(response.getStatus().getStatusMessage().getValue()).isEqualTo("User cancelled authentication");
  }

  @Test
  void testErrorFromForeignCountry() throws Exception {
    final Browser session = new Browser();
    final String authnPath = this.sendAuthnRequest(session);
    final String authnId = authnPath.substring(authnPath.indexOf('=') + 1);
    final AuthnRequest eidasRequest = this.selectCountry(session, authnId);

    final Status status = (Status) XMLObjectSupport.buildXMLObject(Status.DEFAULT_ELEMENT_NAME);
    final StatusCode code = (StatusCode) XMLObjectSupport.buildXMLObject(StatusCode.DEFAULT_ELEMENT_NAME);
    code.setValue(StatusCode.RESPONDER);
    final StatusCode subCode = (StatusCode) XMLObjectSupport.buildXMLObject(StatusCode.DEFAULT_ELEMENT_NAME);
    subCode.setValue(StatusCode.AUTHN_FAILED);
    code.setStatusCode(subCode);
    status.setStatusCode(code);

    final MvcResult foreignResponse = this.mockMvc.perform(post(REQUEST_BASE_URL + "/extauth/saml2/post")
            .contextPath("/idp").with(session)
            .param("SAMLResponse", encode(this.eidasResponse(eidasRequest, status, false)))
            .param("RelayState", "sp-relay"))
        .andDo(session).andReturn();

    final Response response = this.resume(session, foreignResponse.getResponse().getRedirectedUrl());
    assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
    assertThat(response.getStatus().getStatusCode().getStatusCode().getValue()).isEqualTo(StatusCode.AUTHN_FAILED);
    assertThat(response.getStatus().getStatusMessage().getValue())
        .isEqualTo("Failure received from foreign service");
  }

  @Test
  void testSignatureService() throws Exception {
    final Browser session = new Browser();
    final MvcResult request = this.postAuthnRequest(session, SIGN_SP_ENTITY_ID, SIGN_SP_ACS,
        signMessage(SignMessageMimeTypeEnum.TEXT, "I approve <the> contract\nof today"));
    final String authnPath = request.getResponse().getRedirectedUrl();
    final String authnId = authnPath.substring(authnPath.indexOf('=') + 1);
    final AuthnRequest eidasRequest = this.selectCountry(session, authnId);

    // After the authentication at the foreign country, the user is asked to consent to the signature ...
    //
    final MvcResult consent = this.mockMvc.perform(post(REQUEST_BASE_URL + "/extauth/saml2/post")
            .contextPath("/idp").with(session)
            .param("SAMLResponse", encode(this.eidasResponse(eidasRequest, successStatus(), true)))
            .param("RelayState", "sp-relay"))
        .andDo(session).andReturn();
    assertThat(view(consent).getViewName()).isEqualTo("sign-consent");
    assertThat(view(consent).getModel()).containsEntry("authnId", authnId);
    final SignUiModel model = (SignUiModel) view(consent).getModel().get("signMessageConsent");
    assertThat(model.getTextMessage())
        .isEqualTo("<div class=\"message-text\">I approve &lt;the&gt; contract<br />of today</div>");
    assertThat(model.getUserInfo().getName()).isEqualTo("Kalle Kula");
    assertThat(model.getUserInfo().getInternationalId()).isEqualTo(PERSON_IDENTIFIER);
    assertThat(model.getUserInfo().getDateOfBirth()).isEqualTo("1969-03-17");

    final MvcResult signed = this.mockMvc.perform(post(REQUEST_BASE_URL + "/extauth/signed").contextPath("/idp")
        .param("authnId", authnId).param("action", "ok").with(session)).andDo(session).andReturn();
    final Response response = this.resume(session, signed.getResponse().getRedirectedUrl());
    assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.SUCCESS);

    final Assertion assertion = new SAMLObjectDecrypter(SP_CREDENTIAL)
        .decrypt(response.getEncryptedAssertions().getFirst(), Assertion.class);
    assertThat(assertion.getAttributeStatements().getFirst().getAttributes().stream().map(Attribute::getName))
        .contains("urn:oid:1.2.752.201.3.14");
  }

  @Test
  void testSignatureServiceWithOtherPrincipal() throws Exception {
    final Browser session = new Browser();
    final MvcResult request = this.postAuthnRequest(session, SIGN_SP_ENTITY_ID, SIGN_SP_ACS,
        signMessage(SignMessageMimeTypeEnum.TEXT, "I approve"), null, authnRequest -> {
          final PrincipalSelection principalSelection =
              (PrincipalSelection) XMLObjectSupport.buildXMLObject(PrincipalSelection.DEFAULT_ELEMENT_NAME);
          principalSelection.getMatchValues().add(MatchValueBuilder.builder()
              .name("urn:oid:1.2.752.201.3.7").value("XA/SE/0000000000").build());
          authnRequest.getExtensions().getUnknownXMLObjects().add(principalSelection);
        });
    final String authnId = authnId(request);
    final AuthnRequest eidasRequest = this.selectCountry(session, authnId);

    // The user does not match the requested principal, so no sign message is displayed ...
    //
    final MvcResult foreignResponse = this.mockMvc.perform(post(REQUEST_BASE_URL + "/extauth/saml2/post")
            .contextPath("/idp").with(session)
            .param("SAMLResponse", encode(this.eidasResponse(eidasRequest, successStatus(), true)))
            .param("RelayState", "sp-relay"))
        .andDo(session).andReturn();
    assertThat(foreignResponse.getResponse().getRedirectedUrl()).isEqualTo("/idp/resume?authnId=" + authnId);

    final Response response = this.resume(session, foreignResponse.getResponse().getRedirectedUrl());
    assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.REQUESTER);
    assertThat(response.getStatus().getStatusCode().getStatusCode().getValue())
        .isEqualTo(StatusCode.UNKNOWN_PRINCIPAL);
  }

  @Test
  void testSignatureRejected() throws Exception {
    final Browser session = new Browser();
    final MvcResult request = this.postAuthnRequest(session, SIGN_SP_ENTITY_ID, SIGN_SP_ACS,
        signMessage(SignMessageMimeTypeEnum.TEXT, "I approve"));
    final String authnPath = request.getResponse().getRedirectedUrl();
    final String authnId = authnPath.substring(authnPath.indexOf('=') + 1);
    final AuthnRequest eidasRequest = this.selectCountry(session, authnId);
    this.mockMvc.perform(post(REQUEST_BASE_URL + "/extauth/saml2/post").contextPath("/idp").with(session)
            .param("SAMLResponse", encode(this.eidasResponse(eidasRequest, successStatus(), true)))
            .param("RelayState", "sp-relay"))
        .andDo(session).andReturn();

    final MvcResult rejected = this.mockMvc.perform(post(REQUEST_BASE_URL + "/extauth/signed").contextPath("/idp")
        .param("authnId", authnId).param("action", "cancel").with(session)).andDo(session).andReturn();
    final Response response = this.resume(session, rejected.getResponse().getRedirectedUrl());
    assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
    assertThat(response.getStatus().getStatusCode().getStatusCode().getValue())
        .isEqualTo("http://id.elegnamnden.se/status/1.0/cancel");
    assertThat(response.getStatus().getStatusMessage().getValue()).isEqualTo("User did not consent to signature");
  }

  @Test
  void testHtmlSignMessageIsRefused() throws Exception {
    final Browser session = new Browser();
    final MvcResult result = this.postAuthnRequest(session, SIGN_SP_ENTITY_ID, SIGN_SP_ACS,
        signMessage(SignMessageMimeTypeEnum.TEXT_HTML, "<p>I approve</p>"));
    final Response response = toResponse(result);
    assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.REQUESTER);
    assertThat(response.getStatus().getStatusCode().getStatusCode().getValue())
        .isEqualTo(StatusCode.REQUEST_UNSUPPORTED);
    assertThat(response.getStatus().getStatusMessage().getValue()).isEqualTo("Invalid SignMessage extension");
  }

  @Test
  void testUserMessageOnCountrySelection() throws Exception {
    final Browser session = new Browser();
    final String authnId = authnId(this.postAuthnRequest(session, SP_ENTITY_ID, SP_ACS, null,
        userMessage("text/markdown", Map.of("en", "Hello **you**", "sv", "Hej **du**")), r -> {
        }));

    final MvcResult countrySelect = this.mockMvc.perform(get(REQUEST_BASE_URL + "/extauth").contextPath("/idp")
        .param("authnId", authnId).with(session)).andDo(session).andReturn();
    assertThat(view(countrySelect).getViewName()).isEqualTo("country-select");
    assertThat(((EidasUiModel) view(countrySelect).getModel().get("ui")).getUserMessage())
        .isEqualTo("<div class=\"message-markdown\"><p>Hello <strong>you</strong></p></div>");
    assertThat(countrySelect.getResponse().getContentAsString(StandardCharsets.UTF_8))
        .contains("id=\"userMessage\"", "Hello <strong>you</strong>");

    // The user changes language ...
    //
    final MvcResult swedish = this.mockMvc.perform(get(REQUEST_BASE_URL + "/extauth").contextPath("/idp")
        .param("authnId", authnId).param("lang", "sv").with(session)).andDo(session).andReturn();
    assertThat(((EidasUiModel) view(swedish).getModel().get("ui")).getUserMessage()).contains("Hej");

    this.selectCountry(session, authnId);
  }

  @Test
  void testUserMessagePageForRequestedCountry() throws Exception {
    final Browser session = new Browser();
    final String authnId = authnId(this.postAuthnRequest(session, SP_ENTITY_ID, SP_ACS, null,
        userMessage("text/plain", "Hello\nyou"), requestCountry(COUNTRY)));

    final MvcResult page = this.mockMvc.perform(get(REQUEST_BASE_URL + "/extauth").contextPath("/idp")
        .param("authnId", authnId).with(session)).andDo(session).andReturn();
    assertThat(view(page).getViewName()).isEqualTo("user-message");
    assertThat(view(page).getModel()).containsEntry("authnId", authnId).containsEntry("selectedCountry", COUNTRY);
    assertThat(((EidasUiModel) view(page).getModel().get("ui")).getUserMessage())
        .isEqualTo("<div class=\"message-text\">Hello<br />you</div>");
    assertThat(page.getResponse().getContentAsString(StandardCharsets.UTF_8))
        .contains("id=\"userMessage\"", "Hello<br />you", "name=\"selectedCountry\" value=\"XA\"",
            "value=\"cancel\"");

    // Continue goes on to the requested country ...
    //
    this.selectCountry(session, authnId);
  }

  @Test
  void testUserMessagePageCancel() throws Exception {
    final Browser session = new Browser();
    final String authnId = authnId(this.postAuthnRequest(session, SP_ENTITY_ID, SP_ACS, null,
        userMessage("text/plain", "Hello"), requestCountry(COUNTRY)));
    final MvcResult page = this.mockMvc.perform(get(REQUEST_BASE_URL + "/extauth").contextPath("/idp")
        .param("authnId", authnId).with(session)).andDo(session).andReturn();
    assertThat(view(page).getViewName()).isEqualTo("user-message");

    final MvcResult result = this.mockMvc.perform(post(REQUEST_BASE_URL + "/extauth/proxyauth").contextPath("/idp")
        .param("authnId", authnId).param("selectedCountry", "cancel").with(session)).andDo(session).andReturn();
    final Response response = this.resume(session, result.getResponse().getRedirectedUrl());
    assertThat(response.getStatus().getStatusCode().getStatusCode().getValue())
        .isEqualTo("http://id.elegnamnden.se/status/1.0/cancel");
  }

  @Test
  void testNoUserMessagePageForRequestedCountry() throws Exception {
    final Browser session = new Browser();
    final String authnId = authnId(this.postAuthnRequest(session, SP_ENTITY_ID, SP_ACS, null, null,
        requestCountry(COUNTRY)));
    final MvcResult result = this.mockMvc.perform(get(REQUEST_BASE_URL + "/extauth").contextPath("/idp")
        .param("authnId", authnId).with(session)).andDo(session).andReturn();
    assertThat(view(result).getViewName()).isEqualTo("post-request");
  }

  @Test
  void testUserMessagePageForSignatureService() throws Exception {
    final Browser session = new Browser();

    // A first request from the signature service, where the user selects a country ...
    //
    final String firstAuthnId = authnId(this.postAuthnRequest(session, SIGN_SP_ENTITY_ID, SIGN_SP_ACS,
        signMessage(SignMessageMimeTypeEnum.TEXT, "I approve")));
    this.selectCountry(session, firstAuthnId);

    // The next request holds a user message. The country is known from the session, so the message gets a page of
    // its own ...
    //
    final String authnId = authnId(this.postAuthnRequest(session, SIGN_SP_ENTITY_ID, SIGN_SP_ACS, null,
        userMessage("text/plain", "Hello"), r -> {
        }));
    final MvcResult page = this.mockMvc.perform(get(REQUEST_BASE_URL + "/extauth").contextPath("/idp")
        .param("authnId", authnId).with(session)).andDo(session).andReturn();
    assertThat(view(page).getViewName()).isEqualTo("user-message");
    assertThat(view(page).getModel()).containsEntry("selectedCountry", COUNTRY);
  }

  @Test
  void testHtmlUserMessageIsNotDisplayed() throws Exception {
    final Browser session = new Browser();
    final String authnId = authnId(this.postAuthnRequest(session, SP_ENTITY_ID, SP_ACS, null,
        userMessage("text/html", "<p>Hello</p>"), requestCountry(COUNTRY)));

    // No message page, the user is sent to the country ...
    //
    final MvcResult result = this.mockMvc.perform(get(REQUEST_BASE_URL + "/extauth").contextPath("/idp")
        .param("authnId", authnId).with(session)).andDo(session).andReturn();
    assertThat(view(result).getViewName()).isEqualTo("post-request");
  }

  @Test
  void testUserMessageAndSignMessageIsRefused() throws Exception {
    final Browser session = new Browser();
    final MvcResult result = this.postAuthnRequest(session, SIGN_SP_ENTITY_ID, SIGN_SP_ACS,
        signMessage(SignMessageMimeTypeEnum.TEXT, "I approve"), userMessage("text/plain", "Hello"), r -> {
        });
    final Response response = toResponse(result);
    assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.REQUESTER);
    assertThat(response.getStatus().getStatusCode().getStatusCode().getValue())
        .isEqualTo(StatusCode.REQUEST_UNSUPPORTED);
  }

  @Test
  void testPassiveRequestWithUserMessage() throws Exception {
    final Browser session = new Browser();
    final MvcResult result = this.postAuthnRequest(session, SP_ENTITY_ID, SP_ACS, null,
        userMessage("text/plain", "Hello"), r -> r.setIsPassive(true));

    // No page is shown for a passive request ...
    //
    final Response response = toResponse(result);
    assertThat(response.getStatus().getStatusCode().getStatusCode().getValue()).isEqualTo(StatusCode.NO_PASSIVE);
  }

  @Test
  void testSsoForSameServiceProvider() throws Exception {
    final Browser session = new Browser();
    this.authenticate(session);

    final MvcResult result = this.postAuthnRequest(session, SP_ENTITY_ID, SP_ACS, null, null,
        r -> r.setIsPassive(true));
    assertThat(toResponse(result).getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.SUCCESS);
  }

  @Test
  void testNoSsoForOtherServiceProvider() throws Exception {
    final Browser session = new Browser();
    this.authenticate(session);

    final MvcResult result = this.postAuthnRequest(session, OTHER_SP_ENTITY_ID, OTHER_SP_ACS, null, null,
        r -> r.setIsPassive(true));
    assertThat(toResponse(result).getStatus().getStatusCode().getStatusCode().getValue())
        .isEqualTo(StatusCode.NO_PASSIVE);
  }

  @Test
  void testNoSsoForOtherAttributes() throws Exception {
    final Browser session = new Browser();
    this.authenticate(session);

    // The same SP asks for fewer attributes ...
    //
    final MvcResult result = this.postAuthnRequest(session, SP_ENTITY_ID, SP_ACS, null, null, r -> {
      r.setIsPassive(true);
      final RequestedAttributes attributes = (RequestedAttributes) r.getExtensions()
          .getUnknownXMLObjects(RequestedAttributes.DEFAULT_ELEMENT_NAME).getFirst();
      attributes.getRequestedAttributes().removeIf(a -> a.getName().contains("eJusticeNaturalPersonRole"));
    });
    assertThat(toResponse(result).getStatus().getStatusCode().getStatusCode().getValue())
        .isEqualTo(StatusCode.NO_PASSIVE);
  }

  @Test
  void testMarkdownSignMessage() throws Exception {
    final Browser session = new Browser();
    final String authnId = authnId(this.postAuthnRequest(session, SIGN_SP_ENTITY_ID, SIGN_SP_ACS,
        signMessage(SignMessageMimeTypeEnum.TEXT_MARKDOWN, "I *approve* <b>the</b> contract")));
    final AuthnRequest eidasRequest = this.selectCountry(session, authnId);

    final MvcResult consent = this.mockMvc.perform(post(REQUEST_BASE_URL + "/extauth/saml2/post")
            .contextPath("/idp").with(session)
            .param("SAMLResponse", encode(this.eidasResponse(eidasRequest, successStatus(), true)))
            .param("RelayState", "sp-relay"))
        .andDo(session).andReturn();
    final SignUiModel model = (SignUiModel) view(consent).getModel().get("signMessageConsent");
    assertThat(model.getTextMessage())
        .isEqualTo("<div class=\"message-markdown\"><p>I <em>approve</em> the contract</p></div>");
  }

  @Test
  void testMetadataDeclaresUserMessageSupport() throws Exception {
    final MvcResult result = this.mockMvc.perform(get(REQUEST_BASE_URL + "/metadata/idp.xml").contextPath("/idp"))
        .andReturn();
    assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
        .contains(EntityCategoryConstants.GENERAL_CATEGORY_SUPPORTS_USER_MESSAGE.getUri());
  }

  @Test
  void testCountryNotRequestedIsRejected() throws Exception {
    final Browser session = new Browser();
    final String authnId = authnId(this.postAuthnRequest(session, SP_ENTITY_ID, SP_ACS, null, null,
        requestCountry(COUNTRY)));

    final MvcResult result = this.postCountry(session, authnId, OTHER_COUNTRY);
    assertRequestDenied(this.resume(session, result.getResponse().getRedirectedUrl()));
  }

  @Test
  void testCountryThatCannotAuthenticateIsRejected() throws Exception {
    final Browser session = new Browser();
    final String authnId = authnId(this.postAuthnRequest(session, SP_ENTITY_ID, SP_ACS, null));

    final MvcResult result = this.postCountry(session, authnId, LOW_COUNTRY);
    assertRequestDenied(this.resume(session, result.getResponse().getRedirectedUrl()));
  }

  @Test
  void testUnknownCountryIsRejected() throws Exception {
    final Browser session = new Browser();
    final String authnId = authnId(this.postAuthnRequest(session, SP_ENTITY_ID, SP_ACS, null));

    final MvcResult result = this.postCountry(session, authnId, "ZZ");
    assertRequestDenied(this.resume(session, result.getResponse().getRedirectedUrl()));
  }

  @Test
  void testSessionCountryNotRequestedIsIgnored() throws Exception {
    final Browser session = new Browser();

    // A first request from the signature service, where the user selects another country ...
    //
    final String firstAuthnId = authnId(this.postAuthnRequest(session, SIGN_SP_ENTITY_ID, SIGN_SP_ACS,
        signMessage(SignMessageMimeTypeEnum.TEXT, "I approve")));
    final MvcResult first = this.postCountry(session, firstAuthnId, OTHER_COUNTRY);
    assertThat(view(first).getModel()).containsEntry("action", OTHER_COUNTRY_SSO);

    // The next request asks for a country. The country from the session is not used ...
    //
    final String authnId = authnId(this.postAuthnRequest(session, SIGN_SP_ENTITY_ID, SIGN_SP_ACS,
        signMessage(SignMessageMimeTypeEnum.TEXT, "I approve"), null, requestCountry(COUNTRY)));
    final MvcResult result = this.mockMvc.perform(get(REQUEST_BASE_URL + "/extauth").contextPath("/idp")
        .param("authnId", authnId).with(session)).andDo(session).andReturn();
    assertThat(view(result).getViewName()).isEqualTo("post-request");
    assertThat(view(result).getModel()).containsEntry("action", FOREIGN_SSO);
  }

  @Test
  void testSessionCountryThatCannotAuthenticateIsIgnored() throws Exception {
    final Browser session = new Browser();
    session.cookies.put("selectedCountrySession", new Cookie("selectedCountrySession", LOW_COUNTRY));

    final String authnId = authnId(this.postAuthnRequest(session, SIGN_SP_ENTITY_ID, SIGN_SP_ACS,
        signMessage(SignMessageMimeTypeEnum.TEXT, "I approve")));
    final MvcResult result = this.mockMvc.perform(get(REQUEST_BASE_URL + "/extauth").contextPath("/idp")
        .param("authnId", authnId).with(session)).andDo(session).andReturn();
    assertThat(view(result).getViewName()).isEqualTo("country-select");
  }

  private static void assertRequestDenied(final Response response) {
    assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.REQUESTER);
    assertThat(response.getStatus().getStatusCode().getStatusCode().getValue()).isEqualTo(StatusCode.REQUEST_DENIED);
  }

  @Test
  void testResponseWithoutSession() throws Exception {
    final MvcResult result = this.mockMvc.perform(post(REQUEST_BASE_URL + "/extauth/saml2/post").contextPath("/idp")
        .param("SAMLResponse", "foo")).andReturn();
    assertThat(view(result).getViewName()).isEqualTo("error/idp");
    assertThat(view(result).getModel())
        .containsEntry("idpErrorMessageCode", "authn-server.error.unrecoverable.session");
  }

}
