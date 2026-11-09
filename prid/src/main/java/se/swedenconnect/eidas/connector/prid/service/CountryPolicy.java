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

/**
 * Country policy.
 *
 * @author Martin Lindström
 */
public class CountryPolicy {

  /** The algorithm. */
  private String algorithm;

  /** The persistence class. */
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
   * Gets the algorithm.
   *
   * @return the algorithm
   */
  public String getAlgorithm() {
    return this.algorithm;
  }

  /**
   * Assigns the algorithm.
   *
   * @param algorithm the algorithm
   */
  public void setAlgorithm(final String algorithm) {
    this.algorithm = algorithm;
  }

  /**
   * Gets the persistence class.
   *
   * @return the persistence class
   */
  public String getPersistenceClass() {
    return this.persistenceClass;
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
