/*
 * The eidas-connector project is the implementation of the Swedish eIDAS 
 * connector built on top of the Shibboleth IdP.
 *
 * More details on <https://github.com/elegnamnden/eidas-connector> 
 * Copyright (C) 2017 E-legitimationsnämnden
 * 
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.elegnamnden.eidas.idp.connector.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
  public String getPrincipal(List<Attribute> attributes) throws AttributeProcessingException {
    Attribute principalAttribute = getAttribute.apply(AttributeConstants.ATTRIBUTE_NAME_PRID, attributes);
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
  public List<Attribute> performAttributeRelease(ResponseProcessingResult responseResult) throws AttributeProcessingException {

    // Step 1. Transform attributes from eIDAS format to an attribute format according to the Swedish eID Framework.
    //
    List<Attribute> attributesForRelease = new ArrayList<>();
    for (Attribute eidasAttribute : responseResult.getAttributes()) {
      Optional<Attribute> swedishAttribute = this.attributeMappings.toSwedishEidAttribute(eidasAttribute);
      if (swedishAttribute.isPresent()) {
        attributesForRelease.add(swedishAttribute.get());
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

    // Step 3. Get extra attributes from the AA service.
    //
    try {
      List<Attribute> additionalAttributes = this.attributeAuthority.resolveAttributes(attributesForRelease, responseResult.getCountry());

      // OK, we are a bit defensive, but better safe than sorry. Let's make sure that the AA did not give us any attributes
      // that are already part of the attributes we got from the IdP.
      //
      for (Attribute a : additionalAttributes) {
        if (getAttribute.apply(a.getName(), attributesForRelease) != null) {
          final String msg = String.format("Attribute '%s' was received from AA service, but this attribute was already released by the foreign IdP", a.getName());
          throw new AttributeProcessingException(msg);
        }
      }
      
      attributesForRelease.addAll(additionalAttributes);
    }
    catch (AttributeAuthorityException e) {
      throw new AttributeProcessingException("Failed to get attributes from Attribute Authority - " + e.getMessage(), e);
    }
    
    return attributesForRelease;
  }

  /** {@inheritDoc} */
  @Override
  public List<se.litsec.eidas.opensaml.ext.RequestedAttribute> getEidasRequestedAttributesFromAttributeSet(AttributeSet attributeSet) {
    if (attributeSet == null || attributeSet.getRequiredAttributes() == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(attributeSet.getRequiredAttributes())
      .stream()
      .map(t -> this.attributeMappings.toEidasRequestedAttribute(t))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public List<se.litsec.eidas.opensaml.ext.RequestedAttribute> getEidasRequestedAttributesFromMetadata(EntityDescriptor peerMetadata,
      AuthnRequest authnRequest,
      List<se.litsec.eidas.opensaml.ext.RequestedAttribute> alreadyRequested) {

    if (peerMetadata == null) {
      return Collections.emptyList();
    }
    SPSSODescriptor descriptor = peerMetadata.getSPSSODescriptor(SAMLConstants.SAML20P_NS);
    if (descriptor == null || descriptor.getAttributeConsumingServices().isEmpty()) {
      return Collections.emptyList();
    }
    
    // Find the correct AttributeConsumingService
    //
    Integer attributeConsumingServiceIndex = authnRequest.getAttributeConsumingServiceIndex();
    AttributeConsumingService service = null;
    for (AttributeConsumingService s : descriptor.getAttributeConsumingServices()) {
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
    
    Predicate<se.litsec.eidas.opensaml.ext.RequestedAttribute> noDuplicate = r -> alreadyRequested.stream().noneMatch(a -> a.getName()
      .equals(r.getName()));

    return service
      .getRequestAttributes()
      .stream()
      .map(r -> this.attributeMappings.toEidasRequestedAttribute(r))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .filter(noDuplicate)
      .collect(Collectors.toList());
  }

  /**
   * Assigns mappings between eIDAS and Swedish eID attributes.
   * 
   * @param attributeMappings
   *          attribute mapper bean
   */
  public void setAttributeMappings(AttributeMappings attributeMappings) {
    this.attributeMappings = attributeMappings;
  }

  /**
   * Assigns the AA service bean
   * 
   * @param attributeAuthority
   *          the AA service
   */
  public void setAttributeAuthority(AttributeAuthority attributeAuthority) {
    this.attributeAuthority = attributeAuthority;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(this.attributeMappings, "Property 'attributeMappings' must be assigned");
    Assert.notNull(this.attributeAuthority, "Property 'attributeAuthority' must be assigned");
  }

}
