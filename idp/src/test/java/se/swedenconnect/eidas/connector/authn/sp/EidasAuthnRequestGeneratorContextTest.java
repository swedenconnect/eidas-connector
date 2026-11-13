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
package se.swedenconnect.eidas.connector.authn.sp;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.metadata.NameIDFormat;
import se.swedenconnect.eidas.connector.OpenSamlTestBase;
import se.swedenconnect.opensaml.xmlsec.config.SecurityConfiguration;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for {@link EidasAuthnRequestGeneratorContext}.
 *
 * @author Martin Lindström
 */
class EidasAuthnRequestGeneratorContextTest extends OpenSamlTestBase {

  private static final List<String> SUPPORTED_NAME_IDS =
      List.of(NameID.PERSISTENT, NameID.TRANSIENT, NameID.UNSPECIFIED);

  @Test
  void testFirstSupportedFormatListedByCountry() {
    final NameIDPolicy policy = createContext().getNameIDPolicyBuilderFunction()
        .apply(List.of(nameIdFormat(NameID.UNSPECIFIED), nameIdFormat(NameID.TRANSIENT)));
    assertThat(policy.getFormat()).isEqualTo(NameID.TRANSIENT);
    assertThat(policy.getAllowCreate()).isTrue();
  }

  @Test
  void testCountryListsNoFormats() {
    assertThat(createContext().getNameIDPolicyBuilderFunction().apply(Collections.emptyList()).getFormat())
        .isEqualTo(NameID.PERSISTENT);
    assertThat(createContext().getNameIDPolicyBuilderFunction().apply(null).getFormat())
        .isEqualTo(NameID.PERSISTENT);
  }

  @Test
  void testNoSupportedFormatListedByCountry() {
    final NameIDPolicy policy = createContext().getNameIDPolicyBuilderFunction()
        .apply(List.of(nameIdFormat(NameID.EMAIL)));
    assertThat(policy.getFormat()).isEqualTo(NameID.PERSISTENT);
  }

  private static EidasAuthnRequestGeneratorContext createContext() {
    return new EidasAuthnRequestGeneratorContext("DE", "https://sp.example.com", null, null,
        Collections.emptyList(), null, null, Mockito.mock(SecurityConfiguration.class), SUPPORTED_NAME_IDS);
  }

  private static NameIDFormat nameIdFormat(final String format) {
    final NameIDFormat nameIdFormat =
        (NameIDFormat) XMLObjectSupport.buildXMLObject(NameIDFormat.DEFAULT_ELEMENT_NAME);
    nameIdFormat.setURI(format);
    return nameIdFormat;
  }

}
