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
package se.swedenconnect.eidas.connector.config;

import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.NameID;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import se.swedenconnect.security.credential.config.properties.PkiCredentialConfigurationProperties;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for our SP (authentication provider).
 *
 * @author Martin Lindström
 */
public class EidasAuthenticationProperties implements InitializingBean {

  /**
   * The default name to use for the SAML attribute {@code ProviderName}.
   */
  public static final String DEFAULT_PROVIDER_NAME = "Swedish eIDAS Connector";

  /**
   * The entityID for the eIDAS SP.
   */
  private String entityId;

  /**
   * The "provider name" to use in our AuthnRequest:s sent.
   */
  private String providerName;

  /**
   * Whether we require signed eIDAS assertions.
   */
  private Boolean requiresSignedAssertions;

  /**
   * The preferred binding to use when sending authentication requests. Default is
   * {@code urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST}. For redirect, use
   * {@code urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect}.
   */
  private String preferredBinding;

  /**
   * An ordered list of supported NameID formats.
   */
  private final List<String> supportedNameIds = new ArrayList<>();

  /**
   * Some eIDAS countries can not handle the {@code Scoping} element in {@code AuthnRequest} messages. This setting
   * contains the country codes for those countries that we should not include this element for.
   */
  private final List<String> skipScopingFor = new ArrayList<>();

  /**
   * The credentials for the SP part of the eIDAS Connector. If not assigned, the keys configured for the SAML IdP will
   * be used also for the SP.
   */
  private final SpCredentialProperties credentials = new SpCredentialProperties();

  /**
   * Metadata configuration for the eIDAS SP.
   */
  @NestedConfigurationProperty
  private EidasSpMetadataProperties metadata;

  /**
   * Gets the entityID for the eIDAS SP.
   *
   * @return the entityID for the eIDAS SP
   */
  public String getEntityId() {
    return this.entityId;
  }

  /**
   * Assigns the entityID for the eIDAS SP.
   *
   * @param entityId the entityID for the eIDAS SP
   */
  public void setEntityId(final String entityId) {
    this.entityId = entityId;
  }

  /**
   * Gets the "provider name" to use in our AuthnRequest:s sent.
   *
   * @return the "provider name" to use in our AuthnRequest:s sent
   */
  public String getProviderName() {
    return this.providerName;
  }

  /**
   * Assigns the "provider name" to use in our AuthnRequest:s sent.
   *
   * @param providerName the "provider name" to use in our AuthnRequest:s sent
   */
  public void setProviderName(final String providerName) {
    this.providerName = providerName;
  }

  /**
   * Gets whether we require signed eIDAS assertions.
   *
   * @return whether we require signed eIDAS assertions
   */
  public Boolean getRequiresSignedAssertions() {
    return this.requiresSignedAssertions;
  }

  /**
   * Assigns whether we require signed eIDAS assertions.
   *
   * @param requiresSignedAssertions whether we require signed eIDAS assertions
   */
  public void setRequiresSignedAssertions(final Boolean requiresSignedAssertions) {
    this.requiresSignedAssertions = requiresSignedAssertions;
  }

  /**
   * Gets the preferred binding to use when sending authentication requests.
   *
   * @return the preferred binding to use when sending authentication requests
   */
  public String getPreferredBinding() {
    return this.preferredBinding;
  }

  /**
   * Assigns the preferred binding to use when sending authentication requests.
   *
   * @param preferredBinding the preferred binding to use when sending authentication requests
   */
  public void setPreferredBinding(final String preferredBinding) {
    this.preferredBinding = preferredBinding;
  }

  /**
   * Gets an ordered list of supported NameID formats.
   *
   * @return an ordered list of supported NameID formats
   */
  public List<String> getSupportedNameIds() {
    return this.supportedNameIds;
  }

  /**
   * Gets the country codes for which the {@code Scoping} element should not be included.
   *
   * @return the country codes for which the {@code Scoping} element should not be included
   */
  public List<String> getSkipScopingFor() {
    return this.skipScopingFor;
  }

  /**
   * Gets the credentials for the SP part of the eIDAS Connector.
   *
   * @return the credentials for the SP part of the eIDAS Connector
   */
  public SpCredentialProperties getCredentials() {
    return this.credentials;
  }

  /**
   * Gets the metadata configuration for the eIDAS SP.
   *
   * @return the metadata configuration for the eIDAS SP
   */
  public EidasSpMetadataProperties getMetadata() {
    return this.metadata;
  }

  /**
   * Assigns the metadata configuration for the eIDAS SP.
   *
   * @param metadata the metadata configuration for the eIDAS SP
   */
  public void setMetadata(final EidasSpMetadataProperties metadata) {
    this.metadata = metadata;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() {
    Assert.hasText(this.entityId, "connector.eidas.entity-id must be set");
    if (!StringUtils.hasText(this.providerName)) {
      this.providerName = DEFAULT_PROVIDER_NAME;
    }
    if (this.requiresSignedAssertions == null) {
      this.requiresSignedAssertions = Boolean.FALSE;
    }
    if (this.preferredBinding == null) {
      this.preferredBinding = SAMLConstants.SAML2_POST_BINDING_URI;
    }
    else {
      if (!(SAMLConstants.SAML2_POST_BINDING_URI.equals(this.preferredBinding)
          || SAMLConstants.SAML2_REDIRECT_BINDING_URI.equals(this.preferredBinding))) {
        throw new IllegalArgumentException("Invalid value for connector.eidas.preferred-binding");
      }
    }

    if (this.supportedNameIds.isEmpty()) {
      this.supportedNameIds.addAll(List.of(NameID.PERSISTENT, NameID.TRANSIENT, NameID.UNSPECIFIED));
    }
    Assert.notNull(this.metadata, "connector.eidas.metadata.* must be set");
    this.metadata.afterPropertiesSet();
  }

  /**
   * Connector SP credentials.
   */
  public static class SpCredentialProperties {

    /**
     * The SP default credential.
     */
    private PkiCredentialConfigurationProperties defaultCredential;

    /**
     * The SP signing credential.
     */
    private PkiCredentialConfigurationProperties sign;

    /**
     * A certificate that will be the future SP signing certificate. Is set before a key-rollover is performed.
     */
    private X509Certificate futureSign;

    /**
     * The SP encryption credential.
     */
    private PkiCredentialConfigurationProperties encrypt;

    /**
     * The previous SP encryption credential. Assigned after a key-rollover.
     */
    private PkiCredentialConfigurationProperties previousEncrypt;

    /**
     * The SP SAML metadata signing credential.
     */
    private PkiCredentialConfigurationProperties metadataSign;

    /**
     * Gets the SP default credential.
     *
     * @return the SP default credential
     */
    public PkiCredentialConfigurationProperties getDefaultCredential() {
      return this.defaultCredential;
    }

    /**
     * Assigns the SP default credential.
     *
     * @param defaultCredential the SP default credential
     */
    public void setDefaultCredential(final PkiCredentialConfigurationProperties defaultCredential) {
      this.defaultCredential = defaultCredential;
    }

    /**
     * Gets the SP signing credential.
     *
     * @return the SP signing credential
     */
    public PkiCredentialConfigurationProperties getSign() {
      return this.sign;
    }

    /**
     * Assigns the SP signing credential.
     *
     * @param sign the SP signing credential
     */
    public void setSign(final PkiCredentialConfigurationProperties sign) {
      this.sign = sign;
    }

    /**
     * Gets a certificate that will be the future SP signing certificate.
     *
     * @return a certificate that will be the future SP signing certificate
     */
    public X509Certificate getFutureSign() {
      return this.futureSign;
    }

    /**
     * Assigns a certificate that will be the future SP signing certificate.
     *
     * @param futureSign a certificate that will be the future SP signing certificate
     */
    public void setFutureSign(final X509Certificate futureSign) {
      this.futureSign = futureSign;
    }

    /**
     * Gets the SP encryption credential.
     *
     * @return the SP encryption credential
     */
    public PkiCredentialConfigurationProperties getEncrypt() {
      return this.encrypt;
    }

    /**
     * Assigns the SP encryption credential.
     *
     * @param encrypt the SP encryption credential
     */
    public void setEncrypt(final PkiCredentialConfigurationProperties encrypt) {
      this.encrypt = encrypt;
    }

    /**
     * Gets the previous SP encryption credential.
     *
     * @return the previous SP encryption credential
     */
    public PkiCredentialConfigurationProperties getPreviousEncrypt() {
      return this.previousEncrypt;
    }

    /**
     * Assigns the previous SP encryption credential.
     *
     * @param previousEncrypt the previous SP encryption credential
     */
    public void setPreviousEncrypt(final PkiCredentialConfigurationProperties previousEncrypt) {
      this.previousEncrypt = previousEncrypt;
    }

    /**
     * Gets the SP SAML metadata signing credential.
     *
     * @return the SP SAML metadata signing credential
     */
    public PkiCredentialConfigurationProperties getMetadataSign() {
      return this.metadataSign;
    }

    /**
     * Assigns the SP SAML metadata signing credential.
     *
     * @param metadataSign the SP SAML metadata signing credential
     */
    public void setMetadataSign(final PkiCredentialConfigurationProperties metadataSign) {
      this.metadataSign = metadataSign;
    }
  }

}
