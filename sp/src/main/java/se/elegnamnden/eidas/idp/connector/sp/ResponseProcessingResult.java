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

import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.NameID;

/**
 * Interface that describes the result of a response processing operation. It contains the actual {@code Assertion} that
 * really holds all information, but also "easy to access" methods of the elements that are of most interest.
 * <p>
 * Note that only successful responses are represented. Error responses are represented using the
 * {@link ResponseStatusErrorException}.
 * </p>
 *
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public interface ResponseProcessingResult {

  /**
   * Returns the {@code Assertion} from the response.
   * 
   * @return the {@code Assertion}
   */
  Assertion getAssertion();

  /**
   * Returns the attributes that are part of the attribute statement of the assertion.
   * 
   * @return an (unmodifiable) list of attributes
   */
  List<Attribute> getAttributes();

  /**
   * Returns the URI for the {@code AuthnContextClassRef} element that holds the "level of assurance" under which the
   * authentication was made.
   * 
   * @return LoA URI
   */
  String getAuthnContextClassUri();

  /**
   * Returns the authentication instant.
   * 
   * @return the instant at which the user authenticated
   */
  DateTime getAuthnInstant();

  /**
   * Returns the entityID of the issuing IdP.
   * 
   * @return entityID for the IdP
   */
  String getIssuer();

  /**
   * Returns the country identifier for the country in which the IdP resides.
   * 
   * @return the country identifier
   */
  String getCountry();

  /**
   * Returns the {@code NameID} for the subject.
   * 
   * @return the nameID
   */
  NameID getSubjectNameID();

}
