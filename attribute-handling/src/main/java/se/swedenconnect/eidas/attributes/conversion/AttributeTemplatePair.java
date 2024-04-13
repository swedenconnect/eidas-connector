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

import java.util.Objects;

import se.swedenconnect.eidas.attributes.EidasAttributeTemplate;
import se.swedenconnect.opensaml.saml2.attribute.AttributeTemplate;

/**
 * A pair of attribute templates.
 * 
 * @author Martin Lindström
 */
public record AttributeTemplatePair(EidasAttributeTemplate eidasTemplate, AttributeTemplate swedishEidTemplate) {

  /**
   * Creates a {@link AttributeTemplatePair}.
   * 
   * @param eidasTemplate the eIDAS attribute template
   * @param swedishEidTemplate the Swedish eID template
   * @return a template pair
   */
  public static AttributeTemplatePair of(final EidasAttributeTemplate eidasTemplate,
      final AttributeTemplate swedishEidTemplate) {
    return new AttributeTemplatePair(
        Objects.requireNonNull(eidasTemplate, "eidasTemplate must not be null"), 
        Objects.requireNonNull(swedishEidTemplate, "swedishEidTemplate must not be null"));
  }
  
}