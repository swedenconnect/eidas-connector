/*
 * Copyright 2017-2019 Sweden Connect
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
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import se.elegnamnden.eidas.idp.metadata.Countries.CountryEntry;

/**
 * Test cases for the {@code Countries} class.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class CountriesTest {

  @Test
  public void testEmpty() {
    Countries c = new Countries(Collections.emptyList());
    Assert.assertTrue(c.isEmpty());
    Assert.assertTrue(c.getCountries(Collections.emptyList()).isEmpty());
  }

  @Test
  public void testSimple() {
    Countries c = new Countries(Arrays.asList(
      new CountryEntry("SE", false), new CountryEntry("NO", false), new CountryEntry("DK", false)));
    
    Assert.assertFalse(c.isEmpty());
    Assert.assertEquals(Arrays.asList("SE", "NO", "DK"), c.getCountries(Collections.emptyList()));
  }
  
  @Test
  public void testHide() {
    Countries c = new Countries(Arrays.asList(
      new CountryEntry("SE", false), new CountryEntry("NO", true), new CountryEntry("DK", false)));
    
    Assert.assertFalse(c.isEmpty());
    Assert.assertEquals(Arrays.asList("SE", "DK"), c.getCountries(Collections.emptyList()));
  }
  
  @Test
  public void testRequested() {
    Countries c = new Countries(Arrays.asList(
      new CountryEntry("SE", false), new CountryEntry("NO", false), new CountryEntry("DK", false)));
    
    Assert.assertFalse(c.isEmpty());
    Assert.assertEquals(Arrays.asList("NO"), c.getCountries(Arrays.asList("no", "de")));
    
    c = new Countries(Arrays.asList(
      new CountryEntry("SE", false), new CountryEntry("NO", true), new CountryEntry("DK", false)));
    
    Assert.assertFalse(c.isEmpty());
    Assert.assertEquals(Arrays.asList("NO"), c.getCountries(Arrays.asList("no", "de")));
    
    c = new Countries(Arrays.asList(
      new CountryEntry("SE", false), new CountryEntry("NO", true), new CountryEntry("DK", false)));
    
    Assert.assertFalse(c.isEmpty());
    Assert.assertEquals(Arrays.asList("NO", "DK"), c.getCountries(Arrays.asList("no", "dk", "de")));
  }
    
}
