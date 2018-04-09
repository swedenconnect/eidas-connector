/*
 * The eidas-connector project is the implementation of the Swedish eIDAS 
 * connector built on top of the Shibboleth IdP.
 *
 * More details on <https://github.com/elegnamnden/eidas-connector> 
 * Copyright (C) 2017 E-legitimationsnämnden
 * 
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.elegnamnden.eidas.idp.connector.sp.impl;

import org.joda.time.DateTime;
import org.opensaml.core.xml.Namespace;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.StatusResponseType;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import se.elegnamnden.eidas.idp.connector.sp.EidasAuthnRequestGenerator;
import se.elegnamnden.eidas.idp.connector.sp.EidasAuthnRequestGeneratorInput;
import se.litsec.eidas.opensaml.common.EidasConstants;
import se.litsec.eidas.opensaml.ext.SPType;
import se.litsec.eidas.opensaml.ext.SPTypeEnumeration;
import se.litsec.opensaml.saml2.common.request.AbstractAuthnRequestGenerator;
import se.litsec.opensaml.saml2.common.request.RequestGenerationException;
import se.litsec.opensaml.saml2.common.request.RequestHttpObject;
import se.litsec.opensaml.saml2.core.build.AuthnRequestBuilder;
import se.litsec.opensaml.saml2.core.build.NameIDPolicyBuilder;
import se.litsec.opensaml.saml2.core.build.RequestedAuthnContextBuilder;
import se.litsec.opensaml.saml2.metadata.PeerMetadataResolver;
import se.litsec.opensaml.utils.ObjectUtils;

/**
 * SAML Authentication Request generator implementation for eIDAS.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class EidasAuthnRequestGeneratorImpl extends AbstractAuthnRequestGenerator<EidasAuthnRequestGeneratorInput> implements
    EidasAuthnRequestGenerator, InitializingBean {

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(EidasAuthnRequestGeneratorImpl.class);

  /** The configuration object for how to create AuthnRequest objects. */
  private AuthnRequestGeneratorConfig config;

  /**
   * Constructor.
   * 
   * @param entityID
   *          the SP entityID
   */
  public EidasAuthnRequestGeneratorImpl(String entityID) {
    super(entityID);
  }

  /** {@inheritDoc} */
  @Override
  public RequestHttpObject<AuthnRequest> generateRequest(EidasAuthnRequestGeneratorInput input, PeerMetadataResolver metadataResolver)
      throws RequestGenerationException {

    log.debug("Generating AuthnRequest for IdP '{}' [{}] ...", input.getPeerEntityID(), input.getCountry());

    // IdP metadata
    final EntityDescriptor idp = this.getPeerMetadata(input, metadataResolver);

    // Find out where to send the request (and with which binding).
    //
    SingleSignOnService serviceUrl = this.getSingleSignOnService(idp, input);

    // Build the AuthnRequest.
    //
    AuthnRequestBuilder builder = AuthnRequestBuilder.builder();
    builder.object().getNamespaceManager().registerNamespaceDeclaration(new Namespace(EidasConstants.EIDAS_NS,
      EidasConstants.EIDAS_PREFIX));

    // Build request extensions with the SP type and requested attributes.
    Extensions extensions = ObjectUtils.createSamlObject(Extensions.class);
    
    if (config.isIncludeSpType()) {
      SPTypeEnumeration type = input.getSpType();
      if (type == null) {
        log.warn("SPType not set - using PUBLIC as default");
        type = SPTypeEnumeration.PUBLIC;
      }      
      SPType spType = ObjectUtils.createSamlObject(SPType.class);
      spType.setType(type);
      extensions.getUnknownXMLObjects().add(spType);      
    }
    extensions.getUnknownXMLObjects().add(input.getRequestedAttributes());

    // TODO: We should ensure that the NameIDFormat is accepted by the IdP.
    // TODO: We should also make NameIDFormat configurable.

    AuthnRequest authnRequest = builder
      .id(this.generateID())
      .destination(serviceUrl.getLocation())
      .issueInstant(new DateTime())
      .nameIDPolicy(NameIDPolicyBuilder.builder().format(NameID.PERSISTENT).allowCreate(true).build())
      .issuer(this.getEntityID())
      .forceAuthn(true)
      .isPassive(false)
      .providerName(this.getName())
      .requestedAuthnContext(
        RequestedAuthnContextBuilder.builder()
          .comparison(AuthnContextComparisonTypeEnumeration.MINIMUM)
          .authnContextClassRefs(input.getRequestedLevelOfAssurance())
          .build())
      .consent(StatusResponseType.UNSPECIFIED_CONSENT)
      .extensions(extensions)
      .build();

    if (log.isTraceEnabled()) {
      log.trace("Connector SP sending AuthnRequest: {}", ObjectUtils.toStringSafe(authnRequest));
    }

    // TODO: We should make sure that we use an algorithm that is accepted by the recipient.

    return this.buildRequestHttpObject(authnRequest, input, serviceUrl.getBinding(), serviceUrl.getLocation());
  }

  /** {@inheritDoc} */
  @Override
  protected String getDefaultBinding() {
    return this.config.getPreferredBinding();
  }

  /**
   * Assigns the configuration object for this generator.
   * 
   * @param config
   *          the config object
   */
  public void setConfig(AuthnRequestGeneratorConfig config) {
    this.config = config;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    super.afterPropertiesSet();
    Assert.notNull(this.getSigningCredentials(), "Property 'signingCredentials' must be assigned");
    
    Assert.notNull(this.config, "Property 'config' must be assigned");
  }

}
