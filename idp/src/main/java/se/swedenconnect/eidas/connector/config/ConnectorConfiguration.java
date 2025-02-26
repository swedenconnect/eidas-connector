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

import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import se.swedenconnect.eidas.attributes.AttributeMappingService;
import se.swedenconnect.eidas.connector.authn.EidasAuthenticationController;
import se.swedenconnect.eidas.connector.authn.EidasAuthenticationProvider;
import se.swedenconnect.eidas.connector.authn.idm.DefaultIdmClient;
import se.swedenconnect.eidas.connector.authn.idm.IdmClient;
import se.swedenconnect.eidas.connector.authn.idm.NoopIdmClient;
import se.swedenconnect.eidas.connector.authn.idm.OAuth2Handler;
import se.swedenconnect.eidas.connector.authn.idm.OAuth2Server;
import se.swedenconnect.eidas.connector.authn.metadata.DefaultEuMetadataProvider;
import se.swedenconnect.eidas.connector.authn.metadata.EuMetadataProvider;
import se.swedenconnect.eidas.connector.authn.sp.EidasAuthnRequestGenerator;
import se.swedenconnect.eidas.connector.authn.sp.EidasResponseProcessor;
import se.swedenconnect.eidas.connector.authn.sp.EidasSpMetadataController;
import se.swedenconnect.eidas.connector.prid.generator.PridGenBase64Eidas;
import se.swedenconnect.eidas.connector.prid.generator.PridGenColResistEidas;
import se.swedenconnect.eidas.connector.prid.generator.PridGenDefaultEidas;
import se.swedenconnect.eidas.connector.prid.generator.PridGenTestEidas;
import se.swedenconnect.eidas.connector.prid.service.PridService;
import se.swedenconnect.opensaml.saml2.metadata.provider.MetadataProvider;
import se.swedenconnect.opensaml.sweid.saml2.metadata.entitycategory.EntityCategoryRegistry;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.config.properties.PkiCredentialConfigurationProperties;
import se.swedenconnect.security.credential.factory.PkiCredentialFactory;
import se.swedenconnect.spring.saml.idp.config.configurers.Saml2IdpConfigurerAdapter;
import se.swedenconnect.spring.saml.idp.extensions.SignatureMessagePreprocessor;
import se.swedenconnect.spring.saml.idp.metadata.EntityCategoryHelper;
import se.swedenconnect.spring.saml.idp.response.ThymeleafResponsePage;
import se.swedenconnect.spring.saml.idp.settings.IdentityProviderSettings;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

/**
 * Configuration class for the IdP part of the Sweden Connect eIDAS Connector.
 *
 * @author Martin Lindström
 */
@Configuration
@EnableConfigurationProperties(ConnectorConfigurationProperties.class)
@DependsOn("openSAML")
public class ConnectorConfiguration {

  private static final Logger log = LoggerFactory.getLogger(ConnectorConfiguration.class);

  private final ConnectorConfigurationProperties connectorProperties;

  private final IdentityProviderSettings idpSettings;

  private final SslBundles sslBundles;

  private final PkiCredentialFactory credentialFactory;

  /**
   * Connector.
   *
   * @param connectorProperties the configuration properties
   * @param idpSettings the IdP configuration
   * @param sslBundles the SSL bundles
   * @param credentialFactory the credential factory bean
   */
  public ConnectorConfiguration(final ConnectorConfigurationProperties connectorProperties,
      final IdentityProviderSettings idpSettings, final SslBundles sslBundles,
      final PkiCredentialFactory credentialFactory) {
    this.connectorProperties = connectorProperties;
    this.idpSettings = idpSettings;
    this.sslBundles = sslBundles;
    this.credentialFactory = credentialFactory;
  }

  /**
   * Gets a default {@link SecurityFilterChain} protecting other resources.
   * <p>
   * The chain with order 1 is the Spring Security chain for the SAML IdP ...
   * </p>
   *
   * @param http the HttpSecurity object
   * @return a SecurityFilterChain
   * @throws Exception for config errors
   */
  @Bean
  @Order(2)
  SecurityFilterChain defaultSecurityFilterChain(final HttpSecurity http) throws Exception {
    http
        .securityContext(sc -> sc.requireExplicitSave(false))
        .csrf(c -> c.ignoringRequestMatchers(EidasAuthenticationController.ASSERTION_CONSUMER_PATH + "/**"))
        .authorizeHttpRequests((authorize) -> authorize
            .requestMatchers(HttpMethod.POST, EidasAuthenticationController.ASSERTION_CONSUMER_PATH + "/**").permitAll()
            .requestMatchers(EidasAuthenticationProvider.AUTHN_PATH + "/**",
                EidasAuthenticationProvider.RESUME_PATH + "/**")
            .permitAll()
            .requestMatchers(HttpMethod.GET, EidasSpMetadataController.METADATA_PATH + "/**").permitAll()
            .requestMatchers(HttpMethod.GET,
                "/img/**", "/images/**", "/error", "/js/**", "/scripts/**", "/webjars/**", "/css/**")
            .permitAll()
            .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
            .anyRequest().denyAll());

    return http.build();
  }

  /**
   * Gets a {@link Saml2IdpConfigurerAdapter} that applies custom configuration for the IdP.
   *
   * @param signMessageProcessor a {@link SignatureMessagePreprocessor} for display of sign messages
   * @return a {@link Saml2IdpConfigurerAdapter}
   */
  @Bean
  Saml2IdpConfigurerAdapter samlIdpSettingsAdapter(final SignatureMessagePreprocessor signMessageProcessor) {
    return (http, configurer) -> configurer
        .authnRequestProcessor(c -> c.authenticationProvider(
            pc -> pc.signatureMessagePreprocessor(signMessageProcessor)));
  }

  /**
   * A response page using Thymeleaf to post the response.
   *
   * @param templateEngine the template engine
   * @return a {@link ThymeleafResponsePage}
   */
  @Bean
  ThymeleafResponsePage responsePage(final SpringTemplateEngine templateEngine) {
    return new ThymeleafResponsePage(templateEngine, "post-response.html");
  }

  /**
   * Creates the {@link ConnectorCredentials} bean.
   *
   * @param defaultCredential the default credential for the SAML IdP
   * @param signCredential the signing credential for the SAML IdP
   * @param futureSignCertificate the future signing certificate for the SAML IdP
   * @param encryptCredential the encryption credential for the SAML IdP
   * @param previousEncryptCredential the previous encryption credential for the SAML IdP
   * @param metadataSignCredential the metadata signing credential for the SAML IdP
   * @return a {@link ConnectorCredentials} bean
   * @throws Exception for credential loading errors
   */
  @Bean
  ConnectorCredentials connectorCredentials(
      final @Qualifier("saml.idp.credentials.Default") @Autowired(required = false) PkiCredential defaultCredential,
      final @Qualifier("saml.idp.credentials.Sign") @Autowired(required = false) PkiCredential signCredential,
      final @Qualifier("saml.idp.credentials.FutureSign") @Autowired(required = false) X509Certificate futureSignCertificate,
      final @Qualifier("saml.idp.credentials.Encrypt") @Autowired(required = false) PkiCredential encryptCredential,
      final @Qualifier("saml.idp.credentials.PreviousEncrypt") @Autowired(required = false) PkiCredential previousEncryptCredential,
      final @Qualifier("saml.idp.credentials.MetadataSign") @Autowired(required = false) PkiCredential metadataSignCredential)
      throws Exception {

    return new ConnectorCredentials(defaultCredential, signCredential, futureSignCertificate, encryptCredential,
        previousEncryptCredential,
        this.loadCredential(this.connectorProperties.getEidas().getCredentials().getDefaultCredential()),
        this.loadCredential(this.connectorProperties.getEidas().getCredentials().getSign()),
        this.connectorProperties.getEidas().getCredentials().getFutureSign(),
        this.loadCredential(this.connectorProperties.getEidas().getCredentials().getEncrypt()),
        this.loadCredential(this.connectorProperties.getEidas().getCredentials().getPreviousEncrypt()),
        metadataSignCredential,
        this.loadCredential(this.connectorProperties.getEidas().getCredentials().getMetadataSign()),
        this.loadCredential(Optional.ofNullable(this.connectorProperties.getIdm())
            .map(IdmProperties::getOauth2)
            .map(IdmProperties.OAuth2Properties::getCredential)
            .orElse(null)));
  }

  private PkiCredential loadCredential(final PkiCredentialConfigurationProperties props) throws Exception {
    if (props == null) {
      return null;
    }
    return this.credentialFactory.createCredential(props);
  }

  /**
   * Creates a {@link MetadataProvider} bean that is to be used for {@link EuMetadataProvider}.
   *
   * @return a {@link MetadataProvider}
   * @throws Exception for config errors
   */
  @Bean(initMethod = "initialize", destroyMethod = "destroy")
  MetadataProvider metadataProvider(final SslBundles sslBundles) throws Exception {
    return MetadataProviderUtils.createMetadataProvider(this.connectorProperties.getEuMetadata(), sslBundles);
  }

  /**
   * Creates the {@link EuMetadataProvider} bean.
   *
   * @param metadataProvider the EU metadata provider
   * @param publisher the event publisher
   * @return the {@link EuMetadataProvider}
   */
  @Bean
  EuMetadataProvider euMetadataProvider(
      final MetadataProvider metadataProvider, final ApplicationEventPublisher publisher) {
    return new DefaultEuMetadataProvider(metadataProvider, publisher);
  }

  /**
   * Gets an {@link EntityCategoryRegistry} bean.
   *
   * @return an {@link EntityCategoryRegistry}
   */
  @Bean
  EntityCategoryRegistry entityCategoryRegistry() {
    return EntityCategoryHelper.getDefaultEntityCategoryRegistry();
  }

  @Bean
  OAuth2Handler oauth2Handler(final ConnectorCredentials connectorCredentials) {
    if (!this.connectorProperties.getIdm().getActive()) {
      // IdM-feature is not active
      return null;
    }
    if (this.connectorProperties.getIdm().getOauth2().getClient() != null) {
      throw new IllegalArgumentException("OAuth2 client is not yet supported");
    }
    else {
      final OAuth2Server server = new OAuth2Server(
          this.connectorProperties.getIdm().getOauth2().getClientId(),
          this.connectorProperties.getIdm().getOauth2().getCheckScopes(),
          this.connectorProperties.getIdm().getOauth2().getGetScopes(),
          this.connectorProperties.getIdm().getOauth2().getServer().getIssuer(),
          this.connectorProperties.getIdm().getOauth2().getResourceId(),
          connectorCredentials.getOAuth2Credential());

      Optional.ofNullable(this.connectorProperties.getIdm().getOauth2().getServer().getLifetime())
          .ifPresent(server::setLifeTime);

      return server;
    }
  }

  @Bean
  IdmClient idmClient(@Autowired(required = false) final OAuth2Handler oauth2) {
    if (this.connectorProperties.getIdm().getActive()) {
      if (oauth2 == null) {
        throw new IllegalArgumentException("Missing OAuth2 handler");
      }
      try {
        final SslBundle sslBundle = StringUtils.hasText(this.connectorProperties.getIdm().getTrustBundle())
            ? this.sslBundles.getBundle(this.connectorProperties.getIdm().getTrustBundle())
            : null;
        return new DefaultIdmClient(this.connectorProperties.getIdm().getApiBaseUrl(), oauth2, sslBundle);
      }
      catch (final NoSuchSslBundleException e) {
        log.warn("Configured SSL bundle '{}' does not exist - correct configuration!",
            this.connectorProperties.getIdm().getTrustBundle());

        return new DefaultIdmClient(this.connectorProperties.getIdm().getApiBaseUrl(), oauth2, null);
      }
    }
    else {
      log.warn("eIDAS Identity Matching feature is turned off");
      return new NoopIdmClient();
    }
  }

  @Bean
  EidasAuthenticationProvider eidasAuthenticationProvider(
      final ApplicationEventPublisher eventPublisher,
      @Qualifier("connector.sp.metadata") final EntityDescriptor metadata,
      final EidasAuthnRequestGenerator authnRequestGenerator,
      final EidasResponseProcessor eidasResponseProcessor,
      final EuMetadataProvider euMetadataProvider,
      final AttributeMappingService attributeMappingService,
      final PridService pridService,
      final IdmClient idmClient) {
    return new EidasAuthenticationProvider(this.idpSettings.getBaseUrl(),
        eventPublisher, metadata, authnRequestGenerator, eidasResponseProcessor, euMetadataProvider,
        attributeMappingService, pridService, idmClient, this.connectorProperties.getIdp().getSupportedLoas(),
        this.connectorProperties.getIdp().getEntityCategories(), this.connectorProperties.getIdp().getPingWhitelist());
  }

  @Bean
  PridService pridService() {
    return new PridService(
        this.connectorProperties.getPrid().getPolicyResource(),
        List.of(
            new PridGenDefaultEidas(this.connectorProperties.getCountry()),
            new PridGenColResistEidas(this.connectorProperties.getCountry()),
            new PridGenBase64Eidas(this.connectorProperties.getCountry()),
            new PridGenTestEidas(this.connectorProperties.getCountry())));
  }

}
