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
package se.elegnamnden.eidas.idp.connector.service;

import java.util.List;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;

import se.litsec.shibboleth.idp.authn.ExternalAutenticationErrorCodeException;
import se.litsec.shibboleth.idp.authn.service.ProxyIdpAuthnContextService;

/**
 * An eIDAS version of the {@link ProxyIdpAuthnContextService} supporting eIDAS LoA:s and minimum matching.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public interface EidasAuthnContextService extends ProxyIdpAuthnContextService {

  /** The special purpose AuthnContextClassRef for "eIDAS ping". */
  String EIDAS_TEST_LOA = "http://eidas.europa.eu/LoA/test";

  /**
   * Given the supported authentication methods (assurance certification URI:s) indicated by the Proxy Service and the
   * {@code AuthnRequest} that is being processed, the method calculates the {@code RequestedAuthnContext} to be
   * included in the {@code AuthnRequest} that is to be sent to the Proxy Service.
   * 
   * @param context
   *          the request context
   * @param assuranceURIs
   *          a list of assurance certification URI:s
   * @return a RequestedAuthnContext element
   * @throws ExternalAutenticationErrorCodeException
   *           for matching errors
   */
  RequestedAuthnContext getSendRequestedAuthnContext(final ProfileRequestContext<?, ?> context, final List<String> assuranceURIs)
      throws ExternalAutenticationErrorCodeException;

  /**
   * Predicate that tells if the supplied context represents a test request (ping).
   * 
   * @param context
   *          the request context
   * @return true if this is a test request and false otherwise
   */
  boolean isTestRequest(final ProfileRequestContext<?, ?> context);
}
