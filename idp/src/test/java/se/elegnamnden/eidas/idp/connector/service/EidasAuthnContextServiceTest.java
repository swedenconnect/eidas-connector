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


import static se.litsec.swedisheid.opensaml.saml2.authentication.LevelofAssuranceAuthenticationContextURI.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.shibboleth.idp.authn.AuthnEventIds;
import se.litsec.eidas.opensaml.common.EidasConstants;
import se.litsec.shibboleth.idp.authn.ExternalAutenticationErrorCodeException;

/**
 * Test cases for {@code EidasAuthnContextService}.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
@RunWith(Parameterized.class)
public class EidasAuthnContextServiceTest extends AbstractEidasAuthnContextServiceTest {

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {
            // Basic case - Only substantial
                    
            B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
              .signatureService(false)
              .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
              .signMsgDisplayed(false)
              .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
              .build(),
            AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, null
        },
        {
          // SP requests non-notified also
          B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL, AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .signatureService(false)
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
            .signMsgDisplayed(false)
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
          AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, null
        },
        {
          // SP requests only the "don't care" substantial URI - should get that one
          B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL))
            .signatureService(false)
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
            .signMsgDisplayed(false)
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
            AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL, null
        },
        {
          // SP requests low or substantial - should get the best match which is substantial.
          B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_LOW_NF, AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .signatureService(false)
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
            .signMsgDisplayed(false)
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
            AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, null
        },
        {
          // SP requests low or substantial (both don't care and notified) - should get the best match which is substantial-nf.
          B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_LOW, AUTH_CONTEXT_URI_EIDAS_LOW_NF, 
            AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL, AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .signatureService(false)
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
            .signMsgDisplayed(false)
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
            AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, null
        },
        {
          // SP requests low or substantial (both don't care) - should get the best match which is substantial.
          B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_LOW, AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL))
            .signatureService(false)
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
            .signMsgDisplayed(false)
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
            AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL, null
        },
        {
          // SP requests low or substantial (nf), PS declare high (meaning all under but no non-notified).
          // Should result in substantial-nf
          B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_LOW_NF, AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .signatureService(false)
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .signMsgDisplayed(false)
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
            AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, null
        },
        {
          // SP requests low or substantial (nf), PS declare high (meaning all under but no non-notified). PS delivers high.
          // Should result in substantial-nf
          B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_LOW_NF, AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .signatureService(false)
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .signMsgDisplayed(false)
            .deliveredUri(EidasConstants.EIDAS_LOA_HIGH)
            .build(),
            AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, null
        },
        {
          // SP requests low or substantial (nf and don't care), PS declare high (meaning all under but no non-notified). PS delivers high.
          // Should result in substantial-nf
          B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_LOW_NF, AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF,
            AUTH_CONTEXT_URI_EIDAS_LOW, AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL))
            .signatureService(false)
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .signMsgDisplayed(false)
            .deliveredUri(EidasConstants.EIDAS_LOA_HIGH)
            .build(),
            AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, null
        },
        {
          // SP requests low or substantial (nf), PS declare high (meaning all under but no non-notified). PS delivers sub.
          // Should result in substantial-nf
          B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_LOW_NF, AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .signatureService(false)
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .signMsgDisplayed(false)
              .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
            AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, null
        },
        {
          // SP requests low or substantial (nf), PS declare high (meaning all under but no non-notified). PS delivers low.
          // Should result in low-nf
          B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_LOW_NF, AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .signatureService(false)
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .signMsgDisplayed(false)
            .deliveredUri(EidasConstants.EIDAS_LOA_LOW)
            .build(),
            AUTH_CONTEXT_URI_EIDAS_LOW_NF, null
        },
        {
          // Unknown URI released by PS
          B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .deliveredUri("http://www.unknown.com")
            .build(),
            null, AuthnEventIds.AUTHN_EXCEPTION
        },
        {
          // Too low ranked URI released by PS
          B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .deliveredUri(EidasConstants.EIDAS_LOA_LOW)
            .build(),
            null, AuthnEventIds.AUTHN_EXCEPTION
        },
        {
          B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF_SIGMESSAGE))
            .signatureService(true)
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .signMsgDisplayed(false)
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
            null, AuthnEventIds.AUTHN_EXCEPTION
        },
        {
          B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF_SIGMESSAGE, AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .signatureService(true)
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .signMsgDisplayed(false)
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
            AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, null
        },
        {
          B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF_SIGMESSAGE, AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .signatureService(true)
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .signMsgDisplayed(true)
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
            AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF_SIGMESSAGE, null
        },
        {
          B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_SIGMESSAGE, AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .signatureService(true)
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .signMsgDisplayed(true)
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
            AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_SIGMESSAGE, null
        },
        {
          B().requestedUris(Arrays.asList(AUTH_CONTEXT_URI_EIDAS_LOW_SIGMESSAGE, AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .signatureService(true)
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .signMsgDisplayed(true)
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
            AUTH_CONTEXT_URI_EIDAS_LOW_SIGMESSAGE, null
        }
    });
  }

  @Parameter(0)
  public TestParameters testInput;

  @Parameter(1)
  public String expectedAuthnContextUri;

  @Parameter(2)
  public String expectedErrorCode;

  @Test
  public void testAuthentication() throws Exception {

    try {
      String uri = this.runAuthn(
        this.testInput.getRequestedUris(),
        this.testInput.isSignatureService(),
        this.testInput.getProxyServiceDeclaration(),
        this.testInput.getDeliveredUri(),
        this.testInput.isSignMsgDisplayed());
      
      if (this.expectedErrorCode != null) {
        Assert.fail(String.format("Expected ExternalAutenticationErrorCodeException with error code '%s'" +
            ", but received URI '%s'. Test input: %s", this.expectedErrorCode, uri, this.testInput));
      }
      Assert.assertEquals(
        String.format("Expected '%s' but got '%s'. Test input: %s", this.expectedAuthnContextUri, uri, this.testInput),
        this.expectedAuthnContextUri, uri);      
    }
    catch (ExternalAutenticationErrorCodeException e) {
      if (this.expectedErrorCode != null) {
        
      }
      else {
        Assert.fail(String.format("Expected '%s' but error code '%s' (%s). Test input: %s", 
          this.expectedAuthnContextUri, e.getMessage(), e.getActualMessage(), this.testInput));
      }
    }
  }

  private String runAuthn(List<String> requestedUris, boolean isSignatureService, List<String> proxyServiceDeclaration, String deliveredUri,
      boolean displaySignMsg) throws ExternalAutenticationErrorCodeException {

    this.simulateAuthnRequest(requestedUris, isSignatureService);

    this.service.initializeContext(context);
    this.service.processRequest(context);

    this.service.getSendRequestedAuthnContext(context, proxyServiceDeclaration);

    return this.service.getReturnAuthnContextClassRef(context, deliveredUri, displaySignMsg);
  }

  private static TestParameters.TestParametersBuilder B() {
    return TestParameters.builder();
  }
  
  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @ToString
  public static class TestParameters {

    /** The AuthnContext URI:s from the SP AuthnRequest. */
    private List<String> requestedUris;

    /** Is this a signature service? */
    private boolean signatureService;

    /**
     * The eIDAS authn context URI:s declared by the Proxy Service in its metadata as assurance certification attributes.
     */
    private List<String> proxyServiceDeclaration;

    /** Was a signature message displayed. */
    private boolean signMsgDisplayed;

    /** The AuthnContext URI delivered by the Proxy Service. */
    private String deliveredUri;

  }

}
