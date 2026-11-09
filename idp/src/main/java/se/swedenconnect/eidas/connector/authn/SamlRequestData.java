/*
 * Copyright 2017-2026 Sweden Connect
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
package se.swedenconnect.eidas.connector.authn;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import se.swedenconnect.spring.authnserver.authentication.provider.UserAuthenticationInputToken;
import se.swedenconnect.spring.authnserver.error.CommonUnrecoverableError;
import se.swedenconnect.spring.authnserver.error.UnrecoverableErrorException;
import se.swedenconnect.spring.authnserver.saml.authnrequest.Saml2AuthnRequestData;

/**
 * Gives the SAML data of the authentication request from the Swedish Service Provider.
 *
 * @author Martin Lindström
 */
public final class SamlRequestData {

  /**
   * Gets the SAML data of the authentication request.
   *
   * @param token the input token
   * @return the {@link Saml2AuthnRequestData}
   * @throws UnrecoverableErrorException if the token holds no SAML data
   */
  public static @NonNull Saml2AuthnRequestData get(final @NonNull UserAuthenticationInputToken token)
      throws UnrecoverableErrorException {
    if (token.getProtocolRequestData() instanceof final Saml2AuthnRequestData data) {
      return data;
    }
    throw new UnrecoverableErrorException(CommonUnrecoverableError.INTERNAL,
        "No SAML authentication request available");
  }

  /**
   * Gets the authentication request from the Swedish Service Provider.
   *
   * @param token the input token
   * @return the {@link AuthnRequest}
   * @throws UnrecoverableErrorException if the token holds no SAML data
   */
  public static @NonNull AuthnRequest getAuthnRequest(final @NonNull UserAuthenticationInputToken token)
      throws UnrecoverableErrorException {
    return get(token).getAuthnRequest();
  }

  /**
   * Gets the metadata of the Swedish Service Provider.
   *
   * @param token the input token
   * @return the SP metadata
   * @throws UnrecoverableErrorException if the token holds no SAML data
   */
  public static @NonNull EntityDescriptor getSpMetadata(final @NonNull UserAuthenticationInputToken token)
      throws UnrecoverableErrorException {
    return get(token).responseAttributes().getPeerMetadata();
  }

  /**
   * Gets the relay state of the authentication request from the Swedish Service Provider.
   *
   * @param token the input token
   * @return the relay state, or {@code null}
   * @throws UnrecoverableErrorException if the token holds no SAML data
   */
  public static @Nullable String getRelayState(final @NonNull UserAuthenticationInputToken token)
      throws UnrecoverableErrorException {
    return get(token).responseAttributes().relayState();
  }

  // Hidden constructor
  private SamlRequestData() {
  }

}
