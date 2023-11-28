/*
 * Copyright 2023 Sweden Connect
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
package se.swedenconnect.eidas.connector.authn.sp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.opensaml.saml2.response.ResponseProcessingInput;
import se.swedenconnect.opensaml.saml2.response.ResponseProcessorImpl;
import se.swedenconnect.opensaml.saml2.response.replay.MessageReplayChecker;
import se.swedenconnect.opensaml.saml2.response.validation.ResponseValidationException;
import se.swedenconnect.opensaml.xmlsec.encryption.support.SAMLObjectDecrypter;
import se.swedenconnect.spring.saml.idp.error.Saml2ErrorStatusException;

/**
 * Bean for processing SAML responses received from the foreign country IdP.
 *
 * @author Martin Lindström
 */
@Slf4j
public class EidasResponseProcessor extends ResponseProcessorImpl {

  /** Attribute names for the eIDAS minimum dataset. */
  private static final List<String> EIDAS_MINIMUM_DATASET = List.of(
      se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME,
      se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME,
      se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME,
      se.swedenconnect.opensaml.eidas.ext.attributes.AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME);

  // TODO
  // ResponseValidationSettings
  // SignaturePrevalidator

  public EidasResponseProcessor(
      final MetadataResolver metadataResolver,
      final SAMLObjectDecrypter decrypter,
      final MessageReplayChecker messageReplayChecker) {
    this.setMetadataResolver(metadataResolver);
    this.setDecrypter(decrypter);
    this.setMessageReplayChecker(messageReplayChecker);
  }

  @Override
  protected void validateAssertion(final Assertion assertion, final Response response,
      final ResponseProcessingInput input, final EntityDescriptor idpMetadata,
      final ValidationContext validationContext)
      throws ResponseValidationException {

    super.validateAssertion(assertion, response, input, idpMetadata, validationContext);

    // Validate that all attributes from the eIDAS minimum dataset were received ...
    //
    final List<String> minimumDataSet = new ArrayList<>(EIDAS_MINIMUM_DATASET);
    Optional.ofNullable(assertion.getAttributeStatements())
      .map(as -> as.get(0))
      .map(AttributeStatement::getAttributes)
      .ifPresent(attributes ->
        attributes.stream().forEach(a -> minimumDataSet.removeIf(m -> m.equals(a.getName()))));

    if (minimumDataSet.size() > 0) {
      final String msg = "Invalid eIDAS assertion - missing required attribute(s) %s".formatted(minimumDataSet);
      final Saml2ErrorStatusException error = new Saml2ErrorStatusException(
          StatusCode.RESPONDER, StatusCode.AUTHN_FAILED, null,
          "Invalid assertion received from foreign IdP - missing mandatory attribute(s)", msg);
      throw new EidasResponseValidationException(error);
    }
  }

}
