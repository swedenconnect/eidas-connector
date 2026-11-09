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
package se.swedenconnect.eidas.connector.events;

import org.jspecify.annotations.NonNull;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.spring.authnserver.authentication.provider.UserAuthenticationInputToken;

import java.io.Serial;
import java.util.Objects;
import java.util.Optional;

/**
 * Abstract base class for eIDAS authentication events.
 *
 * @author Martin Lindström
 */
public class AbstractConnectorAuthnEvent extends AbstractEidasConnectorEvent {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** Used when the SP entityID or the ID of the authentication request is not known. */
  public static final @NonNull String UNKNOWN = "unknown";

  /**
   * Constructor.
   *
   * @param token the authentication input token
   */
  public AbstractConnectorAuthnEvent(final @NonNull UserAuthenticationInputToken token) {
    super(Objects.requireNonNull(token, "token must not be null"));
  }

  /**
   * Gets the input token.
   *
   * @return the {@link UserAuthenticationInputToken}
   */
  public final @NonNull UserAuthenticationInputToken getToken() {
    return (UserAuthenticationInputToken) this.getSource();
  }

  /**
   * Gets the original SP entityID.
   *
   * @return the entityID of the Swedish SP that requested authentication
   */
  public final @NonNull String getOriginalSpId() {
    return this.getToken().getRequester().identifier();
  }

  /**
   * Gets the ID of the original {@code AuthnRequest}, i.e., the request sent by the Swedish SP.
   *
   * @return the ID of the original {@code AuthnRequest}
   */
  public final @NonNull String getOriginalAuthnRequestId() {
    return Optional.ofNullable(this.getToken().getRequestId()).orElse(UNKNOWN);
  }

}
