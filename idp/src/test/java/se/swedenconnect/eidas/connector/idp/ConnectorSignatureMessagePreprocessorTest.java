/*
 * Copyright 2017-2024 Sweden Connect
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
import se.swedenconnect.opensaml.sweid.saml2.signservice.dss.SignMessageMimeTypeEnum;
import se.swedenconnect.spring.saml.idp.error.Saml2ErrorStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the ConnectorSignatureMessagePreprocessor class.
 * 
 * @author Martin Lindström
 */
public final class ConnectorSignatureMessagePreprocessorTest {

    private final ConnectorSignatureMessagePreprocessor preprocessor = new ConnectorSignatureMessagePreprocessor();

    @Test
    public void testProcessSignMessage_HtmlTypeNotAllowed() {
        final String message = "Hello world!";
        final String base64Message = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));

        assertThrows(Saml2ErrorStatusException.class, () ->
            preprocessor.processSignMessage(base64Message, SignMessageMimeTypeEnum.TEXT_HTML));
    }

    @Test
    public void testProcessSignMessage_SuccessfulProcessing() {
        final String message = "Hello\nworld!";
        final String expectedMessage = "Hello<br />world!";
        final String base64Message = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));

        final String processedMessage = preprocessor.processSignMessage(base64Message, SignMessageMimeTypeEnum.TEXT);

        assertEquals(expectedMessage, processedMessage);
    }
}