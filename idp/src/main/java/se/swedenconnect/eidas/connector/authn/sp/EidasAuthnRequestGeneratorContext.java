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

import org.opensaml.core.xml.Namespace;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.StatusResponseType;
import org.opensaml.xmlsec.SignatureSigningConfiguration;

import se.litsec.eidas.opensaml.common.EidasConstants;
import se.litsec.eidas.opensaml.ext.RequestedAttribute;
import se.litsec.eidas.opensaml.ext.SPTypeEnumeration;
import se.swedenconnect.opensaml.saml2.core.build.NameIDPolicyBuilder;
import se.swedenconnect.opensaml.saml2.request.AuthnRequestGeneratorContext;

/**
 * {@link AuthnRequestGeneratorContext} for eIDAS.
 * 
 * @author Martin Lindström
 */
class EidasAuthnRequestGeneratorContext implements AuthnRequestGeneratorContext {

  /**
   * The default name to use for the SAML attribute {@code ProviderName}. */
  public static final String DEFAULT_PROVIDER_NAME = "Swedish eIDAS Connector";
  
  /** The country code. */
  private final String country;

  /** The entityID of the national SP that requested authentication. */
  private final String nationalSpEntityId;

  /** The type of SP that makes the request. */
  private final SPTypeEnumeration spType;

  /** The requested attributes. */
  private final List<RequestedAttribute> requestedAttributes;

  /** The name to add to the SAML attribute {@code ProviderName}. */
  private final String providerName;

  /**
   * Constructor.
   * 
   * @param country the country code for the current IdP
   * @param nationalSpEntityId the entityID of the national SP that requested authentication
   * @param spType the SP type (if {@code null} it defaults to public)
   * @param requestedAttributes the requested SAML attributes
   * @param providerName the name to add to the field {@code providerName}.
   */
  public EidasAuthnRequestGeneratorContext(final String country, final String nationalSpEntityId,
      final SPTypeEnumeration spType, final List<RequestedAttribute> requestedAttributes,
      final String providerName) {
    this.country = Objects.requireNonNull(country, "country must not be null");
    this.nationalSpEntityId = Objects.requireNonNull(nationalSpEntityId, "nationalSpEntityId must not be null");
    this.spType = Optional.ofNullable(spType).orElseGet(() -> SPTypeEnumeration.PUBLIC);
    this.requestedAttributes = Optional.ofNullable(requestedAttributes).orElseGet(() -> Collections.emptyList());
    this.providerName = Optional.ofNullable(providerName).orElseGet(() -> DEFAULT_PROVIDER_NAME); 
  }

  /**
   * Gets the country code for the current IdP.
   * 
   * @return the country code
   */
  public String getCountryCode() {
    return this.country;
  }

  /**
   * Gets the entityID of the national SP that requested authentication.
   * 
   * @return SAML entityID
   */
  public String getNationalSpEntityId() {
    return this.nationalSpEntityId;
  }

  /**
   * Gets the SP type.
   * 
   * @return the SP type
   */
  public SPTypeEnumeration getSpType() {
    return this.spType;
  }

  /**
   * Gets the requested SAML attributes.
   * 
   * @return the requested SAML attributes
   */
  public List<RequestedAttribute> getRequestedAttributes() {
    return this.requestedAttributes;
  }

  /**
   * Always set {@code isPassive} to {@code false}.
   */
  @Override
  public Boolean getIsPassiveAttribute() {
    return Boolean.FALSE;
  }

  @Override
  public RequestedAuthnContextBuilderFunction getRequestedAuthnContextBuilderFunction() {
    // TODO Auto-generated method stub
    return AuthnRequestGeneratorContext.super.getRequestedAuthnContextBuilderFunction();
  }

  @Override
  public SignatureSigningConfiguration getSignatureSigningConfiguration() {
    // TODO Auto-generated method stub
    return AuthnRequestGeneratorContext.super.getSignatureSigningConfiguration();
  }

  /**
   * Hardwires the {@code NameIDPolicy} to persistent.
   */
  @Override
  public NameIDPolicyBuilderFunction getNameIDPolicyBuilderFunction() {
    return _list -> NameIDPolicyBuilder.builder()
        .allowCreate(true)
        .format(NameID.PERSISTENT)
        .build();
  }

  @Override
  public AuthnRequestCustomizer getAuthnRequestCustomizer() {
    return (authnRequest) -> {
      authnRequest.getNamespaceManager().registerNamespaceDeclaration(
          new Namespace(EidasConstants.EIDAS_NS, EidasConstants.EIDAS_PREFIX));

      authnRequest.setConsent(StatusResponseType.UNSPECIFIED_CONSENT);

      authnRequest.setProviderName(this.providerName);

      // The builder automatically adds the POST binding if not set. The eIDAS spec says that
      // we shouldn't assign the binding attribute, so we remove it ...
      //
      authnRequest.setProtocolBinding(null);
    };
  }

}
