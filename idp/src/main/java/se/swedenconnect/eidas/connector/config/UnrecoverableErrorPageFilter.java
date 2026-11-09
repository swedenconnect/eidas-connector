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
package se.swedenconnect.eidas.connector.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Makes the IdP error page the answer to every error that cannot be reported back to the Service Provider. Spring
 * Boot's error controller answers a request that does not accept HTML with a JSON body, so for the error dispatch of
 * such an error the request is made to accept HTML only. Other errors are left as they are.
 *
 * @author Martin Lindström
 */
public class UnrecoverableErrorPageFilter implements Filter {

  /** {@inheritDoc} */
  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {
    if (request instanceof final HttpServletRequest httpRequest
        && request.getDispatcherType() == DispatcherType.ERROR
        && UnrecoverableErrorViewResolver.findError(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)) != null) {
      chain.doFilter(new HtmlOnlyRequest(httpRequest), response);
      return;
    }
    chain.doFilter(request, response);
  }

  /**
   * A request that accepts HTML only.
   */
  private static final class HtmlOnlyRequest extends HttpServletRequestWrapper {

    /**
     * Constructor.
     *
     * @param request the request
     */
    HtmlOnlyRequest(final HttpServletRequest request) {
      super(request);
    }

    /** {@inheritDoc} */
    @Override
    public String getHeader(final String name) {
      return HttpHeaders.ACCEPT.equalsIgnoreCase(name) ? MediaType.TEXT_HTML_VALUE : super.getHeader(name);
    }

    /** {@inheritDoc} */
    @Override
    public Enumeration<String> getHeaders(final String name) {
      return HttpHeaders.ACCEPT.equalsIgnoreCase(name)
          ? Collections.enumeration(Collections.singletonList(MediaType.TEXT_HTML_VALUE))
          : super.getHeaders(name);
    }

  }

}
