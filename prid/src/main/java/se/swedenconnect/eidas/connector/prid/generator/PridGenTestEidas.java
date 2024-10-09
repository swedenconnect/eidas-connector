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
 * Generator for test cases (especially when running against the EU software locally).
 *
 * @author Martin Lindström
 */
public class PridGenTestEidas extends PridGenDefaultEidas {

  /** The algorithm name that this instance implements. */
  public static final String ALGORITHM_NAME = "test-eIDAS";

  /**
   * Constructor (using "SE" as destination country).
   */
  public PridGenTestEidas() {
    this("SE");
  }

  /**
   * Constructor.
   *
   * @param destinationCountry country code to which the eIDAS person identifiers are issued for
   */
  public PridGenTestEidas(final String destinationCountry) {
    super(destinationCountry);
  }

  /** {@inheritDoc} */
  @Override
  public String getAlgorithmName() {
    return ALGORITHM_NAME;
  }

  /**
   * Removes the length citerion on received identifiers.
   */
  @Override
  protected String calculatePridIdentifier(String checkedStrippedInput, final String originalInput)
      throws PridGeneratorException {
    if (checkedStrippedInput.length() < 10) {
      checkedStrippedInput = "0000000000".substring(checkedStrippedInput.length()) + checkedStrippedInput;
    }
    return super.calculatePridIdentifier(checkedStrippedInput, originalInput);
  }

}
