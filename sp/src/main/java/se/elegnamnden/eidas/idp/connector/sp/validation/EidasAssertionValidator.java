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
package se.elegnamnden.eidas.idp.connector.sp.validation;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.saml.common.assertion.AssertionValidationException;
import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.common.assertion.ValidationResult;
import org.opensaml.saml.saml2.assertion.ConditionValidator;
import org.opensaml.saml.saml2.assertion.impl.AudienceRestrictionConditionValidator;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Condition;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.OneTimeUse;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.xmlsec.signature.support.SignaturePrevalidator;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.litsec.opensaml.saml2.common.assertion.AssertionValidator;
import se.litsec.swedisheid.opensaml.saml2.validation.SwedishEidAssertionValidator;
import se.litsec.swedisheid.opensaml.saml2.validation.SwedishEidSubjectConfirmationValidator;

/**
 * An {@link AssertionValidator} for the eIDAS Framework.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class EidasAssertionValidator extends SwedishEidAssertionValidator {

  /** Class logger. */
  private final Logger log = LoggerFactory.getLogger(EidasAssertionValidator.class);

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
      Arrays.asList(new AudienceRestrictionConditionValidator(), new OneTimeUseDummyConditionValidator()),
      Arrays.asList(new EidasAuthnStatementValidator(), new EidasAttributeStatementValidator()));
  }

  /**
   * The EU-software sometimes issue assertions that are a few milliseconds newer than the response message if we look
   * at their respective issue instants. We can tolerate a few milliseconds.
   */
  @Override
  protected ValidationResult validateIssueInstant(Assertion assertion, ValidationContext context) {

    Long responseIssueInstant = (Long) context.getStaticParameters().get(RESPONSE_ISSUE_INSTANT);
    if (responseIssueInstant != null) {
      if (assertion.getIssueInstant().isAfter(responseIssueInstant)) {
        final String msg = String.format("Invalid Assertion - Its issue-instant (%s) is after the response message issue-instant (%s)",
          assertion.getIssueInstant(), new DateTime(responseIssueInstant, ISOChronology.getInstanceUTC()));

        log.warn("{} [assertion-id:{}]", msg, assertion.getID());

        // We can't accept more than 500 millis
        if (assertion.getIssueInstant().isAfter(responseIssueInstant + 500L)) {
          context.setValidationFailureMessage(msg);
          return ValidationResult.INVALID;
        }
      }
    }
    else {
      return super.validateIssueInstant(assertion, context);
    }

    return ValidationResult.VALID;
  }

  /** {@inheritDoc} */
  @Override
  protected ValidationResult validateSubject(final Assertion assertion, final ValidationContext context) {

    if (assertion.getSubject() == null) {
      context.setValidationFailureMessage("Missing Subject element in Assertion");
      return ValidationResult.INVALID;
    }

    // Assert that there is a NameID ...
    //
    if (assertion.getSubject().getNameID() == null) {
      context.setValidationFailureMessage("Missing NameID in Subject element of Assertion");
      return ValidationResult.INVALID;
    }
    // And that it holds a value ...
    //
    if (assertion.getSubject().getNameID().getValue() == null) {
      context.setValidationFailureMessage("Missing NameID value in Subject element of Assertion");
      return ValidationResult.INVALID;
    }
    // Also check that it is persistent or transient ...
    //
    if (assertion.getSubject().getNameID().getFormat() == null) {
      final String msg = "NameID element of Assertion/@Subject is missing Format attribute";
      if (isStrictValidation(context)) {
        context.setValidationFailureMessage(msg);
        return ValidationResult.INVALID;
      }
      else {
        log.warn(msg);
      }
    }
    else {
      final String format = assertion.getSubject().getNameID().getFormat();
      if (!(format.equals(NameID.PERSISTENT) || format.equals(NameID.TRANSIENT) || format.equals(NameID.UNSPECIFIED))) {
        final String msg = String.format("NameID format in Subject of Assertion is not valid (%s) - '%s', '%s' or '%s' is required",
          format, NameID.PERSISTENT, NameID.TRANSIENT, NameID.UNSPECIFIED);
        if (isStrictValidation(context)) {
          context.setValidationFailureMessage(msg);
          return ValidationResult.INVALID;
        }
        else {
          log.warn(msg);
        }
      }
    }

    List<SubjectConfirmation> confirmations = assertion.getSubject().getSubjectConfirmations();
    if (confirmations != null && !confirmations.isEmpty()) {
      
      // We require the bearer method ...
      //
      boolean bearerFound = false;
      for (SubjectConfirmation sc : confirmations) {
        if (SubjectConfirmation.METHOD_BEARER.equals(sc.getMethod())) {
          bearerFound = true;
          break;
        }
      }
      if (!bearerFound) {
        final String msg = String.format("No SubjectConfirmation with method '%s' is available under Assertion's Subject element",
          SubjectConfirmation.METHOD_BEARER);
        if (isStrictValidation(context)) {
          context.setValidationFailureMessage(msg);
          return ValidationResult.INVALID;
        }
        else {
          log.warn(msg);
        }
      }
    }
    else {
      final String msg = "Assertion/@Subject element contains no SubjectConfirmation elements";
      if (isStrictValidation(context)) {
        context.setValidationFailureMessage(msg);
        return ValidationResult.INVALID;
      }
      log.info("Assertion/@Subject element contains no SubjectConfirmation elements");
    }

    return super.validateSubject(assertion, context);
  }

  /**
   * Just here to turn off warn logging from the default implementation.
   */
  private static class OneTimeUseDummyConditionValidator implements ConditionValidator {

    @Override
    public QName getServicedCondition() {
      return OneTimeUse.DEFAULT_ELEMENT_NAME;
    }

    @Override
    public ValidationResult validate(Condition condition, Assertion assertion, ValidationContext context)
        throws AssertionValidationException {
      return ValidationResult.VALID;
    }

  }

}
