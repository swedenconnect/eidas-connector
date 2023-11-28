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
package se.swedenconnect.eidas.attributes.conversion;

import java.util.List;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.swedenconnect.eidas.attributes.EidasAttributeTemplate;
import se.swedenconnect.eidas.attributes.EidasAttributeTemplateConstants;
import se.swedenconnect.opensaml.eidas.ext.attributes.GenderType;
import se.swedenconnect.opensaml.eidas.ext.attributes.GenderTypeEnumeration;
import se.swedenconnect.opensaml.saml2.attribute.AttributeBuilder;
import se.swedenconnect.opensaml.saml2.attribute.AttributeUtils;
import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;
import se.swedenconnect.spring.saml.idp.attributes.UserAttribute;
import se.swedenconnect.spring.saml.idp.attributes.eidas.Gender;

/**
 * Specialized converter for the Gender attribute, since its value representation differs between eIDAS and the Swedish
 * eID framework.
 *
 * @author Martin Lindström
 */
public class GenderAttributeConverter extends DefaultAttributeConverter {

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(GenderAttributeConverter.class);

  /**
   * Default constructor.
   */
  public GenderAttributeConverter() {
    super(EidasAttributeTemplateConstants.GENDER_TEMPLATE, AttributeConstants.ATTRIBUTE_TEMPLATE_GENDER);
  }

  /** {@inheritDoc} */
  @Override
  public Attribute toEidasAttribute(final Attribute swedishEidAttribute) {

    final EidasAttributeTemplate template = this.getEidasAttributeTemplate(swedishEidAttribute);
    final String value = AttributeUtils.getAttributeStringValue(swedishEidAttribute);
    if (value == null) {
      return null;
    }
    try {
      final XMLObject eidasAttributeValue = template.createAttributeValueObject();
      if (!GenderType.class.isInstance(eidasAttributeValue)) {
        return null;
      }
      final GenderType genderValue = GenderType.class.cast(eidasAttributeValue);
      if ("M".equalsIgnoreCase(value) || GenderTypeEnumeration.MALE.getValue().equalsIgnoreCase(value)) {
        genderValue.setGender(GenderTypeEnumeration.MALE);
      }
      else if ("F".equalsIgnoreCase(value) || GenderTypeEnumeration.FEMALE.getValue().equalsIgnoreCase(value)) {
        genderValue.setGender(GenderTypeEnumeration.FEMALE);
      }
      else {
        genderValue.setGender(GenderTypeEnumeration.UNSPECIFIED);
      }
      log.trace("Transformed Swedish eID attribute '{}' into eIDAS attribute '{}' ({})",
          swedishEidAttribute.getName(), template.getName(), template.getFriendlyName());
      return template.createBuilder().value(genderValue).build();
    }
    catch (final Exception e) {
      log.error("Failed to create eIDAS attribute value object when transforming Swedish eID attribute '{}'",
          swedishEidAttribute.getName(), e);
      return null;
    }
  }

  /** {@inheritDoc} */
  @Override
  protected XMLObject toSwedishEidAttributeValue(final List<XMLObject> values) {
    if (values.isEmpty()) {
      return null;
    }
    if (!GenderType.class.isInstance(values.get(0))) {
      return null;
    }
    final GenderType genderValue = GenderType.class.cast(values.get(0));
    String value = null;
    if (genderValue.getGender().equals(GenderTypeEnumeration.MALE)) {
      value = "M";
    }
    else if (genderValue.getGender().equals(GenderTypeEnumeration.FEMALE)) {
      value = "F";
    }
    else {
      value = "U";
    }
    final XSString stringValue = AttributeBuilder.createValueObject(XSString.TYPE_NAME, XSString.class);
    stringValue.setValue(value);
    return stringValue;
  }

  /** {@inheritDoc} */
  @Override
  protected String toSwedishEidAttributeValue(final UserAttribute eidasAttribute) {
    if (eidasAttribute.getValues().isEmpty()) {
      return null;
    }
    final String gender = Gender.class.cast(eidasAttribute.getValues().get(0)).getValueAsString();
    if (GenderTypeEnumeration.MALE.getValue().equalsIgnoreCase(gender)) {
      return "M";
    }
    else if (GenderTypeEnumeration.FEMALE.getValue().equalsIgnoreCase(gender)) {
      return "F";
    }
    else {
      return "U";
    }
  }

}
