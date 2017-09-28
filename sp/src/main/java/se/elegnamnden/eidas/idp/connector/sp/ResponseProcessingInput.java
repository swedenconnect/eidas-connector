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
package se.elegnamnden.eidas.idp.connector.sp;

import org.opensaml.saml.saml2.core.AuthnRequest;

/**
 * Represents the input passed along with a SAML Response to the {@link ProxyAuthenticationServiceProvider}.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public interface ResponseProcessingInput {

  /**
   * Returns the authentication request message that corresponds to the response message being processed.
   * 
   * @return the AuthnRequest message or {@code null} if no message is available
   */
  AuthnRequest getAuthnRequest();

  /**
   * Returns the RelayState that was included in the request (or {@code null} if none was sent).
   * 
   * @return the RelayState variable or {@code null}
   */
  String getRelayState();

  /**
   * Returns the URL on which the response message was received.
   * 
   * @return the receive URL
   */
  String getReceiveURL();

  /**
   * If the validation should perform a check of the Address(es) found in the assertion, this method should return the
   * address of the client, otherwise return {@code null}.
   * 
   * @return the client IP address of {@code null} if no check should be made
   */
  String getClientIpAddress();

  /**
   * Returns the country code for the country to where the request was sent.
   * 
   * @return an country code
   */
  String getCountry();

}
