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
package se.swedenconnect.eidas.attributes.conversion;

import java.util.List;

import se.swedenconnect.eidas.attributes.EidasAttributeTemplateConstants;
import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;

/**
 * Constants for attribute conversion.
 *
 * @author Martin Lindström
 */
public class AttributeConverterConstants {

  /**
   * Converters according to section 3.3.3, "Conversion of eIDAS Attributes", in <a href=
   * "https://docs.swedenconnect.se/technical-framework/latest/04_-_Attribute_Specification_for_the_Swedish_eID_Framework.html#conversion-of-eidas-attributes">
   * Attribute Specification for the Swedish eID Framework</a>.
   */
  public static final List<AttributeConverter> DEFAULT_CONVERTERS = List.of(
      new DefaultAttributeConverter(List.of(
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
              AttributeConstants.ATTRIBUTE_TEMPLATE_PLACE_OF_BIRTH))),
      new CurrentAddressAttributeConverter(),
      new GenderAttributeConverter());

  private AttributeConverterConstants() {
  }

}
