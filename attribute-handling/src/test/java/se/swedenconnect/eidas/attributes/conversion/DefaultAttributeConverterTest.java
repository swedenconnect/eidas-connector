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
package se.swedenconnect.eidas.attributes.conversion;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Attribute;

import se.litsec.eidas.opensaml.ext.attributes.BirthNameType;
import se.litsec.eidas.opensaml.ext.attributes.CurrentFamilyNameType;
import se.litsec.eidas.opensaml.ext.attributes.CurrentGivenNameType;
import se.litsec.eidas.opensaml.ext.attributes.DateOfBirthType;
import se.litsec.eidas.opensaml.ext.attributes.EidasAttributeValueType;
import se.litsec.eidas.opensaml.ext.attributes.PersonIdentifierType;
import se.litsec.eidas.opensaml.ext.attributes.PlaceOfBirthType;
import se.swedenconnect.eidas.attributes.EidasAttributeTemplateConstants;
import se.swedenconnect.eidas.attributes.OpenSamlTestBase;
import se.swedenconnect.opensaml.saml2.attribute.AttributeUtils;
import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;

/**
 * Test cases for DefaultAttributeConverter.
 *
 * @author Martin Lindström
 */
public class DefaultAttributeConverterTest extends OpenSamlTestBase {

  @Test
  public void testConvert() throws Exception {
    final DefaultAttributeConverter converter = new DefaultAttributeConverter(List.of(
        AttributeTemplatePair.of(EidasAttributeTemplateConstants.PERSON_IDENTIFIER_TEMPLATE,
            AttributeConstants.ATTRIBUTE_TEMPLATE_EIDAS_PERSON_IDENTIFIER),
        AttributeTemplatePair.of(EidasAttributeTemplateConstants.CURRENT_FAMILY_NAME_TEMPLATE,
            AttributeConstants.ATTRIBUTE_TEMPLATE_SN),
        AttributeTemplatePair.of(EidasAttributeTemplateConstants.CURRENT_GIVEN_NAME_TEMPLATE,
            AttributeConstants.ATTRIBUTE_TEMPLATE_GIVEN_NAME),
        AttributeTemplatePair.of(EidasAttributeTemplateConstants.DATE_OF_BIRTH_TEMPLATE,
            AttributeConstants.ATTRIBUTE_TEMPLATE_DATE_OF_BIRTH),
        AttributeTemplatePair.of(EidasAttributeTemplateConstants.BIRTH_NAME_TEMPLATE,
            AttributeConstants.ATTRIBUTE_TEMPLATE_BIRTH_NAME),
        AttributeTemplatePair.of(EidasAttributeTemplateConstants.PLACE_OF_BIRTH_TEMPLATE,
            AttributeConstants.ATTRIBUTE_TEMPLATE_PLACE_OF_BIRTH)));

    // Person identifier
    //
    Attribute swAttr =
        AttributeConstants.ATTRIBUTE_TEMPLATE_EIDAS_PERSON_IDENTIFIER.createBuilder().value("ES/AT/02635542Y").build();

    Assertions.assertTrue(converter.supportsConversionToEidas(swAttr.getName()));

    Attribute eidasAttr = converter.toEidasAttribute(swAttr);
    Assertions.assertNotNull(eidasAttr, "Expected eIDAS PersonIdentifier attribute");
    Assertions.assertEquals(EidasAttributeTemplateConstants.PERSON_IDENTIFIER_TEMPLATE.getName(), eidasAttr.getName());
    final PersonIdentifierType personIdentifier =
        AttributeUtils.getAttributeValue(eidasAttr, PersonIdentifierType.class);
    Assertions.assertEquals("ES/AT/02635542Y", personIdentifier.getValue());

    Assertions.assertTrue(converter.supportsConversionToSwedishAttribute(eidasAttr.getName()));

    Attribute swAttr2 = converter.toSwedishEidAttribute(eidasAttr);
    Assertions.assertNotNull(swAttr2, "Expected Swedish PersonIdentifier attribute");
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER, swAttr2.getName());
    Assertions.assertEquals("ES/AT/02635542Y", AttributeUtils.getAttributeStringValue(swAttr2));

    // Family name
    //
    swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_SN.createBuilder().value("Eriksson").build();

    Assertions.assertTrue(converter.supportsConversionToEidas(swAttr.getName()));

    eidasAttr = converter.toEidasAttribute(swAttr);
    Assertions.assertNotNull(eidasAttr, "Expected eIDAS FamilyName attribute");
    Assertions.assertEquals(EidasAttributeTemplateConstants.CURRENT_FAMILY_NAME_TEMPLATE.getName(),
        eidasAttr.getName());
    final CurrentFamilyNameType currentFamilyName =
        AttributeUtils.getAttributeValue(eidasAttr, CurrentFamilyNameType.class);
    Assertions.assertEquals("Eriksson", currentFamilyName.getValue());
    Assertions.assertTrue(currentFamilyName.getLatinScript(), "Expected LatinScript for FamilyName attribute");

    Assertions.assertTrue(converter.supportsConversionToSwedishAttribute(eidasAttr.getName()));
    swAttr2 = converter.toSwedishEidAttribute(eidasAttr);
    Assertions.assertNotNull(swAttr2, "Expected Swedish FamilyName attribute");
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_SN, swAttr2.getName());
    Assertions.assertEquals("Eriksson", AttributeUtils.getAttributeStringValue(swAttr2));

    // Given name
    //
    swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_GIVEN_NAME.createBuilder().value("Kalle").build();

    Assertions.assertTrue(converter.supportsConversionToEidas(swAttr.getName()));

    eidasAttr = converter.toEidasAttribute(swAttr);
    Assertions.assertNotNull(eidasAttr, "Expected eIDAS GivenName attribute");
    Assertions.assertEquals(EidasAttributeTemplateConstants.CURRENT_GIVEN_NAME_TEMPLATE.getName(), eidasAttr.getName());
    final CurrentGivenNameType currentGivenName =
        AttributeUtils.getAttributeValue(eidasAttr, CurrentGivenNameType.class);
    Assertions.assertEquals("Kalle", currentGivenName.getValue());
    Assertions.assertTrue(currentGivenName.getLatinScript(), "Expected LatinScript for GivenName attribute");

    Assertions.assertTrue(converter.supportsConversionToSwedishAttribute(eidasAttr.getName()));
    swAttr2 = converter.toSwedishEidAttribute(eidasAttr);
    Assertions.assertNotNull(swAttr2, "Expected Swedish GivenName attribute");
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_GIVEN_NAME, swAttr2.getName());
    Assertions.assertEquals("Kalle", AttributeUtils.getAttributeStringValue(swAttr2));

    // Date of birth
    //
    swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_DATE_OF_BIRTH.createBuilder().value("1965-09-22").build();

    Assertions.assertTrue(converter.supportsConversionToEidas(swAttr.getName()));

    eidasAttr = converter.toEidasAttribute(swAttr);
    Assertions.assertNotNull(eidasAttr, "Expected eIDAS DateOfBirth attribute");
    Assertions.assertEquals(EidasAttributeTemplateConstants.DATE_OF_BIRTH_TEMPLATE.getName(), eidasAttr.getName());
    final DateOfBirthType dateOfBirth = AttributeUtils.getAttributeValue(eidasAttr, DateOfBirthType.class);
    Assertions.assertEquals("1965-09-22", dateOfBirth.toStringValue());

    Assertions.assertTrue(converter.supportsConversionToSwedishAttribute(eidasAttr.getName()));
    swAttr2 = converter.toSwedishEidAttribute(eidasAttr);
    Assertions.assertNotNull(swAttr2, "Expected Swedish DateOfBirth attribute");
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_DATE_OF_BIRTH, swAttr2.getName());
    Assertions.assertEquals("1965-09-22", AttributeUtils.getAttributeStringValue(swAttr2));

    // Birth name
    //
    swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_BIRTH_NAME.createBuilder().value("Kalle Karlsson").build();

    Assertions.assertTrue(converter.supportsConversionToEidas(swAttr.getName()));

    eidasAttr = converter.toEidasAttribute(swAttr);
    Assertions.assertNotNull(eidasAttr, "Expected eIDAS BirthName attribute");
    Assertions.assertEquals(EidasAttributeTemplateConstants.BIRTH_NAME_TEMPLATE.getName(), eidasAttr.getName());
    final BirthNameType birthName = AttributeUtils.getAttributeValue(eidasAttr, BirthNameType.class);
    Assertions.assertEquals("Kalle Karlsson", birthName.getValue());
    Assertions.assertTrue(birthName.getLatinScript(), "Expected LatinScript for BirthName attribute");

    Assertions.assertTrue(converter.supportsConversionToSwedishAttribute(eidasAttr.getName()));
    swAttr2 = converter.toSwedishEidAttribute(eidasAttr);
    Assertions.assertNotNull(swAttr2, "Expected Swedish BirthName attribute");
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_BIRTH_NAME, swAttr2.getName());
    Assertions.assertEquals("Kalle Karlsson", AttributeUtils.getAttributeStringValue(swAttr2));

    // Place of birth
    //
    swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_PLACE_OF_BIRTH.createBuilder().value("Enköping").build();

    Assertions.assertTrue(converter.supportsConversionToEidas(swAttr.getName()));

    eidasAttr = converter.toEidasAttribute(swAttr);
    Assertions.assertNotNull(eidasAttr, "Expected eIDAS PlaceOfBirth attribute");
    Assertions.assertEquals(EidasAttributeTemplateConstants.PLACE_OF_BIRTH_TEMPLATE.getName(), eidasAttr.getName());
    final PlaceOfBirthType placeOfBirth = AttributeUtils.getAttributeValue(eidasAttr, PlaceOfBirthType.class);
    Assertions.assertEquals("Enköping", placeOfBirth.getValue());

    Assertions.assertTrue(converter.supportsConversionToSwedishAttribute(eidasAttr.getName()));
    swAttr2 = converter.toSwedishEidAttribute(eidasAttr);
    Assertions.assertNotNull(swAttr2, "Expected Swedish PlaceOfBirth attribute");
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH, swAttr2.getName());
    Assertions.assertEquals("Enköping", AttributeUtils.getAttributeStringValue(swAttr2));
  }

  @Test
  public void testNotSupported() {
    final DefaultAttributeConverter converter = new DefaultAttributeConverter(List.of(
        AttributeTemplatePair.of(EidasAttributeTemplateConstants.PERSON_IDENTIFIER_TEMPLATE,
            AttributeConstants.ATTRIBUTE_TEMPLATE_EIDAS_PERSON_IDENTIFIER),
        AttributeTemplatePair.of(EidasAttributeTemplateConstants.CURRENT_FAMILY_NAME_TEMPLATE,
            AttributeConstants.ATTRIBUTE_TEMPLATE_SN)));

    final Attribute swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_GIVEN_NAME.createBuilder().value("Kalle").build();
    Assertions.assertFalse(converter.supportsConversionToEidas(swAttr.getName()));

    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      converter.toSwedishEidAttribute(swAttr);
    });

    final Attribute eidasAttr = EidasAttributeTemplateConstants.CURRENT_GIVEN_NAME_TEMPLATE.createBuilder()
        .value("Carlos").build();

    Assertions.assertFalse(converter.supportsConversionToSwedishAttribute(eidasAttr.getName()));

    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      converter.toEidasAttribute(eidasAttr);
    });
  }

  @Test
  public void testEmptyEidasAttribute() {
    final DefaultAttributeConverter converter = new DefaultAttributeConverter(
        EidasAttributeTemplateConstants.CURRENT_FAMILY_NAME_TEMPLATE, AttributeConstants.ATTRIBUTE_TEMPLATE_SN);

    final Attribute eidasAttr = EidasAttributeTemplateConstants.CURRENT_FAMILY_NAME_TEMPLATE.createBuilder().build();

    Assertions.assertTrue(converter.supportsConversionToSwedishAttribute(eidasAttr.getName()));

    Assertions.assertNull(converter.toSwedishEidAttribute(eidasAttr), "Expected null since attribute has no value");
  }

  @Test
  public void testEmptySwedishAttribute() {
    final DefaultAttributeConverter converter = new DefaultAttributeConverter(
        EidasAttributeTemplateConstants.CURRENT_FAMILY_NAME_TEMPLATE, AttributeConstants.ATTRIBUTE_TEMPLATE_SN);

    final Attribute swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_SN.createBuilder().build();

    Assertions.assertTrue(converter.supportsConversionToEidas(swAttr.getName()));

    Assertions.assertNull(converter.toEidasAttribute(swAttr), "Expected null since attribute has no value");
  }

  @Test
  public void testMultivaluedEidasAttribute() {
    final DefaultAttributeConverter converter = new DefaultAttributeConverter(
        EidasAttributeTemplateConstants.CURRENT_FAMILY_NAME_TEMPLATE, AttributeConstants.ATTRIBUTE_TEMPLATE_SN);

    final XMLObject eidasAttributeValue1 =
        EidasAttributeTemplateConstants.CURRENT_FAMILY_NAME_TEMPLATE.createAttributeValueObject();
    ((EidasAttributeValueType) eidasAttributeValue1).parseStringValue("Smith");

    final XMLObject eidasAttributeValue2 =
        EidasAttributeTemplateConstants.CURRENT_FAMILY_NAME_TEMPLATE.createAttributeValueObject();
    ((EidasAttributeValueType) eidasAttributeValue2).parseStringValue("Jones");

    Attribute eidasAttr = EidasAttributeTemplateConstants.CURRENT_FAMILY_NAME_TEMPLATE.createBuilder()
        .value(eidasAttributeValue1).value(eidasAttributeValue2).build();

    Assertions.assertTrue(converter.supportsConversionToSwedishAttribute(eidasAttr.getName()));

    Attribute swAttr = converter.toSwedishEidAttribute(eidasAttr);
    Assertions.assertEquals(1, swAttr.getAttributeValues().size());
    Assertions.assertEquals("Smith", AttributeUtils.getAttributeStringValue(swAttr));
    
    Attribute sAttribute = EidasAttributeTemplateConstants.CURRENT_FAMILY_NAME_TEMPLATE.createBuilder()
        .value("Smith", "Jones").build();
    
    swAttr = converter.toSwedishEidAttribute(sAttribute);
    Assertions.assertEquals(1, swAttr.getAttributeValues().size());
    Assertions.assertEquals("Smith", AttributeUtils.getAttributeStringValue(swAttr));
  }

}
