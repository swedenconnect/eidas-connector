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

import java.util.Arrays;

import org.opensaml.saml.saml2.assertion.impl.AudienceRestrictionConditionValidator;
import org.opensaml.xmlsec.signature.support.SignaturePrevalidator;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;

import se.litsec.opensaml.saml2.common.assertion.AssertionValidator;
import se.litsec.swedisheid.opensaml.saml2.validation.SwedishEidAssertionValidator;
import se.litsec.swedisheid.opensaml.saml2.validation.SwedishEidSubjectConfirmationValidator;

/**
 * An {@link AssertionValidator} for the eIDAS Framework.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class EidasAssertionValidator extends SwedishEidAssertionValidator {
  
  /**
   * Constructor setting up the validator with the following validators:
   * <ul>
   * <li>confirmationValidators: {@link SwedishEidSubjectConfirmationValidator}</li>
   * <li>conditionValidators: {@link AudienceRestrictionConditionValidator}</li>
   * <li>statementValidators: {@link EidasAuthnStatementValidator}, {@link EidasAttributeStatementValidator}.</li>
   * </ul>
   * 
   * @param trustEngine
   *          the trust used to validate the object's signature
   * @param signaturePrevalidator
   *          the signature pre-validator used to pre-validate the object's signature
   */  
  public EidasAssertionValidator(SignatureTrustEngine trustEngine, SignaturePrevalidator signaturePrevalidator) {
    super(trustEngine, signaturePrevalidator, 
      Arrays.asList(new SwedishEidSubjectConfirmationValidator()),
      Arrays.asList(new AudienceRestrictionConditionValidator()), 
      Arrays.asList(new EidasAuthnStatementValidator(), new EidasAttributeStatementValidator()));
  }

}
