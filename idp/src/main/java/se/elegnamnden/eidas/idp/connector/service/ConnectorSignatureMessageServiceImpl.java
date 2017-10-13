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

import se.litsec.shibboleth.idp.authn.service.SignatureMessageService;
import se.litsec.shibboleth.idp.authn.service.impl.SignatureMessageServiceImpl;
import se.litsec.swedisheid.opensaml.saml2.signservice.dss.SignMessageMimeTypeEnum;

/**
 * Overrides the default {@link SignatureMessageService} so that we can make sure that only
 * signature messages of the the "text" type is supported.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class ConnectorSignatureMessageServiceImpl extends SignatureMessageServiceImpl {

  /**
   * We only support "text". 
   */
  @Override
  public boolean supportsMimeType(SignMessageMimeTypeEnum mimeType) {
    return mimeType == null || mimeType.equals(SignMessageMimeTypeEnum.TEXT);
  }
  
}
