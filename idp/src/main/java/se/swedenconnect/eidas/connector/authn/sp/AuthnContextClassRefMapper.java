/*
 * Copyright 2017-2025 Sweden Connect
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

import lombok.extern.slf4j.Slf4j;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.StatusCode;
import se.swedenconnect.opensaml.eidas.common.EidasConstants;
import se.swedenconnect.opensaml.saml2.core.build.RequestedAuthnContextBuilder;
import se.swedenconnect.opensaml.sweid.saml2.authn.LevelOfAssuranceUris;
import se.swedenconnect.spring.saml.idp.error.Saml2ErrorStatus;
import se.swedenconnect.spring.saml.idp.error.Saml2ErrorStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Handles mappings between eIDAS and Swedish AuthnContextClassRef URI:s.
 *
 * @author Martin Lindström
 */
@Slf4j
public class AuthnContextClassRefMapper {

  /** Special purpose AuthnContext Class Ref for eIDAS test. */
  public static final String EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF = "http://eidas.europa.eu/LoA/test";

  /**
   * Based on the AuthnContextClassRef URI:s supported by a foreign IdP and the AuthnContextClassRef URI:s requsted by a
   * Swedish SP the method returns the {@link RequestedAuthnContext} element.
   *
   * @param supportedEidasUris the AuthnContextClassRef URI:s supported by the foreign IdP
   * @param requestedSwedishUris the AuthnContextClassRef URI:s requsted by the Swedish SP
   * @return a {@link RequestedAuthnContext}
   * @throws Saml2ErrorStatusException if no match is found
   */
  public static RequestedAuthnContext calculateRequestedAuthnContext(
      final List<String> supportedEidasUris, final List<String> requestedSwedishUris)
      throws Saml2ErrorStatusException {

    final SwedishRequestedUris requested = new SwedishRequestedUris(requestedSwedishUris);
    final EidasUris supported = new EidasUris(supportedEidasUris);

    return supported.getRequestedAuthnContext(requested);
  }

  /**
   * Based on the AuthnContextClassRef URI:s supported by a foreign IdP and the AuthnContextClassRef URI:s requsted by a
   * Swedish SP the method tells whether the IdP can authenticate the user according to the SP's requirements.
   *
   * @param supportedEidasUris the AuthnContextClassRef URI:s supported by the foreign IdP
   * @param requestedSwedishUris the AuthnContextClassRef URI:s requsted by the Swedish SP
   * @return {@code true} if authentication can be done and {@code false} otherwise
   */
  public static boolean canAuthenticate(
      final List<String> supportedEidasUris, final List<String> requestedSwedishUris) {

    final SwedishRequestedUris requested = new SwedishRequestedUris(requestedSwedishUris);
    final EidasUris supported = new EidasUris(supportedEidasUris);

    return supported.canAuthenticate(requested);
  }

  /**
   * After the user has authenticated at the foreign IdP we want to translate the authentication context class ref URI
   * from the assertion to a Swedish URI. This method does this for us.
   * <p>
   * It is assumed that {@link #assertReturnedAuthnContextUri(String, RequestedAuthnContext)} has been called.
   * </p>
   *
   * @param eidasUri the eIDAS AuthnContext URI from the assertion
   * @param requestedSwedishUris the requested Swedish URI:s (exact matching)
   * @return the Swedish URI to use in the Swedish assertion
   * @throws Saml2ErrorStatusException for processing errors
   */
  public static String calculateReturnAuthnContextUri(final String eidasUri, final List<String> requestedSwedishUris)
      throws Saml2ErrorStatusException {

    if (EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF.equals(eidasUri)
        && requestedSwedishUris.contains(EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF)) {
      return EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF;
    }

    final SwedishRequestedUris requested = new SwedishRequestedUris(requestedSwedishUris);
    final List<Function<SwedishRequestedUris, String>> mappings =
        switch (eidasUri) {
          case EidasConstants.EIDAS_LOA_HIGH -> List.of(
              SwedishRequestedUris::getHighNotified,
              SwedishRequestedUris::getHighNotifiedAcceptsNn,
              SwedishRequestedUris::getSubstantialNotified,
              SwedishRequestedUris::getSubstantialAcceptsNn,
              SwedishRequestedUris::getLowNotified,
              SwedishRequestedUris::getLowAcceptsNn);
          case EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED, EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED2 -> List.of(
              SwedishRequestedUris::getHighNotifiedAcceptsNn,
              SwedishRequestedUris::getSubstantialAcceptsNn,
              SwedishRequestedUris::getLowAcceptsNn);
          case EidasConstants.EIDAS_LOA_SUBSTANTIAL -> List.of(
              SwedishRequestedUris::getSubstantialNotified,
              SwedishRequestedUris::getSubstantialAcceptsNn,
              SwedishRequestedUris::getLowNotified,
              SwedishRequestedUris::getLowAcceptsNn);
          case EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED, EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED2 ->
              List.of(
                  SwedishRequestedUris::getSubstantialAcceptsNn,
                  SwedishRequestedUris::getLowAcceptsNn);
          case EidasConstants.EIDAS_LOA_LOW -> List.of(
              SwedishRequestedUris::getLowNotified,
              SwedishRequestedUris::getLowAcceptsNn);
          case EidasConstants.EIDAS_LOA_LOW_NON_NOTIFIED, EidasConstants.EIDAS_LOA_LOW_NON_NOTIFIED2 -> List.of(
              SwedishRequestedUris::getLowAcceptsNn);
          case null, default -> Collections.emptyList();
        };

    return mappings.stream()
        .map(f -> f.apply(requested))
        .filter(Objects::nonNull)
        .findFirst()
        .orElseThrow(() -> {
          final String msg = "Could not map %s to any of the requested AuthnContext URI:s %s"
              .formatted(eidasUri, requestedSwedishUris);
          log.error(msg);
          return new Saml2ErrorStatusException(Saml2ErrorStatus.NO_AUTHN_CONTEXT, msg);
        });
  }

  /**
   * Asserts that we received an AuthnContext class ref URI that corresponds what we requested.
   *
   * @param eidasUri the received URI
   * @param requestedContext the {@link RequestedAuthnContext}
   * @throws Saml2ErrorStatusException for errors
   */
  public static void assertReturnedAuthnContextUri(final String eidasUri, final RequestedAuthnContext requestedContext)
      throws Saml2ErrorStatusException {

    if (eidasUri == null) {
      final String msg = "Invalid assertion received from foreign IdP - No AuthnContextClassRef present";
      throw new Saml2ErrorStatusException(StatusCode.RESPONDER, StatusCode.NO_AUTHN_CONTEXT,
          null, msg, msg);
    }

    if (requestedContext.getComparison() == AuthnContextComparisonTypeEnumeration.MINIMUM) {
      final String requested = requestedContext.getAuthnContextClassRefs().getFirst().getURI();
      if (EidasConstants.EIDAS_LOA_HIGH.equals(requested)) {
        if (!EidasConstants.EIDAS_LOA_HIGH.equals(eidasUri)) {
          final String msg = String.format("Unexpected AuthnContextClassRef received - requested minimum %s but got %s",
              requested, eidasUri);
          throw new Saml2ErrorStatusException(StatusCode.RESPONDER, StatusCode.NO_AUTHN_CONTEXT,
              null, msg, msg);
        }
      }
      else if (EidasConstants.EIDAS_LOA_SUBSTANTIAL.equals(requested)) {
        if (!(EidasConstants.EIDAS_LOA_HIGH.equals(eidasUri)
            || EidasConstants.EIDAS_LOA_SUBSTANTIAL.equals(eidasUri))) {
          final String msg = String.format("Unexpected AuthnContextClassRef received - requested minimum %s but got %s",
              requested, eidasUri);
          throw new Saml2ErrorStatusException(StatusCode.RESPONDER, StatusCode.NO_AUTHN_CONTEXT,
              null, msg, msg);
        }
      }
      else if (EidasConstants.EIDAS_LOA_LOW.equals(requested)) {
        final List<String> allowed =
            List.of(EidasConstants.EIDAS_LOA_HIGH, EidasConstants.EIDAS_LOA_SUBSTANTIAL, EidasConstants.EIDAS_LOA_LOW);
        if (!allowed.contains(eidasUri)) {
          final String msg = String.format("Unexpected AuthnContextClassRef received - requested minimum %s but got %s",
              requested, eidasUri);
          throw new Saml2ErrorStatusException(StatusCode.RESPONDER, StatusCode.NO_AUTHN_CONTEXT,
              null, msg, msg);
        }
      }
      else {
        final String msg = "Unexpected RequestedAuthnContext sent - %s".formatted(requested);
        log.error(msg);
        throw new Saml2ErrorStatusException(Saml2ErrorStatus.NO_AUTHN_CONTEXT, msg);
      }
    }
    else {  // exact

      final List<String> requested = new ArrayList<>();

      requestedContext.getAuthnContextClassRefs()
          .stream()
          .map(AuthnContextClassRef::getURI)
          .forEach(c -> {
            requested.add(c);
            if (EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED.equals(c)) {
              requested.add(EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED2);
            }
            else if (EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED2.equals(c)) {
              requested.add(EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED);
            }
            else if (EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED.equals(c)) {
              requested.add(EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED2);
            }
            else if (EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED2.equals(c)) {
              requested.add(EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED);
            }
            else if (EidasConstants.EIDAS_LOA_LOW_NON_NOTIFIED.equals(c)) {
              requested.add(EidasConstants.EIDAS_LOA_LOW_NON_NOTIFIED2);
            }
            else if (EidasConstants.EIDAS_LOA_LOW_NON_NOTIFIED2.equals(c)) {
              requested.add(EidasConstants.EIDAS_LOA_LOW_NON_NOTIFIED);
            }
          });
      if (!requested.contains(eidasUri)) {
        final String msg = String.format("Unexpected AuthnContextClassRef received - requested any of %s but got %s",
            requestedContext.getAuthnContextClassRefs().stream().map(AuthnContextClassRef::getURI).toList(), eidasUri);
        throw new Saml2ErrorStatusException(StatusCode.RESPONDER, StatusCode.NO_AUTHN_CONTEXT,
            null, msg, msg);
      }
    }
  }

  /**
   * Representation of requested Swedish AuthnContextClassRef URI:s.
   * <p>
   * Note: The comparison is always exact for these.
   * </p>
   */
  private static class SwedishRequestedUris {

    /** eIDAS test (for ping). */
    private static final byte TEST = 0b01000000;

    /** High notified. */
    private static final byte HIGH_NF = 0b00100000;

    /** High notified and non-notified. */
    private static final byte HIGH_NF_NN = 0b00010000;

    /** Substantial notified. */
    private static final byte SUB_NF = 0b00001000;

    /** Substantial notified and non-notified. */
    private static final byte SUB_NF_NN = 0b00000100;

    /** Low notified. */
    private static final byte LOW_NF = 0b00000010;

    /** Low notified. */
    private static final byte LOW_NF_NN = 0b00000001;

    /** The representation of requested URI:s. */
    private byte requested = 0b0;

    /**
     * Constructor.
     *
     * @param requestedUris a list of the requested URI:s
     */
    public SwedishRequestedUris(final List<String> requestedUris) {
      if (requestedUris.contains(EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF)) {
        this.requested = TEST;
      }
      else if (requestedUris.isEmpty()) {
        // Nothing specified - allow all
        this.requested = HIGH_NF | HIGH_NF_NN | SUB_NF | SUB_NF_NN | LOW_NF | LOW_NF_NN;
      }
      else {
        for (final String uri : requestedUris) {
          this.requested |= switch (uri) {
            case LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH_NF -> HIGH_NF;
            case LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH -> HIGH_NF_NN;
            case LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF -> SUB_NF;
            case LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL -> SUB_NF_NN;
            case LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_LOW_NF -> LOW_NF;
            case LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_LOW -> LOW_NF_NN;
            default -> 0b0;
          };
        }
      }
    }

    /**
     * Predicate telling whether the test URI is specified
     *
     * @return whether the test URI is specified
     */
    public boolean isTestUri() {
      return this.test(TEST);
    }

    /**
     * Predicate telling whether high notified is requested.
     *
     * @return whether high notified is requested
     */
    public boolean highNotified() {
      return this.test(HIGH_NF);
    }

    /**
     * Gets the URI for high notified if set, else {@code null}
     *
     * @return the URI for high notified if set, else {@code null}
     */
    public String getHighNotified() {
      return this.highNotified() ? LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH_NF : null;
    }

    /**
     * Predicate telling whether high notified or non-notified is requested
     *
     * @return whether high notified or non-notified is requested
     */
    public boolean highAcceptsNn() {
      return this.test(HIGH_NF_NN);
    }

    /**
     * Gets the URI for high notified accepting non-notified if set, else {@code null}
     *
     * @return the URI for high notified accepting non-notified if set, else {@code null}
     */
    public String getHighNotifiedAcceptsNn() {
      return this.highAcceptsNn() ? LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH : null;
    }

    /**
     * Predicate telling whether substantial notified is requested.
     *
     * @return whether substantial notified is requested
     */
    public boolean substantialNotified() {
      return this.test(SUB_NF);
    }

    /**
     * Gets the URI for substantial notified if set, else {@code null}
     *
     * @return the URI for substantial notified if set, else {@code null}
     */
    public String getSubstantialNotified() {
      return this.substantialNotified() ? LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF : null;
    }

    /**
     * Predicate telling whether substantial notified or non-notified is requested
     *
     * @return whether substantial notified or non-notified is requested
     */
    public boolean substantialAcceptsNn() {
      return this.test(SUB_NF_NN);
    }

    /**
     * Gets the URI for substantial notified accepting non-notified if set, else {@code null}
     *
     * @return the URI for substantial notified accepting non-notified if set, else {@code null}
     */
    public String getSubstantialAcceptsNn() {
      return this.substantialAcceptsNn() ? LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL : null;
    }

    /**
     * Predicate telling whether low notified is requested.
     *
     * @return whether low notified is requested
     */
    public boolean lowNotified() {
      return this.test(LOW_NF);
    }

    /**
     * Gets the URI for low notified if set, else {@code null}
     *
     * @return the URI for low notified if set, else {@code null}
     */
    public String getLowNotified() {
      return this.lowNotified() ? LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_LOW_NF : null;
    }

    /**
     * Predicate telling whether low notified or non-notified is requested
     *
     * @return whether low notified or non-notified is requested
     */
    public boolean lowAcceptsNn() {
      return this.test(LOW_NF_NN);
    }

    /**
     * Gets the URI for low notified accepting non-notified if set, else {@code null}
     *
     * @return the URI for low notified accepting non-notified if set, else {@code null}
     */
    public String getLowAcceptsNn() {
      return this.lowAcceptsNn() ? LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_LOW : null;
    }

    private boolean test(final int value) {
      return (this.requested & value) == value;
    }

  }

  /**
   * Helper class for handling eIDAS URI:s
   *
   * @author Martin Lindström
   */
  private static class EidasUris {

    /** High notified. */
    private static final byte EIDAS_HIGH = 0b00100000;

    /** Substantial notified. */
    private static final byte EIDAS_SUB = 0b00010000;

    /** Low notified. */
    private static final byte EIDAS_LOW = 0b00001000;

    /** High non-notified. */
    private static final byte EIDAS_HIGH_NN = 0b00000100;

    /** Substantial non-notified. */
    private static final byte EIDAS_SUB_NN = 0b00000010;

    /** Low non-notified. */
    private static final byte EIDAS_LOW_NN = 0b00000001;

    /** For checking if any non-notified are set. */
    private static final byte EIDAS_NN = 0b00000111;

    /** Represents the supported URI:s. */
    private byte supported = 0b0;

    /** An array of all URI:s and their byte values. */
    private static final Uri[] uris = {
        new Uri(EidasConstants.EIDAS_LOA_HIGH, EIDAS_HIGH),
        new Uri(EidasConstants.EIDAS_LOA_SUBSTANTIAL, EIDAS_SUB),
        new Uri(EidasConstants.EIDAS_LOA_LOW, EIDAS_LOW),
        new Uri(EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED, EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED2, EIDAS_HIGH_NN),
        new Uri(EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED, EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED2,
            EIDAS_SUB_NN),
        new Uri(EidasConstants.EIDAS_LOA_LOW_NON_NOTIFIED, EidasConstants.EIDAS_LOA_LOW_NON_NOTIFIED2, EIDAS_LOW_NN)
    };

    /**
     * Constructor.
     *
     * @param supportedUris a list of the supported AuthnContextClassRef URI:s
     */
    public EidasUris(final List<String> supportedUris) {

      for (final String uri : supportedUris) {
        this.supported |= switch (uri) {
          case EidasConstants.EIDAS_LOA_HIGH -> EIDAS_HIGH;
          case EidasConstants.EIDAS_LOA_SUBSTANTIAL -> EIDAS_SUB;
          case EidasConstants.EIDAS_LOA_LOW -> EIDAS_LOW;
          case EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED, EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED2 -> EIDAS_HIGH_NN;
          case EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED, EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED2 ->
              EIDAS_SUB_NN;
          case EidasConstants.EIDAS_LOA_LOW_NON_NOTIFIED, EidasConstants.EIDAS_LOA_LOW_NON_NOTIFIED2 -> EIDAS_LOW_NN;
          default -> 0b0;
        };
      }
    }

    /**
     * Predicate that tells whether the supplied URI:s match the supported, i.e., whether an authentication is
     * possible.
     *
     * @param requested the requested Swedish URI:s
     * @return {@code true} if authentication is possible and {@code false} otherwise
     */
    public boolean canAuthenticate(final SwedishRequestedUris requested) {
      return this.getRequestedAuthnContextMatch(requested).value() != 0;
    }

    /**
     * Based on the requested Swedish AuthnContectClassRef URI:s the method calculates which eIDAS URI:s that should be
     * included as a {@link RequestedAuthnContext} element in the request.
     *
     * @param requested the requested Swedish URI:s
     * @return {@link RequestedAuthnContext}
     * @throws Saml2ErrorStatusException if no match is found
     */
    public RequestedAuthnContext getRequestedAuthnContext(final SwedishRequestedUris requested)
        throws Saml2ErrorStatusException {

      final MatchResult result = this.getRequestedAuthnContextMatch(requested);
      if (result.value() == 0) {
        // We should never end up here since a country should not be selectable if canAuthenticate
        // returns false. But we need to handle this ...
        //
        throw new Saml2ErrorStatusException(Saml2ErrorStatus.NO_AUTHN_CONTEXT);
      }

      if ((result.value() & SwedishRequestedUris.TEST) == SwedishRequestedUris.TEST) {
        return RequestedAuthnContextBuilder.builder()
            .comparison(AuthnContextComparisonTypeEnumeration.EXACT)
            .authnContextClassRefs(AuthnContextClassRefMapper.EIDAS_TEST_AUTHN_CONTEXT_CLASS_REF)
            .build();
      }
      if (result.comparison() == AuthnContextComparisonTypeEnumeration.EXACT) {
        return RequestedAuthnContextBuilder.builder()
            .comparison(AuthnContextComparisonTypeEnumeration.EXACT)
            .authnContextClassRefs(Arrays.stream(uris)
                .filter(u -> (result.value() & u.value()) == u.value())
                .map(Uri::uri)
                .toList())
            .build();
      }
      else {
        final String uri = (result.value() & EIDAS_LOW) == EIDAS_LOW
            ? EidasConstants.EIDAS_LOA_LOW
            : (result.value() & EIDAS_SUB) == EIDAS_SUB
                ? EidasConstants.EIDAS_LOA_SUBSTANTIAL
                : EidasConstants.EIDAS_LOA_HIGH;

        return RequestedAuthnContextBuilder.builder()
            .comparison(AuthnContextComparisonTypeEnumeration.MINIMUM)
            .authnContextClassRefs(uri)
            .build();
      }
    }

    private MatchResult getRequestedAuthnContextMatch(final SwedishRequestedUris requested) {

      // Represents the minimum matching URI:s. Only the lowest will be used if no non-notified are present.
      byte minimumMatching = 0b0;

      // Represents the exact matching URI:s. Used if non-notified are present.
      byte exactMatching = 0b0;

      if (requested.isTestUri()) {
        return new MatchResult(AuthnContextComparisonTypeEnumeration.EXACT, SwedishRequestedUris.TEST);
      }

      if (requested.highNotified()) {
        minimumMatching |= (byte) (EIDAS_HIGH & this.supported);
      }
      if (requested.highAcceptsNn()) {
        exactMatching |= (byte) ((EIDAS_HIGH_NN & this.supported) | (EIDAS_HIGH & this.supported));
        minimumMatching |= (byte) (EIDAS_HIGH & this.supported);
      }
      if (requested.substantialNotified()) {
        minimumMatching |= (byte) (EIDAS_SUB & this.supported);
      }
      if (requested.substantialAcceptsNn()) {
        exactMatching |= (byte) ((EIDAS_HIGH & this.supported)
            | (EIDAS_HIGH_NN & this.supported)
            | (EIDAS_SUB & this.supported)
            | (EIDAS_SUB_NN & this.supported));
        minimumMatching |= (byte) (EIDAS_SUB & this.supported);
      }
      if (requested.lowNotified()) {
        minimumMatching |= (byte) (EIDAS_LOW & this.supported);
      }
      if (requested.lowAcceptsNn()) {
        exactMatching |= (byte) ((EIDAS_HIGH & this.supported)
            | (EIDAS_HIGH_NN & this.supported)
            | (EIDAS_SUB & this.supported)
            | (EIDAS_SUB_NN & this.supported)
            | (EIDAS_LOW & this.supported)
            | (EIDAS_LOW_NN & this.supported));
        minimumMatching |= (byte) (EIDAS_LOW & this.supported);
      }

      if ((exactMatching & EIDAS_NN) != 0) {
        return new MatchResult(AuthnContextComparisonTypeEnumeration.EXACT, exactMatching);
      }
      else {
        return new MatchResult(AuthnContextComparisonTypeEnumeration.MINIMUM, minimumMatching);
      }
    }

    // Represents a mapping between URI:s and their byte values
    private record Uri(String uri, String additionalUri, byte value) {
      public Uri(final String uri, final byte value) {
        this(uri, null, value);
      }
    }

    // Internal for representing a match result between requested and supported URI:s
    private record MatchResult(AuthnContextComparisonTypeEnumeration comparison, byte value) {
    }

  }

}
