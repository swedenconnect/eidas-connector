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

import org.junit.jupiter.api.Test;
import se.swedenconnect.spring.authnserver.message.GenericUserMessage;
import se.swedenconnect.spring.authnserver.message.LocalizedMessage;
import se.swedenconnect.spring.authnserver.message.MessageMimeType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UserMessageProcessor}.
 *
 * @author Martin Lindström
 */
class UserMessageProcessorTest {

  @Test
  void testNoMessage() {
    assertThat(UserMessageProcessor.isDisplayable(null)).isFalse();
    assertThat(UserMessageProcessor.toDisplayMessage(null, "en")).isNull();
  }

  @Test
  void testPlainText() {
    final GenericUserMessage message = GenericUserMessage.ofText(null, "Hello <b>you</b>\r\nand\tbye");
    assertThat(UserMessageProcessor.isDisplayable(message)).isTrue();
    assertThat(UserMessageProcessor.toDisplayMessage(message, "en"))
        .isEqualTo("<div class=\"message-text\">Hello &lt;b&gt;you&lt;/b&gt;<br />and&emsp;bye</div>");
  }

  @Test
  void testMarkdown() {
    final GenericUserMessage message = markdown(null, """
        # Heading

        Some **bold** text,
        a [link](https://www.example.com) and ![image](https://www.example.com/x.png).

        - one
        - two
        """);
    assertThat(UserMessageProcessor.toDisplayMessage(message, "en")).isEqualTo(
        "<div class=\"message-markdown\"><h1>Heading</h1>\n<p>Some <strong>bold</strong> text, a link and .</p>\n"
            + "<ul>\n<li>one</li>\n<li>two</li>\n</ul></div>");
  }

  @Test
  void testHtmlInMarkdownIsRemoved() {
    final GenericUserMessage message = markdown(null, """
        Hello <b onclick="alert(1)">you</b>

        <script>alert(1)</script>

        <div style="color: red">Red</div>
        """);
    final String html = UserMessageProcessor.toDisplayMessage(message, "en");
    assertThat(html).doesNotContain("<b", "onclick", "script", "style", "<div style");
    assertThat(html).startsWith("<div class=\"message-markdown\"><p>Hello you</p>");
  }

  @Test
  void testHtmlIsNotDisplayed() {
    final GenericUserMessage message = new GenericUserMessage(
        List.of(LocalizedMessage.ofText(null, "<p>Hello</p>")), MessageMimeType.TEXT_HTML);
    assertThat(UserMessageProcessor.isDisplayable(message)).isFalse();
    assertThat(UserMessageProcessor.toDisplayMessage(message, "en")).isNull();
  }

  @Test
  void testLanguageSelection() {
    final GenericUserMessage message = new GenericUserMessage(List.of(
        LocalizedMessage.ofText("en", "English"),
        LocalizedMessage.ofText("sv-SE", "Svenska")), MessageMimeType.TEXT_PLAIN);

    assertThat(UserMessageProcessor.toDisplayMessage(message, "en")).contains("English");
    assertThat(UserMessageProcessor.toDisplayMessage(message, "sv")).contains("Svenska");
    assertThat(UserMessageProcessor.toDisplayMessage(message, "de")).contains("English");
  }

  @Test
  void testMessageWithoutLanguage() {
    final GenericUserMessage message = new GenericUserMessage(List.of(
        LocalizedMessage.ofText("de", "Deutsch"),
        LocalizedMessage.ofText(null, "Default")), MessageMimeType.TEXT_PLAIN);

    assertThat(UserMessageProcessor.toDisplayMessage(message, "sv")).contains("Default");
    assertThat(UserMessageProcessor.toDisplayMessage(message, "de")).contains("Deutsch");
  }

  private static GenericUserMessage markdown(final String language, final String text) {
    return new GenericUserMessage(List.of(LocalizedMessage.ofText(language, text)), MessageMimeType.TEXT_MARKDOWN);
  }

}
