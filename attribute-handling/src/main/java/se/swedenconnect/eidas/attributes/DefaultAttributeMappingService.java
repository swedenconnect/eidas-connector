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

import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedenconnect.eidas.attributes.conversion.AttributeConverter;
import se.swedenconnect.opensaml.saml2.attribute.AttributeUtils;
import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;
import se.swedenconnect.spring.saml.idp.attributes.ImplicitRequestedAttribute;
import se.swedenconnect.spring.saml.idp.attributes.RequestedAttribute;
import se.swedenconnect.spring.saml.idp.attributes.UserAttribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of the {@link AttributeMappingService} interface.
 *
 * @author Martin Lindström
 */
public class DefaultAttributeMappingService implements AttributeMappingService {

  /** Logging instance. */
  private final static Logger log = LoggerFactory.getLogger(DefaultAttributeMappingService.class);

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
  public se.swedenconnect.opensaml.eidas.ext.RequestedAttribute toEidasRequestedAttribute(
      final RequestedAttribute requestedBySwedishSp) {

    EidasAttributeTemplate template = this.converters.stream()
        .filter(c -> c.supportsConversionToEidas(requestedBySwedishSp.getId()))
        .map(c -> c.getEidasAttributeTemplate(requestedBySwedishSp.getId()))
        .findFirst()
        .orElse(null);

    // Special handling for TownOfBirth and CountryOfBirth that does not have a Swedish counterpart.
    // So, we allow a Swedish SP to require these eIDAS attributes directly ...
    //
    if (template == null) {
      if (se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_COUNTRY_OF_BIRTH_ATTRIBUTE_NAME
          .equals(requestedBySwedishSp.getId())) {
        template = EidasAttributeTemplateConstants.COUNTRY_OF_BIRTH_TEMPLATE;
      }
      else if (se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_NAME
          .equals(requestedBySwedishSp.getId())) {
        template = EidasAttributeTemplateConstants.TOWN_OF_BIRTH_TEMPLATE;
      }
    }

    if (template == null) {
      return null;
    }
    final se.swedenconnect.opensaml.eidas.ext.RequestedAttribute requestedAttribute =
        this.createRequestedAttribute(template);

    // Check if the attribute is part of the minimum data set. If so, set isRequired.
    requestedAttribute.setIsRequired(
        AttributeMappingService.NATURAL_PERSON_MINIMUM_DATASET.contains(template.getName()));

    return requestedAttribute;
  }

  private se.swedenconnect.opensaml.eidas.ext.RequestedAttribute createRequestedAttribute(
      final EidasAttributeTemplate template) {
    final se.swedenconnect.opensaml.eidas.ext.RequestedAttribute requestedAttribute =
        (se.swedenconnect.opensaml.eidas.ext.RequestedAttribute) XMLObjectSupport.buildXMLObject(
            se.swedenconnect.opensaml.eidas.ext.RequestedAttribute.DEFAULT_ELEMENT_NAME);
    requestedAttribute.setName(template.getName());
    requestedAttribute.setFriendlyName(template.getFriendlyName());
    requestedAttribute.setNameFormat(template.getNameFormat());
    return requestedAttribute;
  }

  /** {@inheritDoc} */
  @Override
  public List<se.swedenconnect.opensaml.eidas.ext.RequestedAttribute> toEidasRequestedAttributes(
      final Collection<RequestedAttribute> requestedBySwedishSp, final boolean includeMinimumDataSet) {

    final List<se.swedenconnect.opensaml.eidas.ext.RequestedAttribute> requestedAttributes = new ArrayList<>();
    requestedBySwedishSp.stream()
        .filter(ra -> {
          if (ra instanceof final ImplicitRequestedAttribute impl) {
            // Don't include implicitly required attributes that are not required ...
            return impl.isRequired();
          }
          return true;
        })
        .map(this::toEidasRequestedAttribute)
        .filter(Objects::nonNull)
        .forEach(requestedAttributes::add);

    if (includeMinimumDataSet) {
      for (final String a : AttributeMappingService.NATURAL_PERSON_MINIMUM_DATASET) {
        if (requestedAttributes.stream().noneMatch(ra -> Objects.equals(a, ra.getName()))) {
          final EidasAttributeTemplate template = EidasAttributeTemplateConstants.MINIMUM_DATASET_TEMPLATES.stream()
              .filter(t -> Objects.equals(t.getName(), a))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("eIDAS minimum data set config error"));

          final se.swedenconnect.opensaml.eidas.ext.RequestedAttribute requestedAttribute =
              this.createRequestedAttribute(template);
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

  /**
   * Implements special handling for PlaceOfBirth, CountryOfBirth and TownOfBirth ...
   */
  @Override
  public List<Attribute> toSwedishEidAttributes(final Collection<Attribute> eidasAttributes) {
    final List<Attribute> swedishEidAttributes = new ArrayList<>();
    boolean eidasPlaceOfBirth = false;
    String townOfBirth = null;
    String countryOfBirth = null;

    for (final Attribute eidasAttribute : eidasAttributes) {
      if (se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_PLACE_OF_BIRTH_ATTRIBUTE_NAME
          .equals(eidasAttribute.getName())) {
        eidasPlaceOfBirth = true;
      }

      if (se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_NAME
          .equals(eidasAttribute.getName())) {
        if (!eidasPlaceOfBirth) {
          townOfBirth = Optional.ofNullable(this.toSwedishEidAttribute(eidasAttribute))
              .map(AttributeUtils::getAttributeStringValue)
              .orElse(null);
        }
        else {
          log.info("Ignoring '{}' - {} appears and have precedence", eidasAttribute.getName(),
              se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_PLACE_OF_BIRTH_ATTRIBUTE_NAME);
        }
      }
      else if (se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_COUNTRY_OF_BIRTH_ATTRIBUTE_NAME
          .equals(eidasAttribute.getName())) {
        if (!eidasPlaceOfBirth) {
          countryOfBirth = Optional.ofNullable(this.toSwedishEidAttribute(eidasAttribute))
              .map(AttributeUtils::getAttributeStringValue)
              .orElse(null);
        }
        else {
          log.info("Ignoring '{}' - {} appears and have precedence", eidasAttribute.getName(),
              se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_PLACE_OF_BIRTH_ATTRIBUTE_NAME);
        }
      }
      else {
        Optional.ofNullable(this.toSwedishEidAttribute(eidasAttribute)).ifPresent(swedishEidAttributes::add);
      }
    }
    if (!eidasPlaceOfBirth && (townOfBirth != null || countryOfBirth != null)) {
      final StringBuilder sb = new StringBuilder();
      Optional.ofNullable(townOfBirth).ifPresent(sb::append);
      if (countryOfBirth != null) {
        if (!sb.isEmpty()) {
          sb.append(", ");
        }
        sb.append(countryOfBirth);
      }
      swedishEidAttributes.add(AttributeConstants.ATTRIBUTE_TEMPLATE_PLACE_OF_BIRTH.createBuilder()
          .value(sb.toString())
          .build());
    }

    return swedishEidAttributes;
  }

  /** {@inheritDoc} */
  @Override
  public UserAttribute toSwedishUserAttribute(final UserAttribute eidasAttribute) {
    return this.converters.stream()
        .filter(c -> c.supportsConversionToSwedishAttribute(eidasAttribute.getId()))
        .findFirst()
        .map(c -> c.toSwedishEidAttribute(eidasAttribute))
        .orElse(null);
  }

  /**
   * Implements special handling for PlaceOfBirth, CountryOfBirth and TownOfBirth ...
   */
  @Override
  public List<UserAttribute> toSwedishUserAttributes(final Collection<UserAttribute> eidasAttributes) {
    final List<UserAttribute> swedishEidAttributes = new ArrayList<>();
    boolean eidasPlaceOfBirth = false;
    String townOfBirth = null;
    String countryOfBirth = null;

    for (final UserAttribute eidasAttribute : eidasAttributes) {
      if (se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_PLACE_OF_BIRTH_ATTRIBUTE_NAME
          .equals(eidasAttribute.getId())) {
        eidasPlaceOfBirth = true;
      }

      if (se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_NAME
          .equals(eidasAttribute.getId())) {
        if (!eidasPlaceOfBirth) {
          townOfBirth = Optional.ofNullable(this.toSwedishUserAttribute(eidasAttribute))
              .map(UserAttribute::getStringValues)
              .filter(v -> !v.isEmpty())
              .map(v -> v.get(0))
              .orElse(null);
        }
        else {
          log.info("Ignoring '{}' - {} appears and have precedence", eidasAttribute.getId(),
              se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_PLACE_OF_BIRTH_ATTRIBUTE_NAME);
        }
      }
      else if (se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_COUNTRY_OF_BIRTH_ATTRIBUTE_NAME
          .equals(eidasAttribute.getId())) {
        if (!eidasPlaceOfBirth) {
          countryOfBirth = Optional.ofNullable(this.toSwedishUserAttribute(eidasAttribute))
              .map(UserAttribute::getStringValues)
              .filter(v -> !v.isEmpty())
              .map(v -> v.get(0))
              .orElse(null);
        }
        else {
          log.info("Ignoring '{}' - {} appears and have precedence", eidasAttribute.getId(),
              se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_PLACE_OF_BIRTH_ATTRIBUTE_NAME);
        }
      }
      else {
        Optional.ofNullable(this.toSwedishUserAttribute(eidasAttribute)).ifPresent(swedishEidAttributes::add);
      }
    }
    if (!eidasPlaceOfBirth && (townOfBirth != null || countryOfBirth != null)) {
      final StringBuilder sb = new StringBuilder();
      Optional.ofNullable(townOfBirth).ifPresent(sb::append);
      if (countryOfBirth != null) {
        if (!sb.isEmpty()) {
          sb.append(", ");
        }
        sb.append(countryOfBirth);
      }
      swedishEidAttributes.add(new UserAttribute(AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH,
          AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_PLACE_OF_BIRTH, sb.toString()));
    }

    return swedishEidAttributes;
  }

}
