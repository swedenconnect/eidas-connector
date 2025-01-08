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
package se.swedenconnect.eidas.connector.authn.idm;

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
import se.swedenconnect.security.credential.nimbus.JwkTransformerFunction;

import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Abstract base implementation of the {@link OAuth2Handler} interface.
 *
 * @author Martin Lindström
 */
public abstract class AbstractOAuth2Handler implements OAuth2Handler {

  /** The cached access token for the HEAD calls. */
  private BearerAccessTokenHolder cachedAccessToken;

  /** The OAuth2 credential - used to sign OAuth2 items. */
  private final PkiCredential oauth2Credential;

  /** The JWK representation of the {@code oauth2Credential} including the signer and signing algorithm. */
  private final JwkInfo oauth2Jwk;

  /** The Connector OAuth2 client ID. */
  private final ClientID clientId;

  /** The OAuth2 scope(s) to use for HEAD calls. */
  private final Scope checkScope;

  /** The OAuth2 scope(s) to use for GET calls. */
  private final Scope getScope;

  /**
   * Holds information about a JWK.
   */
  protected record JwkInfo(JWK jwk, JWSSigner signer, JWSAlgorithm signingAlgorithm) {
  }

  /**
   * Constructor.
   *
   * @param clientId the Connector OAuth2 client ID
   * @param checkScopes the OAuth2 scope(s) to use when making HEAD requests
   * @param getScopes the OAuth2 scope(s) to use when making GET requests
   * @param oauth2Credential the credential used to sign the OAuth2 items
   */
  public AbstractOAuth2Handler(final String clientId, final List<String> checkScopes, final List<String> getScopes,
      final PkiCredential oauth2Credential) {
    this.clientId = new ClientID(Objects.requireNonNull(clientId, "clientId must not be null"));
    this.checkScope = Optional.ofNullable(Scope.parse(checkScopes))
        .orElseThrow(() -> new IllegalArgumentException("Missing checkScopes parameter"));
    this.getScope = Optional.ofNullable(Scope.parse(getScopes))
        .orElseThrow(() -> new IllegalArgumentException("Missing getScopes parameter"));
    this.oauth2Credential = Objects.requireNonNull(oauth2Credential, "oauth2Credential must not be null");
    this.oauth2Jwk = this.createJwk(this.oauth2Credential);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized String getCheckAccessToken() throws IdmException {

    final String header = Optional.ofNullable(this.cachedAccessToken)
        .filter(BearerAccessTokenHolder::isValid)
        .map(BearerAccessTokenHolder::getBearerAccessToken)
        .orElse(null);

    if (header != null) {
      return header;
    }
    this.cachedAccessToken = this.obtainAccessToken(this.clientId.getValue(), this.checkScope);

    return this.cachedAccessToken.getBearerAccessToken();
  }

  /** {@inheritDoc} */
  @Override
  public String getGetAccessToken(final String prid) throws IdmException {
    return this.obtainAccessToken(prid, this.getScope).getBearerAccessToken();
  }

  /**
   * Obtains an OAuth2 access token.
   *
   * @param subject the subject of the access token
   * @param scope the scope of the access token
   * @return the {@link BearerAccessTokenHolder}
   * @throws IdmException for OAuth2 errors
   */
  protected abstract BearerAccessTokenHolder obtainAccessToken(final String subject, final Scope scope)
      throws IdmException;

  /**
   * Gets the connector OAuth2 client ID.
   *
   * @return the client ID
   */
  //protected ClientID getClientId() {
  //    return this.clientId;
  //  }

  /**
   * Gets the OAuth2 scope(s) to use.
   *
   * @return the scope(s)
   */
  //  protected Scope getScope() {
  //    return this.scope;
  //  }

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
      final JwkTransformerFunction transformer = new JwkTransformerFunction();
      transformer.setAlgorithmFunction(c -> {
        if (c.getPublicKey() instanceof RSAPublicKey) {
          return JWSAlgorithm.RS256;
        }
        else if (c.getPublicKey() instanceof ECPublicKey) {
          return JWSAlgorithm.ES256;
        }
        else {
          return null;
        }
      });
      final JWK jwk = credential.transform(transformer);

      if (jwk instanceof final RSAKey rsaKey) {
        return new JwkInfo(jwk, new RSASSASigner(rsaKey), (JWSAlgorithm) jwk.getAlgorithm());
      }
      else if (jwk instanceof final ECKey ecKey) {
        return new JwkInfo(jwk, new ECDSASigner(ecKey), (JWSAlgorithm) jwk.getAlgorithm());
      }
      else {
        throw new SecurityException("Unsupported key type - " + credential.getPublicKey().getAlgorithm());
      }
    }
    catch (final JOSEException e) {
      throw new SecurityException("Failed to setup OAuth2 client JWK", e);
    }
  }

}
