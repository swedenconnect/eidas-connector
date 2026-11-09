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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.Assert;
import se.swedenconnect.security.credential.config.properties.PkiCredentialConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for eIDAS Identity Matching.
 *
 * @author Martin Lindström
 */
public class IdmProperties implements InitializingBean {

  /**
   * Whether the IdM feature is active or not.
   */
  private Boolean active;

  /**
   * The URL to the Identity Matching service (for inclusion in views).
   */
  private String serviceUrl;

  /**
   * The base URL for the eIDAS Identity Matching API.
   */
  private String apiBaseUrl;

  /**
   * A reference to a Spring Boot SSL Bundle holding the trust configuration for TLS-calls against the IdM server. If no
   * bundle is set, the system defaults are used.
   */
  private String trustBundle;

  /**
   * Connector OAuth2 client settings.
   */
  private OAuth2Properties oauth2;

  /**
   * Gets whether the IdM feature is active or not.
   *
   * @return whether the IdM feature is active or not
   */
  public Boolean getActive() {
    return this.active;
  }

  /**
   * Assigns whether the IdM feature is active or not.
   *
   * @param active whether the IdM feature is active or not
   */
  public void setActive(final Boolean active) {
    this.active = active;
  }

  /**
   * Gets the URL to the Identity Matching service (for inclusion in views).
   *
   * @return the URL to the Identity Matching service (for inclusion in views)
   */
  public String getServiceUrl() {
    return this.serviceUrl;
  }

  /**
   * Assigns the URL to the Identity Matching service (for inclusion in views).
   *
   * @param serviceUrl the URL to the Identity Matching service (for inclusion in views)
   */
  public void setServiceUrl(final String serviceUrl) {
    this.serviceUrl = serviceUrl;
  }

  /**
   * Gets the base URL for the eIDAS Identity Matching API.
   *
   * @return the base URL for the eIDAS Identity Matching API
   */
  public String getApiBaseUrl() {
    return this.apiBaseUrl;
  }

  /**
   * Assigns the base URL for the eIDAS Identity Matching API.
   *
   * @param apiBaseUrl the base URL for the eIDAS Identity Matching API
   */
  public void setApiBaseUrl(final String apiBaseUrl) {
    this.apiBaseUrl = apiBaseUrl;
  }

  /**
   * Gets the SSL bundle holding the trust configuration for TLS calls against the IdM server.
   *
   * @return the SSL bundle holding the trust configuration for TLS calls against the IdM server
   */
  public String getTrustBundle() {
    return this.trustBundle;
  }

  /**
   * Assigns the SSL bundle holding the trust configuration for TLS calls against the IdM server.
   *
   * @param trustBundle the SSL bundle holding the trust configuration for TLS calls against the IdM server
   */
  public void setTrustBundle(final String trustBundle) {
    this.trustBundle = trustBundle;
  }

  /**
   * Gets the Connector OAuth2 client settings.
   *
   * @return the Connector OAuth2 client settings
   */
  public OAuth2Properties getOauth2() {
    return this.oauth2;
  }

  /**
   * Assigns the Connector OAuth2 client settings.
   *
   * @param oauth2 the Connector OAuth2 client settings
   */
  public void setOauth2(final OAuth2Properties oauth2) {
    this.oauth2 = oauth2;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() {
    if (this.active == null) {
      this.active = Boolean.FALSE;
    }
    if (this.active) {
      Assert.hasText(this.serviceUrl, "connector.idm.serviceUrl must be assigned");
      if (this.apiBaseUrl == null) {
        this.apiBaseUrl = this.serviceUrl;
        if (this.apiBaseUrl.endsWith("/")) {
          this.apiBaseUrl = this.apiBaseUrl.substring(0, this.apiBaseUrl.length() - 1);
        }
      }
      if (this.apiBaseUrl.endsWith("/")) {
        throw new IllegalArgumentException("connector.idm.api-base-url must not end with a /");
      }
      Assert.notNull(this.oauth2, "connector.idm.oauth2.* must be set");
      this.oauth2.afterPropertiesSet();
    }
  }

  /**
   * Properties for the Connector OAuth2 handler.
   */
  public static class OAuth2Properties implements InitializingBean {

    /**
     * The Connector OAuth2 client ID.
     */
    private String clientId;

    /**
     * The scope(s) to request for making check calls to the IdM Query API.
     */
    private List<String> checkScopes;

    /**
     * The scope(s) to request for making get calls to the IdM Query API.
     */
    private List<String> getScopes;

    /**
     * The ID for the resource we are sending our access tokens to (the IdM-service).
     */
    private String resourceId;

    /**
     * The credential to use for authentication against the Authorization Server (if the connector acts as an OAuth2
     * client) OR for use of signing of access tokens (if the connector also acts as an OAuth2 Authorization Server). If
     * not assigned, the connector default credential will be used.
     */
    @NestedConfigurationProperty
    private PkiCredentialConfigurationProperties credential;

    /**
     * Settings if the eIDAS connector should act as an OAuth2 client. Mutually exclusive with 'server'.
     */
    private OAuth2ClientProperties client;

    /**
     * Settings if the eIDAS connector should act as an OAuth2 Authorization Server and issue access token for itself.
     * Mutually exclusive with 'client'.
     */
    private OAuth2ServerProperties server;

    /**
     * Gets the Connector OAuth2 client ID.
     *
     * @return the Connector OAuth2 client ID
     */
    public String getClientId() {
      return this.clientId;
    }

    /**
     * Assigns the Connector OAuth2 client ID.
     *
     * @param clientId the Connector OAuth2 client ID
     */
    public void setClientId(final String clientId) {
      this.clientId = clientId;
    }

    /**
     * Gets the scope(s) to request for making check calls to the IdM Query API.
     *
     * @return the scope(s) to request for making check calls to the IdM Query API
     */
    public List<String> getCheckScopes() {
      return this.checkScopes;
    }

    /**
     * Assigns the scope(s) to request for making check calls to the IdM Query API.
     *
     * @param checkScopes the scope(s) to request for making check calls to the IdM Query API
     */
    public void setCheckScopes(final List<String> checkScopes) {
      this.checkScopes = checkScopes;
    }

    /**
     * Gets the scope(s) to request for making get calls to the IdM Query API.
     *
     * @return the scope(s) to request for making get calls to the IdM Query API
     */
    public List<String> getGetScopes() {
      return this.getScopes;
    }

    /**
     * Assigns the scope(s) to request for making get calls to the IdM Query API.
     *
     * @param getScopes the scope(s) to request for making get calls to the IdM Query API
     */
    public void setGetScopes(final List<String> getScopes) {
      this.getScopes = getScopes;
    }

    /**
     * Gets the ID for the resource we are sending our access tokens to (the IdM-service).
     *
     * @return the ID for the resource we are sending our access tokens to (the IdM-service)
     */
    public String getResourceId() {
      return this.resourceId;
    }

    /**
     * Assigns the ID for the resource we are sending our access tokens to (the IdM-service).
     *
     * @param resourceId the ID for the resource we are sending our access tokens to (the IdM-service)
     */
    public void setResourceId(final String resourceId) {
      this.resourceId = resourceId;
    }

    /**
     * Gets the credential for OAuth2 authentication or access token signing.
     *
     * @return the credential for OAuth2 authentication or access token signing
     */
    public PkiCredentialConfigurationProperties getCredential() {
      return this.credential;
    }

    /**
     * Assigns the credential for OAuth2 authentication or access token signing.
     *
     * @param credential the credential for OAuth2 authentication or access token signing
     */
    public void setCredential(final PkiCredentialConfigurationProperties credential) {
      this.credential = credential;
    }

    /**
     * Gets the OAuth2 client settings.
     *
     * @return the OAuth2 client settings
     */
    public OAuth2ClientProperties getClient() {
      return this.client;
    }

    /**
     * Assigns the OAuth2 client settings.
     *
     * @param client the OAuth2 client settings
     */
    public void setClient(final OAuth2ClientProperties client) {
      this.client = client;
    }

    /**
     * Gets the OAuth2 Authorization Server settings.
     *
     * @return the OAuth2 Authorization Server settings
     */
    public OAuth2ServerProperties getServer() {
      return this.server;
    }

    /**
     * Assigns the OAuth2 Authorization Server settings.
     *
     * @param server the OAuth2 Authorization Server settings
     */
    public void setServer(final OAuth2ServerProperties server) {
      this.server = server;
    }

    /** {@inheritDoc} */
    @Override
    public void afterPropertiesSet() {
      Assert.hasText(this.clientId, "connector.idm.oauth2.client-id must be assigned");
      Assert.notEmpty(this.checkScopes, "connector.idm.oauth2.check-scopes must contain at least one scope");
      Assert.notEmpty(this.getScopes, "connector.idm.oauth2.get-scopes must contain at least one scope");
      Assert.hasText(this.resourceId, "connector.idm.oauth2.resource-id must be assigned");

      if (this.client == null && this.server == null) {
        throw new IllegalArgumentException(
            "One of connector.idm.oauth2.client and connector.idm.oauth2.server must be assigned");
      }
      if (this.client != null && this.server != null) {
        throw new IllegalArgumentException(
            "connector.idm.oauth2.client and connector.idm.oauth2.server can not both be assigned");
      }
      if (this.client != null) {
        this.client.afterPropertiesSet();
      }
      if (this.server != null) {
        this.server.afterPropertiesSet();
      }
    }

    /**
     * Settings if the eIDAS connector should act as an OAuth2 client.
     */
    public static class OAuth2ClientProperties implements InitializingBean {

      /**
       * The Authorization Server Token endpoint.
       */
      private String tokenEndpoint;

      /**
       * When running in development mode we set the audience of our private_key_jwt objects to the AS issuer id instead
       * of the token endpoint. Needed if running in development mode.
       */
      private String asIssuerId;

      /**
       * Gets the Authorization Server Token endpoint.
       *
       * @return the Authorization Server Token endpoint
       */
      public String getTokenEndpoint() {
        return this.tokenEndpoint;
      }

      /**
       * Assigns the Authorization Server Token endpoint.
       *
       * @param tokenEndpoint the Authorization Server Token endpoint
       */
      public void setTokenEndpoint(final String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
      }

      /**
       * Gets the AS issuer ID (used as audience in development mode).
       *
       * @return the AS issuer ID (used as audience in development mode)
       */
      public String getAsIssuerId() {
        return this.asIssuerId;
      }

      /**
       * Assigns the AS issuer ID (used as audience in development mode).
       *
       * @param asIssuerId the AS issuer ID (used as audience in development mode)
       */
      public void setAsIssuerId(final String asIssuerId) {
        this.asIssuerId = asIssuerId;
      }

      /** {@inheritDoc} */
      @Override
      public void afterPropertiesSet() {
        Assert.hasText(this.tokenEndpoint, "connector.idm.oauth2.client.token-endpoint must be assigned");
      }

    }

    /**
     * Settings if the eIDAS connector should act as an OAuth2 Authorization Server and issue access token for itself.
     */
    public static class OAuth2ServerProperties implements InitializingBean {

      /**
       * The issuer ID to use for the issued access tokens.
       */
      private String issuer;

      /**
       * The duration (lifetime) for issued access tokens. The default is one hour.
       */
      private Duration lifetime;

      /**
       * Gets the issuer ID to use for the issued access tokens.
       *
       * @return the issuer ID to use for the issued access tokens
       */
      public String getIssuer() {
        return this.issuer;
      }

      /**
       * Assigns the issuer ID to use for the issued access tokens.
       *
       * @param issuer the issuer ID to use for the issued access tokens
       */
      public void setIssuer(final String issuer) {
        this.issuer = issuer;
      }

      /**
       * Gets the duration (lifetime) for issued access tokens.
       *
       * @return the duration (lifetime) for issued access tokens
       */
      public Duration getLifetime() {
        return this.lifetime;
      }

      /**
       * Assigns the duration (lifetime) for issued access tokens.
       *
       * @param lifetime the duration (lifetime) for issued access tokens
       */
      public void setLifetime(final Duration lifetime) {
        this.lifetime = lifetime;
      }

      /** {@inheritDoc} */
      @Override
      public void afterPropertiesSet() {
        Assert.hasText(this.issuer, "connector.idm.oauth2.server.issuer must be assigned");
      }

    }

  }

}
