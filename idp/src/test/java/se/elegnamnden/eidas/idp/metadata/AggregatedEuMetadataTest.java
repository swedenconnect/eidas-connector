/*
 * Copyright 2017-2018 Sweden Connect
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

import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import se.litsec.opensaml.config.OpenSAMLInitializer;

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
    OpenSAMLInitializer.getInstance().initialize();
  }

  @Test
  public void testLocal() throws Exception {
    Resource mdFile = new ClassPathResource("aggregate-simple.xml");
    
    AggregatedEuMetadataImpl euMetadata = new AggregatedEuMetadataImpl();
    euMetadata.setCacheDirectory(tempDir.getRoot().getAbsolutePath());
    euMetadata.setEuMetadataUrl("file://" + mdFile.getFile().getAbsolutePath());
    euMetadata.setIgnoreSignatureValidation(true);
    euMetadata.afterPropertiesSet();
    
    Collection<String> countries = euMetadata.getCountries();
    Assert.assertTrue("Expected two countries", countries.size() == 2);
    Assert.assertTrue("Expected SE", countries.contains("SE"));
    Assert.assertTrue("Expected XA", countries.contains("XA"));
  }
  
  @Test
  public void testLocalMdslWrapper() throws Exception {
    Resource mdFile = new ClassPathResource("aggregate-simple.xml");
    
    MdslAggregatedEuMetadata euMetadata = new MdslAggregatedEuMetadata();
    euMetadata.setCacheDirectory(tempDir.getRoot().getAbsolutePath());
    euMetadata.setEuMetadataUrl("file://" + mdFile.getFile().getAbsolutePath());
    euMetadata.setIgnoreSignatureValidation(true);
    euMetadata.afterPropertiesSet();
    euMetadata.setMdslUrl("");
    
    Collection<String> countries = euMetadata.getCountries();
    Assert.assertTrue("Expected two countries", countries.size() == 2);
    Assert.assertTrue("Expected SE", countries.contains("SE"));
    Assert.assertTrue("Expected XA", countries.contains("XA"));
  }
  
  @Test
  public void testLarge() throws Exception {
    Resource mdFile = new ClassPathResource("eu-metadata.xml");
    
    AggregatedEuMetadataImpl euMetadata = new AggregatedEuMetadataImpl();
    euMetadata.setCacheDirectory(tempDir.getRoot().getAbsolutePath());
    euMetadata.setEuMetadataUrl("file://" + mdFile.getFile().getAbsolutePath());
    euMetadata.setIgnoreSignatureValidation(true);
    euMetadata.afterPropertiesSet();
    
    Collection<String> countries = euMetadata.getCountries();
    Assert.assertTrue("Expected 6 countries", countries.size() == 6);
  }  
  
}
