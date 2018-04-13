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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.saml.common.assertion.AssertionValidationException;
import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.common.assertion.ValidationResult;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.core.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.litsec.eidas.opensaml.ext.RequestedAttribute;
import se.litsec.eidas.opensaml.ext.RequestedAttributes;
import se.litsec.eidas.opensaml.ext.attributes.AttributeConstants;
import se.litsec.opensaml.common.validation.CoreValidatorParameters;
import se.litsec.swedisheid.opensaml.saml2.validation.SwedishEidAttributeStatementValidator;

/**
 * {@link AttributeStatementValidator} for the eIDAS Framework. This class will check the {@code AuthnRequest} found in
 * the context parameter {@link CoreValidatorParameters#AUTHN_REQUEST} and extract the {@link RequestedAttribute} object
 * from its extension before checking whether all required attributes were delivered.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class EidasAttributeStatementValidator extends SwedishEidAttributeStatementValidator {
  
  /** Class logger. */
  private final Logger log = LoggerFactory.getLogger(EidasAttributeStatementValidator.class);

  /**
   * Validates using the default functionality, but adds checks to ensure that we don't receive any representative
   * attributes.
   */
  @Override
  public ValidationResult validate(Statement statement, Assertion assertion, ValidationContext context)
      throws AssertionValidationException {

    ValidationResult result = super.validate(statement, assertion, context);
    if (result == ValidationResult.INVALID) {
      return result;
    }
    AttributeStatement attributeStatement = (AttributeStatement) statement;
    
    if (attributeStatement.getAttributes().stream().filter(a -> isRepresentativeAttribute(a)).findAny().isPresent()) {
      final String msg = "Assertion contains eIDAS representative attributes - not supported";      
      log.info("{} [assertion-id:{}]", msg, assertion.getID());
      context.setValidationFailureMessage(msg);
      return ValidationResult.INVALID;
    }
    
    return result;
  }

  /**
   * Predicate that tells if the supplied attribute is a representative attribute.
   * 
   * @param attribute
   *          the attribute to check
   * @return {@code true} if the attribute is a representative attribute and {@code false} otherwise
   */
  private static boolean isRepresentativeAttribute(Attribute attribute) {
    if (attribute.getName() == null) {
      return false;
    }
    return (attribute.getName().startsWith(AttributeConstants.REPRESENTATIVE_NATURAL_PERSON_PREFIX)
        || attribute.getName().startsWith(AttributeConstants.REPRESENTATIVE_LEGAL_PERSON_PREFIX));
  }

  /**
   * Extracts the {@link RequestedAttribute} objects from the {@code AuthnRequest}.
   */
  @Override
  protected Collection<String> getRequiredAttributes(ValidationContext context) {

    List<String> attributes = new ArrayList<>();
    attributes.addAll(super.getRequiredAttributes(context));

    AuthnRequest authnRequest = (AuthnRequest) context.getStaticParameters().get(CoreValidatorParameters.AUTHN_REQUEST);
    if (authnRequest != null) {
      Extensions extensions = authnRequest.getExtensions();
      if (extensions != null) {
        RequestedAttributes requestedAttributes = extensions.getUnknownXMLObjects()
          .stream()
          .filter(RequestedAttributes.class::isInstance)
          .map(RequestedAttributes.class::cast)
          .findFirst()
          .orElse(null);
        if (requestedAttributes != null) {
          for (RequestedAttribute ra : requestedAttributes.getRequestedAttributes()) {
            if (ra.isRequired() && !attributes.contains(ra.getName())) {
              attributes.add(ra.getName());
            }
          }
        }
      }
    }

    return attributes;
  }

}
