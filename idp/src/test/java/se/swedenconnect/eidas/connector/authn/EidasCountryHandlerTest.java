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
import se.swedenconnect.spring.authnserver.attributes.AttributeIdentifiers;
import se.swedenconnect.spring.authnserver.attributes.GenericRequestedAttribute;
import se.swedenconnect.spring.authnserver.authentication.AuthenticationRequirements;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EidasCountryHandler#getRequestedCountries(AuthenticationRequirements, String)}.
 *
 * @author Martin Lindström
 */
class EidasCountryHandlerTest {

  @Test
  void testNoRequestedCountry() {
    assertThat(EidasCountryHandler.getRequestedCountries(new AuthenticationRequirements(), "test")).isEmpty();
  }

  @Test
  void testRequestedAuthnProvider() {
    final AuthenticationRequirements requirements = new AuthenticationRequirements();
    requirements.setRequestedAuthnProviders(List.of(EidasCountryHandler.COUNTRY_URI_PREFIX + "de",
        "https://other.example.com", EidasCountryHandler.COUNTRY_URI_PREFIX + "FR"));
    assertThat(EidasCountryHandler.getRequestedCountries(requirements, "test")).containsExactly("DE", "FR");
  }

  @Test
  void testRequestedEidasCountryClaim() {
    final AuthenticationRequirements requirements = new AuthenticationRequirements();
    requirements.setRequestedAttributes(List.of(
        new GenericRequestedAttribute(AttributeIdentifiers.EIDAS_COUNTRY, true, List.of("dk"), null)));
    requirements.setRequestedAuthnProviders(List.of(EidasCountryHandler.COUNTRY_URI_PREFIX + "FR"));
    assertThat(EidasCountryHandler.getRequestedCountries(requirements, "test")).containsExactly("DK");
  }

  @Test
  void testPrincipalSelection() {
    final AuthenticationRequirements requirements = new AuthenticationRequirements();
    requirements.setRequestedAttributes(List.of(
        new GenericRequestedAttribute(AttributeIdentifiers.COUNTRY, false, List.of("NO"), null)));
    assertThat(EidasCountryHandler.getRequestedCountries(requirements, "test")).containsExactly("NO");
  }

}
