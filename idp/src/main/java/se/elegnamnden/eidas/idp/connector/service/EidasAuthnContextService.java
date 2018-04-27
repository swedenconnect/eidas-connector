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

  /**
   * Given the supported authentication methods (assurance certification URI:s) indicated by the Proxy Service and the
   * {@code AuthnRequest} that is being processed, the method calculates the {@code RequestedAuthnContext} to be
   * included in the {@code AuthnRequest} that is to be sent to the Proxy Service.
   * 
   * @param context
   *          the request context
   * @param assuranceURIs
   *          a list of assurance certification URI:s
   * @return a {@code RequestedAuthnContext} element
   * @throws ExternalAutenticationErrorCodeException
   *           for matching errors
   */
  RequestedAuthnContext getSendRequestedAuthnContext(ProfileRequestContext<?, ?> context, List<String> assuranceURIs)
      throws ExternalAutenticationErrorCodeException;

}
