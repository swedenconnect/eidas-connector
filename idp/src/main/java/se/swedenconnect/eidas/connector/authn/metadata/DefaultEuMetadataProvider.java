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
package se.swedenconnect.eidas.connector.authn.metadata;

import net.shibboleth.shared.resolver.ResolverException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import se.swedenconnect.eidas.connector.events.EuMetadataEvent;
import se.swedenconnect.opensaml.saml2.metadata.provider.MetadataProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of the {@link EuMetadataProvider} interface.
 *
 * @author Martin Lindström
 */
public class DefaultEuMetadataProvider implements EuMetadataProvider, InitializingBean {

  /** Logger instance. */
  private static final Logger log = LoggerFactory.getLogger(DefaultEuMetadataProvider.class);

  /** The underlying metadata provider. */
  private final @NonNull MetadataProvider provider;

  /** The system event publisher. */
  private final @NonNull ApplicationEventPublisher publisher;

  /** An index of country codes and their respective entity descriptors. */
  private @NonNull Map<String, CountryMetadata> countries = Collections.emptyMap();

  /** The last time the country list was indexed. */
  private @NonNull Instant countryIndexingTime = Instant.ofEpochMilli(0L);

  /**
   * Constructor.
   *
   * @param provider the underlying metadata provider
   * @param publisher the system event publisher
   */
  public DefaultEuMetadataProvider(final @NonNull MetadataProvider provider,
      final @NonNull ApplicationEventPublisher publisher) {
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
    this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull MetadataProvider getProvider() {
    return this.provider;
  }

  /** {@inheritDoc} */
  @Override
  public @Nullable CountryMetadata getCountry(final @Nullable String countryCode) {
    return this.getCountryMap().get(Optional.ofNullable(countryCode).map(String::toUpperCase).orElse(""));
  }

  /** {@inheritDoc} */
  @Override
  public boolean contains(final @Nullable String countryCode, final boolean hiddenOk) {
    return Optional.ofNullable(this.getCountry(countryCode))
        .filter(c -> (hiddenOk || !c.isHideFromDiscovery()))
        .isPresent();
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull List<CountryMetadata> getCountries(final @Nullable List<String> requestedCountries) {
    final Collection<CountryMetadata> availableCountries = this.getCountryMap().values();
    if (requestedCountries == null || requestedCountries.isEmpty()) {
      return availableCountries.stream()
          .filter(c -> !c.isHideFromDiscovery())
          .toList();
    }
    else {
      return availableCountries.stream()
          .filter(c -> requestedCountries.stream()
              .anyMatch(r -> r.equalsIgnoreCase(c.getCountryCode())))
          .toList();
    }
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull List<CountryMetadata> getAllCountries() {
    return this.getCountryMap().values().stream().toList();
  }

  private synchronized Map<String, CountryMetadata> getCountryMap() {
    if (Optional.ofNullable(this.provider.getLastUpdate()).orElseGet(Instant::now)
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
            final String info =
                "Found IdP '%s' in EU metadata that does not have NodeCountry extension".formatted(ed.getEntityID());
            log.error("{}", info);
            eventInfo.add(info);
          }
        }
        if (this.countries.isEmpty() && !cm.isEmpty()) {
          eventInfo.add("Initial load of EU metadata");
        }
        final List<String> removedCountries = this.countries.values().stream()
            .map(CountryMetadata::getCountryCode)
            .filter(countryCode -> !cm.containsKey(countryCode))
            .toList();
        final List<String> addedCountries = cm.values().stream()
            .map(CountryMetadata::getCountryCode)
            .filter(countryCode -> !this.countries.containsKey(countryCode))
            .toList();

        if (!removedCountries.isEmpty() || !addedCountries.isEmpty()) {
          if (!this.countries.isEmpty()) {
            log.info("EU metadata was updated - added: {} removed: {}", addedCountries, removedCountries);
          }
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

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() {
    log.info("Initial contents of EU metadata: {}",
        this.getCountries().stream().map(CountryMetadata::getCountryCode).toList());
  }

}
