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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import se.swedenconnect.eidas.connector.authn.sp.EidasSpMetadataController;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * Main configuration properties.
 *
 * @author Martin Lindström
 */
@ConfigurationProperties("connector")
public class ConnectorConfigurationProperties implements InitializingBean {

  /** Logger instance. */
  private static final Logger log = LoggerFactory.getLogger(ConnectorConfigurationProperties.class);

  @Value("${server.servlet.context-path:/}")
  private @Nullable String contextPath;

  /**
   * The domain for the eIDAS Connector.
   */
  private @Nullable String domain;

  /**
   * The base URL of the Connector, including protocol, domain and context path.
   */
  private @Nullable String baseUrl;

  /**
   * Directory where caches and backup files are stored during execution.
   */
  private @Nullable File backupDirectory;

  /**
   * Tells whether we are running the connector in "development mode". This can mean that we allow any TLS server
   * certificates or that other settings are set up with less security.
   */
  private @Nullable Boolean developmentMode;

  /**
   * The country code for the eIDAS Connector. Defaults to "SE".
   */
  private @Nullable String country;

  /**
   * Configuration for the IdP part of the eIDAS Connector.
   */
  @NestedConfigurationProperty
  private final @NonNull ConnectorIdpProperties idp = new ConnectorIdpProperties();

  /**
   * Configuration for eIDAS authentication.
   */
  @NestedConfigurationProperty
  private final @NonNull EidasAuthenticationProperties eidas = new EidasAuthenticationProperties();

  /**
   * The configuration for retrieval of aggregated EU metadata.
   */
  private final @NonNull EuMetadataProperties euMetadata = new EuMetadataProperties();

  /**
   * The PRID service configuration.
   */
  private final @NonNull PridServiceProperties prid = new PridServiceProperties();

  /**
   * Identity Matching configuration.
   */
  @NestedConfigurationProperty
  private final @NonNull IdmProperties idm = new IdmProperties();

  /**
   * Gets the domain for the eIDAS Connector.
   *
   * @return the domain for the eIDAS Connector
   */
  public @Nullable String getDomain() {
    return this.domain;
  }

  /**
   * Assigns the domain for the eIDAS Connector.
   *
   * @param domain the domain for the eIDAS Connector
   */
  public void setDomain(final @Nullable String domain) {
    this.domain = domain;
  }

  /**
   * Gets the base URL of the Connector, including protocol, domain and context path.
   *
   * @return the base URL of the Connector, including protocol, domain and context path
   */
  public @Nullable String getBaseUrl() {
    return this.baseUrl;
  }

  /**
   * Assigns the base URL of the Connector, including protocol, domain and context path.
   *
   * @param baseUrl the base URL of the Connector, including protocol, domain and context path
   */
  public void setBaseUrl(final @Nullable String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * Gets the directory where caches and backup files are stored.
   *
   * @return the directory where caches and backup files are stored
   */
  public @Nullable File getBackupDirectory() {
    return this.backupDirectory;
  }

  /**
   * Assigns the directory where caches and backup files are stored.
   *
   * @param backupDirectory the directory where caches and backup files are stored
   */
  public void setBackupDirectory(final @Nullable File backupDirectory) {
    this.backupDirectory = backupDirectory;
  }

  /**
   * Gets whether the connector is running in "development mode".
   *
   * @return whether the connector is running in "development mode"
   */
  public @Nullable Boolean getDevelopmentMode() {
    return this.developmentMode;
  }

  /**
   * Assigns whether the connector is running in "development mode".
   *
   * @param developmentMode whether the connector is running in "development mode"
   */
  public void setDevelopmentMode(final @Nullable Boolean developmentMode) {
    this.developmentMode = developmentMode;
  }

  /**
   * Gets the country code for the eIDAS Connector.
   *
   * @return the country code for the eIDAS Connector
   */
  public @Nullable String getCountry() {
    return this.country;
  }

  /**
   * Assigns the country code for the eIDAS Connector.
   *
   * @param country the country code for the eIDAS Connector
   */
  public void setCountry(final @Nullable String country) {
    this.country = country;
  }

  /**
   * Gets the configuration for the IdP part of the eIDAS Connector.
   *
   * @return the configuration for the IdP part of the eIDAS Connector
   */
  public @NonNull ConnectorIdpProperties getIdp() {
    return this.idp;
  }

  /**
   * Gets the configuration for eIDAS authentication.
   *
   * @return the configuration for eIDAS authentication
   */
  public @NonNull EidasAuthenticationProperties getEidas() {
    return this.eidas;
  }

  /**
   * Gets the configuration for retrieval of aggregated EU metadata.
   *
   * @return the configuration for retrieval of aggregated EU metadata
   */
  public @NonNull EuMetadataProperties getEuMetadata() {
    return this.euMetadata;
  }

  /**
   * Gets the PRID service configuration.
   *
   * @return the PRID service configuration
   */
  public @NonNull PridServiceProperties getPrid() {
    return this.prid;
  }

  /**
   * Gets the Identity Matching configuration.
   *
   * @return the Identity Matching configuration
   */
  public @NonNull IdmProperties getIdm() {
    return this.idm;
  }

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
  public static class EuMetadataProperties implements InitializingBean {

    /**
     * The location of the metadata. Can be a URL, a file, or even a classpath resource.
     */
    private @Nullable Resource location;

    /**
     * If the {@code location} is an HTTPS resource, this setting may be used to specify a
     * <a href="https://spring.io/blog/2023/06/07/securing-spring-boot-applications-with-ssl">Spring SSL Bundle</a>
     * that gives the {@link javax.net.ssl.TrustManager}s to use during TLS verification. If no bundle is given, the
     * Java trust default will be used.
     */
    private @Nullable String httpsTrustBundle;

    /**
     * If the {@code location} setting is a URL, a "backup location" may be assigned to store downloaded metadata.
     */
    private @Nullable File backupLocation;

    /**
     * The certificate used to validate the metadata.
     */
    private @Nullable X509Certificate validationCertificate;

    /**
     * If the {@code location} is an HTTPS resource, this setting tells whether to skip hostname verification in the TLS
     * connection (useful during testing).
     */
    private @Nullable Boolean skipHostnameVerification;

    /**
     * If the {@code location} setting is a URL and an HTTP proxy is required this setting configures this proxy.
     */
    private @Nullable HttpProxy httpProxy;

    /**
     * Gets the location of the metadata.
     *
     * @return the location of the metadata
     */
    public @Nullable Resource getLocation() {
      return this.location;
    }

    /**
     * Assigns the location of the metadata.
     *
     * @param location the location of the metadata
     */
    public void setLocation(final @Nullable Resource location) {
      this.location = location;
    }

    /**
     * Gets the SSL bundle to use for TLS verification of an HTTPS location.
     *
     * @return the SSL bundle to use for TLS verification of an HTTPS location
     */
    public @Nullable String getHttpsTrustBundle() {
      return this.httpsTrustBundle;
    }

    /**
     * Assigns the SSL bundle to use for TLS verification of an HTTPS location.
     *
     * @param httpsTrustBundle the SSL bundle to use for TLS verification of an HTTPS location
     */
    public void setHttpsTrustBundle(final @Nullable String httpsTrustBundle) {
      this.httpsTrustBundle = httpsTrustBundle;
    }

    /**
     * Gets the backup location for downloaded metadata.
     *
     * @return the backup location for downloaded metadata
     */
    public @Nullable File getBackupLocation() {
      return this.backupLocation;
    }

    /**
     * Assigns the backup location for downloaded metadata.
     *
     * @param backupLocation the backup location for downloaded metadata
     */
    public void setBackupLocation(final @Nullable File backupLocation) {
      this.backupLocation = backupLocation;
    }

    /**
     * Gets the certificate used to validate the metadata.
     *
     * @return the certificate used to validate the metadata
     */
    public @Nullable X509Certificate getValidationCertificate() {
      return this.validationCertificate;
    }

    /**
     * Assigns the certificate used to validate the metadata.
     *
     * @param validationCertificate the certificate used to validate the metadata
     */
    public void setValidationCertificate(final @Nullable X509Certificate validationCertificate) {
      this.validationCertificate = validationCertificate;
    }

    /**
     * Gets whether to skip hostname verification in the TLS connection.
     *
     * @return whether to skip hostname verification in the TLS connection
     */
    public @Nullable Boolean getSkipHostnameVerification() {
      return this.skipHostnameVerification;
    }

    /**
     * Assigns whether to skip hostname verification in the TLS connection.
     *
     * @param skipHostnameVerification whether to skip hostname verification in the TLS connection
     */
    public void setSkipHostnameVerification(final @Nullable Boolean skipHostnameVerification) {
      this.skipHostnameVerification = skipHostnameVerification;
    }

    /**
     * Gets the HTTP proxy settings.
     *
     * @return the HTTP proxy settings
     */
    public @Nullable HttpProxy getHttpProxy() {
      return this.httpProxy;
    }

    /**
     * Assigns the HTTP proxy settings.
     *
     * @param httpProxy the HTTP proxy settings
     */
    public void setHttpProxy(final @Nullable HttpProxy httpProxy) {
      this.httpProxy = httpProxy;
    }

    /**
     * Configuration properties for an HTTP proxy.
     */
    public static class HttpProxy {

      /**
       * The proxy host.
       */
      private @Nullable String host;

      /**
       * The proxy port.
       */
      private @Nullable Integer port;

      /**
       * The proxy password (optional).
       */
      private @Nullable String password;

      /**
       * The proxy user name (optional).
       */
      private @Nullable String userName;

      /**
       * Gets the proxy host.
       *
       * @return the proxy host
       */
      public @Nullable String getHost() {
        return this.host;
      }

      /**
       * Assigns the proxy host.
       *
       * @param host the proxy host
       */
      public void setHost(final @Nullable String host) {
        this.host = host;
      }

      /**
       * Gets the proxy port.
       *
       * @return the proxy port
       */
      public @Nullable Integer getPort() {
        return this.port;
      }

      /**
       * Assigns the proxy port.
       *
       * @param port the proxy port
       */
      public void setPort(final @Nullable Integer port) {
        this.port = port;
      }

      /**
       * Gets the proxy password (optional).
       *
       * @return the proxy password (optional)
       */
      public @Nullable String getPassword() {
        return this.password;
      }

      /**
       * Assigns the proxy password (optional).
       *
       * @param password the proxy password (optional)
       */
      public void setPassword(final @Nullable String password) {
        this.password = password;
      }

      /**
       * Gets the proxy user name (optional).
       *
       * @return the proxy user name (optional)
       */
      public @Nullable String getUserName() {
        return this.userName;
      }

      /**
       * Assigns the proxy user name (optional).
       *
       * @param userName the proxy user name (optional)
       */
      public void setUserName(final @Nullable String userName) {
        this.userName = userName;
      }

      /** {@inheritDoc} */
      @Override
      public boolean equals(final @Nullable Object o) {
        if (this == o) {
          return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
          return false;
        }
        final HttpProxy other = (HttpProxy) o;
        return Objects.equals(this.host, other.host)
            && Objects.equals(this.port, other.port)
            && Objects.equals(this.password, other.password)
            && Objects.equals(this.userName, other.userName);
      }

      /** {@inheritDoc} */
      @Override
      public int hashCode() {
        return Objects.hash(this.host, this.port, this.password, this.userName);
      }

      /** {@inheritDoc} */
      @Override
      public @NonNull String toString() {
        return "host=" + this.host
            + ", port=" + this.port
            + ", userName=" + this.userName;
      }
    }

    /** {@inheritDoc} */
    @Override
    public void afterPropertiesSet() {
      Assert.notNull(this.location, "connector.eu-metadata.location must be set");
      if (this.validationCertificate == null) {
        log.warn("connector.eu-metadata.validation-certificate has not been set - Metadata can not be trusted");
      }
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final @Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || this.getClass() != o.getClass()) {
        return false;
      }
      final EuMetadataProperties other = (EuMetadataProperties) o;
      return Objects.equals(this.location, other.location)
          && Objects.equals(this.httpsTrustBundle, other.httpsTrustBundle)
          && Objects.equals(this.backupLocation, other.backupLocation)
          && Objects.equals(this.validationCertificate, other.validationCertificate)
          && Objects.equals(this.skipHostnameVerification, other.skipHostnameVerification)
          && Objects.equals(this.httpProxy, other.httpProxy);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return Objects.hash(this.location, this.httpsTrustBundle, this.backupLocation, this.validationCertificate,
          this.skipHostnameVerification, this.httpProxy);
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull String toString() {
      return "location=" + this.location
          + ", httpsTrustBundle=" + this.httpsTrustBundle
          + ", backupLocation=" + this.backupLocation
          + ", validationCertificate=" + this.validationCertificate
          + ", skipHostnameVerification=" + this.skipHostnameVerification
          + ", httpProxy=[" + this.httpProxy + "]";
    }

  }

  /**
   * Configuration properties for the PRID service.
   */
  public static class PridServiceProperties implements InitializingBean {

    public static final int DEFAULT_UPDATE_INTERVAL = 600;

    /**
     * A Resource pointing at the file containing the PRID configuration.
     */
    private @Nullable Resource policyResource;

    /**
     * Indicates how often the policy should be re-loaded (value is given in seconds).
     */
    private @Nullable Integer updateInterval;

    /**
     * Gets the resource pointing at the file containing the PRID configuration.
     *
     * @return the resource pointing at the file containing the PRID configuration
     */
    public @Nullable Resource getPolicyResource() {
      return this.policyResource;
    }

    /**
     * Assigns the resource pointing at the file containing the PRID configuration.
     *
     * @param policyResource the resource pointing at the file containing the PRID configuration
     */
    public void setPolicyResource(final @Nullable Resource policyResource) {
      this.policyResource = policyResource;
    }

    /**
     * Gets how often the policy should be re-loaded (in seconds).
     *
     * @return how often the policy should be re-loaded (in seconds)
     */
    public @Nullable Integer getUpdateInterval() {
      return this.updateInterval;
    }

    /**
     * Assigns how often the policy should be re-loaded (in seconds).
     *
     * @param updateInterval how often the policy should be re-loaded (in seconds)
     */
    public void setUpdateInterval(final @Nullable Integer updateInterval) {
      this.updateInterval = updateInterval;
    }

    /** {@inheritDoc} */
    @Override
    public void afterPropertiesSet() {
      Assert.notNull(this.policyResource, "connector.prid.policy-resource must be set");
      if (this.updateInterval == null) {
        this.updateInterval = DEFAULT_UPDATE_INTERVAL;
      }
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final @Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || this.getClass() != o.getClass()) {
        return false;
      }
      final PridServiceProperties other = (PridServiceProperties) o;
      return Objects.equals(this.policyResource, other.policyResource)
          && Objects.equals(this.updateInterval, other.updateInterval);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return Objects.hash(this.policyResource, this.updateInterval);
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull String toString() {
      return "policyResource=" + this.policyResource
          + ", updateInterval=" + this.updateInterval;
    }

  }

}
