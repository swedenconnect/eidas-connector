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
import se.swedenconnect.eidas.connector.authn.EidasAuthenticationError;
import se.swedenconnect.spring.authnserver.error.AuthenticationErrorException;
import se.swedenconnect.spring.authnserver.message.GenericSignMessage;
import se.swedenconnect.spring.authnserver.message.LocalizedMessage;
import se.swedenconnect.spring.authnserver.message.MessageMimeType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link SignMessageProcessor}.
 *
 * @author Martin Lindström
 */
class SignMessageProcessorTest {

  @Test
  void testHtmlIsRefused() {
    final GenericSignMessage message = new GenericSignMessage(
        List.of(LocalizedMessage.ofText(null, "<b>Hello</b>")), MessageMimeType.TEXT_HTML, true, null);

    assertThatThrownBy(() -> SignMessageProcessor.assertSupported(message))
        .isInstanceOfSatisfying(AuthenticationErrorException.class, e ->
            assertThat(e.getError()).isEqualTo(EidasAuthenticationError.SIGN_MESSAGE_NOT_SUPPORTED));
    assertThatThrownBy(() -> SignMessageProcessor.toDisplayMessage(message, "sv"))
        .isInstanceOf(AuthenticationErrorException.class);
  }

  @Test
  void testMarkdown() {
    final GenericSignMessage message = new GenericSignMessage(
        List.of(LocalizedMessage.ofText(null, "I *approve* <b>the</b> contract")), MessageMimeType.TEXT_MARKDOWN,
        true, null);
    SignMessageProcessor.assertSupported(message);
    assertThat(SignMessageProcessor.toDisplayMessage(message, "en"))
        .isEqualTo("<div class=\"message-markdown\"><p>I <em>approve</em> the contract</p></div>");
  }

  @Test
  void testPlainText() {
    final GenericSignMessage message = GenericSignMessage.ofText(null, "I <approve>\nthe\tcontract");
    SignMessageProcessor.assertSupported(message);
    assertThat(SignMessageProcessor.toDisplayMessage(message, "en"))
        .isEqualTo("<div class=\"message-text\">I &lt;approve&gt;<br />the&emsp;contract</div>");
  }

  @Test
  void testNoMessage() {
    SignMessageProcessor.assertSupported(null);
    assertThat(SignMessageProcessor.toDisplayMessage(null, "en")).isNull();
  }

}
