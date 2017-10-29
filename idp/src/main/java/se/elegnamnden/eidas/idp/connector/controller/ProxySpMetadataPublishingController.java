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
package se.elegnamnden.eidas.idp.connector.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import se.litsec.opensaml.saml2.metadata.EntityDescriptorContainer;
import se.litsec.shibboleth.idp.metadata.controller.AbstractMetadataPublishingController;

/**
 * MVC controller for publishing signed, and up-to-date, SAML metadata for the SP part of the IdP Proxy.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
@Controller
public class ProxySpMetadataPublishingController extends AbstractMetadataPublishingController {

  /**
   * Constructor taking the container holding the metadata to publish.
   * 
   * @param metadataContainer
   *          the metadata to publish
   */  
  public ProxySpMetadataPublishingController(EntityDescriptorContainer metadataContainer) {
    super(metadataContainer);
  }
  
  /** {@inheritDoc} */
  @RequestMapping(value = "/sp", method = RequestMethod.GET)
  @ResponseBody
  public HttpEntity<byte[]> getMetadata(HttpServletRequest request, @RequestHeader(name = "Accept", required = false) String acceptHeader) {
    return super.getMetadata(request, acceptHeader);
  }

}
