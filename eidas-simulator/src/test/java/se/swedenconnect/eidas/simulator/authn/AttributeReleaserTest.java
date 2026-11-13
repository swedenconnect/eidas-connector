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
package se.swedenconnect.eidas.simulator.authn;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.NameIDType;
import se.swedenconnect.eidas.simulator.OpenSamlTestBase;
import se.swedenconnect.eidas.simulator.TestConnector;
import se.swedenconnect.eidas.simulator.TestCountries;
import se.swedenconnect.eidas.simulator.country.CountryRepository;
import se.swedenconnect.eidas.simulator.country.SimulatedCountry;
import se.swedenconnect.eidas.simulator.country.SimulatedUser;
import se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentAddressType;
import se.swedenconnect.opensaml.eidas.ext.attributes.DateOfBirthType;
import se.swedenconnect.opensaml.eidas.ext.attributes.GenderType;
import se.swedenconnect.opensaml.eidas.ext.attributes.PersonIdentifierType;
import se.swedenconnect.opensaml.eidas.ext.attributes.TransliterationStringType;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for {@link AttributeReleaser}.
 *
 * @author Martin Lindström
 */
class AttributeReleaserTest extends OpenSamlTestBase {

  private static CountryRepository countries;

  private static TestConnector connector;

  private final AttributeReleaser releaser = new AttributeReleaser();

  @BeforeAll
  static void setup() throws Exception {
    countries = TestCountries.repository();
    connector = new TestConnector();
  }

  @Test
  void testReleaseRequestedAndSupported() {
    // HR supports the four mandatory, BirthName, PlaceOfBirth, CurrentAddress, Gender and more
    final SimulatedCountry hr = countries.getCountry("HR");
    final SimulatedUser user = hr.getUsers().getFirst();
    final ProcessedRequest request = request(hr,
        attr(AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME, true),
        attr(AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME, true),
        attr(AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME, true),
        attr(AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME, true),
        attr(AttributeConstants.EIDAS_GENDER_ATTRIBUTE_NAME, false),
        attr(AttributeConstants.EIDAS_CURRENT_ADDRESS_ATTRIBUTE_NAME, false),
        attr(AttributeConstants.EIDAS_EMAIL_ADDRESS_ATTRIBUTE_NAME, false),
        attr(AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_NAME, false));

    final List<Attribute> attributes = this.releaser.releaseAttributes(hr, request, user);

    assertThat(attributes).extracting(Attribute::getName).containsExactly(
        AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME,
        AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME,
        AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME,
        AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME,
        AttributeConstants.EIDAS_CURRENT_ADDRESS_ATTRIBUTE_NAME,
        AttributeConstants.EIDAS_GENDER_ATTRIBUTE_NAME,
        AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_NAME,
        AttributeConstants.EIDAS_EMAIL_ADDRESS_ATTRIBUTE_NAME);
    assertThat(attributes).allMatch(a -> Attribute.URI_REFERENCE.equals(a.getNameFormat()));

    assertThat(value(attributes, AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME, PersonIdentifierType.class)
        .getValue()).isEqualTo("HR/SE/" + user.getPersonIdentifier());
    assertThat(value(attributes, AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME, DateOfBirthType.class)
        .getDate()).isEqualTo(LocalDate.parse(user.getDateOfBirth()));
    assertThat(value(attributes, AttributeConstants.EIDAS_GENDER_ATTRIBUTE_NAME, GenderType.class)
        .getGender().getValue()).isEqualTo("Male");
    assertThat(value(attributes, AttributeConstants.EIDAS_CURRENT_ADDRESS_ATTRIBUTE_NAME, CurrentAddressType.class)
        .getThoroughfare()).isEqualTo("Ilica");
    assertThat(value(attributes, AttributeConstants.EIDAS_EMAIL_ADDRESS_ATTRIBUTE_NAME, XSString.class).getValue())
        .isEqualTo(user.getEmailAddress());
  }

  @Test
  void testUnsupportedAndMissingValuesAreNotReleased() {
    // EE supports only the mandatory attributes
    final SimulatedCountry ee = countries.getCountry("EE");
    final SimulatedUser user = ee.getUsers().getFirst().copy();
    user.setGender("Male");
    final ProcessedRequest request = request(ee,
        attr(AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME, true),
        attr(AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME, true),
        attr(AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME, true),
        attr(AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME, true),
        attr(AttributeConstants.EIDAS_GENDER_ATTRIBUTE_NAME, false));
    assertThat(this.releaser.releaseAttributes(ee, request, user)).extracting(Attribute::getName)
        .doesNotContain(AttributeConstants.EIDAS_GENDER_ATTRIBUTE_NAME);

    // HR supports BirthName, but the user has none
    final SimulatedCountry hr = countries.getCountry("HR");
    final SimulatedUser marko = hr.getUser("marko");
    final ProcessedRequest hrRequest = request(hr,
        attr(AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME, true),
        attr(AttributeConstants.EIDAS_BIRTH_NAME_ATTRIBUTE_NAME, false));
    assertThat(this.releaser.releaseAttributes(hr, hrRequest, marko)).extracting(Attribute::getName)
        .containsExactly(AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME);

    // Attributes that are not requested are not released
    assertThat(this.releaser.getReleasableAttributes(hr, hrRequest)).containsExactly(
        AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME, AttributeConstants.EIDAS_BIRTH_NAME_ATTRIBUTE_NAME);
  }

  @Test
  void testMissingRequiredAttributes() {
    final SimulatedCountry hr = countries.getCountry("HR");
    final SimulatedUser user = hr.getUsers().getFirst().copy();
    final ProcessedRequest request = request(hr,
        attr(AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME, true),
        attr(AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME, true),
        attr(AttributeConstants.EIDAS_BIRTH_NAME_ATTRIBUTE_NAME, false));
    assertThat(this.releaser.getMissingRequiredAttributes(hr, request, user)).isEmpty();

    user.setPersonIdentifier(" ");
    user.setDateOfBirth("not a date");
    user.setBirthName(null);
    assertThat(this.releaser.getMissingRequiredAttributes(hr, request, user)).containsExactly(
        AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME, AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME);
  }

  @Test
  void testNonLatinNames() {
    final SimulatedCountry cy = countries.getCountry("CY");
    final SimulatedUser user = cy.getUser("georgios");
    final ProcessedRequest request = request(cy,
        attr(AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME, true),
        attr(AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME, true));

    final List<Attribute> attributes = this.releaser.releaseAttributes(cy, request, user);
    final Attribute familyName = attributes.stream()
        .filter(a -> AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME.equals(a.getName()))
        .findFirst().orElseThrow();
    assertThat(familyName.getAttributeValues()).hasSize(2);
    final TransliterationStringType latin = (TransliterationStringType) familyName.getAttributeValues().get(0);
    final TransliterationStringType greek = (TransliterationStringType) familyName.getAttributeValues().get(1);
    assertThat(latin.getValue()).isEqualTo("Papadopoulos");
    assertThat(latin.getLatinScript()).isTrue();
    assertThat(greek.getValue()).isEqualTo("Παπαδόπουλος");
    assertThat(greek.getLatinScript()).isFalse();
  }

  @Test
  void testCustomUser() {
    // A user created by the tester, with only some values
    final SimulatedCountry pt = countries.getCountry("PT");
    final SimulatedUser user = new SimulatedUser();
    user.setPersonIdentifier("99999999");
    user.setGivenName("Ana");
    user.setFamilyName("Costa");
    user.setDateOfBirth("2000-02-29");
    final ProcessedRequest request = request(pt,
        attr(AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME, true),
        attr(AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME, true),
        attr(AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME, true),
        attr(AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME, true),
        attr(AttributeConstants.EIDAS_CURRENT_ADDRESS_ATTRIBUTE_NAME, false));
    assertThat(this.releaser.releaseAttributes(pt, request, user)).hasSize(4);
    assertThat(this.releaser.getMissingRequiredAttributes(pt, request, user)).isEmpty();
  }

  private static ProcessedRequest.RequestedAttribute attr(final String name, final boolean required) {
    return new ProcessedRequest.RequestedAttribute(name, required);
  }

  private static ProcessedRequest request(final SimulatedCountry country,
      final ProcessedRequest.RequestedAttribute... attributes) {
    return new ProcessedRequest("id", country.getCode(),
        new ResponseTarget(connector.createMetadata(), TestConnector.ACS_URL, "_id", null), "public", null, null,
        Arrays.asList(attributes), NameIDType.PERSISTENT, List.of("http://eidas.europa.eu/LoA/high"), "SE");
  }

  private static <T extends XMLObject> T value(final List<Attribute> attributes, final String name,
      final Class<T> type) {
    return attributes.stream()
        .filter(a -> name.equals(a.getName()))
        .map(a -> type.cast(a.getAttributeValues().getFirst()))
        .findFirst()
        .orElseThrow();
  }

}
