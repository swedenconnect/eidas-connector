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

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Scoping;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;

import lombok.extern.slf4j.Slf4j;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import se.litsec.eidas.opensaml.ext.RequestedAttribute;
import se.litsec.eidas.opensaml.ext.RequestedAttributes;
import se.litsec.eidas.opensaml.ext.SPType;
import se.swedenconnect.opensaml.saml2.core.build.AuthnRequestBuilder;
import se.swedenconnect.opensaml.saml2.core.build.ExtensionsBuilder;
import se.swedenconnect.opensaml.saml2.core.build.ScopingBuilder;
import se.swedenconnect.opensaml.saml2.request.AbstractAuthnRequestGenerator;
import se.swedenconnect.opensaml.saml2.request.AuthnRequestGenerator;
import se.swedenconnect.opensaml.saml2.request.AuthnRequestGeneratorContext;
import se.swedenconnect.opensaml.saml2.request.RequestGenerationException;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.opensaml.OpenSamlCredential;

/**
 * An {@link AuthnRequestGenerator} for generating {@link AuthnRequest}s for eIDAS.
 * 
 * @author Martin Lindström
 */
@Slf4j
public class EidasAuthnRequestGenerator extends AbstractAuthnRequestGenerator {

  /** The SP metadata. */
  private final EntityDescriptor spMetadata;

  /** The metadata resolver. */
  private final MetadataResolver metadataResolver;

  /** Some countries can not handle the Scoping element. */
  private List<String> skipScopingElementFor = Collections.emptyList();

  /**
   * Constructor.
   * 
   * @param spMetadata the SP metadata
   * @param signatureCredential the signature credential
   * @param metadataResolver
   */
  public EidasAuthnRequestGenerator(final EntityDescriptor spMetadata, final PkiCredential signatureCredential,
      final MetadataResolver metadataResolver) {
    super(Objects.requireNonNull(spMetadata, "spMetadata must not be null").getEntityID(),
        new OpenSamlCredential(Objects.requireNonNull(signatureCredential, "signatureCredential must not be null")));
    this.spMetadata = spMetadata;
    this.metadataResolver = Objects.requireNonNull(metadataResolver, "metadataResolver must not be null");
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
  protected EntityDescriptor getSpMetadata() {
    return this.spMetadata;
  }

  /** {@inheritDoc} */
  @Override
  protected EntityDescriptor getIdpMetadata(final String idpEntityID) {
    try {
      final CriteriaSet criteria = new CriteriaSet();
      criteria.add(new EntityIdCriterion(idpEntityID));
      final EntityDescriptor ed = this.metadataResolver.resolveSingle(criteria);
      if (ed == null) {
        log.warn("Metadata for {} was not found", idpEntityID);
        return null;
      }
      if (ed.getRoleDescriptors(IDPSSODescriptor.DEFAULT_ELEMENT_NAME).isEmpty()) {
        log.warn("Metadata for {} was found, but is not valid metadata for an IdP", idpEntityID);
        return null;
      }
      return ed;
    }
    catch (final ResolverException e) {
      log.warn("Metadata for {} could not be resolved", idpEntityID, e);
      return null;
    }
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
