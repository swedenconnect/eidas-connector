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

import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import se.swedenconnect.eidas.connector.OpenSamlTestBase;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttribute;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttributes;
import se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants;
import se.swedenconnect.opensaml.saml2.core.build.AuthnRequestBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.AttributeConsumingServiceBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.EntityAttributesBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.EntityDescriptorBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.ExtensionsBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.RequestedAttributeBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.SPSSODescriptorBuilder;
import se.swedenconnect.opensaml.sweid.saml2.metadata.entitycategory.EntityCategoryConstants;
import se.swedenconnect.opensaml.sweid.saml2.metadata.entitycategory.ServiceEntityCategory;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EidasRequestedAttributesBuilder}.
 *
 * @author Martin Lindström
 */
class EidasRequestedAttributesBuilderTest extends OpenSamlTestBase {

  private static final List<String> MINIMUM_DATASET = List.of(
      AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME,
      AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME);

  private static final ServiceEntityCategory NATURAL_PERSON =
      (ServiceEntityCategory) EntityCategoryConstants.SERVICE_ENTITY_CATEGORY_EIDAS_NATURAL_PERSON;

  private final EidasRequestedAttributesBuilder builder =
      new EidasRequestedAttributesBuilder(List.of(NATURAL_PERSON.getUri()));

  @Test
  void testMinimumDataSetOnly() {
    final List<RequestedAttribute> result = this.builder.build(authnRequest(), spMetadata(null), true);
    assertThat(names(result)).containsExactlyElementsOf(MINIMUM_DATASET);
    assertThat(result).allMatch(RequestedAttribute::isRequired);
  }

  @Test
  void testNoMinimumDataSet() {
    assertThat(this.builder.build(authnRequest(), spMetadata(null), false)).isEmpty();
  }

  @Test
  void testRequestedInMetadata() {
    final EntityDescriptor metadata = spMetadata(null,
        se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_PLACE_OF_BIRTH,
        se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_GIVEN_NAME,
        se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants.ATTRIBUTE_NAME_C);

    final List<RequestedAttribute> result = this.builder.build(authnRequest(), metadata, true);

    // c has no eIDAS counterpart, and only the minimum data set is required ...
    assertThat(names(result)).containsExactly(
        AttributeConstants.EIDAS_PLACE_OF_BIRTH_ATTRIBUTE_NAME,
        AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME,
        AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME,
        AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME,
        AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME);
    assertThat(result.getFirst().isRequired()).isFalse();
    assertThat(result.subList(1, result.size())).allMatch(RequestedAttribute::isRequired);
  }

  @Test
  void testRequestedByEidasName() {
    final List<RequestedAttribute> result = this.builder.build(
        authnRequest(AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_NAME,
            AttributeConstants.EIDAS_COUNTRY_OF_BIRTH_ATTRIBUTE_NAME,
            AttributeConstants.EJUSTICE_NATURAL_PERSON_ROLE_ATTRIBUTE_NAME,
            AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME),
        spMetadata(null), true);

    // Other eIDAS names than TownOfBirth, CountryOfBirth and eJusticeNaturalPersonRole are not passed on ...
    assertThat(names(result)).containsExactly(
        AttributeConstants.EIDAS_TOWN_OF_BIRTH_ATTRIBUTE_NAME,
        AttributeConstants.EIDAS_COUNTRY_OF_BIRTH_ATTRIBUTE_NAME,
        AttributeConstants.EJUSTICE_NATURAL_PERSON_ROLE_ATTRIBUTE_NAME,
        AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME,
        AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME,
        AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME,
        AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME);
    assertThat(result.subList(0, 3)).noneMatch(RequestedAttribute::isRequired);
  }

  @Test
  void testImplicitlyRequestedAttributes() {
    final List<RequestedAttribute> result =
        this.builder.build(authnRequest(), spMetadata(NATURAL_PERSON.getUri()), false);

    // Only the required attributes of the entity category are passed on ...
    final List<String> required = Arrays.stream(NATURAL_PERSON.getAttributeSet().getRequiredAttributes())
        .map(t -> t.getName())
        .toList();
    final List<String> recommended = Arrays.stream(NATURAL_PERSON.getAttributeSet().getRecommendedAttributes())
        .map(t -> t.getName())
        .toList();

    final List<RequestedAttribute> expected = this.builder.build(authnRequest(),
        spMetadata(null, required.toArray(String[]::new)), false);
    assertThat(names(result)).containsExactlyElementsOf(names(expected));

    final List<RequestedAttribute> notExpected = this.builder.build(authnRequest(),
        spMetadata(null, recommended.toArray(String[]::new)), false);
    names(notExpected).stream()
        .filter(n -> !names(expected).contains(n))
        .forEach(n -> assertThat(names(result)).doesNotContain(n));
  }

  @Test
  void testImplicitNotRequiredButExplicitlyRequested() {
    final List<String> recommended = Arrays.stream(NATURAL_PERSON.getAttributeSet().getRecommendedAttributes())
        .map(t -> t.getName())
        .toList();
    final List<RequestedAttribute> explicit = this.builder.build(authnRequest(),
        spMetadata(null, recommended.toArray(String[]::new)), false);

    final List<RequestedAttribute> result = this.builder.build(authnRequest(),
        spMetadata(NATURAL_PERSON.getUri(), recommended.toArray(String[]::new)), false);
    assertThat(names(result)).containsAll(names(explicit));
  }

  private static List<String> names(final List<RequestedAttribute> attributes) {
    return attributes.stream().map(RequestedAttribute::getName).toList();
  }

  private static AuthnRequest authnRequest(final String... eidasRequestedAttributes) {
    final AuthnRequestBuilder builder = AuthnRequestBuilder.builder()
        .id("_request")
        .issuer("https://sp.example.com");
    if (eidasRequestedAttributes.length > 0) {
      final RequestedAttributes extension =
          (RequestedAttributes) XMLObjectSupport.buildXMLObject(RequestedAttributes.DEFAULT_ELEMENT_NAME);
      for (final String name : eidasRequestedAttributes) {
        final RequestedAttribute attribute =
            (RequestedAttribute) XMLObjectSupport.buildXMLObject(RequestedAttribute.DEFAULT_ELEMENT_NAME);
        attribute.setName(name);
        attribute.setIsRequired(false);
        extension.getRequestedAttributes().add(attribute);
      }
      builder.extensions(se.swedenconnect.opensaml.saml2.core.build.ExtensionsBuilder.builder()
          .extension(extension)
          .build());
    }
    return builder.build();
  }

  private static EntityDescriptor spMetadata(final String entityCategory, final String... requestedAttributes) {
    final EntityDescriptorBuilder builder = EntityDescriptorBuilder.builder().entityID("https://sp.example.com");
    if (entityCategory != null) {
      builder.extensions(ExtensionsBuilder.builder()
          .extension(EntityAttributesBuilder.builder().entityCategoriesAttribute(entityCategory).build())
          .build());
    }
    final SPSSODescriptorBuilder descriptor = SPSSODescriptorBuilder.builder();
    if (requestedAttributes.length > 0) {
      descriptor.attributeConsumingServices(AttributeConsumingServiceBuilder.builder()
          .index(0)
          .isDefault(true)
          .requestedAttributes(Arrays.stream(requestedAttributes)
              .map(n -> RequestedAttributeBuilder.builder(n).isRequired(true).build())
              .toList())
          .build());
    }
    builder.ssoDescriptor(descriptor.build());
    return builder.build();
  }

}
