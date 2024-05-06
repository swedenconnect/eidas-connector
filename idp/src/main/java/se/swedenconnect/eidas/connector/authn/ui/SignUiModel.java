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
package se.swedenconnect.eidas.connector.authn.ui;

import lombok.Data;
import lombok.EqualsAndHashCode;
import se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants;
import se.swedenconnect.opensaml.eidas.ext.attributes.TransliterationStringType;
import se.swedenconnect.spring.saml.idp.attributes.UserAttribute;
import se.swedenconnect.spring.saml.idp.attributes.eidas.EidasAttributeValue;
import se.swedenconnect.spring.saml.idp.attributes.eidas.TransliterationString;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Model class for the Sign Consent view.
 *
 * @author Martin Lindström
 */
@EqualsAndHashCode(callSuper = true)
@Data
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
   * User info.
   */
  @Data
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

    public UserInfo(final List<UserAttribute> attributes) {

      String givenName = null;
      String surName = null;

      for (final UserAttribute attribute : attributes) {
        if (AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME.equals(attribute.getId())) {
          this.internationalId = getEidasAttributeValue(attribute.getValues());
        }
        else if (AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME.equals(attribute.getId())) {
          this.dateOfBirth = getEidasAttributeValue(attribute.getValues());
        }
        else if (AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME.equals(attribute.getId())) {
          givenName = getEidasAttributeValue(attribute.getValues());
        }
        else if (AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME.equals(attribute.getId())) {
          surName = getEidasAttributeValue(attribute.getValues());
        }
        else if (se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_MAPPED_PERSONAL_IDENTITY_NUMBER
            .equals(attribute.getId())) {
          this.swedishId = attribute.getValues().stream()
              .filter(Objects::nonNull)
              .map(String.class::cast)
              .findFirst()
              .orElse(null);
        }
      }

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

    private static String getEidasAttributeValue(final List<? extends Serializable> values) {
      final boolean transliterated = !values.isEmpty() && values.get(0) instanceof TransliterationString;
      if (transliterated && values.size() > 1) {
        return values.stream()
            .filter(Objects::nonNull)
            .map(TransliterationString.class::cast)
            .map(TransliterationString::createXmlObject)
            .filter(TransliterationStringType::getLatinScript)
            .map(TransliterationStringType::getValue)
            .findFirst()
            .orElse(null);
      }
      else {
        return values.stream()
            .filter(Objects::nonNull)
            .map(EidasAttributeValue.class::cast)
            .map(EidasAttributeValue::getValueAsString)
            .findFirst()
            .orElse(null);
      }
    }

  }

}
