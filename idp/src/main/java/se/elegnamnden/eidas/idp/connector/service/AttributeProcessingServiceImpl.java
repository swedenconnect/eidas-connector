/*
 * Copyright 2017-2022 Sweden Connect
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
package se.elegnamnden.eidas.idp.connector.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import se.elegnamnden.eidas.idp.connector.aaclient.AttributeAuthority;
import se.elegnamnden.eidas.idp.connector.aaclient.AttributeAuthorityException;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessingResult;
import se.elegnamnden.eidas.mapping.attributes.AttributeMappings;
import se.litsec.opensaml.saml2.attribute.AttributeUtils;
import se.litsec.swedisheid.opensaml.saml2.attribute.AttributeConstants;
import se.litsec.swedisheid.opensaml.saml2.attribute.AttributeSet;

/**
 * Service that handles attribute processing and mappings.
 *
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class AttributeProcessingServiceImpl implements AttributeProcessingService, InitializingBean {

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(AttributeProcessingServiceImpl.class);

  /** Mappings between eIDAS and Swedish eID attributes. */
  protected AttributeMappings attributeMappings;

  /** The Attribute Authority service. */
  protected AttributeAuthority attributeAuthority;

  /** Function that finds an attribute with a given name from a list of attributes. */
  protected static BiFunction<String, List<Attribute>, Attribute> getAttribute = (name, list) -> {
    return list.stream().filter(a -> name.equals(a.getName())).findFirst().orElse(null);
  };

  /** {@inheritDoc} */
  @Override
  public String getPrincipal(final List<Attribute> attributes) throws AttributeProcessingException {
    final Attribute principalAttribute = getAttribute.apply(AttributeConstants.ATTRIBUTE_NAME_PRID, attributes);
    if (principalAttribute == null) {
      throw new AttributeProcessingException(String.format("Could not get principal ID since attribute '%s' (%s) was not present",
        AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_PRID, AttributeConstants.ATTRIBUTE_NAME_PRID));
    }
    return AttributeUtils.getAttributeStringValue(principalAttribute);
  }

  /** {@inheritDoc} */
  @Override
  public String getPrincipalAttributeName() {
    return AttributeConstants.ATTRIBUTE_NAME_PRID;
  }

  /**
   * Performs the following steps:
   * <ol>
   * <li>Transforms attributes from eIDAS format to an attribute format according to the Swedish eID Framework.</li>
   * <li>Adds the 'transactionIdentifier' attribute holding the value of the assertion ID.</li>
   * <li>Contacts the Attribute Authority to obtain the 'prid', 'pridPersistence' and possibly also a mapped
   * 'personalIdentityNumber' attribute.</li>
   * </ol>
   */
  @Override
  public List<Attribute> performAttributeRelease(final ResponseProcessingResult responseResult) throws AttributeProcessingException {

    // Step 1. Transform attributes from eIDAS format to an attribute format according to the Swedish eID Framework.
    //
    final List<Attribute> attributesForRelease = new ArrayList<>();
    for (final Attribute eidasAttribute : responseResult.getAttributes()) {
      final Attribute swedishAttribute = this.attributeMappings.toSwedishEidAttribute(eidasAttribute);
      if (swedishAttribute != null) {
        attributesForRelease.add(swedishAttribute);
      }
      else {
        log.warn("No mapping exists for eIDAS attribute '{}'", eidasAttribute.getName());
      }
    }

    // Step 2. Add the 'transactionIdentifier' attribute holding the value of the assertion ID.
    //
    attributesForRelease.add(AttributeConstants.ATTRIBUTE_TEMPLATE_TRANSACTION_IDENTIFIER.createBuilder()
      .value(responseResult.getAssertion().getID())
      .build());

    // Step 3. Add the 'c' attribute holding the country code for the country.
    //
    attributesForRelease.add(AttributeConstants.ATTRIBUTE_TEMPLATE_C.createBuilder()
      .value(responseResult.getCountry())
      .build());

    // Step 4. Get extra attributes from the AA service.
    //
    try {
      final List<Attribute> additionalAttributes =
          this.attributeAuthority.resolveAttributes(attributesForRelease, responseResult.getCountry());

      // OK, we are a bit defensive, but better safe than sorry. Let's make sure that the AA did not give us any
      // attributes
      // that are already part of the attributes we got from the IdP.
      //
      for (final Attribute a : additionalAttributes) {
        if (getAttribute.apply(a.getName(), attributesForRelease) != null) {
          final String msg = String
            .format("Attribute '%s' was received from AA service, but this attribute was already released by the foreign IdP", a.getName());
          throw new AttributeProcessingException(msg);
        }
      }

      attributesForRelease.addAll(additionalAttributes);
    }
    catch (final AttributeAuthorityException e) {
      throw new AttributeProcessingException("Failed to get attributes from Attribute Authority - " + e.getMessage(), e);
    }

    return attributesForRelease;
  }

  /** {@inheritDoc} */
  @Override
  public List<se.litsec.eidas.opensaml.ext.RequestedAttribute>
      getEidasRequestedAttributesFromAttributeSet(final AttributeSet attributeSet) {
    if (attributeSet == null || attributeSet.getRequiredAttributes() == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(attributeSet.getRequiredAttributes()).stream()
      .map(t -> this.attributeMappings.toEidasRequestedAttribute(t))
      .filter(a -> a != null)
      .collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public List<se.litsec.eidas.opensaml.ext.RequestedAttribute> getEidasRequestedAttributesFromMetadata(final EntityDescriptor peerMetadata,
      final AuthnRequest authnRequest,
      final List<se.litsec.eidas.opensaml.ext.RequestedAttribute> alreadyRequested) {

    if (peerMetadata == null) {
      return Collections.emptyList();
    }
    final SPSSODescriptor descriptor = peerMetadata.getSPSSODescriptor(SAMLConstants.SAML20P_NS);
    if (descriptor == null || descriptor.getAttributeConsumingServices().isEmpty()) {
      return Collections.emptyList();
    }

    // Find the correct AttributeConsumingService
    //
    final Integer attributeConsumingServiceIndex = authnRequest.getAttributeConsumingServiceIndex();
    AttributeConsumingService service = null;
    for (final AttributeConsumingService s : descriptor.getAttributeConsumingServices()) {
      if (attributeConsumingServiceIndex != null && s.getIndex() == attributeConsumingServiceIndex.intValue()) {
        service = s;
        break;
      }
      if (s.isDefault()) {
        service = s;
        if (attributeConsumingServiceIndex == null) {
          break;
        }
      }
      else {
        // No default and no index given in request - pick the lowest index.
        if (service == null || s.getIndex() < service.getIndex()) {
          service = s;
        }
      }
    }

    if (service == null) {
      return Collections.emptyList();
    }

    final Predicate<se.litsec.eidas.opensaml.ext.RequestedAttribute> noDuplicate = r -> alreadyRequested.stream()
        .filter(a -> a != null)
        .noneMatch(a -> Objects.equals(a.getName(), r.getName()));

    return service.getRequestAttributes().stream()
      .map(r -> this.attributeMappings.toEidasRequestedAttribute(r))
      .filter(r -> r != null)
      .filter(noDuplicate)
      .collect(Collectors.toList());
  }

  /**
   * Assigns mappings between eIDAS and Swedish eID attributes.
   *
   * @param attributeMappings
   *          attribute mapper bean
   */
  public void setAttributeMappings(final AttributeMappings attributeMappings) {
    this.attributeMappings = attributeMappings;
  }

  /**
   * Assigns the AA service bean
   *
   * @param attributeAuthority
   *          the AA service
   */
  public void setAttributeAuthority(final AttributeAuthority attributeAuthority) {
    this.attributeAuthority = attributeAuthority;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(this.attributeMappings, "Property 'attributeMappings' must be assigned");
    Assert.notNull(this.attributeAuthority, "Property 'attributeAuthority' must be assigned");
  }

}
