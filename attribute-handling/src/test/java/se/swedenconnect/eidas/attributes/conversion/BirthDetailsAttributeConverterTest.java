/*
 * Copyright 2017-2025 Sweden Connect
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.Attribute;
import se.swedenconnect.eidas.attributes.EidasAttributeTemplateConstants;
import se.swedenconnect.eidas.attributes.OpenSamlTestBase;
import se.swedenconnect.opensaml.eidas.ext.attributes.CountryOfBirthType;
import se.swedenconnect.opensaml.saml2.attribute.AttributeBuilder;
import se.swedenconnect.opensaml.saml2.attribute.AttributeUtils;
import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;

import java.util.List;

/**
 * Test cases for BirthDetailsAttributeConverter.
 *
 * @author Martin Lindström
 */
public class BirthDetailsAttributeConverterTest extends OpenSamlTestBase {

  private final static BirthDetailsAttributeConverter converter = new BirthDetailsAttributeConverter(List.of(
      new AttributeTemplatePair(EidasAttributeTemplateConstants.COUNTRY_OF_BIRTH_TEMPLATE,
          AttributeConstants.ATTRIBUTE_TEMPLATE_PLACE_OF_BIRTH),
      new AttributeTemplatePair(EidasAttributeTemplateConstants.TOWN_OF_BIRTH_TEMPLATE,
          AttributeConstants.ATTRIBUTE_TEMPLATE_PLACE_OF_BIRTH)));

  @Test
  void testConvertCountryOfBirth() {

    final CountryOfBirthType value = (CountryOfBirthType) XMLObjectSupport.buildXMLObject(CountryOfBirthType.TYPE_NAME);
    value.setValue("SE");

    final Attribute attribute = AttributeBuilder.builder(
            se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_COUNTRY_OF_BIRTH_ATTRIBUTE_NAME)
        .friendlyName(
            se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_COUNTRY_OF_BIRTH_ATTRIBUTE_FRIENDLY_NAME)
        .value(value)
        .build();

    final Attribute swedishAttribute = converter.toSwedishEidAttribute(attribute);
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH, swedishAttribute.getName());
    Assertions.assertEquals("SE", AttributeUtils.getAttributeStringValue(swedishAttribute));

    Assertions.assertFalse(converter.supportsConversionToEidas(AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH));
    Assertions.assertThrows(IllegalArgumentException.class, () -> converter.toEidasAttribute(swedishAttribute));
  }

  @Test
  void testConvertTownOfBirth() {

    final Attribute attribute = AttributeBuilder.builder(
            se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_NAME)
        .friendlyName(
            se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_FRIENDLY_NAME)
        .value("Enköping")
        .build();

    final Attribute swedishAttribute = converter.toSwedishEidAttribute(attribute);
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH, swedishAttribute.getName());
    Assertions.assertEquals("Enköping", AttributeUtils.getAttributeStringValue(swedishAttribute));

    Assertions.assertFalse(converter.supportsConversionToEidas(AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH));
    Assertions.assertThrows(IllegalArgumentException.class, () -> converter.toEidasAttribute(swedishAttribute));
  }

}
