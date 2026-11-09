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
package se.swedenconnect.eidas.connector.prid.service;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Represents a PRID policy.
 *
 * @author Martin Lindström
 */
public class PridPolicy {

  /** The policy map. */
  private @Nullable Map<String, CountryPolicy> policy;

  /**
   * Default constructor.
   */
  public PridPolicy() {
  }

  /**
   * Constructor assigning the policy.
   *
   * @param policy the policy
   */
  public PridPolicy(final @Nullable Map<String, CountryPolicy> policy) {
    this.policy = policy;
  }

  /**
   * Gets the policy map.
   *
   * @return the policy map
   */
  public @Nullable Map<String, CountryPolicy> getPolicy() {
    return this.policy;
  }

  /**
   * Assigns the policy map.
   *
   * @param policy the policy map
   */
  public void setPolicy(final @Nullable Map<String, CountryPolicy> policy) {
    this.policy = policy;
  }

  /**
   * Returns the policy for a given country.
   *
   * @param countryCode the country code
   * @return the policy for the given country, or {@code null} if no match is found
   */
  public @Nullable CountryPolicy getPolicy(final @NonNull String countryCode) {
    return this.policy.get(countryCode);
  }

  /**
   * Predicate that tells if there are any polices stored.
   *
   * @return {@code true} if no policies are stored and {@code false} otherwise
   */
  public boolean isEmpty() {
    return this.policy == null || this.policy.isEmpty();
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull String toString() {
    return "policy=" + this.getPolicy();
  }

}
