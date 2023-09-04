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

import java.util.Objects;

import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.EncryptionMethod;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.UsageType;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.thymeleaf.spring5.SpringTemplateEngine;

import se.swedenconnect.eidas.attributes.AttributeMappingService;
import se.swedenconnect.eidas.attributes.DefaultAttributeMappingService;
import se.swedenconnect.eidas.attributes.conversion.AttributeConverterConstants;
import se.swedenconnect.eidas.connector.authn.EidasAuthenticationProvider;
import se.swedenconnect.eidas.connector.authn.metadata.DefaultEuMetadataProvider;
import se.swedenconnect.eidas.connector.authn.metadata.EuMetadataProvider;
import se.swedenconnect.opensaml.saml2.metadata.provider.MetadataProvider;
import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;
import se.swedenconnect.opensaml.sweid.saml2.authn.psc.RequestedPrincipalSelection;
import se.swedenconnect.opensaml.sweid.saml2.authn.psc.build.MatchValueBuilder;
import se.swedenconnect.opensaml.sweid.saml2.authn.psc.build.RequestedPrincipalSelectionBuilder;
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
   * Gets a {@link SecurityFilterChain} for the actuator endpoints.
   *
   * @param http the HttpSecurity object
   * @return a SecurityFilterChain
   * @throws Exception for config errors
   */
  @Bean
  @Order(2)
  SecurityFilterChain actuatorSecurityFilterChain(final HttpSecurity http) throws Exception {

    http.authorizeHttpRequests((authorize) -> authorize
        .requestMatchers(EndpointRequest.toAnyEndpoint())
        .permitAll());

    return http.build();
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
  @Order(3)
  SecurityFilterChain defaultSecurityFilterChain(final HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests((authorize) -> authorize
            .antMatchers(EidasAuthenticationProvider.AUTHN_PATH + "/**",
                EidasAuthenticationProvider.RESUME_PATH + "/**")
            .permitAll()
            .antMatchers("/images/**", "/error", "/js/**", "/scripts/**", "/webjars/**", "/css/**").permitAll()
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
  Saml2IdpConfigurerAdapter samlIdpSettingsAdapter(/* final SignatureMessagePreprocessor signMessageProcessor */) {
    return (http, configurer) -> {
      configurer
//          .authnRequestProcessor(c -> c.authenticationProvider(
//              pc -> pc.signatureMessagePreprocessor(signMessageProcessor)))
          .idpMetadataEndpoint(mdCustomizer -> {
            mdCustomizer.entityDescriptorCustomizer(this.metadataCustomizer());
          });
    };
  }

  // For customizing the metadata published by the IdP
  //
  private Customizer<EntityDescriptor> metadataCustomizer() {
    return e -> {
      final RequestedPrincipalSelection rps = RequestedPrincipalSelectionBuilder.builder()
          .matchValues(MatchValueBuilder.builder().name(AttributeConstants.ATTRIBUTE_NAME_PRID).build(),
              MatchValueBuilder.builder().name(AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER).build(),
              MatchValueBuilder.builder().name(AttributeConstants.ATTRIBUTE_NAME_PERSONAL_IDENTITY_NUMBER).build(),
              MatchValueBuilder.builder().name(AttributeConstants.ATTRIBUTE_NAME_MAPPED_PERSONAL_IDENTITY_NUMBER)
                  .build())
          .build();

      final IDPSSODescriptor ssoDescriptor = e.getIDPSSODescriptor(SAMLConstants.SAML20P_NS);
      Extensions extensions = ssoDescriptor.getExtensions();
      if (extensions == null) {
        extensions = (Extensions) XMLObjectSupport.buildXMLObject(Extensions.DEFAULT_ELEMENT_NAME);
        ssoDescriptor.setExtensions(extensions);
      }
      extensions.getUnknownXMLObjects().add(rps);

      KeyDescriptor encryption = null;
      for (final KeyDescriptor kd : ssoDescriptor.getKeyDescriptors()) {
        if (Objects.equals(UsageType.ENCRYPTION, kd.getUse())) {
          encryption = kd;
          break;
        }
        if (kd.getUse() == null || Objects.equals(UsageType.UNSPECIFIED, kd.getUse())) {
          encryption = kd;
        }
      }
      if (encryption != null) {
        final String[] algs = { "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p",
            "http://www.w3.org/2009/xmlenc11#aes256-gcm",
            "http://www.w3.org/2009/xmlenc11#aes192-gcm",
            "http://www.w3.org/2009/xmlenc11#aes128-gcm"
        };
        for (final String alg : algs) {
          final EncryptionMethod method =
              (EncryptionMethod) XMLObjectSupport.buildXMLObject(EncryptionMethod.DEFAULT_ELEMENT_NAME);
          method.setAlgorithm(alg);
          encryption.getEncryptionMethods().add(method);
        }
      }

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

  /**
   * Creates a {@link AttributeMappingService} that helps us map between Swedish eID attributes and eIDAS attributes.
   * 
   * @return a {@link AttributeMappingService}
   */
  @Bean
  AttributeMappingService attributeMappingService() {
    return new DefaultAttributeMappingService(AttributeConverterConstants.DEFAULT_CONVERTERS);
  }
  
  @Bean
  EidasAuthenticationProvider eidasAuthenticationProvider(final EuMetadataProvider euMetadataProvider) {
    return new EidasAuthenticationProvider(this.idpSettings.getBaseUrl(),
        null, /* response processor */
        euMetadataProvider,
        this.connectorProperties.getIdp().getSupportedLoas(),
        this.connectorProperties.getIdp().getEntityCategories(),
        this.connectorProperties.getIdp().getPingWhitelist());
        
  }
  
}
