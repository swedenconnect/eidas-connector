/*
 * Copyright 2017-2025 Sweden Connect
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

import lombok.extern.slf4j.Slf4j;
import org.opensaml.saml.common.assertion.AssertionValidationException;
import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.common.assertion.ValidationResult;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Statement;
import se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants;
import se.swedenconnect.opensaml.sweid.saml2.validation.SwedishEidAttributeStatementValidator;

import java.util.Collection;
import java.util.List;

/**
 * Attribute statement validator for the eIDAS Framework.
 *
 * @author Martin Lindström
 */
@Slf4j
public class EidasAttributeStatementValidator extends SwedishEidAttributeStatementValidator {

  /** Attribute names for the eIDAS minimum dataset. */
  private static final List<String> EIDAS_MINIMUM_DATASET = List.of(
      se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME,
      se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME,
      se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME,
      se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME);

  /**
   * Validates using the default functionality, but adds checks to ensure that we don't receive any representative
   * attributes.
   */
  @Override
  public ValidationResult validate(final Statement statement, final Assertion assertion,
      final ValidationContext context) throws AssertionValidationException {

    final ValidationResult result = super.validate(statement, assertion, context);
    if (result == ValidationResult.INVALID) {
      return result;
    }
    final AttributeStatement attributeStatement = (AttributeStatement) statement;

    if (attributeStatement.getAttributes().stream().anyMatch(
        EidasAttributeStatementValidator::isRepresentativeAttribute)) {
      final String msg = "Assertion contains eIDAS representative attributes - not supported";
      log.info("{} [assertion-id:{}]", msg, assertion.getID());
      context.getValidationFailureMessages().add(msg);
      return ValidationResult.INVALID;
    }

    return result;
  }

  /** {@inheritDoc} */
  @Override
  protected Collection<String> getRequiredAttributes(ValidationContext context) {
    return EIDAS_MINIMUM_DATASET;
  }

  /**
   * Predicate that tells if the supplied attribute is a representative attribute.
   *
   * @param attribute the attribute to check
   * @return {@code true} if the attribute is a representative attribute and {@code false} otherwise
   */
  private static boolean isRepresentativeAttribute(final Attribute attribute) {
    if (attribute.getName() == null) {
      return false;
    }
    return attribute.getName().startsWith(AttributeConstants.REPRESENTATIVE_NATURAL_PERSON_PREFIX)
        || attribute.getName().startsWith(AttributeConstants.REPRESENTATIVE_LEGAL_PERSON_PREFIX);
  }

}
