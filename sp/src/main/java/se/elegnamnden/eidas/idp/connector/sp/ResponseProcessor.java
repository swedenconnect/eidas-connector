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

import org.opensaml.saml.saml2.metadata.EntityDescriptor;

/**
 * Interface for a SAML response processor.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public interface ResponseProcessor {

  void processSamlResponse(String samlResponse, String relayState, ResponseProcessingInput input, IdpMetadataResolver idpMetadataResolver)
      throws ResponseProcessingException;

  /**
   * Functional interface for obtaining the metadata for the issuing IdP.
   */
  @FunctionalInterface
  public interface IdpMetadataResolver {

    /**
     * Returns the metadata {@code EntityDescriptor} for the given entityID.
     * 
     * @param entityID
     *          the IdP entityID
     * @return an {@code EntityDescriptor} object, or {@code null} if no metadata record can be found
     */
    EntityDescriptor getIdpMetadata(String entityID);

  }

}
