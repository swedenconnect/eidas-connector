/*
 * Copyright 2017-2025 Sweden Connect
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

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the "special-characters-eIDAS" PRID calculation algorithm as described in section 2.3.3 of the
 * <a href=
 * "https://docs.swedenconnect.se/technical-framework/latest/11_-_eIDAS_Constructed_Attributes_Specification_for_the_Swedish_eID_Framework.html#special-characters-eidas">
 * eIDAS Constructed Attributes Specification for the Swedish eID Framework</a> specification.
 *
 * @author Martin Lindström
 */
@Slf4j
public class PridGenBase64Eidas extends AbstractPridGenerator {

  /** The algorithm name that this instance implements. */
  public static final String ALGORITHM_NAME = "special-characters-eIDAS";

  /**
   * Constructor (using "SE" as destination country).
   */
  public PridGenBase64Eidas() {
    this("SE");
  }

  /**
   * Constructor.
   *
   * @param destinationCountry country code to which the eIDAS person identifiers are issued for
   */
  public PridGenBase64Eidas(final String destinationCountry) {
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
  protected String calculatePridIdentifier(
      final String checkedStrippedInput, final String originalInput) throws PridGeneratorException {

    if (checkedStrippedInput.length() < 16) {
      final String msg = String.format("Input for PRID calculation '%s' is shorter than 16 characters", originalInput);
      log.error("{}: {}", this.getAlgorithmName(), msg);
      throw new PridGeneratorException(msg);
    }

    try {
      return getID(checkedStrippedInput, 36, 30);
    }
    catch (final Exception e) {
      log.error("{}: Failure to calculate PRID for '{}' - {}", this.getAlgorithmName(), originalInput, e.getMessage(),
          e);
      throw new PridGeneratorException("PRID calculation error", e);
    }
  }

}
