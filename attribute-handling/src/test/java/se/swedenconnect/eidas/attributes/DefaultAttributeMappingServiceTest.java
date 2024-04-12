/*
 * Copyright 2023-2024 Sweden Connect
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

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.core.Attribute;

import se.swedenconnect.eidas.attributes.conversion.AttributeConverterConstants;
import se.swedenconnect.opensaml.eidas.ext.attributes.BirthNameType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentFamilyNameType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentGivenNameType;
import se.swedenconnect.opensaml.eidas.ext.attributes.DateOfBirthType;
import se.swedenconnect.opensaml.eidas.ext.attributes.PersonIdentifierType;
import se.swedenconnect.opensaml.eidas.ext.attributes.PlaceOfBirthType;
import se.swedenconnect.opensaml.saml2.attribute.AttributeUtils;
import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;
import se.swedenconnect.spring.saml.idp.attributes.RequestedAttribute;

/**
 * Test cases for DefaultAttributeMappingService.
 *
 * @author Martin Lindström
 */
public class DefaultAttributeMappingServiceTest extends OpenSamlTestBase {

  @Test
  public void testConvert() throws Exception {

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

    // Not supported
    swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_C.createBuilder().value("SE").build();
    Assertions.assertNull(service.toEidasAttribute(swAttr));
  }

  @Test
  public void testToEidasRequestedAttribute() throws Exception {
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
    Assertions.assertFalse(eidasR.isRequired());

    ra = new RequestedAttribute(
        AttributeConstants.ATTRIBUTE_NAME_GIVEN_NAME,
        AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_GIVEN_NAME,
        false);

    eidasR = service.toEidasRequestedAttribute(ra);
    Assertions.assertEquals(EidasAttributeTemplateConstants.CURRENT_GIVEN_NAME_TEMPLATE.getName(), eidasR.getName());
    Assertions.assertEquals(EidasAttributeTemplateConstants.CURRENT_GIVEN_NAME_TEMPLATE.getFriendlyName(),
        eidasR.getFriendlyName());
    // In minimum data set
    Assertions.assertTrue(eidasR.isRequired());

    // No mapping
    ra = new RequestedAttribute(
        AttributeConstants.ATTRIBUTE_NAME_C,
        AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_C,
        false);
    Assertions.assertNull(service.toEidasRequestedAttribute(ra));
  }

  @Test
  public void testToEidasRequestedAttributes() throws Exception {

    final DefaultAttributeMappingService service =
        new DefaultAttributeMappingService(AttributeConverterConstants.DEFAULT_CONVERTERS);

    List<RequestedAttribute> ras = List.of(
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
        // No mapping
        new RequestedAttribute(
            AttributeConstants.ATTRIBUTE_NAME_C,
            AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_C,
            true));

    List<se.swedenconnect.opensaml.eidas.ext.RequestedAttribute> eidas = service.toEidasRequestedAttributes(ras, true);
    Assertions.assertEquals(5, eidas.size());

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

    eidas = service.toEidasRequestedAttributes(ras, false);
    Assertions.assertEquals(4, eidas.size());

    Assertions.assertTrue(eidas.stream().anyMatch(a -> Objects.equals(a.getName(),
        EidasAttributeTemplateConstants.CURRENT_FAMILY_NAME_TEMPLATE.getName())));
    Assertions.assertTrue(eidas.stream().anyMatch(a -> Objects.equals(a.getName(),
        EidasAttributeTemplateConstants.CURRENT_GIVEN_NAME_TEMPLATE.getName())));
    Assertions.assertTrue(eidas.stream().anyMatch(a -> Objects.equals(a.getName(),
        EidasAttributeTemplateConstants.PERSON_IDENTIFIER_TEMPLATE.getName())));
    Assertions.assertTrue(eidas.stream().anyMatch(a -> Objects.equals(a.getName(),
        EidasAttributeTemplateConstants.PLACE_OF_BIRTH_TEMPLATE.getName())));
  }

}
