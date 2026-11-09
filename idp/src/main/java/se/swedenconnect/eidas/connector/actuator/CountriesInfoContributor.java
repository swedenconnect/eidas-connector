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
package se.swedenconnect.eidas.connector.actuator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import se.swedenconnect.eidas.connector.authn.metadata.CountryMetadata;
import se.swedenconnect.eidas.connector.authn.metadata.EuMetadataProvider;

import java.util.List;
import java.util.Objects;

/**
 * Provides information about the countries that we may send requests to.
 *
 * @author Martin Lindström
 */
@Component
public class CountriesInfoContributor implements InfoContributor {

  /** The EU metadata. */
  private final @NonNull EuMetadataProvider euMetadata;

  /**
   * Constructor.
   *
   * @param euMetadata the EU metadata
   */
  public CountriesInfoContributor(final @NonNull EuMetadataProvider euMetadata) {
    this.euMetadata = Objects.requireNonNull(euMetadata, "euMetadata must not be null");
  }

  /** {@inheritDoc} */
  @Override
  public void contribute(final @NonNull Builder builder) {
    builder.withDetail("countries", this.euMetadata.getAllCountries().stream()
        .map(CountryInfo::new)
        .toList());
  }

  /**
   * Information about a country.
   */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private static class CountryInfo {

    @JsonProperty("country-code")
    private @Nullable String countryCode;

    @JsonProperty("entity-id")
    private @Nullable String entityId;

    @JsonProperty("assurance-levels")
    private @Nullable List<String> assuranceLevels;

    private @Nullable Boolean hidden;

    /**
     * Default constructor.
     */
    public CountryInfo() {
    }

    /**
     * Constructor setting up the country information from the country metadata.
     *
     * @param metadata the country metadata
     */
    public CountryInfo(final @NonNull CountryMetadata metadata) {
      this.countryCode = metadata.getCountryCode();
      this.entityId = metadata.getEntityID();
      this.assuranceLevels = metadata.getAssuranceLevels();
      if (metadata.isHideFromDiscovery()) {
        this.hidden = Boolean.TRUE;
      }
    }

    /**
     * Gets the country code.
     *
     * @return the country code
     */
    public @Nullable String getCountryCode() {
      return this.countryCode;
    }

    /**
     * Assigns the country code.
     *
     * @param countryCode the country code
     */
    public void setCountryCode(final @Nullable String countryCode) {
      this.countryCode = countryCode;
    }

    /**
     * Gets the entityID.
     *
     * @return the entityID
     */
    public @Nullable String getEntityId() {
      return this.entityId;
    }

    /**
     * Assigns the entityID.
     *
     * @param entityId the entityID
     */
    public void setEntityId(final @Nullable String entityId) {
      this.entityId = entityId;
    }

    /**
     * Gets the assurance levels.
     *
     * @return the assurance levels
     */
    public @Nullable List<String> getAssuranceLevels() {
      return this.assuranceLevels;
    }

    /**
     * Assigns the assurance levels.
     *
     * @param assuranceLevels the assurance levels
     */
    public void setAssuranceLevels(final @Nullable List<String> assuranceLevels) {
      this.assuranceLevels = assuranceLevels;
    }

    /**
     * Gets whether the country is hidden from discovery.
     *
     * @return whether the country is hidden from discovery
     */
    public @Nullable Boolean getHidden() {
      return this.hidden;
    }

    /**
     * Assigns whether the country is hidden from discovery.
     *
     * @param hidden whether the country is hidden from discovery
     */
    public void setHidden(final @Nullable Boolean hidden) {
      this.hidden = hidden;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final @Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof final CountryInfo that)) {
        return false;
      }
      return Objects.equals(this.countryCode, that.countryCode) && Objects.equals(this.entityId, that.entityId)
          && Objects.equals(this.assuranceLevels, that.assuranceLevels) && Objects.equals(this.hidden, that.hidden);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return Objects.hash(this.countryCode, this.entityId, this.assuranceLevels, this.hidden);
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull String toString() {
      return "countryCode=" + this.countryCode + ", entityId=" + this.entityId
          + ", assuranceLevels=" + this.assuranceLevels + ", hidden=" + this.hidden;
    }

  }

}
