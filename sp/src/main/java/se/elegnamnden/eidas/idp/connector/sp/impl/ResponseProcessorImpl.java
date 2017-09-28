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
import java.util.List;

import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.common.assertion.ValidationContext;
import org.opensaml.saml.common.assertion.ValidationResult;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.criterion.RoleDescriptorCriterion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.security.impl.MetadataCredentialResolver;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.criteria.UsageCriterion;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.xmlsec.config.DefaultSecurityConfigurationBootstrap;
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
import se.elegnamnden.eidas.idp.connector.sp.MessageReplayChecker;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessingException;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessingInput;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessor;
import se.elegnamnden.eidas.idp.connector.sp.ResponseValidationException;
import se.elegnamnden.eidas.idp.connector.sp.SignatureValidationException;
import se.litsec.opensaml.common.validation.ValidatorException;
import se.litsec.opensaml.saml2.common.response.ResponseProfileValidator;
import se.litsec.opensaml.utils.ObjectUtils;

/**
 * Response processor for eIDAS SAML Response messages.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class ResponseProcessorImpl implements ResponseProcessor, InitializingBean {

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(ResponseProcessorImpl.class);

  /** The encryption credentials for the SP. */
  private List<X509Credential> encryptionCredentials;

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
  public void processSamlResponse(String samlResponse, String relayState, ResponseProcessingInput input,
      IdpMetadataResolver idpMetadataResolver)
          throws ResponseProcessingException {

    try {
    // Step 1: Decode the SAML response message.
    //
    Response response = this.decodeResponse(samlResponse);

    log.trace("SAMLResponse:{}", samlResponse);

    if (log.isTraceEnabled()) {
      log.trace("Decoded Response: {}", ObjectUtils.toStringSafe(response));
    }

    // Step 2: Validate the Response against the SAML profile in use.
    //
    this.validateResponseAgainstProfile(response);

    // Step 3: Make sure this isn't a replay attack
    //
    this.messageReplayChecker.checkReplay(response);

    // Step 4. Verify the Response signature
    //
    this.validateSignature(response, idpMetadataResolver.getIdpMetadata(response.getIssuer().getValue()));
    log.debug("Signature on Response message '{}' was successfully validated", response.getID());

    // Step 5. Verify that this response belongs to the AuthnRequest that we sent.
    //
    // TODO

    // Step 6. Check Status.
    //
    }
    catch (ValidatorException e) {
      throw new ResponseProcessingException("Validation of Response message failed: " + e.getMessage(), e);
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
    Assert.notEmpty(this.encryptionCredentials, "Property 'encryptionCredentials' must be assigned");
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

  protected void validateAgainstRequest(Response response, ResponseProcessingInput input) throws ResponseValidationException {

  }

  /**
   * Assigns the encryption credentials for the SP.
   * 
   * @param encryptionCredentials
   *          encryption credentials
   */
  public void setEncryptionCredentials(List<X509Credential> encryptionCredentials) {
    this.encryptionCredentials = encryptionCredentials;
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

}
