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

import net.shibboleth.shared.xml.SerializeSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import se.swedenconnect.eidas.simulator.OpenSamlTestBase;
import se.swedenconnect.eidas.simulator.TestConnector;
import se.swedenconnect.eidas.simulator.TestCountries;
import se.swedenconnect.eidas.simulator.country.CountryRepository;
import se.swedenconnect.eidas.simulator.country.SimulatedCountry;
import se.swedenconnect.eidas.simulator.country.SimulatedUser;
import se.swedenconnect.opensaml.xmlsec.encryption.support.SAMLObjectDecrypter;
import se.swedenconnect.security.credential.opensaml.OpenSamlCredential;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test cases for {@link ResponseFactory} and {@link AlgorithmSelector}.
 *
 * @author Martin Lindström
 */
class ResponseFactoryTest extends OpenSamlTestBase {

  private static CountryRepository countries;

  private static TestConnector connector;

  private static ResponseFactory responseFactory;

  private final AttributeReleaser releaser = new AttributeReleaser();

  @BeforeAll
  static void setup() throws Exception {
    countries = TestCountries.repository();
    connector = new TestConnector();
    responseFactory = new ResponseFactory(new AlgorithmSelector());
  }

  @ParameterizedTest
  @ValueSource(strings = { "AT", "CZ", "DE", "EE", "IT", "PL", "PT", "CY", "LU" })
  void testSuccessResponse(final String code) throws Exception {
    final SimulatedCountry country = countries.getCountry(code);
    final ProcessedRequest request = processedRequest(country, NameIDType.PERSISTENT,
        connector.createMetadata());
    final SimulatedUser user = country.getUsers().getFirst();

    final Response response = roundTrip(responseFactory.createSuccessResponse(country, request, user,
        "http://eidas.europa.eu/LoA/high", this.releaser.releaseAttributes(country, request, user)));

    assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.SUCCESS);
    assertThat(response.getInResponseTo()).isEqualTo("_request");
    assertThat(response.getDestination()).isEqualTo(TestConnector.ACS_URL);
    assertThat(response.getIssuer().getValue()).isEqualTo(country.getEntityId());
    assertThat(response.getAssertions()).isEmpty();
    assertThat(response.getEncryptedAssertions()).hasSize(1);
    assertSignedBy(response, country);

    // The signature algorithm fits the key of the country and the connector metadata
    final String sigAlg = response.getSignature().getSignatureAlgorithm();
    if (country.getSigningCredential().getPublicKey().getAlgorithm().equals("EC")) {
      assertThat(sigAlg).contains("ecdsa");
    }
    else {
      assertThat(sigAlg).isEqualTo("http://www.w3.org/2007/05/xmldsig-more#sha256-rsa-MGF1");
    }

    final Assertion assertion = new SAMLObjectDecrypter(new OpenSamlCredential(connector.getEncryptCredential()))
        .decrypt(response.getEncryptedAssertions().getFirst(), Assertion.class);
    assertThat(response.getEncryptedAssertions().getFirst().getEncryptedData().getEncryptionMethod().getAlgorithm())
        .isEqualTo("http://www.w3.org/2009/xmlenc11#aes256-gcm");
    assertSignedBy(assertion, country);
    assertThat(assertion.getIssuer().getValue()).isEqualTo(country.getEntityId());
    assertThat(assertion.getSubject().getNameID().getFormat()).isEqualTo(NameIDType.PERSISTENT);
    assertThat(assertion.getSubject().getNameID().getValue())
        .isEqualTo("%s/SE/%s".formatted(code, user.getPersonIdentifier()));
    assertThat(assertion.getSubject().getSubjectConfirmations().getFirst().getSubjectConfirmationData()
        .getInResponseTo()).isEqualTo("_request");
    assertThat(assertion.getConditions().getAudienceRestrictions().getFirst().getAudiences().getFirst().getURI())
        .isEqualTo(TestConnector.ENTITY_ID);
    assertThat(assertion.getAuthnStatements().getFirst().getAuthnContext().getAuthnContextClassRef().getURI())
        .isEqualTo("http://eidas.europa.eu/LoA/high");
    assertThat(assertion.getAttributeStatements().getFirst().getAttributes())
        .extracting(a -> a.getFriendlyName())
        .contains("PersonIdentifier", "FirstName", "FamilyName", "DateOfBirth");
  }

  @Test
  void testTransientNameId() throws Exception {
    final SimulatedCountry country = countries.getCountry("HR");
    final ProcessedRequest request = processedRequest(country, NameIDType.TRANSIENT, connector.createMetadata());
    final SimulatedUser user = country.getUsers().getFirst();
    final Response response = responseFactory.createSuccessResponse(country, request, user,
        "http://eidas.europa.eu/LoA/low", this.releaser.releaseAttributes(country, request, user));
    final Assertion assertion = new SAMLObjectDecrypter(new OpenSamlCredential(connector.getEncryptCredential()))
        .decrypt(response.getEncryptedAssertions().getFirst(), Assertion.class);
    assertThat(assertion.getSubject().getNameID().getValue()).doesNotContain(user.getPersonIdentifier());
  }

  @Test
  void testErrorResponse() throws Exception {
    final SimulatedCountry country = countries.getCountry("FI");
    final ProcessedRequest request = processedRequest(country, NameIDType.PERSISTENT, connector.createMetadata());

    final Response response = roundTrip(responseFactory.createErrorResponse(country, request.target(),
        ErrorResponseOption.CANCEL.toStatus(null)));

    assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
    assertThat(response.getStatus().getStatusCode().getStatusCode().getValue()).isEqualTo(StatusCode.REQUEST_DENIED);
    assertThat(response.getStatus().getStatusMessage().getValue()).isEqualTo("User cancelled the authentication");
    assertThat(response.getEncryptedAssertions()).isEmpty();
    assertSignedBy(response, country);

    final Response internal = responseFactory.createErrorResponse(country, request.target(),
        ErrorResponseOption.INTERNAL_ERROR.toStatus("Something broke"));
    assertThat(internal.getStatus().getStatusCode().getStatusCode()).isNull();
    assertThat(internal.getStatus().getStatusMessage().getValue()).isEqualTo("Something broke");
  }

  @Test
  void testNoCommonSignatureAlgorithm() throws Exception {
    // A connector that only allows ECDSA cannot be served by an RSA country
    final EntityDescriptor metadata = connector.createMetadata();
    metadata.getSPSSODescriptor(SAMLConstants.SAML20P_NS).getExtensions().getUnknownXMLObjects()
        .removeIf(o -> o instanceof org.opensaml.saml.ext.saml2alg.SigningMethod sm
            && sm.getAlgorithm().contains("rsa"));
    final SimulatedCountry at = countries.getCountry("AT");
    final ProcessedRequest request = processedRequest(at, NameIDType.PERSISTENT, metadata);

    assertThatThrownBy(() -> responseFactory.createErrorResponse(at, request.target(),
        ErrorResponseOption.CANCEL.toStatus(null)))
        .isInstanceOf(ResponseCreationException.class)
        .hasMessageContaining("signature algorithm");

    // ... but an EC country can
    final SimulatedCountry ee = countries.getCountry("EE");
    assertThat(responseFactory.createErrorResponse(ee, processedRequest(ee, NameIDType.PERSISTENT, metadata).target(),
        ErrorResponseOption.CANCEL.toStatus(null))).isNotNull();
  }

  @Test
  void testNoCommonEncryptionAlgorithm() throws Exception {
    // A connector that only allows AES-CBC cannot be served
    final EntityDescriptor metadata = connector.createMetadata();
    final KeyDescriptor kd = metadata.getSPSSODescriptor(SAMLConstants.SAML20P_NS).getKeyDescriptors().stream()
        .filter(k -> k.getUse() == UsageType.ENCRYPTION).findFirst().orElseThrow();
    kd.getEncryptionMethods().removeIf(m -> m.getAlgorithm().contains("gcm"));
    kd.getEncryptionMethods().add(
        se.swedenconnect.opensaml.saml2.metadata.build.EncryptionMethodBuilder.encryptionMethod(
            "http://www.w3.org/2001/04/xmlenc#aes256-cbc"));
    final SimulatedCountry country = countries.getCountry("AT");
    final ProcessedRequest request = processedRequest(country, NameIDType.PERSISTENT, metadata);
    final SimulatedUser user = country.getUsers().getFirst();

    assertThatThrownBy(() -> responseFactory.createSuccessResponse(country, request, user,
        "http://eidas.europa.eu/LoA/high", List.of()))
        .isInstanceOf(ResponseCreationException.class)
        .hasMessageContaining("encryption algorithm");
  }

  @Test
  void testAlgorithmSelection() throws Exception {
    final AlgorithmSelector selector = new AlgorithmSelector();
    final EntityDescriptor metadata = connector.createMetadata();

    // The connector metadata has no DigestMethod elements, so the first of the country is used
    assertThat(selector.selectDigestAlgorithm(countries.getCountry("ES"), metadata))
        .isEqualTo("http://www.w3.org/2001/04/xmlenc#sha512");

    // ES lists rsa-sha512 first, which the connector does not allow
    assertThat(selector.selectSignatureAlgorithm(countries.getCountry("ES"), metadata))
        .isEqualTo("http://www.w3.org/2007/05/xmldsig-more#sha256-rsa-MGF1");

    // IT has a P-384 key
    assertThat(selector.selectSignatureAlgorithm(countries.getCountry("IT"), metadata))
        .isEqualTo("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256");

    // RO prefers aes192-gcm, which the connector allows
    assertThat(selector.selectDataEncryptionAlgorithm(countries.getCountry("RO"), metadata))
        .isEqualTo("http://www.w3.org/2009/xmlenc11#aes192-gcm");

    // IT has no supported encryption algorithms attribute, the methods of its encryption key are used
    assertThat(selector.selectDataEncryptionAlgorithm(countries.getCountry("IT"), metadata))
        .isEqualTo("http://www.w3.org/2009/xmlenc11#aes256-gcm");

    // DE has neither, the eIDAS defaults are used
    assertThat(selector.selectDataEncryptionAlgorithm(countries.getCountry("DE"), metadata))
        .isEqualTo("http://www.w3.org/2009/xmlenc11#aes256-gcm");
  }

  private static ProcessedRequest processedRequest(final SimulatedCountry country, final String nameIdFormat,
      final EntityDescriptor metadata) {
    return new ProcessedRequest("id", country.getCode(),
        new ResponseTarget(metadata, TestConnector.ACS_URL, "_request", "relay"), "public", null, null,
        List.of(
            new ProcessedRequest.RequestedAttribute(
                "http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier", true),
            new ProcessedRequest.RequestedAttribute(
                "http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName", true),
            new ProcessedRequest.RequestedAttribute(
                "http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName", true),
            new ProcessedRequest.RequestedAttribute(
                "http://eidas.europa.eu/attributes/naturalperson/DateOfBirth", true)),
        nameIdFormat, List.of("http://eidas.europa.eu/LoA/high"), "SE");
  }

  private static void assertSignedBy(final org.opensaml.saml.common.SignableSAMLObject object,
      final SimulatedCountry country) throws Exception {
    assertThat(object.getSignature()).isNotNull();
    SignatureValidator.validate(object.getSignature(),
        new BasicX509Credential(country.getSigningCredential().getCertificate()));
  }

  /**
   * Serializes and parses the response, so that it is validated as the connector receives it.
   */
  private static Response roundTrip(final Response response) throws Exception {
    final String xml = SerializeSupport.nodeToString(XMLObjectSupport.marshall(response));
    return (Response) XMLObjectSupport.unmarshallFromInputStream(XMLObjectProviderRegistrySupport.getParserPool(),
        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  }

}
