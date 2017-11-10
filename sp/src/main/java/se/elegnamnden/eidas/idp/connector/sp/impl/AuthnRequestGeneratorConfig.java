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

import java.util.function.Predicate;

import org.opensaml.saml.common.xml.SAMLConstants;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import lombok.Data;

/**
 * Configuration object for {@link EidasAuthnRequestGeneratorImpl}.
 *
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
@Data
public class AuthnRequestGeneratorConfig implements InitializingBean {
  
  /** The default preferred binding to use when sending the request. */
  public static final String DEFAULT_PREFERRED_BINDING = SAMLConstants.SAML2_POST_BINDING_URI;

  /** The preferred binding to use when sending the request. Default is POST. */
  private String preferredBinding;
  
  /** Should the eIDAS SPType extension be included in the request? Default is false. */
  private boolean includeSpType;
  
  /** Function for checking if a binding is valid. */
  private static Predicate<String> isValidBinding = b -> SAMLConstants.SAML2_POST_BINDING_URI.equals(b)
      || SAMLConstants.SAML2_REDIRECT_BINDING_URI.equals(b);  

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
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
