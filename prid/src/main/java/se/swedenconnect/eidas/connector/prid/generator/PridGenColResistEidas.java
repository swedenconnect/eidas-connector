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
 * Implementation of the "colresist-eIDAS" PRID calculation algorithm as described in section 2.3.2 of the <a href=
 * "https://docs.swedenconnect.se/technical-framework/latest/11_-_eIDAS_Constructed_Attributes_Specification_for_the_Swedish_eID_Framework.html#algorithm-colresist-eidas">
 * eIDAS Constructed Attributes Specification for the Swedish eID Framework</a> specification.
 *
 * @author Martin Lindström
 */
public class PridGenColResistEidas extends PridGenDefaultEidas {

  /** The algorithm name that this instance implements. */
  public static final String ALGORITHM_NAME = "colresist-eIDAS";

  /**
   * Constructor (using "SE" as destination country).
   */
  public PridGenColResistEidas() {
    this("SE");
  }

  /**
   * Constructor.
   *
   * @param destinationCountry country code to which the eIDAS person identifiers are issued for
   */
  public PridGenColResistEidas(final String destinationCountry) {
    super(destinationCountry);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAlgorithmName() {
    return ALGORITHM_NAME;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String generateForLongID(final String input)
      throws IndexOutOfBoundsException, NullPointerException {
    return getID(input, 36, 30);
  }

}
