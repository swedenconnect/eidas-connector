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
import org.opensaml.core.xml.schema.XSString;
import se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants;
import se.swedenconnect.opensaml.saml2.attribute.AttributeTemplate;
import se.swedenconnect.spring.authnserver.attributes.AttributeDefinition;
import se.swedenconnect.spring.authnserver.attributes.AttributeDefinitionRegistry;
import se.swedenconnect.spring.authnserver.attributes.DefaultAttributeDefinitionRegistry;
import se.swedenconnect.spring.authnserver.saml.attributes.SamlAttributeMapping;
import se.swedenconnect.spring.authnserver.saml.attributes.eidas.EidasAttributeTemplate;
import se.swedenconnect.spring.authnserver.saml.attributes.eidas.EidasNaturalPersonAttributes;
import se.swedenconnect.spring.authnserver.saml.attributes.eidas.mapping.EidasToProtocolMapper;
import se.swedenconnect.spring.authnserver.saml.attributes.mapping.SamlFromProtocolMapper;
import se.swedenconnect.spring.authnserver.saml.attributes.mapping.SamlToProtocolMapper;

/**
 * The generic attributes that the connector adds to those of the authentication server, and the SAML mapping that
 * handles them.
 * <p>
 * A Swedish Service Provider may request {@code eJusticeNaturalPersonRole}, {@code TownOfBirth} and
 * {@code CountryOfBirth} by their eIDAS attribute names. Such an attribute is passed on in the eIDAS request, and the
 * value received from the foreign country is released under the same name.
 * </p>
 *
 * @author Martin Lindström
 */
public final class ConnectorAttributes {

  /** The eIDAS {@code eJusticeNaturalPersonRole} attribute. */
  public static final @NonNull String EJUSTICE_NATURAL_PERSON_ROLE = "attribute.ejustice-natural-person-role";

  /** The eIDAS {@code TownOfBirth} attribute, released as received. */
  public static final @NonNull String EIDAS_TOWN_OF_BIRTH = "attribute.eidas-town-of-birth";

  /** The eIDAS {@code CountryOfBirth} attribute, released as received. */
  public static final @NonNull String EIDAS_COUNTRY_OF_BIRTH = "attribute.eidas-country-of-birth";

  /** The eIDAS template for {@code eJusticeNaturalPersonRole}. */
  public static final @NonNull EidasAttributeTemplate EJUSTICE_NATURAL_PERSON_ROLE_TEMPLATE =
      new EidasAttributeTemplate(
          AttributeConstants.EJUSTICE_NATURAL_PERSON_ROLE_ATTRIBUTE_NAME,
          AttributeConstants.EJUSTICE_NATURAL_PERSON_ROLE_ATTRIBUTE_FRIENDLY_NAME,
          XSString.TYPE_NAME, XSString.class);

  /**
   * Creates an attribute definition registry holding the built-in definitions and those of the connector.
   *
   * @return an {@link AttributeDefinitionRegistry}
   */
  public static @NonNull AttributeDefinitionRegistry createAttributeDefinitionRegistry() {
    final DefaultAttributeDefinitionRegistry registry = new DefaultAttributeDefinitionRegistry();
    registry.register(new AttributeDefinition(EJUSTICE_NATURAL_PERSON_ROLE, String.class, true,
        "The eIDAS eJusticeNaturalPersonRole attribute."));
    registry.register(AttributeDefinition.ofString(EIDAS_TOWN_OF_BIRTH,
        "The eIDAS TownOfBirth attribute, as received from the foreign country."));
    registry.register(AttributeDefinition.ofString(EIDAS_COUNTRY_OF_BIRTH,
        "The eIDAS CountryOfBirth attribute, as received from the foreign country."));
    return registry;
  }

  /**
   * Creates the SAML attribute mapping of the connector. It holds the built-in mappers of the authentication server,
   * and the mappers for the attributes of the connector.
   *
   * @return a {@link SamlAttributeMapping}
   */
  public static @NonNull SamlAttributeMapping createSamlAttributeMapping() {
    final SamlAttributeMapping mapping = new SamlAttributeMapping(createAttributeDefinitionRegistry());

    mapping.getFromProtocolMapping().register(new SamlFromProtocolMapper(
        EJUSTICE_NATURAL_PERSON_ROLE_TEMPLATE.name(), EJUSTICE_NATURAL_PERSON_ROLE));
    mapping.getFromProtocolMapping().register(new SamlFromProtocolMapper(
        EidasNaturalPersonAttributes.TOWN_OF_BIRTH.name(), EIDAS_TOWN_OF_BIRTH));
    mapping.getFromProtocolMapping().register(new SamlFromProtocolMapper(
        EidasNaturalPersonAttributes.COUNTRY_OF_BIRTH.name(), EIDAS_COUNTRY_OF_BIRTH));

    mapping.getToProtocolMapping().register(new SamlToProtocolMapper(EJUSTICE_NATURAL_PERSON_ROLE,
        new AttributeTemplate(EJUSTICE_NATURAL_PERSON_ROLE_TEMPLATE.name(),
            EJUSTICE_NATURAL_PERSON_ROLE_TEMPLATE.friendlyName())));
    mapping.getToProtocolMapping().register(
        new EidasToProtocolMapper(EIDAS_TOWN_OF_BIRTH, EidasNaturalPersonAttributes.TOWN_OF_BIRTH));
    mapping.getToProtocolMapping().register(
        new EidasToProtocolMapper(EIDAS_COUNTRY_OF_BIRTH, EidasNaturalPersonAttributes.COUNTRY_OF_BIRTH));

    return mapping;
  }

  // Hidden constructor
  private ConnectorAttributes() {
  }

}
