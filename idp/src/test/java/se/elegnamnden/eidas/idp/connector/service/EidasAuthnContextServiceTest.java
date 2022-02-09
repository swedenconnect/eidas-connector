/*
 * Copyright 2017-2022 Sweden Connect
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
                    
            B().requestedUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
              .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
              .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
              .build(),
            AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, null
        },
        {
          // SP requests non-notified also
          B().requestedUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
          AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, null
        },
        {
          // SP requests only the "don't care" substantial URI - should get that one
          B().requestedUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL))
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
            AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL, null
        },
        {
          // SP requests low or substantial - should get the best match which is substantial.
          B().requestedUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_LOW_NF, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
            AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, null
        },
        {
          // SP requests low or substantial (both don't care and notified) - should get the best match which is substantial-nf.
          B().requestedUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_LOW, AUTHN_CONTEXT_URI_EIDAS_LOW_NF, 
            AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
            AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, null
        },
        {
          // SP requests low or substantial (both don't care) - should get the best match which is substantial.
          B().requestedUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_LOW, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL))
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
            AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL, null
        },
        {
          // SP requests low or substantial (nf), PS declare high (meaning all under but no non-notified).
          // Should result in substantial-nf
          B().requestedUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_LOW_NF, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
            AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, null
        },
        {
          // SP requests low or substantial (nf), PS declare high (meaning all under but no non-notified). PS delivers high.
          // Should result in substantial-nf
          B().requestedUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_LOW_NF, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .deliveredUri(EidasConstants.EIDAS_LOA_HIGH)
            .build(),
            AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, null
        },
        {
          // SP requests low or substantial (nf and don't care), PS declare high (meaning all under but no non-notified). PS delivers high.
          // Should result in substantial-nf
          B().requestedUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_LOW_NF, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF,
            AUTHN_CONTEXT_URI_EIDAS_LOW, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL))
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .deliveredUri(EidasConstants.EIDAS_LOA_HIGH)
            .build(),
            AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, null
        },
        {
          // SP requests low or substantial (nf), PS declare high (meaning all under but no non-notified). PS delivers sub.
          // Should result in substantial-nf
          B().requestedUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_LOW_NF, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .deliveredUri(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
            .build(),
            AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, null
        },
        {
          // SP requests low or substantial (nf), PS declare high (meaning all under but no non-notified). PS delivers low.
          // Should result in low-nf
          B().requestedUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_LOW_NF, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .deliveredUri(EidasConstants.EIDAS_LOA_LOW)
            .build(),
            AUTHN_CONTEXT_URI_EIDAS_LOW_NF, null
        },
        {
          // Unknown URI released by PS
          B().requestedUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .deliveredUri("http://www.unknown.com")
            .build(),
            null, AuthnEventIds.AUTHN_EXCEPTION
        },
        {
          // Too low ranked URI released by PS
          B().requestedUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
            .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
            .deliveredUri(EidasConstants.EIDAS_LOA_LOW)
            .build(),
            null, AuthnEventIds.AUTHN_EXCEPTION
        },
        // To validate reported Iceland bug ...
        {
          B().requestedUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_LOW))
          .proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
          .deliveredUri(EidasConstants.EIDAS_LOA_LOW)
          .build(),
          AUTHN_CONTEXT_URI_EIDAS_LOW, null
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
        this.testInput.getProxyServiceDeclaration(),
        this.testInput.getDeliveredUri());
      
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

  private String runAuthn(List<String> requestedUris, List<String> proxyServiceDeclaration, String deliveredUri) 
      throws ExternalAutenticationErrorCodeException {

    this.simulateAuthnRequest(requestedUris);

    this.service.initializeContext(context);
    this.service.processRequest(context);

    this.service.getSendRequestedAuthnContext(context, proxyServiceDeclaration);

    return this.service.getReturnAuthnContextClassRef(context, deliveredUri);
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

    /**
     * The eIDAS authn context URI:s declared by the Proxy Service in its metadata as assurance certification attributes.
     */
    private List<String> proxyServiceDeclaration;

    /** The AuthnContext URI delivered by the Proxy Service. */
    private String deliveredUri;

  }

}
