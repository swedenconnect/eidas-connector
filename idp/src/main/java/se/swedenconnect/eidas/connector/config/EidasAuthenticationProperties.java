/*
 * Copyright 2017-2025 Sweden Connect
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

import lombok.Getter;
import lombok.Setter;
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
  @Getter
  @Setter
  private String entityId;

  /**
   * The "provider name" to use in our AuthnRequest:s sent.
   */
  @Getter
  @Setter
  private String providerName;

  /**
   * Whether we require signed eIDAS assertions.
   */
  @Getter
  @Setter
  private Boolean requiresSignedAssertions;

  /**
   * The preferred binding to use when sending authentication requests. Default is
   * {@code urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST}. For redirect, use
   * {@code urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect}.
   */
  @Getter
  @Setter
  private String preferredBinding;

  /**
   * An ordered list of supported NameID formats.
   */
  @Getter
  private final List<String> supportedNameIds = new ArrayList<>();

  /**
   * Some eIDAS countries can not handle the {@code Scoping} element in {@code AuthnRequest} messages. This setting
   * contains the country codes for those countries that we should not include this element for.
   */
  @Getter
  private final List<String> skipScopingFor = new ArrayList<>();

  /**
   * The credentials for the SP part of the eIDAS Connector. If not assigned, the keys configured for the SAML IdP will
   * be used also for the SP.
   */
  @Getter
  private final SpCredentialProperties credentials = new SpCredentialProperties();

  /**
   * Metadata configuration for the eIDAS SP.
   */
  @Getter
  @Setter
  @NestedConfigurationProperty
  private EidasSpMetadataProperties metadata;

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
    @Setter
    @Getter
    private PkiCredentialConfigurationProperties defaultCredential;

    /**
     * The SP signing credential.
     */
    @Setter
    @Getter
    private PkiCredentialConfigurationProperties sign;

    /**
     * A certificate that will be the future SP signing certificate. Is set before a key-rollover is performed.
     */
    @Setter
    @Getter
    private X509Certificate futureSign;

    /**
     * The SP encryption credential.
     */
    @Setter
    @Getter
    private PkiCredentialConfigurationProperties encrypt;

    /**
     * The previous SP encryption credential. Assigned after a key-rollover.
     */
    @Setter
    @Getter
    private PkiCredentialConfigurationProperties previousEncrypt;

    /**
     * The SP SAML metadata signing credential.
     */
    @Setter
    @Getter
    private PkiCredentialConfigurationProperties metadataSign;
  }

}
