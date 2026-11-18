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
package se.swedenconnect.eidas.connector.attributes;

import com.nimbusds.openid.connect.sdk.claims.PersonClaims;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Test;
import se.swedenconnect.spring.authnserver.attributes.AttributeIdentifiers;
import se.swedenconnect.spring.authnserver.attributes.GenericAttribute;
import se.swedenconnect.spring.authnserver.oidc.attributes.OidcAttributeMapping;
import se.swedenconnect.spring.authnserver.oidc.attributes.UserClaim;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the OpenID Connect attribute mapping of the connector, see
 * {@link ConnectorAttributes#createOidcAttributeMapping()}.
 *
 * @author Martin Lindström
 */
class ConnectorOidcAttributeMappingTest {

  private final OidcAttributeMapping mapping = ConnectorAttributes.createOidcAttributeMapping();

  @Test
  void testCountryOfResidenceOnly() {
    final Map<String, Object> address = this.address(List.of(
        GenericAttribute.of(AttributeIdentifiers.COUNTRY_OF_RESIDENCE, "DE")));
    assertThat(address).containsOnly(Map.entry("country", "DE"));
  }

  @Test
  void testAddressWithoutCountry() {
    final Map<String, Object> address = this.address(List.of(
        GenericAttribute.of(AttributeIdentifiers.EIDAS_ADDRESS_THOROUGHFARE, "Hauptstrasse"),
        GenericAttribute.of(AttributeIdentifiers.EIDAS_ADDRESS_POST_NAME, "Berlin"),
        GenericAttribute.of(AttributeIdentifiers.COUNTRY_OF_RESIDENCE, "DE")));
    assertThat(address)
        .containsEntry("street_address", "Hauptstrasse")
        .containsEntry("locality", "Berlin")
        .containsEntry("country", "DE");
  }

  @Test
  void testCountryOfAddressWins() {
    final Map<String, Object> address = this.address(List.of(
        GenericAttribute.of(AttributeIdentifiers.EIDAS_ADDRESS_POST_NAME, "Wien"),
        GenericAttribute.of(AttributeIdentifiers.EIDAS_ADDRESS_ADMIN_UNIT_FIRST_LINE, "AT"),
        GenericAttribute.of(AttributeIdentifiers.COUNTRY_OF_RESIDENCE, "DE")));
    assertThat(address)
        .containsEntry("locality", "Wien")
        .containsEntry("country", "AT");
  }

  @Test
  void testSupportedClaims() {
    assertThat(this.mapping.getClaimNames(List.of(AttributeIdentifiers.COUNTRY_OF_RESIDENCE)))
        .containsExactly(PersonClaims.ADDRESS_CLAIM_NAME);
  }

  private Map<String, Object> address(final List<GenericAttribute<? extends Serializable>> attributes) {
    final Map<String, Object> claims = this.mapping.toClaims(attributes, null).stream()
        .collect(Collectors.toMap(UserClaim::name, UserClaim::value));
    assertThat(claims).containsKey(PersonClaims.ADDRESS_CLAIM_NAME);
    final Object address = claims.get(PersonClaims.ADDRESS_CLAIM_NAME);
    return address instanceof final JSONObject json
        ? json
        : ((com.nimbusds.openid.connect.sdk.claims.ClaimsSet) address).toJSONObject();
  }

}
