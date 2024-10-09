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
 * Test cases for {@code PridGenColResistEidas}.
 */
public class PridGenColResistEidasTest {

  private PridGenColResistEidas generator = new PridGenColResistEidas();

  @Test
  public void testGenerate() throws PridGeneratorException {
    Assertions.assertEquals("NO:05068907693", generator.getPridIdentifierComponent("NO/SE/05068907693", "NO"));
    Assertions.assertEquals("DK:09208-2002-2-194967071622",
        generator.getPridIdentifierComponent("DK/SE/09208-2002-2-194967071622", "DK"));
    Assertions.assertEquals("DE:12345-3456-abc", generator.getPridIdentifierComponent("DE/SE/#12345-3456//ABC", "DE"));
    Assertions.assertEquals("DE:0aerf-ead9", generator.getPridIdentifierComponent("DE/SE/aErf#(EAd9)", "DE"));
    Assertions.assertEquals("DE:19521214-1122",
        generator.getPridIdentifierComponent("DE/SE/(1952 12 14-1122)  ", "DE"));
    Assertions.assertEquals("DE:1hc3tpoleczqu3t8jz2995k2rq7nt8",
        generator.getPridIdentifierComponent("DE/SE/1234567890123456789012345678901", "DE"));
    Assertions.assertEquals("SK:0009718515", generator.getPridIdentifierComponent("SK/SE/9718515", "SK"));
  }

}
