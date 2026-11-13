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
package se.swedenconnect.eidas.simulator.authn;

import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;
import net.shibboleth.shared.security.impl.RandomIdentifierGenerationStrategy;
import org.jspecify.annotations.NonNull;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.xmlsec.SecurityConfigurationSupport;
import org.opensaml.xmlsec.encryption.support.EncryptionException;
import org.opensaml.xmlsec.impl.BasicEncryptionConfiguration;
import org.opensaml.xmlsec.impl.BasicSignatureSigningConfiguration;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedenconnect.eidas.simulator.country.SimulatedCountry;
import se.swedenconnect.eidas.simulator.country.SimulatedUser;
import se.swedenconnect.opensaml.xmlsec.encryption.support.SAMLObjectEncrypter;
import se.swedenconnect.opensaml.xmlsec.signature.support.SAMLObjectSigner;
import se.swedenconnect.security.credential.opensaml.OpenSamlCredential;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Creates the responses of a simulated country: signed success responses holding a signed and encrypted assertion,
 * and signed error responses.
 *
 * @author Martin Lindström
 */
public class ResponseFactory {

  /** Logger. */
  private static final Logger log = LoggerFactory.getLogger(ResponseFactory.class);

  /** How long an assertion is valid after it was issued. */
  private static final Duration ASSERTION_VALIDITY = Duration.ofMinutes(5);

  /** How long before it was issued an assertion is valid, to allow for clock skew. */
  private static final Duration NOT_BEFORE_SKEW = Duration.ofSeconds(10);

  /** Selects the algorithms. */
  private final @NonNull AlgorithmSelector algorithmSelector;

  /** Encrypts assertions. */
  private final @NonNull SAMLObjectEncrypter encrypter;

  /** Generates IDs. */
  private final IdentifierGenerationStrategy idGenerator = new RandomIdentifierGenerationStrategy();

  /** The clock. */
  private @NonNull Clock clock = Clock.systemUTC();

  /**
   * Constructor.
   *
   * @param algorithmSelector selects the algorithms
   * @throws ComponentInitializationException if the encrypter cannot be created
   */
  public ResponseFactory(final @NonNull AlgorithmSelector algorithmSelector)
      throws ComponentInitializationException {
    this.algorithmSelector = Objects.requireNonNull(algorithmSelector, "algorithmSelector must not be null");
    this.encrypter = new SAMLObjectEncrypter();
  }

  /**
   * Creates a success response for a user.
   *
   * @param country the simulated country
   * @param request the request
   * @param user the user
   * @param loa the level of assurance
   * @param attributes the released attributes
   * @return a signed response holding an encrypted assertion
   * @throws ResponseCreationException if the response cannot be created
   */
  public @NonNull Response createSuccessResponse(final @NonNull SimulatedCountry country,
      final @NonNull ProcessedRequest request, final @NonNull SimulatedUser user, final @NonNull String loa,
      final @NonNull List<Attribute> attributes) throws ResponseCreationException {

    final ResponseTarget target = request.target();
    final Instant now = this.clock.instant();

    final Assertion assertion = (Assertion) XMLObjectSupport.buildXMLObject(Assertion.DEFAULT_ELEMENT_NAME);
    assertion.setID(this.idGenerator.generateIdentifier());
    assertion.setVersion(SAMLVersion.VERSION_20);
    assertion.setIssueInstant(now);
    assertion.setIssuer(createIssuer(country));

    final Subject subject = (Subject) XMLObjectSupport.buildXMLObject(Subject.DEFAULT_ELEMENT_NAME);
    final NameID nameId = (NameID) XMLObjectSupport.buildXMLObject(NameID.DEFAULT_ELEMENT_NAME);
    nameId.setFormat(request.nameIdFormat());
    nameId.setValue(NameIDType.TRANSIENT.equals(request.nameIdFormat())
        ? this.idGenerator.generateIdentifier()
        : Objects.requireNonNullElse(AttributeReleaser.getPersonIdentifier(country, request, user),
            this.idGenerator.generateIdentifier()));
    subject.setNameID(nameId);
    final SubjectConfirmation confirmation =
        (SubjectConfirmation) XMLObjectSupport.buildXMLObject(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
    confirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
    final SubjectConfirmationData data =
        (SubjectConfirmationData) XMLObjectSupport.buildXMLObject(SubjectConfirmationData.DEFAULT_ELEMENT_NAME);
    data.setInResponseTo(target.inResponseTo());
    data.setNotOnOrAfter(now.plus(ASSERTION_VALIDITY));
    data.setRecipient(target.assertionConsumerServiceUrl());
    confirmation.setSubjectConfirmationData(data);
    subject.getSubjectConfirmations().add(confirmation);
    assertion.setSubject(subject);

    final Conditions conditions = (Conditions) XMLObjectSupport.buildXMLObject(Conditions.DEFAULT_ELEMENT_NAME);
    conditions.setNotBefore(now.minus(NOT_BEFORE_SKEW));
    conditions.setNotOnOrAfter(now.plus(ASSERTION_VALIDITY));
    final AudienceRestriction restriction =
        (AudienceRestriction) XMLObjectSupport.buildXMLObject(AudienceRestriction.DEFAULT_ELEMENT_NAME);
    final Audience audience = (Audience) XMLObjectSupport.buildXMLObject(Audience.DEFAULT_ELEMENT_NAME);
    audience.setURI(target.getConnectorEntityId());
    restriction.getAudiences().add(audience);
    conditions.getAudienceRestrictions().add(restriction);
    assertion.setConditions(conditions);

    final AuthnStatement authnStatement =
        (AuthnStatement) XMLObjectSupport.buildXMLObject(AuthnStatement.DEFAULT_ELEMENT_NAME);
    authnStatement.setAuthnInstant(now);
    authnStatement.setSessionIndex(this.idGenerator.generateIdentifier());
    final AuthnContext authnContext = (AuthnContext) XMLObjectSupport.buildXMLObject(AuthnContext.DEFAULT_ELEMENT_NAME);
    final AuthnContextClassRef classRef =
        (AuthnContextClassRef) XMLObjectSupport.buildXMLObject(AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
    classRef.setURI(loa);
    authnContext.setAuthnContextClassRef(classRef);
    authnStatement.setAuthnContext(authnContext);
    assertion.getAuthnStatements().add(authnStatement);

    final AttributeStatement attributeStatement =
        (AttributeStatement) XMLObjectSupport.buildXMLObject(AttributeStatement.DEFAULT_ELEMENT_NAME);
    attributeStatement.getAttributes().addAll(attributes);
    assertion.getAttributeStatements().add(attributeStatement);

    this.sign(assertion, country, target.connectorMetadata());

    final Response response = this.createResponse(country, target, StatusCode.SUCCESS, null, null);
    response.getEncryptedAssertions().add(this.encrypt(assertion, country, target.connectorMetadata()));
    this.sign(response, country, target.connectorMetadata());
    return response;
  }

  /**
   * Creates an error response.
   *
   * @param country the simulated country
   * @param target where the response is sent
   * @param status the status
   * @return a signed error response
   * @throws ResponseCreationException if the response cannot be created
   */
  public @NonNull Response createErrorResponse(final @NonNull SimulatedCountry country,
      final @NonNull ResponseTarget target, final @NonNull EidasStatus status) throws ResponseCreationException {
    final Response response =
        this.createResponse(country, target, status.statusCode(), status.subStatusCode(), status.message());
    this.sign(response, country, target.connectorMetadata());
    return response;
  }

  /**
   * Creates a response without assertion.
   *
   * @param country the simulated country
   * @param target where the response is sent
   * @param statusCode the top-level status code
   * @param subStatusCode the second-level status code, or {@code null}
   * @param message the status message, or {@code null}
   * @return the response
   */
  private @NonNull Response createResponse(final @NonNull SimulatedCountry country,
      final @NonNull ResponseTarget target, final @NonNull String statusCode, final String subStatusCode,
      final String message) {
    final Response response = (Response) XMLObjectSupport.buildXMLObject(Response.DEFAULT_ELEMENT_NAME);
    response.setID(this.idGenerator.generateIdentifier());
    response.setVersion(SAMLVersion.VERSION_20);
    response.setInResponseTo(target.inResponseTo());
    response.setIssueInstant(this.clock.instant());
    response.setDestination(target.assertionConsumerServiceUrl());
    response.setIssuer(createIssuer(country));

    final Status status = (Status) XMLObjectSupport.buildXMLObject(Status.DEFAULT_ELEMENT_NAME);
    final StatusCode code = (StatusCode) XMLObjectSupport.buildXMLObject(StatusCode.DEFAULT_ELEMENT_NAME);
    code.setValue(statusCode);
    if (subStatusCode != null) {
      final StatusCode subCode = (StatusCode) XMLObjectSupport.buildXMLObject(StatusCode.DEFAULT_ELEMENT_NAME);
      subCode.setValue(subStatusCode);
      code.setStatusCode(subCode);
    }
    status.setStatusCode(code);
    if (message != null && !message.isBlank()) {
      final StatusMessage statusMessage =
          (StatusMessage) XMLObjectSupport.buildXMLObject(StatusMessage.DEFAULT_ELEMENT_NAME);
      statusMessage.setValue(message);
      status.setStatusMessage(statusMessage);
    }
    response.setStatus(status);
    return response;
  }

  /**
   * Signs a response or an assertion with the country's signing key, using algorithms that both the country and the
   * connector allow.
   *
   * @param object the object to sign
   * @param country the simulated country
   * @param connectorMetadata the connector metadata
   * @throws ResponseCreationException if the object cannot be signed
   */
  private void sign(final @NonNull SignableSAMLObject object, final @NonNull SimulatedCountry country,
      final @NonNull EntityDescriptor connectorMetadata) throws ResponseCreationException {
    try {
      final BasicSignatureSigningConfiguration config = new BasicSignatureSigningConfiguration();
      config.setSignatureAlgorithms(
          List.of(this.algorithmSelector.selectSignatureAlgorithm(country, connectorMetadata)));
      config.setSignatureReferenceDigestMethods(
          List.of(this.algorithmSelector.selectDigestAlgorithm(country, connectorMetadata)));
      config.setSignatureCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
      SAMLObjectSigner.sign(object, new OpenSamlCredential(country.getSigningCredential()), config,
          SecurityConfigurationSupport.getGlobalSignatureSigningConfiguration());
    }
    catch (final IllegalStateException | SignatureException e) {
      log.warn("{}: Failed to sign {} - {}", country.getCode(), object.getElementQName().getLocalPart(),
          e.getMessage());
      throw new ResponseCreationException("Failed to sign the response - " + e.getMessage(), e);
    }
  }

  /**
   * Encrypts an assertion for the connector, using a data encryption algorithm that both the country and the
   * connector allow.
   *
   * @param assertion the assertion
   * @param country the simulated country
   * @param connectorMetadata the connector metadata
   * @return the encrypted assertion
   * @throws ResponseCreationException if the assertion cannot be encrypted
   */
  private @NonNull EncryptedAssertion encrypt(final @NonNull Assertion assertion,
      final @NonNull SimulatedCountry country, final @NonNull EntityDescriptor connectorMetadata)
      throws ResponseCreationException {
    try {
      final BasicEncryptionConfiguration config = new BasicEncryptionConfiguration();
      config.setDataEncryptionAlgorithms(
          List.of(this.algorithmSelector.selectDataEncryptionAlgorithm(country, connectorMetadata)));
      final BasicEncryptionConfiguration defaults =
          (BasicEncryptionConfiguration) SecurityConfigurationSupport.getGlobalEncryptionConfiguration();
      if (defaults != null) {
        config.setKeyTransportEncryptionAlgorithms(defaults.getKeyTransportEncryptionAlgorithms());
        config.setRSAOAEPParameters(defaults.getRSAOAEPParameters());
        config.setKeyAgreementConfigurations(defaults.getKeyAgreementConfigurations());
        config.setKeyTransportAlgorithmPredicate(defaults.getKeyTransportAlgorithmPredicate());
      }
      final EncryptedAssertion encrypted =
          (EncryptedAssertion) XMLObjectSupport.buildXMLObject(EncryptedAssertion.DEFAULT_ELEMENT_NAME);
      encrypted.setEncryptedData(
          this.encrypter.encrypt(assertion, new SAMLObjectEncrypter.Peer(connectorMetadata), config));
      return encrypted;
    }
    catch (final IllegalStateException | EncryptionException e) {
      log.warn("{}: Failed to encrypt assertion - {}", country.getCode(), e.getMessage());
      throw new ResponseCreationException("Failed to encrypt the assertion - " + e.getMessage(), e);
    }
  }

  /**
   * Creates the issuer element of a country.
   *
   * @param country the simulated country
   * @return the issuer
   */
  private static @NonNull Issuer createIssuer(final @NonNull SimulatedCountry country) {
    final Issuer issuer = (Issuer) XMLObjectSupport.buildXMLObject(Issuer.DEFAULT_ELEMENT_NAME);
    issuer.setFormat(NameIDType.ENTITY);
    issuer.setValue(country.getEntityId());
    return issuer;
  }

  /**
   * Assigns the clock. For testing.
   *
   * @param clock the clock
   */
  void setClock(final @NonNull Clock clock) {
    this.clock = clock;
  }

}
