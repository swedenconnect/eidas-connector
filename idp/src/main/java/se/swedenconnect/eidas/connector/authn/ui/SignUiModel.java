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

import lombok.Data;
import lombok.EqualsAndHashCode;
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
