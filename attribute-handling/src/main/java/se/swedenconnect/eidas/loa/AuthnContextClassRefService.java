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

import java.util.List;

import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import se.swedenconnect.spring.saml.idp.error.Saml2ErrorStatusException;

/**
 * TODO
 * 
 * @author Martin Lindström
 */
public interface AuthnContextClassRefService {
  
  /** Special purpose AuthnContext Class Ref for eIDAS test. */
  static String EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF = "http://eidas.europa.eu/LoA/test";
  
  RequestedAuthnContext calculateRequestedAuthnContext(final EntityDescriptor idp,
      final List<String> requestedAuthnContextClassRefs) throws Saml2ErrorStatusException;
  
  

}
