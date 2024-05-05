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

import org.junit.jupiter.api.Test;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.HTTPMetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link SamlMetadataHealthIndicator} class.
 *
 * @author Martin Lindström
 */
public class SamlMetadataHealthIndicatorTest {

  @Test
  public void testHealthWhenMetadataResolverIsIterableAndHasNext() {
    final HTTPMetadataResolver iterableMetadataResolver = mock(HTTPMetadataResolver.class);
    when(iterableMetadataResolver.getId()).thenReturn("mockResolver");
    @SuppressWarnings("unchecked")
    final Iterator<EntityDescriptor> iterator = mock(Iterator.class);
    when(iterator.hasNext()).thenReturn(true);
    when(iterableMetadataResolver.iterator()).thenReturn(iterator);
    final SamlMetadataHealthIndicator indicator = new SamlMetadataHealthIndicator(iterableMetadataResolver);

    final Health health = indicator.health();

    assertEquals(Status.UP, health.getStatus());
  }

  @Test
  public void testHealthWhenMetadataResolverIsIterableAndHasNoNext() {
    final HTTPMetadataResolver iterableMetadataResolver = mock(HTTPMetadataResolver.class);
    when(iterableMetadataResolver.getId()).thenReturn("mockResolver");
    @SuppressWarnings("unchecked")
    final Iterator<EntityDescriptor> iterator = mock(Iterator.class);
    when(iterator.hasNext()).thenReturn(false);
    when(iterableMetadataResolver.iterator()).thenReturn(iterator);
    final SamlMetadataHealthIndicator indicator = new SamlMetadataHealthIndicator(iterableMetadataResolver);

    final Health health = indicator.health();

    assertEquals(Status.OUT_OF_SERVICE, health.getStatus());
  }

  @Test
  public void testHealthWhenMetadataResolverIsNotIterable() {
    final MetadataResolver metadataResolver = mock(MetadataResolver.class);
    when(metadataResolver.getId()).thenReturn("mockResolver");
    final SamlMetadataHealthIndicator indicator = new SamlMetadataHealthIndicator(metadataResolver);

    final Health health = indicator.health();

    assertEquals(Status.UP, health.getStatus());
  }
  
  @Test
  public void testHealthWhenErrorOccurredDuringCheck() {
    final HTTPMetadataResolver iterableMetadataResolver = mock(HTTPMetadataResolver.class);
    when(iterableMetadataResolver.getId()).thenReturn("mockResolver");
    when(iterableMetadataResolver.iterator()).thenThrow(RuntimeException.class);
    final SamlMetadataHealthIndicator indicator = new SamlMetadataHealthIndicator(iterableMetadataResolver);

    final Health health = indicator.health();

    assertEquals(Status.DOWN, health.getStatus());
  }

}