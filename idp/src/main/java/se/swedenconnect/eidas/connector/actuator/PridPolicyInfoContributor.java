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

import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import se.swedenconnect.eidas.connector.prid.service.PridPolicy;
import se.swedenconnect.eidas.connector.prid.service.PridService;

import java.util.Objects;
import java.util.Optional;

/**
 * Info contributor displaying the PRID policy configuration.
 *
 * @author Martin Lindström
 */
@Component
public class PridPolicyInfoContributor implements InfoContributor {

  /** The PRID service. */
  private final PridService pridService;

  /**
   * Constructor.
   *
   * @param pridService the PRID service
   */
  public PridPolicyInfoContributor(final PridService pridService) {
    this.pridService =
        Objects.requireNonNull(pridService, "pridService must not be null");
  }

  /**
   * Adds the PRID policy configuration to the information released by the Spring Boot actuator info-endpoint.
   */
  @Override
  public void contribute(final Builder builder) {
    final PridPolicy policy = this.pridService.getPolicy();
    Optional.ofNullable(policy.getPolicy())
        .ifPresent(p -> builder.withDetail("prid-policy", p));
  }

}
