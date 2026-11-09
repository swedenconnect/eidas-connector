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

import org.springframework.security.authentication.AbstractAuthenticationToken;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.spring.authnserver.saml.error.SamlErrorStatusException;

import java.io.Serial;
import java.util.Collections;
import java.util.Objects;

/**
 * The result of an authentication that failed with a SAML error that the authentication server has no
 * {@code AuthenticationError} for. The controller delivers it to the resume path, where the provider throws the error,
 * so that the Swedish Service Provider gets the same status as with earlier versions of the connector.
 *
 * @author Martin Lindström
 */
public class EidasAuthenticationErrorToken extends AbstractAuthenticationToken {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The error to report to the Swedish Service Provider. */
  private final SamlErrorStatusException error;

  /**
   * Constructor.
   *
   * @param error the error to report to the Swedish Service Provider
   */
  public EidasAuthenticationErrorToken(final SamlErrorStatusException error) {
    super(Collections.emptyList());
    this.error = Objects.requireNonNull(error, "error must not be null");
  }

  /**
   * Gets the error to report to the Swedish Service Provider.
   *
   * @return a {@link SamlErrorStatusException}
   */
  public SamlErrorStatusException getError() {
    return this.error;
  }

  /**
   * Returns the description of the error.
   */
  @Override
  public Object getPrincipal() {
    return this.error.getDescription();
  }

  /**
   * Returns an empty string.
   */
  @Override
  public Object getCredentials() {
    return "";
  }

}
