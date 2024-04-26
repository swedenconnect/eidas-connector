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
package se.swedenconnect.eidas.connector.authn.sp;

import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.xmlsec.signature.support.SignaturePrevalidator;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;

import se.swedenconnect.eidas.connector.authn.sp.validators.EidasAssertionValidator;
import se.swedenconnect.eidas.connector.authn.sp.validators.EidasResponseValidator;
import se.swedenconnect.opensaml.saml2.assertion.validation.AssertionValidator;
import se.swedenconnect.opensaml.saml2.response.ResponseProcessorImpl;
import se.swedenconnect.opensaml.saml2.response.replay.MessageReplayChecker;
import se.swedenconnect.opensaml.saml2.response.validation.ResponseValidationSettings;
import se.swedenconnect.opensaml.saml2.response.validation.ResponseValidator;
import se.swedenconnect.opensaml.xmlsec.config.SecurityConfiguration;
import se.swedenconnect.opensaml.xmlsec.encryption.support.SAMLObjectDecrypter;

/**
 * Bean for processing SAML responses received from the foreign country IdP.
 *
 * @author Martin Lindström
 */
public class EidasResponseProcessor extends ResponseProcessorImpl {

  /**
   * Constructor.
   *
   * @param metadataResolver for finding peer metadata
   * @param securityConfiguration the security configuration
   * @param decrypter for decrypting assertions
   * @param messageReplayChecker for protecting against replay attacks
   */
  public EidasResponseProcessor(final MetadataResolver metadataResolver,
      final SecurityConfiguration securityConfiguration, final SAMLObjectDecrypter decrypter,
      final MessageReplayChecker messageReplayChecker, final ResponseValidationSettings validationSettings) {
    this.setMetadataResolver(metadataResolver);
    this.setSecurityConfiguration(securityConfiguration);
    this.setDecrypter(decrypter);
    this.setMessageReplayChecker(messageReplayChecker);
    this.setResponseValidationSettings(validationSettings);
  }

  /** {@inheritDoc} */
  @Override
  protected ResponseValidator createResponseValidator(final SignatureTrustEngine signatureTrustEngine,
      final SignaturePrevalidator signatureProfileValidator) {
    return new EidasResponseValidator(signatureTrustEngine, signatureProfileValidator);
  }

  /** {@inheritDoc} */
  @Override
  protected AssertionValidator createAssertionValidator(final SignatureTrustEngine signatureTrustEngine,
      final SignaturePrevalidator signatureProfileValidator) {
    return new EidasAssertionValidator(signatureTrustEngine, signatureProfileValidator);
  }

}
