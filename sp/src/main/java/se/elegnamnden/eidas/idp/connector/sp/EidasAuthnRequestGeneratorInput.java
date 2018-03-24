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
package se.elegnamnden.eidas.idp.connector.sp;

import java.util.List;

import se.litsec.eidas.opensaml.ext.RequestedAttributes;
import se.litsec.eidas.opensaml.ext.SPTypeEnumeration;
import se.litsec.opensaml.saml2.common.request.AbstractRequestGeneratorInput;
import se.litsec.opensaml.utils.ObjectUtils;

/**
 * Input for the proxy server SP when generating an AuthnRequest.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class EidasAuthnRequestGeneratorInput extends AbstractRequestGeneratorInput {

  /** The country for the IdP. */
  private String country;

  /** The requested level of assurance URI. Only one is specified since eIDAS uses "minimum" matching. */
  private String requestedLevelOfAssurance;

  /** The eIDAS requested attributes to include in the AuthnRequest. */
  private List<se.litsec.eidas.opensaml.ext.RequestedAttribute> requestedAttributeList;

  /** The type of SP that makes the request. */
  private SPTypeEnumeration spType;

  /**
   * Returns the country in which the IdP resides.
   * 
   * @return the country code
   */
  public String getCountry() {
    return this.country;
  }

  /**
   * Assigns the country in which the IdP resides.
   * 
   * @param country
   *          the country code
   */
  public void setCountry(String country) {
    this.country = country;
  }

  /**
   * Returns the requested level of assurance URI. Only one is specified since eIDAS uses "minimum" matching.
   * 
   * @return requested LoA URI
   */
  public String getRequestedLevelOfAssurance() {
    return this.requestedLevelOfAssurance;
  }

  /**
   * Assigns the requested level of assurance URI.
   * 
   * @param requestedLevelOfAssurance
   *          requested LoA URIs
   */
  public void setRequestedLevelOfAssurance(String requestedLevelOfAssurance) {
    this.requestedLevelOfAssurance = requestedLevelOfAssurance;
  }

  /**
   * Returns the list of eIDAS request attributes to include in the request.
   * 
   * @return list of requested attributes
   */
  public List<se.litsec.eidas.opensaml.ext.RequestedAttribute> getRequestedAttributeList() {
    return this.requestedAttributeList;
  }

  /**
   * Assigns the list of eIDAS request attributes to include in the request.
   * 
   * @param requestedAttributeList
   *          list of requested attributes
   */
  public void setRequestedAttributeList(List<se.litsec.eidas.opensaml.ext.RequestedAttribute> requestedAttributeList) {
    this.requestedAttributeList = requestedAttributeList;
  }

  /**
   * Returns a {@link RequestedAttributes} object which encapsulated the attributes returned from
   * {@link #getRequestedAttributeList()}.
   * 
   * @return a {@link RequestedAttributes} object
   */
  public RequestedAttributes getRequestedAttributes() {
    RequestedAttributes requestedAttributes = ObjectUtils.createSamlObject(RequestedAttributes.class);
    if (this.requestedAttributeList != null) {
      requestedAttributes.getRequestedAttributes().addAll(this.requestedAttributeList);
    }
    return requestedAttributes;
  }

  /**
   * Will always return {@code null}. We rely on the default of the generator.
   */
  @Override
  public String getPreferredBinding() {
    return null;
  }

  /**
   * Returns the SP type, or {@code null} if no information is set.
   * 
   * @return the SP type, or {@code null}
   */
  public SPTypeEnumeration getSpType() {
    return this.spType;
  }

  /**
   * Assigns the SP type.
   * 
   * @param spType
   *          the SP type
   */
  public void setSpType(SPTypeEnumeration spType) {
    this.spType = spType;
  }

}
