/*
 * Copyright 2023-2024 Sweden Connect
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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.eidas.connector.prid.service.CountryPolicy;
import se.swedenconnect.eidas.connector.prid.service.PridService;
import se.swedenconnect.eidas.connector.prid.service.PridService.PridPolicyValidation;

/**
 * Endpoint for policy refresh.
 *
 * @author Martin Lindström
 */
@Component
@Endpoint(id = "refreshprid")
@Slf4j
public class PridPolicyRefreshEndpoint {

  /** The PRID service. */
  private final PridService pridService;

  /**
   * Constructor.
   *
   * @param pridService the PRID service
   */
  public PridPolicyRefreshEndpoint(final PridService pridService) {
    this.pridService = Objects.requireNonNull(pridService, "pridService must not be null");
  }

  /**
   * Endpoint that performs an update of the PRID policy configuration.
   *
   * @return the refresh status
   */
  @ReadOperation
  public RefreshStatus refresh() {
    log.debug("Request to refresh PRID policy configuration received");
    final PridPolicyValidation result = this.pridService.updatePolicy();
    final RefreshStatus status = new RefreshStatus(result);
    log.debug("PRID policy refresh status: {}", status);
    if (!result.hasErrors()) {
      status.setPolicy(this.pridService.getPolicy().getPolicy());
    }
    return status;
  }

  /**
   * Represents the status of the refresh operation.
   */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @NoArgsConstructor
  @Data
  @ToString
  public class RefreshStatus {

    /** The overall status string. */
    private String status;

    /** Validation errors. */
    private List<String> errors;

    /** The policy. */
    private Map<String, CountryPolicy> policy;

    /**
     * Constructor setting up the status based on a validation result.
     *
     * @param validationResult validation result
     */
    public RefreshStatus(final PridPolicyValidation validationResult) {
      if (validationResult.hasErrors()) {
        this.status = "ERROR";
        this.errors = validationResult.getErrors();
      }
      else {
        this.status = "OK";
      }
    }
  }

}
