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
package se.swedenconnect.eidas.connector.actuator;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;

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
public class PridPolicyRefreshEndpoint {

  /** Logger instance. */
  private static final Logger log = LoggerFactory.getLogger(PridPolicyRefreshEndpoint.class);

  /** The PRID service. */
  private final @NonNull PridService pridService;

  /**
   * Constructor.
   *
   * @param pridService the PRID service
   */
  public PridPolicyRefreshEndpoint(final @NonNull PridService pridService) {
    this.pridService = Objects.requireNonNull(pridService, "pridService must not be null");
  }

  /**
   * Endpoint that performs an update of the PRID policy configuration.
   *
   * @return the refresh status
   */
  @ReadOperation
  public @NonNull RefreshStatus refresh() {
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
  public static class RefreshStatus {

    /** The overall status string. */
    private @Nullable String status;

    /** Validation errors. */
    private @Nullable List<String> errors;

    /** The policy. */
    private @Nullable Map<String, CountryPolicy> policy;

    /**
     * Default constructor.
     */
    public RefreshStatus() {
    }

    /**
     * Constructor setting up the status based on a validation result.
     *
     * @param validationResult validation result
     */
    public RefreshStatus(final @NonNull PridPolicyValidation validationResult) {
      if (validationResult.hasErrors()) {
        this.status = "ERROR";
        this.errors = validationResult.getErrors();
      }
      else {
        this.status = "OK";
      }
    }

    /**
     * Gets the overall status string.
     *
     * @return the overall status string
     */
    public @Nullable String getStatus() {
      return this.status;
    }

    /**
     * Assigns the overall status string.
     *
     * @param status the overall status string
     */
    public void setStatus(final @Nullable String status) {
      this.status = status;
    }

    /**
     * Gets the validation errors.
     *
     * @return the validation errors
     */
    public @Nullable List<String> getErrors() {
      return this.errors;
    }

    /**
     * Assigns the validation errors.
     *
     * @param errors the validation errors
     */
    public void setErrors(final @Nullable List<String> errors) {
      this.errors = errors;
    }

    /**
     * Gets the policy.
     *
     * @return the policy
     */
    public @Nullable Map<String, CountryPolicy> getPolicy() {
      return this.policy;
    }

    /**
     * Assigns the policy.
     *
     * @param policy the policy
     */
    public void setPolicy(final @Nullable Map<String, CountryPolicy> policy) {
      this.policy = policy;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final @Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof final RefreshStatus that)) {
        return false;
      }
      return Objects.equals(this.status, that.status) && Objects.equals(this.errors, that.errors)
          && Objects.equals(this.policy, that.policy);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return Objects.hash(this.status, this.errors, this.policy);
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull String toString() {
      return "status=" + this.status + ", errors=" + this.errors
          + ", policy=" + this.policy;
    }
  }

}
