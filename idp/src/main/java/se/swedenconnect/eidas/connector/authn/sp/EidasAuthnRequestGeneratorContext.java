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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.opensaml.core.xml.Namespace;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.StatusResponseType;
import org.opensaml.saml.saml2.metadata.NameIDFormat;
import org.opensaml.xmlsec.SignatureSigningConfiguration;
import se.swedenconnect.eidas.connector.config.EidasAuthenticationProperties;
import se.swedenconnect.opensaml.eidas.common.EidasConstants;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttribute;
import se.swedenconnect.opensaml.eidas.ext.SPTypeEnumeration;
import se.swedenconnect.opensaml.saml2.core.build.NameIDPolicyBuilder;
import se.swedenconnect.opensaml.saml2.request.AuthnRequestGeneratorContext;
import se.swedenconnect.opensaml.xmlsec.config.SecurityConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link AuthnRequestGeneratorContext} for eIDAS.
 *
 * @author Martin Lindström
 */
class EidasAuthnRequestGeneratorContext implements AuthnRequestGeneratorContext {

  /** The country code. */
  private final @NonNull String country;

  /** The entityID of the national SP that requested authentication. */
  private final @NonNull String nationalSpEntityId;

  /** The type of SP that makes the request. */
  private final @NonNull SPTypeEnumeration spType;

  /** The requested attributes. */
  private final @NonNull List<RequestedAttribute> requestedAttributes;

  /** The requested authentication context class ref URI:s (Swedish). */
  private final @NonNull List<String> requestedAuthnContextClassRefs;

  /** The name to add to the SAML attribute {@code ProviderName}. */
  private final @NonNull String providerName;

  /** Preferred binding to use for authentication requests. */
  private final @NonNull String preferredBinding;

  /** The security configuration we use for signing. */
  private final @NonNull SecurityConfiguration securityConfiguration;

  /** An ordered list of the NameID formats that we support. */
  private final @NonNull List<String> supportedNameIds;

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
   * @param securityConfiguration the security configuration
   * @param supportedNameIds an ordered list of the NameID formats that we support
   */
  public EidasAuthnRequestGeneratorContext(final @NonNull String country, final @NonNull String nationalSpEntityId,
      final @Nullable SPTypeEnumeration spType, final @Nullable List<RequestedAttribute> requestedAttributes,
      final @NonNull List<String> requestedAuthnContextClassRefs, final @Nullable String providerName,
      final @Nullable String preferredBinding, final @NonNull SecurityConfiguration securityConfiguration,
      final @Nullable List<String> supportedNameIds) {
    this.country = Objects.requireNonNull(country, "country must not be null");
    this.nationalSpEntityId = Objects.requireNonNull(nationalSpEntityId, "nationalSpEntityId must not be null");
    this.spType = Optional.ofNullable(spType).orElse(SPTypeEnumeration.PUBLIC);
    this.requestedAttributes = Optional.ofNullable(requestedAttributes).orElseGet(Collections::emptyList);
    this.requestedAuthnContextClassRefs = Objects.requireNonNull(requestedAuthnContextClassRefs,
        "requestedAuthnContextClassRefs must not be null");
    this.providerName =
        Optional.ofNullable(providerName).orElse(EidasAuthenticationProperties.DEFAULT_PROVIDER_NAME);
    this.preferredBinding = Optional.ofNullable(preferredBinding).orElse(SAMLConstants.SAML2_POST_BINDING_URI);
    this.securityConfiguration =
        Objects.requireNonNull(securityConfiguration, "securityConfiguration must not be null");
    this.supportedNameIds = Optional.ofNullable(supportedNameIds).orElseGet(Collections::emptyList);
  }

  /**
   * Gets the country code for the current IdP.
   *
   * @return the country code
   */
  public @NonNull String getCountryCode() {
    return this.country;
  }

  /**
   * The preferred binding.
   */
  @Override
  public @NonNull String getPreferredBinding() {
    return this.preferredBinding;
  }

  /**
   * Gets the entityID of the national SP that requested authentication.
   *
   * @return SAML entityID
   */
  public @NonNull String getNationalSpEntityId() {
    return this.nationalSpEntityId;
  }

  /**
   * Gets the SP type.
   *
   * @return the SP type
   */
  public @NonNull SPTypeEnumeration getSpType() {
    return this.spType;
  }

  /**
   * Gets the requested SAML attributes.
   *
   * @return the requested SAML attributes
   */
  public @NonNull List<RequestedAttribute> getRequestedAttributes() {
    return this.requestedAttributes;
  }

  /**
   * Gets the requested authentication context class ref URI:s (as stated by the Swedish SP).
   *
   * @return a list of URI:s
   */
  public @NonNull List<String> getRequestedAuthnContextClassRefs() {
    return this.requestedAuthnContextClassRefs;
  }

  /**
   * Always set {@code isPassive} to {@code false}.
   */
  @Override
  public @NonNull Boolean getIsPassiveAttribute() {
    return Boolean.FALSE;
  }

  /**
   * Calculates which URI:s to request based on the SP request and the IdP capabilities.
   */
  @Override
  public @NonNull RequestedAuthnContextBuilderFunction getRequestedAuthnContextBuilderFunction() {

    return (supported, hok) -> AuthnContextClassRefMapper.calculateRequestedAuthnContext(
        supported, this.requestedAuthnContextClassRefs);

  }

  /**
   * Delivers a configuration suitable for eIDAS.
   */
  @Override
  public @NonNull SignatureSigningConfiguration getSignatureSigningConfiguration() {
    return this.securityConfiguration.getSignatureSigningConfiguration();
  }

  /**
   * Uses the first of our supported NameID formats that is listed in the IdP metadata. If the IdP metadata does not
   * list any NameID formats, or none of them are supported, persistent is used.
   */
  @Override
  public @NonNull NameIDPolicyBuilderFunction getNameIDPolicyBuilderFunction() {
    return idpFormats -> {
      final List<String> formats = Optional.ofNullable(idpFormats).orElseGet(Collections::emptyList).stream()
          .map(NameIDFormat::getURI)
          .filter(Objects::nonNull)
          .map(String::trim)
          .toList();
      final String format = this.supportedNameIds.stream()
          .filter(formats::contains)
          .findFirst()
          .orElse(NameID.PERSISTENT);
      return NameIDPolicyBuilder.builder()
          .allowCreate(true)
          .format(format)
          .build();
    };
  }

  /**
   * Adds the eIDAS namespace, consent and provider name, and removes the protocol binding.
   */
  @Override
  public @NonNull AuthnRequestCustomizer getAuthnRequestCustomizer() {
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
