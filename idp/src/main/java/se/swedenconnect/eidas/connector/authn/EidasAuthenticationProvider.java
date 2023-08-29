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
package se.swedenconnect.eidas.connector.authn;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.opensaml.saml.saml2.core.AuthnRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;

import se.swedenconnect.eidas.connector.authn.metadata.EuMetadataProvider;
import se.swedenconnect.opensaml.saml2.response.ResponseProcessingException;
import se.swedenconnect.opensaml.saml2.response.ResponseProcessingInput;
import se.swedenconnect.opensaml.saml2.response.ResponseProcessingResult;
import se.swedenconnect.opensaml.saml2.response.ResponseProcessor;
import se.swedenconnect.opensaml.saml2.response.ResponseStatusErrorException;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthentication;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;
import se.swedenconnect.spring.saml.idp.authentication.provider.external.AbstractUserRedirectAuthenticationProvider;
import se.swedenconnect.spring.saml.idp.authentication.provider.external.ResumedAuthenticationToken;
import se.swedenconnect.spring.saml.idp.error.Saml2ErrorStatusException;
import se.swedenconnect.spring.saml.idp.error.UnrecoverableSaml2IdpError;
import se.swedenconnect.spring.saml.idp.error.UnrecoverableSaml2IdpException;

/**
 * The {@link AuthenticationProvider} handling the authentication of the user against the foreign eIDAS countries.
 *
 * @author Martin Lindström
 */
public class EidasAuthenticationProvider extends AbstractUserRedirectAuthenticationProvider {

  /** The authentication path, i.e., where the SAML engine should direct the user for authentication. */
  public static final String AUTHN_PATH = "/extauth";

  /** The path where we redirect the user to leave the control back to the SAML engine. */
  public static final String RESUME_PATH = "/resume";

  /** Special purpose AuthnContext Class Ref for eIDAS test. */
  public static final String EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF = "http://eidas.europa.eu/LoA/test";

  /** The processor handling the SAML responses received from the foreign eIDAS proxy services. */
  private ResponseProcessor responseProcessor;

  /** The URL where we receive SAML responses. */
  private final String samlResponseUrl;

  /** The metadata provider. */
  private final EuMetadataProvider metadataProvider;

  /** Supported LoA URI:s. */
  private final List<String> supportedLoas;

  /** The entity categories. */
  private final List<String> entityCategories;

  /** Whitelisted SP:s that are allowed to send "ping" requests. */
  private final List<String> pingWhitelist;

  /**
   * Constructor.
   *
   * @param baseUrl the application base URL
   * @param contextPath the application context path
   * @param responseProcessor the processor handling the SAML responses received from the foreign eIDAS proxy services
   * @param metadataProvider the EU metadata provider
   * @param supportedLoas supported LoA URI:s
   * @param entityCategories the entity categories
   * @param pingWhitelist the whitelisted SP:s that are allowed to send ping requests
   */
  public EidasAuthenticationProvider(final String baseUrl, final String contextPath,
      final ResponseProcessor responseProcessor, final EuMetadataProvider metadataProvider,
      final List<String> supportedLoas, final List<String> entityCategories,
      final List<String> pingWhitelist) {
    super(AUTHN_PATH, RESUME_PATH);

    this.responseProcessor = Objects.requireNonNull(responseProcessor, "responseProcessor must not be null");
    this.samlResponseUrl = String.format("%s%s%s",
        Objects.requireNonNull(baseUrl, "baseUrl must not be null"),
        contextPath == null || "/".equals(contextPath) ? "" : contextPath,
        EidasAuthenticationController.ASSERTION_CONSUMER_PATH);
    this.metadataProvider = Objects.requireNonNull(metadataProvider, "metadataProvider must not be null");
    this.supportedLoas = Collections.unmodifiableList(
        Objects.requireNonNull(supportedLoas, "supportedLoas must not be null"));
    this.entityCategories = Collections.unmodifiableList(
        Objects.requireNonNull(entityCategories, "entityCategories must not be null"));
    this.pingWhitelist = Optional.ofNullable(pingWhitelist).orElseGet(() -> Collections.emptyList());

    // "http://eidas.europa.eu/LoA/test"

  }

  /** {@inheritDoc} */
  @Override
  public Saml2UserAuthentication resumeAuthentication(final ResumedAuthenticationToken token)
      throws Saml2ErrorStatusException {

    final EidasAuthenticationToken proxyResponse = EidasAuthenticationToken.class.cast(token.getAuthnToken());

    try {
      final ResponseProcessingResult result =
          this.responseProcessor.processSamlResponse(proxyResponse.getResponse(), proxyResponse.getRelayState(),
              this.buildResponseProcessingInput(token), null);
    }
    catch (final ResponseStatusErrorException e) {
    }
    catch (ResponseProcessingException e) {
    }

    return null;
  }

  /**
   * Supports {@link EidasAuthenticationToken}.
   */
  @Override
  public boolean supportsUserAuthenticationToken(final Authentication authentication) {
    return EidasAuthenticationToken.class.isInstance(authentication);
  }

  /** {@inheritDoc} */
  @Override
  public String getName() {
    return "eidas-authn";
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getSupportedAuthnContextUris() {
    return this.supportedLoas;
  }

  /**
   * Special handling since we also may support the special URI "http://eidas.europa.eu/LoA/test".
   */
  @Override
  protected List<String> filterRequestedAuthnContextUris(final Saml2UserAuthenticationInputToken token) {
    final List<String> filtered = super.filterRequestedAuthnContextUris(token);
    if (this.pingWhitelist.isEmpty() || !this.pingWhitelist.contains(token.getAuthnRequestToken().getEntityId())) {
      return filtered;
    }
    if (token.getAuthnRequirements().getAuthnContextRequirements().contains(EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF)) {
      return List.of(EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF);
    }
    else {
      return filtered;
    }
  }

  /**
   * Tells wheter this is a eIDAS ping request.
   * 
   * @param token the input token
   * @return {@code true} if it is a ping request and {@code false} otherwise
   */
  public boolean isPingRequest(final Saml2UserAuthenticationInputToken token) {
    return token.getAuthnRequirements().getAuthnContextRequirements().contains(EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF);
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getEntityCategories() {
    return this.entityCategories;
  }

  /**
   * Builds the input for processing a SAML response.
   *
   * @param token the current {@link Authentication} token
   * @return a {@link ResponseProcessingInput}
   */
  private ResponseProcessingInput buildResponseProcessingInput(final ResumedAuthenticationToken token) {
    return new ResponseProcessingInput() {

      /** {@inheritDoc} */
      @Override
      public AuthnRequest getAuthnRequest(final String id) {
        final AuthnRequest authnRequest = EidasAuthenticationToken.class.cast(token.getAuthnToken()).getAuthnRequest();
        if (id != null && !Objects.equals(id, authnRequest.getID())) {
          throw new UnrecoverableSaml2IdpException(UnrecoverableSaml2IdpError.INVALID_SESSION, token);
        }
        return authnRequest;
      }

      /** {@inheritDoc} */
      @Override
      public String getRequestRelayState(final String id) {
        return token.getAuthnInputToken().getAuthnRequestToken().getRelayState();
      }

      /** {@inheritDoc} */
      @Override
      public String getReceiveURL() {
        return samlResponseUrl;
      }

      /** {@inheritDoc} */
      @Override
      public Instant getReceiveInstant() {
        return Instant.now();
      }

      /** {@inheritDoc} */
      @Override
      public String getClientIpAddress() {
        return token.getServletRequest().getRemoteAddr();
      }

      /** {@inheritDoc} */
      @Override
      public X509Certificate getClientCertificate() {
        return null;
      }

    };
  }

}
