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
import se.swedenconnect.opensaml.sweid.saml2.metadata.entitycategory.EntityCategoryConstants;
import se.swedenconnect.spring.authnserver.attributes.AttributeIdentifiers;
import se.swedenconnect.spring.authnserver.attributes.GenericAttribute;
import se.swedenconnect.spring.authnserver.authentication.AuthenticatedUser;
import se.swedenconnect.spring.authnserver.authentication.AuthenticationProtocol;
import se.swedenconnect.spring.authnserver.authentication.AuthenticationRequirements;
import se.swedenconnect.spring.authnserver.authentication.Requester;
import se.swedenconnect.spring.authnserver.authentication.UserAuthentication;
import se.swedenconnect.spring.authnserver.saml.authentication.SamlAuthenticationRequirements;
import se.swedenconnect.spring.authnserver.sso.SsoDecision;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EidasSsoVoter} and {@link SignServiceSsoVoter}.
 *
 * @author Martin Lindström
 */
class SsoVotersTest {

  private static final Requester REQUESTER = new Requester(AuthenticationProtocol.SAML, "https://sp.example.com");

  @Test
  void testEidasSsoVoter() {
    final EidasSsoVoter voter = new EidasSsoVoter();
    final UserAuthentication previous = previous("DE");

    assertThat(voter.vote(previous, requirements(), REQUESTER, List.of()).vote())
        .isEqualTo(SsoDecision.Vote.ALLOW);
    assertThat(voter.vote(previous, requirements(EidasCountryHandler.COUNTRY_URI_PREFIX + "DE"), REQUESTER,
        List.of()).vote()).isEqualTo(SsoDecision.Vote.ALLOW);
    assertThat(voter.vote(previous, requirements(EidasCountryHandler.COUNTRY_URI_PREFIX + "FR"), REQUESTER,
        List.of()).vote()).isEqualTo(SsoDecision.Vote.DENY);
    assertThat(voter.vote(previous(null), requirements(EidasCountryHandler.COUNTRY_URI_PREFIX + "FR"), REQUESTER,
        List.of()).vote()).isEqualTo(SsoDecision.Vote.ABSTAIN);
  }

  @Test
  void testSignServiceSsoVoter() {
    final SignServiceSsoVoter voter = new SignServiceSsoVoter();

    final SamlAuthenticationRequirements signService = new SamlAuthenticationRequirements();
    signService.setEntityCategories(List.of(EntityCategoryConstants.SERVICE_TYPE_CATEGORY_SIGSERVICE.getUri()));
    assertThat(voter.vote(previous("DE"), signService, REQUESTER, List.of()).vote())
        .isEqualTo(SsoDecision.Vote.DENY);
    assertThat(SignServiceSsoVoter.isSignatureService(signService)).isTrue();

    final SamlAuthenticationRequirements other = new SamlAuthenticationRequirements();
    assertThat(voter.vote(previous("DE"), other, REQUESTER, List.of()).vote())
        .isEqualTo(SsoDecision.Vote.ABSTAIN);
    assertThat(SignServiceSsoVoter.isSignatureService(new AuthenticationRequirements())).isFalse();
  }

  private static AuthenticationRequirements requirements(final String... providers) {
    final AuthenticationRequirements requirements = new AuthenticationRequirements();
    requirements.setRequestedAuthnProviders(List.of(providers));
    return requirements;
  }

  private static UserAuthentication previous(final String country) {
    final List<GenericAttribute<?>> attributes = country != null
        ? List.of(GenericAttribute.of(AttributeIdentifiers.PRID, "DE:123"),
        GenericAttribute.of(AttributeIdentifiers.COUNTRY, country))
        : List.of(GenericAttribute.of(AttributeIdentifiers.PRID, "DE:123"));
    return new UserAuthentication(new AuthenticatedUser(attributes, AttributeIdentifiers.PRID,
        "http://id.elegnamnden.se/loa/1.0/eidas-sub", Instant.now(), "127.0.0.1"));
  }

}
