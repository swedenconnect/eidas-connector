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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.opensaml.saml.saml2.core.StatusCode;
import se.swedenconnect.eidas.connector.authn.SamlErrors;
import se.swedenconnect.spring.authnserver.message.GenericSignMessage;
import se.swedenconnect.spring.authnserver.message.LocalizedMessage;
import se.swedenconnect.spring.authnserver.message.MessageMimeType;
import se.swedenconnect.spring.authnserver.saml.error.SamlErrorStatusException;

/**
 * Prepares sign messages for display. The connector displays plain text and Markdown sign messages. HTML is never
 * displayed.
 *
 * @author Martin Lindström
 */
public final class SignMessageProcessor {

  /**
   * Asserts that a sign message can be displayed, that is, that it is plain text or Markdown.
   *
   * @param signMessage the sign message, may be {@code null}
   * @throws SamlErrorStatusException {@code Requester} / {@code RequestUnsupported} if the message is HTML
   */
  public static void assertSupported(final @Nullable GenericSignMessage signMessage) throws SamlErrorStatusException {
    if (signMessage != null && signMessage.getMimeType() == MessageMimeType.TEXT_HTML) {
      throw SamlErrors.errorWithMessageCode(StatusCode.REQUESTER, StatusCode.REQUEST_UNSUPPORTED,
          SamlErrors.SIGN_MESSAGE_MESSAGE_CODE,
          "eIDAS Connector does not support display of HTML SignMessages");
    }
  }

  /**
   * Prepares a sign message for display. A plain text message is HTML escaped, and keeps its line breaks and tabs. A
   * Markdown message is rendered into HTML, from which HTML in the Markdown is removed.
   *
   * @param signMessage the sign message, may be {@code null}
   * @param language the language of the page
   * @return the message to display as HTML, or {@code null} if there is no message
   * @throws SamlErrorStatusException if the message is HTML
   */
  public static @Nullable String toDisplayMessage(
      final @Nullable GenericSignMessage signMessage, final @NonNull String language)
      throws SamlErrorStatusException {
    if (signMessage == null) {
      return null;
    }
    assertSupported(signMessage);
    LocalizedMessage message = signMessage.getMessage(language);
    if (message == null) {
      message = signMessage.getMessages().getFirst();
    }
    return MessageHtmlConverter.toHtml(message.getText(), signMessage.getMimeType());
  }

  // Hidden constructor
  private SignMessageProcessor() {
  }

}
