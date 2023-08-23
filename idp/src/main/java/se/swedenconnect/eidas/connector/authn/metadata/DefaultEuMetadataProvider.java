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
package se.swedenconnect.eidas.connector.authn.metadata;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.context.ApplicationEventPublisher;

import lombok.extern.slf4j.Slf4j;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import se.swedenconnect.opensaml.saml2.metadata.provider.MetadataProvider;

/**
 * Default implementation of the {@link EuMetadataProvider} interface.
 *
 * @author Martin Lindström
 */
@Slf4j
public class DefaultEuMetadataProvider implements EuMetadataProvider {

  /** The underlying metadata provider. */
  private final MetadataProvider provider;

  /** The system event publisher. */
  private final ApplicationEventPublisher publisher;

  /** An index of country codes and their respective entity descriptors. */
  private Map<String, CountryMetadata> countries = Collections.emptyMap();

  /** The last time the country list was indexed. */
  private Instant countryIndexingTime = Instant.ofEpochMilli(0L);

  /**
   * Constructor.
   *
   * @param provider the underlying metadata provider
   * @param publisher the system event publisher
   */
  public DefaultEuMetadataProvider(final MetadataProvider provider, final ApplicationEventPublisher publisher) {
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
    this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
  }

  /** {@inheritDoc} */
  @Override
  public MetadataProvider getProvider() {
    return this.provider;
  }

  /** {@inheritDoc} */
  @Override
  public CountryMetadata getCountry(final String countryCode) {
    return this.getCountryMap().get(Optional.ofNullable(countryCode).map(String::toUpperCase).orElse(""));
  }

  /** {@inheritDoc} */
  @Override
  public boolean contains(final String countryCode, final boolean hiddenOk) {
    return Optional.ofNullable(this.getCountry(countryCode))
        .filter(c -> (hiddenOk || !c.isHideFromDiscovery()))
        .isPresent();
  }

  /** {@inheritDoc} */
  @Override
  public List<CountryMetadata> getCountries(final List<String> requestedCountries) {
    final Collection<CountryMetadata> availableCountries = this.getCountryMap().values();
    if (requestedCountries == null || requestedCountries.isEmpty()) {
      return availableCountries.stream()
          .filter(c -> !c.isHideFromDiscovery())
          .toList();
    }
    else {
      return availableCountries.stream()
          .filter(c -> requestedCountries.stream()
              .filter(r -> r.equalsIgnoreCase(c.getCountryCode()))
              .findFirst()
              .isPresent())
          .toList();
    }
  }

  private synchronized Map<String, CountryMetadata> getCountryMap() {
    if (Optional.ofNullable(this.provider.getLastUpdate()).orElseGet(() -> Instant.now())
        .isAfter(this.countryIndexingTime)) {
      try {
        final List<String> eventInfo = new ArrayList<>();
        final Map<String, CountryMetadata> cm = new HashMap<>();
        for (final EntityDescriptor ed : this.provider.getIdentityProviders()) {
          final CountryMetadata c = new CountryMetadata(ed);
          final String countryCode = c.getCountryCode();
          if (countryCode != null) {
            cm.put(countryCode, c);
          }
          else {
            final String info = "Found IdP '%s' in EU metadata that does not have NodeCountry extension".formatted(ed.getEntityID());
            log.error("{}", info);
            eventInfo.add(info);
          }
        }
        if (this.countries.isEmpty() && !cm.isEmpty()) {
          eventInfo.add("Initial load of EU metadata");
        }
        final List<String> removedCountries = this.countries.values().stream()
            .filter(c -> !cm.containsKey(c.getCountryCode()))
            .map(c -> c.getCountryCode())
            .toList();
        final List<String> addedCountries = cm.values().stream()
            .filter(c -> !this.countries.containsKey(c.getCountryCode()))
            .map(c -> c.getCountryCode())
            .toList();

        if (!removedCountries.isEmpty() || !addedCountries.isEmpty()) {
          log.info("EU metadata was updated - added: {} removed: {}", addedCountries, removedCountries);
        }
        else {
          log.debug("EU metadata was updated - no changed countries");
        }

        final EuMetadataEvent event = new EuMetadataEvent(Instant.now(), removedCountries, addedCountries);
        if (!eventInfo.isEmpty()) {
          event.addInformation(String.join(";", eventInfo));
        }
        this.publisher.publishEvent(event);

        this.countries = cm;
        this.countryIndexingTime = Instant.now();
      }
      catch (final ResolverException e) {
        log.error("Failed to list metadata from {}", this.provider.getID(), e);
        this.publisher.publishEvent(new EuMetadataEvent(Instant.now(), e));
      }
    }
    return this.countries;
  }

}
