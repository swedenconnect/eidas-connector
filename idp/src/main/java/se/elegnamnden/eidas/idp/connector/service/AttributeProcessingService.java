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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import se.elegnamnden.eidas.mapping.attributes.AttributeMappings;
import se.litsec.swedisheid.opensaml.saml2.attribute.AttributeSet;

/**
 * Service that handles attribute processing and mappings.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class AttributeProcessingService implements InitializingBean {

  /** Mappings between eIDAS and Swedish eID attributes. */
  private AttributeMappings attributeMappings;

  /**
   * Given an attribute set implemented by the IdP, a list of eIDAS {@code RequestedAttribute} objects are returned.
   * 
   * @param attributeSet
   *          the implemented attribute set
   * @return a list of eIDAS requested attributes
   */
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

  /**
   * Given the peer metadata entry the method checks if the metadata specifies any requsted attributes under its
   * AttributeConsumingService element, and if so, transforms these into eIDAS {@code RequestedAttribute} objects
   * (excluding those already present).
   * 
   * @param peerMetadata
   *          the peer metadata entry
   * @param alreadyRequested
   *          already present attributes
   * @return a list of eIDAS requested attributes
   */
  public List<se.litsec.eidas.opensaml.ext.RequestedAttribute> getEidasRequestedAttributesFromMetadata(EntityDescriptor peerMetadata,
      List<se.litsec.eidas.opensaml.ext.RequestedAttribute> alreadyRequested) {

    if (peerMetadata == null) {
      return Collections.emptyList();
    }
    SPSSODescriptor descriptor = peerMetadata.getSPSSODescriptor(SAMLConstants.SAML20P_NS);
    if (descriptor == null || descriptor.getAttributeConsumingServices().isEmpty()) {
      return Collections.emptyList();
    }

    Predicate<se.litsec.eidas.opensaml.ext.RequestedAttribute> noDuplicate = r -> alreadyRequested.stream().noneMatch(a -> a.getName()
      .equals(r.getName()));

    return descriptor.getAttributeConsumingServices()
      .get(0)
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

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(this.attributeMappings, "Property 'attributeMappings' must be assigned");
  }

}
