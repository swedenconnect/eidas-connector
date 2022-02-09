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

import static org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration.EXACT;
import static org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration.MINIMUM;
import static se.litsec.swedisheid.opensaml.saml2.authentication.LevelofAssuranceAuthenticationContextURI.AUTHN_CONTEXT_URI_EIDAS_HIGH;
import static se.litsec.swedisheid.opensaml.saml2.authentication.LevelofAssuranceAuthenticationContextURI.AUTHN_CONTEXT_URI_EIDAS_LOW;
import static se.litsec.swedisheid.opensaml.saml2.authentication.LevelofAssuranceAuthenticationContextURI.AUTHN_CONTEXT_URI_EIDAS_LOW_NF;
import static se.litsec.swedisheid.opensaml.saml2.authentication.LevelofAssuranceAuthenticationContextURI.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL;
import static se.litsec.swedisheid.opensaml.saml2.authentication.LevelofAssuranceAuthenticationContextURI.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import net.shibboleth.idp.authn.AuthnEventIds;
import se.litsec.eidas.opensaml.common.EidasConstants;
import se.litsec.shibboleth.idp.authn.ExternalAutenticationErrorCodeException;
import se.litsec.shibboleth.idp.authn.context.AuthnContextClassContext;

/**
 * Test cases for the
 * {@link EidasAuthnContextService#getSendRequestedAuthnContext(org.opensaml.profile.context.ProfileRequestContext, java.util.List)}
 * logic.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
@RunWith(Parameterized.class)
public class GetSendRequestedAuthnContextTest extends AbstractEidasAuthnContextServiceTest {

  @Parameters(name = "{index}")
  public static Collection<Object[]> data() {
    List<Object[]> tc = new ArrayList<>();

    // 0
    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
          .build(),
        EB().comparison(MINIMUM)
          .authnContextUris(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
          .remainingAuthnContextClassRefs(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
          .build()
    });
    
    // 1
    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
          .build(),
        EB().comparison(MINIMUM)
          .authnContextUris(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
          .remainingAuthnContextClassRefs(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
          .build()
    });
        
    // 2
    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
          .build(),
        EB().comparison(MINIMUM)
          .authnContextUris(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
          .remainingAuthnContextClassRefs(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
          .build()
    });
    
    // 3
    // Tests that the lowest one is sent
    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL,
            AUTHN_CONTEXT_URI_EIDAS_LOW))
          .build(),
        EB().comparison(MINIMUM)
          .authnContextUris(Arrays.asList(EidasConstants.EIDAS_LOA_LOW))
          .remainingAuthnContextClassRefs(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL,
            AUTHN_CONTEXT_URI_EIDAS_LOW))
          .build()
    });

    // 4
    // The most likely case (1)
    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL))
          .build(),
        EB().comparison(MINIMUM)
          .authnContextUris(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
          .remainingAuthnContextClassRefs(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL))
          .build()
    });

    // 5
    // (2)
    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL))
          .build(),
        EB().comparison(MINIMUM)
          .authnContextUris(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
          .remainingAuthnContextClassRefs(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL))
          .build()
    });

    // 6
    // No match
    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_LOW))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL))
          .build(),
        EB().errorCode(AuthnEventIds.REQUEST_UNSUPPORTED).build()
    });

    // 7
    // Too high requested are filtered away
    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_LOW))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL,
            AUTHN_CONTEXT_URI_EIDAS_LOW_NF))
          .build(),
        EB().comparison(MINIMUM)
          .authnContextUris(Arrays.asList(EidasConstants.EIDAS_LOA_LOW))
          .remainingAuthnContextClassRefs(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_LOW_NF))
          .build()
    });

    // 8
    // No match
    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_HIGH))
          .build(),
        EB().errorCode(AuthnEventIds.REQUEST_UNSUPPORTED).build()
    });

    // 9
    // Proxy service supports only notified schemes - declares all supported
    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH, EidasConstants.EIDAS_LOA_SUBSTANTIAL))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_LOW_NF))
          .build(),
        EB().comparison(MINIMUM)
          .authnContextUris(Arrays.asList(EidasConstants.EIDAS_LOA_LOW))
          .remainingAuthnContextClassRefs(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_LOW_NF))
          .build()
    });

    // 10
    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH, EidasConstants.EIDAS_LOA_LOW))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
          .build(),
        EB().comparison(MINIMUM)
          .authnContextUris(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL))
          .remainingAuthnContextClassRefs(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
          .build()
    });

    // 11
    // PS supports both notified and non-notified
    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH, EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
          .build(),
        EB().comparison(EXACT)
          .authnContextUris(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH, EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED,
            EidasConstants.EIDAS_LOA_SUBSTANTIAL, EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED))
          .remainingAuthnContextClassRefs(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL))
          .build()
    });

    // PS supports both notified and non-notified - several notified shouldn't change anything.
    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH, EidasConstants.EIDAS_LOA_LOW,
          EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
          .build(),
        EB().comparison(EXACT)
          .authnContextUris(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH, EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED,
            EidasConstants.EIDAS_LOA_SUBSTANTIAL, EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED))
          .remainingAuthnContextClassRefs(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
          .build()
    });

    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH, EidasConstants.EIDAS_LOA_SUBSTANTIAL,
          EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_LOW, AUTHN_CONTEXT_URI_EIDAS_LOW_NF))
          .build(),
        EB().comparison(EXACT)
          .authnContextUris(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH, EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED,
            EidasConstants.EIDAS_LOA_SUBSTANTIAL, EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED, EidasConstants.EIDAS_LOA_LOW,
            EidasConstants.EIDAS_LOA_LOW_NON_NOTIFIED))
          .remainingAuthnContextClassRefs(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_LOW, AUTHN_CONTEXT_URI_EIDAS_LOW_NF))
          .build()
    });

    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
          .build(),
        EB().errorCode(AuthnEventIds.REQUEST_UNSUPPORTED).build()
    });

    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
          .build(),
        EB().errorCode(AuthnEventIds.REQUEST_UNSUPPORTED).build()
    });

    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL))
          .build(),
        EB().comparison(EXACT)
          .authnContextUris(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED))
          .remainingAuthnContextClassRefs(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL))
          .build()
    });
    
    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL, AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF))
          .build(),
        EB().comparison(EXACT)
          .authnContextUris(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED))
          .remainingAuthnContextClassRefs(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL))
          .build()
    });
    
    tc.add(new Object[] {
        TB().proxyServiceDeclaration(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED))
          .requestedAuthnContextUris(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL))
          .build(),
        EB().comparison(EXACT)
          .authnContextUris(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED))
          .remainingAuthnContextClassRefs(Arrays.asList(AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL))
          .build()
    });
    
    return tc;
  }

  @Parameter(0)
  public TestInput testInput;

  @Parameter(1)
  public ExpectedResult expectedResult;

  @Test
  public void testGetSendRequestedAuthnContext() throws Exception {
    try {
      RequestedAuthnContext requestedAuthnContext = this.executeTest(this.testInput);

      if (this.expectedResult.getErrorCode() != null) {
        Assert.fail(String.format("Expected error '%s' but got '%s'. Input: %s",
          this.expectedResult.getErrorCode(), toString(requestedAuthnContext), this.testInput));
      }

      // Assert comparison method (exact or minimum)
      Assert.assertEquals(String.format("Expected comparison '%s', but got '%s'. Input: %s",
        this.expectedResult.getComparison(), requestedAuthnContext.getComparison(), this.testInput),
        this.expectedResult.getComparison(), requestedAuthnContext.getComparison());

      // Assert that we got the AuthnContext URI:s that we expected
      Assert.assertEquals(String.format("Expected %s, but got %s. Input: %s",
        this.expectedResult.getAuthnContextUris(), toStringList(requestedAuthnContext), this.testInput),
        sort(this.expectedResult.getAuthnContextUris()),
        sort(toStringList(requestedAuthnContext)));

      // Assert that the filtered URI:s from what was passed in is what we expect
      AuthnContextClassContext authnContextClassContext = this.service.getAuthnContextClassContext(this.context);
      Assert.assertEquals(String.format("Expected remaining URIs to be %s, but was %s. Input: %s",
        this.expectedResult.getRemainingAuthnContextClassRefs(), authnContextClassContext.getAuthnContextClassRefs(), this.testInput),
        sort(this.expectedResult.getRemainingAuthnContextClassRefs()),
        sort(authnContextClassContext.getAuthnContextClassRefs()));
    }
    catch (ExternalAutenticationErrorCodeException e) {
      if (this.expectedResult.getErrorCode() == null) {
        Assert.fail(String.format("Got error '%s' (%s). Expected: %s. Input: %s",
          e.getMessage(), e.getActualMessage(), this.expectedResult, this.testInput));
      }
    }
  }

  private RequestedAuthnContext executeTest(TestInput input) throws Exception {
    this.simulateAuthnRequest(input.getRequestedAuthnContextUris());

    this.service.initializeContext(this.context);
    this.service.processRequest(this.context);

    return this.service.getSendRequestedAuthnContext(this.context, input.getProxyServiceDeclaration());
  }

  private static String toString(RequestedAuthnContext rac) {
    return String.format("RequestedAuthnContext: comparison='%s', authnContextClassRefs=%s",
      rac.getComparison(),
      rac.getAuthnContextClassRefs().stream().map(AuthnContextClassRef::getAuthnContextClassRef).collect(Collectors.toList()));
  }

  private static List<String> toStringList(RequestedAuthnContext rac) {
    return rac.getAuthnContextClassRefs().stream().map(AuthnContextClassRef::getAuthnContextClassRef).collect(Collectors.toList());
  }

  private static List<String> sort(List<String> list) {
    List<String> _list = new ArrayList<>(list);
    Collections.sort(_list);
    return _list;
  }

  @Data
  @ToString
  @Builder
  public static class TestInput {
    private List<String> proxyServiceDeclaration;
    private List<String> requestedAuthnContextUris;
  }

  private static TestInput.TestInputBuilder TB() {
    return TestInput.builder();
  }

  @Data
  @ToString
  @Builder
  public static class ExpectedResult {
    private AuthnContextComparisonTypeEnumeration comparison;
    private List<String> authnContextUris;
    private List<String> remainingAuthnContextClassRefs;

    private String errorCode;
  }

  private static ExpectedResult.ExpectedResultBuilder EB() {
    return ExpectedResult.builder();
  }

}
