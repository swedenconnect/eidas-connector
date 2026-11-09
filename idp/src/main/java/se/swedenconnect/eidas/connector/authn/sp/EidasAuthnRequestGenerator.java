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
package se.swedenconnect.eidas.connector.authn.sp;

import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Scoping;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedenconnect.eidas.connector.attributes.EidasRequestedAttributesBuilder;
import se.swedenconnect.eidas.connector.authn.SamlRequestData;
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
import se.swedenconnect.opensaml.xmlsec.config.SecurityConfiguration;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.opensaml.OpenSamlCredential;
import se.swedenconnect.spring.authnserver.authentication.provider.UserAuthenticationInputToken;
import se.swedenconnect.spring.authnserver.saml.authentication.SamlAuthenticationRequirements;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An {@link AuthnRequestGenerator} for generating {@link AuthnRequest}s for eIDAS.
 *
 * @author Martin Lindström
 */
public class EidasAuthnRequestGenerator extends AbstractAuthnRequestGenerator {

  /** Logger instance. */
  private static final Logger log = LoggerFactory.getLogger(EidasAuthnRequestGenerator.class);

  /** The SP metadata. */
  private final EntityDescriptor spMetadata;

  /** Works out the eIDAS requested attributes. */
  private final EidasRequestedAttributesBuilder requestedAttributesBuilder;

  /** The name to add to the field {@code providerName}. */
  private final String providerName;

  /** Some countries can not handle the Scoping element. */
  private List<String> skipScopingElementFor = Collections.emptyList();

  /** Preferred binding to use for authentication requests. */
  private String preferredBinding;

  /** The security configuration we use for signing. */
  private final SecurityConfiguration securityConfiguration;

  /**
   * Constructor.
   *
   * @param spMetadata the SP metadata
   * @param signatureCredential the signature credential
   * @param securityConfiguration the security configuration
   * @param requestedAttributesBuilder works out the eIDAS requested attributes
   * @param providerName the provider name to use in generated requests
   */
  public EidasAuthnRequestGenerator(final EntityDescriptor spMetadata, final PkiCredential signatureCredential,
      final SecurityConfiguration securityConfiguration,
      final EidasRequestedAttributesBuilder requestedAttributesBuilder, final String providerName) {
    super(Objects.requireNonNull(spMetadata, "spMetadata must not be null").getEntityID(),
        new OpenSamlCredential(Objects.requireNonNull(signatureCredential, "signatureCredential must not be null")));
    this.securityConfiguration =
        Objects.requireNonNull(securityConfiguration, "securityConfiguration must not be null");
    this.spMetadata = spMetadata;
    this.requestedAttributesBuilder =
        Objects.requireNonNull(requestedAttributesBuilder, "requestedAttributesBuilder must not be null");
    this.providerName = Objects.requireNonNullElse(providerName, EidasAuthenticationProperties.DEFAULT_PROVIDER_NAME);
  }

  /**
   * Generates an {@link AuthnRequest} to be sent to the foreign IdP.
   *
   * @param country the country metadata
   * @param token the input token for the authentication requested by the Swedish SP
   * @param relayState the RelayState to use
   * @return a {@link RequestHttpObject}
   * @throws RequestGenerationException for errors generating the {@code AuthnRequest}
   */
  public RequestHttpObject<AuthnRequest> generateAuthnRequest(final CountryMetadata country,
      final UserAuthenticationInputToken token, final String relayState) throws RequestGenerationException {

    // Is this a public or private SP?
    //
    final List<String> entityCategories = token.getAuthnRequirements() instanceof final SamlAuthenticationRequirements r
        ? r.getEntityCategories()
        : Collections.emptyList();
    final SPTypeEnumeration spType =
        entityCategories.contains(EntityCategoryConstants.SERVICE_TYPE_CATEGORY_PRIVATE_SECTOR_SP.getUri())
            ? SPTypeEnumeration.PRIVATE
            : SPTypeEnumeration.PUBLIC;

    // Map requested attributes from Swedish eID format to eIDAS format ...
    //
    final List<RequestedAttribute> requestedAttributes = this.requestedAttributesBuilder.build(
        SamlRequestData.getAuthnRequest(token), SamlRequestData.getSpMetadata(token), true);

    // Set up a context ...
    //
    final EidasAuthnRequestGeneratorContext context = new EidasAuthnRequestGeneratorContext(
        country.getCountryCode(), token.getRequester().identifier(), spType, requestedAttributes,
        token.getAuthnRequirements().getAuthnContextRequirements(), this.providerName, this.preferredBinding,
        this.securityConfiguration);

    // Generate AuthnRequest ...
    //
    return this.generateAuthnRequest(country.getEntityDescriptor(), relayState, context);
  }

  /**
   * Adds a Scoping element containing a RequesterID for the Swedish SP.
   */
  @Override
  protected void addScoping(final AuthnRequestBuilder builder, final AuthnRequestGeneratorContext context,
      final EntityDescriptor idpMetadata) {

    final EidasAuthnRequestGeneratorContext eidasContext = (EidasAuthnRequestGeneratorContext) context;
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
      final EntityDescriptor idpMetadata) {

    final EidasAuthnRequestGeneratorContext eidasContext = (EidasAuthnRequestGeneratorContext) context;

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
      final AuthnRequestGeneratorContext context) {
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
        .orElseGet(Collections::emptyList);
  }

  /**
   * Sets the preferred binding to use when sending authentication requests.
   *
   * @param preferredBinding URI
   */
  public void setPreferredBinding(final String preferredBinding) {
    if (preferredBinding != null) {
      this.preferredBinding = preferredBinding;
    }
  }

}
