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
import java.util.List;
import java.util.Objects;

import org.opensaml.saml.saml2.core.AuthnRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;

import se.swedenconnect.opensaml.saml2.response.ResponseProcessingException;
import se.swedenconnect.opensaml.saml2.response.ResponseProcessingInput;
import se.swedenconnect.opensaml.saml2.response.ResponseProcessingResult;
import se.swedenconnect.opensaml.saml2.response.ResponseProcessor;
import se.swedenconnect.opensaml.saml2.response.ResponseStatusErrorException;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthentication;
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

  /** The processor handling the SAML responses received from the foreign eIDAS proxy services. */
  private ResponseProcessor responseProcessor;

  /** The URL where we receive SAML responses. */
  private final String samlResponseUrl;

  /**
   * Constructor.
   *
   * @param baseUrl the application base URL
   * @param contextPath the application context path
   * @param responseProcessor the processor handling the SAML responses received from the foreign eIDAS proxy services
   */
  public EidasAuthenticationProvider(final String baseUrl, final String contextPath, final ResponseProcessor responseProcessor) {
    super(AUTHN_PATH, RESUME_PATH);
    this.responseProcessor = Objects.requireNonNull(responseProcessor, "responseProcessor must not be null");
    this.samlResponseUrl = String.format("%s%s%s",
        Objects.requireNonNull(baseUrl, "baseUrl must not be null"),
        contextPath == null || "/".equals(contextPath) ? "" : contextPath,
        EidasAuthenticationController.ASSERTION_CONSUMER_PATH);
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
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getEntityCategories() {
    return null;
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
