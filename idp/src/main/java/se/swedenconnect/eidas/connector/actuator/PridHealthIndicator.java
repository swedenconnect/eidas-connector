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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import se.swedenconnect.eidas.connector.authn.metadata.CountryMetadata;
import se.swedenconnect.eidas.connector.authn.metadata.EuMetadataProvider;
import se.swedenconnect.eidas.connector.prid.service.PridPolicy;
import se.swedenconnect.eidas.connector.prid.service.PridService;
import se.swedenconnect.eidas.connector.prid.service.PridService.PridPolicyValidation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * PRID service health indicator bean.
 *
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
@Component("prid")
@Slf4j
public class PridHealthIndicator implements HealthIndicator {

  /** The PRID service. */
  private final PridService pridService;

  /** The EU metadata. */
  private final EuMetadataProvider euMetadata;

  /**
   * Constructor.
   *
   * @param pridService the PRID service
   * @param euMetadata the EU metadata
   */
  public PridHealthIndicator(final PridService pridService, final EuMetadataProvider euMetadata) {
    this.pridService = Objects.requireNonNull(pridService, "pridService must not be null");
    this.euMetadata = Objects.requireNonNull(euMetadata, "euMetadata must not be null");
  }

  /**
   * Calculates the health status of the PRID service.
   */
  @Override
  public Health health() {

    final PridPolicy pridPolicy = this.pridService.getPolicy();

    if (pridPolicy == null || pridPolicy.isEmpty()) {
      log.warn("Health: PRID service policy configuration is empty");
      return Health
          .outOfService()
          .withDetail("error-message", "PRID service policy configuration is empty")
          .build();
    }

    final Health.Builder builder = new Health.Builder();
    builder.up();

    // If a new country has been introduced in the metadata service list that we do not have an
    // algorithm for we issue a warning.
    //
    final List<CountryMetadata> countries = this.euMetadata.getAllCountries();
    final List<String> noPridConfig = countries.stream()
        .map(CountryMetadata::getCountryCode)
        .filter(countryCode -> this.pridService.getPolicy(countryCode) == null)
        .toList();
    if (noPridConfig.isEmpty()) {
      builder.withDetail("prid-policy-status", "ok");
    }
    else {
      log.warn("Health: Missing PRID policy configuration for: {}", noPridConfig);
      builder.status(CustomStatus.WARNING)
          .withDetail("prid-policy-status", Map.of("missing-prid-config", noPridConfig));
    }

    // Any warnings from the PRID policy configuration?
    //
    final PridPolicyValidation pridValidation = this.pridService.getLatestValidationResult();
    if (pridValidation.hasErrors()) {
      log.warn("Health: PRID policy validation errors: {}", pridValidation.getErrors());
      builder.status(CustomStatus.WARNING).withDetail("config-validation", pridValidation.getErrors());
    }

    return builder.build();
  }

}
