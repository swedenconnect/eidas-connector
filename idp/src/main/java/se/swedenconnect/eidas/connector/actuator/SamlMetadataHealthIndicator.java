/*
 * Copyright 2017-2024 Sweden Connect
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Objects;

/**
 * {@link HealthIndicator} asserting that we have valid SAML metadata for the Sweden Connect federation.
 *
 * @author Martin Lindström
 */
@Component("saml-metadata")
@Slf4j
public class SamlMetadataHealthIndicator implements HealthIndicator {

  /** The metadata resolver for the federation. */
  private final MetadataResolver metadataResolver;

  /**
   * Constructor.
   *
   * @param metadataResolver the metadata resolver
   */
  public SamlMetadataHealthIndicator(
      @Qualifier("saml.idp.metadata.Provider") final MetadataResolver metadataResolver) {
    this.metadataResolver = Objects.requireNonNull(metadataResolver, "metadataResolver must not be null");
  }

  /** {@inheritDoc} */
  @Override
  public Health health() {

    final Health.Builder builder = new Health.Builder();
    builder.withDetail("id", this.metadataResolver.getId());

    // Make sure that the resolver delivers at least one SP ...
    //
    try {
      if (this.metadataResolver instanceof IterableMetadataSource) {
        final Iterator<EntityDescriptor> it = ((IterableMetadataSource) this.metadataResolver).iterator();
        if (!it.hasNext()) {
          log.warn("Health: No valid SAML metadata available");
          return builder.outOfService()
              .withDetail("error-message", "No valid SAML metadata available").build();
        }
      }
      return builder.up().build();
    }
    catch (final Exception e) {
      return builder.down(e).build();
    }
  }

}
