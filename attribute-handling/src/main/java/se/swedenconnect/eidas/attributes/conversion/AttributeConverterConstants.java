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

import se.swedenconnect.eidas.attributes.EidasAttributeTemplateConstants;
import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;

import java.util.List;

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
          new AttributeTemplatePair(EidasAttributeTemplateConstants.PERSON_IDENTIFIER_TEMPLATE,
              AttributeConstants.ATTRIBUTE_TEMPLATE_EIDAS_PERSON_IDENTIFIER),
          new AttributeTemplatePair(EidasAttributeTemplateConstants.CURRENT_FAMILY_NAME_TEMPLATE,
              AttributeConstants.ATTRIBUTE_TEMPLATE_SN),
          new AttributeTemplatePair(EidasAttributeTemplateConstants.CURRENT_GIVEN_NAME_TEMPLATE,
              AttributeConstants.ATTRIBUTE_TEMPLATE_GIVEN_NAME),
          new AttributeTemplatePair(EidasAttributeTemplateConstants.DATE_OF_BIRTH_TEMPLATE,
              AttributeConstants.ATTRIBUTE_TEMPLATE_DATE_OF_BIRTH),
          new AttributeTemplatePair(EidasAttributeTemplateConstants.BIRTH_NAME_TEMPLATE,
              AttributeConstants.ATTRIBUTE_TEMPLATE_BIRTH_NAME),
          new AttributeTemplatePair(EidasAttributeTemplateConstants.PLACE_OF_BIRTH_TEMPLATE,
              AttributeConstants.ATTRIBUTE_TEMPLATE_PLACE_OF_BIRTH),
          new AttributeTemplatePair(EidasAttributeTemplateConstants.NATIONALITY_TEMPLATE,
              AttributeConstants.ATTRIBUTE_TEMPLATE_COUNTRY_OF_CITIZENSHIP),
          new AttributeTemplatePair(EidasAttributeTemplateConstants.COUNTRY_OF_RESIDENCE_TEMPLATE,
              AttributeConstants.ATTRIBUTE_TEMPLATE_COUNTRY_OF_RESIDENCE),
          new AttributeTemplatePair(EidasAttributeTemplateConstants.PHONE_NUMBER_TEMPLATE,
              AttributeConstants.ATTRIBUTE_TEMPLATE_TELEPHONE_NUMBER),
          new AttributeTemplatePair(EidasAttributeTemplateConstants.EMAIL_ADDRESS_TEMPLATE,
              AttributeConstants.ATTRIBUTE_TEMPLATE_MAIL))),
      new BirthDetailsAttributeConverter(List.of(
          new AttributeTemplatePair(EidasAttributeTemplateConstants.COUNTRY_OF_BIRTH_TEMPLATE,
              AttributeConstants.ATTRIBUTE_TEMPLATE_PLACE_OF_BIRTH),
          new AttributeTemplatePair(EidasAttributeTemplateConstants.TOWN_OF_BIRTH_TEMPLATE,
              AttributeConstants.ATTRIBUTE_TEMPLATE_PLACE_OF_BIRTH))),
      new CurrentAddressAttributeConverter(),
      new GenderAttributeConverter());

  private AttributeConverterConstants() {
  }

}
