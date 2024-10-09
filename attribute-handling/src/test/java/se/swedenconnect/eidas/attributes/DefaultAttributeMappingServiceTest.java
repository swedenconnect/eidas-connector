/*
 * Copyright 2017-2024 Sweden Connect
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
package se.swedenconnect.eidas.attributes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.Attribute;
import se.swedenconnect.eidas.attributes.conversion.AttributeConverterConstants;
import se.swedenconnect.opensaml.eidas.ext.attributes.BirthNameType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CountryOfBirthType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentFamilyNameType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentGivenNameType;
import se.swedenconnect.opensaml.eidas.ext.attributes.DateOfBirthType;
import se.swedenconnect.opensaml.eidas.ext.attributes.PersonIdentifierType;
import se.swedenconnect.opensaml.eidas.ext.attributes.PlaceOfBirthType;
import se.swedenconnect.opensaml.saml2.attribute.AttributeUtils;
import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;
import se.swedenconnect.spring.saml.idp.attributes.RequestedAttribute;
import se.swedenconnect.spring.saml.idp.attributes.UserAttribute;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Test cases for DefaultAttributeMappingService.
 *
 * @author Martin Lindström
 */
public class DefaultAttributeMappingServiceTest extends OpenSamlTestBase {

  @Test
  void testConvert() {

    final DefaultAttributeMappingService service =
        new DefaultAttributeMappingService(AttributeConverterConstants.DEFAULT_CONVERTERS);

    // Person identifier
    //
    Attribute swAttr =
        AttributeConstants.ATTRIBUTE_TEMPLATE_EIDAS_PERSON_IDENTIFIER.createBuilder().value("ES/AT/02635542Y").build();

    Attribute eidasAttr = service.toEidasAttribute(swAttr);
    Assertions.assertNotNull(eidasAttr, "Expected eIDAS PersonIdentifier attribute");
    Assertions.assertEquals(EidasAttributeTemplateConstants.PERSON_IDENTIFIER_TEMPLATE.getName(), eidasAttr.getName());
    final PersonIdentifierType personIdentifier =
        AttributeUtils.getAttributeValue(eidasAttr, PersonIdentifierType.class);
    Assertions.assertEquals("ES/AT/02635542Y", personIdentifier.getValue());

    Attribute swAttr2 = service.toSwedishEidAttribute(eidasAttr);
    Assertions.assertNotNull(swAttr2, "Expected Swedish PersonIdentifier attribute");
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER, swAttr2.getName());
    Assertions.assertEquals("ES/AT/02635542Y", AttributeUtils.getAttributeStringValue(swAttr2));

    // Family name
    //
    swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_SN.createBuilder().value("Eriksson").build();

    eidasAttr = service.toEidasAttribute(swAttr);
    Assertions.assertNotNull(eidasAttr, "Expected eIDAS FamilyName attribute");
    Assertions.assertEquals(EidasAttributeTemplateConstants.CURRENT_FAMILY_NAME_TEMPLATE.getName(),
        eidasAttr.getName());
    final CurrentFamilyNameType currentFamilyName =
        AttributeUtils.getAttributeValue(eidasAttr, CurrentFamilyNameType.class);
    Assertions.assertEquals("Eriksson", currentFamilyName.getValue());
    Assertions.assertTrue(currentFamilyName.getLatinScript(), "Expected LatinScript for FamilyName attribute");

    swAttr2 = service.toSwedishEidAttribute(eidasAttr);
    Assertions.assertNotNull(swAttr2, "Expected Swedish FamilyName attribute");
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_SN, swAttr2.getName());
    Assertions.assertEquals("Eriksson", AttributeUtils.getAttributeStringValue(swAttr2));

    // Given name
    //
    swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_GIVEN_NAME.createBuilder().value("Kalle").build();

    eidasAttr = service.toEidasAttribute(swAttr);
    Assertions.assertNotNull(eidasAttr, "Expected eIDAS GivenName attribute");
    Assertions.assertEquals(EidasAttributeTemplateConstants.CURRENT_GIVEN_NAME_TEMPLATE.getName(), eidasAttr.getName());
    final CurrentGivenNameType currentGivenName =
        AttributeUtils.getAttributeValue(eidasAttr, CurrentGivenNameType.class);
    Assertions.assertEquals("Kalle", currentGivenName.getValue());
    Assertions.assertTrue(currentGivenName.getLatinScript(), "Expected LatinScript for GivenName attribute");

    swAttr2 = service.toSwedishEidAttribute(eidasAttr);
    Assertions.assertNotNull(swAttr2, "Expected Swedish GivenName attribute");
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_GIVEN_NAME, swAttr2.getName());
    Assertions.assertEquals("Kalle", AttributeUtils.getAttributeStringValue(swAttr2));

    // Date of birth
    //
    swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_DATE_OF_BIRTH.createBuilder().value("1965-09-22").build();

    eidasAttr = service.toEidasAttribute(swAttr);
    Assertions.assertNotNull(eidasAttr, "Expected eIDAS DateOfBirth attribute");
    Assertions.assertEquals(EidasAttributeTemplateConstants.DATE_OF_BIRTH_TEMPLATE.getName(), eidasAttr.getName());
    final DateOfBirthType dateOfBirth = AttributeUtils.getAttributeValue(eidasAttr, DateOfBirthType.class);
    Assertions.assertEquals("1965-09-22", dateOfBirth.toStringValue());

    swAttr2 = service.toSwedishEidAttribute(eidasAttr);
    Assertions.assertNotNull(swAttr2, "Expected Swedish DateOfBirth attribute");
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_DATE_OF_BIRTH, swAttr2.getName());
    Assertions.assertEquals("1965-09-22", AttributeUtils.getAttributeStringValue(swAttr2));

    // Birth name
    //
    swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_BIRTH_NAME.createBuilder().value("Kalle Karlsson").build();

    eidasAttr = service.toEidasAttribute(swAttr);
    Assertions.assertNotNull(eidasAttr, "Expected eIDAS BirthName attribute");
    Assertions.assertEquals(EidasAttributeTemplateConstants.BIRTH_NAME_TEMPLATE.getName(), eidasAttr.getName());
    final BirthNameType birthName = AttributeUtils.getAttributeValue(eidasAttr, BirthNameType.class);
    Assertions.assertEquals("Kalle Karlsson", birthName.getValue());
    Assertions.assertTrue(birthName.getLatinScript(), "Expected LatinScript for BirthName attribute");

    swAttr2 = service.toSwedishEidAttribute(eidasAttr);
    Assertions.assertNotNull(swAttr2, "Expected Swedish BirthName attribute");
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_BIRTH_NAME, swAttr2.getName());
    Assertions.assertEquals("Kalle Karlsson", AttributeUtils.getAttributeStringValue(swAttr2));

    // Place of birth
    //
    swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_PLACE_OF_BIRTH.createBuilder().value("Enköping").build();

    eidasAttr = service.toEidasAttribute(swAttr);
    Assertions.assertNotNull(eidasAttr, "Expected eIDAS PlaceOfBirth attribute");
    Assertions.assertEquals(EidasAttributeTemplateConstants.PLACE_OF_BIRTH_TEMPLATE.getName(), eidasAttr.getName());
    final PlaceOfBirthType placeOfBirth = AttributeUtils.getAttributeValue(eidasAttr, PlaceOfBirthType.class);
    Assertions.assertEquals("Enköping", placeOfBirth.getValue());

    swAttr2 = service.toSwedishEidAttribute(eidasAttr);
    Assertions.assertNotNull(swAttr2, "Expected Swedish PlaceOfBirth attribute");
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH, swAttr2.getName());
    Assertions.assertEquals("Enköping", AttributeUtils.getAttributeStringValue(swAttr2));

    // Phone number
    //
    swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_TELEPHONE_NUMBER.createBuilder().value("+46 111111").build();

    eidasAttr = service.toEidasAttribute(swAttr);
    Assertions.assertNotNull(eidasAttr, "Expected eIDAS PhoneNumber attribute");
    Assertions.assertEquals(EidasAttributeTemplateConstants.PHONE_NUMBER_TEMPLATE.getName(), eidasAttr.getName());
    final String number = AttributeUtils.getAttributeStringValue(eidasAttr);
    Assertions.assertEquals("+46 111111", number);

    swAttr2 = service.toSwedishEidAttribute(eidasAttr);
    Assertions.assertNotNull(swAttr2, "Expected Swedish TelephoneNumber attribute");
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_TELEPHONE_NUMBER, swAttr2.getName());
    Assertions.assertEquals("+46 111111", AttributeUtils.getAttributeStringValue(swAttr2));

    // Email address
    //
    swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_MAIL.createBuilder().value("user@example.com").build();

    eidasAttr = service.toEidasAttribute(swAttr);
    Assertions.assertNotNull(eidasAttr, "Expected eIDAS EmailAddress attribute");
    Assertions.assertEquals(EidasAttributeTemplateConstants.EMAIL_ADDRESS_TEMPLATE.getName(), eidasAttr.getName());
    final String mail = AttributeUtils.getAttributeStringValue(eidasAttr);
    Assertions.assertEquals("user@example.com", mail);

    swAttr2 = service.toSwedishEidAttribute(eidasAttr);
    Assertions.assertNotNull(swAttr2, "Expected Swedish Mail attribute");
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_MAIL, swAttr2.getName());
    Assertions.assertEquals("user@example.com", AttributeUtils.getAttributeStringValue(swAttr2));

    // Not supported
    swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_C.createBuilder().value("SE").build();
    Assertions.assertNull(service.toEidasAttribute(swAttr));
  }

  @Test
  void testConvertSpecial() {

    final DefaultAttributeMappingService service =
        new DefaultAttributeMappingService(AttributeConverterConstants.DEFAULT_CONVERTERS);

    // Person identifier
    //
    final PersonIdentifierType eidasPersonIdentifierValue =
        (PersonIdentifierType) XMLObjectSupport.buildXMLObject(PersonIdentifierType.TYPE_NAME);
    eidasPersonIdentifierValue.setValue("ES/AT/02635542Y");
    final Attribute eidasPersonIdentifier = EidasAttributeTemplateConstants.PERSON_IDENTIFIER_TEMPLATE.createBuilder()
        .value(eidasPersonIdentifierValue)
        .build();

    // Place of birth
    //
    final PlaceOfBirthType eidasPlaceOfBirthValue =
        (PlaceOfBirthType) XMLObjectSupport.buildXMLObject(PlaceOfBirthType.TYPE_NAME);
    eidasPlaceOfBirthValue.setValue("Enköping");
    final Attribute eidasPlaceOfBirth = EidasAttributeTemplateConstants.PLACE_OF_BIRTH_TEMPLATE.createBuilder()
        .value(eidasPlaceOfBirthValue)
        .build();

    // Country of birth
    //
    final CountryOfBirthType eidasCountryOfBirthValue =
        (CountryOfBirthType) XMLObjectSupport.buildXMLObject(CountryOfBirthType.TYPE_NAME);
    eidasCountryOfBirthValue.setValue("SE");
    final Attribute eidasCountryOfBirth = EidasAttributeTemplateConstants.COUNTRY_OF_BIRTH_TEMPLATE.createBuilder()
        .value(eidasCountryOfBirthValue)
        .build();

    // Town of birth
    //
    final Attribute eidasTownOfBirth = EidasAttributeTemplateConstants.TOWN_OF_BIRTH_TEMPLATE.createBuilder()
        .value("Enköping")
        .build();

    // Test 1: Assert that CountryOfBirth and TownOfBirth are ignored if PlaceOfBirth is set.
    //
    final List<Attribute> res1 = service.toSwedishEidAttributes(
        List.of(eidasPersonIdentifier, eidasPlaceOfBirth, eidasCountryOfBirth, eidasTownOfBirth));

    Assertions.assertEquals(2, res1.size());
    Assertions.assertTrue(
        res1.stream().anyMatch(a -> AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER.equals(a.getName())));
    Assertions.assertTrue(
        res1.stream().anyMatch(a -> AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH.equals(a.getName())));

    // 1b. The same with UserAttribute:s ...
    //
    final List<UserAttribute> res1b = service.toSwedishUserAttributes(
        Stream.of(eidasPersonIdentifier, eidasPlaceOfBirth, eidasCountryOfBirth, eidasTownOfBirth)
            .map(UserAttribute::new).toList());

    Assertions.assertEquals(2, res1b.size());
    Assertions.assertTrue(
        res1b.stream().anyMatch(a -> AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER.equals(a.getId())));
    Assertions.assertTrue(
        res1b.stream().anyMatch(a -> AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH.equals(a.getId())));

    // Test 2. If PlaceOfBirth is not set, we combine the values from CountryOfBirth and TownOfBirth.
    //
    final List<Attribute> res2 = service.toSwedishEidAttributes(
        List.of(eidasPersonIdentifier, eidasCountryOfBirth, eidasTownOfBirth));

    Assertions.assertEquals(2, res2.size());
    Assertions.assertTrue(
        res2.stream().anyMatch(a -> AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER.equals(a.getName())));
    Assertions.assertTrue(
        res2.stream().anyMatch(a -> AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH.equals(a.getName())));
    Assertions.assertEquals("Enköping, SE", res2.stream()
        .filter(a -> AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH.equals(a.getName()))
        .map(AttributeUtils::getAttributeStringValue)
        .findFirst()
        .orElse(null));

    // 2b. The same with UserAttribute:s ...
    //
    final List<UserAttribute> res2b = service.toSwedishUserAttributes(
        Stream.of(eidasPersonIdentifier, eidasCountryOfBirth, eidasTownOfBirth).map(UserAttribute::new).toList());

    Assertions.assertEquals(2, res2b.size());
    Assertions.assertTrue(
        res2b.stream().anyMatch(a -> AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER.equals(a.getId())));
    Assertions.assertTrue(
        res2b.stream().anyMatch(a -> AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH.equals(a.getId())));
    Assertions.assertEquals(List.of("Enköping, SE"), res2b.stream()
        .filter(a -> AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH.equals(a.getId()))
        .map(UserAttribute::getStringValues)
        .findFirst()
        .orElse(null));

    // 3 and 4. PlaceOfBirth is not set, one of CountryOfBirth and TownOfBirth is set
    //
    final List<Attribute> res3 = service.toSwedishEidAttributes(
        List.of(eidasPersonIdentifier, eidasTownOfBirth));

    Assertions.assertEquals(2, res3.size());
    Assertions.assertTrue(
        res3.stream().anyMatch(a -> AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER.equals(a.getName())));
    Assertions.assertTrue(
        res3.stream().anyMatch(a -> AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH.equals(a.getName())));
    Assertions.assertEquals("Enköping", res3.stream()
        .filter(a -> AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH.equals(a.getName()))
        .map(AttributeUtils::getAttributeStringValue)
        .findFirst()
        .orElse(null));

    final List<Attribute> res4 = service.toSwedishEidAttributes(
        List.of(eidasPersonIdentifier, eidasCountryOfBirth));

    Assertions.assertEquals(2, res4.size());
    Assertions.assertTrue(
        res4.stream().anyMatch(a -> AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER.equals(a.getName())));
    Assertions.assertTrue(
        res4.stream().anyMatch(a -> AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH.equals(a.getName())));
    Assertions.assertEquals("SE", res4.stream()
        .filter(a -> AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH.equals(a.getName()))
        .map(AttributeUtils::getAttributeStringValue)
        .findFirst()
        .orElse(null));
  }

  @Test
  void testToEidasRequestedAttribute() {
    final DefaultAttributeMappingService service =
        new DefaultAttributeMappingService(AttributeConverterConstants.DEFAULT_CONVERTERS);

    RequestedAttribute ra = new RequestedAttribute(
        AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH,
        AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_PLACE_OF_BIRTH,
        true);

    se.swedenconnect.opensaml.eidas.ext.RequestedAttribute eidasR = service.toEidasRequestedAttribute(ra);
    Assertions.assertEquals(EidasAttributeTemplateConstants.PLACE_OF_BIRTH_TEMPLATE.getName(), eidasR.getName());
    Assertions.assertEquals(EidasAttributeTemplateConstants.PLACE_OF_BIRTH_TEMPLATE.getFriendlyName(),
        eidasR.getFriendlyName());
    // Not in minimum data set
    Assertions.assertEquals(false, eidasR.isRequired());

    ra = new RequestedAttribute(
        AttributeConstants.ATTRIBUTE_NAME_GIVEN_NAME,
        AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_GIVEN_NAME,
        false);

    eidasR = service.toEidasRequestedAttribute(ra);
    Assertions.assertEquals(EidasAttributeTemplateConstants.CURRENT_GIVEN_NAME_TEMPLATE.getName(), eidasR.getName());
    Assertions.assertEquals(EidasAttributeTemplateConstants.CURRENT_GIVEN_NAME_TEMPLATE.getFriendlyName(),
        eidasR.getFriendlyName());
    // In minimum data set
    Assertions.assertEquals(true, eidasR.isRequired());

    // No mapping
    ra = new RequestedAttribute(
        AttributeConstants.ATTRIBUTE_NAME_C,
        AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_C,
        false);
    Assertions.assertNull(service.toEidasRequestedAttribute(ra));
  }

  @Test
  public void testToEidasRequestedAttributes() {

    final DefaultAttributeMappingService service =
        new DefaultAttributeMappingService(AttributeConverterConstants.DEFAULT_CONVERTERS);

    final List<RequestedAttribute> ras = List.of(
        new RequestedAttribute(
            AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH,
            AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_PLACE_OF_BIRTH,
            true),
        new RequestedAttribute(
            AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER,
            AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_EIDAS_PERSON_IDENTIFIER,
            true),
        new RequestedAttribute(
            AttributeConstants.ATTRIBUTE_NAME_GIVEN_NAME,
            AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_GIVEN_NAME,
            true),
        new RequestedAttribute(
            AttributeConstants.ATTRIBUTE_NAME_SN,
            AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_SN,
            true),

        // Special handling ...
        new RequestedAttribute(
            se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_COUNTRY_OF_BIRTH_ATTRIBUTE_NAME,
            se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_COUNTRY_OF_BIRTH_ATTRIBUTE_FRIENDLY_NAME,
            false),
        new RequestedAttribute(
            se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_NAME,
            se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_FRIENDLY_NAME,
            false),

        // No mapping
        new RequestedAttribute(
            AttributeConstants.ATTRIBUTE_NAME_C,
            AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_C,
            true));

    List<se.swedenconnect.opensaml.eidas.ext.RequestedAttribute> eidas = service.toEidasRequestedAttributes(ras, true);
    Assertions.assertEquals(7, eidas.size());

    Assertions.assertTrue(eidas.stream().anyMatch(a -> Objects.equals(a.getName(),
        EidasAttributeTemplateConstants.DATE_OF_BIRTH_TEMPLATE.getName())));

    Assertions.assertTrue(eidas.stream().anyMatch(a -> Objects.equals(a.getName(),
        EidasAttributeTemplateConstants.CURRENT_FAMILY_NAME_TEMPLATE.getName())));
    Assertions.assertTrue(eidas.stream().anyMatch(a -> Objects.equals(a.getName(),
        EidasAttributeTemplateConstants.CURRENT_GIVEN_NAME_TEMPLATE.getName())));
    Assertions.assertTrue(eidas.stream().anyMatch(a -> Objects.equals(a.getName(),
        EidasAttributeTemplateConstants.PERSON_IDENTIFIER_TEMPLATE.getName())));
    Assertions.assertTrue(eidas.stream().anyMatch(a -> Objects.equals(a.getName(),
        EidasAttributeTemplateConstants.PLACE_OF_BIRTH_TEMPLATE.getName())));
    Assertions.assertTrue(eidas.stream().anyMatch(a -> Objects.equals(a.getName(),
        EidasAttributeTemplateConstants.COUNTRY_OF_BIRTH_TEMPLATE.getName())));
    Assertions.assertTrue(eidas.stream().anyMatch(a -> Objects.equals(a.getName(),
        EidasAttributeTemplateConstants.TOWN_OF_BIRTH_TEMPLATE.getName())));

    eidas = service.toEidasRequestedAttributes(ras, false);
    Assertions.assertEquals(6, eidas.size());

    Assertions.assertTrue(eidas.stream().anyMatch(a -> Objects.equals(a.getName(),
        EidasAttributeTemplateConstants.CURRENT_FAMILY_NAME_TEMPLATE.getName())));
    Assertions.assertTrue(eidas.stream().anyMatch(a -> Objects.equals(a.getName(),
        EidasAttributeTemplateConstants.CURRENT_GIVEN_NAME_TEMPLATE.getName())));
    Assertions.assertTrue(eidas.stream().anyMatch(a -> Objects.equals(a.getName(),
        EidasAttributeTemplateConstants.PERSON_IDENTIFIER_TEMPLATE.getName())));
    Assertions.assertTrue(eidas.stream().anyMatch(a -> Objects.equals(a.getName(),
        EidasAttributeTemplateConstants.PLACE_OF_BIRTH_TEMPLATE.getName())));
    Assertions.assertTrue(eidas.stream().anyMatch(a -> Objects.equals(a.getName(),
        EidasAttributeTemplateConstants.COUNTRY_OF_BIRTH_TEMPLATE.getName())));
    Assertions.assertTrue(eidas.stream().anyMatch(a -> Objects.equals(a.getName(),
        EidasAttributeTemplateConstants.TOWN_OF_BIRTH_TEMPLATE.getName())));
  }

}
