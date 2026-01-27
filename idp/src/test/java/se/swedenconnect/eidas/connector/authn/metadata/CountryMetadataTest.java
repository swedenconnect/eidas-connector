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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import se.swedenconnect.eidas.connector.OpenSamlTestBase;
import se.swedenconnect.opensaml.saml2.metadata.provider.MetadataProvider;
import se.swedenconnect.opensaml.sweid.saml2.authn.LevelOfAssuranceUris;

import java.util.List;

/**
 * Test cases for the {@link CountryMetadata} class.
 *
 * @author Martin Lindström
 */
public class CountryMetadataTest extends OpenSamlTestBase {

  @Test
  void testCanAuthenticate() throws Exception {
    final MetadataProvider metadata = DefaultEuMetadataProviderTest.createProvider("metadata/eu-metadata-idps.xml");
    final DefaultEuMetadataProvider provider = new DefaultEuMetadataProvider(metadata, Mockito.mock(
        ApplicationEventPublisher.class));

    final CountryMetadata spain = provider.getCountry("ES");
    Assertions.assertNotNull(spain);

    Assertions.assertFalse(spain.canAuthenticate(List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF)));
    Assertions.assertTrue(spain.canAuthenticate(List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF,
        LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH)));
    Assertions.assertTrue(spain.canAuthenticate(List.of(LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF,
        LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH_NF)));
  }

}
