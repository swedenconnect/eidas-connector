/*
 * Copyright 2023-2024 Sweden Connect
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

import java.security.cert.X509Certificate;
import java.util.Optional;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.factory.PkiCredentialConfigurationProperties;
import se.swedenconnect.security.credential.factory.PkiCredentialFactoryBean;
import se.swedenconnect.spring.saml.idp.autoconfigure.settings.CredentialConfigurationProperties;

/**
 * Configuration for connector credentials (i.e., the SP credentials).
 *
 * @author Martin Lindström
 */
@Configuration
@EnableConfigurationProperties(ConnectorConfigurationProperties.class)
public class ConnectorCredentialsConfiguration {

  /** The credential properties - may be {@code null}. */
  private final CredentialConfigurationProperties properties;

  /** OAuth2 client properties. */
  private final IdmProperties.OAuth2Properties oauth2Properties;

  /**
   * Constructor.
   *
   * @param connectorProperties the connector properties
   */
  public ConnectorCredentialsConfiguration(final ConnectorConfigurationProperties connectorProperties) {
    this.properties = Optional.ofNullable(connectorProperties.getEidas().getCredentials())
        .orElseGet(() -> new CredentialConfigurationProperties());
    this.oauth2Properties = Optional.ofNullable(connectorProperties.getIdm())
        .map(IdmProperties::getOauth2)
        .orElseThrow(() -> new IllegalArgumentException("Missing connector.idm.oauth2"));
  }

  @Bean("connector.sp.credentials.Default")
  PkiCredential defaultCredential() throws Exception {
    return this.loadCredential(this.properties.getDefaultCredential());
  }

  @Bean("connector.sp.credentials.Sign")
  PkiCredential signCredential() throws Exception {
    return this.loadCredential(this.properties.getSign());
  }

  @Bean("connector.sp.credentials.FutureSign")
  X509Certificate futureSignCertificate() throws Exception {
    return this.properties.getFutureSign();
  }

  @Bean("connector.sp.credentials.Encrypt")
  PkiCredential encryptCredential() throws Exception {
    return this.loadCredential(this.properties.getEncrypt());
  }

  @Bean("connector.sp.credentials.PreviousEncrypt")
  PkiCredential previousEncryptCredential() throws Exception {
    return this.loadCredential(this.properties.getPreviousEncrypt());
  }

  @Bean("connector.sp.credentials.MetadataSign")
  PkiCredential metadataSignCredential() throws Exception {
    return this.loadCredential(this.properties.getMetadataSign());
  }

  @Bean("connector.idm.oauth2.Credential")
  PkiCredential oauth2Credential() throws Exception {
    return this.loadCredential(this.oauth2Properties.getCredential());
  }

  private PkiCredential loadCredential(final PkiCredentialConfigurationProperties props) throws Exception {
    if (props == null) {
      return null;
    }
    PkiCredentialFactoryBean factory = new PkiCredentialFactoryBean(props);
    factory.afterPropertiesSet();
    return factory.getObject();
  }

}
