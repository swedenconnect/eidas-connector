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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.core.Attribute;

import se.litsec.eidas.opensaml.ext.attributes.GenderType;
import se.swedenconnect.eidas.attributes.EidasAttributeTemplateConstants;
import se.swedenconnect.eidas.attributes.OpenSamlTestBase;
import se.swedenconnect.opensaml.saml2.attribute.AttributeUtils;
import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;

/**
 * Test cases for GenderAttributeConverter.
 *
 * @author Martin Lindström
 */
public class GenderAttributeConverterTest extends OpenSamlTestBase {

  @Test
  public void testConvert() throws Exception {

    final GenderAttributeConverter converter = new GenderAttributeConverter();

    Attribute swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_GENDER.createBuilder().value("M").build();

    Assertions.assertTrue(converter.supportsConversionToEidas(swAttr.getName()));

    Attribute eidasAttr = converter.toEidasAttribute(swAttr);
    Assertions.assertNotNull(eidasAttr);
    Assertions.assertEquals(EidasAttributeTemplateConstants.GENDER_TEMPLATE.getName(), eidasAttr.getName());
    GenderType gender = AttributeUtils.getAttributeValue(eidasAttr, GenderType.class);
    Assertions.assertEquals("Male", gender.getGender().getValue());

    Attribute swAttr2 = converter.toSwedishEidAttribute(eidasAttr);

    Assertions.assertTrue(converter.supportsConversionToSwedishAttribute(eidasAttr.getName()));

    Assertions.assertNotNull(swAttr2);
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_GENDER, swAttr2.getName());
    Assertions.assertEquals("M", AttributeUtils.getAttributeStringValue(swAttr2));

    swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_GENDER.createBuilder().value("F").build();
    eidasAttr = converter.toEidasAttribute(swAttr);
    Assertions.assertNotNull(eidasAttr);
    gender = AttributeUtils.getAttributeValue(eidasAttr, GenderType.class);
    Assertions.assertEquals("Female", gender.getGender().getValue());

    swAttr2 = converter.toSwedishEidAttribute(eidasAttr);
    Assertions.assertNotNull(swAttr2);
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_GENDER, swAttr2.getName());
    Assertions.assertEquals("F", AttributeUtils.getAttributeStringValue(swAttr2));

    swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_GENDER.createBuilder().value("U").build();
    eidasAttr = converter.toEidasAttribute(swAttr);
    Assertions.assertNotNull(eidasAttr);
    gender = AttributeUtils.getAttributeValue(eidasAttr, GenderType.class);
    Assertions.assertEquals("Unspecified", gender.getGender().getValue());

    swAttr2 = converter.toSwedishEidAttribute(eidasAttr);
    Assertions.assertNotNull(swAttr2);
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_GENDER, swAttr2.getName());
    Assertions.assertEquals("U", AttributeUtils.getAttributeStringValue(swAttr2));
  }

}
