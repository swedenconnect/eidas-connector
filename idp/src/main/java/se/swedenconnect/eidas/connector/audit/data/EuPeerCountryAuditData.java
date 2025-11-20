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
package se.swedenconnect.eidas.connector.audit.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import org.opensaml.saml.saml2.core.Assertion;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.opensaml.saml2.attribute.AttributeUtils;
import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;

import java.io.Serial;

/**
 * @author Martin Lindström
 */
public class EuPeerCountryAuditData extends ConnectorAuditData {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The receiving country. */
  @Getter
  @Setter
  @JsonProperty("country")
  private String country;

  /** {@inheritDoc} */
  @Override
  public String getName() {
    return "eu-peer-country";
  }

  /**
   * Creates an {@link EuPeerCountryAuditData} given a country code.
   *
   * @param country the country code
   * @return an {@link EuPeerCountryAuditData}
   */
  @Nonnull
  public static EuPeerCountryAuditData of(@Nonnull final String country) {
    final EuPeerCountryAuditData data = new EuPeerCountryAuditData();
    data.setCountry(country);
    return data;
  }

  /**
   * Creates an {@link EuPeerCountryAuditData} given an assertion.
   *
   * @param assertion the SAML assertion
   * @return an {@link EuPeerCountryAuditData}
   */
  @Nonnull
  public static EuPeerCountryAuditData of(@Nonnull final Assertion assertion) {
    final EuPeerCountryAuditData data = new EuPeerCountryAuditData();

    if (assertion != null && !assertion.getAttributeStatements().isEmpty()) {
      data.setCountry(
          assertion.getAttributeStatements().getFirst().getAttributes().stream()
              .filter(a -> AttributeConstants.ATTRIBUTE_NAME_C.equals(a.getName()))
              .map(AttributeUtils::getAttributeStringValue)
              .findFirst()
              .orElse(null));
    }
    return data;
  }

}
