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
package se.swedenconnect.eidas.connector.config;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.ext.saml2alg.DigestMethod;
import org.opensaml.saml.ext.saml2alg.SigningMethod;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.saml2.metadata.ContactPersonTypeEnumeration;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import se.litsec.eidas.opensaml.ext.NodeCountry;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.eidas.connector.authn.EidasAuthenticationController;
import se.swedenconnect.opensaml.common.utils.LocalizedString;
import se.swedenconnect.opensaml.saml2.attribute.AttributeBuilder;
import se.swedenconnect.opensaml.saml2.metadata.EntityDescriptorContainer;
import se.swedenconnect.opensaml.saml2.metadata.EntityDescriptorUtils;
import se.swedenconnect.opensaml.saml2.metadata.build.AssertionConsumerServiceBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.ContactPersonBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.DigestMethodBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.EntityAttributesBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.EntityDescriptorBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.OrganizationBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.SPSSODescriptorBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.SigningMethodBuilder;
import se.swedenconnect.security.credential.opensaml.OpenSamlCredential;

/**
 * Configuration class for the eIDAS SP part of the connector.
 *
 * @author Martin Lindström
 */
@Configuration
public class EidasAuthenticationConfiguration {

  /** eIDAS application identifier attribute name. */
  private static final String APPLICATION_IDENTIFIER_NAME =
      "http://eidas.europa.eu/entity-attributes/application-identifier";

  /** eIDAS protocol version attribute name. */
  private static final String PROTOCOL_VERSION_NAME = "http://eidas.europa.eu/entity-attributes/protocol-version";

  /** The connector properties. */
  private final ConnectorConfigurationProperties properties;

  /** The connector credentials. */
  private final ConnectorCredentials credentials;

  /**
   * Constructor.
   *
   * @param properties the connector properties
   * @param credentials the connector credentials
   * @param buildProperties the Spring Boot build properties
   */
  public EidasAuthenticationConfiguration(final ConnectorConfigurationProperties properties,
      final ConnectorCredentials credentials, final BuildProperties buildProperties) {
    this.properties = Objects.requireNonNull(properties, "properties must not be null");
    this.credentials = Objects.requireNonNull(credentials, "credentials must not be null");
  }

  /**
   * Creates an {@link EntityDescriptorContainer} for publishing the eIDAS SP metadata.
   *
   * @param metadata the SP metadata
   * @return an {@link EntityDescriptorContainer}
   */
  @Bean
  EntityDescriptorContainer spEntityDescriptorContainer(
      @Qualifier("connector.sp.metadata") final EntityDescriptor metadata) {

    final EntityDescriptorContainer container = new EntityDescriptorContainer(
        metadata, new OpenSamlCredential(this.credentials.getSpMetadataSigningCredential()));
    container.setValidity(this.properties.getEidas().getMetadata().getValidityPeriod());

    return container;
  }

  /**
   * Gets the {@link EntityDescriptor} for the SP metadata.
   *
   * @return {@link EntityDescriptor}
   * @throws Exception for OpenSAML errors
   */
  @Bean("connector.sp.metadata")
  EntityDescriptor spMetadata() throws Exception {

    final EidasSpMetadataProperties mprop = this.properties.getEidas().getMetadata();

    final EntityDescriptorBuilder builder;
    if (mprop.getTemplate() != null) {
      final EntityDescriptor template = (EntityDescriptor) XMLObjectSupport.unmarshallFromInputStream(
          XMLObjectProviderRegistrySupport.getParserPool(), mprop.getTemplate().getInputStream());
      builder = new EntityDescriptorBuilder(template);
    }
    else {
      builder = new EntityDescriptorBuilder();
    }

    builder
        .entityID(this.properties.getEidas().getEntityId())
        .cacheDuration(mprop.getCacheDuration());

    final Extensions extensions = Optional.ofNullable(builder.object().getExtensions())
        .orElseGet(() -> {
          final Extensions e = (Extensions) XMLObjectSupport.buildXMLObject(Extensions.DEFAULT_ELEMENT_NAME);
          builder.extensions(e);
          return e;
        });

    // EntityAttributes
    //
    final EntityAttributesBuilder entityAttributesBuilder = EntityAttributesBuilder.builder();
    final EntityAttributes entityAttributes =
        EntityDescriptorUtils.getMetadataExtension(extensions, EntityAttributes.class);
    if (entityAttributes != null) {
      entityAttributesBuilder.attributes(entityAttributes.getAttributes());
      extensions.getUnknownXMLObjects().removeIf(o -> EntityAttributes.class.isAssignableFrom(o.getClass()));
    }

    // Application Identifier
    entityAttributesBuilder.object().getAttributes().removeIf(a -> APPLICATION_IDENTIFIER_NAME.equals(a.getName()));
    entityAttributesBuilder.attribute(AttributeBuilder.builder(APPLICATION_IDENTIFIER_NAME)
        .value(mprop.getApplicationIdentifierPrefix() + ApplicationVersion.getVersion())
        .build());

    // Protocol version
    if (mprop.getProtocolVersions() != null && !mprop.getProtocolVersions().isEmpty()) {
      entityAttributesBuilder.object().getAttributes().removeIf(a -> PROTOCOL_VERSION_NAME.equals(a.getName()));
      entityAttributesBuilder.attribute(AttributeBuilder.builder(PROTOCOL_VERSION_NAME)
          .value(mprop.getProtocolVersions())
          .build());
    }

    extensions.getUnknownXMLObjects().add(entityAttributesBuilder.build());

    // Algorithms
    //
    if (mprop.getDigestMethods() != null && !mprop.isIncludeDigestMethodsUnderRole()) {
      extensions.getUnknownXMLObjects().removeIf(o -> DigestMethod.class.isAssignableFrom(o.getClass()));
      mprop.getDigestMethods().stream()
          .filter(d -> StringUtils.hasText(d))
          .forEach(d -> extensions.getUnknownXMLObjects().add(DigestMethodBuilder.builder().algorithm(d).build()));
    }
    if (mprop.getSigningMethods() != null && !mprop.isIncludeSigningMethodsUnderRole()) {
      extensions.getUnknownXMLObjects().removeIf(o -> SigningMethod.class.isAssignableFrom(o.getClass()));
      mprop.getSigningMethods().stream()
          .filter(s -> StringUtils.hasText(s.getAlgorithm()))
          .forEach(s -> extensions.getUnknownXMLObjects().add(
              SigningMethodBuilder.signingMethod(s.getAlgorithm(), s.getMinKeySize(), s.getMaxKeySize())));
    }

    // SPSSODescriptor
    //
    final SPSSODescriptor existingSsoDescriptor = builder.object().getSPSSODescriptor(SAMLConstants.SAML20P_NS);
    final SPSSODescriptorBuilder descBuilder = existingSsoDescriptor != null
        ? new SPSSODescriptorBuilder(existingSsoDescriptor, true)
        : new SPSSODescriptorBuilder();

    descBuilder
        .wantAssertionsSigned(this.properties.getEidas().getRequiresSignedAssertions())
        .authnRequestsSigned(true);

    final Extensions roleExtensions = Optional.ofNullable(descBuilder.object().getExtensions())
        .orElseGet(() -> (Extensions) XMLObjectSupport.buildXMLObject(Extensions.DEFAULT_ELEMENT_NAME));

    // Node country
    final NodeCountry nodeCountry = (NodeCountry) XMLObjectSupport.buildXMLObject(NodeCountry.DEFAULT_ELEMENT_NAME);
    nodeCountry.setNodeCountry(mprop.getNodeCountry());
    roleExtensions.getUnknownXMLObjects().removeIf(o -> NodeCountry.class.isAssignableFrom(o.getClass()));
    roleExtensions.getUnknownXMLObjects().add(nodeCountry);

    // Algorithms
    if (mprop.getDigestMethods() != null && mprop.isIncludeDigestMethodsUnderRole()) {
      roleExtensions.getUnknownXMLObjects().removeIf(o -> DigestMethod.class.isAssignableFrom(o.getClass()));
      mprop.getDigestMethods().stream()
          .filter(d -> StringUtils.hasText(d))
          .forEach(d -> roleExtensions.getUnknownXMLObjects().add(DigestMethodBuilder.builder().algorithm(d).build()));
    }
    if (mprop.getSigningMethods() != null && mprop.isIncludeSigningMethodsUnderRole()) {
      roleExtensions.getUnknownXMLObjects().removeIf(o -> SigningMethod.class.isAssignableFrom(o.getClass()));
      mprop.getSigningMethods().stream()
          .filter(s -> StringUtils.hasText(s.getAlgorithm()))
          .forEach(s -> roleExtensions.getUnknownXMLObjects().add(
              SigningMethodBuilder.signingMethod(s.getAlgorithm(), s.getMinKeySize(), s.getMaxKeySize())));
    }

    if (descBuilder.object().getExtensions() == null && !roleExtensions.getUnknownXMLObjects().isEmpty()) {
      descBuilder.extensions(roleExtensions);
    }

    // Key descriptors
    //
    descBuilder.keyDescriptors(this.credentials.getSpKeyDescriptors(mprop.getEncryptionMethods()));

    // NameID formats
    //
    descBuilder.nameIDFormats(this.properties.getEidas().getSupportedNameIds());

    // AssertionConsumerService
    //
    descBuilder.assertionConsumerServices(
        AssertionConsumerServiceBuilder.builder()
            .postBinding()
            .isDefault(true)
            .index(0)
            .location(this.properties.getBaseUrl() + EidasAuthenticationController.ASSERTION_CONSUMER_PATH)
            .build());

    builder.ssoDescriptor(descBuilder.build());

    // Organization
    //
    if (mprop.getOrganization() != null) {
      builder.organization(OrganizationBuilder.builder()
          .organizationNames(Optional.ofNullable(mprop.getOrganization().getNames())
              .map(map -> map.entrySet().stream()
                  .map(e -> new LocalizedString(e.getValue(), e.getKey()))
                  .toList())
              .orElse(null))
          .organizationDisplayNames(Optional.ofNullable(mprop.getOrganization().getDisplayNames())
              .map(map -> map.entrySet().stream()
                  .map(e -> new LocalizedString(e.getValue(), e.getKey()))
                  .toList())
              .orElse(null))
          .organizationURLs(Optional.ofNullable(mprop.getOrganization().getUrls())
              .map(map -> map.entrySet().stream()
                  .map(e -> new LocalizedString(e.getValue(), e.getKey()))
                  .toList())
              .orElse(null))
          .build());
    }

    // Contact persons
    //
    if (mprop.getContactPersons() != null) {
      builder.contactPersons(mprop.getContactPersons().entrySet().stream()
          .map(e -> ContactPersonBuilder.builder()
              .type(Arrays.stream(ContactPersonTypeEnumeration.values())
                  .filter(t -> e.getKey().name().equals(t.toString()))
                  .findFirst()
                  .orElseGet(() -> ContactPersonTypeEnumeration.OTHER))
              .company(e.getValue().getCompany())
              .emailAddresses(e.getValue().getEmailAddresses())
              .givenName(e.getValue().getGivenName())
              .surname(e.getValue().getSurname())
              .telephoneNumbers(e.getValue().getTelephoneNumbers())
              .build())
          .toList());
    }

    return builder.build();
  }

}
