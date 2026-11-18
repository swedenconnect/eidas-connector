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

import com.nimbusds.openid.connect.sdk.claims.Address;
import com.nimbusds.openid.connect.sdk.claims.PersonClaims;
import org.jspecify.annotations.NonNull;
import org.springframework.util.StringUtils;
import se.swedenconnect.spring.authnserver.attributes.AttributeIdentifiers;
import se.swedenconnect.spring.authnserver.attributes.GenericAttribute;
import se.swedenconnect.spring.authnserver.attributes.mapping.ToProtocolMappingContext;
import se.swedenconnect.spring.authnserver.oidc.attributes.UserClaim;
import se.swedenconnect.spring.authnserver.oidc.attributes.mapping.ToClaimMapper;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Maps the eIDAS {@code CountryOfResidence} attribute into the {@code country} member of the OpenID Connect
 * {@code address} claim, as the OpenID Connect Claims and Scopes Specification for Sweden Connect, Appendix A, says.
 * <p>
 * The country of a received {@code CurrentAddress} wins. The country of residence is only used when the address has no
 * country, or when no address was received.
 * </p>
 *
 * @author Martin Lindström
 */
public class CountryOfResidenceToClaimMapper implements ToClaimMapper {

  /** {@inheritDoc} */
  @Override
  public @NonNull Collection<String> getSupportedIdentifiers() {
    return List.of(AttributeIdentifiers.COUNTRY_OF_RESIDENCE);
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull Collection<String> getClaimNames(final @NonNull String identifier) {
    return AttributeIdentifiers.COUNTRY_OF_RESIDENCE.equals(identifier)
        ? List.of(PersonClaims.ADDRESS_CLAIM_NAME)
        : List.of();
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull List<UserClaim> map(final @NonNull List<GenericAttribute<? extends Serializable>> attributes,
      final @NonNull ToProtocolMappingContext context) {

    if (StringUtils.hasText(context.getStringValue(AttributeIdentifiers.COUNTRY))
        || StringUtils.hasText(context.getStringValue(AttributeIdentifiers.EIDAS_ADDRESS_ADMIN_UNIT_FIRST_LINE))) {
      return List.of();
    }
    final String country = context.getStringValue(AttributeIdentifiers.COUNTRY_OF_RESIDENCE);
    if (!StringUtils.hasText(country)) {
      return List.of();
    }
    final Address address = new Address();
    address.setCountry(country);
    return List.of(UserClaim.of(PersonClaims.ADDRESS_CLAIM_NAME, address));
  }

}
