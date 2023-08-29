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

import org.opensaml.saml.saml2.core.Attribute;

import se.swedenconnect.eidas.attributes.EidasAttributeTemplate;
import se.swedenconnect.opensaml.saml2.attribute.AttributeTemplate;

/**
 * Interface for attribute conversion between eIDAS and Swedish eID attribute values.
 * 
 * @author Martin Lindström
 */
public interface AttributeConverter {

  /**
   * Predicate that tells whether the converter supports converting the supplied Swedish attribute into a corresponding
   * eIDAS attribute.
   * 
   * @param swedishEidAttribute the name of the Swedish attribute
   * @return {@code true} if conversion is possible and {@code false} otherwise
   */
  boolean supportsConversionToEidas(final String swedishEidAttribute);

  /**
   * Converts the supplied Swedish eID attribute to an eIDAS attribute, including attribute value conversion.
   * 
   * @param swedishEidAttribute the Swedish eID attribute
   * @return an eIDAS attribute
   * @throws IllegalArgumentException if the converter does not support conversion for the given attribute
   */
  Attribute toEidasAttribute(final Attribute swedishEidAttribute) throws IllegalArgumentException;

  /**
   * Gets the {@link EidasAttributeTemplate} that can be used to transform the supplied Swedish eID attribute.
   * 
   * @param swedishEidAttribute attribute name
   * @return an {@link EidasAttributeTemplate}
   */
  EidasAttributeTemplate getEidasAttributeTemplate(final String swedishEidAttribute);

  /**
   * Predicate that tells if this converter supports conversion of the given eIDAS attribute to a Swedish eID attribute.
   * 
   * @param eidasAttribute the name of the eIDAS attribute
   * @return {@code true} if conversion can be done, and {@code false} otherwise
   */
  boolean supportsConversionToSwedishAttribute(final String eidasAttribute);

  /**
   * Converts the supplied eIDAS attribute to a Swedish eID attribute, including attribute value conversion.
   * 
   * @param eidasAttribute the eIDAS attribute
   * @return a Swedish eID attribute
   * @throws IllegalArgumentException if the converter does not support conversion for the given attribute
   */
  Attribute toSwedishEidAttribute(final Attribute eidasAttribute);

  /**
   * Gets the {@link AttributeTemplate} that can be used to transform the supplied eIDAS attribute.
   * 
   * @param eidasAttribute attribute name
   * @return an {@link AttributeTemplate}
   */
  AttributeTemplate getSwedishAttributeTemplate(final String eidasAttribute);

}
