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
import org.opensaml.saml.saml2.core.StatusCode;
import se.swedenconnect.spring.authnserver.message.GenericSignMessage;
import se.swedenconnect.spring.authnserver.message.LocalizedMessage;
import se.swedenconnect.spring.authnserver.message.MessageMimeType;
import se.swedenconnect.spring.authnserver.saml.error.SamlErrorStatusException;

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
        .isInstanceOfSatisfying(SamlErrorStatusException.class, e -> {
          assertThat(e.getStatus().statusCode()).isEqualTo(StatusCode.REQUESTER);
          assertThat(e.getStatus().subStatusCode()).isEqualTo(StatusCode.REQUEST_UNSUPPORTED);
        });
    assertThatThrownBy(() -> SignMessageProcessor.toDisplayMessage(message, "sv"))
        .isInstanceOf(SamlErrorStatusException.class);
  }

  @Test
  void testMarkdownIsRefused() {
    final GenericSignMessage message = new GenericSignMessage(
        List.of(LocalizedMessage.ofText(null, "*Hello*")), MessageMimeType.TEXT_MARKDOWN, true, null);
    assertThatThrownBy(() -> SignMessageProcessor.assertSupported(message))
        .isInstanceOf(SamlErrorStatusException.class);
  }

  @Test
  void testPlainText() {
    final GenericSignMessage message = GenericSignMessage.ofText(null, "I <approve>\nthe\tcontract");
    SignMessageProcessor.assertSupported(message);
    assertThat(SignMessageProcessor.toDisplayMessage(message, "en"))
        .isEqualTo("I &lt;approve&gt;<br />the&emsp;contract");
  }

  @Test
  void testNoMessage() {
    SignMessageProcessor.assertSupported(null);
    assertThat(SignMessageProcessor.toDisplayMessage(null, "en")).isNull();
  }

}
