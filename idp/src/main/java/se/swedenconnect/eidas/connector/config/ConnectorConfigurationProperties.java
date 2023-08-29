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

import java.io.File;
import java.security.cert.X509Certificate;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.spring.saml.idp.autoconfigure.settings.MetadataProviderConfigurationProperties;

/**
 * Main configuration properties.
 *
 * @author Martin Lindström
 */
@ConfigurationProperties("connector")
@Slf4j
public class ConnectorConfigurationProperties implements InitializingBean {

  /**
   * Directory where caches and backup files are stored during execution.
   */
  @Getter
  @Setter
  private File backupDirectory;

  /**
   * Configuration for the IdP part of the eIDAS Connector.
   */
  @NestedConfigurationProperty
  @Getter
  private ConnectorIdpProperties idp = new ConnectorIdpProperties();
  
  /**
   * Configuration for eIDAS authentication.
   */
  @NestedConfigurationProperty
  @Getter
  private EidasAuthenticationProperties eidas = new EidasAuthenticationProperties();

  /**
   * The configuration for retrieval of aggregated EU metadata.
   */
  @NestedConfigurationProperty
  @Getter
  private EuMetadataProperties euMetadata = new EuMetadataProperties();


  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(this.backupDirectory, "connector.backup-directory must be set");
    this.idp.afterPropertiesSet();
    this.eidas.afterPropertiesSet();
    this.euMetadata.afterPropertiesSet();
  }

  /**
   * Configuration properties for configuring EU metadata retrieval.
   */
  @Data
  public static class EuMetadataProperties implements InitializingBean {

    /**
     * The location of the metadata. Can be an URL, a file, or even a classpath resource.
     */
    private Resource location;

    /**
     * If the {@code location} setting is an URL, a "backup location" may be assigned to store downloaded metadata.
     */
    private File backupLocation;

    /**
     * The certificate used to validate the metadata.
     */
    private X509Certificate validationCertificate;

    /**
     * If the {@code location} setting is an URL and a HTTP proxy is required this setting configures this proxy.
     */
    private MetadataProviderConfigurationProperties.HttpProxy httpProxy;

    /** {@inheritDoc} */
    @Override
    public void afterPropertiesSet() throws Exception {
      Assert.notNull(this.location, "connector.eu-metadata.location must be set");
      if (this.validationCertificate == null) {
        log.warn("connector.eu-metadata.validation-certificate has not been set - Metadata can not be trusted");
      }
    }

  }

}