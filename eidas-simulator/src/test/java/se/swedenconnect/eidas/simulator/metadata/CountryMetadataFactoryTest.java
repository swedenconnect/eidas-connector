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
package se.swedenconnect.eidas.simulator.metadata;

import net.shibboleth.shared.xml.SerializeSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.ext.saml2alg.SigningMethod;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import se.swedenconnect.eidas.simulator.OpenSamlTestBase;
import se.swedenconnect.eidas.simulator.TestCountries;
import se.swedenconnect.eidas.simulator.country.CountryRepository;
import se.swedenconnect.opensaml.eidas.ext.NodeCountry;
import se.swedenconnect.security.credential.KeyStoreCredential;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for {@link CountryMetadataFactory}.
 *
 * @author Martin Lindström
 */
class CountryMetadataFactoryTest extends OpenSamlTestBase {

  private static CountryRepository countries;

  private static KeyStoreCredential signingCredential;

  private static CountryMetadataFactory factory;

  @BeforeAll
  static void setup() throws Exception {
    countries = TestCountries.repository();
    signingCredential = new KeyStoreCredential(TestCountries.loadKeyStore(), "metadata", TestCountries.PASSWORD);
    factory = new CountryMetadataFactory(countries, signingCredential, TestCountries.BASE_URL, Duration.ofDays(7),
        Duration.ofHours(1));
  }

  @Test
  void testAggregate() throws Exception {
    final EntitiesDescriptor aggregate = (EntitiesDescriptor) roundTrip(factory.createAggregate());

    assertThat(aggregate.getEntityDescriptors()).hasSize(23);
    assertThat(aggregate.getValidUntil()).isAfter(Instant.now().plus(Duration.ofDays(6)));
    assertThat(aggregate.getCacheDuration()).isEqualTo(Duration.ofHours(1));
    assertThat(aggregate.getSignature()).isNotNull();
    SignatureValidator.validate(aggregate.getSignature(),
        new BasicX509Credential(signingCredential.getCertificate()));

    for (final EntityDescriptor ed : aggregate.getEntityDescriptors()) {
      final IDPSSODescriptor idp = ed.getIDPSSODescriptor(SAMLConstants.SAML20P_NS);
      assertThat(idp.getWantAuthnRequestsSigned()).isTrue();
      final String country = idp.getExtensions().getUnknownXMLObjects().stream()
          .filter(NodeCountry.class::isInstance).map(NodeCountry.class::cast)
          .map(NodeCountry::getNodeCountry).findFirst().orElseThrow();
      assertThat(ed.getEntityID()).isEqualTo(
          TestCountries.BASE_URL + "/" + country.toLowerCase() + "/ServiceMetadata");
      assertThat(idp.getSingleSignOnServices()).allMatch(s -> s.getLocation().equals(
          TestCountries.BASE_URL + "/" + country.toLowerCase() + "/ColleagueRequest"));
      assertThat(ed.getOrganization().getOrganizationNames().getFirst().getValue())
          .startsWith("Simulated eIDAS node, ");

      // The hashes of production URLs and keys are left out
      assertThat(entityAttributeNames(ed)).noneMatch(n -> n.contains("service-md-signature-trust-store")
          || n.contains("service-recognized-urls"));
    }
  }

  @Test
  void testCountrySettingsAreMirrored() throws Exception {
    // Germany: only unspecified NameID, no NameFormat on attributes, signing methods with key sizes
    final EntityDescriptor de = (EntityDescriptor) roundTrip(
        factory.createSignedEntityDescriptor(Objects.requireNonNull(countries.getCountry("DE"))));
    SignatureValidator.validate(de.getSignature(), new BasicX509Credential(signingCredential.getCertificate()));
    final IDPSSODescriptor deIdp = de.getIDPSSODescriptor(SAMLConstants.SAML20P_NS);
    assertThat(deIdp.getNameIDFormats()).extracting(n -> n.getURI())
        .containsExactly("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
    assertThat(deIdp.getAttributes()).allMatch(a -> a.getNameFormat() == null && a.getFriendlyName() != null);
    assertThat(entityAttributeValues(de, "http://eidas.europa.eu/entity-attributes/application-identifier"))
        .containsExactly("German eIDAS Middleware version: 4.1.0");
    assertThat(entityAttributeValues(de, "http://macedir.org/entity-category"))
        .containsExactly("http://eidas.europa.eu/entity-attributes/termsofaccess/requesterid");
    final SigningMethod rsa = de.getExtensions().getUnknownXMLObjects().stream()
        .filter(SigningMethod.class::isInstance).map(SigningMethod.class::cast)
        .filter(s -> s.getAlgorithm().contains("rsa"))
        .findFirst().orElseThrow();
    assertThat(rsa.getMinKeySize()).isEqualTo(3072);
    assertThat(rsa.getMaxKeySize()).isEqualTo(4096);

    // Italy: an encryption key, old protocol versions, no supported encryption algorithms attribute
    final EntityDescriptor it = factory.createEntityDescriptor(Objects.requireNonNull(countries.getCountry("IT")));
    final List<KeyDescriptor> itKeys = it.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getKeyDescriptors();
    assertThat(itKeys).extracting(KeyDescriptor::getUse).containsExactly(UsageType.SIGNING, UsageType.ENCRYPTION);
    assertThat(itKeys.get(1).getEncryptionMethods()).hasSize(3);
    assertThat(entityAttributeValues(it, "http://eidas.europa.eu/entity-attributes/protocol-version"))
        .containsExactly("1.2", "1.1");
    assertThat(entityAttributeNames(it))
        .doesNotContain("http://eidas.europa.eu/entity-attributes/supported-encryption-algorithms");

    // Estonia: no encryption key, only high, only the mandatory natural person attributes
    final EntityDescriptor ee = factory.createEntityDescriptor(Objects.requireNonNull(countries.getCountry("EE")));
    assertThat(ee.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getKeyDescriptors()).hasSize(1);
    assertThat(entityAttributeValues(ee, "urn:oasis:names:tc:SAML:attribute:assurance-certification"))
        .containsExactly("http://eidas.europa.eu/LoA/high");
    assertThat(ee.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getAttributes())
        .filteredOn(a -> a.getName().contains("/naturalperson/"))
        .hasSize(4);
    assertThat(ee.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getKeyDescriptors().getFirst().getKeyInfo())
        .isNotNull();

    // Denmark: no application identifier
    final EntityDescriptor dk = factory.createEntityDescriptor(Objects.requireNonNull(countries.getCountry("DK")));
    assertThat(entityAttributeNames(dk))
        .doesNotContain("http://eidas.europa.eu/entity-attributes/application-identifier");
  }

  @Test
  void testActiveCountries() throws Exception {
    final CountryRepository some = TestCountries.repository("fi", "EE");
    final CountryMetadataFactory f = new CountryMetadataFactory(some, signingCredential, TestCountries.BASE_URL,
        Duration.ofDays(1), Duration.ofMinutes(10));
    assertThat(f.createAggregate().getEntityDescriptors()).extracting(EntityDescriptor::getEntityID).containsExactly(
        TestCountries.BASE_URL + "/ee/ServiceMetadata", TestCountries.BASE_URL + "/fi/ServiceMetadata");
  }

  private static List<String> entityAttributeNames(final EntityDescriptor ed) {
    return entityAttributes(ed).getAttributes().stream().map(Attribute::getName).toList();
  }

  private static List<String> entityAttributeValues(final EntityDescriptor ed, final String name) {
    return entityAttributes(ed).getAttributes().stream()
        .filter(a -> name.equals(a.getName()))
        .flatMap(a -> a.getAttributeValues().stream())
        .map(v -> ((org.opensaml.core.xml.schema.XSString) v).getValue())
        .toList();
  }

  private static EntityAttributes entityAttributes(final EntityDescriptor ed) {
    return ed.getExtensions().getUnknownXMLObjects().stream()
        .filter(EntityAttributes.class::isInstance).map(EntityAttributes.class::cast)
        .findFirst().orElseThrow();
  }

  private static org.opensaml.core.xml.XMLObject roundTrip(final org.opensaml.core.xml.XMLObject object)
      throws Exception {
    final String xml = SerializeSupport.nodeToString(XMLObjectSupport.marshall(object));
    return XMLObjectSupport.unmarshallFromInputStream(XMLObjectProviderRegistrySupport.getParserPool(),
        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  }

}
