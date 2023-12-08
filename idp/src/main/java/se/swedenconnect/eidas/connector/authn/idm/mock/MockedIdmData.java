/*
 * Copyright 2023 Sweden Connect
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

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import se.swedenconnect.eidas.connector.authn.idm.IdmQueryResponse;

/**
 * Mocked IdM data.
 *
 * @author Martin Lindström
 */
@Profile("idmmock")
@Component
@ConfigurationProperties("idmmock")
public class MockedIdmData {

  @Getter
  @Setter
  private List<IdmQueryResponse> records;

  @Getter
  @Setter
  private X509Certificate asCertificate;

  @Setter
  @Getter
  @Value("${connector.idm.oauth2.client-id}")
  private String connectorId;

  public IdmQueryResponse getRecord(final String prid) {
    if (this.records == null) {
      return null;
    }
    return this.records.stream()
      .filter(r -> Objects.equals(prid, r.getEidasUserId()))
      .findFirst()
      .orElse(null);
  }

}
