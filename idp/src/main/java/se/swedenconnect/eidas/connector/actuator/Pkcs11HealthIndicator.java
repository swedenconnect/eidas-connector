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
package se.swedenconnect.eidas.connector.actuator;

import java.util.Objects;

import org.opensaml.security.crypto.SigningUtil;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.eidas.connector.config.ConnectorCredentials;
import se.swedenconnect.security.credential.PkiCredential;

/**
 * A {@link HealthIndicator} that is active if XXX
 *
 * @author Martin Lindström
 */
@Component("credential")
@Slf4j
public class Pkcs11HealthIndicator implements HealthIndicator {

  private PkiCredential testCredential;

  public Pkcs11HealthIndicator(final ConnectorCredentials credentials) {
    this.testCredential = Objects.requireNonNull(credentials, "credentials must not be null")
        .getIdpSigningCredential();
  }

  /** {@inheritDoc} */
  @Override
  public Health health() {
    if (this.testCredential.isHardwareCredential()) {
      return null;
    }
    else {
      return null;
    }
  }

}
