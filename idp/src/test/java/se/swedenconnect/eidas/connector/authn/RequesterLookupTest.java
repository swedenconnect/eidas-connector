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
package se.swedenconnect.eidas.connector.authn;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.swedenconnect.opensaml.sweid.saml2.metadata.entitycategory.EntityCategoryConstants;
import se.swedenconnect.spring.authnserver.authentication.AuthenticationProtocol;
import se.swedenconnect.spring.authnserver.authentication.Requester;
import se.swedenconnect.spring.authnserver.authentication.provider.UserAuthenticationInputToken;
import se.swedenconnect.spring.authnserver.oidc.authentication.OidcAuthenticationRequirements;
import se.swedenconnect.spring.authnserver.registry.ClientRegistry;
import se.swedenconnect.spring.authnserver.registry.ClientRegistryException;
import se.swedenconnect.spring.authnserver.registry.RequesterRecord;
import se.swedenconnect.spring.authnserver.saml.authentication.SamlAuthenticationRequirements;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RequesterLookup}.
 *
 * @author Martin Lindström
 */
class RequesterLookupTest {

  private static final Requester RP = new Requester(AuthenticationProtocol.OIDC, "https://rp.example.com");

  private static final Requester SP = new Requester(AuthenticationProtocol.SAML, "https://sp.example.com");

  @Test
  void testSamlPublicSector() {
    final RequesterLookup lookup = new RequesterLookup(() -> null);

    assertThat(lookup.isPublicSector(new UserAuthenticationInputToken(new SamlAuthenticationRequirements(), SP)))
        .isTrue();

    final SamlAuthenticationRequirements privateSector = new SamlAuthenticationRequirements();
    privateSector.setEntityCategories(
        List.of(EntityCategoryConstants.SERVICE_TYPE_CATEGORY_PRIVATE_SECTOR_SP.getUri()));
    assertThat(lookup.isPublicSector(new UserAuthenticationInputToken(privateSector, SP))).isFalse();
  }

  @Test
  void testOidcPublicSector() throws Exception {
    final ClientRegistry registry = Mockito.mock(ClientRegistry.class);
    final RequesterLookup lookup = new RequesterLookup(() -> registry);
    final UserAuthenticationInputToken token =
        new UserAuthenticationInputToken(new OidcAuthenticationRequirements(), RP);

    Mockito.when(registry.requestMark(RP, RequesterLookup.PUBLIC_BODY_TRUST_MARK))
        .thenReturn(record(Set.of(RequesterLookup.PUBLIC_BODY_TRUST_MARK)));
    assertThat(lookup.isPublicSector(token)).isTrue();

    Mockito.when(registry.requestMark(RP, RequesterLookup.PUBLIC_BODY_TRUST_MARK)).thenReturn(record(Set.of()));
    assertThat(lookup.isPublicSector(token)).isFalse();

    Mockito.when(registry.requestMark(RP, RequesterLookup.PUBLIC_BODY_TRUST_MARK))
        .thenThrow(new ClientRegistryException("Issuer cannot be reached"));
    assertThat(lookup.isPublicSector(token)).isFalse();

    assertThat(new RequesterLookup(() -> null).isPublicSector(token)).isFalse();
  }

  @Test
  void testGetRecord() throws Exception {
    final ClientRegistry registry = Mockito.mock(ClientRegistry.class);
    final RequesterRecord record = record(Set.of());
    Mockito.when(registry.lookup(RP)).thenReturn(record);

    final UserAuthenticationInputToken token =
        new UserAuthenticationInputToken(new OidcAuthenticationRequirements(), RP);
    assertThat(new RequesterLookup(() -> registry).getRecord(token)).isSameAs(record);
    assertThat(new RequesterLookup(() -> null).getRecord(token)).isNull();

    Mockito.when(registry.lookup(RP)).thenThrow(new ClientRegistryException("Failure"));
    assertThat(new RequesterLookup(() -> registry).getRecord(token)).isNull();
  }

  private static RequesterRecord record(final Set<String> marks) {
    return new RequesterRecord(RP, List.of(), List.of(), marks, "metadata");
  }

}
