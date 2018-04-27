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
package se.elegnamnden.eidas.idp.connector.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import net.shibboleth.idp.authn.AuthnEventIds;
import se.elegnamnden.eidas.mapping.loa.LevelOfAssuranceMappings;
import se.litsec.eidas.opensaml.common.EidasConstants;
import se.litsec.eidas.opensaml.common.EidasLoaEnum;
import se.litsec.opensaml.saml2.core.build.RequestedAuthnContextBuilder;
import se.litsec.shibboleth.idp.authn.ExternalAutenticationErrorCodeException;
import se.litsec.shibboleth.idp.authn.context.AuthnContextClassContext;
import se.litsec.shibboleth.idp.authn.service.impl.AuthnContextServiceImpl;
import se.litsec.swedisheid.opensaml.saml2.authentication.LevelofAssuranceAuthenticationContextURI.LoaEnum;

/**
 * Implementation of {@code EidasAuthnContextService}.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class EidasAuthnContextServiceImpl extends AuthnContextServiceImpl implements EidasAuthnContextService {

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(EidasAuthnContextServiceImpl.class);

  /** Mappings between eIDAs and national (Swedish) eID LoA URI:s. */
  private LevelOfAssuranceMappings loaMappings;

  /** Helper for sorting eIDAS notified URI:s. */
  private static Map<String, Integer> eidasNotifiedRank = new HashMap<>();

  /** Helper for sorting eIDAS non-notified URI:s. */
  private static Map<String, Integer> eidasNonNotifiedRank = new HashMap<>();

  /** Comparator for comparing eIDAS notified URI:s. */
  private static Comparator<String> eidasNotifiedLoaByOrder = (u1, u2) -> {
    Integer r1 = eidasNotifiedRank.get(u1);
    Integer r2 = eidasNotifiedRank.get(u2);
    return r1 != null ? r1.compareTo(r2 != null ? r2 : 0) : (r2 != null ? -1 : 0);
  };

  /** Comparator for comparing eIDAS non-notified URI:s. */
  private static Comparator<String> eidasNonNotifiedLoaByOrder = (u1, u2) -> {
    Integer r1 = eidasNonNotifiedRank.get(u1);
    Integer r2 = eidasNonNotifiedRank.get(u2);
    return r1 != null ? r1.compareTo(r2 != null ? r2 : 0) : (r2 != null ? -1 : 0);
  };

  /** Comparator for sorting Swedish eID AuthnContext URI:s for eIDAS. */
  private static Comparator<String> swedishEidUriComparator = (u1, u2) -> {
    LoaEnum loa1 = LoaEnum.parse(u1);
    LoaEnum loa2 = LoaEnum.parse(u2);
    Integer i1 = 0;
    Integer i2 = 0;
    if (loa1 != null) {
      i1 = loa1.getLevel() + (loa1.isNotified() ? 1 : 0) + (loa1.isSignatureMessageUri() ? 1 : 0);
    }
    if (loa2 != null) {
      i2 = loa2.getLevel() + (loa2.isNotified() ? 1 : 0) + (loa2.isSignatureMessageUri() ? 1 : 0);
    }

    return Integer.compare(i1, i2);
  };

  static {
    eidasNotifiedRank.put(EidasConstants.EIDAS_LOA_HIGH, 3);
    eidasNotifiedRank.put(EidasConstants.EIDAS_LOA_SUBSTANTIAL, 2);
    eidasNotifiedRank.put(EidasConstants.EIDAS_LOA_LOW, 1);

    eidasNonNotifiedRank.put(EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED, 3);
    eidasNonNotifiedRank.put(EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED, 2);
    eidasNonNotifiedRank.put(EidasConstants.EIDAS_LOA_LOW_NON_NOTIFIED, 1);
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getSendAuthnContextClassRefs(ProfileRequestContext<?, ?> context, List<String> assuranceURIs,
      boolean idpSupportsSignMessage) throws ExternalAutenticationErrorCodeException {

    final String logId = this.getLogString(context);
    AuthnContextClassContext authnContextClassContext = this.getAuthnContextClassContext(context);
    authnContextClassContext.setSupportsNonNotifiedConcept(false);

    // Iterate over all assurance-certification URI:s
    // - Delete those not understood
    // - If we run into a non-notified - set nn-flag
    // - Copy all to "possible delivery URI:s"
    //
    // Locate the highest ranked notified-URI - copy all lower ranked notified-URI:s to the "possible delivery URI:s"
    // Locate the highest ranked nn-URI - copy all lower ranked nn-URI:s to the "possible delivery URI:s"
    //
    Set<String> possibleDeliveryUris = new HashSet<>();
    if (assuranceURIs != null) {
      for (String au : assuranceURIs) {
        EidasLoaEnum loa = EidasLoaEnum.parse(au);
        if (loa == null) {
          log.warn("Proxy service declared assurance URI '{}' - this is unknown and will be ignored [{}]", au, logId);
          continue;
        }
        if (possibleDeliveryUris.contains(au)) {
          continue;
        }
        possibleDeliveryUris.add(au);

        if (isNonNotifiedEidasUri(au)) {
          authnContextClassContext.setSupportsNonNotifiedConcept(true);
          if (au.equals(EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED)) {
            possibleDeliveryUris.addAll(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED,
              EidasConstants.EIDAS_LOA_LOW_NON_NOTIFIED));
          }
          else if (au.equals(EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED)) {
            possibleDeliveryUris.add(EidasConstants.EIDAS_LOA_LOW_NON_NOTIFIED);
          }
        }
        else {
          if (au.equals(EidasConstants.EIDAS_LOA_HIGH)) {
            possibleDeliveryUris.addAll(Arrays.asList(EidasConstants.EIDAS_LOA_SUBSTANTIAL, EidasConstants.EIDAS_LOA_LOW));
          }
          else if (au.equals(EidasConstants.EIDAS_LOA_SUBSTANTIAL)) {
            possibleDeliveryUris.add(EidasConstants.EIDAS_LOA_LOW);
          }
        }
      }
    }

    if (possibleDeliveryUris.isEmpty()) {
      log.warn("Foreign Proxy Service IdP did not declare any assurance certification URI:s - assuming {} [{}]",
        EidasConstants.EIDAS_LOA_SUBSTANTIAL, logId);
      possibleDeliveryUris.add(EidasConstants.EIDAS_LOA_SUBSTANTIAL);
    }

    authnContextClassContext.setAuthnContextComparison(
      authnContextClassContext.isSupportsNonNotifiedConcept() ? AuthnContextComparisonTypeEnumeration.EXACT
          : AuthnContextComparisonTypeEnumeration.MINIMUM);

    // Iterate over all URI:s requested by the Swedish SP and remove those that we can't
    // return (based on the possible delivery list).
    //
    for (String spUri : authnContextClassContext.getAuthnContextClassRefs()) {
      String eidasUri = this.loaMappings.toEidasURI(spUri, authnContextClassContext.isSupportsNonNotifiedConcept());
      if (eidasUri == null) {
        continue;
      }
      if (!possibleDeliveryUris.contains(eidasUri)) {
        log.info("Requested AuthnContext URI '{}' is not supported by foreign Proxy Service IdP - will remove [{}]", spUri, logId);
        authnContextClassContext.deleteAuthnContextClassRef(spUri);
      }
    }

    // For each URI in the "possible delivery URI" set, check if we can accept that one based on the
    // URI:s requested by the Swedish SP. Remove the ones not accepted.
    //
    List<String> urisToRequest = possibleDeliveryUris.stream()
      .filter(u -> this.canAccept(u, authnContextClassContext.getAuthnContextClassRefs(), authnContextClassContext
        .isSupportsNonNotifiedConcept()))
      .collect(Collectors.toList());

    if (urisToRequest.isEmpty()) {
      final String msg = "No matching AuthnContext URI:s remain after matching against IdP declared assurance certification";
      log.info("{} - failing [{}]", msg, logId);
      throw new ExternalAutenticationErrorCodeException(AuthnEventIds.REQUEST_UNSUPPORTED, msg);
    }

    if (authnContextClassContext.getAuthnContextComparison().equals(AuthnContextComparisonTypeEnumeration.MINIMUM)) {
      // Sort - weakest first
      urisToRequest.sort(eidasNotifiedLoaByOrder);
    }

    authnContextClassContext.setProxiedAuthnContextClassRefs(urisToRequest);
    log.debug("Will include the following AuthnContextClassRef URI:s in AuthnContext: {} [{}]", urisToRequest, logId);

    return urisToRequest;
  }

  /**
   * Method that tells whether the supplied eIDAS AuthnContext URI will be accepted if it is returned from the foreign
   * Proxy Service.
   * 
   * @param eidasUri
   *          the URI to test
   * @param requestedAuthnContextUris
   *          the Swedish AuthnContext URI:s received in the SP request
   * @param supportsNonNotifiedConcept
   *          tells whether the foreign PS understands the non-notified URI:s
   * @return if the URI is accepted as a valid LoA {@code true} is returned, otherwise {@code false}
   */
  private boolean canAccept(String eidasUri, List<String> requestedAuthnContextUris, boolean supportsNonNotifiedConcept) {
    boolean notified = isNotifiedEidasUri(eidasUri);
    for (String u : requestedAuthnContextUris) {
      String e = this.loaMappings.toEidasURI(u, supportsNonNotifiedConcept);
      if (e == null) {
        continue;
      }
      if (notified) {
        // If the Swedish URI accepts both notified and non-notified or if it requires notified, its a match if
        // it is equal or less than the supplied notified eIDAS URI.
        //
        String base = toEidasBaseUri(e);
        if (eidasNotifiedLoaByOrder.compare(base, eidasUri) <= 0) {
          return true;
        }
      }
      else {
        // A Swedish notified URI can not accept a non-notified eIDAS URI
        if (isNotifiedEidasUri(e)) {
          continue;
        }
        // The Swedish URI says "I accept non-notified" and the eIDAS URI is non-notified.
        // It is a match if the Swedish URI is less or equal to the eIDAS URI.
        //
        if (eidasNonNotifiedLoaByOrder.compare(e, eidasUri) <= 0) {
          return true;
        }
      }

    }
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public RequestedAuthnContext getSendRequestedAuthnContext(ProfileRequestContext<?, ?> context, List<String> assuranceURIs)
      throws ExternalAutenticationErrorCodeException {

    final String logId = this.getLogString(context);
    AuthnContextClassContext authnContextClassContext = this.getAuthnContextClassContext(context);

    List<String> urisToRequest = this.getSendAuthnContextClassRefs(context, assuranceURIs, false);

    if (authnContextClassContext.getAuthnContextComparison().equals(AuthnContextComparisonTypeEnumeration.EXACT)) {
      log.debug("Sending AuthnContextClassRefs {} with exact matching [{}]", urisToRequest, logId);

      return RequestedAuthnContextBuilder.builder()
        .comparison(AuthnContextComparisonTypeEnumeration.EXACT)
        .authnContextClassRefs(urisToRequest)
        .build();
    }
    else {
      log.debug("Sending AuthnContextClassRefs '{}' with minimum matching [{}]", urisToRequest.get(0), logId);

      this.getAuthnContextClassContext(context).setProxiedAuthnContextClassRefs(Arrays.asList(urisToRequest.get(0)));

      return RequestedAuthnContextBuilder.builder()
        .comparison(AuthnContextComparisonTypeEnumeration.MINIMUM)
        .authnContextClassRefs(urisToRequest.get(0))
        .build();
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getReturnAuthnContextClassRef(ProfileRequestContext<?, ?> context, String authnContextUri, boolean displayedSignMessage)
      throws ExternalAutenticationErrorCodeException {

    final String logId = this.getLogString(context);

    AuthnContextClassContext authnContextClassContext = this.getAuthnContextClassContext(context);

    // Make sure we received one of the requested AuthnContextClassRef URI:s.
    //
    if (!this.isIssuedAuthnContextClassRefAccepted(authnContextClassContext, authnContextUri)) {
      final String msg = String.format(
        "AuthnContextClassRef URI received in assertion from IdP (%s) does not match any of the URI:s (implicitly) sent in the AuthnRequest (%s)",
        authnContextUri, authnContextClassContext.getProxiedAuthnContextClassRefs());
      log.info("{} [{}]", msg, logId);
      throw new ExternalAutenticationErrorCodeException(AuthnEventIds.AUTHN_EXCEPTION, msg);
    }

    // OK, that seems OK. Next, we have to find the strongest possible requested URI that is equal to or weaker
    // than what we received. The logic holds for both minimum and exact matching ...
    //

    // Sort all requsted URI:s, and then go through the list and pick the strongest one that matches.
    //
    List<String> sortedRequested = authnContextClassContext.getAuthnContextClassRefs()
      .stream()
      .sorted(swedishEidUriComparator.reversed())
      .collect(Collectors.toList());

    String authnContextUriToReturn = null;
    boolean issuedUriNotified = isNotifiedEidasUri(authnContextUri);

    for (String requestedUri : sortedRequested) {

      boolean requiresNotified = doesSwedishUriRequireNotified(requestedUri);

      if (!issuedUriNotified && requiresNotified) {
        // If the Swedish URI requires notified, we can't use this one if the issued one is
        // for a non-notified scheme.
        continue;
      }

      boolean isSigMessage = this.isSignMessageURI(requestedUri);
      if (!displayedSignMessage && isSigMessage) {
        // If this is a sig message URI, and we did not display a signature message, this
        // URI can not be used.
        continue;
      }

      // Transform the national URI to eIDAS format and check if it is weaker or equal to the
      // ACC we received.
      //
      String eidasUri = this.loaMappings.toEidasURI(requestedUri, authnContextClassContext.isSupportsNonNotifiedConcept());

      // If it is stronger, we can't use this one ...
      if (eidasNotifiedLoaByOrder.compare(toEidasBaseUri(eidasUri), toEidasBaseUri(authnContextUri)) > 0) {
        continue;
      }

      // Else. We have a match ...
      // Now, we want the best match.
      //
      if (isSigMessage) {
        // If this is a sigmessage URI, we have found a perfect match.
        // If not, we may iterate over a sigmessage URI later so we keep trying (the SP
        // may have requested both x and x-sigmessage).
        authnContextUriToReturn = requestedUri;
        break;
      }
      else if (!displayedSignMessage) {
        // If we shouldn't return a sig message URI and we get a match we are done.
        authnContextUriToReturn = requestedUri;
        break;
      }
      else if (authnContextUriToReturn == null) {
        // Otherwise, save the URI. It matches, but we may stumble on a better match (sigmessage).
        authnContextUriToReturn = requestedUri;
      }
    }

    if (authnContextUriToReturn == null) {
      final String msg = String.format(
        "AuthnContextClassRef received from IdP '%s' cannot be mapped against requested URI:s", authnContextUri);
      log.info("{} [{}]", msg, logId);
      throw new ExternalAutenticationErrorCodeException(AuthnEventIds.AUTHN_EXCEPTION, msg);
    }

    return authnContextUriToReturn;
  }

  /**
   * Tells whether an issued AuthnContext URI from the PS is accepted given what was requsted.
   * 
   * @param authnContextClassContext
   *          the context
   * @param authnContextUri
   *          the received AuthnContext URI
   * @return {@code true} if the URI is accepted and {@code false} otherwise
   */
  private boolean isIssuedAuthnContextClassRefAccepted(AuthnContextClassContext authnContextClassContext, String authnContextUri) {

    List<String> requestedUris = authnContextClassContext.getProxiedAuthnContextClassRefs();
    if (requestedUris == null || requestedUris.isEmpty()) {
      // If we did not request anything, we accept what we were given.
      return true;
    }

    if (authnContextClassContext.getAuthnContextComparison().equals(AuthnContextComparisonTypeEnumeration.EXACT)) {
      return requestedUris.contains(authnContextUri);
    }

    // For minimum comparison, there will be only one URI sent.
    String requestedUri = requestedUris.get(0);

    // If we used minimum matching the Proxy Service indicated that it only supports notified schemes.
    // If we then received a non-notified URI, it is an error.
    //
    if (isNonNotifiedEidasUri(authnContextUri)) {
      return false;
    }

    return eidasNotifiedLoaByOrder.compare(requestedUri, authnContextUri) <= 0;
  }

  /**
   * Predicate that tells if the supplied URI is a non notified eIDAS AuthnContext URI.
   * 
   * @param uri
   *          the URI to test
   * @return {@code true} if the URI represents a non-notified scheme and {@code false} otherwise
   */
  private static boolean isNonNotifiedEidasUri(String uri) {
    return EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED.equals(uri)
        || EidasConstants.EIDAS_LOA_LOW_NON_NOTIFIED.equals(uri)
        || EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED.equals(uri);
  }

  /**
   * Predicate that tells if the supplied URI is a notified eIDAS AuthnContext URI.
   * 
   * @param uri
   *          the URI to test
   * @return {@code true} if the URI represents a notified scheme and {@code false} otherwise
   */
  private static boolean isNotifiedEidasUri(String uri) {
    return EidasConstants.EIDAS_LOA_SUBSTANTIAL.equals(uri)
        || EidasConstants.EIDAS_LOA_LOW.equals(uri)
        || EidasConstants.EIDAS_LOA_HIGH.equals(uri);
  }

  /**
   * Predicate that tells whether the supplied URI represents a Swedish AuthnContext URI that expresses a notified eIDAS
   * scheme.
   * 
   * @param uri
   *          the URI to test
   * @return {@code true}/{@code false}
   */
  private static boolean doesSwedishUriRequireNotified(String uri) {
    LoaEnum loaEnum = LoaEnum.parse(uri);
    if (uri == null) {
      return false;
    }
    return loaEnum.isNotified();
  }

  /**
   * Given an eIDAS URI, the method returns its "base" meaning the URI without any potential "non-notified" indication.
   * 
   * @param uri
   *          the URI to convert
   * @return the base URI
   */
  private static String toEidasBaseUri(String uri) {
    if (EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED.equals(uri)) {
      return EidasConstants.EIDAS_LOA_SUBSTANTIAL;
    }
    else if (EidasConstants.EIDAS_LOA_LOW_NON_NOTIFIED.equals(uri)) {
      return EidasConstants.EIDAS_LOA_LOW;
    }
    else if (EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED.equals(uri)) {
      return EidasConstants.EIDAS_LOA_HIGH;
    }
    else {
      return uri;
    }
  }

  /**
   * Assigns a mapper that helps us to go between eIDAS and national (Swedish) eID LoA URI definitions.
   * 
   * @param loaMappings
   *          the mapper
   */
  public void setLoaMappings(LevelOfAssuranceMappings loaMappings) {
    this.loaMappings = loaMappings;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    super.afterPropertiesSet();
    Assert.notNull(this.loaMappings, "Property 'loaMappings' must be assigned");
  }

}
