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
  String getPrincipal(final List<Attribute> attributes) throws AttributeProcessingException;

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
  List<Attribute> performAttributeRelease(final ResponseProcessingResult responseResult) throws AttributeProcessingException;

  /**
   * Given an attribute set implemented by the IdP, a list of eIDAS {@code RequestedAttribute} objects are returned.
   * 
   * @param attributeSet
   *          the implemented attribute set
   * @return a list of eIDAS requested attributes
   */
  List<se.litsec.eidas.opensaml.ext.RequestedAttribute> getEidasRequestedAttributesFromAttributeSet(final AttributeSet attributeSet);

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
  List<se.litsec.eidas.opensaml.ext.RequestedAttribute> getEidasRequestedAttributesFromMetadata(final EntityDescriptor peerMetadata,
      final AuthnRequest authnRequest, final List<se.litsec.eidas.opensaml.ext.RequestedAttribute> alreadyRequested);
}
