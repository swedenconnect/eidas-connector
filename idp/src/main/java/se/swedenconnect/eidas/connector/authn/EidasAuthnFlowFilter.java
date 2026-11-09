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
package se.swedenconnect.eidas.connector.authn;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import se.swedenconnect.eidas.connector.authn.sp.EidasAuthnRequest;
import se.swedenconnect.spring.authnserver.audit.AuditFlowData;
import se.swedenconnect.spring.authnserver.audit.AuditRequestContext;
import se.swedenconnect.spring.authnserver.audit.AuditStage;
import se.swedenconnect.spring.authnserver.audit.FlowCorrelation;
import se.swedenconnect.spring.authnserver.authentication.provider.redirect.RedirectForAuthenticationToken;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A filter for the pages of the connector that the filter chain of the authentication server does not cover.
 * <ul>
 * <li>The SAML response from the foreign country does not carry the identifier of the authentication in progress. The
 * identifier is taken from the eIDAS authentication request that is saved in the session, and added to the request as
 * the {@value RedirectForAuthenticationToken#AUTHN_ID_PARAMETER} parameter.</li>
 * <li>The events that are published while the request is processed are audited under the correlation ID of the
 * authentication in progress.</li>
 * </ul>
 *
 * @author Martin Lindström
 */
public class EidasAuthnFlowFilter extends OncePerRequestFilter {

  /** Logger instance. */
  private static final Logger log = LoggerFactory.getLogger(EidasAuthnFlowFilter.class);

  /** The authentication provider. */
  private final EidasAuthenticationProvider provider;

  /**
   * Constructor.
   *
   * @param provider the authentication provider
   */
  public EidasAuthnFlowFilter(final EidasAuthenticationProvider provider) {
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
  }

  /** {@inheritDoc} */
  @Override
  protected boolean shouldNotFilter(final HttpServletRequest request) {
    return !request.getRequestURI().startsWith(
        request.getContextPath() + EidasAuthenticationProvider.AUTHN_PATH + "/");
  }

  /** {@inheritDoc} */
  @Override
  protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
      final FilterChain filterChain) throws ServletException, IOException {

    final HttpServletRequest flowRequest = this.addAuthnId(request);

    FlowCorrelation.clear();
    try {
      this.joinFlow(flowRequest);
      filterChain.doFilter(flowRequest, response);
    }
    finally {
      FlowCorrelation.clear();
    }
  }

  /**
   * Adds the identifier of the authentication to the response from the foreign country, if it is not already given.
   *
   * @param request the HTTP servlet request
   * @return the request to use
   */
  private HttpServletRequest addAuthnId(final HttpServletRequest request) {
    if (RedirectForAuthenticationToken.getAuthnId(request) != null
        || !request.getRequestURI().equals(
        request.getContextPath() + EidasAuthenticationController.ASSERTION_CONSUMER_PATH)) {
      return request;
    }
    final HttpSession session = request.getSession(false);
    if (session != null
        && session.getAttribute(EidasAuthenticationProvider.EIDAS_AUTHNREQUEST_SESSION_KEY)
        instanceof final EidasAuthnRequest authnRequest) {
      return new AuthnIdRequest(request, authnRequest.getAuthnId());
    }
    log.debug("Received SAML response, but no authentication request exists in the session");
    return request;
  }

  /**
   * Makes the events of the request part of the flow of the authentication in progress.
   *
   * @param request the HTTP servlet request
   */
  private void joinFlow(final HttpServletRequest request) {
    if (RedirectForAuthenticationToken.getAuthnId(request) == null) {
      return;
    }
    final RedirectForAuthenticationToken token = this.provider.getAuthenticatorRepository().getInputToken(request);
    final AuditFlowData flowData = token != null ? token.getAuthnInputToken().getAuditData() : null;
    if (flowData != null) {
      FlowCorrelation.join(flowData.correlationId());
      AuditRequestContext.get(request)
          .setRequester(flowData.requester())
          .setStage(AuditStage.USER_AUTHENTICATION);
    }
  }

  /**
   * A request that carries the identifier of the authentication.
   */
  private static final class AuthnIdRequest extends HttpServletRequestWrapper {

    /** The parameters of the request. */
    private final Map<String, String[]> parameters;

    /**
     * Constructor.
     *
     * @param request the request
     * @param authnId the identifier of the authentication
     */
    AuthnIdRequest(final HttpServletRequest request, final String authnId) {
      super(request);
      final Map<String, String[]> map = new LinkedHashMap<>(request.getParameterMap());
      map.put(RedirectForAuthenticationToken.AUTHN_ID_PARAMETER, new String[] { authnId });
      this.parameters = Collections.unmodifiableMap(map);
    }

    /** {@inheritDoc} */
    @Override
    public String getParameter(final String name) {
      final String[] values = this.parameters.get(name);
      return values != null && values.length > 0 ? values[0] : null;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String[]> getParameterMap() {
      return this.parameters;
    }

    /** {@inheritDoc} */
    @Override
    public Enumeration<String> getParameterNames() {
      return Collections.enumeration(this.parameters.keySet());
    }

    /** {@inheritDoc} */
    @Override
    public String[] getParameterValues(final String name) {
      return this.parameters.get(name);
    }

  }

}
