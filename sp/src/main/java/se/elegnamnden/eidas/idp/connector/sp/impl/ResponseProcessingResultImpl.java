/*
 * Copyright 2017-2020 Sweden Connect
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

  /** The ID for the response. */
  private final String responseID;

  /** The assertion. */
  private final Assertion assertion;

  /** The country. */
  private final String country;

  /**
   * Constructor.
   * 
   * @param responseID
   *          the ID from the response
   * @param assertion
   *          the Assertion
   * @param country
   *          the country code for the country in which the IdP that issued the assertion resides
   */
  public ResponseProcessingResultImpl(final String responseID, final Assertion assertion, final String country) {
    this.responseID = responseID;
    this.assertion = assertion;
    this.country = country;
  }
  
  /** {@inheritDoc} */
  @Override
  public String getResponseID() {
    return this.responseID;
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
