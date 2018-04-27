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
package se.elegnamnden.eidas.idp.connector;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;

/**
 * Test helper constants and methods.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class TestHelper {
  
  /** The supported AuthnContext URI:s by the eIDAS connector. */
  public static final List<Principal> SUPPORTED_PRINCIPALS = Arrays.asList(
    new AuthnContextClassRefPrincipal("http://id.elegnamnden.se/loa/1.0/eidas-low"),
    new AuthnContextClassRefPrincipal("http://id.elegnamnden.se/loa/1.0/eidas-sub"),
    new AuthnContextClassRefPrincipal("http://id.elegnamnden.se/loa/1.0/eidas-high"),
    new AuthnContextClassRefPrincipal("http://id.elegnamnden.se/loa/1.0/eidas-nf-low"),
    new AuthnContextClassRefPrincipal("http://id.elegnamnden.se/loa/1.0/eidas-nf-sub"),
    new AuthnContextClassRefPrincipal("http://id.elegnamnden.se/loa/1.0/eidas-nf-high"),
    new AuthnContextClassRefPrincipal("http://id.elegnamnden.se/loa/1.0/eidas-low-sigm"),
    new AuthnContextClassRefPrincipal("http://id.elegnamnden.se/loa/1.0/eidas-sub-sigm"),
    new AuthnContextClassRefPrincipal("http://id.elegnamnden.se/loa/1.0/eidas-high-sigm"),
    new AuthnContextClassRefPrincipal("http://id.elegnamnden.se/loa/1.0/eidas-nf-low-sigm"),
    new AuthnContextClassRefPrincipal("http://id.elegnamnden.se/loa/1.0/eidas-nf-sub-sigm"),
    new AuthnContextClassRefPrincipal("http://id.elegnamnden.se/loa/1.0/eidas-nf-high-sigm"));
  
  public static Map<Principal, Integer> AUTHN_WEIGHT_MAP;
  
  public static final String FLOW_NAME = "authn/External";
  
  static {
    AUTHN_WEIGHT_MAP = new HashMap<>();
    AUTHN_WEIGHT_MAP.put(new AuthnContextClassRefPrincipal("http://id.elegnamnden.se/loa/1.0/eidas-nf-sub"), 1);
  }

  // Hidden
  private TestHelper() {
  }

}
