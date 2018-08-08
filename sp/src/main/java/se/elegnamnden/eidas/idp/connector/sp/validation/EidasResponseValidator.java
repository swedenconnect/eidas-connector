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

import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.common.assertion.ValidationResult;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.xmlsec.signature.support.SignaturePrevalidator;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.litsec.swedisheid.opensaml.saml2.validation.SwedishEidResponseValidator;

/**
 * Validator that ensures that a {@code Response} element is valid according to the eIDAS Framework.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class EidasResponseValidator extends SwedishEidResponseValidator {
  
  /** Class logger. */
  private final Logger log = LoggerFactory.getLogger(EidasResponseValidator.class);

  /**
   * Constructor.
   * 
   * @param trustEngine
   *          the trust used to validate the object's signature
   * @param signaturePrevalidator
   *          the signature pre-validator used to pre-validate the object's signature
   * @throws IllegalArgumentException
   *           if {@code null} values are supplied
   */  
  public EidasResponseValidator(SignatureTrustEngine trustEngine, SignaturePrevalidator signaturePrevalidator)
      throws IllegalArgumentException {
    super(trustEngine, signaturePrevalidator);
  }

  /**
   * The EU software sometimes includes assertions in error responses. We'll have to accept that.
   */
  @Override
  public ValidationResult validateAssertions(Response response, ValidationContext context) {
    if (StatusCode.SUCCESS.equals(response.getStatus().getStatusCode().getValue())) {
      if (response.getAssertions().isEmpty() && response.getEncryptedAssertions().isEmpty()) {
        context.setValidationFailureMessage("Response message has success status but does not contain any assertions - invalid");
        return ValidationResult.INVALID;
      }
      if (response.getEncryptedAssertions().isEmpty()) {
        context.setValidationFailureMessage("Response does not contain EncryptedAssertion");
        return ValidationResult.INVALID;
      }
      if (response.getEncryptedAssertions().size() > 1) {
        String msg = "Response contains more than one EncryptedAssertion";
        if (isStrictValidation(context)) {
          context.setValidationFailureMessage(msg);
          return ValidationResult.INVALID;
        }
        log.warn(msg);
      }
      if (!response.getAssertions().isEmpty()) {
        String msg = "Response contains non encrypted Assertion(s)";
        if (isStrictValidation(context)) {
          context.setValidationFailureMessage(msg);
          return ValidationResult.INVALID;
        }
        log.warn(msg);
      }  
    }
    else {
      if (response.getAssertions().size() > 0 || response.getEncryptedAssertions().size() > 0) {
        log.warn("Response message has failure status but contains assertions [response-id:'{}']", response.getID());        
      }
    }
    return ValidationResult.VALID;
  }
  
}
