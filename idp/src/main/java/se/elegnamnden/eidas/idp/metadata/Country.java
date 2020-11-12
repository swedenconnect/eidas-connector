/*
 * Copyright 2017-2020 Sweden Connect
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
package se.elegnamnden.eidas.idp.metadata;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import se.litsec.eidas.opensaml.common.EidasConstants;
import se.litsec.swedisheid.opensaml.saml2.authentication.LevelofAssuranceAuthenticationContextURI;

/**
 * Representation of a country.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class Country implements Comparable<Country> {

  /** The metadata entry for the country. */
  private final EntityDescriptor entityDescriptor;

  /** The ISO code for the country. */
  private String countryCode;

  /** The assurance levels. */
  private List<String> assuranceLevels;

  /** The swedish LoA URI:s that are supported. */
  private List<String> supportedSwedishAssuranceLevels;

  /** Hide from discovery? */
  private Boolean hideFromDiscovery;

  /**
   * Constructor.
   * 
   * @param entityDescriptor
   *          the metadata entry for the country (eIDAS proxy service)
   */
  public Country(final EntityDescriptor entityDescriptor) {
    if (entityDescriptor == null) {
      throw new IllegalArgumentException("entityDescriptor must not be null");
    }
    this.entityDescriptor = entityDescriptor;
  }

  /**
   * Gets the entity descriptor.
   * 
   * @return the entity descriptor
   */
  public EntityDescriptor getEntityDescriptor() {
    return this.entityDescriptor;
  }

  /**
   * Gets the country code for the country.
   * 
   * @return the country code
   */
  public String getCountryCode() {
    if (this.countryCode == null) {
      this.countryCode = MetadataFunctions.getNodeCountry(this.entityDescriptor);
      if (this.countryCode != null) {
        this.countryCode = this.countryCode.toUpperCase();
      }
    }
    return this.countryCode;
  }

  /**
   * Gets the entityID of the country's IdP.
   * 
   * @return the SAML entityID
   */
  public String getEntityID() {
    return this.entityDescriptor.getEntityID();
  }

  /**
   * Gets the assurance levels that are declared by the country.
   * 
   * @return the assurance levels
   */
  public List<String> getAssuranceLevels() {
    if (this.assuranceLevels == null) {
      this.assuranceLevels = MetadataFunctions.getAssuranceLevels(this.entityDescriptor);
    }
    return this.assuranceLevels;
  }
  
  public boolean canAuthenticate(final List<String> requestedAuthnContextClassRefs) {
    if (requestedAuthnContextClassRefs.isEmpty()) {
      // If the Swedish SP did not request any AuthnContextClassRefs we let the defaults kick-in ...
      return true;
    }
    final List<String> supported = this.getSupportedSwedishAssuranceLevels();
    return requestedAuthnContextClassRefs.stream().filter(u -> supported.contains(u)).findFirst().isPresent();
  }

  /**
   * Given the assurance levels, this method tells which assurance levels Swedish assurance levels that are supported.
   * 
   * @return a list of URI:s
   */
  private List<String> getSupportedSwedishAssuranceLevels() {
    if (this.supportedSwedishAssuranceLevels == null) {
      final Set<String> supported = new HashSet<>();
      final List<String> idpLevels = this.getAssuranceLevels();
      for (String uri : idpLevels) {
        if (EidasConstants.EIDAS_LOA_LOW.equals(uri)) {
          supported.addAll(SUPPORTED_FOR_EIDAS_LOA_LOW);
        }
        else if (EidasConstants.EIDAS_LOA_LOW_NON_NOTIFIED.equals(uri)) {
          supported.addAll(SUPPORTED_FOR_EIDAS_LOA_LOW_NON_NOTIFIED);
        }
        else if (EidasConstants.EIDAS_LOA_SUBSTANTIAL.equals(uri)) {
          supported.addAll(SUPPORTED_FOR_EIDAS_LOA_SUBSTANTIAL);
        }
        else if (EidasConstants.EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED.equals(uri)) {
          supported.addAll(SUPPORTED_FOR_EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED);
        }
        else if (EidasConstants.EIDAS_LOA_HIGH.equals(uri)) {
          supported.addAll(SUPPORTED_FOR_EIDAS_LOA_HIGH);
        }
        else if (EidasConstants.EIDAS_LOA_HIGH_NON_NOTIFIED.equals(uri)) {
          supported.addAll(SUPPORTED_FOR_EIDAS_LOA_HIGH_NON_NOTIFIED);
        }
      }
      this.supportedSwedishAssuranceLevels = supported.stream().collect(Collectors.toList());
      
      if (this.supportedSwedishAssuranceLevels.isEmpty()) {
        // If there is no URI:s here it must mean that the foreign IdP did not declare any URI:s.
        // In these cases we support anything since we can't know what is supported.
        this.supportedSwedishAssuranceLevels = SUPPORTED_FOR_EIDAS_LOA_HIGH;
      }
    }
    return this.supportedSwedishAssuranceLevels;
  }

  private static List<String> SUPPORTED_FOR_EIDAS_LOA_LOW = Arrays.asList(
    LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_LOW,
    LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_LOW_NF,
    LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_LOW_SIGMESSAGE,
    LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_LOW_NF_SIGMESSAGE);

  private static List<String> SUPPORTED_FOR_EIDAS_LOA_LOW_NON_NOTIFIED = Arrays.asList(
    LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_LOW,
    LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_LOW_SIGMESSAGE);

  private static List<String> SUPPORTED_FOR_EIDAS_LOA_SUBSTANTIAL = Stream.concat(
    Arrays.asList(
      LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL,
      LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF,
      LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_SIGMESSAGE,
      LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF_SIGMESSAGE)
      .stream(), SUPPORTED_FOR_EIDAS_LOA_LOW.stream())
    .collect(Collectors.toList());

  private static List<String> SUPPORTED_FOR_EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED = Stream.concat(
    Arrays.asList(
      LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL,
      LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_SUBSTANTIAL_SIGMESSAGE)
      .stream(), SUPPORTED_FOR_EIDAS_LOA_LOW_NON_NOTIFIED.stream())
    .collect(Collectors.toList());

  private static List<String> SUPPORTED_FOR_EIDAS_LOA_HIGH = Stream.concat(
    Arrays.asList(
      LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_HIGH,
      LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_HIGH_NF,
      LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_HIGH_SIGMESSAGE,
      LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_HIGH_NF_SIGMESSAGE)
      .stream(), SUPPORTED_FOR_EIDAS_LOA_SUBSTANTIAL.stream())
    .collect(Collectors.toList());

  private static List<String> SUPPORTED_FOR_EIDAS_LOA_HIGH_NON_NOTIFIED = Stream.concat(
    Arrays.asList(
      LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_HIGH,
      LevelofAssuranceAuthenticationContextURI.AUTH_CONTEXT_URI_EIDAS_HIGH_SIGMESSAGE)
      .stream(), SUPPORTED_FOR_EIDAS_LOA_SUBSTANTIAL_NON_NOTIFIED.stream())
    .collect(Collectors.toList());

  /**
   * Tells whether the country should be hidden from discovery.
   * 
   * @return true if the country should be hidden from discovery and false otherwise
   */
  public boolean isHideFromDiscovery() {
    if (this.hideFromDiscovery == null) {
      this.hideFromDiscovery = MetadataFunctions.getHideFromDiscovery(this.entityDescriptor);
    }
    return this.hideFromDiscovery;
  }

  /** {@inheritDoc} */
  @Override
  public int compareTo(final Country o) {
    final String code = Optional.ofNullable(this.getCountryCode()).orElse("");
    return code.compareTo(Optional.ofNullable(o.getCountryCode()).orElse(""));
  }

}
