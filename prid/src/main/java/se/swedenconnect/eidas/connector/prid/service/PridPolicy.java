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
package se.swedenconnect.eidas.connector.prid.service;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 * Represents a PRID policy.
 *
 * @author Martin Lindström
 */
@ToString
public class PridPolicy {

  /** The policy map. */
  @Getter
  @Setter
  private Map<String, CountryPolicy> policy;

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
  public PridPolicy(final Map<String, CountryPolicy> policy) {
    this.policy = policy;
  }

  /**
   * Returns the policy for a given country.
   *
   * @param countryCode the country code
   * @return the policy for the given country, or {@code null} if no match is found
   */
  public CountryPolicy getPolicy(final String countryCode) {
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

}
