/*
 * Copyright 2023 Sweden Connect
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.Attribute;

import se.swedenconnect.eidas.attributes.conversion.AttributeConverter;
import se.swedenconnect.spring.saml.idp.attributes.RequestedAttribute;

/**
 * Default implementation of the {@link AttributeMappingService} interface.
 * 
 * @author Martin Lindström
 */
public class DefaultAttributeMappingService implements AttributeMappingService {

  /** The converters used by the service. */
  private final List<AttributeConverter> converters;

  /**
   * Constructor.
   * 
   * @param converters the converters used by the service
   */
  public DefaultAttributeMappingService(final List<AttributeConverter> converters) {
    this.converters = Objects.requireNonNull(converters, "converters must not be null");
  }

  /** {@inheritDoc} */
  @Override
  public Attribute toEidasAttribute(final Attribute swedishAttribute) {
    return this.converters.stream()
        .filter(c -> c.supportsConversionToEidas(swedishAttribute.getName()))
        .findFirst()
        .map(c -> c.toEidasAttribute(swedishAttribute))
        .orElse(null);
  }

  /** {@inheritDoc} */
  @Override
  public se.litsec.eidas.opensaml.ext.RequestedAttribute toEidasRequestedAttribute(
      final RequestedAttribute requestedBySwedishSp) {

    final EidasAttributeTemplate template = this.converters.stream()
        .filter(c -> c.supportsConversionToEidas(requestedBySwedishSp.getId()))
        .map(c -> c.getEidasAttributeTemplate(requestedBySwedishSp.getId()))
        .findFirst()
        .orElse(null);

    if (template == null) {
      return null;
    }
    final se.litsec.eidas.opensaml.ext.RequestedAttribute requestedAttribute =
        (se.litsec.eidas.opensaml.ext.RequestedAttribute) XMLObjectSupport.buildXMLObject(
            se.litsec.eidas.opensaml.ext.RequestedAttribute.DEFAULT_ELEMENT_NAME);
    requestedAttribute.setName(template.getName());
    requestedAttribute.setFriendlyName(template.getFriendlyName());
    requestedAttribute.setNameFormat(template.getNameFormat());

    // Check if the attribute is part of the minimum data set. If so, set isRequired.
    requestedAttribute.setIsRequired(
        AttributeMappingService.NATURAL_PERSON_MINIMUM_DATASET.contains(template.getName()));

    return requestedAttribute;
  }

  /** {@inheritDoc} */
  @Override
  public List<se.litsec.eidas.opensaml.ext.RequestedAttribute> toEidasRequestedAttributes(
      final List<RequestedAttribute> requestedBySwedishSp, final boolean includeMinimumDataSet) {

    final List<se.litsec.eidas.opensaml.ext.RequestedAttribute> requestedAttributes = new ArrayList<>();
    requestedBySwedishSp.stream()
        .map(ra -> this.toEidasRequestedAttribute(ra))
        .filter(r -> r != null)
        .forEach(requestedAttributes::add);

    if (includeMinimumDataSet) {
      for (final String a : AttributeMappingService.NATURAL_PERSON_MINIMUM_DATASET) {
        if (requestedAttributes.stream().noneMatch(ra -> Objects.equals(a, ra.getName()))) {
          final EidasAttributeTemplate template = EidasAttributeTemplateConstants.MINIMUM_DATASET_TEMPLATES.stream()
              .filter(t -> Objects.equals(t.getName(), a))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("eIDAS minimum data set config error"));

          final se.litsec.eidas.opensaml.ext.RequestedAttribute requestedAttribute =
              (se.litsec.eidas.opensaml.ext.RequestedAttribute) XMLObjectSupport.buildXMLObject(
                  se.litsec.eidas.opensaml.ext.RequestedAttribute.DEFAULT_ELEMENT_NAME);
          requestedAttribute.setName(template.getName());
          requestedAttribute.setFriendlyName(template.getFriendlyName());
          requestedAttribute.setNameFormat(template.getNameFormat());
          requestedAttribute.setIsRequired(true);

          requestedAttributes.add(requestedAttribute);
        }
      }
    }

    return requestedAttributes;
  }

  /** {@inheritDoc} */
  @Override
  public Attribute toSwedishEidAttribute(final Attribute eidasAttribute) {
    return this.converters.stream()
        .filter(c -> c.supportsConversionToSwedishAttribute(eidasAttribute.getName()))
        .findFirst()
        .map(c -> c.toSwedishEidAttribute(eidasAttribute))
        .orElse(null);
  }

}
