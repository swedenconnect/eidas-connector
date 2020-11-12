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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;

import se.litsec.eidas.opensaml.ext.NodeCountry;
import se.litsec.opensaml.saml2.metadata.build.IdpEntityDescriptorBuilder;
import se.litsec.opensaml.utils.ObjectUtils;
import se.swedenconnect.opensaml.OpenSAMLInitializer;

/**
 * Test cases for the {@code Countries} class.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class CountriesTest {

  @Before
  public void setup() throws Exception {
    OpenSAMLInitializer.getInstance().initialize();
  }

  @Test
  public void testEmpty() {
    Countries c = new Countries(Collections.emptyList());
    Assert.assertTrue(c.isEmpty());
    Assert.assertTrue(c.getCountries(Collections.emptyList()).isEmpty());
  }

  @Test
  public void testSimple() {
    Countries c = new Countries(Arrays.asList(
      build("SE", false), build("NO", false), build("DK", false)));

    Assert.assertFalse(c.isEmpty());
    List<Country> countries = c.getCountries(Collections.emptyList());
    Assert.assertEquals(Arrays.asList("SE", "NO", "DK"), countries.stream().map(Country::getCountryCode).collect(Collectors.toList()));
  }

  @Test
  public void testHide() {
    Countries c = new Countries(Arrays.asList(
      build("SE", false), build("NO", true), build("DK", false)));
    
    Assert.assertFalse(c.isEmpty());
    Assert.assertEquals(Arrays.asList("SE", "DK"), c.getCountries(Collections.emptyList())
      .stream()
      .map(Country::getCountryCode)
      .collect(Collectors.toList()));
  }

  @Test
  public void testRequested() {
    Countries c = new Countries(Arrays.asList(
      build("SE", false), build("NO", false), build("DK", false)));

    Assert.assertFalse(c.isEmpty());
    Assert.assertEquals(Arrays.asList("NO"), c.getCountries(Arrays.asList("no", "de")).stream().map(Country::getCountryCode).collect(Collectors.toList()));

    c = new Countries(Arrays.asList(
      build("SE", false), build("NO", true), build("DK", false)));

    Assert.assertFalse(c.isEmpty());
    Assert.assertEquals(Arrays.asList("NO"), c.getCountries(Arrays.asList("no", "de")).stream().map(Country::getCountryCode).collect(Collectors.toList()));

    c = new Countries(Arrays.asList(
      build("SE", false), build("NO", true), build("DK", false)));

    Assert.assertFalse(c.isEmpty());
    Assert.assertEquals(Arrays.asList("NO", "DK"), c.getCountries(Arrays.asList("no", "dk", "de")).stream().map(Country::getCountryCode).collect(Collectors.toList()));
  }

  private static Country build(final String country, final boolean hide, String... assuranceLevels) {
    Extensions exts = ObjectUtils.createSamlObject(Extensions.class);
    NodeCountry nc = ObjectUtils.createSamlObject(NodeCountry.class);
    nc.setNodeCountry(country);
    exts.getUnknownXMLObjects().add(nc);

    EntityDescriptor ed = IdpEntityDescriptorBuilder.builder()
      .assuranceCertificationUris(assuranceLevels)
      .entityCategories(hide ? MetadataFunctions.HIDE_FROM_DISCOVERY_ENTITY_CATEGORY : null)
      .wantAuthnRequestsSigned(true)
      .build();

    ed.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).setExtensions(exts);

    return new Country(ed);
  }

}
