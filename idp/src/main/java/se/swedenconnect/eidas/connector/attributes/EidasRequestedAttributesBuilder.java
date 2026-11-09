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
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttribute;
import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;
import se.swedenconnect.spring.authnserver.saml.attributes.SamlRequestedAttribute;
import se.swedenconnect.spring.authnserver.saml.attributes.eidas.EidasAttributeTemplate;
import se.swedenconnect.spring.authnserver.saml.attributes.eidas.EidasNaturalPersonAttributes;
import se.swedenconnect.spring.authnserver.saml.attributes.requested.EidasRequestedAttributeProcessor;
import se.swedenconnect.spring.authnserver.saml.attributes.requested.EntityCategoryRequestedAttributeProcessor;
import se.swedenconnect.spring.authnserver.saml.attributes.requested.MetadataRequestedAttributeProcessor;
import se.swedenconnect.spring.authnserver.saml.attributes.requested.OasisExtensionRequestedAttributeProcessor;
import se.swedenconnect.spring.authnserver.saml.attributes.requested.RequestedAttributeContext;
import se.swedenconnect.spring.authnserver.saml.attributes.requested.RequestedAttributeProcessor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Works out the eIDAS requested attributes of an eIDAS authentication request from what a Swedish Service Provider
 * requested.
 * <p>
 * The rules are:
 * </p>
 * <ul>
 * <li>The attributes that the Service Provider requests in its metadata or in the {@code RequestedAttributes}
 * extensions of the request are passed on, mapped to their eIDAS counterparts.</li>
 * <li>The attributes that the Service Provider requests implicitly, through its entity categories, are passed on only
 * if they are required.</li>
 * <li>{@code TownOfBirth}, {@code CountryOfBirth} and {@code eJusticeNaturalPersonRole} may be requested by their
 * eIDAS names.</li>
 * <li>Only the attributes of the eIDAS minimum data set are marked as required.</li>
 * <li>The minimum data set is always requested when asked for.</li>
 * </ul>
 *
 * @author Martin Lindström
 */
public class EidasRequestedAttributesBuilder {

  /** Logger instance. */
  private static final Logger log = LoggerFactory.getLogger(EidasRequestedAttributesBuilder.class);

  /** The eIDAS minimum data set for natural persons. */
  public static final @NonNull List<EidasAttributeTemplate> NATURAL_PERSON_MINIMUM_DATASET = List.of(
      EidasNaturalPersonAttributes.PERSON_IDENTIFIER,
      EidasNaturalPersonAttributes.CURRENT_GIVEN_NAME,
      EidasNaturalPersonAttributes.CURRENT_FAMILY_NAME,
      EidasNaturalPersonAttributes.DATE_OF_BIRTH);

  /** The eIDAS attribute for each attribute that a Swedish Service Provider may request. */
  private static final Map<String, EidasAttributeTemplate> EIDAS_TEMPLATES = new LinkedHashMap<>();

  static {
    EIDAS_TEMPLATES.put(AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER,
        EidasNaturalPersonAttributes.PERSON_IDENTIFIER);
    EIDAS_TEMPLATES.put(AttributeConstants.ATTRIBUTE_NAME_SN, EidasNaturalPersonAttributes.CURRENT_FAMILY_NAME);
    EIDAS_TEMPLATES.put(AttributeConstants.ATTRIBUTE_NAME_GIVEN_NAME, EidasNaturalPersonAttributes.CURRENT_GIVEN_NAME);
    EIDAS_TEMPLATES.put(AttributeConstants.ATTRIBUTE_NAME_DATE_OF_BIRTH, EidasNaturalPersonAttributes.DATE_OF_BIRTH);
    EIDAS_TEMPLATES.put(AttributeConstants.ATTRIBUTE_NAME_BIRTH_NAME, EidasNaturalPersonAttributes.BIRTH_NAME);
    EIDAS_TEMPLATES.put(AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH,
        EidasNaturalPersonAttributes.PLACE_OF_BIRTH);
    EIDAS_TEMPLATES.put(AttributeConstants.ATTRIBUTE_NAME_COUNTRY_OF_CITIZENSHIP,
        EidasNaturalPersonAttributes.NATIONALITY);
    EIDAS_TEMPLATES.put(AttributeConstants.ATTRIBUTE_NAME_COUNTRY_OF_RESIDENCE,
        EidasNaturalPersonAttributes.COUNTRY_OF_RESIDENCE);
    EIDAS_TEMPLATES.put(AttributeConstants.ATTRIBUTE_NAME_TELEPHONE_NUMBER, EidasNaturalPersonAttributes.PHONE_NUMBER);
    EIDAS_TEMPLATES.put(AttributeConstants.ATTRIBUTE_NAME_MAIL, EidasNaturalPersonAttributes.EMAIL_ADDRESS);
    EIDAS_TEMPLATES.put(AttributeConstants.ATTRIBUTE_NAME_EIDAS_NATURAL_PERSON_ADDRESS,
        EidasNaturalPersonAttributes.CURRENT_ADDRESS);
    EIDAS_TEMPLATES.put(AttributeConstants.ATTRIBUTE_NAME_GENDER, EidasNaturalPersonAttributes.GENDER);

    // Attributes that a Swedish Service Provider requests by their eIDAS names ...
    //
    EIDAS_TEMPLATES.put(ConnectorAttributes.EJUSTICE_NATURAL_PERSON_ROLE_TEMPLATE.name(),
        ConnectorAttributes.EJUSTICE_NATURAL_PERSON_ROLE_TEMPLATE);
    EIDAS_TEMPLATES.put(EidasNaturalPersonAttributes.TOWN_OF_BIRTH.name(), EidasNaturalPersonAttributes.TOWN_OF_BIRTH);
    EIDAS_TEMPLATES.put(EidasNaturalPersonAttributes.COUNTRY_OF_BIRTH.name(),
        EidasNaturalPersonAttributes.COUNTRY_OF_BIRTH);
  }

  /** The processors for the attributes that the Service Provider requests explicitly. */
  private final @NonNull List<RequestedAttributeProcessor> explicitProcessors = List.of(
      new MetadataRequestedAttributeProcessor(),
      new OasisExtensionRequestedAttributeProcessor(),
      new EidasRequestedAttributeProcessor());

  /** The processor for the attributes that the Service Provider requests through its entity categories. */
  private final @NonNull EntityCategoryRequestedAttributeProcessor entityCategoryProcessor;

  /**
   * Constructor.
   *
   * @param idpEntityCategories the entity categories that the Identity Provider declares
   */
  public EidasRequestedAttributesBuilder(final @NonNull List<String> idpEntityCategories) {
    this.entityCategoryProcessor = new EntityCategoryRequestedAttributeProcessor(
        Objects.requireNonNull(idpEntityCategories, "idpEntityCategories must not be null"));
  }

  /**
   * Works out the eIDAS requested attributes for an authentication request from a Swedish Service Provider.
   *
   * @param authnRequest the authentication request from the Swedish Service Provider
   * @param spMetadata the metadata of the Swedish Service Provider
   * @param includeMinimumDataSet whether the eIDAS minimum data set should always be requested
   * @return the eIDAS requested attributes
   */
  public @NonNull List<RequestedAttribute> build(final @NonNull AuthnRequest authnRequest,
      final @NonNull EntityDescriptor spMetadata, final boolean includeMinimumDataSet) {

    final RequestedAttributeContext context = new RequestedAttributeContext(authnRequest, spMetadata);

    // The attributes that are requested explicitly come first. An attribute that is requested by several sources is
    // required only if all of them require it.
    //
    final Map<String, Requested> requested = new LinkedHashMap<>();
    for (final RequestedAttributeProcessor processor : this.explicitProcessors) {
      for (final SamlRequestedAttribute attribute : processor.extractRequestedAttributes(context)) {
        requested.merge(attribute.name(), new Requested(false, attribute.required()),
            (a, b) -> new Requested(a.implicit(), a.required() && b.required()));
      }
    }
    for (final SamlRequestedAttribute attribute : this.entityCategoryProcessor.extractRequestedAttributes(context)) {
      requested.merge(attribute.name(), new Requested(true, attribute.required()),
          (a, b) -> new Requested(a.implicit(), a.required() && b.required()));
    }

    final List<RequestedAttribute> eidasAttributes = new ArrayList<>();
    requested.forEach((name, r) -> {
      if (r.implicit() && !r.required()) {
        // Don't include implicitly requested attributes that are not required ...
        return;
      }
      final EidasAttributeTemplate template = EIDAS_TEMPLATES.get(name);
      if (template == null) {
        log.trace("Requested attribute '{}' has no eIDAS counterpart", name);
        return;
      }
      eidasAttributes.add(createRequestedAttribute(template, NATURAL_PERSON_MINIMUM_DATASET.contains(template)));
    });

    if (includeMinimumDataSet) {
      for (final EidasAttributeTemplate template : NATURAL_PERSON_MINIMUM_DATASET) {
        if (eidasAttributes.stream().noneMatch(a -> Objects.equals(template.name(), a.getName()))) {
          eidasAttributes.add(createRequestedAttribute(template, true));
        }
      }
    }
    return eidasAttributes;
  }

  /**
   * Creates an eIDAS {@link RequestedAttribute} element.
   *
   * @param template the eIDAS attribute template
   * @param required whether the attribute is required
   * @return a {@link RequestedAttribute}
   */
  private static RequestedAttribute createRequestedAttribute(
      final EidasAttributeTemplate template, final boolean required) {
    final RequestedAttribute requestedAttribute =
        (RequestedAttribute) XMLObjectSupport.buildXMLObject(RequestedAttribute.DEFAULT_ELEMENT_NAME);
    requestedAttribute.setName(template.name());
    requestedAttribute.setFriendlyName(template.friendlyName());
    requestedAttribute.setNameFormat(Attribute.URI_REFERENCE);
    requestedAttribute.setIsRequired(required);
    return requestedAttribute;
  }

  /**
   * How an attribute was requested.
   *
   * @param implicit whether the attribute was requested through an entity category only
   * @param required whether the attribute is required
   */
  private record Requested(boolean implicit, boolean required) {
  }

}
