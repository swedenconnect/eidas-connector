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
package se.swedenconnect.eidas.simulator.country;

import org.junit.jupiter.api.Test;
import se.swedenconnect.eidas.simulator.TestCountries;
import se.swedenconnect.eidas.simulator.config.CountryProperties;
import se.swedenconnect.eidas.simulator.config.SimulatorProperties;

import java.security.KeyStore;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test cases for the country configuration files and the {@link CountryRepository}.
 *
 * @author Martin Lindström
 */
class CountryConfigurationTest {

  /** The countries available in production. */
  private static final List<String> COUNTRIES = List.of("AT", "BE", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR",
      "HR", "IT", "LI", "LT", "LU", "LV", "MT", "NL", "PL", "PT", "RO", "SI", "SK");

  /**
   * The formats of the identifier part of PersonIdentifier, from "Vägledning förlitande parter". Countries not
   * listed here use opaque values.
   */
  private static final Map<String, Pattern> FORMATS = Map.ofEntries(
      Map.entry("BE", Pattern.compile("[A-Za-z0-9+/=]{32,128}")),
      Map.entry("CY", Pattern.compile("\\d{10}")),
      Map.entry("EE", Pattern.compile("\\d{11}")),
      Map.entry("FI", Pattern.compile(".{1,17}")),
      Map.entry("IT", Pattern.compile("[A-Za-z0-9]{10}")),
      Map.entry("HR", Pattern.compile("\\d{11}")),
      Map.entry("LU", Pattern.compile("[0-9a-f]{32}")),
      Map.entry("MT", Pattern.compile(".{1,256}")),
      Map.entry("NL", Pattern.compile("[0-9a-f]{64}")),
      Map.entry("PL", Pattern.compile("\\d{11}")),
      Map.entry("PT", Pattern.compile("\\d{8}")),
      Map.entry("SK", Pattern.compile(".{1,32}")),
      Map.entry("SI", Pattern.compile("[\\x20-\\x7E]{1,24}")),
      Map.entry("ES", Pattern.compile("\\d{8}[A-Z]")),
      Map.entry("DE", Pattern.compile("[0-9a-f]{64}")));

  @Test
  void testAllCountriesAreConfigured() throws Exception {
    final SimulatorProperties properties = TestCountries.loadProperties();
    assertThat(properties.getCountries().keySet()).containsExactlyInAnyOrderElementsOf(COUNTRIES);
    assertThat(TestCountries.repository().getCountries()).hasSize(COUNTRIES.size());
  }

  @Test
  void testUsers() throws Exception {
    final SimulatorProperties properties = TestCountries.loadProperties();
    properties.getCountries().forEach((code, country) -> {
      assertThat(country.getUsers()).as(code).hasSizeBetween(3, 5);
      assertThat(country.getUsers().stream().map(SimulatedUser::getId).distinct()).as(code)
          .hasSameSizeAs(country.getUsers());
      assertThat(country.getUsers().stream().map(SimulatedUser::getPersonIdentifier).distinct()).as(code)
          .hasSameSizeAs(country.getUsers());
      for (final SimulatedUser user : country.getUsers()) {
        assertThat(user.getPersonIdentifier()).as(code).isNotBlank();
        assertThat(user.getGivenName()).as(code).isNotBlank();
        assertThat(user.getFamilyName()).as(code).isNotBlank();
        assertThat(LocalDate.parse(user.getDateOfBirth())).as(code).isBefore(LocalDate.now());
        if (FORMATS.containsKey(code)) {
          assertThat(user.getPersonIdentifier()).as(code).matches(FORMATS.get(code));
        }
      }
    });
  }

  @Test
  void testGreekNamesInCyprus() throws Exception {
    final CountryProperties cy = TestCountries.loadProperties().getCountries().get("CY");
    assertThat(cy.getUsers()).anyMatch(u -> u.getFamilyNameNonLatin() != null
        && u.getFamilyNameNonLatin().codePoints().anyMatch(c -> Character.UnicodeScript.of(c)
        == Character.UnicodeScript.GREEK)
        && u.getFamilyName().chars().allMatch(c -> c < 128));
  }

  @Test
  void testEncryptionKeys() throws Exception {
    final CountryRepository repository = TestCountries.repository();
    for (final String code : List.of("DK", "IT", "LU", "LV", "MT", "NL", "PT", "SI")) {
      assertThat(repository.getCountry(code).getEncryptionCredential()).as(code).isNotNull();
    }
    assertThat(repository.getCountry("AT").getEncryptionCredential()).isNull();
    assertThat(repository.getCountry("EE").getSigningCredential().getPublicKey().getAlgorithm()).isEqualTo("EC");
  }

  @Test
  void testActiveCountries() throws Exception {
    final CountryRepository repository = TestCountries.repository("fi", "ee");
    assertThat(repository.getCountries()).extracting(SimulatedCountry::getCode).containsExactly("EE", "FI");
    assertThat(repository.getCountry("fi")).isNotNull();
    assertThat(repository.getCountry("AT")).isNull();
  }

  @Test
  void testUnknownActiveCountry() throws Exception {
    assertThatThrownBy(() -> TestCountries.repository("XX"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("XX");
  }

  @Test
  void testMissingKey() throws Exception {
    final SimulatorProperties properties = TestCountries.loadProperties();
    final KeyStore keyStore = TestCountries.loadKeyStore();
    keyStore.deleteEntry("at-sign");
    assertThatThrownBy(() -> new CountryRepository(properties.getCountries(), List.of("AT"),
        TestCountries.BASE_URL, keyStore, TestCountries.PASSWORD))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at-sign");

    keyStore.deleteEntry("it-encrypt");
    assertThatThrownBy(() -> new CountryRepository(properties.getCountries(), List.of("IT"),
        TestCountries.BASE_URL, keyStore, TestCountries.PASSWORD))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("it-encrypt");
  }

}
