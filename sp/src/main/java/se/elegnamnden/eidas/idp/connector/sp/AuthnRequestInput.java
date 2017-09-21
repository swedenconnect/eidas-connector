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

/**
 * Input for the proxy server SP when generating an AuthnRequest.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class AuthnRequestInput {
  
  /** The RelayState from the AuthnRequest. */
  private String relayState;
  
  /** The requested level of assurance URI. Only one is specified since eIDAS uses "minimum" matching. */
  private String requestedLevelOfAssurance;
    
  public String getRelayState() {
    return this.relayState;
  }

  public void setRelayState(String relayState) {
    this.relayState = relayState;
  }
  
  public String getRequestedLevelOfAssurance() {
    return this.requestedLevelOfAssurance;
  }

  public void setRequestedLevelOfAssurance(String requestedLevelOfAssurance) {
    this.requestedLevelOfAssurance = requestedLevelOfAssurance;
  }
  
}
