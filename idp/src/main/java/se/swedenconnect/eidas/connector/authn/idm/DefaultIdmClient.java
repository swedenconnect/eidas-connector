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
package se.swedenconnect.eidas.connector.authn.idm;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import se.swedenconnect.eidas.connector.authn.EidasAuthenticationToken;
import se.swedenconnect.eidas.connector.config.DevelopmentMode;
import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;

import javax.net.ssl.SSLContext;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

  /** Constant for binding URI that we should not receive ... */
  private static final String REGISTERED_USER_BINDING = "http://id.swedenconnect.se/id-binding/process/registered";

  /**
   * Constructor.
   *
   * @param idmApiBaseUrl the base URL for the API
   * @param oauth2 the OAuth2 handler
   * @param trustBundle SSL Bundle holding the trust configuration for TLS-calls against the IdM server (optional)
   */
  public DefaultIdmClient(final String idmApiBaseUrl, final OAuth2Handler oauth2, final SslBundle trustBundle) {

    Objects.requireNonNull(idmApiBaseUrl, "idmApiBaseUrl must not be null");
    this.oauth2 = Objects.requireNonNull(oauth2, "oauth2 must not be null");

    final RestClient.Builder builder = RestClient.builder().baseUrl(idmApiBaseUrl);
    if (trustBundle != null) {
      final SSLConnectionSocketFactory sslSocketFactory =
          SSLConnectionSocketFactoryBuilder.create().setSslContext(trustBundle.createSslContext()).build();
      final HttpClientConnectionManager cm =
          PoolingHttpClientConnectionManagerBuilder.create().setSSLSocketFactory(sslSocketFactory).build();
      final HttpClient httpClient = HttpClients.custom().setConnectionManager(cm).evictExpiredConnections().build();
      builder.requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
    }
    else if (DevelopmentMode.isActive()) {
      builder.requestFactory(developmentModeSettings());
    }
    this.restClient = builder.build();
  }

  /**
   * Returns {@code true}.
   */
  @Override
  public boolean isActive() {
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasRecord(final EidasAuthenticationToken token) throws IdmException {

    final String prid = this.getPridAttribute(token);

    log.debug("Querying Identity Matching API for existence of record for user '{}' ...", prid);

    final String accessToken = this.oauth2.getCheckAccessToken();

    try {
      return this.restClient.head()
          .uri(IDM_API_PATH, prid)
          .header(HttpHeaders.AUTHORIZATION, accessToken)
          .exchange((request, response) -> {
            if (response.getStatusCode().is2xxSuccessful()) {
              log.debug("IdM reported that there is an IdM record for '{}'", prid);
              return true;
            }
            else if (response.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()))) {
              log.debug("IdM reported that there is no IdM record for '{}'", prid);
              return false;
            }
            else {
              log.warn("Error checking for IdM record for '{}' - {}", prid, response.getStatusCode());
              return false;
            }
          });
    }
    catch (final RestClientResponseException e) {
      log.info("Failed to query IdM service", e);
      throw new IdmException("Failure querying for IdM record - " + e.getMessage(), e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public IdmRecord getRecord(final EidasAuthenticationToken token) throws IdmException {

    final String prid = this.getPridAttribute(token);

    log.debug("Querying Identity Matching API for record contents for '{}' ...", prid);

    // Get the OAuth2 token needed for our call to the IdM Query API ...
    //
    final String accessToken = this.oauth2.getGetAccessToken(prid);

    // Invoke the API ...
    //
    try {
      final IdmQueryResponse response = this.restClient.get()
          .uri(IDM_API_PATH, prid)
          .accept(MediaType.APPLICATION_JSON)
          .header(HttpHeaders.AUTHORIZATION, accessToken)
          .retrieve()
          .body(IdmQueryResponse.class);

      if (response == null) {
        throw new IdmException("Invalid response from IdM API - missing response");
      }
      if (response.getRecordId() == null) {
        throw new IdmException("Invalid response from IdM API - missing record ID");
      }
      if (response.getSwedishId() == null) {
        throw new IdmException("Invalid response from IdM API - missing Swedish ID");
      }
      if (response.getBindingLevel() == null) {
        throw new IdmException("Invalid response from IdM API - missing binding level");
      }
      if (!Objects.equals(prid, response.getEidasUserId())) {
        final String msg = "Invalid response from IdM API - expected prid '%s', but was '%s'"
            .formatted(prid, response.getEidasUserId());
        throw new IdmException(msg);
      }
      if (response.getBindings() == null || response.getBindings().isEmpty()) {
        throw new IdmException("Invalid response from IdM API - missing Binding URI");
      }
      final List<String> bindings = response.getBindings().stream()
          .filter(b -> {
            if (Objects.equals(REGISTERED_USER_BINDING, b)) {
              log.warn("IdM service included '{}' binding for user '{}' - this is incorrect",
                  REGISTERED_USER_BINDING, prid);
              return false;
            }
            return true;
          })
          .toList();
      final String bindingUris = String.join(";", bindings);

      return new IdmRecord(response.getRecordId(), response.getSwedishId(), bindingUris);
    }
    catch (final RestClientResponseException e) {
      if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
        throw new IdmException("User '%s' has no Identity Matching record".formatted(prid), e);
      }
      final String msg = "Error querying for IdM record - %d %s"
          .formatted(e.getStatusCode().value(), e.getResponseBodyAsString());
      throw new IdmException(msg, e);
    }
    catch (final Exception e) {
      final String msg = "Error querying for IdM record - %s".formatted(e.getMessage());
      throw new IdmException(msg, e);
    }
  }

  /**
   * Extracts the PRID attribute value from the user authentication token.
   *
   * @param token the user authentication token
   * @return the PRID attribute value
   * @throws IdmException if no PRID attribute is available
   */
  private String getPridAttribute(final EidasAuthenticationToken token) throws IdmException {
    return Optional.ofNullable(token.getAttribute(AttributeConstants.ATTRIBUTE_NAME_PRID))
        .filter(a -> !a.getValues().isEmpty())
        .map(a -> a.getStringValues().get(0))
        .orElseThrow(() -> new IdmException("No PRID attribute available for user"));
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
