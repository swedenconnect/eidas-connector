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

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

import org.joda.time.DateTime;
import org.opensaml.core.xml.Namespace;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.StatusResponseType;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import se.elegnamnden.eidas.idp.connector.sp.AuthnRequestInput;
import se.elegnamnden.eidas.idp.connector.sp.ProxyAuthenticationServiceProvider;
import se.elegnamnden.eidas.idp.connector.sp.ProxyAuthenticationServiceProviderException;
import se.elegnamnden.eidas.metadataconfig.MetadataConfig;
import se.elegnamnden.eidas.metadataconfig.data.EndPointConfig;
import se.elegnamnden.eidas.metadataconfig.data.ServiceUrl;
import se.litsec.eidas.opensaml.common.EidasConstants;
import se.litsec.eidas.opensaml.ext.RequestedAttributeTemplates;
import se.litsec.eidas.opensaml.ext.RequestedAttributes;
import se.litsec.eidas.opensaml.ext.SPType;
import se.litsec.eidas.opensaml.ext.SPTypeEnumeration;
import se.litsec.opensaml.saml2.authentication.PostRequestHttpObject;
import se.litsec.opensaml.saml2.authentication.RedirectRequestHttpObject;
import se.litsec.opensaml.saml2.authentication.RequestHttpObject;
import se.litsec.opensaml.saml2.core.build.AuthnRequestBuilder;
import se.litsec.opensaml.saml2.core.build.NameIDPolicyBuilder;
import se.litsec.opensaml.saml2.core.build.RequestedAuthnContextBuilder;
import se.litsec.opensaml.utils.ObjectUtils;

/**
 * Implementation of the eIDAS SP part of the Swedish eIDAS connector IdP.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class ProxyAuthenticationServiceProviderImpl implements ProxyAuthenticationServiceProvider, InitializingBean {

  public static final String PREFERRED_BINDING = SAMLConstants.SAML2_POST_BINDING_URI;

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(ProxyAuthenticationServiceProviderImpl.class);

  /** The SP entityID. */
  private String entityID;

  /** The name of the SP (for display). */
  private String name;

  /** The signature credentials for the SP. */
  private X509Credential signingCredentials;

  /** Random generator. */
  private static Random randomizer = new SecureRandom(String.valueOf(System.currentTimeMillis()).getBytes());

  public ProxyAuthenticationServiceProviderImpl() {
  }

  /** {@inheritDoc} */
  @Override
  public RequestHttpObject<AuthnRequest> generateAuthnRequest(EndPointConfig endpoint, AuthnRequestInput input)
      throws ProxyAuthenticationServiceProviderException {

    // Build the AuthnRequest.
    //
    AuthnRequestBuilder builder = AuthnRequestBuilder.builder();
    builder.object().getNamespaceManager().registerNamespaceDeclaration(new Namespace(EidasConstants.EIDAS_NS,
      EidasConstants.EIDAS_PREFIX));

    ServiceUrl serviceUrl = MetadataConfig.getPreferredServiceUrl(endpoint.getServiceUrl(), PREFERRED_BINDING);
    if (serviceUrl == null) {
      String msg = String.format("IdP '%s' does not specify endpoints for POST or Redirect - cannot send request", endpoint.getEntityID());
      log.error(msg);
      throw new ProxyAuthenticationServiceProviderException(msg);
    }

    Extensions extensions = ObjectUtils.createSamlObject(Extensions.class);

    SPType spType = ObjectUtils.createSamlObject(SPType.class);
    spType.setType(SPTypeEnumeration.PUBLIC);
    extensions.getUnknownXMLObjects().add(spType);

    extensions.getUnknownXMLObjects().add(this.getRequestedAttributes());

    AuthnRequest authnRequest = builder
      .id("_" + new BigInteger(128, randomizer).toString(16))
      .destination(serviceUrl.getUrl())
      .issueInstant(new DateTime())
      .nameIDPolicy(NameIDPolicyBuilder.builder().format(NameID.PERSISTENT).allowCreate(true).build())
      .issuer(this.entityID)
      .forceAuthn(true)
      .isPassive(false)
      .providerName(this.name)
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

    try {
      if (SAMLConstants.SAML2_REDIRECT_BINDING_URI.equals(serviceUrl.getBinding())) {
        // Redirect binding
        return new RedirectRequestHttpObject<AuthnRequest>(authnRequest, input.getRelayState(), this.signingCredentials, serviceUrl
          .getUrl());
      }
      else {
        // POST binding
        return new PostRequestHttpObject<AuthnRequest>(authnRequest, input.getRelayState(), this.signingCredentials, serviceUrl.getUrl());
      }
    }
    catch (MessageEncodingException | SignatureException e) {
      String msg = "Failed to encode/sign AuthnRequest for transport";
      log.error(msg, e);
      throw new ProxyAuthenticationServiceProviderException(msg);
    }
  }

  private RequestedAttributes getRequestedAttributes() {

    // TODO: make configurable

    RequestedAttributes requestedAttributesElement = ObjectUtils.createSamlObject(RequestedAttributes.class);
    requestedAttributesElement.getRequestedAttributes().add(RequestedAttributeTemplates.PERSON_IDENTIFIER(true, true));
    requestedAttributesElement.getRequestedAttributes().add(RequestedAttributeTemplates.CURRENT_FAMILY_NAME(true, true));
    requestedAttributesElement.getRequestedAttributes().add(RequestedAttributeTemplates.CURRENT_GIVEN_NAME(true, true));
    requestedAttributesElement.getRequestedAttributes().add(RequestedAttributeTemplates.DATE_OF_BIRTH(true, true));
    requestedAttributesElement.getRequestedAttributes().add(RequestedAttributeTemplates.GENDER(false, true));

    return requestedAttributesElement;
  }

  public void setEntityID(String entityID) {
    this.entityID = entityID;
  }

  public void setSigningCredentials(X509Credential signingCredentials) {
    this.signingCredentials = signingCredentials;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.hasText(this.entityID, "Property 'entityID' must be assigned");
    Assert.notNull(this.signingCredentials, "Property 'signingCredentials' must be assigned");
    Assert.hasText(this.name, "Property 'name' must be assigned");
  }

}
