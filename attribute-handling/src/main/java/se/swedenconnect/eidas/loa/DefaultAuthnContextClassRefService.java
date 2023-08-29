/*
 * Copyright 2023 Sweden Connect
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
package se.swedenconnect.eidas.loa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;

import se.swedenconnect.opensaml.saml2.attribute.AttributeConstants;
import se.swedenconnect.opensaml.saml2.attribute.AttributeUtils;
import se.swedenconnect.opensaml.saml2.core.build.RequestedAuthnContextBuilder;
import se.swedenconnect.spring.saml.idp.error.Saml2ErrorStatusException;

public class DefaultAuthnContextClassRefService implements AuthnContextClassRefService {

  public DefaultAuthnContextClassRefService() {
  }

  @Override
  public RequestedAuthnContext calculateRequestedAuthnContext(final EntityDescriptor idp,
      final List<String> requestedAuthnContextClassRefs) throws Saml2ErrorStatusException {

    // Handle the eIDAS test URI ...
    //
    if (requestedAuthnContextClassRefs.contains(AuthnContextClassRefService.EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF)) {
      if (requestedAuthnContextClassRefs.size() > 1) {
        final String msg = "If '%s' is requested, no other LoA:s must be requested"
            .formatted(AuthnContextClassRefService.EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF);
        throw new Saml2ErrorStatusException(StatusCode.REQUESTER, StatusCode.REQUEST_UNSUPPORTED, null, msg, msg);
      }
      else {
        return RequestedAuthnContextBuilder.builder()
          .comparison(AuthnContextComparisonTypeEnumeration.EXACT)
          .authnContextClassRefs(AuthnContextClassRefService.EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF)
          .build();
      }
    }
    
    // List which URI:s that are supported by the IdP ...
    //
    final List<String> supported = getAssuranceLevels(idp);
    if (supported.isEmpty()) {
      
    }
    
    // First transform the Swedish URI to an eIDAS URI ...

    return null;
  }

  /**
   * Gets the declared assurance levels from the supplied metadata entry.
   * 
   * @param ed
   *          the metadata entry
   * @return a list of assurance levels
   */
  private static List<String> getAssuranceLevels(final EntityDescriptor ed) {
    
    final Extensions extensions = ed.getExtensions();
    if (extensions == null) {
      return Collections.emptyList();
    }
    final List<String> loas = new ArrayList<>();
    for (final XMLObject xml : extensions.getUnknownXMLObjects()) {
      if (xml instanceof EntityAttributes ea) {
        ea.getAttributes().stream()
        .filter(a -> AttributeConstants.ASSURANCE_CERTIFICATION_ATTRIBUTE_NAME.equals(a.getName()))
        .map(a -> AttributeUtils.getAttributeStringValues(a))
        .flatMap(List::stream)
        .forEach(a -> {
          if (loas.contains(a)) {
            loas.add(a);
          }
        });        
      }
    }
    return loas;
  }
  
}
