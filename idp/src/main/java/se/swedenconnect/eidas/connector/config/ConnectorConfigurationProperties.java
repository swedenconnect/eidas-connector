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

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import se.swedenconnect.eidas.connector.authn.sp.EidasSpMetadataController;
import se.swedenconnect.spring.saml.idp.autoconfigure.settings.IdentityProviderConfigurationProperties;

import java.io.File;
import java.security.cert.X509Certificate;

/**
 * Main configuration properties.
 *
 * @author Martin Lindström
 */
@ConfigurationProperties("connector")
@Slf4j
public class ConnectorConfigurationProperties implements InitializingBean {

  @Value("${server.servlet.context-path:/}")
  private String contextPath;

  /**
   * The domain for the eIDAS Connector.
   */
  @Getter
  @Setter
  private String domain;

  /**
   * The base URL of the Connector, including protocol, domain and context path.
   */
  @Getter
  @Setter
  private String baseUrl;

  /**
   * Directory where caches and backup files are stored during execution.
   */
  @Getter
  @Setter
  private File backupDirectory;

  /**
   * Tells whether we are running the connector in "development mode". This can mean that we allow any TLS server
   * certificates or that other settings are set up with less security.
   */
  @Getter
  @Setter
  private Boolean developmentMode;

  /**
   * The country code for the eIDAS Connector. Defaults to "SE".
   */
  @Getter
  @Setter
  private String country;

  /**
   * Configuration for the IdP part of the eIDAS Connector.
   */
  @NestedConfigurationProperty
  @Getter
  private final ConnectorIdpProperties idp = new ConnectorIdpProperties();

  /**
   * Configuration for eIDAS authentication.
   */
  @NestedConfigurationProperty
  @Getter
  private final EidasAuthenticationProperties eidas = new EidasAuthenticationProperties();

  /**
   * The configuration for retrieval of aggregated EU metadata.
   */
  @Getter
  private final EuMetadataProperties euMetadata = new EuMetadataProperties();

  /**
   * The PRID service configuration.
   */
  @Getter
  private final PridServiceProperties prid = new PridServiceProperties();

  /**
   * Identity Matching configuration.
   */
  @NestedConfigurationProperty
  @Getter
  private final IdmProperties idm = new IdmProperties();

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(this.domain, "connector.domain must be set");
    if (!StringUtils.hasText(this.baseUrl)) {
      this.baseUrl = String.format("https://%s%s", this.domain,
          "/".equals(this.contextPath) ? "" : this.contextPath);
      log.info("Defaulting connector.base-url to {}", this.baseUrl);
    }
    Assert.notNull(this.backupDirectory, "connector.backup-directory must be set");

    if (this.developmentMode != null) {
      DevelopmentMode.init(this.developmentMode);
    }

    if (!StringUtils.hasText(this.country)) {
      this.country = "SE";
    }
    this.idp.afterPropertiesSet();

    if (!StringUtils.hasText(this.eidas.getEntityId())) {
      this.eidas.setEntityId(this.baseUrl + EidasSpMetadataController.METADATA_PATH);
    }
    this.eidas.afterPropertiesSet();
    this.euMetadata.afterPropertiesSet();
    this.prid.afterPropertiesSet();
    this.idm.afterPropertiesSet();
  }

  /**
   * Configuration properties for configuring EU metadata retrieval.
   */
  @Data
  public static class EuMetadataProperties implements InitializingBean {

    /**
     * The location of the metadata. Can be a URL, a file, or even a classpath resource.
     */
    private Resource location;

    /**
     * If the {@code location} is an HTTPS resource, this setting may be used to specify a
     * <a href="https://spring.io/blog/2023/06/07/securing-spring-boot-applications-with-ssl">Spring SSL Bundle</a>
     * that gives the {@link javax.net.ssl.TrustManager}s to use during TLS verification. If no bundle is given, the
     * Java trust default will be used.
     */
    @Setter
    @Getter
    private String httpsTrustBundle;

    /**
     * If the {@code location} setting is a URL, a "backup location" may be assigned to store downloaded metadata.
     */
    private File backupLocation;

    /**
     * The certificate used to validate the metadata.
     */
    private X509Certificate validationCertificate;

    /**
     * If the {@code location} is an HTTPS resource, this setting tells whether to skip hostname verification in the TLS
     * connection (useful during testing).
     */
    private Boolean skipHostnameVerification;

    /**
     * If the {@code location} setting is a URL and an HTTP proxy is required this setting configures this proxy.
     */
    private IdentityProviderConfigurationProperties.MetadataProviderConfigurationProperties.HttpProxy httpProxy;

    /** {@inheritDoc} */
    @Override
    public void afterPropertiesSet() {
      Assert.notNull(this.location, "connector.eu-metadata.location must be set");
      if (this.validationCertificate == null) {
        log.warn("connector.eu-metadata.validation-certificate has not been set - Metadata can not be trusted");
      }
    }

  }

  /**
   * Configuration properties for the PRID service.
   */
  @Data
  public static class PridServiceProperties implements InitializingBean {

    public static final int DEFAULT_UPDATE_INTERVAL = 600;

    /**
     * A Resource pointing at the file containing the PRID configuration.
     */
    private Resource policyResource;

    /**
     * Indicates how often the policy should be re-loaded (value is given in seconds).
     */
    private Integer updateInterval;

    /** {@inheritDoc} */
    @Override
    public void afterPropertiesSet() {
      Assert.notNull(this.policyResource, "connector.prid.policy-resource must be set");
      if (this.updateInterval == null) {
        this.updateInterval = DEFAULT_UPDATE_INTERVAL;
      }
    }

  }

}
