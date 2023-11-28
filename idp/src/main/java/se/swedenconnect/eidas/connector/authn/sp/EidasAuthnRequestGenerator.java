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
package se.swedenconnect.eidas.connector.authn.sp;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Scoping;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.eidas.attributes.AttributeMappingService;
import se.swedenconnect.eidas.connector.authn.metadata.CountryMetadata;
import se.swedenconnect.eidas.connector.authn.metadata.MetadataFunctions;
import se.swedenconnect.eidas.connector.config.EidasAuthenticationProperties;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttribute;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttributes;
import se.swedenconnect.opensaml.eidas.ext.SPType;
import se.swedenconnect.opensaml.eidas.ext.SPTypeEnumeration;
import se.swedenconnect.opensaml.saml2.core.build.AuthnRequestBuilder;
import se.swedenconnect.opensaml.saml2.core.build.ExtensionsBuilder;
import se.swedenconnect.opensaml.saml2.core.build.ScopingBuilder;
import se.swedenconnect.opensaml.saml2.request.AbstractAuthnRequestGenerator;
import se.swedenconnect.opensaml.saml2.request.AuthnRequestGenerator;
import se.swedenconnect.opensaml.saml2.request.AuthnRequestGeneratorContext;
import se.swedenconnect.opensaml.saml2.request.RequestGenerationException;
import se.swedenconnect.opensaml.saml2.request.RequestHttpObject;
import se.swedenconnect.opensaml.sweid.saml2.metadata.entitycategory.EntityCategoryConstants;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.opensaml.OpenSamlCredential;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;

/**
 * An {@link AuthnRequestGenerator} for generating {@link AuthnRequest}s for eIDAS.
 *
 * @author Martin Lindström
 */
@Slf4j
public class EidasAuthnRequestGenerator extends AbstractAuthnRequestGenerator {

  /** The SP metadata. */
  private final EntityDescriptor spMetadata;

  /** Attribute mappings. */
  private final AttributeMappingService attributeMappings;

  /** The name to add to the field {@code providerName}. */
  private final String providerName;

  /** Some countries can not handle the Scoping element. */
  private List<String> skipScopingElementFor = Collections.emptyList();

  /**
   * Constructor.
   *
   * @param spMetadata the SP metadata
   * @param signatureCredential the signature credential
   * @param attributeMappings attribute mapping service
   * @param providerName the provider name to use in generated requests
   */
  public EidasAuthnRequestGenerator(final EntityDescriptor spMetadata, final PkiCredential signatureCredential,
      final AttributeMappingService attributeMappings, final String providerName) {
    super(Objects.requireNonNull(spMetadata, "spMetadata must not be null").getEntityID(),
        new OpenSamlCredential(Objects.requireNonNull(signatureCredential, "signatureCredential must not be null")));
    this.spMetadata = spMetadata;
    this.attributeMappings = Objects.requireNonNull(attributeMappings, "attributeMappings must not be null");
    this.providerName = Objects.requireNonNullElse(providerName, EidasAuthenticationProperties.DEFAULT_PROVIDER_NAME);
  }

  /**
   * Generates an {@link AuthnRequest} to be sent to the foreign IdP.
   *
   * @param country the country metadata
   * @param token the input SAML token
   * @param relayState the RelayState to use
   * @return a {@link RequestHttpObject}
   * @throws RequestGenerationException for errors generating the {@code AuthnRequest}
   */
  public RequestHttpObject<AuthnRequest> generateAuthnRequest(final CountryMetadata country,
      final Saml2UserAuthenticationInputToken token, final String relayState) throws RequestGenerationException {

    // Is this a public or private SP?
    //
    final SPTypeEnumeration spType = token.getAuthnRequirements().getEntityCategories()
        .contains(EntityCategoryConstants.SERVICE_TYPE_CATEGORY_PRIVATE_SECTOR_SP.getUri())
            ? SPTypeEnumeration.PRIVATE
            : SPTypeEnumeration.PUBLIC;

    // Map requested attributes from Swedish eID format to eIDAS format ...
    //
    final List<se.swedenconnect.opensaml.eidas.ext.RequestedAttribute> requestedAttributes =
        this.attributeMappings.toEidasRequestedAttributes(token.getAuthnRequirements().getRequestedAttributes(), true);

    // Set up a context ...
    //
    final EidasAuthnRequestGeneratorContext context = new EidasAuthnRequestGeneratorContext(
        country.getCountryCode(), token.getAuthnRequestToken().getEntityId(), spType, requestedAttributes,
        token.getAuthnRequirements().getAuthnContextRequirements(), this.providerName);

    // Generate AuthnRequest ...
    //
    return this.generateAuthnRequest(country.getEntityDescriptor(), relayState, context);
  }

  /**
   * Adds a Scoping element containing a RequesterID for the Swedish SP.
   */
  @Override
  protected void addScoping(final AuthnRequestBuilder builder, final AuthnRequestGeneratorContext context,
      final EntityDescriptor idpMetadata) throws RequestGenerationException {

    final EidasAuthnRequestGeneratorContext eidasContext = EidasAuthnRequestGeneratorContext.class.cast(context);
    final String country = eidasContext.getCountryCode();
    if (this.skipScopingElementFor.contains(country)) {
      log.debug("AuthnRequest generator will not add Scoping element for {} - configured to skip", country);
      return;
    }
    final Scoping scoping = ScopingBuilder.builder().requesterIDs(eidasContext.getNationalSpEntityId()).build();
    builder.scoping(scoping);
  }

  /**
   * Adds the {@code SPType} extension and the requested attributes.
   */
  @Override
  protected void addExtensions(final AuthnRequestBuilder builder, final AuthnRequestGeneratorContext context,
      final EntityDescriptor idpMetadata) throws RequestGenerationException {

    final EidasAuthnRequestGeneratorContext eidasContext = EidasAuthnRequestGeneratorContext.class.cast(context);

    final ExtensionsBuilder extensionsBuilder = ExtensionsBuilder.builder();

    final SPType spType = (SPType) XMLObjectSupport.buildXMLObject(SPType.DEFAULT_ELEMENT_NAME);
    spType.setType(eidasContext.getSpType());
    extensionsBuilder.extension(spType);

    final List<RequestedAttribute> requestedAttributesList = eidasContext.getRequestedAttributes();
    if (!requestedAttributesList.isEmpty()) {
      final RequestedAttributes requestedAttributes =
          (RequestedAttributes) XMLObjectSupport.buildXMLObject(RequestedAttributes.DEFAULT_ELEMENT_NAME);
      requestedAttributes.getRequestedAttributes().addAll(requestedAttributesList);
      extensionsBuilder.extension(requestedAttributes);
    }

    builder.extensions(extensionsBuilder.build());
  }

  /** {@inheritDoc} */
  @Override
  protected List<String> getAssuranceCertificationUris(final EntityDescriptor idpMetadata,
      final AuthnRequestGeneratorContext context) throws RequestGenerationException {
    return MetadataFunctions.getAssuranceLevels(idpMetadata);
  }

  /** {@inheritDoc} */
  @Override
  protected EntityDescriptor getSpMetadata() {
    return this.spMetadata;
  }

  /**
   * Some countries can not handle the Scoping element. This method adds the list of countries for which we should skip
   * Scoping.
   *
   * @param skipScopingElementFor list of countries
   */
  public void setSkipScopingElementFor(final List<String> skipScopingElementFor) {
    this.skipScopingElementFor = Optional.ofNullable(skipScopingElementFor)
        .orElseGet(() -> Collections.emptyList());
  }

}
