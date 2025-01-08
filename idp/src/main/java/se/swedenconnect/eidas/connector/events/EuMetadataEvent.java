/*
 * Copyright 2017-2025 Sweden Connect
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
package se.swedenconnect.eidas.connector.events;

import lombok.Getter;
import lombok.Setter;
import se.swedenconnect.eidas.connector.ApplicationVersion;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Event for EU metadata updates.
 *
 * @author Martin Lindström
 */
public class EuMetadataEvent extends AbstractEidasConnectorEvent {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /**
   * Constructor for reporting the result of the latest update.
   *
   * @param instant the update instant
   * @param removedCountries a (possibly empty) list of removed countries
   * @param addedCountries a (possibly empty) list of added countries
   */
  public EuMetadataEvent(
      final Instant instant, final List<String> removedCountries, final List<String> addedCountries) {
    super(new EuMetadataUpdateData(instant, removedCountries, addedCountries));
  }

  /**
   * Constructor for reporting an error during metadata update.
   *
   * @param instant the update instant
   * @param error the error that occurred
   */
  public EuMetadataEvent(final Instant instant, final Exception error) {
    super(new EuMetadataUpdateData(instant, error));
  }

  /**
   * Gets the EU metadata update information.
   *
   * @return an {@link EuMetadataUpdateData} object
   */
  public EuMetadataUpdateData getEuMetadataUpdateData() {
    return (EuMetadataUpdateData) this.getSource();
  }

  /**
   * Adds information string to the event.
   *
   * @param info the information string
   */
  public void addInformation(final String info) {
    this.getEuMetadataUpdateData().setInfo(info);
  }

  /**
   * Metadata update information.
   */
  public static class EuMetadataUpdateData implements Serializable {

    @Serial
    private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

    /** The update instant. */
    @Getter
    private final Instant instant;

    /** A list of country codes for countries that was previously present in the metadata, but now are not. */
    @Getter
    private final List<String> removedCountries;

    /** A list of country codes for countries that have been added since the last update. */
    @Getter
    private final List<String> addedCountries;

    /** For errors updating the metadata. */
    @Getter
    private final Exception error;

    /** Additional information string. */
    @Getter
    @Setter
    private String info;

    /**
     * Constructor.
     *
     * @param instant the update instant
     * @param removedCountries a (possibly empty) list of removed countries
     * @param addedCountries a (possibly empty) list of added countries
     */
    public EuMetadataUpdateData(final Instant instant, final List<String> removedCountries,
        final List<String> addedCountries) {
      this.instant = Objects.requireNonNull(instant, "instant must not be null");
      this.removedCountries = Optional.ofNullable(removedCountries).orElse(Collections.emptyList());
      this.addedCountries = Optional.ofNullable(addedCountries).orElse(Collections.emptyList());
      this.error = null;
    }

    /**
     * Constructor.
     *
     * @param instant the update instant
     * @param error the error that occurred
     */
    public EuMetadataUpdateData(final Instant instant, final Exception error) {
      this.instant = Objects.requireNonNull(instant, "instant must not be null");
      this.removedCountries = Collections.emptyList();
      this.addedCountries = Collections.emptyList();
      this.error = Objects.requireNonNull(error, "error must not be null");
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return String.format("instant='%s', removed=%s, added=%s, info='%s', error=%s",
          this.instant, this.removedCountries, this.addedCountries, this.info != null ? this.info : "not-set",
          this.error != null ? String.format("%s (%s)", this.error.getClass().getSimpleName(), this.error.getMessage())
              : "not-set");

    }

  }

}
