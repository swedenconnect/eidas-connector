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

import java.util.List;

import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessingResult;
import se.litsec.swedisheid.opensaml.saml2.attribute.AttributeSet;

/**
 * Interface for a service that handles attribute processing.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public interface AttributeProcessingService {

  /**
   * Given a list of attributes, the method will locate the attribute that is the primary attribute holding the
   * principal's name.
   * 
   * @param attributes
   *          a list of attributes
   * @return the principal name
   * @throws AttributeProcessingException
   *           if no matching attribute is found
   */
  String getPrincipal(List<Attribute> attributes) throws AttributeProcessingException;

  /**
   * Returns the name of the attribute that is the attribute that represents the principal (subject).
   * 
   * @return attribute name
   */
  String getPrincipalAttributeName();

  /**
   * Given a response result, the implementation will perform an attribute release process including transforming eIDAS
   * attributes into national eID definitions of attributes, possibly add extra attributes to release and so on.
   * 
   * @param responseResult
   *          the result from the authentication at the foreign IdP
   * @return a list of attributes to be released to the relying party
   * @throws AttributeProcessingException
   *           if errors occur during the attribute release process
   */
  List<Attribute> performAttributeRelease(ResponseProcessingResult responseResult) throws AttributeProcessingException;

  /**
   * Given an attribute set implemented by the IdP, a list of eIDAS {@code RequestedAttribute} objects are returned.
   * 
   * @param attributeSet
   *          the implemented attribute set
   * @return a list of eIDAS requested attributes
   */
  List<se.litsec.eidas.opensaml.ext.RequestedAttribute> getEidasRequestedAttributesFromAttributeSet(AttributeSet attributeSet);

  /**
   * Given the peer metadata entry the method checks if the metadata specifies any requsted attributes under its
   * AttributeConsumingService element, and if so, transforms these into eIDAS {@code RequestedAttribute} objects
   * (excluding those already present).
   * 
   * @param peerMetadata
   *          the peer metadata entry
   * @param authnRequest
   *          the authentication request (potentially holding a {@code AssertionConsumerServiceIndex})
   * @param alreadyRequested
   *          already present attributes
   * @return a list of eIDAS requested attributes
   */
  List<se.litsec.eidas.opensaml.ext.RequestedAttribute> getEidasRequestedAttributesFromMetadata(EntityDescriptor peerMetadata,
      AuthnRequest authnRequest, List<se.litsec.eidas.opensaml.ext.RequestedAttribute> alreadyRequested);
}
