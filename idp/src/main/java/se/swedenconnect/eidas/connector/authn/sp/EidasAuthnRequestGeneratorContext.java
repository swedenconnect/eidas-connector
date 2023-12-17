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
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.StatusResponseType;
import org.opensaml.xmlsec.SignatureSigningConfiguration;

import se.swedenconnect.eidas.connector.config.EidasAuthenticationProperties;
import se.swedenconnect.opensaml.eidas.common.EidasConstants;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttribute;
import se.swedenconnect.opensaml.eidas.ext.SPTypeEnumeration;
import se.swedenconnect.opensaml.saml2.core.build.NameIDPolicyBuilder;
import se.swedenconnect.opensaml.saml2.request.AuthnRequestGeneratorContext;

/**
 * {@link AuthnRequestGeneratorContext} for eIDAS.
 *
 * @author Martin Lindström
 */
class EidasAuthnRequestGeneratorContext implements AuthnRequestGeneratorContext {

  /** The country code. */
  private final String country;

  /** The entityID of the national SP that requested authentication. */
  private final String nationalSpEntityId;

  /** The type of SP that makes the request. */
  private final SPTypeEnumeration spType;

  /** The requested attributes. */
  private final List<RequestedAttribute> requestedAttributes;

  /** The requested authentication context class ref URI:s (Swedish). */
  private final List<String> requestedAuthnContextClassRefs;

  /** The name to add to the SAML attribute {@code ProviderName}. */
  private final String providerName;

  /** Preferred binding to use for authentication requests. */
  private final String preferredBinding;

  /**
   * Constructor.
   *
   * @param country the country code for the current IdP
   * @param nationalSpEntityId the entityID of the national SP that requested authentication
   * @param spType the SP type (if {@code null} it defaults to public)
   * @param requestedAttributes the requested SAML attributes
   * @param requestedAuthnContextClassRefs the requested authentication context class ref URI:s (Swedish)
   * @param providerName the name to add to the field {@code providerName}.
   * @param preferredBinding the preferred binding
   */
  public EidasAuthnRequestGeneratorContext(final String country, final String nationalSpEntityId,
      final SPTypeEnumeration spType, final List<RequestedAttribute> requestedAttributes,
      final List<String> requestedAuthnContextClassRefs, final String providerName,
      final String preferredBinding) {
    this.country = Objects.requireNonNull(country, "country must not be null");
    this.nationalSpEntityId = Objects.requireNonNull(nationalSpEntityId, "nationalSpEntityId must not be null");
    this.spType = Optional.ofNullable(spType).orElseGet(() -> SPTypeEnumeration.PUBLIC);
    this.requestedAttributes = Optional.ofNullable(requestedAttributes).orElseGet(() -> Collections.emptyList());
    this.requestedAuthnContextClassRefs = Objects.requireNonNull(requestedAuthnContextClassRefs,
        "requestedAuthnContextClassRefs must not be null");
    this.providerName =
        Optional.ofNullable(providerName).orElseGet(() -> EidasAuthenticationProperties.DEFAULT_PROVIDER_NAME);
    this.preferredBinding = Optional.ofNullable(preferredBinding).orElseGet(() -> SAMLConstants.SAML2_POST_BINDING_URI);
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
   * The preferred binding.
   */
  @Override
  public String getPreferredBinding() {
    return this.preferredBinding;
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
   * Gets the requested authentication context class ref URI:s (as stated by the Swedish SP).
   *
   * @return a list of URI:s
   */
  public List<String> getRequestedAuthnContextClassRefs() {
    return this.requestedAuthnContextClassRefs;
  }

  /**
   * Always set {@code isPassive} to {@code false}.
   */
  @Override
  public Boolean getIsPassiveAttribute() {
    return Boolean.FALSE;
  }

  /**
   * Calculates which URI:s to request based on the SP request and the IdP capabilities.
   */
  @Override
  public RequestedAuthnContextBuilderFunction getRequestedAuthnContextBuilderFunction() {

    return (supported, hok) -> {
      return AuthnContextClassRefMapper.calculateRequestedAuthnContext(
          supported, this.requestedAuthnContextClassRefs);
    };

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
