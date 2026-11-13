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

import org.jspecify.annotations.NonNull;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.metadata.ContactPerson;
import org.opensaml.saml.saml2.metadata.ContactPersonTypeEnumeration;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.SecurityConfigurationSupport;
import org.opensaml.xmlsec.signature.support.SignatureException;
import se.swedenconnect.eidas.simulator.config.CountryProperties;
import se.swedenconnect.eidas.simulator.country.CountryRepository;
import se.swedenconnect.eidas.simulator.country.SimulatedCountry;
import se.swedenconnect.opensaml.common.utils.LocalizedString;
import se.swedenconnect.opensaml.eidas.common.EidasConstants;
import se.swedenconnect.opensaml.eidas.ext.NodeCountry;
import se.swedenconnect.opensaml.saml2.attribute.AttributeBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.ContactPersonBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.DigestMethodBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.EntityAttributesBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.EntityDescriptorBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.ExtensionsBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.IDPSSODescriptorBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.KeyDescriptorBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.OrganizationBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.SigningMethodBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.SingleSignOnServiceBuilder;
import se.swedenconnect.opensaml.xmlsec.signature.support.SAMLObjectSigner;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.opensaml.OpenSamlCredential;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Creates the metadata of the simulated countries: one {@code EntityDescriptor} per country, and the signed
 * aggregate ({@code EntitiesDescriptor}) holding all active countries.
 *
 * @author Martin Lindström
 */
public class CountryMetadataFactory {

  /** The entity attribute that lists the supported encryption algorithms. */
  public static final @NonNull String SUPPORTED_ENCRYPTION_ALGORITHMS_ATTRIBUTE_NAME =
      "http://eidas.europa.eu/entity-attributes/supported-encryption-algorithms";

  /** The entity attribute holding entity categories. */
  public static final @NonNull String ENTITY_CATEGORY_ATTRIBUTE_NAME = "http://macedir.org/entity-category";

  /** The e-mail address of the contact persons of the simulated nodes. */
  private static final @NonNull String CONTACT_EMAIL = "simulator@local.eidas.swedenconnect.se";

  /** The active countries. */
  private final @NonNull CountryRepository countries;

  /** The metadata signing credential. */
  private final @NonNull PkiCredential signingCredential;

  /** The base URL of the simulator. */
  private final @NonNull String baseUrl;

  /** For how long the metadata is valid. */
  private final @NonNull Duration validity;

  /** For how long the metadata may be cached. */
  private final @NonNull Duration cacheDuration;

  /**
   * Constructor.
   *
   * @param countries the active countries
   * @param signingCredential the metadata signing credential
   * @param baseUrl the base URL of the simulator
   * @param validity for how long the metadata is valid
   * @param cacheDuration for how long the metadata may be cached
   */
  public CountryMetadataFactory(final @NonNull CountryRepository countries,
      final @NonNull PkiCredential signingCredential, final @NonNull String baseUrl, final @NonNull Duration validity,
      final @NonNull Duration cacheDuration) {
    this.countries = Objects.requireNonNull(countries, "countries must not be null");
    this.signingCredential = Objects.requireNonNull(signingCredential, "signingCredential must not be null");
    this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
    this.validity = Objects.requireNonNull(validity, "validity must not be null");
    this.cacheDuration = Objects.requireNonNull(cacheDuration, "cacheDuration must not be null");
  }

  /**
   * Creates the signed aggregate holding the entries of all active countries.
   *
   * @return a signed {@link EntitiesDescriptor}
   * @throws SignatureException if the aggregate cannot be signed
   */
  public @NonNull EntitiesDescriptor createAggregate() throws SignatureException {
    final EntitiesDescriptor entities =
        (EntitiesDescriptor) XMLObjectSupport.buildXMLObject(EntitiesDescriptor.DEFAULT_ELEMENT_NAME);
    entities.setID(newId());
    entities.setName(this.baseUrl + "/metadata");
    entities.setValidUntil(Instant.now().plus(this.validity));
    entities.setCacheDuration(this.cacheDuration);
    for (final SimulatedCountry country : this.countries.getCountries()) {
      entities.getEntityDescriptors().add(this.createEntityDescriptor(country));
    }
    this.sign(entities);
    return entities;
  }

  /**
   * Creates the signed entry of one country.
   *
   * @param country the country
   * @return a signed {@link EntityDescriptor}
   * @throws SignatureException if the entry cannot be signed
   */
  public @NonNull EntityDescriptor createSignedEntityDescriptor(final @NonNull SimulatedCountry country)
      throws SignatureException {
    final EntityDescriptor descriptor = this.createEntityDescriptor(country);
    descriptor.setID(newId());
    descriptor.setValidUntil(Instant.now().plus(this.validity));
    descriptor.setCacheDuration(this.cacheDuration);
    this.sign(descriptor);
    return descriptor;
  }

  /**
   * Creates the (unsigned) entry of one country.
   *
   * @param country the country
   * @return an {@link EntityDescriptor}
   */
  public @NonNull EntityDescriptor createEntityDescriptor(final @NonNull SimulatedCountry country) {
    final CountryProperties settings = country.getSettings();

    final List<XMLObject> entityExtensions = new ArrayList<>();
    entityExtensions.add(this.createEntityAttributes(settings));
    settings.getDigestMethods().forEach(d -> entityExtensions.add(DigestMethodBuilder.digestMethod(d)));
    settings.getSigningMethods().forEach(s -> entityExtensions.add(
        SigningMethodBuilder.signingMethod(s.getAlgorithm(), s.getMinKeySize(), s.getMaxKeySize())));

    final List<KeyDescriptor> keyDescriptors = new ArrayList<>();
    keyDescriptors.add(KeyDescriptorBuilder.builder()
        .use(UsageType.SIGNING)
        .certificate(country.getSigningCredential().getCertificate())
        .build());
    final PkiCredential encryptionCredential = country.getEncryptionCredential();
    if (settings.getEncryptionKey() != null && encryptionCredential != null) {
      keyDescriptors.add(KeyDescriptorBuilder.builder()
          .use(UsageType.ENCRYPTION)
          .certificate(encryptionCredential.getCertificate())
          .encryptionMethods(settings.getEncryptionKey().getEncryptionMethods())
          .build());
    }

    final List<SingleSignOnService> ssoServices = settings.getBindings().stream()
        .map(b -> SingleSignOnServiceBuilder.builder().binding(b).location(country.getSsoUrl()).build())
        .toList();

    final List<Attribute> attributes = settings.getAttributes().stream()
        .map(a -> {
          final Attribute attribute = AttributeBuilder.builder(Objects.requireNonNull(a.getName()))
              .friendlyName(a.getFriendlyName())
              .build();
          if (a.getNameFormat() != null) {
            attribute.setNameFormat(a.getNameFormat().isBlank() ? null : a.getNameFormat());
          }
          return attribute;
        })
        .toList();

    final NodeCountry nodeCountry = (NodeCountry) XMLObjectSupport.buildXMLObject(NodeCountry.DEFAULT_ELEMENT_NAME);
    nodeCountry.setNodeCountry(country.getCode());

    final String organizationName = "Simulated eIDAS node, " + country.getName();

    return EntityDescriptorBuilder.builder()
        .entityID(country.getEntityId())
        .extensions(ExtensionsBuilder.builder().extensions(entityExtensions).build())
        .ssoDescriptor(IDPSSODescriptorBuilder.builder()
            .wantAuthnRequestsSigned(true)
            .extensions(ExtensionsBuilder.builder().extensions(nodeCountry).build())
            .keyDescriptors(keyDescriptors)
            .nameIDFormats(settings.getNameIdFormats())
            .singleSignOnServices(ssoServices)
            .attributes(attributes)
            .build())
        .organization(OrganizationBuilder.builder()
            .organizationNames(new LocalizedString(organizationName, "en"))
            .organizationDisplayNames(new LocalizedString(organizationName, "en"))
            .organizationURLs(new LocalizedString(this.baseUrl, "en"))
            .build())
        .contactPersons(
            this.createContactPerson(ContactPersonTypeEnumeration.SUPPORT, organizationName),
            this.createContactPerson(ContactPersonTypeEnumeration.TECHNICAL, organizationName))
        .build();
  }

  /**
   * Creates the entity attributes of a country.
   *
   * @param settings the country settings
   * @return an {@link EntityAttributes} element
   */
  private @NonNull EntityAttributes createEntityAttributes(final @NonNull CountryProperties settings) {
    final EntityAttributesBuilder builder = EntityAttributesBuilder.builder();
    if (!settings.getProtocolVersions().isEmpty()) {
      builder.attribute(AttributeBuilder.builder(EidasConstants.EIDAS_PROTOCOL_VERSION_ATTRIBUTE_NAME)
          .value(settings.getProtocolVersions())
          .build());
    }
    if (settings.getApplicationIdentifier() != null) {
      builder.attribute(AttributeBuilder.builder(EidasConstants.EIDAS_APPLICATION_IDENTIFIER_ATTRIBUTE_NAME)
          .value(settings.getApplicationIdentifier())
          .build());
    }
    if (!settings.getAssuranceCertifications().isEmpty()) {
      builder.assuranceCertificationAttribute(settings.getAssuranceCertifications());
    }
    if (!settings.getSupportedEncryptionAlgorithms().isEmpty()) {
      builder.attribute(AttributeBuilder.builder(SUPPORTED_ENCRYPTION_ALGORITHMS_ATTRIBUTE_NAME)
          .value(settings.getSupportedEncryptionAlgorithms())
          .build());
    }
    if (!settings.getEntityCategories().isEmpty()) {
      builder.attribute(AttributeBuilder.builder(ENTITY_CATEGORY_ATTRIBUTE_NAME)
          .value(settings.getEntityCategories())
          .build());
    }
    return builder.build();
  }

  /**
   * Creates a contact person that marks the entry as simulated.
   *
   * @param type the contact type
   * @param company the company
   * @return a contact person
   */
  private @NonNull ContactPerson createContactPerson(
      final @NonNull ContactPersonTypeEnumeration type, final @NonNull String company) {
    return ContactPersonBuilder.builder()
        .type(type)
        .company(company)
        .givenName("Simulator")
        .emailAddresses(CONTACT_EMAIL)
        .build();
  }

  /**
   * Signs a metadata object with the metadata signing key.
   *
   * @param object the object to sign
   * @throws SignatureException if signing fails
   */
  private void sign(final @NonNull SignableSAMLObject object) throws SignatureException {
    SAMLObjectSigner.sign(object, new OpenSamlCredential(this.signingCredential),
        SecurityConfigurationSupport.getGlobalSignatureSigningConfiguration());
  }

  /**
   * Creates an ID for a metadata object.
   *
   * @return the ID
   */
  private static @NonNull String newId() {
    return "_" + UUID.randomUUID().toString().replace("-", "");
  }

}
