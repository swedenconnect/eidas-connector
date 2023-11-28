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

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * Base class for PRID generator implementations.
 *
 * @author Martin Lindström
 */
@Slf4j
public abstract class AbstractPridGenerator implements PridGenerator {

  /** The country to which the eIDAS person identifiers are issued for. */
  private final String destinationCountry;

  /** Regexp pattern for checking if the supplied identifier can be processed. */
  private final Pattern personIdentifierPrefixPattern;

  /**
   * Constructor (using "SE" as destination country).
   */
  public AbstractPridGenerator() {
    this("SE");
  }

  /**
   * Constructor.
   *
   * @param destinationCountry country code to which the eIDAS person identifiers are issued for
   */
  public AbstractPridGenerator(final String destinationCountry) {
    this.destinationCountry = Objects.requireNonNull(destinationCountry, "destinationCountry must not be null").toUpperCase();
    this.personIdentifierPrefixPattern = Pattern.compile(
        "^[A-Za-z]{2}[\\/](%s|%s)[\\/]".formatted(this.destinationCountry, this.destinationCountry.toLowerCase()));
  }

  /**
   * Verifies the supplied input, strips the input from whitespace and non-printable characters and invokes
   * {@link #calculatePridIdentifier(String, String)}.
   */
  @Override
  public final String getPridIdentifierComponent(final String personIdentifier, final String countryCode)
      throws PridGeneratorException, IllegalArgumentException {

    log.trace("{}: Calculating PRID for '{}' ...", this.getAlgorithmName(), personIdentifier);

    if (personIdentifier == null) {
      final String msg = "Supplied personIdentifier must not be null";
      log.error("{}: {}", this.getAlgorithmName(), msg);
      throw new IllegalArgumentException(msg);
    }
    if (countryCode == null || countryCode.length() != 2) {
      final String msg = "Supplied country code must be 2 characters";
      log.error("{}: {}", this.getAlgorithmName(), msg);
      throw new IllegalArgumentException(msg);
    }
    if (personIdentifier.length() < 6
        || !this.personIdentifierPrefixPattern.matcher(personIdentifier.substring(0, 6)).matches()) {
      final String msg = String.format("Illegal input - identifier '%s is not valid'", personIdentifier);
      log.error("{}: {}", this.getAlgorithmName(), msg);
      throw new IllegalArgumentException(msg);
    }
    if (!countryCode.equalsIgnoreCase(personIdentifier.substring(0, 2))) {
      final String msg = String.format("Mismatching country - expected '%s', but this is not present in '%s'",
          countryCode, personIdentifier);
      log.error("{}: {}", this.getAlgorithmName(), msg);
      throw new PridGeneratorException(msg);
    }

    // Get ID component without whitespace and non-printable characters
    final String strippedID = personIdentifier.substring(6).replaceAll("\\s+", "");

    String calculatedID = this.calculatePridIdentifier(strippedID, personIdentifier);
    calculatedID = countryCode.toUpperCase() + ":" + calculatedID;

    log.debug("{}: PRID for '{}' calculated as '{}'", this.getAlgorithmName(), personIdentifier, calculatedID);

    return calculatedID;
  }

  /**
   * Calculates the PRID identifier (minus the country code prefix) based on the checked input that also has been
   * stripped for whitespace and non-printable characters.
   *
   * @param checkedStrippedInput the checked and stripped input to operate upon
   * @param originalInput the original input (supplied for logging purposes)
   * @return the generated PRID
   * @throws PridGeneratorException for PRID calculation errors
   */
  protected abstract String calculatePridIdentifier(final String checkedStrippedInput, final String originalInput)
      throws PridGeneratorException;

  /**
   * Calculates the ID.
   *
   * @param idSource the source
   * @param radix the radix
   * @param length the length
   * @return the ID
   * @throws IndexOutOfBoundsException for wrong length
   * @throws NullPointerException if idSource is null
   */
  protected static String getID(final String idSource, final int radix, final int length)
      throws IndexOutOfBoundsException, NullPointerException {
    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      final byte[] digest = md.digest(idSource.getBytes(StandardCharsets.UTF_8));
      return new BigInteger(1, digest).toString(radix).substring(0, length);
    }
    catch (final NoSuchAlgorithmException e) {
      throw new SecurityException(e);
    }
  }

}
