/*
 * Copyright 2017-2026 Sweden Connect
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
package se.swedenconnect.eidas.simulator.metadata;

import net.shibboleth.shared.xml.SerializeSupport;
import org.jspecify.annotations.NonNull;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.eidas.simulator.country.CountryRepository;
import se.swedenconnect.eidas.simulator.country.SimulatedCountry;


/**
 * Publishes the metadata of the simulated countries: the signed aggregate of all active countries, and the entry of
 * each country on its own.
 *
 * @author Martin Lindström
 */
@RestController
public class MetadataController {

  /** The media type of SAML metadata. */
  public static final @NonNull MediaType SAML_METADATA = MediaType.parseMediaType("application/samlmetadata+xml");

  /** The active countries. */
  private final @NonNull CountryRepository countries;

  /** Creates the metadata. */
  private final @NonNull CountryMetadataFactory metadataFactory;

  /**
   * Constructor.
   *
   * @param countries the active countries
   * @param metadataFactory creates the metadata
   */
  public MetadataController(final @NonNull CountryRepository countries,
      final @NonNull CountryMetadataFactory metadataFactory) {
    this.countries = countries;
    this.metadataFactory = metadataFactory;
  }

  /**
   * Gets the signed aggregate holding all active countries.
   *
   * @return the metadata
   * @throws SignatureException if the metadata cannot be signed
   * @throws MarshallingException if the metadata cannot be marshalled
   */
  @GetMapping(value = "/metadata", produces = { "application/samlmetadata+xml", "application/xml", "text/xml" })
  public @NonNull ResponseEntity<String> getAggregate() throws SignatureException, MarshallingException {
    return ResponseEntity.ok().contentType(SAML_METADATA).body(serialize(this.metadataFactory.createAggregate()));
  }

  /**
   * Gets the signed metadata of one country. The URL is also the entityID of the country.
   *
   * @param country the country code
   * @return the metadata
   * @throws SignatureException if the metadata cannot be signed
   * @throws MarshallingException if the metadata cannot be marshalled
   */
  @GetMapping(value = "/{country}" + SimulatedCountry.METADATA_PATH,
      produces = { "application/samlmetadata+xml", "application/xml", "text/xml" })
  public @NonNull ResponseEntity<String> getCountryMetadata(final @PathVariable("country") @NonNull String country)
      throws SignatureException, MarshallingException {
    final SimulatedCountry simulatedCountry = this.countries.getCountry(country);
    if (simulatedCountry == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
    return ResponseEntity.ok()
        .contentType(SAML_METADATA)
        .body(serialize(this.metadataFactory.createSignedEntityDescriptor(simulatedCountry)));
  }

  /**
   * Serializes an XML object.
   *
   * @param object the object
   * @return the XML
   * @throws MarshallingException if the object cannot be marshalled
   */
  private static @NonNull String serialize(final @NonNull XMLObject object) throws MarshallingException {
    return SerializeSupport.nodeToString(XMLObjectSupport.marshall(object));
  }

}
