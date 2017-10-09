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

import java.security.Principal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import se.elegnamnden.eidas.mapping.loa.LevelOfAssuranceMappings;
import se.litsec.eidas.opensaml.common.EidasLoaEnum;
import se.litsec.swedisheid.opensaml.saml2.authentication.LevelofAssuranceAuthenticationContextURI;

/**
 * Provides IdP services for handling Authentication Contexts (level of assurance).
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
@Deprecated
public class AuthenticationContextService implements InitializingBean {

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(AuthenticationContextService.class);

  /** The default eIDAS context URI to use in cases where we cannot calculate this from the configuration. */
  private static final EidasLoaEnum DEFAULT_CTX_URI = EidasLoaEnum.LOA_SUBSTANTIAL_NON_NOTIFIED;

  /**
   * The default eIDAS context URI to use in cases where we cannot calculate this from the configuration. When the
   * recipient does not support Notified/non-notified.
   */
  private static final EidasLoaEnum DEFAULT_CTX_URI_NNS = EidasLoaEnum.LOA_SUBSTANTIAL;

  /** Mappings between eIDAs and Swedish eID LoA URI:s. */
  private LevelOfAssuranceMappings loaMappings;

  /**
   * The Shibboleth bean shibboleth.AuthenticationPrincipalWeightMap. We use this to find out the default Authentication
   * Context URI to use.
   */
  private Map<Principal, Integer> authnContextweightMap;

  /**
   * Given the Authn Context URI:s given in the Swedish eID request, the method calculates which Authn Context URI
   * according to the eIDAS definition to use. We will use the eIDAS URI with the lowest rank since eIDAS uses "minimum"
   * comparison for requested authentication contexts.
   * 
   * @param requestedLoaURIsFromRequest
   *          the LoA URI:s (Swedish eID definitions) from the original request (may be empty)
   * @param assuranceCertifications
   *          the LoA URI:s (eIDAS definitions) declared by the foreign IdP in its metadata assurance certifications
   *          attribute
   * @param supportsNonNotifiedConcept
   *          flag telling whether the recipient of the mapped URI:s understands the concept of non-notified and
   *          notified eID-schemes
   * @return the eIDAS Authn Context URI to use for the eIDAS AuthnRequest. If {@code null} there is an error
   */
  public String getEidasAuthnContextURI(List<String> requestedLoaURIsFromRequest, List<String> assuranceCertifications,
      boolean supportsNonNotifiedConcept) {

    final List<String> sortedAssuranceCertifications = sortEidasContextURIs(assuranceCertifications);

    // If the request does not contain any requested authentication context URI:s, we use
    // the lowest level announced by the IdP, or if this is not given, the connector default.
    //
    if (requestedLoaURIsFromRequest.isEmpty()) {
      if (!sortedAssuranceCertifications.isEmpty()) {
        log.info("Request did not contain any (valid) requested authentication context URIs - using IdP:s lowest defined URI: '{}'",
          sortedAssuranceCertifications.get(0));
        return sortedAssuranceCertifications.get(0);
      }
      else {
        String connectorDefault = this.getDefaultEidasAuthenticationContextURI(supportsNonNotifiedConcept);
        log.info(
          "Request did not contain any (valid) requested authentication context URIs and IdP did not declare any URIs - using default '{}'",
          connectorDefault);
        return connectorDefault;
      }
    }

    // Map the requested LoA:s from the request (Swedish LoA:s) to eIDAS definitions.
    // We get a sorted list back.
    //
    List<String> requestedLoaURIsFromRequestMapped = this.transformAuthenticationContextURIs(requestedLoaURIsFromRequest,
      supportsNonNotifiedConcept);

    // This should not happen since Shibboleth should stop the request before hand.
    //
    if (requestedLoaURIsFromRequestMapped.isEmpty()) {
      return null;
    }

    // If the IdP hasn't declared any assurance certifications, we use our lowest URI.
    //
    if (sortedAssuranceCertifications.isEmpty()) {
      log.info("IdP does not declare assurance certification - using lowest URI from AuthnRequest: '{}'", requestedLoaURIsFromRequestMapped
        .get(0));
      return requestedLoaURIsFromRequest.get(0);
    }

    return this.matchRequestedAuthenticationContextURIs(requestedLoaURIsFromRequestMapped, sortedAssuranceCertifications);
  }

  /**
   * Transforms a list of authentication context URI:s received as requested AuthnContext URI:s from the Swedish eID
   * AuthnRequest into a list of corresponding eIDAS AuthnContext URI:s.
   * 
   * @param fromSwedishRequest
   *          list of AuthnContext URI:s
   * @param supportsNonNotifiedConcept
   *          flag telling whether the recipient of the mapped URI:s understands the concept of non-notified and
   *          notified eID-schemes
   * @return transformed list of AuthnContext URI:s (sorted according to the order of importance where the least
   *         important is placed first)
   */
  private List<String> transformAuthenticationContextURIs(List<String> fromSwedishRequest, boolean supportsNonNotifiedConcept) {

    if (fromSwedishRequest.isEmpty()) {
      return Collections.emptyList();
    }

    List<EidasLoaEnum> loas = fromSwedishRequest.stream()
      .map(LevelofAssuranceAuthenticationContextURI.LoaEnum::parse)
      .filter(loa -> loa != null)
      .map(loa -> this.loaMappings.toEidasLoa(loa, supportsNonNotifiedConcept))
      .distinct()
      .collect(Collectors.toList());

    // Sort and return URI:s
    //
    Comparator<EidasLoaEnum> byOrder = (u1, u2) -> Integer.compare(u1.getOrder(), u2.getOrder());

    return loas.stream()
      .sorted(byOrder)
      .map(EidasLoaEnum::getUri)
      .distinct()
      .collect(Collectors.toList());
  }

  /**
   * Matches the requested AuthnContext URI:s against the ones declared under the "assurance certification" section in
   * the IdP metadata.
   * <p>
   * If the IdP does not specify any URI:s, we assume that we can use all requested URI:s in the request.
   * </p>
   * 
   * @param requestedURIs
   *          the requested URI:s (non empty, sorted by order, weakest first)
   * @param assuranceURIs
   *          the URI:s declared by the IdP (non empty, sorted by order, weakest first)
   * @return the URI that match against the IdP declaration and has the lowest rank (if more than one matches)
   */
  private String matchRequestedAuthenticationContextURIs(List<String> requestedURIs, List<String> assuranceURIs) {

    return requestedURIs.stream()
      .filter(u -> assuranceURIs.contains(u))
      .findFirst()
      .orElse(null);
  }

  /**
   * Returns the default authentication context URI to use in an eIDAS AuthnRequest if no URI:s are supplied in the
   * original request.
   * 
   * @param supportsNonNotifiedConcept
   *          flag telling whether the recipient of the mapped URI:s understands the concept of non-notified and
   *          notified eID-schemes
   * @return the default URI
   */
  private String getDefaultEidasAuthenticationContextURI(boolean supportsNonNotifiedConcept) {

    // Look for the default Swedish eID URI first. This is placed in the weight map that the Shibboleth
    // authentication system is configured with.
    //
    String swedishDefaultUri = this.authnContextweightMap.entrySet()
      .stream()
      .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
      .map(Map.Entry::getKey)
      .filter(AuthnContextClassRefPrincipal.class::isInstance)
      .map(p -> p.getName())
      .findFirst()
      .orElse(null);

    if (swedishDefaultUri == null) {
      return supportsNonNotifiedConcept ? DEFAULT_CTX_URI.getUri() : DEFAULT_CTX_URI_NNS.getUri();
    }
    String eidasUri = this.loaMappings.toEidasURI(swedishDefaultUri, supportsNonNotifiedConcept);
    return eidasUri != null ? eidasUri : (supportsNonNotifiedConcept ? DEFAULT_CTX_URI.getUri() : DEFAULT_CTX_URI_NNS.getUri());
  }

  /**
   * Sorts a list of eIDAS Authn Context URI:s in order where the "weakest" is placed first.
   * 
   * @param uris
   *          the list to sort
   * @return a sorted list of URI:s
   */
  public static List<String> sortEidasContextURIs(List<String> uris) {

    Comparator<EidasLoaEnum> byOrder = (u1, u2) -> Integer.compare(u1.getOrder(), u2.getOrder());

    if (uris == null) {
      return Collections.emptyList();
    }

    return uris.stream()
      .map(EidasLoaEnum::parse)
      .sorted(byOrder)
      .map(EidasLoaEnum::getUri)
      .distinct()
      .collect(Collectors.toList());
  }

  /**
   * Assigns a mapper that helps us to go between eIDAS and Swedish eID LoA URI definitions.
   * 
   * @param loaMappings
   *          the mapper
   */
  public void setLoaMappings(LevelOfAssuranceMappings loaMappings) {
    this.loaMappings = loaMappings;
  }

  /**
   * The Shibboleth bean {@code shibboleth.AuthenticationPrincipalWeightMap}. We use this to find out the default
   * Authentication Context URI to use.
   * 
   * @param authnContextweightMap
   *          the Shibboleth bean {@code shibboleth.AuthenticationPrincipalWeightMap}
   */
  public void setAuthnContextweightMap(Map<Principal, Integer> authnContextweightMap) {
    this.authnContextweightMap = authnContextweightMap;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notEmpty(this.authnContextweightMap, "Property 'authnContextweightMap' has not be assigned, or is empty");
    Assert.notNull(this.loaMappings, "Property 'loaMappings' must be assigned");
  }

}
