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
package se.elegnamnden.eidas.idp.connector.aaclient;

import java.util.List;

import org.opensaml.saml.saml2.core.Attribute;

/**
 * Defines the interface for an Attribute Authority that is used by the eIDAS connector.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public interface AttributeAuthority {

  /**
   * Resolves the supplied id to a list (possibly empty) of attributes.
   * <p>
   * Deprecated - use {@link #resolveAttributes(List, String)} instead.
   * </p>
   * 
   * @param id
   *          the identifier to supply as input to the attribute query
   * @param country
   *          the country from which the attributes were originally obtained (before conversion)
   * @return a list of resolved attributes
   * @throws AttributeAuthorityException
   *           for errors during communication with the attribute authority
   */
  @Deprecated
  List<Attribute> resolveAttributes(String id, String country) throws AttributeAuthorityException;

  /**
   * Based on a set of attributes the implementation resolves additional attributes.
   * 
   * @param attributes
   *          the attributes received from authentication (mapped to Swedish eID format)
   * @param country
   *          the the country from which the attributes were originally obtained (before conversion)
   * @return a list of resolved attributes
   * @throws AttributeAuthorityException
   *           for resolving errors
   */
  List<Attribute> resolveAttributes(List<Attribute> attributes, String country) throws AttributeAuthorityException;

}
