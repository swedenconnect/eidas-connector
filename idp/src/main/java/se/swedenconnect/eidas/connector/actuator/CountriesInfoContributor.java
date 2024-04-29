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
package se.swedenconnect.eidas.connector.actuator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
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
  private final EuMetadataProvider euMetadata;

  /**
   * Constructor.
   *
   * @param euMetadata the EU metadata
   */
  public CountriesInfoContributor(final EuMetadataProvider euMetadata) {
    this.euMetadata = Objects.requireNonNull(euMetadata, "euMetadata must not be null");
  }

  /** {@inheritDoc} */
  @Override
  public void contribute(final Builder builder) {
    builder.withDetail("countries", this.euMetadata.getAllCountries().stream()
        .map(CountryInfo::new)
        .toList());
  }

  @Data
  @NoArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private static class CountryInfo {

    @JsonProperty("country-code")
    private String countryCode;

    @JsonProperty("entity-id")
    private String entityId;

    @JsonProperty("assurance-levels")
    private List<String> assuranceLevels;

    private Boolean hidden;

    public CountryInfo(final CountryMetadata metadata) {
      this.countryCode = metadata.getCountryCode();
      this.entityId = metadata.getEntityID();
      this.assuranceLevels = metadata.getAssuranceLevels();
      if (metadata.isHideFromDiscovery()) {
        this.hidden = Boolean.TRUE;
      }
    }

  }

}
