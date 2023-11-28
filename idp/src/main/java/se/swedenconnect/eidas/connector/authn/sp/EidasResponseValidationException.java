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
package se.swedenconnect.eidas.connector.authn.sp;

import java.util.Objects;

import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.opensaml.saml2.response.validation.ResponseValidationException;
import se.swedenconnect.spring.saml.idp.error.Saml2ErrorStatusException;

/**
 * Exception class that represents errors from validating the eIDAS response.
 *
 * @author Martin Lindström
 */
public class EidasResponseValidationException extends ResponseValidationException {

  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  private final Saml2ErrorStatusException error;

  /**
   * Constructor.
   *
   * @param error the error to report back to the Swedish SP
   */
  public EidasResponseValidationException(final Saml2ErrorStatusException error) {
    super(error.getMessage(), error.getCause());
    this.error = Objects.requireNonNull(error, "error must not be null");
  }

  /**
   * Gets the error to report back to the Swedish SP.
   *
   * @return a {@link Saml2ErrorStatusException}
   */
  public Saml2ErrorStatusException getError() {
    return this.error;
  }

}
