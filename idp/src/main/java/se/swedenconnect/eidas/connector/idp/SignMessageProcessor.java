/*
 * Copyright 2017-2026 Sweden Connect
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
package se.swedenconnect.eidas.connector.idp;

import org.apache.commons.text.StringEscapeUtils;
import org.opensaml.saml.saml2.core.StatusCode;
import se.swedenconnect.eidas.connector.authn.SamlErrors;
import se.swedenconnect.spring.authnserver.message.GenericSignMessage;
import se.swedenconnect.spring.authnserver.message.LocalizedMessage;
import se.swedenconnect.spring.authnserver.message.MessageMimeType;
import se.swedenconnect.spring.authnserver.saml.error.SamlErrorStatusException;

/**
 * Prepares sign messages for display. The connector only displays plain text sign messages. HTML and Markdown are
 * never displayed.
 *
 * @author Martin Lindström
 */
public final class SignMessageProcessor {

  /**
   * Asserts that a sign message can be displayed, that is, that it is plain text.
   *
   * @param signMessage the sign message, may be {@code null}
   * @throws SamlErrorStatusException {@code Requester} / {@code RequestUnsupported} if the message is not plain
   *     text
   */
  public static void assertSupported(final GenericSignMessage signMessage) throws SamlErrorStatusException {
    if (signMessage != null && signMessage.getMimeType() != MessageMimeType.TEXT_PLAIN) {
      throw SamlErrors.errorWithMessageCode(StatusCode.REQUESTER, StatusCode.REQUEST_UNSUPPORTED,
          SamlErrors.SIGN_MESSAGE_MESSAGE_CODE,
          "eIDAS Connector does not support display of HTML or Markdown SignMessages");
    }
  }

  /**
   * Prepares a plain text sign message for display. The text is HTML escaped, line breaks become {@code <br />} and
   * tabs become {@code &emsp;}.
   *
   * @param signMessage the sign message, may be {@code null}
   * @param language the language of the page
   * @return the message to display, or {@code null} if there is no message
   * @throws SamlErrorStatusException if the message is not plain text
   */
  public static String toDisplayMessage(final GenericSignMessage signMessage, final String language)
      throws SamlErrorStatusException {
    if (signMessage == null) {
      return null;
    }
    assertSupported(signMessage);
    LocalizedMessage message = signMessage.getMessage(language);
    if (message == null) {
      message = signMessage.getMessages().getFirst();
    }
    String filteredMessage = StringEscapeUtils.escapeHtml4(message.getText());

    // Replace NL with <br />
    filteredMessage = filteredMessage.replaceAll("(\r\n|\n\r|\r|\n)", "<br />");

    // Replace tabs with &emsp;
    return filteredMessage.replaceAll("\t", "&emsp;");
  }

  // Hidden constructor
  private SignMessageProcessor() {
  }

}
