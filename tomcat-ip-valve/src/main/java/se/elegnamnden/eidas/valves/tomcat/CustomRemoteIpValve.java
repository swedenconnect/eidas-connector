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
package se.elegnamnden.eidas.valves.tomcat;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.AccessLog;
import org.apache.catalina.Globals;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.http.MimeHeaders;

/**
 * A customized remote IP valve that checks for a shared secret instead of matching internal proxies against
 * regular expressions.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class CustomRemoteIpValve extends RemoteIpValve {

  /** Logger. */
  private static final Log log = LogFactory.getLog(CustomRemoteIpValve.class);

  /**
   * This valve does not use the internalProxies setting from the {@link RemoteIpValve}. Instead we make use of the
   * shared secret. In order to use the implementation from RemoteIpValve we simply use a "match anything regex".
   */
  private static final String ANY_INTERNAL_PROXY = ".*";

  /** Default header name for the header that holds the shared secret value. */
  private static final String SHARED_SECRET_HEADER_DEFAULT = "X-Proxy-Authenticate";

  /** The shared secret header name. */
  private String sharedSecretHeader = SHARED_SECRET_HEADER_DEFAULT;

  /** The shared secret that replaces the internalProxies value when checking if the valve should do its work. */
  private String sharedSecret;

  /**
   * Constructor.
   */
  public CustomRemoteIpValve() {
    this.setInternalProxies(ANY_INTERNAL_PROXY);
  }

  /**
   * Returns the name for the shared secret header.
   * 
   * @return shared secret header name
   */
  public String getSharedSecretHeader() {
    return this.sharedSecretHeader;
  }

  /**
   * Assigns the name for the shared secret header.
   * 
   * @param sharedSecretHeader
   *          shared secret header name
   */
  public void setSharedSecretHeader(String sharedSecretHeader) {
    this.sharedSecretHeader = sharedSecretHeader;
  }

  /**
   * Assigns the shared secret that we use to match requests instead of matching internal proxies.
   * 
   * @param sharedSecret
   *          the shared secret
   */
  public void setSharedSecret(String sharedSecret) {
    if (sharedSecret == null || sharedSecret.trim().isEmpty()) {
      throw new IllegalArgumentException("CustomRemoteIpValve shared secret must not be null or empty");
    }
    this.sharedSecret = sharedSecret;
  }

  /**
   * Replaces that check if the internalProxes regex matches the remote address with a check that looks for a shared
   * secret and compares that with the configured secret for the valve.
   */
  @Override
  public void invoke(Request request, Response response) throws IOException, ServletException {

    final String originalScheme = request.getScheme();
    final String requestSharedSecret = request.getHeader(this.sharedSecretHeader);

    if ("https".equals(originalScheme) && requestSharedSecret != null && requestSharedSecret.equals(this.sharedSecret)) { 
      if (log.isDebugEnabled()) {
        log.debug(String.format("CustomRemoteIpValve found matching shared secret in %s header, proceeding to RemoteIpValve processing ...",
          this.sharedSecretHeader));
      }
      super.invoke(request, response);
    }
    else {      
      if (log.isDebugEnabled()) {
        log.debug(String.format("Skip RemoteIpValve for request %s with originalRemoteAddr '%s'", request.getRequestURI(), request
          .getRemoteAddr()));
      }
      if (this.getRequestAttributesEnabled()) {
        request.setAttribute(AccessLog.REMOTE_ADDR_ATTRIBUTE, request.getRemoteAddr());
        request.setAttribute(Globals.REMOTE_ADDR_ATTRIBUTE, request.getRemoteAddr());
        request.setAttribute(AccessLog.REMOTE_HOST_ATTRIBUTE, request.getRemoteHost());
        request.setAttribute(AccessLog.PROTOCOL_ATTRIBUTE, request.getProtocol());
        request.setAttribute(AccessLog.SERVER_PORT_ATTRIBUTE, Integer.valueOf(request.getServerPort()));
      }

      final String originalRemoteAddr = request.getRemoteAddr();
      final String originalRemoteHost = request.getRemoteHost();
      final boolean originalSecure = request.isSecure();
      final int originalServerPort = request.getServerPort();
      final String originalProxiesHeader = request.getHeader(this.getProxiesHeader());
      final String originalRemoteIpHeader = request.getHeader(this.getRemoteIpHeader());

      try {
        getNext().invoke(request, response);
      }
      finally {
        request.setRemoteAddr(originalRemoteAddr);
        request.setRemoteHost(originalRemoteHost);
        request.setSecure(originalSecure);

        MimeHeaders headers = request.getCoyoteRequest().getMimeHeaders();
        request.getCoyoteRequest().scheme().setString(originalScheme);

        request.setServerPort(originalServerPort);

        if (originalProxiesHeader == null || originalProxiesHeader.length() == 0) {
          headers.removeHeader(this.getProxiesHeader());
        }
        else {
          headers.setValue(this.getProxiesHeader()).setString(originalProxiesHeader);
        }

        if (originalRemoteIpHeader == null || originalRemoteIpHeader.length() == 0) {
          headers.removeHeader(this.getRemoteIpHeader());
        }
        else {
          headers.setValue(this.getRemoteIpHeader()).setString(originalRemoteIpHeader);
        }
      }
    }
  }

}
