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
package se.swedenconnect.eidas.connector.config;

import java.util.List;

import org.opensaml.saml.saml2.core.NameID;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import lombok.Getter;
import lombok.Setter;
import se.swedenconnect.spring.saml.idp.autoconfigure.settings.CredentialConfigurationProperties;

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
   * The credentials for the SP part of the eIDAS Connector. If not assigned, the keys configured for the SAML IdP will
   * be used also for the SP.
   */
  @Getter
  @Setter
  private CredentialConfigurationProperties credentials;

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
   * An ordered list of supported NameID formats.
   */
  @Getter
  @Setter
  private List<String> supportedNameIds;

  /**
   * Metadata configuration for the eIDAS SP.
   */
  @Getter
  @Setter
  private EidasSpMetadataProperties metadata;

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.hasText(this.entityId, "connector.eidas.entity-id must be set");
    if (!StringUtils.hasText(providerName)) {
      this.providerName = DEFAULT_PROVIDER_NAME;
    }
    if (this.requiresSignedAssertions == null) {
      this.requiresSignedAssertions = Boolean.FALSE;
    }
    if (this.supportedNameIds == null || this.supportedNameIds.isEmpty()) {
      this.supportedNameIds = List.of(NameID.PERSISTENT, NameID.TRANSIENT, NameID.UNSPECIFIED);
    }

    Assert.notNull(this.metadata, "connector.eidas.metadata.* must be set");
    this.metadata.afterPropertiesSet();
  }

}
