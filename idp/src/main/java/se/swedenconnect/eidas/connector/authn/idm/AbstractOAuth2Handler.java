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

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;

import se.swedenconnect.security.credential.PkiCredential;

/**
 * Abstract base implementation of the {@link OAuth2Handler} interface.
 *
 * @author Martin Lindström
 */
public abstract class AbstractOAuth2Handler implements OAuth2Handler {

  /** The cached access token. */
  private BearerAccessTokenHolder cachedAccessToken;

  /** The OAuth2 credential - used to sign OAuth2 items. */
  private final PkiCredential oauth2Credential;

  /** The JWK representation of the {@code oauth2Credential} including the signer and signing algorithm. */
  private final JwkInfo oauth2Jwk;

  /** The Connector OAuth2 client ID. */
  private final ClientID clientId;

  /** The OAuth2 scope(s) to use. */
  private final Scope scope;

  /**
   * Holds information about a JWK.
   */
  protected static record JwkInfo(JWK jwk, JWSSigner signer, JWSAlgorithm signingAlgorithm) {
  }

  /**
   * Constructor.
   *
   * @param clientId the Connector OAuth2 client ID
   * @param scopes the OAuth2 scope(s) to use
   * @param oauth2Credential the credential used to sign the OAuth2 items
   */
  public AbstractOAuth2Handler(
      final String clientId,
      final List<String> scopes,
      final PkiCredential oauth2Credential) {
    this.clientId = new ClientID(Objects.requireNonNull(clientId, "clientId must not be null"));
    this.scope = Optional.ofNullable(Scope.parse(scopes))
        .orElseThrow(() -> new IllegalArgumentException("Missing scopes parameter"));
    this.oauth2Credential = Objects.requireNonNull(oauth2Credential, "oauth2Credential must not be null");
    this.oauth2Jwk = this.createJwk(this.oauth2Credential);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized String getAccessToken() throws IdmException {

    final String header = Optional.ofNullable(this.cachedAccessToken)
        .filter(BearerAccessTokenHolder::isValid)
        .map(BearerAccessTokenHolder::getBearerAccessToken)
        .orElse(null);

    if (header != null) {
      return header;
    }
    this.cachedAccessToken = this.obtainAccessToken();

    return this.cachedAccessToken.getBearerAccessToken();
  }

  /**
   * Obtains an OAuth2 access token.
   *
   * @return the {@link BearerAccessTokenHolder}
   * @throws IdmException for OAuth2 errors
   */
  protected abstract BearerAccessTokenHolder obtainAccessToken() throws IdmException;

  /**
   * Gets the connector OAuth2 client ID.
   *
   * @return the client ID
   */
  protected ClientID getClientId() {
    return this.clientId;
  }

  /**
   * Gets the OAuth2 scope(s) to use.
   *
   * @return the scope(s)
   */
  protected Scope getScope() {
    return this.scope;
  }

  /**
   * Gets the OAuth2 credential - used to sign OAuth2 items.
   *
   * @return the {@link PkiCredential}
   */
  protected PkiCredential getOauth2Credential() {
    return this.oauth2Credential;
  }

  /**
   * Gets the JWK representation of the {@code oauth2Credential} including the signer and signing algorithm.
   *
   * @return the {@link JwkInfo}
   */
  protected JwkInfo getOauth2Jwk() {
    return this.oauth2Jwk;
  }

  /**
   * Based on a {@link PkiCredential} a {@link JWK}, {@link JWSSigner} and {@link JWSAlgorithm} is created.
   *
   * @param credential the {@link PkiCredential}
   * @return a {@link JwkInfo}
   */
  protected JwkInfo createJwk(final PkiCredential credential) {
    try {
      if ("RSA".equals(credential.getPublicKey().getAlgorithm())) {
        final JWK jwk = new RSAKey.Builder(RSAPublicKey.class.cast(credential.getPublicKey()))
            .privateKey(credential.getPrivateKey())
            .keyIDFromThumbprint()
            .algorithm(JWSAlgorithm.RS256)
            .build();
        return new JwkInfo(jwk, new RSASSASigner((RSAKey) jwk), JWSAlgorithm.RS256);
      }
      else if ("EC".equals(credential.getPublicKey().getAlgorithm())) {
        final JWK jwk = new ECKey.Builder(ECKey.parse(credential.getCertificate()))
            .privateKey(credential.getPrivateKey())
            .keyIDFromThumbprint()
            .algorithm(JWSAlgorithm.ES256)
            .build();
        return new JwkInfo(jwk, new ECDSASigner((ECKey) jwk), JWSAlgorithm.ES256);
      }
      else {
        throw new SecurityException(
            "Unsupported key type - " + credential.getPublicKey().getAlgorithm());
      }
    }
    catch (final JOSEException e) {
      throw new SecurityException("Failed to setup OAuth2 client JWK", e);
    }
  }

}
