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
package se.swedenconnect.eidas.simulator.authn;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.RequesterID;
import org.opensaml.saml.saml2.core.Scoping;
import org.opensaml.saml.saml2.core.StatusCode;
import org.springframework.mock.web.MockHttpServletRequest;
import se.swedenconnect.eidas.simulator.OpenSamlTestBase;
import se.swedenconnect.eidas.simulator.TestConnector;
import se.swedenconnect.eidas.simulator.TestCountries;
import se.swedenconnect.eidas.simulator.country.CountryRepository;
import se.swedenconnect.eidas.simulator.country.SimulatedCountry;
import se.swedenconnect.eidas.simulator.metadata.ConnectorMetadataResolver;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttributes;
import se.swedenconnect.opensaml.eidas.ext.SPType;
import se.swedenconnect.opensaml.eidas.ext.SPTypeEnumeration;
import se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test cases for {@link AuthnRequestProcessor} and {@link AuthnRequestDecoder}.
 *
 * @author Martin Lindström
 */
class AuthnRequestProcessorTest extends OpenSamlTestBase {

  private static CountryRepository countries;

  private static TestConnector connector;

  @TempDir
  static Path tempDir;

  private AuthnRequestProcessor processor;

  private final AuthnRequestDecoder decoder = new AuthnRequestDecoder();

  @BeforeAll
  static void setup() throws Exception {
    countries = TestCountries.repository();
    connector = new TestConnector();
  }

  @BeforeEach
  void createProcessor() throws Exception {
    final Path metadata = connector.writeMetadata(tempDir, false);
    this.processor = new AuthnRequestProcessor(
        new ConnectorMetadataResolver(metadata.toUri().toString(), null, Duration.ofMinutes(10)),
        Duration.ofMinutes(5), Duration.ofSeconds(30));
  }

  @Test
  void testPostRequest() throws Exception {
    final SimulatedCountry hr = countries.getCountry("HR");
    final AuthnRequest request = connector.createRequest(hr);

    final ProcessedRequest processed = this.processor.process(hr, this.decodePost(request, "relay"));

    assertThat(processed.countryCode()).isEqualTo("HR");
    assertThat(processed.target().assertionConsumerServiceUrl()).isEqualTo(TestConnector.ACS_URL);
    assertThat(processed.target().inResponseTo()).isEqualTo(request.getID());
    assertThat(processed.target().relayState()).isEqualTo("relay");
    assertThat(processed.spType()).isEqualTo("public");
    assertThat(processed.nameIdFormat()).isEqualTo(NameIDType.PERSISTENT);
    assertThat(processed.destinationCountry()).isEqualTo("SE");
    assertThat(processed.allowedLoas())
        .containsExactly("http://eidas.europa.eu/LoA/substantial", "http://eidas.europa.eu/LoA/high");
    assertThat(processed.isRequired(AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME)).isTrue();
    assertThat(processed.isRequired(AttributeConstants.EIDAS_BIRTH_NAME_ATTRIBUTE_NAME)).isFalse();
    assertThat(processed.isRequested(AttributeConstants.EIDAS_BIRTH_NAME_ATTRIBUTE_NAME)).isTrue();
  }

  @Test
  void testRedirectRequest() throws Exception {
    final SimulatedCountry at = countries.getCountry("AT");
    final AuthnRequest request = connector.createRequest(at);
    final MockHttpServletRequest http = new MockHttpServletRequest("GET", "/at/ColleagueRequest");
    http.setQueryString(connector.encodeRedirect(request, "relay state"));

    final ProcessedRequest processed = this.processor.process(at, this.decoder.decode(http));
    assertThat(processed.target().relayState()).isEqualTo("relay state");
    assertThat(processed.allowedLoas()).containsExactly("http://eidas.europa.eu/LoA/high");
  }

  @Test
  void testRedirectRequestWithInvalidSignature() throws Exception {
    final SimulatedCountry at = countries.getCountry("AT");
    final MockHttpServletRequest http = new MockHttpServletRequest("GET", "/at/ColleagueRequest");
    http.setQueryString(connector.encodeRedirect(connector.createRequest(at), "relay").replace("relay", "other"));

    assertThatThrownBy(() -> this.processor.process(at, this.decoder.decode(http)))
        .isInstanceOf(UnrecoverableRequestException.class)
        .hasMessageContaining("signature");
  }

  @Test
  void testDecodeFailures() {
    assertThatThrownBy(() -> this.decoder.decode(new MockHttpServletRequest("POST", "/at/ColleagueRequest")))
        .isInstanceOf(UnrecoverableRequestException.class);

    final MockHttpServletRequest notBase64 = new MockHttpServletRequest("POST", "/at/ColleagueRequest");
    notBase64.addParameter("SAMLRequest", "%%%");
    assertThatThrownBy(() -> this.decoder.decode(notBase64)).isInstanceOf(UnrecoverableRequestException.class);

    final MockHttpServletRequest notSaml = new MockHttpServletRequest("POST", "/at/ColleagueRequest");
    notSaml.addParameter("SAMLRequest", "PGZvby8+");
    assertThatThrownBy(() -> this.decoder.decode(notSaml)).isInstanceOf(UnrecoverableRequestException.class);

    final MockHttpServletRequest noQuery = new MockHttpServletRequest("GET", "/at/ColleagueRequest");
    assertThatThrownBy(() -> this.decoder.decode(noQuery)).isInstanceOf(UnrecoverableRequestException.class);
  }

  @Test
  void testUnsignedRequest() throws Exception {
    final SimulatedCountry hr = countries.getCountry("HR");
    final MockHttpServletRequest http = new MockHttpServletRequest("POST", "/hr/ColleagueRequest");
    http.addParameter("SAMLRequest", connector.encodePost(connector.createRequest(hr), null));

    assertThatThrownBy(() -> this.processor.process(hr, this.decoder.decode(http)))
        .isInstanceOf(UnrecoverableRequestException.class)
        .hasMessage("The AuthnRequest is not signed");
  }

  @Test
  void testRequestSignedWithUnknownKey() throws Exception {
    final SimulatedCountry hr = countries.getCountry("HR");
    final MockHttpServletRequest http = new MockHttpServletRequest("POST", "/hr/ColleagueRequest");
    http.addParameter("SAMLRequest", connector.encodePost(connector.createRequest(hr), connector.getOtherCredential()));

    assertThatThrownBy(() -> this.processor.process(hr, this.decoder.decode(http)))
        .isInstanceOf(UnrecoverableRequestException.class)
        .hasMessage("The signature of the AuthnRequest is not valid");
  }

  @Test
  void testUnknownIssuer() throws Exception {
    final SimulatedCountry hr = countries.getCountry("HR");
    final AuthnRequest request = connector.createRequest(hr);
    request.getIssuer().setValue("https://other.example.com");

    assertThatThrownBy(() -> this.processor.process(hr, this.decodePost(request, null)))
        .isInstanceOf(UnrecoverableRequestException.class)
        .hasMessageContaining("is not the connector");
  }

  @Test
  void testConnectorMetadataNotAvailable() throws Exception {
    final AuthnRequestProcessor p = new AuthnRequestProcessor(
        new ConnectorMetadataResolver(tempDir.resolve("missing.xml").toUri().toString(), null, Duration.ofMinutes(1)),
        Duration.ofMinutes(5), Duration.ofSeconds(30));
    final SimulatedCountry hr = countries.getCountry("HR");

    assertThatThrownBy(() -> p.process(hr, this.decodePost(connector.createRequest(hr), null)))
        .isInstanceOf(UnrecoverableRequestException.class);
  }

  @Test
  void testAssertionConsumerServiceUrl() throws Exception {
    final SimulatedCountry hr = countries.getCountry("HR");

    final AuthnRequest valid = connector.createRequest(hr);
    valid.setAssertionConsumerServiceURL(TestConnector.ACS_URL);
    assertThat(this.processor.process(hr, this.decodePost(valid, null)).target().assertionConsumerServiceUrl())
        .isEqualTo(TestConnector.ACS_URL);

    final AuthnRequest invalid = connector.createRequest(hr);
    invalid.setAssertionConsumerServiceURL(TestConnector.ACS_URL.toUpperCase());
    assertThatThrownBy(() -> this.processor.process(hr, this.decodePost(invalid, null)))
        .isInstanceOf(UnrecoverableRequestException.class)
        .hasMessageContaining("AssertionConsumerServiceURL");

    final AuthnRequest index = connector.createRequest(hr);
    index.setAssertionConsumerServiceIndex(5);
    assertThatThrownBy(() -> this.processor.process(hr, this.decodePost(index, null)))
        .isInstanceOf(UnrecoverableRequestException.class)
        .hasMessageContaining("index 5");
  }

  @Test
  void testDestination() throws Exception {
    final SimulatedCountry hr = countries.getCountry("HR");
    final AuthnRequest request = connector.createRequest(hr);
    request.setDestination(countries.getCountry("AT").getSsoUrl());

    this.assertErrorResponse(hr, request, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, "Invalid Destination");
  }

  @Test
  void testMessageAge() throws Exception {
    final SimulatedCountry hr = countries.getCountry("HR");
    final AuthnRequest request = connector.createRequest(hr);
    final Instant issued = request.getIssueInstant();

    // Just within the limits: max age plus clock skew
    this.processor.setClock(Clock.fixed(issued.plus(Duration.ofMinutes(5)).plusSeconds(29), ZoneOffset.UTC));
    assertThat(this.processor.process(hr, this.decodePost(connector.createRequest(hr), null))).isNotNull();

    final AuthnRequest old = connector.createRequest(hr);
    this.processor.setClock(Clock.fixed(old.getIssueInstant().plus(Duration.ofMinutes(5)).plusSeconds(31),
        ZoneOffset.UTC));
    this.assertErrorResponse(hr, old, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, "too old");

    final AuthnRequest future = connector.createRequest(hr);
    this.processor.setClock(Clock.fixed(future.getIssueInstant().minusSeconds(31), ZoneOffset.UTC));
    this.assertErrorResponse(hr, future, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, "future");
  }

  @Test
  void testIsPassiveAndForceAuthn() throws Exception {
    final SimulatedCountry hr = countries.getCountry("HR");

    final AuthnRequest passive = connector.createRequest(hr);
    passive.setIsPassive(true);
    this.assertErrorResponse(hr, passive, StatusCode.RESPONDER, StatusCode.NO_PASSIVE, "IsPassive");

    final AuthnRequest noForce = connector.createRequest(hr);
    noForce.setForceAuthn(false);
    this.assertErrorResponse(hr, noForce, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, "ForceAuthn");
  }

  @Test
  void testSpType() throws Exception {
    final SimulatedCountry hr = countries.getCountry("HR");

    final AuthnRequest noSpType = connector.createRequest(hr);
    noSpType.getExtensions().getUnknownXMLObjects().removeIf(SPType.class::isInstance);
    this.assertErrorResponse(hr, noSpType, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, "No SPType");

    final AuthnRequest privateSp = connector.createRequest(hr);
    privateSp.getExtensions().getUnknownXMLObjects().stream()
        .filter(SPType.class::isInstance).map(SPType.class::cast)
        .forEach(s -> s.setType(SPTypeEnumeration.PRIVATE));
    assertThat(this.processor.process(hr, this.decodePost(privateSp, null)).spType()).isEqualTo("private");
  }

  @Test
  void testRequesterIdRequiredForPrivateSp() throws Exception {
    // BE requires the RequesterID of private sector relying parties
    final SimulatedCountry be = countries.getCountry("BE");
    final AuthnRequest request = connector.createRequest(be);
    request.getExtensions().getUnknownXMLObjects().stream()
        .filter(SPType.class::isInstance).map(SPType.class::cast)
        .forEach(s -> s.setType(SPTypeEnumeration.PRIVATE));
    this.assertErrorResponse(be, request, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, "RequesterID");

    final AuthnRequest withRequesterId = connector.createRequest(be);
    withRequesterId.getExtensions().getUnknownXMLObjects().stream()
        .filter(SPType.class::isInstance).map(SPType.class::cast)
        .forEach(s -> s.setType(SPTypeEnumeration.PRIVATE));
    final Scoping scoping = (Scoping) XMLObjectSupport.buildXMLObject(Scoping.DEFAULT_ELEMENT_NAME);
    final RequesterID requesterId = (RequesterID) XMLObjectSupport.buildXMLObject(RequesterID.DEFAULT_ELEMENT_NAME);
    requesterId.setURI("https://sp.example.com");
    scoping.getRequesterIDs().add(requesterId);
    withRequesterId.setScoping(scoping);
    assertThat(this.processor.process(be, this.decodePost(withRequesterId, null)).requesterId())
        .isEqualTo("https://sp.example.com");

    // HR does not require it
    final SimulatedCountry hr = countries.getCountry("HR");
    final AuthnRequest hrRequest = connector.createRequest(hr);
    hrRequest.getExtensions().getUnknownXMLObjects().stream()
        .filter(SPType.class::isInstance).map(SPType.class::cast)
        .forEach(s -> s.setType(SPTypeEnumeration.PRIVATE));
    assertThat(this.processor.process(hr, this.decodePost(hrRequest, null)).requesterId()).isNull();
  }

  @Test
  void testRequestedAttributes() throws Exception {
    // EE supports only the four mandatory attributes, an optional unsupported attribute is ignored
    final SimulatedCountry ee = countries.getCountry("EE");
    assertThat(this.processor.process(ee, this.decodePost(connector.createRequest(ee), null))).isNotNull();

    // ... but a required one is not
    final AuthnRequest required = connector.createRequest(ee);
    this.requestedAttributes(required).getRequestedAttributes()
        .add(TestConnector.requestedAttribute(AttributeConstants.EIDAS_GENDER_ATTRIBUTE_NAME, true));
    this.assertErrorResponse(ee, required, StatusCode.RESPONDER, StatusCode.REQUEST_DENIED, "is not supported");

    // A mandatory attribute that is not requested
    final AuthnRequest missing = connector.createRequest(ee);
    this.requestedAttributes(missing).getRequestedAttributes()
        .removeIf(a -> AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME.equals(a.getName()));
    this.assertErrorResponse(ee, missing, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, "DateOfBirth");

    // Natural and legal person attributes mixed
    final SimulatedCountry hr = countries.getCountry("HR");
    final AuthnRequest mixed = connector.createRequest(hr);
    this.requestedAttributes(mixed).getRequestedAttributes().add(TestConnector.requestedAttribute(
        AttributeConstants.LEGAL_PERSON_PREFIX + "LegalName", false));
    this.assertErrorResponse(hr, mixed, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, "natural and legal");

    // A required representative attribute that the country supports
    final AuthnRequest representative = connector.createRequest(hr);
    this.requestedAttributes(representative).getRequestedAttributes().add(TestConnector.requestedAttribute(
        AttributeConstants.EIDAS_REPRESENTATIVE_PERSON_IDENTIFIER_ATTRIBUTE_NAME, true));
    this.assertErrorResponse(hr, representative, StatusCode.RESPONDER, StatusCode.REQUEST_UNSUPPORTED,
        "only natural persons");

    // No attributes
    final AuthnRequest none = connector.createRequest(hr);
    none.getExtensions().getUnknownXMLObjects().removeIf(RequestedAttributes.class::isInstance);
    this.assertErrorResponse(hr, none, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, "No attributes");
  }

  @Test
  void testNameIdPolicy() throws Exception {
    // DE supports only unspecified
    final SimulatedCountry de = countries.getCountry("DE");
    assertThat(this.processor.process(de, this.decodePost(connector.createRequest(de), null)).nameIdFormat())
        .isEqualTo(NameIDType.UNSPECIFIED);

    this.processor.setStrictNameIdPolicy(true);
    this.assertErrorResponse(de, connector.createRequest(de), StatusCode.REQUESTER, StatusCode.INVALID_NAMEID_POLICY,
        "persistent");

    final AuthnRequest transientId = connector.createRequest(countries.getCountry("HR"));
    transientId.getNameIDPolicy().setFormat(NameIDType.TRANSIENT);
    assertThat(this.processor.process(countries.getCountry("HR"), this.decodePost(transientId, null)).nameIdFormat())
        .isEqualTo(NameIDType.TRANSIENT);

    final AuthnRequest noPolicy = connector.createRequest(countries.getCountry("HR"));
    noPolicy.setNameIDPolicy(null);
    assertThat(this.processor.process(countries.getCountry("HR"), this.decodePost(noPolicy, null)).nameIdFormat())
        .isEqualTo(NameIDType.PERSISTENT);
  }

  @Test
  void testLevelOfAssurance() throws Exception {
    final SimulatedCountry hr = countries.getCountry("HR");   // low, substantial and high
    final SimulatedCountry at = countries.getCountry("AT");   // high
    final SimulatedCountry ro = countries.getCountry("RO");   // substantial and low
    final SimulatedCountry si = countries.getCountry("SI");   // also non-notified URIs

    final AuthnRequest minimumLow = connector.createRequest(hr);
    minimumLow.setRequestedAuthnContext(TestConnector.requestedAuthnContext(
        AuthnContextComparisonTypeEnumeration.MINIMUM, "http://eidas.europa.eu/LoA/low"));
    assertThat(this.processor.process(hr, this.decodePost(minimumLow, null)).allowedLoas()).containsExactly(
        "http://eidas.europa.eu/LoA/low", "http://eidas.europa.eu/LoA/substantial", "http://eidas.europa.eu/LoA/high");

    final AuthnRequest minimumHigh = connector.createRequest(ro);
    minimumHigh.setRequestedAuthnContext(TestConnector.requestedAuthnContext(
        AuthnContextComparisonTypeEnumeration.MINIMUM, "http://eidas.europa.eu/LoA/high"));
    this.assertErrorResponse(ro, minimumHigh, StatusCode.RESPONDER, StatusCode.NO_AUTHN_CONTEXT, "can be delivered");

    final AuthnRequest exact = connector.createRequest(hr);
    exact.setRequestedAuthnContext(TestConnector.requestedAuthnContext(
        AuthnContextComparisonTypeEnumeration.EXACT, "http://eidas.europa.eu/LoA/substantial",
        "http://eidas.europa.eu/NotNotified/LoA/high"));
    assertThat(this.processor.process(hr, this.decodePost(exact, null)).allowedLoas())
        .containsExactly("http://eidas.europa.eu/LoA/substantial");

    final AuthnRequest exactNotSupported = connector.createRequest(at);
    exactNotSupported.setRequestedAuthnContext(TestConnector.requestedAuthnContext(
        AuthnContextComparisonTypeEnumeration.EXACT, "http://eidas.europa.eu/LoA/substantial"));
    this.assertErrorResponse(at, exactNotSupported, StatusCode.RESPONDER, StatusCode.NO_AUTHN_CONTEXT,
        "can be delivered");

    // No comparison means exact
    final AuthnRequest noComparison = connector.createRequest(si);
    noComparison.setRequestedAuthnContext(TestConnector.requestedAuthnContext(null,
        "http://non.eidas.eu/NotNotified/LoA/2"));
    assertThat(this.processor.process(si, this.decodePost(noComparison, null)).allowedLoas())
        .containsExactly("http://non.eidas.eu/NotNotified/LoA/2");

    final AuthnRequest minimumNonNotified = connector.createRequest(si);
    minimumNonNotified.setRequestedAuthnContext(TestConnector.requestedAuthnContext(
        AuthnContextComparisonTypeEnumeration.MINIMUM, "http://non.eidas.eu/NotNotified/LoA/2"));
    this.assertErrorResponse(si, minimumNonNotified, StatusCode.REQUESTER, StatusCode.REQUEST_UNSUPPORTED,
        "minimum");

    final AuthnRequest better = connector.createRequest(hr);
    better.setRequestedAuthnContext(TestConnector.requestedAuthnContext(
        AuthnContextComparisonTypeEnumeration.BETTER, "http://eidas.europa.eu/LoA/low"));
    this.assertErrorResponse(hr, better, StatusCode.REQUESTER, StatusCode.REQUEST_UNSUPPORTED, "better");

    final AuthnRequest none = connector.createRequest(hr);
    none.setRequestedAuthnContext(null);
    this.assertErrorResponse(hr, none, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED, "RequestedAuthnContext");
  }

  private void assertErrorResponse(final SimulatedCountry country, final AuthnRequest request,
      final String statusCode, final String subStatusCode, final String messagePart) throws Exception {
    final AuthnRequestDecoder.DecodedRequest decoded = this.decodePost(request, null);
    assertThatThrownBy(() -> this.processor.process(country, decoded))
        .isInstanceOfSatisfying(ErrorResponseException.class, e -> {
          assertThat(e.getStatus().statusCode()).isEqualTo(statusCode);
          assertThat(e.getStatus().subStatusCode()).isEqualTo(subStatusCode);
          assertThat(e.getStatus().message()).containsIgnoringCase(messagePart);
          assertThat(e.getTarget().assertionConsumerServiceUrl()).isEqualTo(TestConnector.ACS_URL);
          assertThat(e.getTarget().inResponseTo()).isEqualTo(request.getID());
        });
  }

  private AuthnRequestDecoder.DecodedRequest decodePost(final AuthnRequest request, final String relayState)
      throws Exception {
    final MockHttpServletRequest http = new MockHttpServletRequest("POST", "/ColleagueRequest");
    http.addParameter("SAMLRequest", connector.encodePost(request));
    if (relayState != null) {
      http.addParameter("RelayState", relayState);
    }
    return this.decoder.decode(http);
  }

  private RequestedAttributes requestedAttributes(final AuthnRequest request) {
    return request.getExtensions().getUnknownXMLObjects().stream()
        .filter(RequestedAttributes.class::isInstance)
        .map(RequestedAttributes.class::cast)
        .findFirst()
        .orElseThrow();
  }

}
