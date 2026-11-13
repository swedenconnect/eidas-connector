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
package se.swedenconnect.eidas.simulator;

import net.shibboleth.shared.xml.SerializeSupport;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.SecurityConfigurationSupport;
import org.opensaml.xmlsec.crypto.XMLSigningUtil;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.springframework.core.io.ClassPathResource;
import se.swedenconnect.eidas.simulator.country.SimulatedCountry;
import se.swedenconnect.opensaml.eidas.ext.NodeCountry;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttribute;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttributes;
import se.swedenconnect.opensaml.eidas.ext.SPType;
import se.swedenconnect.opensaml.eidas.ext.SPTypeEnumeration;
import se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants;
import se.swedenconnect.opensaml.saml2.metadata.build.AssertionConsumerServiceBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.EncryptionMethodBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.EntityDescriptorBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.ExtensionsBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.KeyDescriptorBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.SPSSODescriptorBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.SigningMethodBuilder;
import se.swedenconnect.opensaml.xmlsec.signature.support.SAMLObjectSigner;
import se.swedenconnect.security.credential.KeyStoreCredential;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.factory.KeyStoreFactory;
import se.swedenconnect.security.credential.opensaml.OpenSamlCredential;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * A fake eIDAS connector for tests: its metadata, and signed authentication requests.
 *
 * @author Martin Lindström
 */
public class TestConnector {

  /** The entityID of the connector. */
  public static final String ENTITY_ID = "https://connector.example.com/idp/metadata/sp";

  /** The assertion consumer service of the connector. */
  public static final String ACS_URL = "https://connector.example.com/idp/extauth/saml2/post";

  /** The signing credential. */
  private final PkiCredential signCredential;

  /** The encryption credential. */
  private final PkiCredential encryptCredential;

  /** A credential that is not in the metadata. */
  private final PkiCredential otherCredential;

  /**
   * Constructor.
   *
   * @throws Exception for errors loading the keys
   */
  public TestConnector() throws Exception {
    final KeyStore keyStore;
    try (final InputStream in = new ClassPathResource("test-connector.jks").getInputStream()) {
      keyStore = KeyStoreFactory.loadKeyStore(in, TestCountries.PASSWORD, "JKS", null);
    }
    this.signCredential = new KeyStoreCredential(keyStore, "sign", TestCountries.PASSWORD);
    this.encryptCredential = new KeyStoreCredential(keyStore, "encrypt", TestCountries.PASSWORD);
    this.otherCredential = new KeyStoreCredential(keyStore, "other", TestCountries.PASSWORD);
  }

  /**
   * Creates the metadata of the connector, as the connector publishes it.
   *
   * @return the metadata
   */
  public EntityDescriptor createMetadata() {
    final NodeCountry nodeCountry = (NodeCountry) XMLObjectSupport.buildXMLObject(NodeCountry.DEFAULT_ELEMENT_NAME);
    nodeCountry.setNodeCountry("SE");
    return EntityDescriptorBuilder.builder()
        .entityID(ENTITY_ID)
        .ssoDescriptor(SPSSODescriptorBuilder.builder()
            .authnRequestsSigned(true)
            .wantAssertionsSigned(false)
            .extensions(ExtensionsBuilder.builder()
                .extension(nodeCountry)
                .extension(SigningMethodBuilder.signingMethod("http://www.w3.org/2007/05/xmldsig-more#sha256-rsa-MGF1",
                    3072, null))
                .extension(SigningMethodBuilder.signingMethod("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256",
                    256, null))
                .extension(SigningMethodBuilder.signingMethod("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384",
                    256, null))
                .extension(SigningMethodBuilder.signingMethod("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512",
                    256, null))
                .build())
            .keyDescriptors(
                KeyDescriptorBuilder.builder().use(UsageType.SIGNING)
                    .certificate(this.signCredential.getCertificate()).build(),
                KeyDescriptorBuilder.builder().use(UsageType.ENCRYPTION)
                    .certificate(this.encryptCredential.getCertificate())
                    .encryptionMethodsExt(java.util.List.of(
                        EncryptionMethodBuilder.encryptionMethod("http://www.w3.org/2009/xmlenc11#aes256-gcm"),
                        EncryptionMethodBuilder.encryptionMethod("http://www.w3.org/2009/xmlenc11#aes192-gcm"),
                        EncryptionMethodBuilder.encryptionMethod("http://www.w3.org/2009/xmlenc11#aes128-gcm"),
                        EncryptionMethodBuilder.encryptionMethod("http://www.w3.org/2009/xmlenc11#rsa-oaep")))
                    .build())
            .assertionConsumerServices(AssertionConsumerServiceBuilder.builder()
                .postBinding()
                .location(ACS_URL)
                .index(0)
                .isDefault(true)
                .build())
            .build())
        .build();
  }

  /**
   * Writes the metadata to a file.
   *
   * @param dir the directory
   * @param signed whether the metadata is signed with the signing key
   * @return the file
   * @throws Exception for errors
   */
  public Path writeMetadata(final Path dir, final boolean signed) throws Exception {
    final EntityDescriptor metadata = this.createMetadata();
    if (signed) {
      this.sign(metadata, this.signCredential);
    }
    final Path file = dir.resolve("connector-" + UUID.randomUUID() + ".xml");
    Files.writeString(file, SerializeSupport.nodeToString(XMLObjectSupport.marshall(metadata)));
    return file;
  }

  /**
   * Creates an authentication request for a country, as the connector sends it: SPType public, the mandatory
   * attributes required and BirthName optional, persistent NameID and minimum substantial.
   *
   * @param country the country
   * @return the (unsigned) request
   */
  public AuthnRequest createRequest(final SimulatedCountry country) {
    final AuthnRequest request = (AuthnRequest) XMLObjectSupport.buildXMLObject(AuthnRequest.DEFAULT_ELEMENT_NAME);
    request.setID("_" + UUID.randomUUID().toString().replace("-", ""));
    request.setVersion(SAMLVersion.VERSION_20);
    request.setIssueInstant(Instant.now());
    request.setDestination(country.getSsoUrl());
    request.setForceAuthn(true);
    request.setIsPassive(false);

    final Issuer issuer = (Issuer) XMLObjectSupport.buildXMLObject(Issuer.DEFAULT_ELEMENT_NAME);
    issuer.setFormat(NameIDType.ENTITY);
    issuer.setValue(ENTITY_ID);
    request.setIssuer(issuer);

    final Extensions extensions = (Extensions) XMLObjectSupport.buildXMLObject(Extensions.DEFAULT_ELEMENT_NAME);
    final SPType spType = (SPType) XMLObjectSupport.buildXMLObject(SPType.DEFAULT_ELEMENT_NAME);
    spType.setType(SPTypeEnumeration.PUBLIC);
    extensions.getUnknownXMLObjects().add(spType);
    final RequestedAttributes attributes =
        (RequestedAttributes) XMLObjectSupport.buildXMLObject(RequestedAttributes.DEFAULT_ELEMENT_NAME);
    attributes.getRequestedAttributes().add(requestedAttribute(
        AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME, true));
    attributes.getRequestedAttributes().add(requestedAttribute(
        AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME, true));
    attributes.getRequestedAttributes().add(requestedAttribute(
        AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME, true));
    attributes.getRequestedAttributes().add(requestedAttribute(
        AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME, true));
    attributes.getRequestedAttributes().add(requestedAttribute(
        AttributeConstants.EIDAS_BIRTH_NAME_ATTRIBUTE_NAME, false));
    extensions.getUnknownXMLObjects().add(attributes);
    request.setExtensions(extensions);

    final NameIDPolicy nameIDPolicy = (NameIDPolicy) XMLObjectSupport.buildXMLObject(NameIDPolicy.DEFAULT_ELEMENT_NAME);
    nameIDPolicy.setFormat(NameIDType.PERSISTENT);
    nameIDPolicy.setAllowCreate(true);
    request.setNameIDPolicy(nameIDPolicy);

    request.setRequestedAuthnContext(requestedAuthnContext(AuthnContextComparisonTypeEnumeration.MINIMUM,
        "http://eidas.europa.eu/LoA/substantial"));
    return request;
  }

  /**
   * Creates a requested attribute.
   *
   * @param name the attribute name
   * @param required whether it is required
   * @return the requested attribute
   */
  public static RequestedAttribute requestedAttribute(final String name, final boolean required) {
    final RequestedAttribute attribute =
        (RequestedAttribute) XMLObjectSupport.buildXMLObject(RequestedAttribute.DEFAULT_ELEMENT_NAME);
    attribute.setName(name);
    attribute.setNameFormat(RequestedAttribute.URI_REFERENCE);
    attribute.setIsRequired(required);
    return attribute;
  }

  /**
   * Creates a requested authentication context.
   *
   * @param comparison the comparison, or {@code null}
   * @param uris the URIs
   * @return the requested authentication context
   */
  public static RequestedAuthnContext requestedAuthnContext(final AuthnContextComparisonTypeEnumeration comparison,
      final String... uris) {
    final RequestedAuthnContext rac =
        (RequestedAuthnContext) XMLObjectSupport.buildXMLObject(RequestedAuthnContext.DEFAULT_ELEMENT_NAME);
    rac.setComparison(comparison);
    for (final String uri : uris) {
      final AuthnContextClassRef ref =
          (AuthnContextClassRef) XMLObjectSupport.buildXMLObject(AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
      ref.setURI(uri);
      rac.getAuthnContextClassRefs().add(ref);
    }
    return rac;
  }

  /**
   * Signs a request with the signing key and encodes it for the HTTP-POST binding.
   *
   * @param request the request
   * @return the value of the SAMLRequest parameter
   * @throws Exception for errors
   */
  public String encodePost(final AuthnRequest request) throws Exception {
    return this.encodePost(request, this.signCredential);
  }

  /**
   * Signs a request with the given key, unless {@code null}, and encodes it for the HTTP-POST binding.
   *
   * @param request the request
   * @param credential the signing key, or {@code null} for an unsigned request
   * @return the value of the SAMLRequest parameter
   * @throws Exception for errors
   */
  public String encodePost(final AuthnRequest request, final PkiCredential credential) throws Exception {
    if (credential != null) {
      this.sign(request, credential);
    }
    final String xml = SerializeSupport.nodeToString(XMLObjectSupport.marshall(request));
    return Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Encodes a request for the HTTP-Redirect binding, signed with the signing key.
   *
   * @param request the request
   * @param relayState the relay state, or {@code null}
   * @return the query string
   * @throws Exception for errors
   */
  public String encodeRedirect(final AuthnRequest request, final String relayState) throws Exception {
    final String xml = SerializeSupport.nodeToString(XMLObjectSupport.marshall(request));
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (final DeflaterOutputStream out =
        new DeflaterOutputStream(bytes, new Deflater(Deflater.DEFLATED, true))) {
      out.write(xml.getBytes(StandardCharsets.UTF_8));
    }
    final StringBuilder query = new StringBuilder("SAMLRequest=")
        .append(urlEncode(Base64.getEncoder().encodeToString(bytes.toByteArray())));
    if (relayState != null) {
      query.append("&RelayState=").append(urlEncode(relayState));
    }
    query.append("&SigAlg=").append(urlEncode(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256));
    final byte[] signature = XMLSigningUtil.signWithURI(new OpenSamlCredential(this.signCredential),
        SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256, query.toString().getBytes(StandardCharsets.UTF_8));
    query.append("&Signature=").append(urlEncode(Base64.getEncoder().encodeToString(signature)));
    return query.toString();
  }

  /**
   * Signs a SAML object.
   *
   * @param object the object
   * @param credential the key
   * @throws Exception for errors
   */
  private void sign(final SignableSAMLObject object, final PkiCredential credential) throws Exception {
    SAMLObjectSigner.sign(object, new OpenSamlCredential(credential),
        SecurityConfigurationSupport.getGlobalSignatureSigningConfiguration());
  }

  /**
   * Gets the signing key.
   *
   * @return the signing key
   */
  public PkiCredential getSignCredential() {
    return this.signCredential;
  }

  /**
   * Gets the encryption key.
   *
   * @return the encryption key
   */
  public PkiCredential getEncryptCredential() {
    return this.encryptCredential;
  }

  /**
   * Gets a key that is not in the metadata.
   *
   * @return the key
   */
  public PkiCredential getOtherCredential() {
    return this.otherCredential;
  }

  /**
   * Adds an extension element to a request.
   *
   * @param request the request
   * @param extension the extension
   */
  public static void addExtension(final AuthnRequest request, final XMLObject extension) {
    request.getExtensions().getUnknownXMLObjects().add(extension);
  }

  /**
   * URL-encodes a value.
   *
   * @param value the value
   * @return the encoded value
   */
  private static String urlEncode(final String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  /**
   * The binding URI of HTTP-POST, for readability in tests.
   */
  public static final String POST = SAMLConstants.SAML2_POST_BINDING_URI;

}
