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
package se.elegnamnden.eidas.idp.connector.aaclient;

/**
 * Exception class for errors during communication with the Attribute Authority.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class AttributeAuthorityException extends Exception {

  /** For serializing. */
  private static final long serialVersionUID = 8111349446716372930L;

  /**
   * Constructor that initializes the exception with an error message.
   * 
   * @param message
   *          the error message
   */
  public AttributeAuthorityException(String message) {
    super(message);
  }

  /**
   * Constructor that initializes the exception with an error message and an underlying cause.
   * 
   * @param message
   *          the error message
   * @param cause
   *          the cause of the error
   */
  public AttributeAuthorityException(String message, Throwable cause) {
    super(message, cause);
  }

}
