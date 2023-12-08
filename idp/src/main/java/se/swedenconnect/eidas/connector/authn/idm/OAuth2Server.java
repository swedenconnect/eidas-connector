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

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.security.credential.PkiCredential;

/**
 * An implementation of the {@link OAuth2Handler} interface where the connector itself acts as an OAuth2 Authorization
 * Server and issues access token to itself for use when invoking the Identity Matching service API:s.
 *
 * @author Martin Lindström
 */
@Slf4j
public class OAuth2Server extends AbstractOAuth2Handler {

  /** The issuer of the access tokens. */
  private final String issuer;

  /** The intended audience for the issued access tokens. */
  private final String audience;

  /** The duration for issued access tokens. */
  @Setter
  private Duration lifeTime = Duration.ofSeconds(3600);

  /**
   * Constructor.
   *
   * @param clientId the Connector OAuth2 client ID
   * @param scopes the OAuth2 scope(s) to use
   * @param issuer the issuer of the access tokens
   * @param audience the audience of the access tokens
   * @param oauth2Credential the credential used to sign the OAuth2 items
   */
  public OAuth2Server(final String clientId, final List<String> scopes, final String issuer,
      final String audience, final PkiCredential oauth2Credential) {
    super(clientId, scopes, oauth2Credential);
    this.issuer = Objects.requireNonNull(issuer, "issuer must not be null");
    this.audience = Objects.requireNonNull(audience, "audience must not be null");
  }

  /** {@inheritDoc} */
  @Override
  protected BearerAccessTokenHolder obtainAccessToken() throws IdmException {

    try {
      final Instant now = Instant.now();
      final Instant expires = now.plus(this.lifeTime);

      final JWTClaimsSet jwt = new JWTClaimsSet.Builder()
          .issuer(this.issuer)
          .subject(this.getClientId().getValue())
          .audience(this.audience)
          .expirationTime(Date.from(expires))
          .notBeforeTime(Date.from(now.minusSeconds(10)))
          .issueTime(Date.from(now))
          .jwtID(UUID.randomUUID().toString())
          .claim("client_id", this.getClientId().getValue())
          .build();

      log.debug("Issuing access token: {}", jwt);

      final SignedJWT signedJwt = new SignedJWT(
          new JWSHeader.Builder(this.getOauth2Jwk().signingAlgorithm())
              .keyID(this.getOauth2Jwk().jwk().getKeyID())
              .build(),
          jwt);

      signedJwt.sign(this.getOauth2Jwk().signer());

      return new BearerAccessTokenHolder("Bearer " + signedJwt.serialize(), expires);
    }
    catch (final JOSEException e) {
      throw new IdmException("eIDAS Connector Authorization Server failed to issue Access Token", e);
    }
  }

}
