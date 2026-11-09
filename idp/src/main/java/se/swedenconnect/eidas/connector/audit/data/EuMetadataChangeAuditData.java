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
package se.swedenconnect.eidas.connector.audit.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.eidas.connector.events.EuMetadataEvent.EuMetadataUpdateData;

import java.io.Serial;
import java.util.List;

/**
 * Audit event data indicating that the contents of the EU metadata has changed.
 *
 * @author Martin Lindström
 */
public class EuMetadataChangeAuditData extends ConnectorAuditData {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /**
   * A list of country codes for countries that previously appeared in the EU metadata, but were removed after the last
   * update.
   */
  @JsonProperty(value = "removed-countries")
  private List<String> removedCountries;

  /**
   * A list of country codes for countries that previously did not appear in the EU metadata, but were added in the last
   * update.
   */
  @JsonProperty(value = "added-countries")
  private List<String> addedCountries;

  /**
   * Textual information from the update. Contains information that may be of interest.
   */
  @JsonProperty(value = "info")
  private String info;

  /**
   * Textual information from the update if an error occurred.
   */
  @JsonProperty(value = "error-info")
  private String errorInfo;

  /**
   * Gets the list of country codes for countries that were removed in the last update.
   *
   * @return the list of country codes for countries that were removed in the last update
   */
  public List<String> getRemovedCountries() {
    return this.removedCountries;
  }

  /**
   * Assigns the list of country codes for countries that were removed in the last update.
   *
   * @param removedCountries the list of country codes for countries that were removed in the last update
   */
  public void setRemovedCountries(final List<String> removedCountries) {
    this.removedCountries = removedCountries;
  }

  /**
   * Gets the list of country codes for countries that were added in the last update.
   *
   * @return the list of country codes for countries that were added in the last update
   */
  public List<String> getAddedCountries() {
    return this.addedCountries;
  }

  /**
   * Assigns the list of country codes for countries that were added in the last update.
   *
   * @param addedCountries the list of country codes for countries that were added in the last update
   */
  public void setAddedCountries(final List<String> addedCountries) {
    this.addedCountries = addedCountries;
  }

  /**
   * Gets the textual information from the update.
   *
   * @return the textual information from the update
   */
  public String getInfo() {
    return this.info;
  }

  /**
   * Assigns the textual information from the update.
   *
   * @param info the textual information from the update
   */
  public void setInfo(final String info) {
    this.info = info;
  }

  /**
   * Gets the textual error information from the update.
   *
   * @return the textual error information from the update
   */
  public String getErrorInfo() {
    return this.errorInfo;
  }

  /**
   * Assigns the textual error information from the update.
   *
   * @param errorInfo the textual error information from the update
   */
  public void setErrorInfo(final String errorInfo) {
    this.errorInfo = errorInfo;
  }

  /** {@inheritDoc} */
  @Override
  @JsonIgnore
  public String getName() {
    return "eu-metadata-change";
  }

  /**
   * Given a {@link EuMetadataUpdateData} an audit data object is created.
   *
   * @param data the {@link EuMetadataUpdateData}
   * @return an {@link EuMetadataChangeAuditData}
   */
  public static EuMetadataChangeAuditData of(final EuMetadataUpdateData data) {
    if (data == null) {
      return null;
    }
    if (data.getAddedCountries().isEmpty() && data.getRemovedCountries().isEmpty()
        && data.getInfo() == null && data.getError() == null) {
      return null;
    }

    final EuMetadataChangeAuditData auditdata = new EuMetadataChangeAuditData();
    if (!data.getRemovedCountries().isEmpty()) {
      auditdata.setRemovedCountries(data.getRemovedCountries());
    }
    if (!data.getAddedCountries().isEmpty()) {
      auditdata.setAddedCountries(data.getAddedCountries());
    }
    auditdata.setInfo(data.getInfo());
    if (data.getError() != null) {
      auditdata.setErrorInfo(String.format("Exception %s caught with error message '%s'",
          data.getError().getClass().getSimpleName(), data.getError().getMessage()));
    }

    return auditdata;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return String.format("removed-countries=%s, added-countries=%s, info='%s', error-info='%s'",
        this.removedCountries, this.addedCountries, this.info, this.errorInfo);
  }

}
