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
import se.swedenconnect.eidas.simulator.config.CountryProperties;
import se.swedenconnect.security.credential.PkiCredential;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A simulated country, that is, one simulated eIDAS proxy service with its own entityID, endpoints and keys.
 *
 * @author Martin Lindström
 */
public class SimulatedCountry {

  /** The path of the metadata of a country, relative to its base URL. Also its entityID. */
  public static final @NonNull String METADATA_PATH = "/ServiceMetadata";

  /** The path of the single sign-on service of a country, relative to its base URL. */
  public static final @NonNull String SSO_PATH = "/ColleagueRequest";

  /** The country code, in upper case. */
  private final @NonNull String code;

  /** The country settings. */
  private final @NonNull CountryProperties settings;

  /** The base URL of the country, {@code <simulator base URL>/<country code in lower case>}. */
  private final @NonNull String baseUrl;

  /** The signing credential. */
  private final @NonNull PkiCredential signingCredential;

  /** The encryption credential, if the country has an encryption key. */
  private final @Nullable PkiCredential encryptionCredential;

  /**
   * Constructor.
   *
   * @param code the country code
   * @param settings the country settings
   * @param simulatorBaseUrl the base URL of the simulator
   * @param signingCredential the signing credential
   * @param encryptionCredential the encryption credential, or {@code null}
   */
  public SimulatedCountry(final @NonNull String code, final @NonNull CountryProperties settings,
      final @NonNull String simulatorBaseUrl, final @NonNull PkiCredential signingCredential,
      final @Nullable PkiCredential encryptionCredential) {
    this.code = Objects.requireNonNull(code, "code must not be null").toUpperCase();
    this.settings = Objects.requireNonNull(settings, "settings must not be null");
    this.baseUrl = Objects.requireNonNull(simulatorBaseUrl, "simulatorBaseUrl must not be null")
        + "/" + this.code.toLowerCase();
    this.signingCredential = Objects.requireNonNull(signingCredential, "signingCredential must not be null");
    this.encryptionCredential = encryptionCredential;
  }

  /**
   * Gets the country code, in upper case.
   *
   * @return the country code
   */
  public @NonNull String getCode() {
    return this.code;
  }

  /**
   * Gets the English name of the country.
   *
   * @return the name
   */
  public @NonNull String getName() {
    return Optional.ofNullable(this.settings.getName()).orElse(this.code);
  }

  /**
   * Gets the country settings.
   *
   * @return the settings
   */
  public @NonNull CountryProperties getSettings() {
    return this.settings;
  }

  /**
   * Gets the entityID of the country, which is also the URL of its metadata.
   *
   * @return the entityID
   */
  public @NonNull String getEntityId() {
    return this.baseUrl + METADATA_PATH;
  }

  /**
   * Gets the URL of the single sign-on service of the country.
   *
   * @return the URL
   */
  public @NonNull String getSsoUrl() {
    return this.baseUrl + SSO_PATH;
  }

  /**
   * Gets the signing credential.
   *
   * @return the signing credential
   */
  public @NonNull PkiCredential getSigningCredential() {
    return this.signingCredential;
  }

  /**
   * Gets the encryption credential.
   *
   * @return the encryption credential, or {@code null} if the country has no encryption key
   */
  public @Nullable PkiCredential getEncryptionCredential() {
    return this.encryptionCredential;
  }

  /**
   * Gets the fixed users of the country.
   *
   * @return the users
   */
  public @NonNull List<SimulatedUser> getUsers() {
    return this.settings.getUsers();
  }

  /**
   * Gets a fixed user by its identity.
   *
   * @param id the identity of the user
   * @return the user, or {@code null} if there is no such user
   */
  public @Nullable SimulatedUser getUser(final @Nullable String id) {
    return this.settings.getUsers().stream()
        .filter(u -> Objects.equals(id, u.getId()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Tells whether the country supports the given attribute, that is, whether the attribute is published in its
   * metadata.
   *
   * @param name the attribute name
   * @return {@code true} if the attribute is supported and {@code false} otherwise
   */
  public boolean isAttributeSupported(final @NonNull String name) {
    return this.settings.getAttributes().stream().anyMatch(a -> name.equals(a.getName()));
  }

  /**
   * Tells whether the country supports the given binding.
   *
   * @param binding the binding URI
   * @return {@code true} if the binding is supported and {@code false} otherwise
   */
  public boolean isBindingSupported(final @NonNull String binding) {
    return this.settings.getBindings().contains(binding);
  }

}
