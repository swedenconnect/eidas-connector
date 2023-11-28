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
package se.swedenconnect.eidas.connector.prid.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.eidas.connector.prid.generator.PridGenerator;
import se.swedenconnect.eidas.connector.prid.generator.PridGeneratorException;

/**
 * The PRID service that knows how to generate PRID attributes based on the PRID policy configuration.
 */
@Slf4j
public class PridService implements InitializingBean {

  /** The resource holding the PRID policy configuration. */
  private final Resource policyResource;

  /** The installed PRID-generators. */
  private final List<PridGenerator> pridGenerators;

  /** The actual policy. */
  private PridPolicy policy;

  /** The latest PRID policy validation result. */
  private PridPolicyValidation latestValidationResult;

  /**
   * Constructor.
   *
   * @param policyConfiguration the resource holding the PRID policy configuration
   * @param pridGenerators the installed PRID-generators. Used to validate algorithms from config
   */
  public PridService(final Resource policyConfiguration,
      final List<PridGenerator> pridGenerators) {
    this.policyResource = Objects.requireNonNull(policyConfiguration, "policyConfiguration must not be null");
    this.pridGenerators = Objects.requireNonNull(pridGenerators, "pridGenerators must not be null");
  }

  /**
   * Generates a PRID for the supplied ID and country.
   *
   * @param id the eIDAS person identifier to base the PRID on
   * @param country the issuing country
   * @return a response holding the generated PRID and its persistence class
   * @throws PridGeneratorException for generation errors
   * @throws CountryPolicyNotFoundException if a country is not supported
   */
  public PridResult generatePrid(final String id, final String country)
      throws PridGeneratorException, CountryPolicyNotFoundException {

    final CountryPolicy policy = Optional.ofNullable(this.getPolicy(country))
        .orElseThrow(() -> new CountryPolicyNotFoundException(
            "Country '%s' is not supported by the PRID service".formatted(country)));

    final PridGenerator pridGenerator = this.pridGenerators.stream()
        .filter(p -> p.getAlgorithmName().equalsIgnoreCase(policy.getAlgorithm()))
        .findFirst()
        .orElseThrow(() -> new PridGeneratorException(
            "No matching PRID generator for algorithm '%s'".formatted(policy.getAlgorithm())));

    final PridResult result =
        new PridResult(pridGenerator.getPridIdentifierComponent(id, country), policy.getPersistenceClass());

    log.debug("PRID service returning '{}' for id: '{}' and country '{}'", result, id, country);

    return result;
  }

  /**
   * Returns the policy for the given country.
   *
   * @param countryCode the country code
   * @return the policy, or {@code null} if the country has not been configured
   */
  public CountryPolicy getPolicy(final String countryCode) {
    return this.getPolicy().getPolicy(countryCode);
  }

  /**
   * Returns the current policy.
   *
   * @return PRID policy
   */
  public synchronized PridPolicy getPolicy() {
    return this.policy;
  }

  /**
   * Returns the latest validation result.
   *
   * @return validation result
   */
  public PridPolicyValidation getLatestValidationResult() {
    return this.latestValidationResult;
  }

  /**
   * Updates the PRID policy configuration by re-loading the policy file.
   *
   * @return the validation result for the updated policy
   */
  @Scheduled(timeUnit = TimeUnit.SECONDS, initialDelayString = "${connector.prid.update-interval:600}", fixedRateString = "${connector.prid.update-interval:600}")
  public PridPolicyValidation updatePolicy() {

    log.debug("Updating PRID policy configuration ...");

    final PridPolicyValidation validation = new PridPolicyValidation();
    try {
      final PridPolicy newPolicy = this.loadPolicy();
      final PridPolicy updatedPolicy = this.validate(newPolicy, validation);
      synchronized (this.policy) {
        this.policy = updatedPolicy;
        this.latestValidationResult = validation;
      }
      log.debug("PRID policy configuration was updated");
    }
    catch (final BindException e) {
      final String msg = "Failed to update PRID policy - invalid format";
      log.error(msg, e);
      validation.addError(msg);
      this.latestValidationResult = validation;
    }
    catch (final IOException e) {
      final String msg = "Failed to read PRID policy file - " + e.getMessage();
      log.error(msg, e);
      validation.addError(msg);
      this.latestValidationResult = validation;
    }
    return validation;
  }

  /**
   * Loads the PRID policy from the policy resource file.
   *
   * @return a {@link PridPolicy} element.
   * @throws IOException for errors reading the policy file
   * @throws BindException for format errors when creating a policy object
   */
  protected PridPolicy loadPolicy() throws IOException, BindException {

    // Get hold of the properties file holding the config ...
    //
    log.debug("Loading policy file '{}' ...", this.policyResource);
    final PropertiesFactoryBean pfactory = new PropertiesFactoryBean();
    pfactory.setSingleton(true);
    pfactory.setLocation(this.policyResource);
    pfactory.afterPropertiesSet();

    final ConfigurationPropertySource propertySource = new MapConfigurationPropertySource(
        pfactory.getObject()
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(e -> e.getKey(), e -> e.getValue())));

    final Binder binder = new Binder(propertySource);
    final BindResult<PridPolicy> result = binder.bind("", Bindable.of(PridPolicy.class));
    PridPolicy pridPolicy;
    if (result.isBound()) {
      pridPolicy = result.get();
      log.debug("Successfully loaded policy: {}", pridPolicy);
    }
    else {
      pridPolicy = new PridPolicy();
      log.warn("Empty policy loaded");
    }
    return pridPolicy;
  }

  /**
   * Validates a PRID policy and filters out the invalid entries.
   *
   * @param policy the policy to validate
   * @param validationResult the validation result (will be updated with errors)
   * @return a policy object with only correct entries
   */
  protected PridPolicy validate(final PridPolicy policy, final PridPolicyValidation validationResult) {
    log.debug("Validating policy ...");

    if (policy.getPolicy() == null || policy.isEmpty()) {
      validationResult.addError("Empty PRID policy");
      return policy;
    }

    final Map<String, CountryPolicy> updatedPolicy = new HashMap<>();

    for (final Map.Entry<String, CountryPolicy> p : policy.getPolicy().entrySet()) {
      final List<String> result = new ArrayList<>();
      if (p.getKey() != null && !p.getKey().matches("[A-Za-z][A-Za-z]")) {
        result.add("Invalid country code: " + p.getKey());
      }
      if (p.getValue().getAlgorithm() == null) {
        result.add(String.format("Invalid entry - Missing 'algorithm' for country %s", p.getKey()));
      }
      else if (!this.isValidAlgorithm(p.getValue().getAlgorithm())) {
        result.add(String.format("Invalid algorithm (%s) for country %s", p.getValue().getAlgorithm(), p.getKey()));
      }

      if (p.getValue().getPersistenceClass() == null) {
        result.add(String.format("Invalid entry - Missing 'persistenceClass' for country %s", p.getKey()));
      }
      if (!p.getValue().getPersistenceClass().matches("A|B|C")) {
        result.add(String.format(
            "Invalid entry - Bad value for 'persistenceClass' for country %s (%s) - A, B or C is required",
            p.getKey(), p.getValue().getPersistenceClass()));
      }
      if (result.isEmpty()) {
        updatedPolicy.put(p.getKey().toUpperCase(), p.getValue());
      }
      else {
        log.warn("Validation errors for country '{}' - will not be added - {}", p.getKey(), result);
        validationResult.addErrors(result);
      }
    }

    if (validationResult.hasErrors()) {
      log.warn("Policy validation reported errors - {}", validationResult.getErrors());
    }
    else {
      log.debug("Policy validation was successful");
    }
    return new PridPolicy(updatedPolicy);
  }

  /**
   * Predicate that tests if the supplied algorithm is valid given the installed PRID generators.
   *
   * @param algorithm the algorithm to test
   * @return {@code true} if the algorithm is valid and {@code false} otherwise
   */
  protected boolean isValidAlgorithm(final String algorithm) {
    return this.pridGenerators.stream()
        .filter(p -> p.getAlgorithmName().equalsIgnoreCase(algorithm))
        .findFirst()
        .isPresent();
  }

  /**
   * Loads the policy for the first time.
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    final PridPolicy pridPolicy = this.loadPolicy();
    this.latestValidationResult = new PridPolicyValidation();
    this.policy = this.validate(pridPolicy, this.latestValidationResult);
    if (this.latestValidationResult.hasErrors()) {
      throw new IllegalArgumentException(
          String.format("PRID policy error - %s", this.latestValidationResult.getErrors()));
    }
  }

  /**
   * Represents a validation result for a PRID policy.
   */
  @ToString
  public static class PridPolicyValidation {

    /** The validation errors. */
    private final List<String> errors;

    /**
     * Constructor.
     */
    public PridPolicyValidation() {
      this.errors = new ArrayList<>();
    }

    /**
     * Adds an error message.
     *
     * @param msg the error message
     */
    public void addError(final String msg) {
      this.errors.add(msg);
    }

    /**
     * Adds error messages.
     *
     * @param msgs the messages to add
     */
    public void addErrors(final List<String> msgs) {
      this.errors.addAll(msgs);
    }

    /**
     * Returns the error messages.
     *
     * @return the error messages
     */
    public List<String> getErrors() {
      return this.errors;
    }

    /**
     * Predicate that tells whether this validation object contains any errors.
     *
     * @return {@code true} if the validation object contains errors, and {@code false} otherwise
     */
    public boolean hasErrors() {
      return !this.errors.isEmpty();
    }
  }

}
