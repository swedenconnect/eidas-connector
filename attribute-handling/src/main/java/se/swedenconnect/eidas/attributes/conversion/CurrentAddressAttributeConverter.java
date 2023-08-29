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

import java.net.URLDecoder;
import java.util.List;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.Attribute;

import lombok.extern.slf4j.Slf4j;
import se.litsec.eidas.opensaml.ext.attributes.CurrentAddressType;
import se.swedenconnect.eidas.attributes.EidasAttributeTemplate;
import se.swedenconnect.eidas.attributes.EidasAttributeTemplateConstants;
import se.swedenconnect.opensaml.saml2.attribute.AttributeBuilder;
import se.swedenconnect.opensaml.saml2.attribute.AttributeUtils;
import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;

/**
 * Specialized converter for the CurrentAddress attribute, since its value representation differs between eIDAS and the
 * Swedish eID framework.
 *
 * @author Martin Lindström
 */
@Slf4j
public class CurrentAddressAttributeConverter extends DefaultAttributeConverter {

  /**
   * Default constructor.
   */
  public CurrentAddressAttributeConverter() {
    super(EidasAttributeTemplateConstants.CURRENT_ADDRESS_TEMPLATE, 
        AttributeConstants.ATTRIBUTE_TEMPLATE_EIDAS_NATURAL_PERSON_ADDRESS);
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
      if (!CurrentAddressType.class.isInstance(eidasAttributeValue)) {
        return null;
      }
      final CurrentAddressType currentAddress = CurrentAddressType.class.cast(eidasAttributeValue);
      // Parse the string ...
      final String[] parts = value.split(";");
      for (final String p : parts) {
        final String[] kv = p.split("=");
        if (kv.length != 2) {
          log.warn("Unknown token of CurrentAddress {}", p);
          continue;
        }
        final String type = kv[0];
        final String subValue = URLDecoder.decode(kv[1], "UTF-8");
        if (type.equalsIgnoreCase("PoBox")) {
          currentAddress.setPoBox(subValue);
        }
        else if (type.equalsIgnoreCase("LocatorDesignator")) {
          currentAddress.setLocatorDesignator(subValue);
        }
        else if (type.equalsIgnoreCase("LocatorName")) {
          currentAddress.setLocatorName(subValue);
        }
        else if (type.equalsIgnoreCase("CvaddressArea")) {
          currentAddress.setCvaddressArea(subValue);
        }
        else if (type.equalsIgnoreCase("Thoroughfare")) {
          currentAddress.setThoroughfare(subValue);
        }
        else if (type.equalsIgnoreCase("PostName")) {
          currentAddress.setPostName(subValue);
        }
        else if (type.equalsIgnoreCase("AdminunitFirstline")) {
          currentAddress.setAdminunitFirstline(subValue);
        }
        else if (type.equalsIgnoreCase("AdminunitSecondline")) {
          currentAddress.setAdminunitSecondline(subValue);
        }
        else if (type.equalsIgnoreCase("PostCode")) {
          currentAddress.setPostCode(subValue);
        }
        else {
          log.warn("Unknown CurrentAddress type - {}", type);
        }
      }
      log.trace("Transformed Swedish eID attribute '{}' into eIDAS attribute '{}' ({})",
          swedishEidAttribute.getName(), template.getName(), template.getFriendlyName());
      return template.createBuilder().value(currentAddress).build();
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
    if (!CurrentAddressType.class.isInstance(values.get(0))) {
      return null;
    }
    final CurrentAddressType currentAddress = CurrentAddressType.class.cast(values.get(0));

    final String value = currentAddress.toSwedishEidString();
    final XSString stringValue = AttributeBuilder.createValueObject(XSString.TYPE_NAME, XSString.class);
    stringValue.setValue(value);
    return stringValue;
  }

}
