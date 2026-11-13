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
package se.swedenconnect.eidas.simulator.config;

import net.shibboleth.shared.component.ComponentInitializationException;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.Resource;
import se.swedenconnect.eidas.simulator.authn.AlgorithmSelector;
import se.swedenconnect.eidas.simulator.authn.AttributeReleaser;
import se.swedenconnect.eidas.simulator.authn.AuthnRequestDecoder;
import se.swedenconnect.eidas.simulator.authn.AuthnRequestProcessor;
import se.swedenconnect.eidas.simulator.authn.ResponseFactory;
import se.swedenconnect.eidas.simulator.country.CountryRepository;
import se.swedenconnect.eidas.simulator.metadata.ConnectorMetadataResolver;
import se.swedenconnect.eidas.simulator.metadata.CountryMetadataFactory;
import se.swedenconnect.opensaml.OpenSAMLInitializer;
import se.swedenconnect.opensaml.OpenSAMLSecurityDefaultsConfig;
import se.swedenconnect.opensaml.OpenSAMLSecurityExtensionConfig;
import se.swedenconnect.opensaml.eidas.xmlsec.Eidas_1_4_SecurityConfiguration;
import se.swedenconnect.security.credential.KeyStoreCredential;
import se.swedenconnect.security.credential.factory.KeyStoreFactory;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * Configuration of the eIDAS simulator.
 *
 * @author Martin Lindström
 */
@Configuration
@EnableConfigurationProperties(SimulatorProperties.class)
public class SimulatorConfiguration {

  /** The configuration properties. */
  private final @NonNull SimulatorProperties properties;

  /**
   * Constructor.
   *
   * @param properties the configuration properties
   */
  public SimulatorConfiguration(final @NonNull SimulatorProperties properties) {
    this.properties = properties;
  }

  /**
   * Initializes OpenSAML with the eIDAS security defaults.
   *
   * @return the initializer
   * @throws Exception for initialization errors
   */
  @Bean("openSAML")
  @NonNull
  OpenSAMLInitializer openSAML() throws Exception {
    OpenSAMLInitializer.getInstance().initialize(
        new OpenSAMLSecurityDefaultsConfig(new Eidas_1_4_SecurityConfiguration()),
        new OpenSAMLSecurityExtensionConfig());
    return OpenSAMLInitializer.getInstance();
  }

  /**
   * Loads the key store of the simulator.
   *
   * @return the key store
   * @throws Exception if the key store cannot be loaded
   */
  @Bean
  @NonNull
  KeyStore simulatorKeyStore() throws Exception {
    final SimulatorProperties.KeyStoreProperties ks = this.properties.getKeystore();
    final Resource location = Objects.requireNonNull(ks.getLocation(), "simulator.keystore.location must be set");
    try (final InputStream in = location.getInputStream()) {
      return KeyStoreFactory.loadKeyStore(in, keyPassword(ks), ks.getType(), null);
    }
  }

  /**
   * Creates the repository of the active countries.
   *
   * @param simulatorKeyStore the key store
   * @return the country repository
   */
  @Bean
  @NonNull
  CountryRepository countryRepository(final @NonNull KeyStore simulatorKeyStore) {
    return new CountryRepository(this.properties.getCountries(), this.properties.getActiveCountries(),
        this.getBaseUrl(), simulatorKeyStore, keyPassword(this.properties.getKeystore()));
  }

  /**
   * Creates the factory of the published metadata.
   *
   * @param countryRepository the active countries
   * @param simulatorKeyStore the key store
   * @return the metadata factory
   * @throws Exception if the metadata signing key cannot be loaded
   */
  @Bean
  @DependsOn("openSAML")
  @NonNull
  CountryMetadataFactory countryMetadataFactory(final @NonNull CountryRepository countryRepository,
      final @NonNull KeyStore simulatorKeyStore) throws Exception {
    final SimulatorProperties.MetadataProperties md = this.properties.getMetadata();
    final KeyStoreCredential credential = new KeyStoreCredential(simulatorKeyStore, md.getSigningKeyAlias(),
        keyPassword(this.properties.getKeystore()));
    credential.setName("metadata-signing");
    return new CountryMetadataFactory(countryRepository, credential, this.getBaseUrl(), md.getValidity(),
        md.getCacheDuration());
  }

  /**
   * Creates the resolver of the connector metadata.
   *
   * @return the resolver
   * @throws Exception if the validation certificate cannot be read
   */
  @Bean
  @DependsOn("openSAML")
  @NonNull
  ConnectorMetadataResolver connectorMetadataResolver() throws Exception {
    final SimulatorProperties.ConnectorProperties connector = this.properties.getConnector();
    X509Certificate certificate = null;
    if (connector.getValidationCertificate() != null) {
      try (final InputStream in = connector.getValidationCertificate().getInputStream()) {
        certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
      }
    }
    return new ConnectorMetadataResolver(
        Objects.requireNonNull(connector.getMetadataUrl(), "simulator.connector.metadata-url must be set"),
        certificate, connector.getRefreshInterval());
  }

  /**
   * Creates the decoder of authentication requests.
   *
   * @return the decoder
   */
  @Bean
  @NonNull
  AuthnRequestDecoder authnRequestDecoder() {
    return new AuthnRequestDecoder();
  }

  /**
   * Creates the processor of authentication requests.
   *
   * @param connectorMetadataResolver the connector metadata
   * @return the processor
   */
  @Bean
  @DependsOn("openSAML")
  @NonNull
  AuthnRequestProcessor authnRequestProcessor(final @NonNull ConnectorMetadataResolver connectorMetadataResolver) {
    final SimulatorProperties.RequestProperties request = this.properties.getRequest();
    final AuthnRequestProcessor processor =
        new AuthnRequestProcessor(connectorMetadataResolver, request.getMaxAge(), request.getClockSkew());
    processor.setStrictNameIdPolicy(request.isStrictNameIdPolicy());
    return processor;
  }

  /**
   * Creates the attribute releaser.
   *
   * @return the attribute releaser
   */
  @Bean
  @NonNull
  AttributeReleaser attributeReleaser() {
    return new AttributeReleaser();
  }

  /**
   * Creates the response factory.
   *
   * @return the response factory
   * @throws ComponentInitializationException if the factory cannot be created
   */
  @Bean
  @DependsOn("openSAML")
  @NonNull
  ResponseFactory responseFactory() throws ComponentInitializationException {
    return new ResponseFactory(new AlgorithmSelector());
  }

  /**
   * Gets the base URL of the simulator, without a trailing slash.
   *
   * @return the base URL
   */
  private @NonNull String getBaseUrl() {
    final String baseUrl = Objects.requireNonNull(this.properties.getBaseUrl(), "simulator.base-url must be set");
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  /**
   * Gets the password of the key store and its keys.
   *
   * @param ks the key store settings
   * @return the password
   */
  private static char @NonNull [] keyPassword(final SimulatorProperties.@NonNull KeyStoreProperties ks) {
    return Objects.requireNonNull(ks.getPassword(), "simulator.keystore.password must be set").toCharArray();
  }

}
