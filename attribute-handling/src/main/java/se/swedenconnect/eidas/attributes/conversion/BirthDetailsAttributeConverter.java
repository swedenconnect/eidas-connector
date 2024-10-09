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

import org.opensaml.saml.saml2.core.Attribute;
import se.swedenconnect.eidas.attributes.EidasAttributeTemplate;
import se.swedenconnect.opensaml.saml2.attribute.AttributeTemplate;

import java.util.List;

/**
 * Attribute converter that handles the CountryOfBirth and TownOfBirth attributes.
 *
 * @author Martin Lindström
 */
public class BirthDetailsAttributeConverter extends DefaultAttributeConverter {

  /**
   * Constructor assigning support for one attribute mapping.
   *
   * @param eidasTemplate the attribute template for the eIDAS attribute that this converter handles
   * @param swedishEidTemplate the attribute template for the Swedish eID attribute that this converter handles
   */
  public BirthDetailsAttributeConverter(final EidasAttributeTemplate eidasTemplate,
      final AttributeTemplate swedishEidTemplate) {
    super(eidasTemplate, swedishEidTemplate);
  }

  /**
   * Constructor assigning a list of template pairs.
   *
   * @param templates the templates that are supported
   */
  public BirthDetailsAttributeConverter(final List<AttributeTemplatePair> templates) {
    super(templates);
  }

  /**
   * Will return {@code false}.
   */
  @Override
  public boolean supportsConversionToEidas(final String swedishEidAttribute) {
    return false;
  }

  /**
   * Will throw {@link IllegalArgumentException} since conversion to eIDAS is not supported by the converter.
   */
  @Override
  public Attribute toEidasAttribute(final Attribute swedishEidAttribute) {
    throw new IllegalArgumentException("Unsupported attribute conversion");
  }
}
