/*
 * Copyright 2017-2018 E-legitimationsnämnden
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

import org.apache.commons.lang.StringEscapeUtils;

import se.litsec.shibboleth.idp.authn.service.SignMessageContentException;
import se.litsec.shibboleth.idp.authn.service.SignMessagePreProcessor;
import se.litsec.swedisheid.opensaml.saml2.signservice.dss.SignMessageMimeTypeEnum;

/**
 * A processor for handling text to be displayed in the Sign Consent Dialogue.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class TextSignMessagePreProcessor implements SignMessagePreProcessor {

  /** {@inheritDoc} */
  @Override
  public String processSignMessage(String clearText, SignMessageMimeTypeEnum messageType) throws SignMessageContentException {
    
    if (messageType != null && messageType != SignMessageMimeTypeEnum.TEXT) {
      throw new SignMessageContentException("Unsupported MIME type on SignMessage");
    }
    
    String filteredMessage = StringEscapeUtils.escapeHtml(clearText);
    
    // Replace NL with <br />
    filteredMessage = filteredMessage.replaceAll("(\r\n|\n\r|\r|\n)", "<br />");
    
    // Replace tabs with &emsp;
    return filteredMessage.replaceAll("\t", "&emsp;");    
  }

}
