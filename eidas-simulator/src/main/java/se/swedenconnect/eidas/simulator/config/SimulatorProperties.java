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
package se.swedenconnect.eidas.simulator.config;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for the eIDAS simulator.
 *
 * @author Martin Lindström
 */
@ConfigurationProperties("simulator")
public class SimulatorProperties {

  /**
   * The base URL of the simulator, including protocol, host, port and context path. The URLs and entityIDs of the
   * simulated countries are derived from it.
   */
  private @Nullable String baseUrl;

  /**
   * The key store holding the keys of the simulator. The signing key of a country has the alias
   * {@code <country code>-sign} and its encryption key {@code <country code>-encrypt}, in lower case.
   */
  private @NonNull KeyStoreProperties keystore = new KeyStoreProperties();

  /**
   * Settings for the published metadata.
   */
  private @NonNull MetadataProperties metadata = new MetadataProperties();

  /**
   * Settings for the eIDAS connector that the simulator serves.
   */
  private @NonNull ConnectorProperties connector = new ConnectorProperties();

  /**
   * Settings for the processing of authentication requests.
   */
  private @NonNull RequestProperties request = new RequestProperties();

  /**
   * The country codes of the active countries. If empty, all configured countries are active.
   */
  private @NonNull List<String> activeCountries = new ArrayList<>();

  /**
   * The settings of the simulated countries, keyed by country code.
   */
  private @NonNull Map<String, CountryProperties> countries = new LinkedHashMap<>();

  /**
   * Gets the base URL.
   *
   * @return the base URL
   */
  public @Nullable String getBaseUrl() {
    return this.baseUrl;
  }

  /**
   * Assigns the base URL.
   *
   * @param baseUrl the base URL
   */
  public void setBaseUrl(final @Nullable String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * Gets the key store settings.
   *
   * @return the key store settings
   */
  public @NonNull KeyStoreProperties getKeystore() {
    return this.keystore;
  }

  /**
   * Assigns the key store settings.
   *
   * @param keystore the key store settings
   */
  public void setKeystore(final @NonNull KeyStoreProperties keystore) {
    this.keystore = keystore;
  }

  /**
   * Gets the metadata settings.
   *
   * @return the metadata settings
   */
  public @NonNull MetadataProperties getMetadata() {
    return this.metadata;
  }

  /**
   * Assigns the metadata settings.
   *
   * @param metadata the metadata settings
   */
  public void setMetadata(final @NonNull MetadataProperties metadata) {
    this.metadata = metadata;
  }

  /**
   * Gets the connector settings.
   *
   * @return the connector settings
   */
  public @NonNull ConnectorProperties getConnector() {
    return this.connector;
  }

  /**
   * Assigns the connector settings.
   *
   * @param connector the connector settings
   */
  public void setConnector(final @NonNull ConnectorProperties connector) {
    this.connector = connector;
  }

  /**
   * Gets the request processing settings.
   *
   * @return the request processing settings
   */
  public @NonNull RequestProperties getRequest() {
    return this.request;
  }

  /**
   * Assigns the request processing settings.
   *
   * @param request the request processing settings
   */
  public void setRequest(final @NonNull RequestProperties request) {
    this.request = request;
  }

  /**
   * Gets the country codes of the active countries.
   *
   * @return the country codes, empty for all configured countries
   */
  public @NonNull List<String> getActiveCountries() {
    return this.activeCountries;
  }

  /**
   * Assigns the country codes of the active countries.
   *
   * @param activeCountries the country codes
   */
  public void setActiveCountries(final @NonNull List<String> activeCountries) {
    this.activeCountries = activeCountries;
  }

  /**
   * Gets the country settings, keyed by country code.
   *
   * @return the country settings
   */
  public @NonNull Map<String, CountryProperties> getCountries() {
    return this.countries;
  }

  /**
   * Assigns the country settings, keyed by country code.
   *
   * @param countries the country settings
   */
  public void setCountries(final @NonNull Map<String, CountryProperties> countries) {
    this.countries = countries;
  }

  /**
   * Key store settings.
   */
  public static class KeyStoreProperties {

    /**
     * The location of the key store.
     */
    private @Nullable Resource location;

    /**
     * The password of the key store, also used for its keys.
     */
    private @Nullable String password;

    /**
     * The key store type.
     */
    private @NonNull String type = "JKS";

    /**
     * Gets the location of the key store.
     *
     * @return the location
     */
    public @Nullable Resource getLocation() {
      return this.location;
    }

    /**
     * Assigns the location of the key store.
     *
     * @param location the location
     */
    public void setLocation(final @Nullable Resource location) {
      this.location = location;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public @Nullable String getPassword() {
      return this.password;
    }

    /**
     * Assigns the password.
     *
     * @param password the password
     */
    public void setPassword(final @Nullable String password) {
      this.password = password;
    }

    /**
     * Gets the key store type.
     *
     * @return the key store type
     */
    public @NonNull String getType() {
      return this.type;
    }

    /**
     * Assigns the key store type.
     *
     * @param type the key store type
     */
    public void setType(final @NonNull String type) {
      this.type = type;
    }

  }

  /**
   * Settings for the published metadata.
   */
  public static class MetadataProperties {

    /**
     * The alias of the metadata signing key in the key store.
     */
    private @NonNull String signingKeyAlias = "metadata";

    /**
     * For how long the published metadata is valid.
     */
    private @NonNull Duration validity = Duration.ofDays(7);

    /**
     * For how long the published metadata may be cached.
     */
    private @NonNull Duration cacheDuration = Duration.ofHours(1);

    /**
     * Gets the alias of the metadata signing key.
     *
     * @return the alias
     */
    public @NonNull String getSigningKeyAlias() {
      return this.signingKeyAlias;
    }

    /**
     * Assigns the alias of the metadata signing key.
     *
     * @param signingKeyAlias the alias
     */
    public void setSigningKeyAlias(final @NonNull String signingKeyAlias) {
      this.signingKeyAlias = signingKeyAlias;
    }

    /**
     * Gets the validity of the published metadata.
     *
     * @return the validity
     */
    public @NonNull Duration getValidity() {
      return this.validity;
    }

    /**
     * Assigns the validity of the published metadata.
     *
     * @param validity the validity
     */
    public void setValidity(final @NonNull Duration validity) {
      this.validity = validity;
    }

    /**
     * Gets the cache duration of the published metadata.
     *
     * @return the cache duration
     */
    public @NonNull Duration getCacheDuration() {
      return this.cacheDuration;
    }

    /**
     * Assigns the cache duration of the published metadata.
     *
     * @param cacheDuration the cache duration
     */
    public void setCacheDuration(final @NonNull Duration cacheDuration) {
      this.cacheDuration = cacheDuration;
    }

  }

  /**
   * Settings for the eIDAS connector that the simulator serves.
   */
  public static class ConnectorProperties {

    /**
     * The URL of the connector's eIDAS SP metadata.
     */
    private @Nullable String metadataUrl;

    /**
     * The certificate that validates the signature of the connector metadata. If not set, the signature is not
     * checked.
     */
    private @Nullable Resource validationCertificate;

    /**
     * How often the connector metadata is downloaded again.
     */
    private @NonNull Duration refreshInterval = Duration.ofMinutes(10);

    /**
     * Gets the URL of the connector metadata.
     *
     * @return the URL
     */
    public @Nullable String getMetadataUrl() {
      return this.metadataUrl;
    }

    /**
     * Assigns the URL of the connector metadata.
     *
     * @param metadataUrl the URL
     */
    public void setMetadataUrl(final @Nullable String metadataUrl) {
      this.metadataUrl = metadataUrl;
    }

    /**
     * Gets the certificate that validates the connector metadata.
     *
     * @return the certificate resource, or {@code null}
     */
    public @Nullable Resource getValidationCertificate() {
      return this.validationCertificate;
    }

    /**
     * Assigns the certificate that validates the connector metadata.
     *
     * @param validationCertificate the certificate resource
     */
    public void setValidationCertificate(final @Nullable Resource validationCertificate) {
      this.validationCertificate = validationCertificate;
    }

    /**
     * Gets how often the connector metadata is downloaded again.
     *
     * @return the refresh interval
     */
    public @NonNull Duration getRefreshInterval() {
      return this.refreshInterval;
    }

    /**
     * Assigns how often the connector metadata is downloaded again.
     *
     * @param refreshInterval the refresh interval
     */
    public void setRefreshInterval(final @NonNull Duration refreshInterval) {
      this.refreshInterval = refreshInterval;
    }

  }

  /**
   * Settings for the processing of authentication requests.
   */
  public static class RequestProperties {

    /**
     * The maximum age of an authentication request, counted from its {@code IssueInstant}.
     */
    private @NonNull Duration maxAge = Duration.ofMinutes(5);

    /**
     * The allowed clock skew between the connector and the simulator.
     */
    private @NonNull Duration clockSkew = Duration.ofSeconds(30);

    /**
     * Whether a NameID format in the request that the country does not support is answered with an error response
     * ({@code InvalidNameIDPolicy}). If {@code false}, a format that the country supports is used instead.
     */
    private boolean strictNameIdPolicy;

    /**
     * Gets the maximum age of an authentication request.
     *
     * @return the maximum age
     */
    public @NonNull Duration getMaxAge() {
      return this.maxAge;
    }

    /**
     * Assigns the maximum age of an authentication request.
     *
     * @param maxAge the maximum age
     */
    public void setMaxAge(final @NonNull Duration maxAge) {
      this.maxAge = maxAge;
    }

    /**
     * Gets the allowed clock skew.
     *
     * @return the clock skew
     */
    public @NonNull Duration getClockSkew() {
      return this.clockSkew;
    }

    /**
     * Assigns the allowed clock skew.
     *
     * @param clockSkew the clock skew
     */
    public void setClockSkew(final @NonNull Duration clockSkew) {
      this.clockSkew = clockSkew;
    }

    /**
     * Tells whether the NameID policy of requests is checked strictly.
     *
     * @return whether the NameID policy is checked strictly
     */
    public boolean isStrictNameIdPolicy() {
      return this.strictNameIdPolicy;
    }

    /**
     * Assigns whether the NameID policy of requests is checked strictly.
     *
     * @param strictNameIdPolicy whether the NameID policy is checked strictly
     */
    public void setStrictNameIdPolicy(final boolean strictNameIdPolicy) {
      this.strictNameIdPolicy = strictNameIdPolicy;
    }

  }

}
