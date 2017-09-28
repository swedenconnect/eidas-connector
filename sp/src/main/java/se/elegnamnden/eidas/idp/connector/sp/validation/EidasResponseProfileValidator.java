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
package se.elegnamnden.eidas.idp.connector.sp.validation;

import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.common.assertion.ValidationResult;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.litsec.swedisheid.opensaml.saml2.validation.SwedishEidResponseProfileValidator;

/**
 * Validator that ensures that a {@code Response} element is valid according to the eIDAS Framework.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class EidasResponseProfileValidator extends SwedishEidResponseProfileValidator {

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(EidasResponseProfileValidator.class);

  /**
   * Temporary allow non-encrypted assertions.
   */
  @Override
  public ValidationResult validateAssertions(Response response, ValidationContext context) {

    if (StatusCode.SUCCESS.equals(response.getStatus().getStatusCode().getValue())) {
      if (response.getEncryptedAssertions().isEmpty()) {
        log.warn("Response does not contain EncryptedAssertion");
      }
      if (response.getEncryptedAssertions().isEmpty() && response.getAssertions().isEmpty()) {
        context.setValidationFailureMessage("Response does not contain any Assertion(s)");
        return ValidationResult.INVALID;
      }
    }
    return ValidationResult.VALID;
  }

}
