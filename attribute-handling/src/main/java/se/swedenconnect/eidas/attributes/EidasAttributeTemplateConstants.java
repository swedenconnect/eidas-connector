/*
 * Copyright 2017-2023 Sweden Connect
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

import org.opensaml.core.xml.schema.XSString;
import se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants;
import se.swedenconnect.opensaml.eidas.ext.attributes.BirthNameType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CountryOfBirthType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CountryOfResidenceType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentAddressType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentFamilyNameType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentGivenNameType;
import se.swedenconnect.opensaml.eidas.ext.attributes.DateOfBirthType;
import se.swedenconnect.opensaml.eidas.ext.attributes.GenderType;
import se.swedenconnect.opensaml.eidas.ext.attributes.NationalityType;
import se.swedenconnect.opensaml.eidas.ext.attributes.PersonIdentifierType;
import se.swedenconnect.opensaml.eidas.ext.attributes.PlaceOfBirthType;

import java.util.List;

/**
 * Attribute templates for all known (supported) eIDAS attributes.
 *
 * @author Martin Lindström
 */
public class EidasAttributeTemplateConstants {

  /** Attribute template for the "PersonIdentifier" attribute. */
  public static final EidasAttributeTemplate PERSON_IDENTIFIER_TEMPLATE = new EidasAttributeTemplate(
      PersonIdentifierType.class, AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_FRIENDLY_NAME);

  /** Attribute template for the "CurrentFamilyName" attribute. */
  public static final EidasAttributeTemplate CURRENT_FAMILY_NAME_TEMPLATE = new EidasAttributeTemplate(
      CurrentFamilyNameType.class, AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_FRIENDLY_NAME);

  /** Attribute template for the "CurrentGivenName" attribute. */
  public static final EidasAttributeTemplate CURRENT_GIVEN_NAME_TEMPLATE = new EidasAttributeTemplate(
      CurrentGivenNameType.class, AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_FRIENDLY_NAME);

  /** Attribute template for the "DateOfBirth" attribute. */
  public static final EidasAttributeTemplate DATE_OF_BIRTH_TEMPLATE = new EidasAttributeTemplate(
      DateOfBirthType.class, AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_FRIENDLY_NAME);

  /** Attribute template for the "Gender" attribute. */
  public static final EidasAttributeTemplate GENDER_TEMPLATE = new EidasAttributeTemplate(
      GenderType.class, AttributeConstants.EIDAS_GENDER_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_GENDER_ATTRIBUTE_FRIENDLY_NAME);

  /** Attribute template for the "CurrentAddress" attribute. */
  public static final EidasAttributeTemplate CURRENT_ADDRESS_TEMPLATE = new EidasAttributeTemplate(
      CurrentAddressType.class, AttributeConstants.EIDAS_CURRENT_ADDRESS_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_CURRENT_ADDRESS_ATTRIBUTE_FRIENDLY_NAME);

  /** Attribute template for the "BirthName" attribute. */
  public static final EidasAttributeTemplate BIRTH_NAME_TEMPLATE = new EidasAttributeTemplate(
      BirthNameType.class, AttributeConstants.EIDAS_BIRTH_NAME_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_BIRTH_NAME_ATTRIBUTE_FRIENDLY_NAME);

  /** Attribute template for the "PlaceOfBirth" attribute. */
  public static final EidasAttributeTemplate PLACE_OF_BIRTH_TEMPLATE = new EidasAttributeTemplate(
      PlaceOfBirthType.class, AttributeConstants.EIDAS_PLACE_OF_BIRTH_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_PLACE_OF_BIRTH_ATTRIBUTE_FRIENDLY_NAME);

  /** Attribute template for the "Nationality" attribute. */
  public static final EidasAttributeTemplate NATIONALITY_TEMPLATE = new EidasAttributeTemplate(
      NationalityType.class, AttributeConstants.EIDAS_NATIONALITY_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_NATIONALITY_ATTRIBUTE_FRIENDLY_NAME);

  /** Attribute template for the "CountryOfBirth" attribute. */
  public static final EidasAttributeTemplate COUNTRY_OF_BIRTH_TEMPLATE = new EidasAttributeTemplate(
      CountryOfBirthType.class, AttributeConstants.EIDAS_COUNTRY_OF_BIRTH_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_COUNTRY_OF_BIRTH_ATTRIBUTE_FRIENDLY_NAME);

  /** Attribute template for the "TownOfBirth" attribute. */
  public static final EidasAttributeTemplate TOWN_OF_BIRTH_TEMPLATE = new EidasAttributeTemplate(
      XSString.class, AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_FRIENDLY_NAME);

  /** Attribute template for the "CountryOfResidence" attribute. */
  public static final EidasAttributeTemplate COUNTRY_OF_RESIDENCE_TEMPLATE = new EidasAttributeTemplate(
      CountryOfResidenceType.class, AttributeConstants.EIDAS_COUNTRY_OF_RESIDENCE_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_COUNTRY_OF_RESIDENCE_ATTRIBUTE_FRIENDLY_NAME);

  /** Attribute template for the "PhoneNumber" attribute. */
  public static final EidasAttributeTemplate PHONE_NUMBER_TEMPLATE = new EidasAttributeTemplate(
      XSString.class, AttributeConstants.EIDAS_PHONE_NUMBER_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_PHONE_NUMBER_ATTRIBUTE_FRIENDLY_NAME);

  /** Attribute template for the "EmailAddress" attribute. */
  public static final EidasAttributeTemplate EMAIL_ADDRESS_TEMPLATE = new EidasAttributeTemplate(
      XSString.class, AttributeConstants.EIDAS_EMAIL_ADDRESS_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_EMAIL_ADDRESS_ATTRIBUTE_FRIENDLY_NAME);

  /** Templates for the eIDAS minimum data set. */
  public static final List<EidasAttributeTemplate> MINIMUM_DATASET_TEMPLATES = List.of(
      PERSON_IDENTIFIER_TEMPLATE, CURRENT_GIVEN_NAME_TEMPLATE, CURRENT_FAMILY_NAME_TEMPLATE, DATE_OF_BIRTH_TEMPLATE);

  // Hidden
  private EidasAttributeTemplateConstants() {
  }

}
