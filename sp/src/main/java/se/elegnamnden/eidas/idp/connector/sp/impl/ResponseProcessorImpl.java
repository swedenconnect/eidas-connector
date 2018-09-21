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
package se.elegnamnden.eidas.idp.connector.sp.impl;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.common.assertion.ValidationResult;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.criterion.RoleDescriptorCriterion;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.security.impl.MetadataCredentialResolver;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.criteria.UsageCriterion;
import org.opensaml.xmlsec.config.DefaultSecurityConfigurationBootstrap;
import org.opensaml.xmlsec.encryption.support.DecryptionException;
import org.opensaml.xmlsec.signature.support.SignaturePrevalidator;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessingException;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessingInput;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessingResult;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessor;
import se.elegnamnden.eidas.idp.connector.sp.ResponseStatusErrorException;
import se.elegnamnden.eidas.idp.connector.sp.ResponseValidationException;
import se.elegnamnden.eidas.idp.connector.sp.ResponseValidationSettings;
import se.elegnamnden.eidas.idp.connector.sp.validation.EidasAssertionValidator;
import se.elegnamnden.eidas.idp.connector.sp.validation.EidasResponseValidator;
import se.litsec.opensaml.common.validation.ValidatorException;
import se.litsec.opensaml.saml2.common.assertion.AssertionValidationParametersBuilder;
import se.litsec.opensaml.saml2.common.assertion.AssertionValidator;
import se.litsec.opensaml.saml2.common.response.MessageReplayChecker;
import se.litsec.opensaml.saml2.common.response.MessageReplayException;
import se.litsec.opensaml.saml2.common.response.ResponseValidationParametersBuilder;
import se.litsec.opensaml.saml2.common.response.ResponseValidator;
import se.litsec.opensaml.saml2.metadata.PeerMetadataResolver;
import se.litsec.opensaml.utils.ObjectUtils;
import se.litsec.opensaml.xmlsec.SAMLObjectDecrypter;

/**
 * Response processor for eIDAS SAML Response messages.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class ResponseProcessorImpl implements ResponseProcessor, InitializingBean {

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(ResponseProcessorImpl.class);

  /** The decrypter instance. */
  private SAMLObjectDecrypter decrypter;

  /** The replay checker. */
  private MessageReplayChecker messageReplayChecker;

  /** Used to locate certificates from the IdP metadata. */
  private MetadataCredentialResolver metadataCredentialResolver;

  /** The signature trust engine to be used when validating signatures. */
  private SignatureTrustEngine signatureTrustEngine;

  /** Validator for checking the a Signature is correct with respect to the standards. */
  private SignaturePrevalidator signatureProfileValidator = new SAMLSignatureProfileValidator();

  /** The response validator. */
  private ResponseValidator responseValidator;

  /** The assertion validator. */
  private AssertionValidator assertionValidator;

  /** Response validation settings. */
  protected ResponseValidationSettings responseValidationSettings;

  /** Is this component initialized? */
  private boolean isInitialized = false;

  /** {@inheritDoc} */
  @Override
  public ResponseProcessingResult processSamlResponse(String samlResponse, String relayState, ResponseProcessingInput input,
      PeerMetadataResolver peerMetadataResolver) throws ResponseStatusErrorException, ResponseProcessingException {

    try {
      // Step 1: Decode the SAML response message.
      //
      Response response = this.decodeResponse(samlResponse);

      if (log.isTraceEnabled()) {
        log.trace("[{}] Decoded Response: {}", logId(response), ObjectUtils.toStringSafe(response));
      }

      // The IdP metadata is required for all steps below ...
      //
      final String issuer = response.getIssuer() != null ? response.getIssuer().getValue() : null;
      final EntityDescriptor idpMetadata = issuer != null ? peerMetadataResolver.getMetadata(issuer) : null;

      // Step 2: Validate the Response (including its signature).
      //
      this.validateResponse(response, relayState, input, idpMetadata);

      // Step 3: Make sure this isn't a replay attack
      //
      this.messageReplayChecker.checkReplay(response);

      // Step 4. Check Status
      //
      if (!StatusCode.SUCCESS.equals(response.getStatus().getStatusCode().getValue())) {
        log.info("Authentication failed with status '{}' [{}]", ResponseStatusErrorException.statusToString(response.getStatus()), logId(
          response));
        throw new ResponseStatusErrorException(response.getStatus(), response.getID());
      }

      // Step 5. Verify that the relay state matches the request.
      //
      this.validateRelayState(response, relayState, input);

      // Step 6. Decrypt assertion
      //
      Assertion assertion = this.decrypter.decrypt(response.getEncryptedAssertions().get(0), Assertion.class);
      if (log.isTraceEnabled()) {
        log.trace("[{}] Decrypted Assertion: {}", logId(response, assertion), ObjectUtils.toStringSafe(assertion));
      }

      // Step 7. Validate the assertion
      //
      this.validateAssertion(assertion, response, input, idpMetadata);

      // And finally, build the result.
      //
      return new ResponseProcessingResultImpl(assertion, input.getCountry());
    }
    catch (ValidatorException e) {
      throw new ResponseProcessingException("Validation of Response message failed: " + e.getMessage(), e);
    }
    catch (MessageReplayException e) {
      throw new ResponseProcessingException("Message replay: " + e.getMessage(), e);
    }
    catch (DecryptionException e) {
      throw new ResponseProcessingException("Failed to decrypt assertion: " + e.getMessage(), e);
    }
  }

  /**
   * Initializes the component. Will be invoked by the {@link #afterPropertiesSet()}, so this method only needs to
   * explicitly called if the bean is created outside of the Spring application context.
   * 
   * @throws Exception
   *           for initialization errors
   */
  public void initialize() throws Exception {
    Assert.notNull(this.decrypter, "Property 'decrypter' must be assigned");
    Assert.notNull(this.messageReplayChecker, "Property 'messageReplayChecker' must be assigned");
    Assert.notNull(this.responseValidationSettings, "Property 'responseValidationSettings' must be assigned");

    if (!this.isInitialized) {

      this.metadataCredentialResolver = new MetadataCredentialResolver();
      this.metadataCredentialResolver.setKeyInfoCredentialResolver(DefaultSecurityConfigurationBootstrap
        .buildBasicInlineKeyInfoCredentialResolver());
      this.metadataCredentialResolver.initialize();

      this.signatureTrustEngine = new ExplicitKeySignatureTrustEngine(this.metadataCredentialResolver,
        DefaultSecurityConfigurationBootstrap.buildBasicInlineKeyInfoCredentialResolver());

      this.responseValidator = new EidasResponseValidator(this.signatureTrustEngine, this.signatureProfileValidator);
      this.assertionValidator = new EidasAssertionValidator(this.signatureTrustEngine, this.signatureProfileValidator);

      this.isInitialized = true;
    }
  }

  /**
   * Decodes the received SAML response message into a {@link Response} object.
   * 
   * @param samlResponse
   *          the Base64 encoded SAML response
   * @return a {@code Response} object
   * @throws ResponseProcessingException
   *           for decoding errors
   */
  protected Response decodeResponse(String samlResponse) throws ResponseProcessingException {
    try {
      final byte[] decodedBytes = Base64Support.decode(samlResponse);
      if (decodedBytes == null) {
        log.error("Unable to Base64 decode SAML response message");
        throw new MessageDecodingException("Unable to Base64 decode SAML response message");
      }
      return ObjectUtils.unmarshall(new ByteArrayInputStream(decodedBytes), Response.class);
    }
    catch (MessageDecodingException | XMLParserException | UnmarshallingException e) {
      throw new ResponseProcessingException("Failed to decode message", e);
    }
  }

  /**
   * Validates the response including its signature.
   * 
   * @param response
   *          the response to verify
   * @param relayState
   *          the relay state that was received
   * @param input
   *          the processing input
   * @throws ResponseValidationException
   *           for validation errors
   */
  protected void validateResponse(Response response, String relayState, ResponseProcessingInput input, EntityDescriptor idpMetadata)
      throws ResponseValidationException {

    if (input.getAuthnRequest() == null) {
      String msg = String.format("No AuthnRequest available when processing Response [%s]", logId(response));
      log.error("{}", msg);
      throw new ResponseValidationException(msg);
    }

    IDPSSODescriptor descriptor = idpMetadata != null ? idpMetadata.getIDPSSODescriptor(SAMLConstants.SAML20P_NS) : null;
    if (descriptor == null) {
      throw new ResponseValidationException("Invalid/missing IdP metadata - cannot verify Response signature");
    }

    ValidationContext context = ResponseValidationParametersBuilder.builder()
      .expectedIssuer(idpMetadata.getEntityID())
      .strictValidation(this.responseValidationSettings.isStrictValidation())
      .allowedClockSkew(this.responseValidationSettings.getAllowedClockSkew())
      .maxAgeReceivedMessage(this.responseValidationSettings.getMaxAgeResponse())
      .signatureRequired(Boolean.TRUE)
      .signatureValidationCriteriaSet(new CriteriaSet(new RoleDescriptorCriterion(descriptor), new UsageCriterion(UsageType.SIGNING)))
      .receiveInstant(input.getReceiveInstant())
      .receiveUrl(input.getReceiveURL())
      .authnRequest(input.getAuthnRequest())
      .build();

    ValidationResult result = this.responseValidator.validate(response, context);
    switch (result) {
    case VALID:
      log.debug("Response was successfully validated [{}]", logId(response));
      break;
    case INDETERMINATE:
      log.warn("Validation of Response was indeterminate - {} [{}]", context.getValidationFailureMessage(), logId(response));
      break;
    case INVALID:
      log.error("Validation of Response failed - {} [{}]", context.getValidationFailureMessage(), logId(response));
      throw new ResponseValidationException(context.getValidationFailureMessage());
    }
  }

  /**
   * Validates the received relay state matches what we sent.
   * 
   * @param response
   *          the response
   * @param relayState
   *          the received relay state
   * @param input
   *          the response processing input
   * @throws ResponseValidationException
   *           for validation errors
   */
  protected void validateRelayState(Response response, String relayState, ResponseProcessingInput input)
      throws ResponseValidationException {

    Optional<String> relayStateOptional = relayState == null || relayState.trim().length() == 0 ? Optional.empty()
        : Optional.of(relayState);
    Optional<String> relayStateInputOptional = input.getRelayState() == null || input.getRelayState().trim().length() == 0
        ? Optional.empty() : Optional.of(input.getRelayState());

    boolean relayStateMatch = (!relayStateOptional.isPresent() && !relayStateInputOptional.isPresent())
        || (relayStateOptional.isPresent() && relayState.equals(input.getRelayState()))
        || (relayStateInputOptional.isPresent() && input.getRelayState().equals(relayState));

    if (!relayStateMatch) {
      String msg = String.format("RelayState variable received with response (%s) does not match the sent one (%s)", relayState, input
        .getRelayState());
      log.error("{} [{}]", msg, logId(response));
      throw new ResponseValidationException(msg);
    }
  }

  /**
   * Validates the assertion.
   * 
   * @param assertion
   *          the assertion to validate
   * @param response
   *          the response that contained the assertion
   * @param input
   *          the processing input
   * @throws ResponseValidationException
   *           for validation errors
   */
  protected void validateAssertion(Assertion assertion, Response response, ResponseProcessingInput input, EntityDescriptor idpMetadata)
      throws ValidatorException, ResponseValidationException {

    IDPSSODescriptor descriptor = idpMetadata != null ? idpMetadata.getIDPSSODescriptor(SAMLConstants.SAML20P_NS) : null;
    if (descriptor == null) {
      throw new ResponseValidationException("Invalid/missing IdP metadata - cannot verify Assertion");
    }

    AuthnRequest authnRequest = input.getAuthnRequest();
    String entityID = null;
    if (authnRequest != null) {
      entityID = authnRequest.getIssuer().getValue();
    }

    ValidationContext context = AssertionValidationParametersBuilder.builder()
      .strictValidation(this.responseValidationSettings.isStrictValidation())
      .allowedClockSkew(this.responseValidationSettings.getAllowedClockSkew())
      .maxAgeReceivedMessage(this.responseValidationSettings.getMaxAgeResponse())
      .signatureRequired(this.responseValidationSettings.isRequireSignedAssertions())
      .signatureValidationCriteriaSet(new CriteriaSet(new RoleDescriptorCriterion(descriptor), new UsageCriterion(UsageType.SIGNING)))
      .receiveInstant(input.getReceiveInstant())
      .receiveUrl(input.getReceiveURL())
      .authnRequest(authnRequest)
      .expectedIssuer(idpMetadata.getEntityID())
      .responseIssueInstant(response.getIssueInstant().getMillis())
      .validAudiences(entityID)
      /*
       * We add the entityID as a valid recipient also since Germany places the entityID in the
       * SubjectConfirmation/Recipient field instead of the URL to which the response is sent.
       */
      .validRecipients(input.getReceiveURL(), entityID)
      .build();

    ValidationResult result = this.assertionValidator.validate(assertion, context);
    switch (result) {
    case VALID:
      log.debug("Assertion with ID '{}' was successfully validated", assertion.getID());
      break;
    case INDETERMINATE:
      log.warn("Validation of Assertion with ID '{}' was indeterminate - {}", assertion.getID(), context.getValidationFailureMessage());
      break;
    case INVALID:
      log.error("Validation of Assertion failed - {}", context.getValidationFailureMessage());
      throw new ValidatorException(context);
    }
  }

  /**
   * Assigns the decrypter instance.
   * 
   * @param decrypter
   *          the decrypter
   */
  public void setDecrypter(SAMLObjectDecrypter decrypter) {
    this.decrypter = decrypter;
  }

  /**
   * Assigns the message replay checker to use.
   * 
   * @param messageReplayChecker
   *          message replay checker
   */
  public void setMessageReplayChecker(MessageReplayChecker messageReplayChecker) {
    this.messageReplayChecker = messageReplayChecker;
  }

  /**
   * Assigns the response validation settings.
   * 
   * @param responseValidationSettings
   *          validation settings
   */
  public void setResponseValidationSettings(ResponseValidationSettings responseValidationSettings) {
    this.responseValidationSettings = responseValidationSettings;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    this.initialize();
  }

  private static String logId(Response response) {
    return String.format("response-id:'%s'", response.getID() != null ? response.getID() : "<empty>");
  }

  private static String logId(Response response, Assertion assertion) {
    return String.format("response-id:'%s',assertion-id:'%s'",
      response.getID() != null ? response.getID() : "<empty>",
      assertion.getID() != null ? assertion.getID() : "<empty>");
  }

}
