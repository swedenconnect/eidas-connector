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

import se.swedenconnect.eidas.attributes.EidasAttributeTemplate;
import se.swedenconnect.opensaml.saml2.attribute.AttributeTemplate;

/**
 * A pair of attribute templates.
 *
 * @param eidasTemplate the eIDAS attribute template
 * @param swedishEidTemplate the Swedish eID template
 */
public record AttributeTemplatePair(EidasAttributeTemplate eidasTemplate, AttributeTemplate swedishEidTemplate) {

  public AttributeTemplatePair {
    if (eidasTemplate == null) {
      throw new IllegalArgumentException("eidasTemplate cannot be null");
    }
    if (swedishEidTemplate == null) {
      throw new IllegalArgumentException("swedishEidTemplate cannot be null");
    }
  }

}
