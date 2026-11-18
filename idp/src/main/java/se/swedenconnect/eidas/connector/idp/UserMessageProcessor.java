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
import se.swedenconnect.spring.authnserver.message.GenericUserMessage;
import se.swedenconnect.spring.authnserver.message.LocalizedMessage;
import se.swedenconnect.spring.authnserver.message.MessageMimeType;

/**
 * Prepares user messages for display. Plain text and Markdown messages are displayed. HTML messages are never
 * displayed.
 *
 * @author Martin Lindström
 */
public final class UserMessageProcessor {

  /**
   * Tells whether a user message can be displayed, that is, that it is plain text or Markdown.
   *
   * @param userMessage the user message, may be {@code null}
   * @return {@code true} if there is a message that can be displayed, and {@code false} otherwise
   */
  public static boolean isDisplayable(final @Nullable GenericUserMessage userMessage) {
    return userMessage != null && userMessage.getMimeType() != MessageMimeType.TEXT_HTML;
  }

  /**
   * Prepares a user message for display. The message that best matches the language is used, and if none matches, the
   * first message.
   *
   * @param userMessage the user message, may be {@code null}
   * @param language the language of the page
   * @return the message to display as HTML, or {@code null} if there is no message that can be displayed
   */
  public static @Nullable String toDisplayMessage(
      final @Nullable GenericUserMessage userMessage, final @NonNull String language) {
    if (userMessage == null || !isDisplayable(userMessage)) {
      return null;
    }
    LocalizedMessage message = userMessage.getMessage(language);
    if (message == null) {
      message = userMessage.getMessages().getFirst();
    }
    return MessageHtmlConverter.toHtml(message.getText(), userMessage.getMimeType());
  }

  // Hidden constructor
  private UserMessageProcessor() {
  }

}
