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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for {@code PridGenDefaultEidas}. Also tests functions of {@code AbstractPridGenerator}.
 */
public class PridGenDefaultEidasTest {

  private final PridGenDefaultEidas generator = new PridGenDefaultEidas();

  @Test
  public void testGenerate() throws PridGeneratorException {

    Assertions.assertEquals("NO:05068907693", this.generator.getPridIdentifierComponent("NO/SE/05068907693", "NO"));
    Assertions.assertEquals("NO:05068907693", this.generator.getPridIdentifierComponent("NO/SE/05068907693", "no"));
    Assertions.assertEquals("DK:09208-2002-2-194967071622",
        this.generator.getPridIdentifierComponent("DK/SE/09208-2002-2-194967071622", "DK"));
    Assertions.assertEquals("DE:12345-3456-abc",
        this.generator.getPridIdentifierComponent("DE/SE/#12345-3456//ABC", "DE"));
    Assertions.assertEquals("DE:0aerf-ead9", this.generator.getPridIdentifierComponent("DE/SE/aErf#(EAd9)", "DE"));
    Assertions.assertEquals("DE:19521214-1122",
        this.generator.getPridIdentifierComponent("DE/SE/(1952 12 14-1122)", "DE"));
    Assertions.assertEquals("DE:3b7184c0ceaf76a9607a31e4e1f87f",
        this.generator.getPridIdentifierComponent("DE/SE/1234567890123456789012345678901", "DE"));
    Assertions.assertEquals("SK:0009718515", this.generator.getPridIdentifierComponent("SK/SE/9718515", "SK"));

    // The test algorithm should handle short ID:s as well.
    final PridGenTestEidas testGenerator = new PridGenTestEidas();
    Assertions.assertEquals("XA:0000011111", testGenerator.getPridIdentifierComponent("XA/SE/11111", "XA"));
  }

  @Test
  public void testBadTargetCountry() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      this.generator.getPridIdentifierComponent("UK/DK/1234567890", "UK");
    });
  }

  @Test
  public void testTooShortId() {
    Assertions.assertThrows(PridGeneratorException.class, () -> {
      this.generator.getPridIdentifierComponent("de/se/aErf#(E)", "DE");
    });
  }

  @Test
  public void testLeadingFormatError() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      this.generator.getPridIdentifierComponent("19521214-1122", "DE");
    });
  }

  @Test
  public void testNullPersonIdentifier() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      this.generator.getPridIdentifierComponent(null, "DE");
    });
  }

  @Test
  public void testNullCountryCode() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      this.generator.getPridIdentifierComponent("NO/SE/05068907693", null);
    });
  }

  @Test
  public void testMismatchingCountryCode() {
    Assertions.assertThrows(PridGeneratorException.class, () -> {
      this.generator.getPridIdentifierComponent("NO/SE/05068907693", "DK");
    });
  }

}
