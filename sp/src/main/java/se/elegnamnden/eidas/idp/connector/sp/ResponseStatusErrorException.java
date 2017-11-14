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

import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;

/**
 * Exception that indicates a non-successful status code received in a Response message.
 *
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class ResponseStatusErrorException extends Exception {

  /** For serializing. */
  private static final long serialVersionUID = -8050896611037764108L;

  /** The SAML Status. */
  private Status status;

  /** The Response ID. */
  private String responseId;

  /**
   * Constructor taking the error status and the response ID.
   * 
   * @param status
   *          status
   * @param responseId
   *          the response ID
   */
  public ResponseStatusErrorException(Status status, String responseId) {
    super(statusToString(status));
    this.status = status;
    this.responseId = responseId;
    
    if (StatusCode.SUCCESS.equals(status.getStatusCode().getValue())) {
      throw new IllegalArgumentException("Status is success - can not throw ResponseStatusErrorException");
    }
  }

  /**
   * Returns the status object.
   * 
   * @return the status object
   */
  public Status getStatus() {
    return this.status;
  }

  /**
   * Returns the ID of the Response.
   * 
   * @return the response ID
   */
  public String getResponseId() {
    return this.responseId;
  }

  /**
   * Returns a textual representation of the status.
   * 
   * @param status
   *          the Status to print
   * @return a status string
   */
  public static String statusToString(Status status) {
    StringBuffer sb = new StringBuffer("Status: ");
    sb.append(status.getStatusCode().getValue());
    if (status.getStatusCode().getStatusCode() != null) {
      sb.append(", ").append(status.getStatusCode().getStatusCode().getValue());
    }
    if (status.getStatusMessage() != null && status.getStatusMessage().getMessage() != null) {
      sb.append(" - ").append(status.getStatusMessage().getMessage());
    }
    return sb.toString();
  }

}
