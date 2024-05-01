/*
 * Copyright 2017-2024 Sweden Connect
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import se.swedenconnect.security.credential.factory.PkiCredentialConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for eIDAS Identity Matching.
 *
 * @author Martin Lindström
 */
public class IdmProperties implements InitializingBean {

  /**
   * The URL to the Identity Matching service (for inclusion in views).
   */
  @Getter
  @Setter
  private String serviceUrl;

  /**
   * The base URL for the eIDAS Identity Matching API.
   */
  @Getter
  @Setter
  private String apiBaseUrl;

  /**
   * Connector OAuth2 client settings.
   */
  @Getter
  @Setter
  private OAuth2Properties oauth2;

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
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

  /**
   * Properties for the Connector OAuth2 handler.
   */
  public static class OAuth2Properties implements InitializingBean {

    /**
     * The Connector OAuth2 client ID.
     */
    @Getter
    @Setter
    private String clientId;

    /**
     * The scope(s) to request for accessing the IdM Query API.
     */
    @Getter
    @Setter
    private List<String> scopes;

    /**
     * The ID for the resource we are sending our access tokens to (the IdM-service).
     */
    @Getter
    @Setter
    private String resourceId;

    /**
     * The credential to use for authentication against the Authorization Server (if the connector acts as an OAuth2
     * client) OR for use of signing of access tokens (if the connector also acts as an OAuth2 Authorization Server). If
     * not assigned, the connector default credential will be used.
     */
    @Getter
    @Setter
    private PkiCredentialConfigurationProperties credential;

    /**
     * Settings if the eIDAS connector should act as an OAuth2 client. Mutually exclusive with 'server'.
     */
    @Getter
    @Setter
    private OAuth2ClientProperties client;

    /**
     * Settings if the eIDAS connector should act as an OAuth2 Authorization Server and issue
     * access token for itself. Mutually exclusive with 'client'.
     */
    @Getter
    @Setter
    private OAuth2ServerProperties server;

    /** {@inheritDoc} */
    @Override
    public void afterPropertiesSet() throws Exception {
      Assert.hasText(this.clientId, "connector.idm.oauth2.client-id must be assigned");
      Assert.notEmpty(this.scopes, "connector.idm.oauth2.scopes must contain at least one scope");
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
      @Getter
      @Setter
      private String tokenEndpoint;

      /**
       * When running in development mode we set the audience of our private_key_jwt objects
       * to the AS issuer id instead of the token endpoint. Needed if running in development mode.
       */
      @Getter
      @Setter
      private String asIssuerId;


      /** {@inheritDoc} */
      @Override
      public void afterPropertiesSet() {
        Assert.hasText(this.tokenEndpoint, "connector.idm.oauth2.client.token-endpoint must be assigned");
      }

    }

    /**
     * Settings if the eIDAS connector should act as an OAuth2 Authorization Server and issue
     * access token for itself.
     */
    public static class OAuth2ServerProperties implements InitializingBean {

      /**
       * The issuer ID to use for the issued access tokens.
       */
      @Getter
      @Setter
      private String issuer;

      /**
       * The duration (lifetime) for issued access tokens. The default is one hour.
       */
      @Getter
      @Setter
      private Duration lifetime;

      /** {@inheritDoc} */
      @Override
      public void afterPropertiesSet() {
        Assert.hasText(this.issuer, "connector.idm.oauth2.server.issuer must be assigned");
      }

    }

  }

}
