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
package se.swedenconnect.eidas.connector.actuator;

import lombok.extern.slf4j.Slf4j;
import org.opensaml.saml.metadata.IterableMetadataSource;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import se.swedenconnect.spring.authnserver.autoconfigure.ConfiguredAuthnServer;
import se.swedenconnect.spring.authnserver.saml.metadata.ManagedMetadataSource;
import se.swedenconnect.spring.authnserver.saml.metadata.SamlMetadataBackend;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * {@link HealthIndicator} asserting that we have valid SAML metadata for the Sweden Connect federation.
 * <p>
 * The indicator replaces the {@code saml-metadata} health indicator of the authentication server.
 * </p>
 *
 * @author Martin Lindström
 */
@Component("saml-metadata")
@Slf4j
public class SamlMetadataHealthIndicator implements HealthIndicator {

  /** Gives the metadata resolvers for the federation. */
  private final Supplier<List<MetadataResolver>> metadataResolvers;

  /**
   * Constructor.
   *
   * @param authnServer gives the metadata sources of the authentication server
   */
  @Autowired
  public SamlMetadataHealthIndicator(final ConfiguredAuthnServer authnServer) {
    Objects.requireNonNull(authnServer, "authnServer must not be null");
    this.metadataResolvers = () -> authnServer.getClientRegistryBackends(SamlMetadataBackend.class).stream()
        .flatMap(b -> b.getMetadataSources().stream())
        .map(ManagedMetadataSource::getResolver)
        .toList();
  }

  /**
   * Constructor.
   *
   * @param metadataResolver the metadata resolver
   */
  public SamlMetadataHealthIndicator(final MetadataResolver metadataResolver) {
    Objects.requireNonNull(metadataResolver, "metadataResolver must not be null");
    this.metadataResolvers = () -> List.of(metadataResolver);
  }

  /** {@inheritDoc} */
  @Override
  public Health health() {

    final List<MetadataResolver> resolvers = this.metadataResolvers.get();
    final Health.Builder builder = new Health.Builder();
    builder.withDetail("id", String.join(", ", resolvers.stream()
        .map(MetadataResolver::getId)
        .filter(Objects::nonNull)
        .toList()));

    // Make sure that the resolvers deliver at least one SP ...
    //
    try {
      boolean hasMetadata = false;
      for (final MetadataResolver resolver : resolvers) {
        if (resolver instanceof final IterableMetadataSource iterable) {
          final Iterator<EntityDescriptor> it = iterable.iterator();
          if (it.hasNext()) {
            hasMetadata = true;
          }
        }
        else {
          hasMetadata = true;
        }
      }
      if (!hasMetadata) {
        log.warn("Health: No valid SAML metadata available");
        return builder.outOfService()
            .withDetail("error-message", "No valid SAML metadata available").build();
      }
      return builder.up().build();
    }
    catch (final Exception e) {
      return builder.down(e).build();
    }
  }

}
