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

import java.time.Instant;

/**
 * An Access Token holder.
 *
 * @author Martin Lindström
 */
public class BearerAccessTokenHolder {

  /**
   * The serialized access token (including the "Bearer " prefix).
   */
  private final String accessToken;

  /**
   * The expiration time.
   */
  private final Instant expires;

  /**
   * Constructor.
   *
   * @param authorizationHeader the authorization header (containing the OAuth2 access token)
   * @param expires the expiration time
   */
  public BearerAccessTokenHolder(final String authorizationHeader, final Instant expires) {
    this.accessToken = authorizationHeader;
    this.expires = expires;
  }

  /**
   * Gets the serialized access token including the "Bearer "-prefix.
   *
   * @return the bearer access token
   */
  public String getBearerAccessToken() {
    return this.accessToken;
  }

  /**
   * Whether the access token still is valid.
   *
   * @return {@code true} if the token still is valid and {@code false} otherwise
   */
  public boolean isValid() {
    return this.expires != null && Instant.now().plusSeconds(10).isBefore(this.expires);
  }

}
