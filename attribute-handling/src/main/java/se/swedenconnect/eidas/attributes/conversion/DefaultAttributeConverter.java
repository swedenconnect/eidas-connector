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

import lombok.extern.slf4j.Slf4j;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.Attribute;
import se.swedenconnect.eidas.attributes.EidasAttributeTemplate;
import se.swedenconnect.opensaml.eidas.ext.attributes.EidasAttributeValueType;
import se.swedenconnect.opensaml.eidas.ext.attributes.TransliterationStringType;
import se.swedenconnect.opensaml.saml2.attribute.AttributeBuilder;
import se.swedenconnect.opensaml.saml2.attribute.AttributeTemplate;
import se.swedenconnect.opensaml.saml2.attribute.AttributeUtils;
import se.swedenconnect.spring.saml.idp.attributes.UserAttribute;
import se.swedenconnect.spring.saml.idp.attributes.eidas.EidasAttributeValue;
import se.swedenconnect.spring.saml.idp.attributes.eidas.TransliterationString;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Default implementation for the {@link AttributeConverter} interface.
 * <p>
 * This implementation assumes that all Swedish eID attributes are single valued string attributes and that all eIDAS
 * attributes handled implement the {@link EidasAttributeValueType} interface.
 * </p>
 *
 * @author Martin Lindström
 */
@Slf4j
public class DefaultAttributeConverter implements AttributeConverter {

  /** A list of the attributes that this converter handles. */
  protected final List<AttributeTemplatePair> templates;

  /**
   * Constructor assigning support for one attribute mapping.
   *
   * @param eidasTemplate the attribute template for the eIDAS attribute that this converter handles
   * @param swedishEidTemplate the attribute template for the Swedish eID attribute that this converter handles
   */
  public DefaultAttributeConverter(
      final EidasAttributeTemplate eidasTemplate, final AttributeTemplate swedishEidTemplate) {
    this.templates = List.of(new AttributeTemplatePair(eidasTemplate, swedishEidTemplate));
  }

  /**
   * Constructor assigning a list of template pairs.
   *
   * @param templates the templates that are supported
   */
  public DefaultAttributeConverter(final List<AttributeTemplatePair> templates) {
    this.templates = Objects.requireNonNull(templates, "templates must not be null");
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsConversionToEidas(final String swedishEidAttribute) {
    return this.templates.stream().anyMatch(p -> Objects.equals(
        p.swedishEidTemplate().getName(), swedishEidAttribute));
  }

  /** {@inheritDoc} */
  @Override
  public Attribute toEidasAttribute(final Attribute swedishEidAttribute) {

    final EidasAttributeTemplate template = this.getEidasAttributeTemplate(swedishEidAttribute);

    final String value = AttributeUtils.getAttributeStringValue(swedishEidAttribute);
    if (value == null) {
      log.info("Swedish eID attribute '{}' does not contain a string value - cannot convert",
          swedishEidAttribute.getName());
      return null;
    }
    try {
      final XMLObject eidasAttributeValue = template.createAttributeValueObject();
      if (eidasAttributeValue instanceof EidasAttributeValueType) {
        ((EidasAttributeValueType) eidasAttributeValue).parseStringValue(value);
        log.trace("Transformed Swedish eID attribute '{}' into eIDAS attribute '{}' ({})",
            swedishEidAttribute.getName(), template.getName(), template.getFriendlyName());
        return template.createBuilder().value(eidasAttributeValue).build();
      }
      // This is how we hope eIDAS attributes will turn out, so we'll add support for it.
      else if (eidasAttributeValue instanceof XSString) {
        return template.createBuilder().value(value).build();
      }
      else {
        log.info("Unknown type for eIDAS attribute value ({}) - can not convert Swedish eID attribute '{}'",
            eidasAttributeValue.getElementQName(), swedishEidAttribute.getName());
        return null;
      }
    }
    catch (final Exception e) {
      log.error("Failed to create eIDAS attribute value object when transforming Swedish eID attribute '{}'",
          swedishEidAttribute.getName(), e);
      return null;
    }
  }

  /** {@inheritDoc} */
  @Override
  public EidasAttributeTemplate getEidasAttributeTemplate(final String swedishEidAttribute) {
    return this.templates.stream()
        .filter(p -> Objects.equals(p.swedishEidTemplate().getName(), swedishEidAttribute))
        .map(AttributeTemplatePair::eidasTemplate)
        .findFirst()
        .orElse(null);
  }

  /**
   * Gets the {@link EidasAttributeTemplate} that can be used for transforming the supplied Swedish attribute into an
   * eIDAS attribute.
   *
   * @param swedishEidAttribute the Swedish eID attribute
   * @return an {@link EidasAttributeTemplate}
   * @throws IllegalArgumentException if no match is found
   */
  protected EidasAttributeTemplate getEidasAttributeTemplate(final Attribute swedishEidAttribute) {
    return Optional.ofNullable(this.getEidasAttributeTemplate(swedishEidAttribute.getName()))
        .orElseThrow(() -> new IllegalArgumentException("Unsupported attribute conversion"));
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsConversionToSwedishAttribute(final String eidasAttribute) {
    return this.templates.stream().anyMatch(p -> Objects.equals(
        p.eidasTemplate().getName(), eidasAttribute));
  }

  /** {@inheritDoc} */
  @Override
  public Attribute toSwedishEidAttribute(final Attribute eidasAttribute) {

    final AttributeTemplate template = this.getSwedishEidAttributeTemplate(eidasAttribute.getName());

    if (eidasAttribute.getAttributeValues().isEmpty()) {
      log.info("No attribute value present for eIDAS attribute '{}' - no conversion will be made",
          eidasAttribute.getName());
      return null;
    }
    final XMLObject attributeValue = this.toSwedishEidAttributeValue(eidasAttribute.getAttributeValues());
    if (attributeValue != null) {
      log.trace("Transformed eIDAS attribute '{}' into Swedish eID attribute '{}' ({})", eidasAttribute.getName(),
          template.getName(), template.getFriendlyName());
      return template.createBuilder().value(attributeValue).build();
    }
    else {
      log.info("No attribute value conversion was possible for eIDAS attribute '{}'", eidasAttribute.getName());
      return null;
    }
  }

  /** {@inheritDoc} */
  @Override
  public UserAttribute toSwedishEidAttribute(final UserAttribute eidasAttribute) {

    final AttributeTemplate template = this.getSwedishEidAttributeTemplate(eidasAttribute.getId());

    if (eidasAttribute.getValues().isEmpty()) {
      log.info("No attribute value present for eIDAS attribute '{}' - no conversion will be made",
          eidasAttribute.getId());
      return null;
    }
    final String stringValue = this.toSwedishEidAttributeValue(eidasAttribute);
    log.trace("Transformed eIDAS attribute '{}' into Swedish eID attribute '{}' ({})", eidasAttribute.getId(),
        template.getName(), template.getFriendlyName());
    return new UserAttribute(template.getName(), template.getFriendlyName(), stringValue);
  }

  /**
   * Gets the {@link AttributeTemplate} that can be used for transforming the supplied eIDAS attribute into a Swedish
   * eID attribute.
   *
   * @param eidasAttributeName the eIDAS attribute
   * @return an {@link AttributeTemplate}
   * @throws IllegalArgumentException if no match is found
   */
  protected AttributeTemplate getSwedishEidAttributeTemplate(final String eidasAttributeName) {
    return Optional.ofNullable(this.getSwedishAttributeTemplate(eidasAttributeName))
        .orElseThrow(() -> new IllegalArgumentException("Unsupported attribute conversion"));
  }

  /** {@inheritDoc} */
  @Override
  public AttributeTemplate getSwedishAttributeTemplate(final String eidasAttribute) {
    return this.templates.stream()
        .filter(p -> Objects.equals(p.eidasTemplate().getName(), eidasAttribute))
        .map(AttributeTemplatePair::swedishEidTemplate)
        .findFirst()
        .orElse(null);
  }

  /**
   * Given the eIDAS attribute value(s) the method returns the corresponding attribute value to use for the Swedish eID
   * attribute.
   * <p>
   * If more than one eIDAS attribute value is supplied, it probably means that the type is a transliteration string. In
   * that case, we use the value that has the {@code LatinScript} attribute set. If the types are not transliteration
   * strings, we first value is used.
   * </p>
   * <p>
   * The default implementation handles all eIDAS attribute values that implements the {@link EidasAttributeValueType}
   * interface (and string values).
   * </p>
   *
   * @param values the value(s) to transform
   * @return the attribute value to be used for the Swedish eID attribute
   */
  protected XMLObject toSwedishEidAttributeValue(final List<XMLObject> values) {

    final Function<List<XMLObject>, XMLObject> singleFunc = vl -> vl.stream()
        .filter(TransliterationStringType.class::isInstance)
        .filter(v -> ((TransliterationStringType) v).getLatinScript())
        .findFirst()
        .orElseGet(() -> values.get(0));

    final XMLObject eidasValue = values.size() == 1 ? values.get(0) : singleFunc.apply(values);
    if (eidasValue instanceof EidasAttributeValueType) {
      final String value = ((EidasAttributeValueType) eidasValue).toStringValue();
      final XSString stringValue = AttributeBuilder.createValueObject(XSString.TYPE_NAME, XSString.class);
      stringValue.setValue(value);
      return stringValue;
    }
    else if (eidasValue instanceof XSString) {
      final XSString stringValue = AttributeBuilder.createValueObject(XSString.TYPE_NAME, XSString.class);
      stringValue.setValue(((XSString) eidasValue).getValue());
      return stringValue;
    }
    else {
      log.info("Unsupported eIDAS attribute value type: {}", eidasValue.getElementQName());
      return null;
    }
  }

  /**
   * Extracts a string value from the given eIDAS {@link UserAttribute}.
   *
   * @param eidasAttribute the eIDAS attribute
   * @return the string value
   */
  protected String toSwedishEidAttributeValue(final UserAttribute eidasAttribute) {
    final List<? extends Serializable> values = eidasAttribute.getValues();
    if (values.get(0) instanceof TransliterationString) {
      final XMLObject attributeValue = this.toSwedishEidAttributeValue(values.stream()
          .map(TransliterationString.class::cast)
          .map(TransliterationString::createXmlObject)
          .map(XMLObject.class::cast)
          .toList());
      return ((XSString) attributeValue).getValue();
    }
    else if (values.get(0) instanceof EidasAttributeValue) {
      return ((EidasAttributeValue<?>) values.get(0)).getValueAsString();
    }
    else {
      return values.get(0).toString();
    }
  }

}
