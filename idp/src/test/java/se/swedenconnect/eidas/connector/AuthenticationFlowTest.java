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

import net.shibboleth.shared.xml.SerializeSupport;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthenticatingAuthority;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.x509.BasicX509Credential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import jakarta.servlet.http.Cookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.ModelAndView;
import se.swedenconnect.eidas.connector.audit.ConnectorAuditEvents;
import se.swedenconnect.eidas.connector.authn.ui.SignUiModel;
import se.swedenconnect.eidas.connector.prid.service.PridService;
import se.swedenconnect.opensaml.OpenSAMLInitializer;
import se.swedenconnect.opensaml.OpenSAMLSecurityDefaultsConfig;
import se.swedenconnect.opensaml.OpenSAMLSecurityExtensionConfig;
import se.swedenconnect.opensaml.eidas.ext.NodeCountry;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttribute;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttributes;
import se.swedenconnect.opensaml.eidas.ext.attributes.CountryOfBirthType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentFamilyNameType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentGivenNameType;
import se.swedenconnect.opensaml.eidas.ext.attributes.DateOfBirthType;
import se.swedenconnect.opensaml.eidas.ext.attributes.GenderType;
import se.swedenconnect.opensaml.eidas.ext.attributes.GenderTypeEnumeration;
import se.swedenconnect.opensaml.eidas.ext.attributes.PersonIdentifierType;
import se.swedenconnect.opensaml.saml2.attribute.AttributeBuilder;
import se.swedenconnect.opensaml.saml2.core.build.AuthnRequestBuilder;
import se.swedenconnect.opensaml.saml2.core.build.NameIDPolicyBuilder;
import se.swedenconnect.opensaml.saml2.core.build.RequestedAuthnContextBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.AssertionConsumerServiceBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.AttributeConsumingServiceBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.EntityAttributesBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.EntityDescriptorBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.ExtensionsBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.IDPSSODescriptorBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.KeyDescriptorBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.RequestedAttributeBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.SPSSODescriptorBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.SingleSignOnServiceBuilder;
import se.swedenconnect.opensaml.sweid.saml2.metadata.entitycategory.EntityCategoryConstants;
import se.swedenconnect.opensaml.sweid.saml2.signservice.dss.Message;
import se.swedenconnect.opensaml.sweid.saml2.signservice.dss.SignMessage;
import se.swedenconnect.opensaml.sweid.saml2.signservice.dss.SignMessageMimeTypeEnum;
import se.swedenconnect.opensaml.sweid.xmlsec.config.SwedishEidSecurityConfiguration;
import se.swedenconnect.opensaml.xmlsec.encryption.support.SAMLObjectDecrypter;
import se.swedenconnect.opensaml.xmlsec.encryption.support.SAMLObjectEncrypter;
import se.swedenconnect.opensaml.xmlsec.signature.support.SAMLObjectSigner;
import se.swedenconnect.spring.authnserver.saml.attributes.SamlAttributeValues;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Runs an authentication through the connector: a Swedish SP sends an authentication request, the user selects a
 * country, the connector sends an eIDAS authentication request, the foreign country answers, and the connector posts a
 * SAML response to the Swedish SP.
 * <p>
 * The connector runs with its base configuration and a test profile. Keys and metadata are generated by the test.
 * </p>
 *
 * @author Martin Lindström
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("flowtest")
class AuthenticationFlowTest {

  private static final String BASE_URL = "https://www.example.com/idp";

  /** MockMvc needs the port to treat the request as received on port 443. */
  private static final String REQUEST_BASE_URL = "https://www.example.com:443/idp";

  private static final String IDP_ENTITY_ID = "https://connector.example.com/eidas";

  private static final String CONNECTOR_SP_ENTITY_ID = BASE_URL + "/metadata/sp";

  private static final String SP_ENTITY_ID = "https://sp.example.com";

  private static final String SP_ACS = "https://sp.example.com/acs";

  private static final String SIGN_SP_ENTITY_ID = "https://sign.example.com";

  private static final String SIGN_SP_ACS = "https://sign.example.com/acs";

  private static final String FOREIGN_IDP = "https://idp.xa.example.com";

  private static final String FOREIGN_SSO = "https://idp.xa.example.com/sso";

  private static final String COUNTRY = "XA";

  private static final String PERSON_IDENTIFIER = "XA/SE/1234567890";

  private static final String LOA_SUB = "http://id.elegnamnden.se/loa/1.0/eidas-sub";

  private static final String EIDAS_SUBSTANTIAL = "http://eidas.europa.eu/LoA/substantial";

  private static final Path DIRECTORY;

  private static final BasicX509Credential SP_CREDENTIAL;

  private static final BasicX509Credential FOREIGN_IDP_CREDENTIAL;

  static {
    try {
      final OpenSAMLInitializer initializer = OpenSAMLInitializer.getInstance();
      if (!initializer.isInitialized()) {
        initializer.initialize(new OpenSAMLSecurityDefaultsConfig(new SwedishEidSecurityConfiguration()),
            new OpenSAMLSecurityExtensionConfig());
      }
      DIRECTORY = Files.createTempDirectory("connector-flow-test");
      Files.createDirectories(DIRECTORY.resolve("backup"));

      // Connector keys ...
      final KeyPair connectorKey = generateKey();
      final X509Certificate connectorCert = selfSigned(connectorKey, "Connector");
      final KeyStore keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(null, null);
      keyStore.setKeyEntry("connector", connectorKey.getPrivate(), "secret".toCharArray(),
          new Certificate[] { connectorCert });
      try (final OutputStream os = Files.newOutputStream(DIRECTORY.resolve("connector.p12"))) {
        keyStore.store(os, "secret".toCharArray());
      }

      // The Swedish SP and the foreign IdP ...
      final KeyPair spKey = generateKey();
      SP_CREDENTIAL = new BasicX509Credential(selfSigned(spKey, "SP"), spKey.getPrivate());
      final KeyPair idpKey = generateKey();
      FOREIGN_IDP_CREDENTIAL = new BasicX509Credential(selfSigned(idpKey, "Foreign IdP"), idpKey.getPrivate());

      write(DIRECTORY.resolve("sp-metadata.xml"), spMetadata());
      write(DIRECTORY.resolve("eu-metadata.xml"), euMetadata());
      Files.writeString(DIRECTORY.resolve("prid-policy.yml"), """
          policy:
            XA:
              algorithm: test-eIDAS
              persistenceClass: A
          """);
    }
    catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @DynamicPropertySource
  static void properties(final DynamicPropertyRegistry registry) {
    registry.add("credential.bundles.keystore.connector-ks.location",
        () -> "file:" + DIRECTORY.resolve("connector.p12"));
    registry.add("credential.bundles.keystore.connector-ks.password", () -> "secret");
    registry.add("credential.bundles.keystore.connector-ks.type", () -> "PKCS12");
    registry.add("credential.bundles.jks.connector.store-reference", () -> "connector-ks");
    registry.add("credential.bundles.jks.connector.key.alias", () -> "connector");
    registry.add("credential.bundles.jks.connector.key.key-password", () -> "secret");
    registry.add("connector.backup-directory", () -> DIRECTORY.resolve("backup").toString());
    registry.add("connector.eu-metadata.location", () -> "file:" + DIRECTORY.resolve("eu-metadata.xml"));
    registry.add("connector.prid.policy-resource", () -> "file:" + DIRECTORY.resolve("prid-policy.yml"));
    registry.add("authn-server.saml.metadata-providers[0].location",
        () -> "file:" + DIRECTORY.resolve("sp-metadata.xml"));
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  @Qualifier("connector.sp.metadata")
  private EntityDescriptor connectorSpMetadata;

  @Autowired
  private PridService pridService;

  @Autowired
  private AuditEventRepository auditEventRepository;

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
    assertThat(model.getTextMessage()).isEqualTo("I approve &lt;the&gt; contract<br />of today");
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
  void testResponseWithoutSession() throws Exception {
    final MvcResult result = this.mockMvc.perform(post(REQUEST_BASE_URL + "/extauth/saml2/post").contextPath("/idp")
        .param("SAMLResponse", "foo")).andReturn();
    assertThat(view(result).getViewName()).isEqualTo("error/idp");
    assertThat(view(result).getModel())
        .containsEntry("idpErrorMessageCode", "authn-server.error.unrecoverable.session");
  }

  private String sendAuthnRequest(final Browser session) throws Exception {
    final MvcResult result = this.postAuthnRequest(session, SP_ENTITY_ID, SP_ACS, null);
    return Objects.requireNonNull(result.getResponse().getRedirectedUrl(), "Expected redirect - "
        + result.getResponse().getStatus() + " " + result.getResponse().getContentAsString());
  }

  private MvcResult postAuthnRequest(final Browser session, final String issuer, final String acs,
      final SignMessage signMessage) throws Exception {
    final RequestedAttributes eidasAttributes =
        (RequestedAttributes) XMLObjectSupport.buildXMLObject(RequestedAttributes.DEFAULT_ELEMENT_NAME);
    for (final String name : List.of("http://eidas.europa.eu/attributes/naturalperson/TownOfBirth",
        "http://eidas.europa.eu/attributes/naturalperson/CountryOfBirth",
        "http://e-justice.europa.eu/attributes/naturalperson/eJusticeNaturalPersonRole")) {
      final RequestedAttribute attribute =
          (RequestedAttribute) XMLObjectSupport.buildXMLObject(RequestedAttribute.DEFAULT_ELEMENT_NAME);
      attribute.setName(name);
      attribute.setIsRequired(false);
      eidasAttributes.getRequestedAttributes().add(attribute);
    }
    final se.swedenconnect.opensaml.saml2.core.build.ExtensionsBuilder extensions =
        se.swedenconnect.opensaml.saml2.core.build.ExtensionsBuilder.builder().extension(eidasAttributes);
    if (signMessage != null) {
      extensions.extension(signMessage);
    }

    final AuthnRequest authnRequest = AuthnRequestBuilder.builder()
        .id("_" + UUID.randomUUID())
        .issueInstant(Instant.now())
        .destination(BASE_URL + "/profile/SAML2/POST/SSO")
        .issuer(issuer)
        .postProtocolBinding()
        .assertionConsumerServiceURL(acs)
        .nameIDPolicy(NameIDPolicyBuilder.builder().format(NameID.PERSISTENT).allowCreate(true).build())
        .requestedAuthnContext(RequestedAuthnContextBuilder.builder()
            .comparison(AuthnContextComparisonTypeEnumeration.EXACT)
            .authnContextClassRefs(LOA_SUB)
            .build())
        .extensions(extensions.build())
        .build();
    SAMLObjectSigner.sign(authnRequest, SP_CREDENTIAL);

    return this.mockMvc.perform(post(REQUEST_BASE_URL + "/profile/SAML2/POST/SSO").contextPath("/idp")
            .param("SAMLRequest", encode(authnRequest)).param("RelayState", "sp-relay").with(session))
        .andDo(session).andReturn();
  }

  private static SignMessage signMessage(final SignMessageMimeTypeEnum mimeType, final String text) {
    final SignMessage signMessage = (SignMessage) XMLObjectSupport.buildXMLObject(SignMessage.DEFAULT_ELEMENT_NAME);
    signMessage.setMimeType(mimeType);
    signMessage.setMustShow(true);
    final Message message = (Message) XMLObjectSupport.buildXMLObject(Message.DEFAULT_ELEMENT_NAME);
    message.setContent(text);
    signMessage.setMessage(message);
    return signMessage;
  }

  private static Response toResponse(final MvcResult result) throws Exception {
    final String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    final Matcher matcher = Pattern.compile("name=\"SAMLResponse\" value=\"([^\"]+)\"").matcher(html);
    assertThat(matcher.find()).as("Expected SAML response in %s", html).isTrue();
    return (Response) XMLObjectSupport.unmarshallFromInputStream(
        Objects.requireNonNull(org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport.getParserPool()),
        new ByteArrayInputStream(Base64.getDecoder().decode(matcher.group(1))));
  }

  private AuthnRequest selectCountry(final Browser session, final String authnId) throws Exception {
    final MvcResult result = this.mockMvc.perform(post(REQUEST_BASE_URL + "/extauth/proxyauth").contextPath("/idp")
        .param("authnId", authnId).param("selectedCountry", COUNTRY).with(session)).andDo(session).andReturn();
    assertThat(view(result).getViewName()).isEqualTo("post-request");
    assertThat(view(result).getModel()).containsEntry("action", FOREIGN_SSO);
    return (AuthnRequest) XMLObjectSupport.unmarshallFromInputStream(
        Objects.requireNonNull(org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport.getParserPool()),
        new ByteArrayInputStream(Base64.getDecoder().decode((String) view(result).getModel().get("SAMLRequest"))));
  }

  private Response resume(final Browser session, final String resumePath) throws Exception {
    return toResponse(this.mockMvc.perform(get("https://www.example.com:443" + resumePath).contextPath("/idp")
        .with(session)).andDo(session).andReturn());
  }

  private Response eidasResponse(final AuthnRequest eidasRequest, final Status status, final boolean withAssertion)
      throws Exception {
    final Instant now = Instant.now();

    final Response response = (Response) XMLObjectSupport.buildXMLObject(Response.DEFAULT_ELEMENT_NAME);
    response.setID("_" + UUID.randomUUID());
    response.setInResponseTo(eidasRequest.getID());
    response.setIssueInstant(now);
    response.setDestination(BASE_URL + "/extauth/saml2/post");
    response.setIssuer(issuer());
    response.setStatus(status);

    if (withAssertion) {
      final Assertion assertion = (Assertion) XMLObjectSupport.buildXMLObject(Assertion.DEFAULT_ELEMENT_NAME);
      assertion.setID("_" + UUID.randomUUID());
      assertion.setIssueInstant(now);
      assertion.setIssuer(issuer());

      final Subject subject = (Subject) XMLObjectSupport.buildXMLObject(Subject.DEFAULT_ELEMENT_NAME);
      final NameID nameId = (NameID) XMLObjectSupport.buildXMLObject(NameID.DEFAULT_ELEMENT_NAME);
      nameId.setFormat(NameID.TRANSIENT);
      nameId.setValue("_transient");
      subject.setNameID(nameId);
      final SubjectConfirmation confirmation =
          (SubjectConfirmation) XMLObjectSupport.buildXMLObject(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
      confirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
      final SubjectConfirmationData data =
          (SubjectConfirmationData) XMLObjectSupport.buildXMLObject(SubjectConfirmationData.DEFAULT_ELEMENT_NAME);
      data.setRecipient(BASE_URL + "/extauth/saml2/post");
      data.setInResponseTo(eidasRequest.getID());
      data.setNotOnOrAfter(now.plus(5, ChronoUnit.MINUTES));
      data.setAddress("127.0.0.1");
      confirmation.setSubjectConfirmationData(data);
      subject.getSubjectConfirmations().add(confirmation);
      assertion.setSubject(subject);

      final Conditions conditions = (Conditions) XMLObjectSupport.buildXMLObject(Conditions.DEFAULT_ELEMENT_NAME);
      conditions.setNotBefore(now.minus(1, ChronoUnit.MINUTES));
      conditions.setNotOnOrAfter(now.plus(5, ChronoUnit.MINUTES));
      final AudienceRestriction restriction =
          (AudienceRestriction) XMLObjectSupport.buildXMLObject(AudienceRestriction.DEFAULT_ELEMENT_NAME);
      final Audience audience = (Audience) XMLObjectSupport.buildXMLObject(Audience.DEFAULT_ELEMENT_NAME);
      audience.setURI(CONNECTOR_SP_ENTITY_ID);
      restriction.getAudiences().add(audience);
      conditions.getAudienceRestrictions().add(restriction);
      assertion.setConditions(conditions);

      final AuthnStatement statement =
          (AuthnStatement) XMLObjectSupport.buildXMLObject(AuthnStatement.DEFAULT_ELEMENT_NAME);
      statement.setAuthnInstant(now);
      final AuthnContext context = (AuthnContext) XMLObjectSupport.buildXMLObject(AuthnContext.DEFAULT_ELEMENT_NAME);
      final AuthnContextClassRef classRef =
          (AuthnContextClassRef) XMLObjectSupport.buildXMLObject(AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
      classRef.setURI(EIDAS_SUBSTANTIAL);
      context.setAuthnContextClassRef(classRef);
      statement.setAuthnContext(context);
      assertion.getAuthnStatements().add(statement);

      final AttributeStatement attributeStatement =
          (AttributeStatement) XMLObjectSupport.buildXMLObject(AttributeStatement.DEFAULT_ELEMENT_NAME);
      attributeStatement.getAttributes().addAll(eidasAttributes());
      assertion.getAttributeStatements().add(attributeStatement);

      final EncryptedAssertion encryptedAssertion =
          (EncryptedAssertion) XMLObjectSupport.buildXMLObject(EncryptedAssertion.DEFAULT_ELEMENT_NAME);
      encryptedAssertion.setEncryptedData(new SAMLObjectEncrypter()
          .encrypt(assertion, new SAMLObjectEncrypter.Peer(this.connectorSpMetadata)));
      response.getEncryptedAssertions().add(encryptedAssertion);
    }
    SAMLObjectSigner.sign(response, FOREIGN_IDP_CREDENTIAL);
    return response;
  }

  private static List<Attribute> eidasAttributes() {
    final PersonIdentifierType personIdentifier =
        AttributeBuilder.createValueObject(PersonIdentifierType.TYPE_NAME, PersonIdentifierType.class);
    personIdentifier.setValue(PERSON_IDENTIFIER);
    final CurrentGivenNameType givenName =
        AttributeBuilder.createValueObject(CurrentGivenNameType.TYPE_NAME, CurrentGivenNameType.class);
    givenName.setValue("Kalle");
    final CurrentFamilyNameType familyName =
        AttributeBuilder.createValueObject(CurrentFamilyNameType.TYPE_NAME, CurrentFamilyNameType.class);
    familyName.setValue("Kula");
    final DateOfBirthType dateOfBirth =
        AttributeBuilder.createValueObject(DateOfBirthType.TYPE_NAME, DateOfBirthType.class);
    dateOfBirth.setDate(LocalDate.of(1969, 3, 17));
    final GenderType gender = AttributeBuilder.createValueObject(GenderType.TYPE_NAME, GenderType.class);
    gender.setGender(GenderTypeEnumeration.MALE);
    final CountryOfBirthType countryOfBirth =
        AttributeBuilder.createValueObject(CountryOfBirthType.TYPE_NAME, CountryOfBirthType.class);
    countryOfBirth.setValue("SE");

    return List.of(
        AttributeBuilder.builder("http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier")
            .value(personIdentifier).build(),
        AttributeBuilder.builder("http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName")
            .value(givenName).build(),
        AttributeBuilder.builder("http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName")
            .value(familyName).build(),
        AttributeBuilder.builder("http://eidas.europa.eu/attributes/naturalperson/DateOfBirth")
            .value(dateOfBirth).build(),
        AttributeBuilder.builder("http://eidas.europa.eu/attributes/naturalperson/Gender").value(gender).build(),
        AttributeBuilder.builder("http://eidas.europa.eu/attributes/naturalperson/TownOfBirth")
            .value("Enköping").build(),
        AttributeBuilder.builder("http://eidas.europa.eu/attributes/naturalperson/CountryOfBirth")
            .value(countryOfBirth).build(),
        AttributeBuilder.builder("http://e-justice.europa.eu/attributes/naturalperson/eJusticeNaturalPersonRole")
            .value("VIP1").value("VIP2").build());
  }

  private static Status successStatus() {
    final Status status = (Status) XMLObjectSupport.buildXMLObject(Status.DEFAULT_ELEMENT_NAME);
    final StatusCode code = (StatusCode) XMLObjectSupport.buildXMLObject(StatusCode.DEFAULT_ELEMENT_NAME);
    code.setValue(StatusCode.SUCCESS);
    status.setStatusCode(code);
    return status;
  }

  private static Issuer issuer() {
    final Issuer issuer = (Issuer) XMLObjectSupport.buildXMLObject(Issuer.DEFAULT_ELEMENT_NAME);
    issuer.setValue(FOREIGN_IDP);
    issuer.setFormat(Issuer.ENTITY);
    return issuer;
  }

  private static ModelAndView view(final MvcResult result) {
    return Objects.requireNonNull(result.getModelAndView(), "Expected a view - " + result.getResponse().getStatus());
  }

  /**
   * The computation of the persistent NameID by saml-identity-provider, the library that version 2.1.0 of the
   * connector was built on.
   */
  private static String saml2IdpNameId(final String userId) throws Exception {
    final MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(SP_ENTITY_ID.getBytes(StandardCharsets.UTF_8));
    md.update((byte) '!');
    md.update(IDP_ENTITY_ID.getBytes(StandardCharsets.UTF_8));
    md.update((byte) '!');
    md.update(userId.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(md.digest());
  }

  private static String encode(final XMLObject object) throws Exception {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    SerializeSupport.writeNode(XMLObjectSupport.marshall(object), bos);
    return Base64.getEncoder().encodeToString(bos.toByteArray());
  }

  private static void write(final Path path, final XMLObject object) throws Exception {
    try (final OutputStream os = Files.newOutputStream(path)) {
      SerializeSupport.writeNode(XMLObjectSupport.marshall(object), os);
    }
  }

  private static EntitiesDescriptor spMetadata() throws Exception {
    final EntitiesDescriptor entities =
        (EntitiesDescriptor) XMLObjectSupport.buildXMLObject(EntitiesDescriptor.DEFAULT_ELEMENT_NAME);
    entities.getEntityDescriptors().add(spMetadata(SP_ENTITY_ID, SP_ACS, null));
    entities.getEntityDescriptors().add(spMetadata(SIGN_SP_ENTITY_ID, SIGN_SP_ACS,
        EntityCategoryConstants.SERVICE_TYPE_CATEGORY_SIGSERVICE.getUri()));
    return entities;
  }

  private static EntityDescriptor spMetadata(final String entityId, final String acs, final String entityCategory)
      throws Exception {
    final EntityDescriptorBuilder builder = EntityDescriptorBuilder.builder().entityID(entityId);
    if (entityCategory != null) {
      builder.extensions(ExtensionsBuilder.builder()
          .extension(EntityAttributesBuilder.builder().entityCategoriesAttribute(entityCategory).build())
          .build());
    }
    return builder
        .ssoDescriptor(SPSSODescriptorBuilder.builder()
            .authnRequestsSigned(true)
            .wantAssertionsSigned(false)
            .keyDescriptors(KeyDescriptorBuilder.builder()
                .use(UsageType.UNSPECIFIED)
                .certificate(SP_CREDENTIAL.getEntityCertificate())
                .build())
            .nameIDFormats(NameID.PERSISTENT)
            .assertionConsumerServices(AssertionConsumerServiceBuilder.builder()
                .postBinding()
                .location(acs)
                .index(0)
                .isDefault(true)
                .build())
            .attributeConsumingServices(AttributeConsumingServiceBuilder.builder()
                .index(0)
                .isDefault(true)
                .requestedAttributes(
                    RequestedAttributeBuilder.builder("urn:oid:2.5.4.4").isRequired(true).build(),
                    RequestedAttributeBuilder.builder("urn:oid:2.5.4.42").isRequired(true).build(),
                    RequestedAttributeBuilder.builder("urn:oid:1.2.752.201.3.7").isRequired(true).build(),
                    RequestedAttributeBuilder.builder("urn:oid:1.2.752.201.3.4").isRequired(true).build(),
                    RequestedAttributeBuilder.builder("urn:oid:1.3.6.1.5.5.7.9.1").isRequired(false).build(),
                    RequestedAttributeBuilder.builder("urn:oid:1.3.6.1.5.5.7.9.3").isRequired(false).build(),
                    RequestedAttributeBuilder.builder("urn:oid:1.3.6.1.5.5.7.9.2").isRequired(false).build(),
                    RequestedAttributeBuilder.builder("urn:oid:2.5.4.6").isRequired(false).build())
                .build())
            .build())
        .build();
  }

  private static EntitiesDescriptor euMetadata() throws Exception {
    final NodeCountry nodeCountry = (NodeCountry) XMLObjectSupport.buildXMLObject(NodeCountry.DEFAULT_ELEMENT_NAME);
    nodeCountry.setNodeCountry(COUNTRY);

    final EntityDescriptor idp = EntityDescriptorBuilder.builder()
        .entityID(FOREIGN_IDP)
        .extensions(ExtensionsBuilder.builder()
            .extension(EntityAttributesBuilder.builder()
                .assuranceCertificationAttribute("http://eidas.europa.eu/LoA/low", EIDAS_SUBSTANTIAL,
                    "http://eidas.europa.eu/LoA/high")
                .build())
            .build())
        .ssoDescriptor(IDPSSODescriptorBuilder.builder()
            .wantAuthnRequestsSigned(true)
            .keyDescriptors(KeyDescriptorBuilder.builder()
                .use(UsageType.UNSPECIFIED)
                .certificate(FOREIGN_IDP_CREDENTIAL.getEntityCertificate())
                .build())
            .extensions(ExtensionsBuilder.builder().extension(nodeCountry).build())
            .singleSignOnServices(SingleSignOnServiceBuilder.builder()
                .binding(SAMLConstants.SAML2_POST_BINDING_URI)
                .location(FOREIGN_SSO)
                .build())
            .build())
        .build();

    final EntitiesDescriptor entities =
        (EntitiesDescriptor) XMLObjectSupport.buildXMLObject(EntitiesDescriptor.DEFAULT_ELEMENT_NAME);
    entities.getEntityDescriptors().add(idp);
    return entities;
  }

  private static KeyPair generateKey() throws Exception {
    final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    return generator.generateKeyPair();
  }

  private static X509Certificate selfSigned(final KeyPair keyPair, final String name) throws Exception {
    final X500Name subject = new X500Name("CN=" + name);
    final Instant now = Instant.now();
    final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(subject,
        BigInteger.valueOf(now.toEpochMilli()), Date.from(now.minus(1, ChronoUnit.DAYS)),
        Date.from(now.plus(365, ChronoUnit.DAYS)), subject, keyPair.getPublic());
    return new JcaX509CertificateConverter().getCertificate(
        builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate())));
  }

  /**
   * Keeps the session of a user agent: the mock session, and the cookies, which carry the session when it is kept by
   * Spring Session.
   */
  private static class Browser implements RequestPostProcessor, ResultHandler {

    private final MockHttpSession session = new MockHttpSession();

    private final Map<String, Cookie> cookies = new java.util.LinkedHashMap<>();

    @Override
    public MockHttpServletRequest postProcessRequest(final MockHttpServletRequest request) {
      request.setSession(this.session);
      if (!this.cookies.isEmpty()) {
        request.setCookies(this.cookies.values().toArray(Cookie[]::new));
      }
      return request;
    }

    @Override
    public void handle(final MvcResult result) {
      for (final Cookie cookie : result.getResponse().getCookies()) {
        this.cookies.put(cookie.getName(), cookie);
      }
    }
  }

}
