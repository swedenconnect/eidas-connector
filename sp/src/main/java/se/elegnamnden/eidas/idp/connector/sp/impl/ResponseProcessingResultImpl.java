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
package se.elegnamnden.eidas.idp.connector.sp.impl;

import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.NameID;

import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessingResult;

/**
 * Implementation of the {@code ResponseProcessingResult} interface.
 *
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class ResponseProcessingResultImpl implements ResponseProcessingResult {

  /** The assertion. */
  private Assertion assertion;

  /** The country. */
  private String country;

  /**
   * Constructor.
   * 
   * @param assertion
   *          the {@code Assertion}
   * @param country
   *          the country code for the country in which the IdP that issued the assertion resides
   */
  public ResponseProcessingResultImpl(Assertion assertion, String country) {
    this.assertion = assertion;
    this.country = country;
  }

  /** {@inheritDoc} */
  @Override
  public Assertion getAssertion() {
    return this.assertion;
  }

  /** {@inheritDoc} */
  @Override
  public List<Attribute> getAttributes() {
    try {
      return Collections.unmodifiableList(this.assertion.getAttributeStatements().get(0).getAttributes());
    }
    catch (NullPointerException e) {
      return Collections.emptyList();
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getAuthnContextClassUri() {
    try {
      return this.assertion.getAuthnStatements().get(0).getAuthnContext().getAuthnContextClassRef().getAuthnContextClassRef();
    }
    catch (NullPointerException e) {
      return null;
    }
  }

  /** {@inheritDoc} */
  @Override
  public DateTime getAuthnInstant() {
    
    DateTime authnInstant = this.assertion.getAuthnStatements().get(0).getAuthnInstant();
    
    // We have already checked the validity of the authentication instant, but if it is
    // after the current time it means that it is within the allowed clock skew. If so,
    // we set it to the current time (it's the best we can do).
    
    if (authnInstant.isAfterNow()) {
      return new DateTime();
    }
    
    return authnInstant;
  }

  /** {@inheritDoc} */
  @Override
  public String getIssuer() {
    try {
      return this.assertion.getIssuer().getValue();
    }
    catch (NullPointerException e) {
      return null;
    }
  }

  /** {@inheritDoc} */
  @Override
  public NameID getSubjectNameID() {
    try {
      return this.assertion.getSubject().getNameID();
    }
    catch (NullPointerException e) {
      return null;
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getCountry() {
    return this.country;
  }

}
