/*
 * Copyright 2017-2024 Sweden Connect
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
package se.swedenconnect.eidas.connector.authn.sp.validators;

import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.common.assertion.ValidationResult;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.xmlsec.signature.support.SignaturePrevalidator;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.opensaml.sweid.saml2.validation.SwedishEidResponseValidator;

/**
 * Custom response validation for eIDAS.
 *
 * @author Martin Lindström
 */
@Slf4j
public class EidasResponseValidator extends SwedishEidResponseValidator {

  /**
   * Constructor.
   *
   * @param trustEngine the trust used to validate the object's signature
   * @param signaturePrevalidator the signature pre-validator used to pre-validate the object's signature
   * @throws IllegalArgumentException if {@code null} values are supplied
   */
  public EidasResponseValidator(final SignatureTrustEngine trustEngine,
      final SignaturePrevalidator signaturePrevalidator) throws IllegalArgumentException {
    super(trustEngine, signaturePrevalidator);
  }

  /**
   * The EU software sometimes includes assertions in error responses. We'll have to accept that.
   */
  @Override
  public ValidationResult validateAssertions(final Response response, final ValidationContext context) {
    if (StatusCode.SUCCESS.equals(response.getStatus().getStatusCode().getValue())) {
      if (response.getAssertions().isEmpty() && response.getEncryptedAssertions().isEmpty()) {
        context.getValidationFailureMessages().add(
            "Response message has success status but does not contain any assertions - invalid");
        return ValidationResult.INVALID;
      }
      if (response.getEncryptedAssertions().isEmpty()) {
        context.getValidationFailureMessages().add("Response does not contain EncryptedAssertion");
        return ValidationResult.INVALID;
      }
      if (response.getEncryptedAssertions().size() > 1) {
        final String msg = "Response contains more than one EncryptedAssertion";
        if (isStrictValidation(context)) {
          context.getValidationFailureMessages().add(msg);
          return ValidationResult.INVALID;
        }
        log.warn(msg);
      }
      if (!response.getAssertions().isEmpty()) {
        final String msg = "Response contains non encrypted Assertion(s)";
        if (isStrictValidation(context)) {
          context.getValidationFailureMessages().add(msg);
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