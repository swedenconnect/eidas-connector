/*
 * Copyright 2017-2025 Sweden Connect
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

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Attribute;

import se.swedenconnect.opensaml.saml2.attribute.AttributeBuilder;
import se.swedenconnect.opensaml.saml2.attribute.AttributeTemplate;

import java.io.Serial;

/**
 * Extends the {@link AttributeTemplate} with support for creating the attribute value object to be used for the
 * attribute.
 *
 * @author Martin Lindström
 */
public class EidasAttributeTemplate extends AttributeTemplate {

  @Serial
  private static final long serialVersionUID = 3099219622410367843L;

  /** The class implementing the attribute's value. */
  private final Class<? extends XMLObject> valueClass;

  /**
   * Creates an attribute template with the given name and friendly name, the default name format
   * {@code urn:oasis:names:tc:SAML:2.0:attrname-format:uri} ({@link Attribute#URI_REFERENCE}).
   *
   * @param valueClass the class implementing the attribute's value
   * @param name the attribute name
   * @param friendlyName the attribute friendly name (optional)
   */
  public EidasAttributeTemplate(final Class<? extends XMLObject> valueClass, final String name,
      final String friendlyName) {
    super(name, friendlyName);
    this.valueClass = valueClass;
  }

  /**
   * Creates an attribute template with the given name, friendly name and name format.
   *
   * @param valueClass the class implementing the attribute's value
   * @param name the attribute name
   * @param friendlyName the attribute friendly name
   * @param nameFormat the name format
   */
  public EidasAttributeTemplate(final Class<? extends XMLObject> valueClass, final String name,
      final String friendlyName, final String nameFormat) {
    super(name, friendlyName, nameFormat);
    this.valueClass = valueClass;
  }

  /**
   * Creates an attribute value object for the given attribute.
   * <p>
   * Note: The contents of the value object still has to be assigned.
   * </p>
   *
   * @return the value object
   */
  public XMLObject createAttributeValueObject() {
    return AttributeBuilder.createValueObject(this.valueClass);
  }

}
