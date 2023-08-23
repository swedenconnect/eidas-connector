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

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import se.swedenconnect.spring.saml.idp.authentication.provider.external.AbstractAuthenticationController;

/**
 * The authentication controller.
 *
 * @author Martin Lindström
 */
@Controller
public class EidasAuthenticationController extends AbstractAuthenticationController<EidasAuthenticationProvider> {

  public static final String METADATA_PATH = "/metadata/sp";

  public static final String ASSERTION_CONSUMER_PATH = "/extauth/saml2/post";

  /** The authentication provider. */
  private final EidasAuthenticationProvider provider;

  /**
   * Constructor.
   *
   * @param provider the eIDAS authentication provider
   */
  public EidasAuthenticationController(final EidasAuthenticationProvider provider) {
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
  }

  /** {@inheritDoc} */
  @Override
  protected EidasAuthenticationProvider getProvider() {
    return this.provider;
  }

  /**
   * The entry point for the external authentication process.
   *
   * @param request the HTTP servlet request
   * @param response the HTTP servlet response
   * @return a {@link ModelAndView}
   */
  @GetMapping(EidasAuthenticationProvider.AUTHN_PATH)
  public ModelAndView authenticate(final HttpServletRequest request, final HttpServletResponse response) {
    return null;
  }

}
