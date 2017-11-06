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
package se.elegnamnden.eidas.idp.connector.service;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.authn.context.AuthenticationContext;
import se.elegnamnden.eidas.mapping.loa.StaticLevelOfAssuranceMappings;
import se.litsec.eidas.opensaml.common.EidasConstants;
import se.litsec.shibboleth.idp.authn.context.AuthnContextClassContext;
import se.litsec.swedisheid.opensaml.saml2.authentication.LevelofAssuranceAuthenticationContextURI;

/**
 * Test cases for {@code EidasAuthnContextService}.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class EidasAuthnContextServiceTest {

  @Spy
  StaticLevelOfAssuranceMappings mappings;

  @InjectMocks
  EidasAuthnContextServiceImpl service;
  
  ProfileRequestContext<?,?> context;
  AuthenticationContext authenticationContext;

  @SuppressWarnings("rawtypes")
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    
    this.context = new ProfileRequestContext();
    this.authenticationContext = new AuthenticationContext(); 
    this.context.addSubcontext(this.authenticationContext);
  }
  
  @Test
  public void testGetSendAuthnContextClassRefs() throws Exception {
    
    AuthnContextClassContext authnContextClassContext = new AuthnContextClassContext(Arrays.asList(LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL));
    authnContextClassContext.setSupportsNonNotifiedConcept(false);
    this.authenticationContext.addSubcontext(authnContextClassContext);
            
    List<String> uris = service.getSendAuthnContextClassRefs(this.context, Arrays.asList(EidasConstants.EIDAS_LOA_HIGH), false);
    Assert.assertTrue(uris.size() == 1);
    Assert.assertEquals(EidasConstants.EIDAS_LOA_SUBSTANTIAL, uris.get(0));
  }

}
