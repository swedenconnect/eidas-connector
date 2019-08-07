/*
 * Copyright 2017-2019 Sweden Connect
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
package se.elegnamnden.eidas.idp.connector.sp.impl;

import java.util.function.Predicate;

import org.opensaml.saml.common.xml.SAMLConstants;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import se.litsec.eidas.opensaml.xmlsec.RelaxedEidasSecurityConfiguration;
import se.swedenconnect.opensaml.xmlsec.config.SecurityConfiguration;

/**
 * Configuration object for {@link EidasAuthnRequestGeneratorImpl}.
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Data
@Slf4j
public class AuthnRequestGeneratorConfig implements InitializingBean {
  
  /** The default preferred binding to use when sending the request. */
  public static final String DEFAULT_PREFERRED_BINDING = SAMLConstants.SAML2_POST_BINDING_URI;

  /** The preferred binding to use when sending the request. Default is POST. */
  private String preferredBinding;
  
  /** Should the eIDAS SPType extension be included in the request? Default is false. */
  private boolean includeSpType;
  
  /** The security configuration for the eIDAS SP part. */
  private SecurityConfiguration spSecurityConfiguration;
  
  /** Function for checking if a binding is valid. */
  private static Predicate<String> isValidBinding = b -> SAMLConstants.SAML2_POST_BINDING_URI.equals(b)
      || SAMLConstants.SAML2_REDIRECT_BINDING_URI.equals(b);  

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    if (this.spSecurityConfiguration == null) {
      this.spSecurityConfiguration = new RelaxedEidasSecurityConfiguration();
      log.info("SP security configuration has not been assigned - using '{}' as default",
        this.spSecurityConfiguration.getProfileName());
    }
    else {
      log.debug("Using '{}' SP security configuration", this.spSecurityConfiguration.getProfileName());
    }    
    if (this.preferredBinding == null) {
      this.preferredBinding = DEFAULT_PREFERRED_BINDING;
    }
    else {
      Assert.isTrue(isValidBinding.test(this.preferredBinding),
        String.format("Property 'preferredBinding' must be '%s' or '%s'", SAMLConstants.SAML2_POST_BINDING_URI,
          SAMLConstants.SAML2_REDIRECT_BINDING_URI));
    }
  }
  
}
