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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedenconnect.eidas.simulator.config.CountryProperties;
import se.swedenconnect.security.credential.KeyStoreCredential;
import se.swedenconnect.security.credential.PkiCredential;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the active simulated countries.
 *
 * @author Martin Lindström
 */
public class CountryRepository {

  /** Logger. */
  private static final Logger log = LoggerFactory.getLogger(CountryRepository.class);

  /** The active countries, keyed by country code in upper case. */
  private final @NonNull Map<String, SimulatedCountry> countries = new LinkedHashMap<>();

  /**
   * Constructor. The signing key of a country is read from the key store under the alias
   * {@code <country code>-sign}, and its encryption key, if it has one, under {@code <country code>-encrypt}.
   *
   * @param settings the country settings, keyed by country code
   * @param activeCountries the country codes of the active countries, empty for all
   * @param simulatorBaseUrl the base URL of the simulator
   * @param keyStore the key store holding the keys of the countries
   * @param keyPassword the password of the keys
   * @throws IllegalArgumentException for an active country that is not configured, or a missing key
   */
  public CountryRepository(final @NonNull Map<String, CountryProperties> settings,
      final @NonNull List<String> activeCountries, final @NonNull String simulatorBaseUrl,
      final @NonNull KeyStore keyStore, final char @NonNull [] keyPassword) throws IllegalArgumentException {

    final Map<String, CountryProperties> byCode = new LinkedHashMap<>();
    settings.forEach((code, country) -> byCode.put(code.toUpperCase(), country));

    final List<String> codes = activeCountries.isEmpty()
        ? new ArrayList<>(byCode.keySet())
        : activeCountries.stream().map(String::toUpperCase).toList();

    for (final String code : codes.stream().sorted().toList()) {
      final CountryProperties country = byCode.get(code);
      if (country == null) {
        throw new IllegalArgumentException("Active country %s is not configured".formatted(code));
      }
      final PkiCredential signing = loadCredential(keyStore, code.toLowerCase() + "-sign", keyPassword);
      if (signing == null) {
        throw new IllegalArgumentException("No signing key for %s in the key store (alias %s-sign)"
            .formatted(code, code.toLowerCase()));
      }
      PkiCredential encryption = null;
      if (country.getEncryptionKey() != null) {
        encryption = loadCredential(keyStore, code.toLowerCase() + "-encrypt", keyPassword);
        if (encryption == null) {
          throw new IllegalArgumentException("No encryption key for %s in the key store (alias %s-encrypt)"
              .formatted(code, code.toLowerCase()));
        }
      }
      this.countries.put(code, new SimulatedCountry(code, country, simulatorBaseUrl, signing, encryption));
    }
    log.info("Simulated countries: {}", this.countries.keySet());
  }

  /**
   * Gets an active country.
   *
   * @param code the country code, in any case
   * @return the country, or {@code null} if it is not active
   */
  public @Nullable SimulatedCountry getCountry(final @Nullable String code) {
    return code != null ? this.countries.get(code.toUpperCase()) : null;
  }

  /**
   * Gets all active countries, sorted by country code.
   *
   * @return the countries
   */
  public @NonNull Collection<SimulatedCountry> getCountries() {
    return Collections.unmodifiableCollection(this.countries.values());
  }

  /**
   * Loads a credential from the key store.
   *
   * @param keyStore the key store
   * @param alias the alias
   * @param password the key password
   * @return the credential, or {@code null} if the alias is not present
   */
  private static @Nullable PkiCredential loadCredential(final @NonNull KeyStore keyStore,
      final @NonNull String alias, final char @NonNull [] password) {
    try {
      if (!keyStore.containsAlias(alias)) {
        return null;
      }
      final KeyStoreCredential credential = new KeyStoreCredential(keyStore, alias, password);
      credential.setName(alias);
      return credential;
    }
    catch (final KeyStoreException e) {
      throw new IllegalArgumentException("Failed to load key %s - %s".formatted(alias, e.getMessage()), e);
    }
  }

}
