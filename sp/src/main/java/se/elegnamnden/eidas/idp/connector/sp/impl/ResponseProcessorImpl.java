/*
 * The eidas-connector project is the implementation of the Swedish eIDAS 
 * connector built on top of the Shibboleth IdP.
 *
 * More details on <https://github.com/elegnamnden/eidas-connector> 
 * Copyright (C) 2017 E-legitimationsnämnden
 * 
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.elegnamnden.eidas.idp.connector.sp.impl;

import java.io.ByteArrayInputStream;

import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.common.SignableSAMLObject;
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
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.criteria.UsageCriterion;
import org.opensaml.xmlsec.config.DefaultSecurityConfigurationBootstrap;
import org.opensaml.xmlsec.encryption.support.DecryptionException;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignaturePrevalidator;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.w3c.dom.Attr;

import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessingException;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessingInput;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessingResult;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessor;
import se.elegnamnden.eidas.idp.connector.sp.ResponseStatusErrorException;
import se.elegnamnden.eidas.idp.connector.sp.ResponseValidationException;
import se.elegnamnden.eidas.idp.connector.sp.SignatureValidationException;
import se.litsec.opensaml.common.validation.ValidatorException;
import se.litsec.opensaml.saml2.common.response.MessageReplayChecker;
import se.litsec.opensaml.saml2.common.response.MessageReplayException;
import se.litsec.opensaml.saml2.common.response.ResponseProfileValidator;
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

  /** The response profile validator. */
  private ResponseProfileValidator responseProfileValidator;

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

      // Step 2: Validate the Response against the SAML profile in use.
      //
      this.validateResponseAgainstProfile(response);

      // Step 3: Make sure this isn't a replay attack
      //
      this.messageReplayChecker.checkReplay(response);

      // Step 4. Verify the Response signature
      //
      this.validateSignature(response, peerMetadataResolver.getMetadata(response.getIssuer().getValue()));
      log.debug("Signature on Response was successfully validated [{}]", logId(response));

      // Step 5. Verify that this response belongs to the AuthnRequest that we sent.
      //
      this.validateAgainstRequest(response, relayState, input);

      // Step 6. Check Status
      //
      if (!StatusCode.SUCCESS.equals(response.getStatus().getStatusCode().getValue())) {
        log.info("Authentication failed with status '{}' [{}]", ResponseStatusErrorException.statusToString(response.getStatus()), logId(
          response));
        throw new ResponseStatusErrorException(response.getStatus(), response.getID());
      }

      // Step 7. Decrypt assertion
      //
      Assertion assertion = this.decrypter.decrypt(response.getEncryptedAssertions().get(0), Assertion.class);
      if (log.isTraceEnabled()) {
        log.trace("[{}] Decrypted Assertion: {}", logId(response), ObjectUtils.toStringSafe(assertion));
      }

      // Step 8. Validate the assertion
      //

      // TODO

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
    Assert.notNull(this.responseProfileValidator, "Property 'responseProfileValidator' must be assigned");

    if (!this.isInitialized) {

      this.metadataCredentialResolver = new MetadataCredentialResolver();
      this.metadataCredentialResolver.setKeyInfoCredentialResolver(DefaultSecurityConfigurationBootstrap
        .buildBasicInlineKeyInfoCredentialResolver());
      this.metadataCredentialResolver.initialize();

      this.signatureTrustEngine = new ExplicitKeySignatureTrustEngine(this.metadataCredentialResolver,
        DefaultSecurityConfigurationBootstrap.buildBasicInlineKeyInfoCredentialResolver());

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
   * Validates that the response message is valid regarding the SAML profile.
   * 
   * @param response
   *          the response to validate
   * @throws ValidatorException
   *           if validation fails
   */
  protected void validateResponseAgainstProfile(Response response) throws ValidatorException {
    ValidationContext context = new ValidationContext();
    ValidationResult result = this.responseProfileValidator.validate(response, context);
    switch (result) {
    case VALID:
      log.debug("Response with ID '{}' was successfully validated", response.getID());
      break;
    case INDETERMINATE:
      log.warn("Validation of Response with '{}' was indeterminate - {}", response.getID(), context.getValidationFailureMessage());
      break;
    case INVALID:
      log.error("Validation of Response failed - {}", context.getValidationFailureMessage());
      throw new ValidatorException(context);
    }
  }

  /**
   * Validates the signature of the supplied signed object (Response).
   * 
   * @param signedObject
   *          the object to check
   * @param idpMetadata
   *          the IdP metadata holding the signature certificate(s)
   * @throws SignatureValidationException
   *           for signature validation errors
   */
  protected void validateSignature(SignableSAMLObject signedObject, EntityDescriptor idpMetadata) throws SignatureValidationException {

    // Resolve the certificate(s) from the IdP metadata that we need to validate the signature.
    //
    IDPSSODescriptor descriptor = idpMetadata != null ? idpMetadata.getIDPSSODescriptor(SAMLConstants.SAML20P_NS) : null;
    if (descriptor == null) {
      throw new SignatureValidationException("Invalid IdP metadata");
    }

    // Temporary code until we figure out how to make the OpenSAML unmarshaller to
    // mark the ID attribute as an ID.
    //
    Attr idAttr = signedObject.getDOM().getAttributeNode("ID");
    if (idAttr != null) {
      idAttr.getOwnerElement().setIdAttributeNode(idAttr, true);
    }

    // The signature to validate.
    //
    final Signature signature = signedObject.getSignature();

    // Criteria for finding the certificates to use when validating the signature.
    //
    CriteriaSet criteria = new CriteriaSet(new RoleDescriptorCriterion(descriptor), new UsageCriterion(UsageType.SIGNING));

    // Is the signature correct according to the SAML signature profile?
    //
    try {
      this.signatureProfileValidator.validate(signature);
    }
    catch (SignatureException e) {
      String msg = String.format("Signature failed pre-validation: %s", e.getMessage());
      log.warn(msg);
      throw new SignatureValidationException(msg, e);
    }

    // Validate the signature.
    //
    try {
      if (!this.signatureTrustEngine.validate(signature, criteria)) {
        String msg = "Signature validation failed";
        log.warn(msg);
        throw new SignatureValidationException(msg);
      }
    }
    catch (SecurityException e) {
      String msg = String.format("A problem was encountered evaluating the signature: %s", e.getMessage());
      log.warn(msg);
      throw new SignatureValidationException(msg, e);
    }

  }

  /**
   * Validates the received response message against the AuthnRequest that was sent.
   * 
   * @param response
   *          the response
   * @param input
   *          the response processing input
   * @throws ResponseValidationException
   *           for validation errors
   */
  protected void validateAgainstRequest(Response response, String relayState, ResponseProcessingInput input)
      throws ResponseValidationException {

    final AuthnRequest authnRequest = input.getAuthnRequest();
    if (authnRequest == null) {
      String msg = String.format("No AuthnRequest available when processing Response [%s]", logId(response));
      log.error("{}", msg);
      throw new ResponseValidationException(msg);
    }

    // Is it the response that we are expecting?
    //
    if (!response.getInResponseTo().equals(authnRequest.getID())) {
      String msg = String.format("Expected Response message for AuthnRequest with ID '%s', but this Response is for '%s' [%s]", authnRequest
        .getID(), response.getID(), logId(response));
      log.info(msg);
      throw new ResponseValidationException(msg);
    }

    // Does the relayState variables match?
    //
    boolean relayStateMatch = (relayState == null && input.getRelayState() == null)
        || (relayState != null && relayState.equals(input.getRelayState()))
        || (input.getRelayState() != null && input.getRelayState().equals(relayState));

    if (!relayStateMatch) {
      String msg = String.format("RelayState variable received with response (%s) does not match the sent one (%s)", relayState, input
        .getRelayState());
      log.error("{} [{}]", msg, logId(response));
      throw new ResponseValidationException(msg);
    }

    // Did we receive it on the correct location?
    //
    if (response.getDestination() != null && input.getReceiveURL() != null) {
      if (!response.getDestination().equals(input.getReceiveURL())) {
        String msg = String.format(
          "Destination attribute (%s) of Response does not match URL on which response was received (%s)", response.getDestination(), input
            .getReceiveURL());
        log.error("{} [{}]", msg, logId(response));
        throw new ResponseValidationException(msg);
      }
    }

    // Make checks to ensure that the IssueInstant of the Response makes sense.
    // if (response.getIssueInstant() != null) {
    // DateTime now = this.getNow(responseProcessingInput);
    // long clockSkew = this.getAllowedClockSkew(responseProcessingInput);
    // if (!this.verifyIssueInstant(response.getIssueInstant(), now.getMillis(), clockSkew)) {
    // String msg = String.format(
    // "Validation of the issueInstant of the response failed [issueInstant: %s - current time: %s - max allowed age: %d
    // milliseconds - allowed clock skew: %d milliseconds]",
    // response.getIssueInstant().toString(), now.toString(), this.maxAgeResponse, clockSkew);
    // logger.error(String.format("%s [logId]", msg, logId));
    // throw new ResponseProcessingException(msg, response);
    // }
    // }
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
   * Assigns the response profile validator.
   * 
   * @param responseProfileValidator
   *          response profile validator
   */
  public void setResponseProfileValidator(ResponseProfileValidator responseProfileValidator) {
    this.responseProfileValidator = responseProfileValidator;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    this.initialize();
  }

  private static String logId(Response response) {
    return String.format("response-id:'%s'", response.getID() != null ? response.getID() : "<empty>");
  }

}
