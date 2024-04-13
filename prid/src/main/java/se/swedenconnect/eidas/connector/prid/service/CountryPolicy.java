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

/**
 * Country policy.
 *
 * @author Martin Lindström
 */
public class CountryPolicy {

  /** The algorithm. */
  @Setter
  @Getter
  private String algorithm;

  /** The persistence class. */
  @Getter
  private String persistenceClass;

  /**
   * Default constructor.
   */
  public CountryPolicy() {
  }

  /**
   * Constructor.
   *
   * @param algorithm the algorithm
   * @param persistenceClass the persistence class
   */
  public CountryPolicy(final String algorithm, final String persistenceClass) {
    this.algorithm = algorithm;
    this.setPersistenceClass(persistenceClass);
  }

  /**
   * Sets the persistence class.
   *
   * @param persistenceClass the persistence class
   */
  public void setPersistenceClass(final String persistenceClass) {
    if (persistenceClass != null) {
      this.persistenceClass = persistenceClass.toUpperCase();
    }
  }

}
