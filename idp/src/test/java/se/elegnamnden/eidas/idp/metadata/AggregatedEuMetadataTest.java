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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import se.swedenconnect.opensaml.OpenSAMLInitializer;
import se.swedenconnect.opensaml.OpenSAMLSecurityDefaultsConfig;
import se.swedenconnect.opensaml.OpenSAMLSecurityExtensionConfig;
import se.swedenconnect.opensaml.xmlsec.config.DefaultSecurityConfiguration;

/**
 * Test cases for {@code AggregatedEuMetadata}.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class AggregatedEuMetadataTest {
  
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Before
  public void setup() throws Exception {
    OpenSAMLInitializer.getInstance().initialize(
      new OpenSAMLSecurityDefaultsConfig(new DefaultSecurityConfiguration()),
      new OpenSAMLSecurityExtensionConfig());
  }

  @Test
  public void testLocal() throws Exception {
    Resource mdFile = new ClassPathResource("aggregate-simple.xml");
    
    // The aggregated metadata has two countries, SE and XA. XA is marked with "hide-from-discovery".
    
    AggregatedEuMetadataImpl euMetadata = new AggregatedEuMetadataImpl();
    euMetadata.setCacheDirectory(tempDir.getRoot().getAbsolutePath());
    euMetadata.setEuMetadataUrl("file://" + mdFile.getFile().getAbsolutePath());
    euMetadata.setIgnoreSignatureValidation(true);
    euMetadata.afterPropertiesSet();
    
    Countries countries = euMetadata.getCountries();
    List<Country> _countries = countries.getCountries(Collections.emptyList());
    Assert.assertTrue("Expected one country", _countries.size() == 1);
    Assert.assertTrue("Expected SE", _countries.stream().map(Country::getCountryCode).collect(Collectors.toList()).contains("SE"));
    
    // If we explictly request XA, we should get it.
    _countries = countries.getCountries(Arrays.asList("XA"));
    Assert.assertTrue("Expected one country", _countries.size() == 1);
    Assert.assertTrue("Expected XA", _countries.stream().map(Country::getCountryCode).collect(Collectors.toList()).contains("XA"));
  }
    
  @Test
  public void testLarge() throws Exception {
    Resource mdFile = new ClassPathResource("eu-metadata.xml");
    
    AggregatedEuMetadataImpl euMetadata = new AggregatedEuMetadataImpl();
    euMetadata.setCacheDirectory(tempDir.getRoot().getAbsolutePath());
    euMetadata.setEuMetadataUrl("file://" + mdFile.getFile().getAbsolutePath());
    euMetadata.setIgnoreSignatureValidation(true);
    euMetadata.afterPropertiesSet();
    
    Countries countries = euMetadata.getCountries();
    Collection<Country> _countries = countries.getCountries(Collections.emptyList());
    Assert.assertTrue("Expected 6 countries", _countries.size() == 6);
  }  
  
}
