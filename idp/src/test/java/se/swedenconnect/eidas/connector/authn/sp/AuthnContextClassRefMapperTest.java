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
package se.swedenconnect.eidas.connector.authn.sp;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;

import se.litsec.eidas.opensaml.common.EidasConstants;
import se.swedenconnect.eidas.connector.OpenSamlTestBase;
import se.swedenconnect.opensaml.saml2.core.build.RequestedAuthnContextBuilder;
import se.swedenconnect.opensaml.sweid.saml2.authn.LevelOfAssuranceUris;
import se.swedenconnect.spring.saml.idp.error.Saml2ErrorStatusException;

/**
 * Test cases for AuthnContextClassRefMapper.
 *
 * @author Martin Lindström
 */
public class AuthnContextClassRefMapperTest extends OpenSamlTestBase {

  public enum Comp {
    exact, minimum
  };

  @ParameterizedTest
  @MethodSource("requestedAuthnContextProvider")
  public void testRequestedAuthnContext(final List<String> requested, final List<String> supported,
      final Comp comparison, final List<String> result, final Boolean success) {

    try {
      final RequestedAuthnContext ctx = AuthnContextClassRefMapper.calculateRequestedAuthnContext(supported, requested);
      if (!success) {
        Assertions.fail("Expected failure: requested: %s - supported: %s".formatted(requested, supported));
      }
      if (comparison == Comp.minimum) {
        Assertions.assertEquals(AuthnContextComparisonTypeEnumeration.MINIMUM, ctx.getComparison(),
            "Bad comparison: %s. Expected %s %s - requested: %s - supported: %s".formatted(
                ctx.getComparison(), comparison, result, requested, supported));
      }
      if (comparison == Comp.exact) {
        Assertions.assertEquals(AuthnContextComparisonTypeEnumeration.EXACT, ctx.getComparison(),
            "Bad comparison: %s. Expected %s %s - requested: %s - supported: %s".formatted(
                ctx.getComparison(), comparison, result, requested, supported));
      }
      final List<String> uris = ctx.getAuthnContextClassRefs().stream()
          .map(AuthnContextClassRef::getURI)
          .toList();

      Assertions.assertTrue(uris.containsAll(result) && result.containsAll(uris),
          "Unexpected URIs: %s - Expected: %s - requested: %s - supported: %s".formatted(
              uris, result, requested, supported));

    }
    catch (final Saml2ErrorStatusException e) {
      if (success) {
        Assertions.fail("Expected %s %s - requested: %s - supported: %s".formatted(
            comparison, result, requested, supported));
      }
    }
  }

  @ParameterizedTest
  @MethodSource("requestedAuthnContextProvider")
  public void testCanAuthenticate(final List<String> requested, final List<String> supported,
      final Comp comparison, final List<String> result, final Boolean success) {

    if (success) {
      Assertions.assertTrue(AuthnContextClassRefMapper.canAuthenticate(supported, requested),
          "Expected canAuthenticate = true. requested: %s - supported: %s".formatted(requested, supported));
    }
    else {
      Assertions.assertFalse(AuthnContextClassRefMapper.canAuthenticate(supported, requested),
          "Expected canAuthenticate = false. requested: %s - supported: %s".formatted(requested, supported));
    }
  }

  private static Stream<Arguments> requestedAuthnContextProvider() {
    return Stream.of(
        Arguments.of(
            List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH_NF),
            List.of(EidasConstants.EIDAS_LOA_HIGH),
            Comp.minimum,
            List.of(EidasConstants.EIDAS_LOA_HIGH),
            true),
        Arguments.of(
            List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH),
            List.of(EidasConstants.EIDAS_LOA_HIGH),
            Comp.minimum,
            List.of(EidasConstants.EIDAS_LOA_HIGH),
            true),
        Arguments.of(
            List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH,
                LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH_NF),
            List.of(EidasConstants.EIDAS_LOA_HIGH, EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED2),
            Comp.exact,
            List.of(EidasConstants.EIDAS_LOA_HIGH, EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED),
            true),
        Arguments.of(
            List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH,
                LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH_NF),
            List.of(EidasConstants.EIDAS_LOA_SUBSTANTIAL, EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED),
            null,
            null,
            false),
        Arguments.of(
            List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH,
                LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL),
            List.of(EidasConstants.EIDAS_LOA_HIGH, EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED2),
            Comp.exact,
            List.of(EidasConstants.EIDAS_LOA_HIGH, EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED),
            true),
        Arguments.of(
            List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH,
                LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL),
            List.of(EidasConstants.EIDAS_LOA_HIGH, EidasConstants.EIDAS_LOA_SUBSTANTIAL),
            Comp.minimum,
            List.of(EidasConstants.EIDAS_LOA_SUBSTANTIAL),
            true),
        Arguments.of(
            List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL),
            List.of(EidasConstants.EIDAS_LOA_SUBSTANTIAL, EidasConstants.EIDAS_LOA_LOW),
            Comp.minimum,
            List.of(EidasConstants.EIDAS_LOA_SUBSTANTIAL),
            true));
  }

  @ParameterizedTest
  @MethodSource("returnAuthnContextProvider")
  public void testCalculateReturnAuthnContextUri(final String eidasUri, final List<String> requested,
      final String expected) {
    try {
      final String uri = AuthnContextClassRefMapper.calculateReturnAuthnContextUri(eidasUri, requested);
      Assertions.assertEquals(expected, uri,
          "Expected %s but got %s - eIDAS URI: %s - Requested: %s"
              .formatted(expected, uri, eidasUri, requested));
    }
    catch (Saml2ErrorStatusException e) {
      if (expected != null) {
        Assertions.fail(
            "Expected %s but got exception - eIDAS URI: %s - Requested: %s".formatted(expected, eidasUri, requested));
      }
    }
  }

  private static Stream<Arguments> returnAuthnContextProvider() {
    return Stream.of(
        Arguments.of(
            EidasConstants.EIDAS_LOA_HIGH,
            List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH_NF,
                LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF),
            LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH_NF),
        Arguments.of(
            EidasConstants.EIDAS_LOA_HIGH,
            List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH_NF,
                LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH,
                LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF),
            LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH_NF),
        Arguments.of(
            EidasConstants.EIDAS_LOA_HIGH,
            List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF),
            LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF),
        Arguments.of(
            EidasConstants.EIDAS_LOA_LOW,
            List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF),
            null),
        Arguments.of(
            EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED,
            List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF),
            null),
        Arguments.of(
            EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED,
            List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF,
                LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_LOW_NF),
            null),
        Arguments.of(
            EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED,
            List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF,
                LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL),
            LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL),
        Arguments.of(
            EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED,
            List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF,
                LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_LOW),
            LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_LOW));
  }

  @ParameterizedTest
  @MethodSource("assertReturnedAuthnContextUriProvider")
  public void testAssertReturnedAuthnContextUri(final String eidasUri, final RequestedAuthnContext requestedContext,
      final Boolean result) {

    try {
      AuthnContextClassRefMapper.assertReturnedAuthnContextUri(eidasUri, requestedContext);
      if (!result) {
        Assertions.fail("Expected failure. eIDAS URI: %s. RAC: %s %s".formatted(
            eidasUri, requestedContext.getComparison(), requestedContext.getAuthnContextClassRefs().stream()
                .map(AuthnContextClassRef::getURI).toList()));
      }
    }
    catch (final Saml2ErrorStatusException e) {
      if (result) {
        Assertions.fail("Expected success. eIDAS URI: %s. RAC: %s %s".formatted(
            eidasUri, requestedContext.getComparison(), requestedContext.getAuthnContextClassRefs().stream()
                .map(AuthnContextClassRef::getURI).toList()));
      }
    }
  }

  private static Stream<Arguments> assertReturnedAuthnContextUriProvider() {
    return Stream.of(
        Arguments.of(
            EidasConstants.EIDAS_LOA_HIGH,
            RequestedAuthnContextBuilder.builder()
              .comparison(AuthnContextComparisonTypeEnumeration.MINIMUM)
              .authnContextClassRefs(EidasConstants.EIDAS_LOA_HIGH)
              .build(),
            Boolean.TRUE),
        Arguments.of(
            EidasConstants.EIDAS_LOA_HIGH,
            RequestedAuthnContextBuilder.builder()
              .comparison(AuthnContextComparisonTypeEnumeration.MINIMUM)
              .authnContextClassRefs(EidasConstants.EIDAS_LOA_SUBSTANTIAL)
              .build(),
            Boolean.TRUE),
        Arguments.of(
            EidasConstants.EIDAS_LOA_HIGH,
            RequestedAuthnContextBuilder.builder()
              .comparison(AuthnContextComparisonTypeEnumeration.MINIMUM)
              .authnContextClassRefs(EidasConstants.EIDAS_LOA_LOW)
              .build(),
            Boolean.TRUE),
        Arguments.of(
            EidasConstants.EIDAS_LOA_SUBSTANTIAL,
            RequestedAuthnContextBuilder.builder()
              .comparison(AuthnContextComparisonTypeEnumeration.MINIMUM)
              .authnContextClassRefs(EidasConstants.EIDAS_LOA_HIGH)
              .build(),
            Boolean.FALSE),
        Arguments.of(
            EidasConstants.EIDAS_LOA_HIGH,
            RequestedAuthnContextBuilder.builder()
              .comparison(AuthnContextComparisonTypeEnumeration.EXACT)
              .authnContextClassRefs(EidasConstants.EIDAS_LOA_HIGH, EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED)
              .build(),
            Boolean.TRUE),
        Arguments.of(
            EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED2,
            RequestedAuthnContextBuilder.builder()
              .comparison(AuthnContextComparisonTypeEnumeration.EXACT)
              .authnContextClassRefs(EidasConstants.EIDAS_LOA_SUBSTANTIAL, EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED)
              .build(),
            Boolean.TRUE),
        Arguments.of(
            EidasConstants.EIDAS_LOA_HIGH,
            RequestedAuthnContextBuilder.builder()
              .comparison(AuthnContextComparisonTypeEnumeration.EXACT)
              .authnContextClassRefs(EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED)
              .build(),
            Boolean.FALSE)

        );

  }

}
