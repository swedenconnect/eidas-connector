/*
 * Copyright 2017-2026 Sweden Connect
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
package se.swedenconnect.eidas.connector.attributes;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentAddressType;
import se.swedenconnect.opensaml.eidas.ext.attributes.EidasAttributeValueType;
import se.swedenconnect.opensaml.eidas.ext.attributes.TransliterationStringType;
import se.swedenconnect.spring.authnserver.attributes.AttributeIdentifiers;
import se.swedenconnect.spring.authnserver.attributes.GenericAttribute;
import se.swedenconnect.spring.authnserver.saml.attributes.EidasNaturalPersonAddress;
import se.swedenconnect.spring.authnserver.saml.attributes.eidas.EidasAttributeValues;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converts the attributes of an eIDAS assertion into the generic attributes of the authentication server, which
 * releases them as Swedish eID attributes.
 * <p>
 * The rules are:
 * </p>
 * <ul>
 * <li>An attribute with several values gives its first value. For a transliterated attribute, the first value in Latin
 * script is used.</li>
 * <li>The gender {@code Male}, {@code Female} or {@code Unspecified} is released as {@code M}, {@code F} or {@code U}.
 * Any other value is released as {@code U}.</li>
 * <li>The current address is released as the {@code eidasNaturalPersonAddress} attribute.</li>
 * <li>{@code TownOfBirth} and {@code CountryOfBirth} give the place of birth, {@code "town, country"}, only when the
 * assertion holds no {@code PlaceOfBirth}. They are also kept as they are, for a Service Provider that requested
 * them by their eIDAS names.</li>
 * <li>{@code eJusticeNaturalPersonRole} is kept with all its values.</li>
 * <li>Attributes that the connector does not know are left out.</li>
 * </ul>
 *
 * @author Martin Lindström
 */
public class EidasAttributeConverter {

  /** Logger instance. */
  private static final Logger log = LoggerFactory.getLogger(EidasAttributeConverter.class);

  /** eIDAS attributes that map to exactly one generic attribute with a string value. */
  private static final Map<String, String> STRING_ATTRIBUTES = Map.of(
      AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME, AttributeIdentifiers.EIDAS_PERSON_IDENTIFIER,
      AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME, AttributeIdentifiers.SURNAME,
      AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME, AttributeIdentifiers.GIVEN_NAME,
      AttributeConstants.EIDAS_BIRTH_NAME_ATTRIBUTE_NAME, AttributeIdentifiers.BIRTH_NAME,
      AttributeConstants.EIDAS_PLACE_OF_BIRTH_ATTRIBUTE_NAME, AttributeIdentifiers.PLACE_OF_BIRTH,
      AttributeConstants.EIDAS_NATIONALITY_ATTRIBUTE_NAME, AttributeIdentifiers.COUNTRY_OF_CITIZENSHIP,
      AttributeConstants.EIDAS_COUNTRY_OF_RESIDENCE_ATTRIBUTE_NAME, AttributeIdentifiers.COUNTRY_OF_RESIDENCE,
      AttributeConstants.EIDAS_PHONE_NUMBER_ATTRIBUTE_NAME, AttributeIdentifiers.TELEPHONE_NUMBER,
      AttributeConstants.EIDAS_EMAIL_ADDRESS_ATTRIBUTE_NAME, AttributeIdentifiers.EMAIL);

  /**
   * Converts the attributes of an eIDAS assertion into generic attributes.
   *
   * @param eidasAttributes the attributes of the eIDAS assertion
   * @return the generic attributes
   */
  public @NonNull List<GenericAttribute<?>> toGenericAttributes(final @NonNull List<Attribute> eidasAttributes) {
    final List<GenericAttribute<?>> attributes = new ArrayList<>();

    final boolean hasPlaceOfBirth = eidasAttributes.stream()
        .anyMatch(a -> AttributeConstants.EIDAS_PLACE_OF_BIRTH_ATTRIBUTE_NAME.equals(a.getName()));

    for (final Attribute eidasAttribute : eidasAttributes) {
      final String name = eidasAttribute.getName();
      if (name == null) {
        continue;
      }
      final String identifier = STRING_ATTRIBUTES.get(name);
      if (identifier != null) {
        addString(attributes, identifier, getValue(eidasAttribute));
      }
      else if (AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME.equals(name)) {
        final String value = getValue(eidasAttribute);
        if (value != null) {
          try {
            attributes.add(GenericAttribute.of(AttributeIdentifiers.DATE_OF_BIRTH, LocalDate.parse(value)));
          }
          catch (final DateTimeParseException e) {
            log.info("Received {} attribute with invalid value '{}' - ignored", name, value);
          }
        }
      }
      else if (AttributeConstants.EIDAS_GENDER_ATTRIBUTE_NAME.equals(name)) {
        final String value = getValue(eidasAttribute);
        if (value != null) {
          attributes.add(GenericAttribute.of(AttributeIdentifiers.GENDER,
              Objects.requireNonNullElse(EidasAttributeValues.fromGender(value), "u")));
        }
      }
      else if (AttributeConstants.EIDAS_CURRENT_ADDRESS_ATTRIBUTE_NAME.equals(name)) {
        final String value = getValue(eidasAttribute);
        EidasNaturalPersonAddress.parse(value).forEach((id, part) -> addString(attributes, id, part));
      }
      else if (AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_NAME.equals(name)) {
        addStrings(attributes, ConnectorAttributes.EIDAS_TOWN_OF_BIRTH, getValues(eidasAttribute));
        if (!hasPlaceOfBirth) {
          addString(attributes, AttributeIdentifiers.PLACE_OF_BIRTH_LOCALITY, getValue(eidasAttribute));
        }
        else {
          log.info("Ignoring '{}' for the place of birth - {} appears and has precedence", name,
              AttributeConstants.EIDAS_PLACE_OF_BIRTH_ATTRIBUTE_NAME);
        }
      }
      else if (AttributeConstants.EIDAS_COUNTRY_OF_BIRTH_ATTRIBUTE_NAME.equals(name)) {
        addStrings(attributes, ConnectorAttributes.EIDAS_COUNTRY_OF_BIRTH, getValues(eidasAttribute));
        if (!hasPlaceOfBirth) {
          addString(attributes, AttributeIdentifiers.PLACE_OF_BIRTH_COUNTRY, getValue(eidasAttribute));
        }
        else {
          log.info("Ignoring '{}' for the place of birth - {} appears and has precedence", name,
              AttributeConstants.EIDAS_PLACE_OF_BIRTH_ATTRIBUTE_NAME);
        }
      }
      else if (AttributeConstants.EJUSTICE_NATURAL_PERSON_ROLE_ATTRIBUTE_NAME.equals(name)) {
        addStrings(attributes, ConnectorAttributes.EJUSTICE_NATURAL_PERSON_ROLE, getValues(eidasAttribute));
      }
      else {
        log.debug("No mapping exists for eIDAS attribute '{}' - ignored", name);
      }
    }
    return attributes;
  }

  /**
   * Gets the value of an eIDAS attribute as a string. If the attribute has several values, the first one is used,
   * except for a transliterated attribute, where the first value in Latin script is used.
   *
   * @param attribute the eIDAS attribute
   * @return the value, or {@code null} if the attribute has no value that can be read
   */
  public static @Nullable String getValue(final @NonNull Attribute attribute) {
    final List<XMLObject> values = attribute.getAttributeValues();
    if (values.isEmpty()) {
      log.info("No attribute value present for eIDAS attribute '{}' - no conversion will be made",
          attribute.getName());
      return null;
    }
    XMLObject value = values.getFirst();
    if (values.size() > 1 && value instanceof TransliterationStringType) {
      value = values.stream()
          .filter(TransliterationStringType.class::isInstance)
          .map(TransliterationStringType.class::cast)
          .filter(v -> Boolean.TRUE.equals(v.getLatinScript()))
          .map(XMLObject.class::cast)
          .findFirst()
          .orElse(value);
    }
    return toString(attribute, value);
  }

  /**
   * Gets all values of an eIDAS attribute as strings.
   *
   * @param attribute the eIDAS attribute
   * @return the values
   */
  public static @NonNull List<String> getValues(final @NonNull Attribute attribute) {
    return attribute.getAttributeValues().stream()
        .map(v -> toString(attribute, v))
        .filter(Objects::nonNull)
        .toList();
  }

  /**
   * Gets the string representation of an attribute value.
   *
   * @param attribute the attribute (for logging)
   * @param value the value
   * @return the string, or {@code null} if the value type is not supported
   */
  private static String toString(final Attribute attribute, final XMLObject value) {
    if (value instanceof final CurrentAddressType address) {
      return address.toSwedishEidString();
    }
    if (value instanceof final EidasAttributeValueType eidasValue) {
      return eidasValue.toStringValue();
    }
    if (value instanceof final XSString stringValue) {
      return stringValue.getValue();
    }
    log.info("Unsupported value type {} for eIDAS attribute '{}'", value.getElementQName(), attribute.getName());
    return null;
  }

  /**
   * Adds a string attribute, provided that the value is set.
   *
   * @param attributes the list to add to
   * @param identifier the attribute identifier
   * @param value the value
   */
  private static void addString(
      final List<GenericAttribute<?>> attributes, final String identifier, final String value) {
    if (value != null && !value.isEmpty()) {
      attributes.add(GenericAttribute.of(identifier, value));
    }
  }

  /**
   * Adds a string attribute with several values, provided that there are values.
   *
   * @param attributes the list to add to
   * @param identifier the attribute identifier
   * @param values the values
   */
  private static void addStrings(
      final List<GenericAttribute<?>> attributes, final String identifier, final List<String> values) {
    final List<Serializable> nonEmpty = values.stream()
        .filter(v -> !v.isEmpty())
        .map(Serializable.class::cast)
        .toList();
    if (!nonEmpty.isEmpty()) {
      attributes.add(new GenericAttribute<>(identifier, nonEmpty));
    }
  }

}
