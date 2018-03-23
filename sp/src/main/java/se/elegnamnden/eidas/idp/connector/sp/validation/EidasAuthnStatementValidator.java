/*
 * Copyright 2017-2018 E-legitimationsnämnden
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
package se.elegnamnden.eidas.idp.connector.sp.validation;

import java.util.Collection;

import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.common.assertion.ValidationResult;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnStatement;

import se.litsec.opensaml.saml2.common.assertion.AuthnStatementValidator;
import se.litsec.swedisheid.opensaml.saml2.validation.SwedishEidAuthnStatementValidator;

/**
 * An {@link AuthnStatementValidator} for the eIDAS Framework.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class EidasAuthnStatementValidator extends SwedishEidAuthnStatementValidator {

  /**
   * Implements the eIDAS matching rules for LoA matching.
   */
  @Override
  protected ValidationResult validateAuthnContextClassRef(String authnContextClassRef, Collection<String> requestedContextClassRefs,
      AuthnStatement statement, Assertion assertion, ValidationContext context) {

    // TODO
    return ValidationResult.VALID;
  }

}
