/*
 * Copyright 2017-2023 Sweden Connect
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
 * Implementation of the "default-eIDAS" PRID calculation algorithm as described in section 2.3.1 of the <a href=
 * "https://docs.swedenconnect.se/technical-framework/latest/11_-_eIDAS_Constructed_Attributes_Specification_for_the_Swedish_eID_Framework.html#algorithm-default-eidas">
 * eIDAS Constructed Attributes Specification for the Swedish eID Framework</a> specification.
 *
 * @author Martin Lindström
 */
@Slf4j
public class PridGenDefaultEidas extends AbstractPridGenerator {

  /** The algorithm name that this instance implements. */
  public static final String ALGORITHM_NAME = "default-eIDAS";

  /**
   * Constructor (using "SE" as destination country).
   */
  public PridGenDefaultEidas() {
    this("SE");
  }

  /**
   * Constructor.
   *
   * @param destinationCountry country code to which the eIDAS person identifiers are issued for
   */
  public PridGenDefaultEidas(final String destinationCountry) {
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
  protected String calculatePridIdentifier(final String checkedStrippedInput, final String originalInput)
      throws PridGeneratorException {

    // Convert to lower case
    String normalizedID = checkedStrippedInput.toLowerCase();

    // Replace sequences of non ID characters to "-"
    normalizedID = normalizedID.replaceAll("[^a-z0-9]+", "-");

    // Remove leading and trailing "-"
    normalizedID = normalizedID.replaceAll("^-+", "").replaceAll("-+$", "");

    if (normalizedID.replaceAll("-", "").length() < 6) {
      final String msg = String.format("Calculated normalizedID of '%s' is shorter than 6 characters", originalInput);
      log.error("{}: {}", this.getAlgorithmName(), msg);
      throw new PridGeneratorException(msg);
    }

    if (normalizedID.length() < 10) {
      normalizedID = "0000000000".substring(normalizedID.length()) + normalizedID;
    }

    if (normalizedID.length() > 30) {
      try {
        return this.generateForLongID(checkedStrippedInput);
      }
      catch (final Exception e) {
        log.error("{}: Failure to calculate PRID for '{}' - {}", this.getAlgorithmName(), originalInput, e.getMessage(),
            e);
        throw new PridGeneratorException("PRID calculation error", e);
      }
    }

    return normalizedID;
  }

  /**
   * Performs a hash to calculate a PRID for long ID:s.
   *
   * @param input the input
   * @return a hashed ID
   */
  protected String generateForLongID(final String input) {
    return getID(input, 16, 30);
  }

}
