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

import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.Attribute;
import se.swedenconnect.eidas.connector.OpenSamlTestBase;
import se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants;
import se.swedenconnect.opensaml.eidas.ext.attributes.CountryOfBirthType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentAddressType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentFamilyNameType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentGivenNameType;
import se.swedenconnect.opensaml.eidas.ext.attributes.DateOfBirthType;
import se.swedenconnect.opensaml.eidas.ext.attributes.GenderType;
import se.swedenconnect.opensaml.eidas.ext.attributes.GenderTypeEnumeration;
import se.swedenconnect.opensaml.eidas.ext.attributes.NationalityType;
import se.swedenconnect.opensaml.eidas.ext.attributes.PersonIdentifierType;
import se.swedenconnect.opensaml.eidas.ext.attributes.PlaceOfBirthType;
import se.swedenconnect.opensaml.saml2.attribute.AttributeBuilder;
import se.swedenconnect.spring.authnserver.attributes.AttributeIdentifiers;
import se.swedenconnect.spring.authnserver.attributes.GenericAttribute;
import se.swedenconnect.spring.authnserver.saml.attributes.SamlAttributeValues;
import se.swedenconnect.spring.authnserver.saml.attributes.eidas.EidasNaturalPersonAttributes;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EidasAttributeConverter}. The released values are those of the Swedish SAML attributes that the
 * connector puts in the assertion, and they are the same as in version 2.1.0 of the connector.
 *
 * @author Martin Lindström
 */
class EidasAttributeConverterTest extends OpenSamlTestBase {

  private final EidasAttributeConverter converter = new EidasAttributeConverter();

  @Test
  void testMinimumDataSet() {
    final Map<String, List<String>> released = this.release(List.of(
        personIdentifier("ES/SE/1234"),
        transliterated(CurrentGivenNameType.class, AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME,
            "Kalle"),
        transliterated(CurrentFamilyNameType.class, AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME,
            "Kula"),
        dateOfBirth(LocalDate.of(1969, 3, 17))));

    assertThat(released).containsEntry(
        se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER,
        List.of("ES/SE/1234"));
    assertThat(released).containsEntry(
        se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_GIVEN_NAME,
        List.of("Kalle"));
    assertThat(released).containsEntry(
        se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_SN, List.of("Kula"));
    assertThat(released).containsEntry(
        se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_DATE_OF_BIRTH,
        List.of("1969-03-17"));
  }

  @Test
  void testTransliteration() {
    // The Latin script value is used, even if it does not come first ...
    final Attribute givenName = transliterated(CurrentGivenNameType.class,
        AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME, "Κώστας", "Kostas");
    ((CurrentGivenNameType) givenName.getAttributeValues().getFirst()).setLatinScript(false);

    // A single value is used, whatever its script ...
    final Attribute familyName = transliterated(CurrentFamilyNameType.class,
        AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME, "Παπαδόπουλος");
    ((CurrentFamilyNameType) familyName.getAttributeValues().getFirst()).setLatinScript(false);

    final Map<String, List<String>> released = this.release(List.of(givenName, familyName));
    assertThat(released).containsEntry(
        se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_GIVEN_NAME,
        List.of("Kostas"));
    assertThat(released).containsEntry(
        se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_SN,
        List.of("Παπαδόπουλος"));
  }

  @Test
  void testGender() {
    assertThat(this.releasedGender(GenderTypeEnumeration.MALE)).isEqualTo("M");
    assertThat(this.releasedGender(GenderTypeEnumeration.FEMALE)).isEqualTo("F");
    assertThat(this.releasedGender(GenderTypeEnumeration.UNSPECIFIED)).isEqualTo("U");
  }

  @Test
  void testCurrentAddress() {
    final CurrentAddressType address =
        AttributeBuilder.createValueObject(CurrentAddressType.TYPE_NAME, CurrentAddressType.class);
    address.setLocatorDesignator("22");
    address.setThoroughfare("Arcacia Avenue");
    address.setPostName("London");
    address.setPostCode("SW1A 1AA");
    address.setAdminunitFirstline("UK");
    final Attribute attribute = AttributeBuilder.builder(AttributeConstants.EIDAS_CURRENT_ADDRESS_ATTRIBUTE_NAME)
        .value(address)
        .build();

    final Map<String, List<String>> released = this.release(List.of(attribute));
    assertThat(released).containsEntry(
        se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_EIDAS_NATURAL_PERSON_ADDRESS,
        List.of(address.toSwedishEidString()));
  }

  @Test
  void testPlaceOfBirthHasPrecedence() {
    final List<GenericAttribute<?>> attributes = this.converter.toGenericAttributes(List.of(
        placeOfBirth("Enköping"), countryOfBirth("SE"), townOfBirth("Uppsala")));
    final Map<String, List<String>> released = this.toSaml(attributes);

    assertThat(released).containsEntry(
        se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH,
        List.of("Enköping"));
    assertThat(identifiers(attributes)).doesNotContain(
        AttributeIdentifiers.PLACE_OF_BIRTH_LOCALITY, AttributeIdentifiers.PLACE_OF_BIRTH_COUNTRY);
  }

  @Test
  void testPlaceOfBirthFromTownAndCountry() {
    assertThat(this.release(List.of(countryOfBirth("SE"), townOfBirth("Enköping")))).containsEntry(
        se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH,
        List.of("Enköping, SE"));
    assertThat(this.release(List.of(townOfBirth("Enköping")))).containsEntry(
        se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH,
        List.of("Enköping"));
    assertThat(this.release(List.of(countryOfBirth("SE")))).containsEntry(
        se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH,
        List.of("SE"));
  }

  @Test
  void testTownAndCountryOfBirthByEidasName() {
    final Map<String, List<String>> released = this.release(List.of(countryOfBirth("SE"), townOfBirth("Enköping")));
    assertThat(released).containsEntry(AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_NAME, List.of("Enköping"));
    assertThat(released).containsEntry(AttributeConstants.EIDAS_COUNTRY_OF_BIRTH_ATTRIBUTE_NAME, List.of("SE"));
  }

  @Test
  void testEJusticeNaturalPersonRole() {
    final Attribute attribute = AttributeBuilder.builder(AttributeConstants.EJUSTICE_NATURAL_PERSON_ROLE_ATTRIBUTE_NAME)
        .friendlyName(AttributeConstants.EJUSTICE_NATURAL_PERSON_ROLE_ATTRIBUTE_FRIENDLY_NAME)
        .value("VIP1")
        .value("VIP2")
        .build();
    final List<Attribute> released = ConnectorAttributes.createSamlAttributeMapping()
        .toSaml(new ArrayList<>(this.converter.toGenericAttributes(List.of(attribute))), null);

    assertThat(released).hasSize(1);
    assertThat(released.getFirst().getName())
        .isEqualTo(AttributeConstants.EJUSTICE_NATURAL_PERSON_ROLE_ATTRIBUTE_NAME);
    assertThat(released.getFirst().getFriendlyName())
        .isEqualTo(AttributeConstants.EJUSTICE_NATURAL_PERSON_ROLE_ATTRIBUTE_FRIENDLY_NAME);
    assertThat(SamlAttributeValues.getStringValues(released.getFirst())).containsExactly("VIP1", "VIP2");
  }

  @Test
  void testFirstValueOfNationality() {
    final NationalityType swedish =
        AttributeBuilder.createValueObject(NationalityType.TYPE_NAME, NationalityType.class);
    swedish.setValue("SE");
    final NationalityType no = AttributeBuilder.createValueObject(NationalityType.TYPE_NAME, NationalityType.class);
    no.setValue("NO");
    final Attribute attribute = AttributeBuilder.builder(AttributeConstants.EIDAS_NATIONALITY_ATTRIBUTE_NAME)
        .value(swedish)
        .value(no)
        .build();

    assertThat(this.release(List.of(attribute))).containsEntry(
        se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_COUNTRY_OF_CITIZENSHIP,
        List.of("SE"));
  }

  @Test
  void testStringAttributes() {
    final Map<String, List<String>> released = this.release(List.of(
        AttributeBuilder.builder(AttributeConstants.EIDAS_PHONE_NUMBER_ATTRIBUTE_NAME).value("+46701234567").build(),
        AttributeBuilder.builder(AttributeConstants.EIDAS_EMAIL_ADDRESS_ATTRIBUTE_NAME)
            .value("user@example.com").build()));

    assertThat(released).containsEntry(
        se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_TELEPHONE_NUMBER,
        List.of("+46701234567"));
    assertThat(released).containsEntry(
        se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_MAIL,
        List.of("user@example.com"));
  }

  @Test
  void testUnknownAttributeIsIgnored() {
    final Attribute attribute = AttributeBuilder.builder("http://eidas.europa.eu/attributes/legalperson/LegalName")
        .value("ACME")
        .build();
    assertThat(this.converter.toGenericAttributes(List.of(attribute))).isEmpty();
  }

  private String releasedGender(final GenderTypeEnumeration gender) {
    final GenderType value = AttributeBuilder.createValueObject(GenderType.TYPE_NAME, GenderType.class);
    value.setGender(gender);
    final Attribute attribute =
        AttributeBuilder.builder(AttributeConstants.EIDAS_GENDER_ATTRIBUTE_NAME).value(value).build();
    return this.release(List.of(attribute))
        .get(se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_GENDER)
        .getFirst();
  }

  private Map<String, List<String>> release(final List<Attribute> eidasAttributes) {
    return this.toSaml(this.converter.toGenericAttributes(eidasAttributes));
  }

  private Map<String, List<String>> toSaml(final List<GenericAttribute<?>> attributes) {
    final List<GenericAttribute<? extends Serializable>> input = new ArrayList<>(attributes);
    return ConnectorAttributes.createSamlAttributeMapping().toSaml(input, null).stream()
        .collect(Collectors.toMap(Attribute::getName, SamlAttributeValues::getStringValues));
  }

  private static List<String> identifiers(final List<GenericAttribute<?>> attributes) {
    return attributes.stream().map(GenericAttribute::getIdentifier).toList();
  }

  private static Attribute personIdentifier(final String id) {
    final PersonIdentifierType value =
        AttributeBuilder.createValueObject(PersonIdentifierType.TYPE_NAME, PersonIdentifierType.class);
    value.setValue(id);
    return AttributeBuilder.builder(AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME).value(value).build();
  }

  private static Attribute dateOfBirth(final LocalDate date) {
    final DateOfBirthType value =
        AttributeBuilder.createValueObject(DateOfBirthType.TYPE_NAME, DateOfBirthType.class);
    value.setDate(date);
    return AttributeBuilder.builder(AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME).value(value).build();
  }

  private static <T extends XSString> Attribute transliterated(
      final Class<T> type, final String name, final String... values) {
    final AttributeBuilder builder = AttributeBuilder.builder(name);
    for (final String v : values) {
      final T value = AttributeBuilder.createValueObject(
          type == CurrentGivenNameType.class ? CurrentGivenNameType.TYPE_NAME : CurrentFamilyNameType.TYPE_NAME,
          type);
      value.setValue(v);
      builder.value((XMLObject) value);
    }
    return builder.build();
  }

  private static Attribute placeOfBirth(final String place) {
    final PlaceOfBirthType value =
        AttributeBuilder.createValueObject(PlaceOfBirthType.TYPE_NAME, PlaceOfBirthType.class);
    value.setValue(place);
    return AttributeBuilder.builder(AttributeConstants.EIDAS_PLACE_OF_BIRTH_ATTRIBUTE_NAME).value(value).build();
  }

  private static Attribute countryOfBirth(final String country) {
    final CountryOfBirthType value =
        AttributeBuilder.createValueObject(CountryOfBirthType.TYPE_NAME, CountryOfBirthType.class);
    value.setValue(country);
    return AttributeBuilder.builder(EidasNaturalPersonAttributes.COUNTRY_OF_BIRTH.name()).value(value).build();
  }

  private static Attribute townOfBirth(final String town) {
    return AttributeBuilder.builder(EidasNaturalPersonAttributes.TOWN_OF_BIRTH.name()).value(town).build();
  }

}
