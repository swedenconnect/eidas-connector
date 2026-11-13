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
package se.swedenconnect.eidas.simulator;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import se.swedenconnect.eidas.simulator.config.SimulatorProperties;
import se.swedenconnect.eidas.simulator.country.CountryRepository;
import se.swedenconnect.security.credential.factory.KeyStoreFactory;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.List;

/**
 * Loads the simulated countries of the configuration, with the keys of the test key store.
 *
 * @author Martin Lindström
 */
public final class TestCountries {

  /** The base URL of the simulator in tests. */
  public static final String BASE_URL = "https://simulator.example.com/simulator";

  /** The password of the test key stores. */
  public static final char[] PASSWORD = "secret".toCharArray();

  /**
   * Binds the country files under {@code countries/} to the simulator properties.
   *
   * @return the simulator properties holding all configured countries
   * @throws Exception for errors reading the files
   */
  public static SimulatorProperties loadProperties() throws Exception {
    final MutablePropertySources sources = new MutablePropertySources();
    final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    for (final Resource resource : new PathMatchingResourcePatternResolver().getResources("classpath:countries/*.yml")) {
      final List<PropertySource<?>> loaded = loader.load(resource.getFilename(), resource);
      loaded.forEach(sources::addLast);
    }
    final SimulatorProperties properties = new Binder(ConfigurationPropertySources.from(sources))
        .bind("simulator", SimulatorProperties.class)
        .orElseThrow(() -> new IllegalStateException("No simulator configuration"));
    properties.setBaseUrl(BASE_URL);
    return properties;
  }

  /**
   * Loads the test key store holding the keys of the simulator.
   *
   * @return the key store
   * @throws Exception for errors loading the key store
   */
  public static KeyStore loadKeyStore() throws Exception {
    try (final InputStream in = new ClassPathResource("simulator-keys.jks").getInputStream()) {
      return KeyStoreFactory.loadKeyStore(in, PASSWORD, "JKS", null);
    }
  }

  /**
   * Creates a repository holding the given countries, or all countries.
   *
   * @param codes the country codes, none for all countries
   * @return the repository
   * @throws Exception for errors loading the configuration
   */
  public static CountryRepository repository(final String... codes) throws Exception {
    final SimulatorProperties properties = loadProperties();
    return new CountryRepository(properties.getCountries(), List.of(codes), BASE_URL, loadKeyStore(), PASSWORD);
  }

  private TestCountries() {
  }

}
