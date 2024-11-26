/*
 * Copyright 2017-2024 Sweden Connect
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

import net.shibboleth.shared.component.ComponentInitializationException;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.ext.saml2alg.DigestMethod;
import org.opensaml.saml.ext.saml2alg.SigningMethod;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.encryption.EncryptedElementTypeEncryptedKeyResolver;
import org.opensaml.saml.saml2.metadata.ContactPersonTypeEnumeration;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.DecryptionConfiguration;
import org.opensaml.xmlsec.DecryptionParameters;
import org.opensaml.xmlsec.encryption.support.ChainingEncryptedKeyResolver;
import org.opensaml.xmlsec.encryption.support.InlineEncryptedKeyResolver;
import org.opensaml.xmlsec.encryption.support.SimpleKeyInfoReferenceEncryptedKeyResolver;
import org.opensaml.xmlsec.encryption.support.SimpleRetrievalMethodEncryptedKeyResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import se.swedenconnect.eidas.attributes.AttributeMappingService;
import se.swedenconnect.eidas.attributes.DefaultAttributeMappingService;
import se.swedenconnect.eidas.attributes.conversion.AttributeConverterConstants;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.eidas.connector.authn.EidasAuthenticationController;
import se.swedenconnect.eidas.connector.authn.metadata.EuMetadataProvider;
import se.swedenconnect.eidas.connector.authn.sp.EidasAuthnRequestGenerator;
import se.swedenconnect.eidas.connector.authn.sp.EidasResponseProcessor;
import se.swedenconnect.opensaml.OpenSAMLInitializer;
import se.swedenconnect.opensaml.common.utils.LocalizedString;
import se.swedenconnect.opensaml.eidas.ext.NodeCountry;
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
import se.swedenconnect.opensaml.saml2.response.replay.MessageReplayChecker;
import se.swedenconnect.opensaml.saml2.response.validation.ResponseValidationSettings;
import se.swedenconnect.opensaml.xmlsec.config.ExtendedDefaultSecurityConfigurationBootstrap;
import se.swedenconnect.opensaml.xmlsec.config.SecurityConfiguration;
import se.swedenconnect.opensaml.xmlsec.encryption.support.DecryptionUtils;
import se.swedenconnect.opensaml.xmlsec.encryption.support.SAMLObjectDecrypter;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.opensaml.OpenSamlCredential;
import se.swedenconnect.spring.saml.idp.settings.IdentityProviderSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Configuration class for the eIDAS SP part of the connector.
 *
 * @author Martin Lindström
 */
@Configuration
@EnableConfigurationProperties(ConnectorConfigurationProperties.class)
public class EidasAuthenticationConfiguration {

  /** eIDAS application identifier attribute name. */
  private static final String APPLICATION_IDENTIFIER_NAME =
      "http://eidas.europa.eu/entity-attributes/application-identifier";

  /** eIDAS protocol version attribute name. */
  private static final String PROTOCOL_VERSION_NAME = "http://eidas.europa.eu/entity-attributes/protocol-version";

  /** The connector properties. */
  private final ConnectorConfigurationProperties properties;

  /** The IdP settings. */
  private final IdentityProviderSettings idpSettings;

  /** The connector credentials. */
  private final ConnectorCredentials credentials;

  /**
   * Constructor.
   *
   * @param properties the connector properties
   * @param idpSettings the SAML IP settings
   * @param credentials the connector credentials
   * @param buildProperties the Spring Boot build properties
   */
  public EidasAuthenticationConfiguration(final ConnectorConfigurationProperties properties,
      final IdentityProviderSettings idpSettings, final ConnectorCredentials credentials,
      final BuildProperties buildProperties) {
    this.properties = properties;
    this.idpSettings = idpSettings;
    this.credentials = credentials;
  }

  /**
   * Gets the {@link SecurityConfiguration} for eIDAS.
   *
   * @return a {@link SecurityConfiguration}
   */
  @Bean("connector.sp.SecurityConfiguration")
  SecurityConfiguration eidasSecurityConfiguration() {
    return new ConnectorSecurityConfiguration();
  }

  /**
   * Gets the {@link EidasAuthnRequestGenerator} that we use to generate {@link AuthnRequest} messages with.
   *
   * @param metadata the SP metadata
   * @param securityConfiguration the security configuration
   * @param attributeMappingService attribute mappings
   * @return {@link EidasAuthnRequestGenerator}
   */
  @Bean
  EidasAuthnRequestGenerator eidasAuthnRequestGenerator(
      @Qualifier("connector.sp.metadata") final EntityDescriptor metadata,
      @Qualifier("connector.sp.SecurityConfiguration") final SecurityConfiguration securityConfiguration,
      final AttributeMappingService attributeMappingService) {

    final EidasAuthnRequestGenerator generator = new EidasAuthnRequestGenerator(
        metadata, this.credentials.getSpSigningCredential(), securityConfiguration,
        attributeMappingService, this.properties.getEidas().getProviderName());
    generator.setSkipScopingElementFor(this.properties.getEidas().getSkipScopingFor());
    generator.setPreferredBinding(this.properties.getEidas().getPreferredBinding());

    return generator;
  }

  /**
   * Creates a {@link AttributeMappingService} that helps us map between Swedish eID attributes and eIDAS attributes.
   *
   * @return a {@link AttributeMappingService}
   */
  @Bean
  AttributeMappingService attributeMappingService() {
    return new DefaultAttributeMappingService(AttributeConverterConstants.DEFAULT_CONVERTERS);
  }

  /**
   * Sets up a {@link SAMLObjectDecrypter} for use when decrypting assertions.
   *
   * @param securityConfiguration the {@link SecurityConfiguration} to use
   * @return a {@link SAMLObjectDecrypter}
   */
  @Bean("connector.sp.SAMLObjectDecrypter")
  SAMLObjectDecrypter samlObjectDecrypter(
      @Qualifier("connector.sp.SecurityConfiguration") final SecurityConfiguration securityConfiguration) {
    final List<PkiCredential> credentials = this.credentials.getSpEncryptCredentials();

    final List<Credential> creds = new ArrayList<>(credentials.size());
    credentials.forEach(c -> creds.add(new OpenSamlCredential(c)));
    final boolean pkcs11Mode = credentials.stream().anyMatch(PkiCredential::isHardwareCredential);

    final DecryptionParameters decryptionParameters = this.createDecryptionParameters(securityConfiguration, creds);
    final SAMLObjectDecrypter decrypter = new SAMLObjectDecrypter(decryptionParameters);
    decrypter.setPkcs11Workaround(pkcs11Mode);

    return decrypter;
  }

  private DecryptionParameters createDecryptionParameters(
      final SecurityConfiguration securityConfiguration,
      final List<Credential> credentials) {

    final DecryptionParameters parameters = new DecryptionParameters();

    final DecryptionConfiguration config = Optional.ofNullable(securityConfiguration.getDecryptionConfiguration())
        .orElseGet(() -> Optional.ofNullable(ConfigurationService.get(DecryptionConfiguration.class))
            .orElseGet(ExtendedDefaultSecurityConfigurationBootstrap::buildDefaultDecryptionConfiguration));

    parameters.setExcludedAlgorithms(config.getExcludedAlgorithms());
    parameters.setIncludedAlgorithms(config.getIncludedAlgorithms());
    parameters.setDataKeyInfoCredentialResolver(config.getDataKeyInfoCredentialResolver());

    // We set our own encrypted key resolver (OpenSAML defaults don't include EncryptedElementTypeEncryptedKeyResolver).
    //
    final ChainingEncryptedKeyResolver encryptedKeyResolver = new ChainingEncryptedKeyResolver(Arrays.asList(
        new InlineEncryptedKeyResolver(),
        new EncryptedElementTypeEncryptedKeyResolver(),
        new SimpleRetrievalMethodEncryptedKeyResolver(),
        new SimpleKeyInfoReferenceEncryptedKeyResolver()));

    parameters.setEncryptedKeyResolver(encryptedKeyResolver);

    // Based on the supplied local credentials, set a key info credential resolver.
    //
    parameters.setKEKKeyInfoCredentialResolver(
        DecryptionUtils.createKeyInfoCredentialResolver(credentials.toArray(Credential[]::new)));

    return parameters;
  }

  /**
   * Creates a {@link EidasResponseProcessor}.
   *
   * @param euMetadataProvider the EU metadata
   * @param securityConfiguration the security configuration
   * @param decrypter object decrypter
   * @param messageReplayChecker the message replay checker
   * @return a {@link EidasResponseProcessor}.
   * @throws ComponentInitializationException for init errors
   */
  @Bean
  EidasResponseProcessor eidasResponseProcessor(final EuMetadataProvider euMetadataProvider,
      @Qualifier("connector.sp.SecurityConfiguration") final SecurityConfiguration securityConfiguration,
      @Qualifier("connector.sp.SAMLObjectDecrypter") final SAMLObjectDecrypter decrypter,
      final MessageReplayChecker messageReplayChecker) throws ComponentInitializationException {

    final ResponseValidationSettings validationSettings = new ResponseValidationSettings();
    validationSettings.setRequireSignedAssertions(
        this.properties.getEidas().getRequiresSignedAssertions());
    validationSettings.setAllowedClockSkew(this.idpSettings.getClockSkewAdjustment());
    validationSettings.setMaxAgeResponse(this.idpSettings.getMaxMessageAge());

    // TODO: Make configurable
    validationSettings.setStrictValidation(false);

    final EidasResponseProcessor processor = new EidasResponseProcessor(
        euMetadataProvider.getProvider().getMetadataResolver(), securityConfiguration, decrypter,
        messageReplayChecker, validationSettings);
    processor.initialize();

    return processor;
  }

  /**
   * Creates an {@link EntityDescriptorContainer} for publishing the eIDAS SP metadata.
   *
   * @param securityConfiguration the security configuration
   * @param metadata the SP metadata
   * @return an {@link EntityDescriptorContainer}
   */
  @Bean("connector.sp.EntityDescriptorContainer")
  EntityDescriptorContainer spEntityDescriptorContainer(
      @Qualifier("connector.sp.SecurityConfiguration") final SecurityConfiguration securityConfiguration,
      @Qualifier("connector.sp.metadata") final EntityDescriptor metadata) {

    final EntityDescriptorContainer container = new EntityDescriptorContainer(
        metadata, new OpenSamlCredential(this.credentials.getSpMetadataSigningCredential()));
    container.setValidity(this.properties.getEidas().getMetadata().getValidityPeriod());
    container.setSigningConfiguration(securityConfiguration.getSignatureSigningConfiguration());

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
          Optional.ofNullable(XMLObjectProviderRegistrySupport.getParserPool()).orElseGet(() -> {
            try {
              return OpenSAMLInitializer.createDefaultParserPool();
            }
            catch (final ComponentInitializationException e) {
              throw new RuntimeException(e);
            }
          }), mprop.getTemplate().getInputStream());
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
    if (!mprop.getProtocolVersions().isEmpty()) {
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
          .filter(StringUtils::hasText)
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
    nodeCountry.setNodeCountry(this.properties.getCountry());
    roleExtensions.getUnknownXMLObjects().removeIf(o -> NodeCountry.class.isAssignableFrom(o.getClass()));
    roleExtensions.getUnknownXMLObjects().add(nodeCountry);

    // Algorithms
    if (mprop.getDigestMethods() != null && mprop.isIncludeDigestMethodsUnderRole()) {
      roleExtensions.getUnknownXMLObjects().removeIf(o -> DigestMethod.class.isAssignableFrom(o.getClass()));
      mprop.getDigestMethods().stream()
          .filter(StringUtils::hasText)
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
                  .orElse(ContactPersonTypeEnumeration.OTHER))
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
