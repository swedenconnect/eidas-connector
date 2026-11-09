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
package se.swedenconnect.eidas.connector.authn.idm.mock;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import se.swedenconnect.eidas.connector.authn.idm.IdmQueryResponse;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;

/**
 * Mocked IdM data.
 *
 * @author Martin Lindström
 */
@Profile("idmmock")
@Component
@ConfigurationProperties("idmmock")
public class MockedIdmData {

  private @Nullable List<IdmQueryResponse> records;

  private @Nullable X509Certificate asCertificate;

  @Value("${connector.idm.oauth2.client-id}")
  private @Nullable String connectorId;

  /**
   * Gets the mocked IdM records.
   *
   * @return the mocked IdM records
   */
  public @Nullable List<IdmQueryResponse> getRecords() {
    return this.records;
  }

  /**
   * Assigns the mocked IdM records.
   *
   * @param records the mocked IdM records
   */
  public void setRecords(final @Nullable List<IdmQueryResponse> records) {
    this.records = records;
  }

  /**
   * Gets the certificate of the authorization server issuing access tokens.
   *
   * @return the certificate of the authorization server issuing access tokens
   */
  public @Nullable X509Certificate getAsCertificate() {
    return this.asCertificate;
  }

  /**
   * Assigns the certificate of the authorization server issuing access tokens.
   *
   * @param asCertificate the certificate of the authorization server issuing access tokens
   */
  public void setAsCertificate(final @Nullable X509Certificate asCertificate) {
    this.asCertificate = asCertificate;
  }

  /**
   * Gets the OAuth2 client ID of the connector.
   *
   * @return the OAuth2 client ID of the connector
   */
  public @Nullable String getConnectorId() {
    return this.connectorId;
  }

  /**
   * Assigns the OAuth2 client ID of the connector.
   *
   * @param connectorId the OAuth2 client ID of the connector
   */
  public void setConnectorId(final @Nullable String connectorId) {
    this.connectorId = connectorId;
  }

  /**
   * Gets the mocked IdM record for the given PRID.
   *
   * @param prid the PRID
   * @return the record, or {@code null} if no record exists
   */
  public @Nullable IdmQueryResponse getRecord(final @Nullable String prid) {
    if (this.records == null) {
      return null;
    }
    return this.records.stream()
        .filter(r -> Objects.equals(prid, r.getEidasUserId()))
        .findFirst()
        .orElse(null);
  }

}
