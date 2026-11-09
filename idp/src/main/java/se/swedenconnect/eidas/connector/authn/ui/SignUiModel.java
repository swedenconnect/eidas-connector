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
package se.swedenconnect.eidas.connector.authn.ui;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Attribute;
import se.swedenconnect.eidas.connector.attributes.EidasAttributeConverter;
import se.swedenconnect.eidas.connector.authn.EidasAuthenticationToken;
import se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants;
import se.swedenconnect.opensaml.eidas.ext.attributes.TransliterationStringType;
import se.swedenconnect.spring.authnserver.attributes.AttributeIdentifiers;
import se.swedenconnect.spring.authnserver.attributes.GenericAttribute;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Model class for the Sign Consent view.
 *
 * @author Martin Lindström
 */
public class SignUiModel extends BaseUiModel {

  /**
   * The sign message.
   */
  private String textMessage;

  /**
   * Information about the user that signs.
   */
  private UserInfo userInfo;

  /**
   * Gets the sign message.
   *
   * @return the sign message
   */
  public String getTextMessage() {
    return this.textMessage;
  }

  /**
   * Assigns the sign message.
   *
   * @param textMessage the sign message
   */
  public void setTextMessage(final String textMessage) {
    this.textMessage = textMessage;
  }

  /**
   * Gets the information about the user that signs.
   *
   * @return the user info
   */
  public UserInfo getUserInfo() {
    return this.userInfo;
  }

  /**
   * Assigns the information about the user that signs.
   *
   * @param userInfo the user info
   */
  public void setUserInfo(final UserInfo userInfo) {
    this.userInfo = userInfo;
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    final SignUiModel that = (SignUiModel) o;
    return Objects.equals(this.textMessage, that.textMessage) && Objects.equals(this.userInfo, that.userInfo);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), this.textMessage, this.userInfo);
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "textMessage=" + this.textMessage + ", userInfo=[" + this.userInfo + "]";
  }

  /**
   * User info.
   */
  public static class UserInfo {

    /**
     * The display name.
     */
    private String name;

    /**
     * The Swedish personal identity number.
     */
    private String swedishId;

    /**
     * The date of birth
     */
    private String dateOfBirth;

    /**
     * The eIDAS person identifier
     */
    private String internationalId;

    /**
     * Default constructor.
     */
    public UserInfo() {
    }

    /**
     * Constructor setting up the user info from the eIDAS authentication.
     *
     * @param token the eIDAS authentication token
     */
    public UserInfo(final EidasAuthenticationToken token) {

      String givenName = null;
      String surName = null;

      for (final Attribute attribute : token.getEidasAttributes()) {
        if (AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME.equals(attribute.getName())) {
          this.internationalId = getEidasAttributeValue(attribute);
        }
        else if (AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME.equals(attribute.getName())) {
          this.dateOfBirth = getEidasAttributeValue(attribute);
        }
        else if (AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME.equals(attribute.getName())) {
          givenName = getEidasAttributeValue(attribute);
        }
        else if (AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME.equals(attribute.getName())) {
          surName = getEidasAttributeValue(attribute);
        }
      }
      this.swedishId = Stream.of(AttributeIdentifiers.MAPPED_PERSONAL_IDENTITY_NUMBER,
              AttributeIdentifiers.MAPPED_COORDINATION_NUMBER)
          .map(token::getAttribute)
          .filter(Objects::nonNull)
          .map(GenericAttribute::getStringValues)
          .flatMap(List::stream)
          .findFirst()
          .orElse(null);

      if (this.swedishId != null && this.swedishId.length() == 12) {
        this.swedishId = "%s-%s".formatted(this.swedishId.substring(0, 8), this.swedishId.substring(8));
      }

      final StringBuilder _name = new StringBuilder();
      if (givenName != null) {
        _name.append(givenName);
      }
      if (surName != null) {
        if (!_name.isEmpty()) {
          _name.append(" ");
        }
        _name.append(surName);
      }
      this.name = !_name.isEmpty() ? _name.toString() : null;
    }

    /**
     * Gets the display name.
     *
     * @return the display name
     */
    public String getName() {
      return this.name;
    }

    /**
     * Assigns the display name.
     *
     * @param name the display name
     */
    public void setName(final String name) {
      this.name = name;
    }

    /**
     * Gets the Swedish personal identity number.
     *
     * @return the Swedish personal identity number
     */
    public String getSwedishId() {
      return this.swedishId;
    }

    /**
     * Assigns the Swedish personal identity number.
     *
     * @param swedishId the Swedish personal identity number
     */
    public void setSwedishId(final String swedishId) {
      this.swedishId = swedishId;
    }

    /**
     * Gets the date of birth.
     *
     * @return the date of birth
     */
    public String getDateOfBirth() {
      return this.dateOfBirth;
    }

    /**
     * Assigns the date of birth.
     *
     * @param dateOfBirth the date of birth
     */
    public void setDateOfBirth(final String dateOfBirth) {
      this.dateOfBirth = dateOfBirth;
    }

    /**
     * Gets the eIDAS person identifier.
     *
     * @return the eIDAS person identifier
     */
    public String getInternationalId() {
      return this.internationalId;
    }

    /**
     * Assigns the eIDAS person identifier.
     *
     * @param internationalId the eIDAS person identifier
     */
    public void setInternationalId(final String internationalId) {
      this.internationalId = internationalId;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || this.getClass() != o.getClass()) {
        return false;
      }
      final UserInfo that = (UserInfo) o;
      return Objects.equals(this.name, that.name) && Objects.equals(this.swedishId, that.swedishId)
          && Objects.equals(this.dateOfBirth, that.dateOfBirth)
          && Objects.equals(this.internationalId, that.internationalId);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return Objects.hash(this.name, this.swedishId, this.dateOfBirth, this.internationalId);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return "name=" + this.name + ", swedishId=" + this.swedishId + ", dateOfBirth="
          + this.dateOfBirth + ", internationalId=" + this.internationalId;
    }

    /**
     * Gets the value of an eIDAS attribute for display. For a transliterated attribute with several values, the value
     * in Latin script is used.
     *
     * @param attribute the eIDAS attribute
     * @return the value, or {@code null}
     */
    private static String getEidasAttributeValue(final Attribute attribute) {
      final List<XMLObject> values = attribute.getAttributeValues();
      final boolean transliterated = !values.isEmpty() && values.getFirst() instanceof TransliterationStringType;
      if (transliterated && values.size() > 1) {
        return values.stream()
            .filter(TransliterationStringType.class::isInstance)
            .map(TransliterationStringType.class::cast)
            .filter(v -> Boolean.TRUE.equals(v.getLatinScript()))
            .map(TransliterationStringType::getValue)
            .findFirst()
            .orElse(null);
      }
      return EidasAttributeConverter.getValue(attribute);
    }

  }

}
