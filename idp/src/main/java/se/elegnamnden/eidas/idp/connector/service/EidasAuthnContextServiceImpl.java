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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import net.shibboleth.idp.authn.AuthnEventIds;
import se.elegnamnden.eidas.mapping.loa.LevelOfAssuranceMappings;
import se.litsec.eidas.opensaml.common.EidasLoaEnum;
import se.litsec.shibboleth.idp.authn.ExternalAutenticationErrorCodeException;
import se.litsec.shibboleth.idp.authn.context.AuthnContextClassContext;
import se.litsec.shibboleth.idp.authn.service.impl.ProxyIdpAuthnContextServiceImpl;
import se.litsec.swedisheid.opensaml.saml2.authentication.LevelofAssuranceAuthenticationContextURI.LoaEnum;

/**
 * Implementation of {@code EidasAuthnContextService}.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class EidasAuthnContextServiceImpl extends ProxyIdpAuthnContextServiceImpl implements EidasAuthnContextService {

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(EidasAuthnContextServiceImpl.class);

  /** Mappings between eIDAs and national (Swedish) eID LoA URI:s. */
  private LevelOfAssuranceMappings loaMappings;

  /** Comparator for comparing eIDAS URI:s. */
  private static Comparator<EidasLoaEnum> eidasLoAbyOrder = (u1, u2) -> Integer.compare(u1.getOrder(), u2.getOrder());


  /** {@inheritDoc} */
  @Override
  public void setSupportsNonNotifiedConcept(ProfileRequestContext<?, ?> context, boolean supportsNonNotifiedConcept) {
    try {
      this.getAuthnContextClassContext(context).setSupportsNonNotifiedConcept(supportsNonNotifiedConcept);
    }
    catch (ExternalAutenticationErrorCodeException e) {
      // Will fail later.
      log.error("No AuthnContextClassContext available");
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getSendAuthnContextClassRef(ProfileRequestContext<?, ?> context, List<String> assuranceURIs)
      throws ExternalAutenticationErrorCodeException {
    List<String> uris = this.getSendAuthnContextClassRefs(context, assuranceURIs, false);

    // eIDAS used "minimum" comparison rules, so we only send one URI in the request. The lowest
    // ranked method we can accept. Therefore, we sort the list and extract the URI with the lowest
    // rank.
    //
    List<String> sortedUris = uris.stream()
      .map(EidasLoaEnum::parse)
      .filter(Objects::nonNull)
      .sorted(eidasLoAbyOrder)
      .map(EidasLoaEnum::getUri)
      .collect(Collectors.toList());

    this.getAuthnContextClassContext(context).setProxiedAuthnContextClassRefs(Collections.singletonList(sortedUris.get(0)));
    return sortedUris.get(0);
  }

  /** {@inheritDoc} */
  @Override
  public String getReturnAuthnContextClassRef(ProfileRequestContext<?, ?> context, String authnContextUri, boolean displayedSignMessage)
      throws ExternalAutenticationErrorCodeException {

    final String logId = this.getLogString(context);

    AuthnContextClassContext authnContextClassContext = this.getAuthnContextClassContext(context);

    // Make sure we received one of the requested AuthnContextClassRef URI:s.
    //
    if (!this.isIssuedAuthnContextClassRefAccepted(context, authnContextUri)) {
      final String msg = String.format(
        "AuthnContextClassRef URI received in assertion from IdP (%s) does not match any of the URI:s sent in the AuthnRequest (%s)",
        authnContextUri, authnContextClassContext.getProxiedAuthnContextClassRefs());
      log.info("{} [{}]", msg, logId);
      throw new ExternalAutenticationErrorCodeException(AuthnEventIds.AUTHN_EXCEPTION, msg);
    }

    // OK, that seems OK. Next, we have to find the strongest possible requested URI that is equal to or weaker
    // than what we received.
    //

    // Sort all requsted URI:s, and then go through the list and pick the strongest one that matches.
    //
    Comparator<String> byOrder = this.getNationalUriComparator();
    List<String> sortedRequested = authnContextClassContext.getAuthnContextClassRefs()
        .stream()
        .sorted(byOrder.reversed())
        .collect(Collectors.toList()); 

    String authnContextUriToReturn = null;
    
    for (String requestedUri : sortedRequested) {
      boolean isSigMessage = this.isSignMessageURI(requestedUri);
      if (!displayedSignMessage && isSigMessage) {
        // If this is a sig message URI, and we did not display a signature message, this
        // URI can not be used.
        continue;
      }
      // Transform the national URI to eIDAS format and check if it is weaker or equal to the 
      // ACC we received.
      String eidasUri = this.transformToEidas(context, requestedUri);
      if (minimumComparisonMatch(eidasUri, authnContextUri)) {
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
    }    
    
    if (authnContextUriToReturn == null) {
      final String msg = String.format("AuthnContextClassRef received from IdP '%s' cannot be mapped against requested URI:s", authnContextUri);
      log.info("{} [{}]", msg, logId);
      throw new ExternalAutenticationErrorCodeException(AuthnEventIds.AUTHN_EXCEPTION, msg);      
    }

    return authnContextUriToReturn;
  }
  
  private static boolean minimumComparisonMatch(String uri, String issuedUri) {
    EidasLoaEnum loa = EidasLoaEnum.parse(uri);
    EidasLoaEnum issuedLoa = EidasLoaEnum.parse(issuedUri);
    return eidasLoAbyOrder.compare(loa, issuedLoa) <= 0;
  }

  /**
   * Determines if the received URI is accepted based on "minimum" comparison rules.
   */
  @Override
  protected boolean isIssuedAuthnContextClassRefAccepted(ProfileRequestContext<?, ?> context, String authnContextUri) {

    try {
      List<String> requestedUris = this.getAuthnContextClassContext(context).getProxiedAuthnContextClassRefs();
      if (requestedUris == null || requestedUris.isEmpty()) {
        // If we did not request anything, we accept what we were given.
        return true;
      }
      // For eIDAS, there will be only one URI sent.
      String requestedUri = requestedUris.get(0);

      // If the requested URI is equal to, or weaker than what we received, we accept it.
      EidasLoaEnum requestedLoa = EidasLoaEnum.parse(requestedUri);
      EidasLoaEnum authnContextLoa = EidasLoaEnum.parse(authnContextUri);
      if (requestedLoa == null || authnContextLoa == null) {
        return true; // Should not happen
      }
      return eidasLoAbyOrder.compare(requestedLoa, authnContextLoa) <= 0;
    }
    catch (ExternalAutenticationErrorCodeException e) {
      return false;
    }
  }

  /**
   * Transform the supplied URI to eIDAS formats before checking if it exists in the list of declared URI:s.
   */
  @Override
  protected boolean isSupported(ProfileRequestContext<?, ?> context, String uri, List<String> assuranceURIs) {

    try {
      AuthnContextClassContext authnContextClassContext = this.getAuthnContextClassContext(context);
      String nationalUri = this.loaMappings.toEidasURI(uri, authnContextClassContext.isSupportsNonNotifiedConcept());
      if (nationalUri == null) {
        return false;
      }
      return assuranceURIs.contains(nationalUri);
    }
    catch (ExternalAutenticationErrorCodeException e) {
      // Will fail later.
      log.error("No EidasAuthnContextClassContext available");
      return false;
    }
  }

  /** {@inheritDoc} */
  @Override
  protected List<String> transformForIdp(ProfileRequestContext<?, ?> context, List<String> urisForRequest) {

    try {
      final boolean supportsNotifiedNonNotified = this.getAuthnContextClassContext(context).isSupportsNonNotifiedConcept();
      return urisForRequest.stream()
          .map(u -> this.loaMappings.toEidasURI(u, supportsNotifiedNonNotified))
          .filter(Objects::nonNull)
          .distinct()
          .collect(Collectors.toList());
    }
    catch (ExternalAutenticationErrorCodeException e) {
      return urisForRequest.stream()
          .map(u -> this.loaMappings.toEidasURI(u, true))
          .filter(Objects::nonNull)
          .distinct()
          .collect(Collectors.toList());      
    }
  }
  
  protected String transformToEidas(ProfileRequestContext<?, ?> context, String uri) {
    List<String> eidas = this.transformForIdp(context, Arrays.asList(uri));
    return !eidas.isEmpty() ? eidas.get(0) : null;
  }

  /** {@inheritDoc} */
  @Override
  protected String transformForSp(ProfileRequestContext<?, ?> context, String authnContextUri) {

    try {
      AuthnContextClassContext authnContextClassContext = this.getAuthnContextClassContext(context);
      return this.loaMappings.toSwedishEidUri(authnContextUri, authnContextClassContext.isSupportsNonNotifiedConcept());
    }
    catch (ExternalAutenticationErrorCodeException e) {
      return this.loaMappings.toSwedishEidUri(authnContextUri, true);
    }
  }

  /**
   * Returns a comparator for comparing levels of national AuthnContext URI:s.
   * 
   * @return a comparator
   */
  protected Comparator<String> getNationalUriComparator() {

    return (u1, u2) -> {
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
