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
package se.swedenconnect.eidas.connector.authn.idm;

import java.util.Objects;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.eidas.connector.authn.EidasAuthenticationToken;
import se.swedenconnect.eidas.connector.config.DevelopmentMode;
import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;
import se.swedenconnect.spring.saml.idp.attributes.UserAttribute;

/**
 * Default implementation of the {@link IdmClient} interface.
 *
 * @author Martin Lindström
 */
@Slf4j
public class DefaultIdmClient implements IdmClient {

  /** The API path. */
  public static final String IDM_API_PATH = "/api/v1/mrecord/{prid}";

  /** The OAuth2 handler. */
  private final OAuth2Handler oauth2;

  /** The RestClient. */
  private final RestClient restClient;

  /**
   * Constructor.
   *
   * @param idmApiBaseUrl the base URL for the API
   * @param oauth2 the OAuth2 handler
   */
  public DefaultIdmClient(final String idmApiBaseUrl, final OAuth2Handler oauth2) {

    Objects.requireNonNull(idmApiBaseUrl, "idmApiBaseUrl must not be null");
    this.oauth2 = Objects.requireNonNull(oauth2, "oauth2 must not be null");

    final RestClient.Builder builder = RestClient.builder().baseUrl(idmApiBaseUrl);
    if (DevelopmentMode.isActive()) {
      builder.requestFactory(developmentModeSettings());
    }
    this.restClient = builder.build();
  }

  /** {@inheritDoc} */
  @Override
  public boolean getRecord(final EidasAuthenticationToken token) throws IdmException {

    // First extract the PRID attribute ...
    //
    final String prid = Optional.ofNullable(token.getAttribute(AttributeConstants.ATTRIBUTE_NAME_PRID))
        .filter(a -> !a.getValues().isEmpty())
        .map(a -> a.getStringValues().get(0))
        .orElseThrow(() -> new IdmException("No PRID attribute available for user"));

    log.debug("Querying Identity Matching API for record for '{} ...'", prid);

    // Get the OAuth2 token needed for our call to the IdM Query API ...
    //
    final String accessToken = this.oauth2.getAccessToken();

    // Invoke the API ...
    //
    try {
      final IdmQueryResponse response = this.restClient.get()
          .uri(IDM_API_PATH, prid)
          .accept(MediaType.APPLICATION_JSON)
          .header(HttpHeaders.AUTHORIZATION, accessToken)
          .retrieve()
          .body(IdmQueryResponse.class);

      if (response.getSwedishId() == null) {
        throw new IdmException("Invalid response from IdM API - missing Swedish ID");
      }
      if (response.getBindingLevel() == null) {
        throw new IdmException("Invalid response from IdM API - missing binding level");
      }
      if (!Objects.equals(prid, response.getEidasUserId())) {
        log.error("Invalid response from IdM API - expected prid '{}', but was '{}'", prid, response.getEidasUserId());
        throw new IdmException("Invalid response from IdM API - mismatching PRID");
      }

      // TODO: audit log

      // Add attributes ...
      //
      token.addAttribute(new UserAttribute(
          AttributeConstants.ATTRIBUTE_NAME_MAPPED_PERSONAL_IDENTITY_NUMBER,
          AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_MAPPED_PERSONAL_IDENTITY_NUMBER,
          response.getSwedishId()));
      token.addAttribute(new UserAttribute(
          AttributeConstants.ATTRIBUTE_NAME_PERSONAL_IDENTITY_NUMBER_BINDING,
          AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_PERSONAL_IDENTITY_NUMBER_BINDING,
          response.getBindingLevel()));

      log.info("Identity matching record exists for user '{}'", prid); // TODO

      return true;
    }
    catch (final RestClientResponseException e) {
      if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
        log.debug("User '{}' has no Identity Matching record", prid);
        return false;
      }
      final String msg =
          "Error querying for IdM record - %d %s".formatted(e.getStatusCode().value(), e.getResponseBodyAsString());
      log.info("{}", msg, e);
      throw new IdmException(msg, e);
    }
    catch (final Exception e) {
      final String msg = "Error querying for IdM record - %s".formatted(e.getMessage());
      log.info("{}", msg, e);
      throw new IdmException(msg, e);
    }
  }

  private static ClientHttpRequestFactory developmentModeSettings() {
    try {
      // For this example we trust all SSL/TLS certs. DO NOT COPY AND USE IN PRODUCTION!
      //
      final TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
      final SSLContext sslContext = SSLContexts.custom()
          .loadTrustMaterial(null, acceptingTrustStrategy)
          .build();
      final SSLConnectionSocketFactory sslsf =
          new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
      final Registry<ConnectionSocketFactory> socketFactoryRegistry =
          RegistryBuilder.<ConnectionSocketFactory> create()
              .register("https", sslsf)
              .register("http", new PlainConnectionSocketFactory())
              .build();

      final BasicHttpClientConnectionManager connectionManager =
          new BasicHttpClientConnectionManager(socketFactoryRegistry);
      final CloseableHttpClient httpClient = HttpClients.custom()
          .setConnectionManager(connectionManager)
          .disableRedirectHandling()
          .build();

      final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
      requestFactory.setHttpClient(httpClient);

      return requestFactory;
    }
    catch (final Exception e) {
      throw new IllegalArgumentException("Failed to configure RestClient", e);
    }
  }

}
