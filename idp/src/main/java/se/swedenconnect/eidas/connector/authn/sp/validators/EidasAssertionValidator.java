/*
 * Copyright 2017-2026 Sweden Connect
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

import org.jspecify.annotations.NonNull;
import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.common.assertion.ValidationResult;
import org.opensaml.saml.saml2.assertion.ConditionValidator;
import org.opensaml.saml.saml2.assertion.impl.AudienceRestrictionConditionValidator;
import org.opensaml.saml.saml2.assertion.impl.BearerSubjectConfirmationValidator;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Condition;
import org.opensaml.saml.saml2.core.OneTimeUse;
import org.opensaml.xmlsec.signature.support.SignaturePrevalidator;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedenconnect.opensaml.saml2.assertion.validation.AssertionValidator;
import se.swedenconnect.opensaml.sweid.saml2.validation.SwedishEidAssertionValidator;

import javax.xml.namespace.QName;
import java.time.Instant;
import java.util.List;

/**
 * An {@link AssertionValidator} for the eIDAS Framework.
 *
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class EidasAssertionValidator extends SwedishEidAssertionValidator {

  /** Logger instance. */
  private static final Logger log = LoggerFactory.getLogger(EidasAssertionValidator.class);

  /**
   * Constructor setting up the validator with the following validators:
   * <ul>
   * <li>confirmationValidators: {@link BearerSubjectConfirmationValidator}</li>
   * <li>conditionValidators: {@link AudienceRestrictionConditionValidator}</li>
   * <li>statementValidators: {@link EidasAuthnStatementValidator}, {@link EidasAttributeStatementValidator}.</li>
   * </ul>
   *
   * @param trustEngine the trust used to validate the object's signature
   * @param signaturePrevalidator the signature pre-validator used to pre-validate the object's signature
   */
  public EidasAssertionValidator(final @NonNull SignatureTrustEngine trustEngine,
      final @NonNull SignaturePrevalidator signaturePrevalidator) {
    super(trustEngine, signaturePrevalidator,
        List.of(new BearerSubjectConfirmationValidator()),
        List.of(new AudienceRestrictionConditionValidator(), new OneTimeUseDummyConditionValidator()),
        List.of(new EidasAuthnStatementValidator(), new EidasAttributeStatementValidator()));
  }

  /**
   * The EU-software sometimes issue assertions that are a few milliseconds newer than the response message if we look
   * at their respective issue instants. We can tolerate a few milliseconds.
   */
  @Override
  protected @NonNull ValidationResult validateIssueInstant(final @NonNull Assertion assertion,
      final @NonNull ValidationContext context) {

    final Instant responseIssueInstant = this.getResponseIssueInstant(context);
    if (responseIssueInstant != null) {

      if (assertion.getIssueInstant() != null && assertion.getIssueInstant().isAfter(responseIssueInstant)) {
        final String msg =
            String.format("Invalid Assertion - Its issue-instant (%s) is after the response message issue-instant (%s)",
                assertion.getIssueInstant(), responseIssueInstant);
        log.warn("{} [assertion-id:{}]", msg, assertion.getID());

        // We can't accept more than 500 millis
        if (assertion.getIssueInstant().isAfter(responseIssueInstant.plusMillis(500L))) {
          context.getValidationFailureMessages().add(msg);
          return ValidationResult.INVALID;
        }
      }
    }
    else {
      return super.validateIssueInstant(assertion, context);
    }

    return ValidationResult.VALID;
  }

  /**
   * Just here to turn off warn logging from the default implementation.
   */
  private static class OneTimeUseDummyConditionValidator implements ConditionValidator {

    @Override
    public @NonNull QName getServicedCondition() {
      return OneTimeUse.DEFAULT_ELEMENT_NAME;
    }

    @Override
    public @NonNull ValidationResult validate(final @NonNull Condition condition,
        final @NonNull Assertion assertion, final @NonNull ValidationContext context) {
      return ValidationResult.VALID;
    }

  }

}
