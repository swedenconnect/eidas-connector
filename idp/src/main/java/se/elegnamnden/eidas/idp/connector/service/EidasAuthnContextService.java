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
   * When the eIDAS connector knows which foreign Proxy Service IdP to communicate with it must inform the service
   * whether this IdP supports the notified/non notified eID scheme concept. This is needed so that the service can
   * perform a correct transformation between national AuthnContextClassRef URI:s and eIDAS URI:s.
   * <p>
   * By default, a service assumes that the foreign IdP understands the notified/non notified eID scheme concept.
   * </p>
   * 
   * @param context
   *          the request context
   * @param supportsNonNotifiedConcept
   *          does the foreign IdP support the notified/non notified eID scheme concept?
   */
  void setSupportsNonNotifiedConcept(ProfileRequestContext<?, ?> context, boolean supportsNonNotifiedConcept);

  /**
   * Maps to {@link #getSendAuthnContextClassRefs(ProfileRequestContext, List, boolean)} where the
   * {@code idpSupportsSignMessage} parameter always is {@code false}. This is because eIDAS does not support the
   * SignMessage extension and signature message displaying.
   * <p>
   * eIDAS uses "minimum" comparison of AuthnContextClassRefs which means that we only have to send one URI (the one
   * with the lowest rank). Therefore, one URI instead of a list of URI:s will be returned. 
   * </p>
   * 
   * @param context
   *          the request context
   * @param assuranceURIs
   *          IdP assurance certification URI:s
   * @throws ExternalAutenticationErrorCodeException
   *           if no AuthnContext URI:s matches
   */
  String getSendAuthnContextClassRef(ProfileRequestContext<?, ?> context, List<String> assuranceURIs)
      throws ExternalAutenticationErrorCodeException;

}
