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
package se.swedenconnect.eidas.connector.prid.generator;

/**
 * Interface for Provisional ID (prid) generation.
 *
 * @author Martin Lindström
 */
public interface PridGenerator {

  /**
   * Based on the supplied person identifier, the method calculates the PRID.
   *
   * @param personIdentifier the person identifier
   * @param countryCode the country code for the issuing country (must correspond to contents of the person identifier)
   * @return the PRID
   * @throws PridGeneratorException for errors during generation
   * @throws IllegalArgumentException for bad input
   */
  String getPridIdentifierComponent(final String personIdentifier, final String countryCode)
      throws PridGeneratorException, IllegalArgumentException;

  /**
   * Returns the name of the PRID calculation algorithm that is implemented.
   *
   * @return algorithm name
   */
  String getAlgorithmName();

}
