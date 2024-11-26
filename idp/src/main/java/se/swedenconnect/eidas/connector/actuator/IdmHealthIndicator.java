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
package se.swedenconnect.eidas.connector.actuator;

import jakarta.annotation.Nonnull;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import se.swedenconnect.eidas.connector.authn.idm.IdmClient;
import se.swedenconnect.eidas.connector.authn.idm.IdmException;

import java.util.Optional;

/**
 * {@link HealthIndicator} for checking the connectivity against the Identity Matching API.
 *
 * @author Martin Lindström
 */
@Component("idm")
public class IdmHealthIndicator implements HealthIndicator {

  private final IdmClient idmClient;

  /**
   * Constructor assigning the {@link IdmClient}.
   *
   * @param idmClient the IDM client
   */
  public IdmHealthIndicator(@Nonnull final IdmClient idmClient) {
    this.idmClient = idmClient;
  }

  /**
   * Checks that the IDM API is responsive.
   */
  @Override
  public Health health() {
    if (!this.idmClient.isActive()) {
      return null;
    }
    try {
      this.idmClient.ping();
      return Health.up().build();
    }
    catch (final IdmException e) {
      return Health
          .status(CustomStatus.WARNING)
          .withDetail("error-message", e.getMessage())
          .withDetail("exception", Optional.ofNullable(e.getCause())
              .orElse(e)
              .getClass()
              .getName())
          .build();

    }
  }

}
