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
import java.util.Optional;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.thymeleaf.spring6.SpringTemplateEngine;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.eidas.attributes.AttributeMappingService;
import se.swedenconnect.eidas.connector.authn.EidasAuthenticationController;
import se.swedenconnect.eidas.connector.authn.EidasAuthenticationProvider;
import se.swedenconnect.eidas.connector.authn.idm.DefaultIdmClient;
import se.swedenconnect.eidas.connector.authn.idm.IdmClient;
import se.swedenconnect.eidas.connector.authn.idm.NoopIdmClient;
import se.swedenconnect.eidas.connector.authn.idm.OAuth2Client;
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
import se.swedenconnect.spring.saml.idp.config.configurers.Saml2IdpConfigurerAdapter;
import se.swedenconnect.spring.saml.idp.extensions.SignatureMessagePreprocessor;
import se.swedenconnect.spring.saml.idp.metadata.EntityCategoryHelper;
import se.swedenconnect.spring.saml.idp.response.ThymeleafResponsePage;
import se.swedenconnect.spring.saml.idp.settings.IdentityProviderSettings;

/**
 * Configuration class for the IdP part of the Sweden Connect eIDAS Connector.
 *
 * @author Martin Lindström
 */
@Configuration
@EnableConfigurationProperties({ ConnectorConfigurationProperties.class })
@DependsOn("openSAML")
@Slf4j
public class ConnectorConfiguration {

  private final ConnectorConfigurationProperties connectorProperties;

  private final IdentityProviderSettings idpSettings;

  /**
   * Connector.
   *
   * @param connectorProperties the configuration properties
   */
  public ConnectorConfiguration(final ConnectorConfigurationProperties connectorProperties,
      IdentityProviderSettings idpSettings) {
    this.connectorProperties = connectorProperties;
    this.idpSettings = idpSettings;
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
//        .csrf(csrf -> {
//          csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
//          final CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
//          requestHandler.setCsrfRequestAttributeName(null);
//          csrf.csrfTokenRequestHandler(requestHandler);
//        })
        .csrf(c -> c.ignoringRequestMatchers(EidasAuthenticationController.ASSERTION_CONSUMER_PATH + "/**"))
        .authorizeHttpRequests((authorize) -> authorize
            .requestMatchers(HttpMethod.POST, EidasAuthenticationController.ASSERTION_CONSUMER_PATH + "/**").permitAll()
            .requestMatchers(EidasAuthenticationProvider.AUTHN_PATH + "/**", EidasAuthenticationProvider.RESUME_PATH + "/**")
            .permitAll()
            .requestMatchers(HttpMethod.GET, EidasSpMetadataController.METADATA_PATH + "/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/mrecord/**").permitAll() // mocked IdM
            .requestMatchers(HttpMethod.GET, "/images/**", "/error", "/js/**", "/scripts/**", "/webjars/**", "/css/**")
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
    return (http, configurer) -> {
      configurer
          .authnRequestProcessor(c -> c.authenticationProvider(
              pc -> pc.signatureMessagePreprocessor(signMessageProcessor)));
//          .idpMetadataEndpoint(mdCustomizer -> {
//            mdCustomizer.entityDescriptorCustomizer(this.metadataCustomizer());
//          });
    };
  }

  // For customizing the metadata published by the IdP
  //
  private Customizer<EntityDescriptor> metadataCustomizer() {
    return e -> {
//      final RequestedPrincipalSelection rps = RequestedPrincipalSelectionBuilder.builder()
//          .matchValues(MatchValueBuilder.builder().name(AttributeConstants.ATTRIBUTE_NAME_PRID).build(),
//              MatchValueBuilder.builder().name(AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER).build(),
//              MatchValueBuilder.builder().name(AttributeConstants.ATTRIBUTE_NAME_PERSONAL_IDENTITY_NUMBER).build(),
//              MatchValueBuilder.builder().name(AttributeConstants.ATTRIBUTE_NAME_MAPPED_PERSONAL_IDENTITY_NUMBER)
//                  .build())
//          .build();
//
//      final IDPSSODescriptor ssoDescriptor = e.getIDPSSODescriptor(SAMLConstants.SAML20P_NS);
//      Extensions extensions = ssoDescriptor.getExtensions();
//      if (extensions == null) {
//        extensions = (Extensions) XMLObjectSupport.buildXMLObject(Extensions.DEFAULT_ELEMENT_NAME);
//        ssoDescriptor.setExtensions(extensions);
//      }
//      extensions.getUnknownXMLObjects().add(rps);
    };
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
   * Creates a {@link MetadataProvider} bean that is to be used for {@link EuMetadataProvider}.
   *
   * @return a {@link MetadataProvider}
   * @throws Exception for config errors
   */
  @Bean(initMethod = "initialize", destroyMethod = "destroy")
  MetadataProvider metadataProvider() throws Exception {
    return MetadataProviderUtils.createMetadataProvider(this.connectorProperties.getEuMetadata());
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
  OAuth2Handler oauth2Handler(final ConnectorCredentials connectorCredentials) throws Exception {
    if (this.connectorProperties.getIdm() == null) {
      // IdM-feature is not active
      return null;
    }
    if (this.connectorProperties.getIdm().getOauth2().getClient() != null) {
      final OAuth2Client client = new OAuth2Client(
          this.connectorProperties.getIdm().getOauth2().getClient().getTokenEndpoint(),
          this.connectorProperties.getIdm().getOauth2().getClientId(),
          this.connectorProperties.getIdm().getOauth2().getScopes(),
          connectorCredentials.getOAuth2Credential());

      Optional.ofNullable(this.connectorProperties.getIdm().getOauth2().getClient().getAsIssuerId())
        .ifPresent(i -> client.setAsIssuerId(i));

      return client;
    }
    else {
      final OAuth2Server server = new OAuth2Server(
          this.connectorProperties.getIdm().getOauth2().getClientId(),
          this.connectorProperties.getIdm().getOauth2().getScopes(),
          this.connectorProperties.getIdm().getOauth2().getServer().getIssuer(),
          this.connectorProperties.getIdm().getOauth2().getServer().getAudience(),
          connectorCredentials.getOAuth2Credential());

      Optional.ofNullable(this.connectorProperties.getIdm().getOauth2().getServer().getLifetime())
        .ifPresent(d -> server.setLifeTime(d));

      return server;
    }
  }

  @Bean
  IdmClient idmClient(@Autowired(required = false) final OAuth2Handler oauth2) {
    if (this.connectorProperties.getIdm() != null) {
      if (oauth2 == null) {
        throw new IllegalArgumentException("Missing OAuth2 handler");
      }
      return new DefaultIdmClient(this.connectorProperties.getIdm().getApiBaseUrl(), oauth2);
    }
    else {
      log.warn("eIDAS Identity Matching feature is turned off");
      return new NoopIdmClient();
    }
  }

  @Bean
  EidasAuthenticationProvider eidasAuthenticationProvider(
      final EidasAuthnRequestGenerator authnRequestGenerator,
      final EidasResponseProcessor eidasResponseProcessor,
      final EuMetadataProvider euMetadataProvider,
      final AttributeMappingService attributeMappingService,
      final PridService pridService,
      final IdmClient idmClient) {
    return new EidasAuthenticationProvider(this.idpSettings.getBaseUrl(),
        authnRequestGenerator,
        eidasResponseProcessor,
        euMetadataProvider,
        attributeMappingService,
        pridService,
        idmClient,
        this.connectorProperties.getIdp().getSupportedLoas(),
        this.connectorProperties.getIdp().getEntityCategories(),
        this.connectorProperties.getIdp().getPingWhitelist());
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
