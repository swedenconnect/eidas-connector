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

import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Extensions;

import se.litsec.eidas.opensaml.ext.RequestedAttribute;
import se.litsec.eidas.opensaml.ext.RequestedAttributes;
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
