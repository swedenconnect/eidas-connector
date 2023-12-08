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
package se.swedenconnect.eidas.connector.authn.idm;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.util.tls.TLSVersion;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.eidas.connector.config.DevelopmentMode;
import se.swedenconnect.security.credential.PkiCredential;

/**
 * An OAuth2 client implementation of the {@link OAuth2Handler} interface. The implementation will request an access
 * token by calling the configured token endpoint at the Authorization Server.
 *
 * @author Martin Lindström
 */
@Slf4j
public class OAuth2Client extends AbstractOAuth2Handler {

  /** The eIDAS Authorization Server endpoint. */
  private final URI tokenEndpoint;

  /** Needed if running in development mode. */
  private URI asIssuerId;

  /** Used when invoking the AS in development mode. */
  private static SSLSocketFactory sslSocketFactory;

  /**
   * Constructor.
   *
   * @param tokenEndpoint the Authorization Server token endpoint
   * @param clientId the Connector OAuth2 client ID
   * @param scopes the OAuth2 scope(s) to use
   * @param oauth2Credential the credential used to sign the OAuth2 items
   * @throws URISyntaxException for bad token endpoint
   */
  public OAuth2Client(
      final String tokenEndpoint,
      final String clientId,
      final List<String> scopes,
      final PkiCredential oauth2Credential) throws URISyntaxException {
    super(clientId, scopes, oauth2Credential);
    this.tokenEndpoint = new URI(tokenEndpoint);
  }

  /**
   * If running in development mode we use the AS issuer ID for the aud claim instead of the endpoint.
   *
   * @param asIssuerId the AS issuer ID
   */
  public void setAsIssuerId(final String asIssuerId) {
    try {
      this.asIssuerId = new URI(asIssuerId);
    }
    catch (final URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected BearerAccessTokenHolder obtainAccessToken() throws IdmException {
    try {

      // Set up for private_key_jwt client authentication ...
      //
      final URI audience = DevelopmentMode.isActive() && this.asIssuerId != null
          ? this.asIssuerId
          : this.tokenEndpoint;

      final ClientAuthentication clientAuthentication =
          new PrivateKeyJWT(this.getClientId(), audience,
              (JWSAlgorithm) this.getOauth2Jwk().jwk().getAlgorithm(),
              this.getOauth2Credential().getPrivateKey(),
              this.getOauth2Jwk().jwk().getKeyID(),
              (Provider) null);

      // Create the token request ...
      //
      final TokenRequest request = new TokenRequest(this.tokenEndpoint, clientAuthentication,
          new ClientCredentialsGrant(), this.getScope(), null,
          Map.of("client_id", List.of(this.getClientId().getValue())));

      log.debug("Authorization request (Client credentials): token-endpoint-uri={}, client_id={}, scope={}",
          this.tokenEndpoint, this.getClientId(), this.getScope());

      final HTTPRequest httpRequest = request.toHTTPRequest();
      if (DevelopmentMode.isActive()) {
        httpRequest.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        httpRequest.setSSLSocketFactory(getSSLSocketFactory());
      }

      final HTTPResponse httpResponse = httpRequest.send();

      final TokenResponse response = TokenResponse.parse(httpResponse);

      if (!response.indicatesSuccess()) {
        final String msg = toString(response.toErrorResponse());
        log.info("Received OAuth2 error response: {}", msg);
        throw new IdmException(msg);
      }

      final AccessTokenResponse atResponse = response.toSuccessResponse();
      final AccessToken accessToken = atResponse.getTokens().getAccessToken();
      final Instant expiry = accessToken.getLifetime() != 0
          ? Instant.now().plusSeconds(accessToken.getLifetime())
          : null;
      final String authorizationHeader = accessToken.toAuthorizationHeader();

      return new BearerAccessTokenHolder(authorizationHeader, expiry);
    }
    catch (ParseException | JOSEException | IOException e) {
      final String msg = "Failed to obtain OAuth2 access token";
      log.warn("{}", msg, e);
      throw new IdmException(msg, e);
    }
  }

  private static String toString(final TokenErrorResponse response) {
    final ErrorObject error = response.getErrorObject();
    if (error == null) {
      return "Unknown OAuth2 error";
    }
    final StringBuffer sb = new StringBuffer("OAuth2 Error Response: ");
    sb.append("code=").append(Optional.ofNullable(error.getCode()).orElse("<not set>"));
    sb.append(", description=").append(Optional.ofNullable(error.getDescription()).orElse("<not set>"));

    return sb.toString();
  }

  private static SSLSocketFactory getSSLSocketFactory() {

    if (sslSocketFactory != null) {
      return sslSocketFactory;
    }

    try {
      final SSLContext sslContext = SSLContext.getInstance(TLSVersion.TLS_1_3.toString());

      final TrustManager[] trustAllCerts = {
          new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return null;
            }

            @Override
            public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
            }
          }
      };
      sslContext.init(null, trustAllCerts, null);

      sslSocketFactory = sslContext.getSocketFactory();
      return sslSocketFactory;
    }
    catch (final NoSuchAlgorithmException | KeyManagementException e) {
      throw new SecurityException(e);
    }
  }

}
