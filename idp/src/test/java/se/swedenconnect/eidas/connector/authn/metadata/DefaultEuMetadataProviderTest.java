/*
 * Copyright 2023-2024 Sweden Connect
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
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.shibboleth.shared.resolver.ResolverException;
import se.swedenconnect.eidas.connector.OpenSamlTestBase;
import se.swedenconnect.eidas.connector.events.EuMetadataEvent;
import se.swedenconnect.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import se.swedenconnect.opensaml.saml2.metadata.provider.MetadataProvider;
import se.swedenconnect.opensaml.sweid.saml2.authn.psc.RequestedPrincipalSelection;
import se.swedenconnect.opensaml.sweid.saml2.signservice.dss.EncryptedMessage;

/**
 * Test cases for DefaultEuMetadataProvider.
 *
 * @author Martin Lindström
 */
@Slf4j
public class DefaultEuMetadataProviderTest extends OpenSamlTestBase {

  private static TestApplicationEventPublisher publisher = new TestApplicationEventPublisher();

  @BeforeEach
  public void init() {
    publisher.clear();
  }

  @Test
  public void test() throws Exception {

    RequestedPrincipalSelection s = (RequestedPrincipalSelection) XMLObjectSupport.buildXMLObject(RequestedPrincipalSelection.DEFAULT_ELEMENT_NAME);
    Assertions.assertNotNull(s);
  }

  @Test
  public void testListCountries() throws Exception {
    // There are 17 countries in the metadata. ES is marked as hidden-from-disco ...
    //
    final MetadataProvider provider = this.createProvider("metadata/eu-metadata-idps.xml");
    try {
      final DefaultEuMetadataProvider euProvider = new DefaultEuMetadataProvider(provider, publisher);

      final List<CountryMetadata> countries = euProvider.getCountries();
      Assertions.assertEquals(16, countries.size());

      // Assert that ES is not there
      Assertions.assertTrue(countries.stream().noneMatch(c -> "ES".equals(c.getCountryCode())));

      // Null argument should also work
      Assertions.assertEquals(16, euProvider.getCountries(null).size());
    }
    finally {
      provider.destroy();
    }
  }

  @Test
  public void testListCountriesExplicit() throws Exception {
    // Assert that we get also hidden countries if explicitly asked for.
    //
    final MetadataProvider provider = this.createProvider("metadata/eu-metadata-idps.xml");
    try {
      final DefaultEuMetadataProvider euProvider = new DefaultEuMetadataProvider(provider, publisher);

      final List<CountryMetadata> countries = euProvider.getCountries(List.of("ES", "HR", "IT"));
      Assertions.assertEquals(3, countries.size());

      // Assert that ES is there (and marked as hidden)
      Assertions.assertTrue(countries.stream().anyMatch(c -> "ES".equals(c.getCountryCode())));
      Assertions.assertTrue(countries.stream().filter(c -> "ES".equals(c.getCountryCode()))
          .map(c -> c.isHideFromDiscovery()).findFirst().orElse(null));
    }
    finally {
      provider.destroy();
    }
  }

  @Test
  public void testGetCountry() throws Exception {
    final MetadataProvider provider = this.createProvider("metadata/eu-metadata-idps.xml");
    try {
      final DefaultEuMetadataProvider euProvider = new DefaultEuMetadataProvider(provider, publisher);

      Assertions.assertNotNull(euProvider.getCountry("es"));
      Assertions.assertNotNull(euProvider.getCountry("HR"));
      Assertions.assertNull(euProvider.getCountry("SE"));
    }
    finally {
      provider.destroy();
    }
  }

  @Test
  public void testContains() throws Exception {
    final MetadataProvider provider = this.createProvider("metadata/eu-metadata-idps.xml");
    try {
      final DefaultEuMetadataProvider euProvider = new DefaultEuMetadataProvider(provider, publisher);

      Assertions.assertTrue(euProvider.contains("ES", true));
      Assertions.assertFalse(euProvider.contains("ES", false));

      Assertions.assertTrue(euProvider.contains("IT", true));
      Assertions.assertTrue(euProvider.contains("IT", false));
    }
    finally {
      provider.destroy();
    }
  }

  @Test
  public void testGetProvider() throws Exception {
    final MetadataProvider provider = this.createProvider("metadata/eu-metadata-idps.xml");
    try {
      final DefaultEuMetadataProvider euProvider = new DefaultEuMetadataProvider(provider, publisher);

      Assertions.assertNotNull(euProvider.getProvider());
    }
    finally {
      provider.destroy();
    }
  }

  @Test
  public void testEvents() throws Exception {

    final MetadataProvider provider = Mockito.mock(MetadataProvider.class);
    final MetadataProvider provider1 = this.createProvider("metadata/eu-metadata-idps.xml");
    final MetadataProvider provider2 = this.createProvider("metadata/eu-metadata-idps2.xml");

    Mockito.when(provider.getID()).thenReturn("test-provider");

    try {
      final DefaultEuMetadataProvider euProvider = new DefaultEuMetadataProvider(provider, publisher);

      Mockito.when(provider.getLastUpdate()).thenReturn(provider1.getLastUpdate());
      Mockito.when(provider.getIdentityProviders()).thenReturn(provider1.getIdentityProviders());

      final List<CountryMetadata> countries = euProvider.getCountries();
      Assertions.assertEquals(16, countries.size());
      Assertions.assertTrue(euProvider.contains("PT", false));

      Assertions.assertEquals(1, publisher.getEvents().size());
      Assertions.assertEquals(17, publisher.getEvents().get(0).getEuMetadataUpdateData().getAddedCountries().size());
      Assertions.assertEquals(0, publisher.getEvents().get(0).getEuMetadataUpdateData().getRemovedCountries().size());

      // Use other provider (PT is removed)
      Mockito.when(provider.getLastUpdate()).thenReturn(Instant.now().plusSeconds(60));
      Mockito.when(provider.getIdentityProviders()).thenReturn(provider2.getIdentityProviders());

      Assertions.assertEquals(15, euProvider.getCountries().size());

      Mockito.when(provider.getLastUpdate()).thenReturn(Instant.now().minusSeconds(61));
      Assertions.assertFalse(euProvider.contains("PT", false));

      Assertions.assertEquals(2, publisher.getEvents().size());
      Assertions.assertEquals(0, publisher.getEvents().get(1).getEuMetadataUpdateData().getAddedCountries().size());
      Assertions.assertEquals(1, publisher.getEvents().get(1).getEuMetadataUpdateData().getRemovedCountries().size());
      Assertions.assertEquals("PT", publisher.getEvents().get(1).getEuMetadataUpdateData().getRemovedCountries().get(0));

      // And again
      Mockito.when(provider.getLastUpdate()).thenReturn(null);
      Mockito.when(provider.getIdentityProviders()).thenReturn(provider1.getIdentityProviders());

      Assertions.assertEquals(16, euProvider.getCountries().size());
      Mockito.when(provider.getLastUpdate()).thenReturn(Instant.now().minusSeconds(60));
      Assertions.assertTrue(euProvider.contains("PT", false));

      Assertions.assertEquals(3, publisher.getEvents().size());
      Assertions.assertEquals(1, publisher.getEvents().get(2).getEuMetadataUpdateData().getAddedCountries().size());
      Assertions.assertEquals("PT", publisher.getEvents().get(2).getEuMetadataUpdateData().getAddedCountries().get(0));
      Assertions.assertEquals(0, publisher.getEvents().get(2).getEuMetadataUpdateData().getRemovedCountries().size());

      // No change
      Mockito.when(provider.getLastUpdate()).thenReturn(null);

      euProvider.getCountries();

      Assertions.assertEquals(4, publisher.getEvents().size());
      Assertions.assertEquals(0, publisher.getEvents().get(3).getEuMetadataUpdateData().getAddedCountries().size());
      Assertions.assertEquals(0, publisher.getEvents().get(3).getEuMetadataUpdateData().getRemovedCountries().size());
    }
    finally {
      provider1.destroy();
      provider2.destroy();
    }

  }

  @Test
  public void testEventsError() throws Exception {

    final MetadataProvider provider = Mockito.mock(MetadataProvider.class);
    Mockito.when(provider.getID()).thenReturn("test-provider");
    final ResolverException error = new ResolverException("Failed to read URL");
    Mockito.when(provider.getIdentityProviders()).thenThrow(error);
    Mockito.when(provider.getLastUpdate()).thenReturn(null);

    final DefaultEuMetadataProvider euProvider = new DefaultEuMetadataProvider(provider, publisher);

    Assertions.assertTrue(euProvider.getCountries().isEmpty());

    Assertions.assertEquals(1, publisher.getEvents().size());
    Assertions.assertEquals(error, publisher.getEvents().get(0).getEuMetadataUpdateData().getError());
  }

  private MetadataProvider createProvider(final String xml) throws Exception {
    final Resource resource = new ClassPathResource(xml);
    final FilesystemMetadataProvider provider = new FilesystemMetadataProvider(resource.getFile());
    provider.initialize();
    return provider;
  }

  private static class TestApplicationEventPublisher implements ApplicationEventPublisher {

    @Getter
    private final List<EuMetadataEvent> events = new ArrayList<>();

    @Override
    public void publishEvent(final Object event) {
      final EuMetadataEvent e = EuMetadataEvent.class.cast(event);
      this.events.add(e);
      log.info("Event: {}", e.getSource());
    }

    public void clear() {
      this.events.clear();
    }

  }



}
