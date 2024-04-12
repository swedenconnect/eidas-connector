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
package se.swedenconnect.eidas.attributes.conversion;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.core.Attribute;

import se.swedenconnect.eidas.attributes.EidasAttributeTemplateConstants;
import se.swedenconnect.eidas.attributes.OpenSamlTestBase;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentAddressType;
import se.swedenconnect.opensaml.saml2.attribute.AttributeBuilder;
import se.swedenconnect.opensaml.saml2.attribute.AttributeUtils;
import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;

/**
 * Test cases for CurrentAddressAttributeConverter.
 *
 * @author Martin Lindström
 */
public class CurrentAddressAttributeConverterTest extends OpenSamlTestBase {

  @Test
  public void testConvert() {

    final CurrentAddressAttributeConverter converter = new CurrentAddressAttributeConverter();

    final Attribute swAttr = AttributeConstants.ATTRIBUTE_TEMPLATE_EIDAS_NATURAL_PERSON_ADDRESS.createBuilder()
        .value("LocatorDesignator=6%20tr;LocatorName=10;Thoroughfare=Korta%20gatan;PostName=Solna;PostCode=19174")
        .build();

    Assertions.assertTrue(converter.supportsConversionToEidas(swAttr.getName()));

    final Attribute eidasAttr = converter.toEidasAttribute(swAttr);

    Assertions.assertNotNull(eidasAttr);
    final CurrentAddressType cat = AttributeUtils.getAttributeValue(eidasAttr, CurrentAddressType.class);
    Assertions.assertEquals("6 tr", cat.getLocatorDesignator());
    Assertions.assertEquals("10", cat.getLocatorName());
    Assertions.assertEquals("Korta gatan", cat.getThoroughfare());
    Assertions.assertEquals("Solna", cat.getPostName());
    Assertions.assertEquals("19174", cat.getPostCode());

    final CurrentAddressType address = AttributeBuilder.createValueObject(CurrentAddressType.class);
    address.setLocatorDesignator("6 tr");
    address.setLocatorName("10");
    address.setThoroughfare("Korta gatan");
    address.setPostName("Solna");
    address.setPostCode("19174");

    final Attribute addressAttribute =
        EidasAttributeTemplateConstants.CURRENT_ADDRESS_TEMPLATE.createBuilder().value(address).build();

    Assertions.assertTrue(converter.supportsConversionToSwedishAttribute(addressAttribute.getName()));

    final Attribute swAttr2 = converter.toSwedishEidAttribute(addressAttribute);
    Assertions.assertNotNull(swAttr2);
    Assertions.assertEquals(AttributeConstants.ATTRIBUTE_NAME_EIDAS_NATURAL_PERSON_ADDRESS, swAttr2.getName());
    Assertions.assertEquals(
        "LocatorDesignator=6%20tr;LocatorName=10;Thoroughfare=Korta%20gatan;PostName=Solna;PostCode=19174",
        AttributeUtils.getAttributeStringValue(swAttr2));
  }

}
