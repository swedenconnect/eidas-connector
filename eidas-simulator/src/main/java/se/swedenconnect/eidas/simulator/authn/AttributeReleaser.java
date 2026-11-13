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
package se.swedenconnect.eidas.simulator.authn;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.springframework.util.StringUtils;
import se.swedenconnect.eidas.simulator.country.SimulatedCountry;
import se.swedenconnect.eidas.simulator.country.SimulatedUser;
import se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants;
import se.swedenconnect.opensaml.eidas.ext.attributes.AttributeUtils;
import se.swedenconnect.opensaml.eidas.ext.attributes.BirthNameType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CountryOfBirthType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CountryOfResidenceType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentAddressType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentFamilyNameType;
import se.swedenconnect.opensaml.eidas.ext.attributes.CurrentGivenNameType;
import se.swedenconnect.opensaml.eidas.ext.attributes.DateOfBirthType;
import se.swedenconnect.opensaml.eidas.ext.attributes.GenderType;
import se.swedenconnect.opensaml.eidas.ext.attributes.GenderTypeEnumeration;
import se.swedenconnect.opensaml.eidas.ext.attributes.NationalityType;
import se.swedenconnect.opensaml.eidas.ext.attributes.PersonIdentifierType;
import se.swedenconnect.opensaml.eidas.ext.attributes.PlaceOfBirthType;
import se.swedenconnect.opensaml.eidas.ext.attributes.TransliterationStringType;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Decides which attributes are released and creates them. The released attributes are the requested attributes
 * that the country supports and that the user has a value for.
 *
 * @author Martin Lindström
 */
public class AttributeReleaser {

  /** The natural person attributes that the simulator can release, in the order they are released. */
  public static final @NonNull List<String> RELEASABLE_ATTRIBUTES = List.of(
      AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_BIRTH_NAME_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_PLACE_OF_BIRTH_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_CURRENT_ADDRESS_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_GENDER_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_NATIONALITY_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_COUNTRY_OF_BIRTH_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_COUNTRY_OF_RESIDENCE_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_EMAIL_ADDRESS_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_PHONE_NUMBER_ATTRIBUTE_NAME);

  /**
   * Gets the names of the attributes that may be released for a request: the requested attributes that the country
   * supports and the simulator can produce. Whether they are released also depends on the user having a value.
   *
   * @param country the simulated country
   * @param request the request
   * @return the attribute names
   */
  public @NonNull List<String> getReleasableAttributes(final @NonNull SimulatedCountry country,
      final @NonNull ProcessedRequest request) {
    return RELEASABLE_ATTRIBUTES.stream()
        .filter(request::isRequested)
        .filter(country::isAttributeSupported)
        .toList();
  }

  /**
   * Gets the names of the required attributes that the user has no value for.
   *
   * @param country the simulated country
   * @param request the request
   * @param user the user
   * @return the attribute names, empty if all required attributes can be released
   */
  public @NonNull List<String> getMissingRequiredAttributes(final @NonNull SimulatedCountry country,
      final @NonNull ProcessedRequest request, final @NonNull SimulatedUser user) {
    final List<String> released = this.releaseAttributes(country, request, user).stream()
        .map(Attribute::getName)
        .toList();
    return request.requestedAttributes().stream()
        .filter(ProcessedRequest.RequestedAttribute::required)
        .map(ProcessedRequest.RequestedAttribute::name)
        .filter(n -> !released.contains(n))
        .toList();
  }

  /**
   * Creates the attributes that are released for a user.
   *
   * @param country the simulated country
   * @param request the request
   * @param user the user
   * @return the released attributes
   */
  public @NonNull List<Attribute> releaseAttributes(final @NonNull SimulatedCountry country,
      final @NonNull ProcessedRequest request, final @NonNull SimulatedUser user) {
    final List<Attribute> attributes = new ArrayList<>();
    for (final String name : this.getReleasableAttributes(country, request)) {
      final Attribute attribute = this.createAttribute(name, country, request, user);
      if (attribute != null) {
        attributes.add(attribute);
      }
    }
    return attributes;
  }

  /**
   * Creates one attribute.
   *
   * @param name the attribute name
   * @param country the simulated country
   * @param request the request
   * @param user the user
   * @return the attribute, or {@code null} if the user has no value for it
   */
  private @Nullable Attribute createAttribute(final @NonNull String name, final @NonNull SimulatedCountry country,
      final @NonNull ProcessedRequest request, final @NonNull SimulatedUser user) {

    return switch (name) {
      case AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME ->
          stringAttribute(name, AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_FRIENDLY_NAME,
              PersonIdentifierType.class, getPersonIdentifier(country, request, user));
      case AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME ->
          transliterated(name, AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_FRIENDLY_NAME,
              CurrentGivenNameType.class, user.getGivenName(), user.getGivenNameNonLatin());
      case AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME ->
          transliterated(name, AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_FRIENDLY_NAME,
              CurrentFamilyNameType.class, user.getFamilyName(), user.getFamilyNameNonLatin());
      case AttributeConstants.EIDAS_BIRTH_NAME_ATTRIBUTE_NAME ->
          transliterated(name, AttributeConstants.EIDAS_BIRTH_NAME_ATTRIBUTE_FRIENDLY_NAME,
              BirthNameType.class, user.getBirthName(), user.getBirthNameNonLatin());
      case AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME -> dateOfBirth(name, user.getDateOfBirth());
      case AttributeConstants.EIDAS_GENDER_ATTRIBUTE_NAME -> gender(name, user.getGender());
      case AttributeConstants.EIDAS_PLACE_OF_BIRTH_ATTRIBUTE_NAME ->
          stringAttribute(name, AttributeConstants.EIDAS_PLACE_OF_BIRTH_ATTRIBUTE_FRIENDLY_NAME,
              PlaceOfBirthType.class, user.getPlaceOfBirth());
      case AttributeConstants.EIDAS_CURRENT_ADDRESS_ATTRIBUTE_NAME -> currentAddress(name, user.getAddress());
      case AttributeConstants.EIDAS_NATIONALITY_ATTRIBUTE_NAME ->
          stringAttribute(name, AttributeConstants.EIDAS_NATIONALITY_ATTRIBUTE_FRIENDLY_NAME,
              NationalityType.class, user.getNationality());
      case AttributeConstants.EIDAS_COUNTRY_OF_BIRTH_ATTRIBUTE_NAME ->
          stringAttribute(name, AttributeConstants.EIDAS_COUNTRY_OF_BIRTH_ATTRIBUTE_FRIENDLY_NAME,
              CountryOfBirthType.class, user.getCountryOfBirth());
      case AttributeConstants.EIDAS_COUNTRY_OF_RESIDENCE_ATTRIBUTE_NAME ->
          stringAttribute(name, AttributeConstants.EIDAS_COUNTRY_OF_RESIDENCE_ATTRIBUTE_FRIENDLY_NAME,
              CountryOfResidenceType.class, user.getCountryOfResidence());
      case AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_NAME ->
          stringAttribute(name, AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_FRIENDLY_NAME,
              XSString.class, user.getTownOfBirth());
      case AttributeConstants.EIDAS_EMAIL_ADDRESS_ATTRIBUTE_NAME ->
          stringAttribute(name, AttributeConstants.EIDAS_EMAIL_ADDRESS_ATTRIBUTE_FRIENDLY_NAME,
              XSString.class, user.getEmailAddress());
      case AttributeConstants.EIDAS_PHONE_NUMBER_ATTRIBUTE_NAME ->
          stringAttribute(name, AttributeConstants.EIDAS_PHONE_NUMBER_ATTRIBUTE_FRIENDLY_NAME,
              XSString.class, user.getPhoneNumber());
      default -> null;
    };
  }

  /**
   * Gets the PersonIdentifier value of a user, {@code <country>/<destination country>/<identifier>}.
   *
   * @param country the simulated country
   * @param request the request
   * @param user the user
   * @return the PersonIdentifier value, or {@code null} if the user has no identifier
   */
  public static @Nullable String getPersonIdentifier(final @NonNull SimulatedCountry country,
      final @NonNull ProcessedRequest request, final @NonNull SimulatedUser user) {
    return StringUtils.hasText(user.getPersonIdentifier())
        ? "%s/%s/%s".formatted(country.getCode(), request.destinationCountry(), user.getPersonIdentifier().trim())
        : null;
  }

  /**
   * Creates an attribute with one string value.
   *
   * @param name the attribute name
   * @param friendlyName the friendly name
   * @param type the value type
   * @param value the value
   * @param <T> the value type
   * @return the attribute, or {@code null} if there is no value
   */
  private static <T extends XSString> @Nullable Attribute stringAttribute(final @NonNull String name,
      final @NonNull String friendlyName, final @NonNull Class<T> type, final @Nullable String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    final Attribute attribute = AttributeUtils.createAttribute(name, friendlyName);
    final T v = type == XSString.class
        ? type.cast(Objects.requireNonNull(XMLObjectProviderRegistrySupport.getBuilderFactory()
            .getBuilder(XSString.TYPE_NAME)).buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME))
        : AttributeUtils.createAttributeValueObject(type);
    v.setValue(value.trim());
    AttributeUtils.addAttributeValue(attribute, v);
    return attribute;
  }

  /**
   * Creates an attribute with a value in Latin script and, if given, a value in non-Latin script.
   *
   * @param name the attribute name
   * @param friendlyName the friendly name
   * @param type the value type
   * @param latin the value in Latin script
   * @param nonLatin the value in non-Latin script, or {@code null}
   * @param <T> the value type
   * @return the attribute, or {@code null} if there is no value
   */
  private static <T extends TransliterationStringType> @Nullable Attribute transliterated(final @NonNull String name,
      final @NonNull String friendlyName, final @NonNull Class<T> type, final @Nullable String latin,
      final @Nullable String nonLatin) {
    if (!StringUtils.hasText(latin) && !StringUtils.hasText(nonLatin)) {
      return null;
    }
    final Attribute attribute = AttributeUtils.createAttribute(name, friendlyName);
    if (StringUtils.hasText(latin)) {
      final T v = AttributeUtils.createAttributeValueObject(type);
      v.setValue(latin.trim());
      AttributeUtils.addAttributeValue(attribute, v);
    }
    if (StringUtils.hasText(nonLatin)) {
      final T v = AttributeUtils.createAttributeValueObject(type);
      v.setValue(nonLatin.trim());
      v.setLatinScript(false);
      AttributeUtils.addAttributeValue(attribute, v);
    }
    return attribute;
  }

  /**
   * Creates the DateOfBirth attribute.
   *
   * @param name the attribute name
   * @param value the date of birth, YYYY-MM-DD
   * @return the attribute, or {@code null} if there is no valid value
   */
  private static @Nullable Attribute dateOfBirth(final @NonNull String name, final @Nullable String value) {
    final LocalDate date = parse(value, LocalDate::parse);
    if (date == null) {
      return null;
    }
    final Attribute attribute =
        AttributeUtils.createAttribute(name, AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_FRIENDLY_NAME);
    final DateOfBirthType v = AttributeUtils.createAttributeValueObject(DateOfBirthType.class);
    v.setDate(date);
    AttributeUtils.addAttributeValue(attribute, v);
    return attribute;
  }

  /**
   * Creates the Gender attribute.
   *
   * @param name the attribute name
   * @param value the gender
   * @return the attribute, or {@code null} if there is no valid value
   */
  private static @Nullable Attribute gender(final @NonNull String name, final @Nullable String value) {
    final GenderTypeEnumeration gender = parse(value, GenderTypeEnumeration::fromValue);
    if (gender == null) {
      return null;
    }
    final Attribute attribute =
        AttributeUtils.createAttribute(name, AttributeConstants.EIDAS_GENDER_ATTRIBUTE_FRIENDLY_NAME);
    final GenderType v = AttributeUtils.createAttributeValueObject(GenderType.class);
    v.setGender(gender);
    AttributeUtils.addAttributeValue(attribute, v);
    return attribute;
  }

  /**
   * Creates the CurrentAddress attribute.
   *
   * @param name the attribute name
   * @param address the address
   * @return the attribute, or {@code null} if there is no address
   */
  private static @Nullable Attribute currentAddress(final @NonNull String name,
      final SimulatedUser.@NonNull Address address) {
    if (!address.hasValue()) {
      return null;
    }
    final Attribute attribute =
        AttributeUtils.createAttribute(name, AttributeConstants.EIDAS_CURRENT_ADDRESS_ATTRIBUTE_FRIENDLY_NAME);
    final CurrentAddressType v = AttributeUtils.createAttributeValueObject(CurrentAddressType.class);
    v.setPoBox(trim(address.getPoBox()));
    v.setLocatorDesignator(trim(address.getLocatorDesignator()));
    v.setLocatorName(trim(address.getLocatorName()));
    v.setCvaddressArea(trim(address.getCvAddressArea()));
    v.setThoroughfare(trim(address.getThoroughfare()));
    v.setPostName(trim(address.getPostName()));
    v.setAdminunitFirstline(trim(address.getAdminUnitFirstLine()));
    v.setAdminunitSecondline(trim(address.getAdminUnitSecondLine()));
    v.setPostCode(trim(address.getPostCode()));
    AttributeUtils.addAttributeValue(attribute, v);
    return attribute;
  }

  /**
   * Parses a value, returning {@code null} for an empty or invalid value.
   *
   * @param value the value
   * @param parser the parser
   * @param <T> the parsed type
   * @return the parsed value, or {@code null}
   */
  private static <T> @Nullable T parse(final @Nullable String value, final @NonNull Function<String, T> parser) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return parser.apply(value.trim());
    }
    catch (final DateTimeParseException | IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Trims a value, returning {@code null} for an empty value.
   *
   * @param value the value
   * @return the trimmed value, or {@code null}
   */
  private static @Nullable String trim(final @Nullable String value) {
    return StringUtils.hasText(value) ? Objects.requireNonNull(value).trim() : null;
  }

}
