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
package se.swedenconnect.eidas.simulator.authn;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.ModelAndView;
import se.swedenconnect.eidas.simulator.OpenSamlTestBase;
import se.swedenconnect.eidas.simulator.TestConnector;
import se.swedenconnect.eidas.simulator.TestCountries;
import se.swedenconnect.opensaml.xmlsec.encryption.support.SAMLObjectDecrypter;
import se.swedenconnect.security.credential.opensaml.OpenSamlCredential;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the simulator application through its web endpoints.
 *
 * @author Martin Lindström
 */
@SpringBootTest
@AutoConfigureMockMvc
class SimulatorControllerTest extends OpenSamlTestBase {

  private static TestConnector connector;

  private static Path metadataFile;

  @Autowired
  private MockMvc mvc;

  @BeforeAll
  static void setup() throws Exception {
    connector = new TestConnector();
    metadataFile = connector.writeMetadata(Files.createTempDirectory("simulator"), true);
  }

  @DynamicPropertySource
  static void properties(final DynamicPropertyRegistry registry) {
    registry.add("simulator.base-url", () -> TestCountries.BASE_URL);
    registry.add("simulator.keystore.location", () -> "classpath:simulator-keys.jks");
    registry.add("simulator.keystore.password", () -> "secret");
    registry.add("simulator.connector.metadata-url", () -> metadataFile.toUri().toString());
    registry.add("simulator.connector.validation-certificate", () -> "classpath:test-connector-sign.crt");
  }

  @Test
  void testMetadata() throws Exception {
    this.mvc.perform(get("/metadata"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/samlmetadata+xml"));
    this.mvc.perform(get("/at/ServiceMetadata")).andExpect(status().isOk());
    this.mvc.perform(get("/xx/ServiceMetadata")).andExpect(status().isNotFound());
  }

  @Test
  void testStartPage() throws Exception {
    final String page = this.mvc.perform(get("/")).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertThat(page).contains("Austria (AT)").contains("Spain (ES)");
  }

  @Test
  void testAuthentication() throws Exception {
    final MockHttpSession session = new MockHttpSession();
    final String requestPage = this.sendRequest("hr", session);

    // The page of the request shows the users of the country
    final String page = this.mvc.perform(get(requestPage).session(session))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertThat(page).contains("Simulated eIDAS node, Croatia").contains("Ivan Horvat").contains("Ana Kovačević");

    // The tester changes the given name and selects substantial
    final MvcResult result = this.mvc.perform(post(requestPage).session(session)
            .param("action", "authenticate")
            .param("userId", "ivan")
            .param("user.id", "ivan")
            .param("user.personIdentifier", "69482513073")
            .param("user.givenName", "Ivo")
            .param("user.familyName", "Horvat")
            .param("user.dateOfBirth", "1980-07-19")
            .param("loa", "http://eidas.europa.eu/LoA/substantial"))
        .andExpect(status().isOk())
        .andReturn();
    final ModelAndView mav = Objects.requireNonNull(result.getModelAndView());
    assertThat(mav.getViewName()).isEqualTo("post-response");
    assertThat(mav.getModel().get("action")).isEqualTo(TestConnector.ACS_URL);
    assertThat(mav.getModel().get("RelayState")).isEqualTo("relay");

    final Response response = decode((String) mav.getModel().get("SAMLResponse"));
    assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.SUCCESS);
    final Assertion assertion = new SAMLObjectDecrypter(new OpenSamlCredential(connector.getEncryptCredential()))
        .decrypt(response.getEncryptedAssertions().getFirst(), Assertion.class);
    assertThat(assertion.getAuthnStatements().getFirst().getAuthnContext().getAuthnContextClassRef().getURI())
        .isEqualTo("http://eidas.europa.eu/LoA/substantial");
    assertThat(assertion.getAttributeStatements().getFirst().getAttributes())
        .filteredOn(a -> "FirstName".equals(a.getFriendlyName()))
        .singleElement()
        .satisfies(a -> assertThat(((org.opensaml.core.xml.schema.XSString) a.getAttributeValues().getFirst())
            .getValue()).isEqualTo("Ivo"));

    // The request has been answered
    this.mvc.perform(get(requestPage).session(session)).andExpect(status().isNotFound());
  }

  @Test
  void testOnlyReleasableAttributesAreShown() throws Exception {
    // BirthName is requested, but EE does not support it
    final MockHttpSession session = new MockHttpSession();
    final String page = this.mvc.perform(get(this.sendRequest("ee", session)).session(session))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertThat(page).contains("PersonIdentifier").contains("Date of birth").doesNotContain("Birth name")
        .doesNotContain("Place of birth");

    final MockHttpSession hrSession = new MockHttpSession();
    assertThat(this.mvc.perform(get(this.sendRequest("hr", hrSession)).session(hrSession))
        .andReturn().getResponse().getContentAsString()).contains("Birth name").doesNotContain("Place of birth");
  }

  @Test
  void testMissingRequiredValueAndLoa() throws Exception {
    final MockHttpSession session = new MockHttpSession();
    final String requestPage = this.sendRequest("hr", session);

    final String page = this.mvc.perform(post(requestPage).session(session)
            .param("action", "authenticate")
            .param("userId", "new")
            .param("user.givenName", "Someone"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertThat(page)
        .contains("Select a level of assurance")
        .contains("A value is required for PersonIdentifier")
        .contains("A value is required for DateOfBirth");
  }

  @Test
  void testSaveCustomUser() throws Exception {
    final MockHttpSession session = new MockHttpSession();
    final String requestPage = this.sendRequest("pt", session);

    final MvcResult result = this.mvc.perform(post(requestPage).session(session)
            .param("action", "authenticate")
            .param("userId", "new")
            .param("saveUser", "true")
            .param("user.personIdentifier", "87654321")
            .param("user.givenName", "Inês")
            .param("user.familyName", "Rocha")
            .param("user.dateOfBirth", "1999-12-31")
            .param("loa", "http://eidas.europa.eu/LoA/high"))
        .andExpect(status().isOk())
        .andReturn();
    final Cookie cookie = result.getResponse().getCookie(SavedUsersCookie.getCookieName("pt"));
    assertThat(cookie).isNotNull();
    assertThat(cookie.getMaxAge()).isPositive();

    // The saved user can be selected for the next request, also in a new session
    final MockHttpSession newSession = new MockHttpSession();
    final String next = this.sendRequest("pt", newSession);
    final String page = this.mvc.perform(get(next).session(newSession).cookie(cookie))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertThat(page).contains("Inês Rocha");

    // But not for another country
    final String other = this.sendRequest("es", newSession);
    final String otherPage = this.mvc.perform(get(other).session(newSession).cookie(cookie))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    assertThat(otherPage).doesNotContain("Inês Rocha");
  }

  @Test
  void testErrorResponseFromTester() throws Exception {
    final MockHttpSession session = new MockHttpSession();
    final String requestPage = this.sendRequest("fi", session);

    final MvcResult result = this.mvc.perform(post(requestPage).session(session)
            .param("action", "error")
            .param("errorOption", "AUTHN_FAILED")
            .param("statusMessage", "Card blocked"))
        .andExpect(status().isOk())
        .andReturn();
    final Response response =
        decode((String) Objects.requireNonNull(result.getModelAndView()).getModel().get("SAMLResponse"));
    assertThat(response.getStatus().getStatusCode().getValue()).isEqualTo(StatusCode.RESPONDER);
    assertThat(response.getStatus().getStatusCode().getStatusCode().getValue()).isEqualTo(StatusCode.AUTHN_FAILED);
    assertThat(response.getStatus().getStatusMessage().getValue()).isEqualTo("Card blocked");
  }

  @Test
  void testErrorResponseForInvalidRequest() throws Exception {
    final AuthnRequest request = connector.createRequest(TestCountries.repository("AT").getCountry("AT"));
    request.setIsPassive(true);
    final MvcResult result = this.mvc.perform(post("/at/ColleagueRequest")
            .param("SAMLRequest", connector.encodePost(request)))
        .andExpect(status().isOk())
        .andReturn();
    final ModelAndView mav = Objects.requireNonNull(result.getModelAndView());
    assertThat(mav.getViewName()).isEqualTo("post-response");
    final Response response = decode((String) mav.getModel().get("SAMLResponse"));
    assertThat(response.getStatus().getStatusCode().getStatusCode().getValue()).isEqualTo(StatusCode.NO_PASSIVE);
  }

  @Test
  void testErrorPage() throws Exception {
    final AuthnRequest request = connector.createRequest(TestCountries.repository("AT").getCountry("AT"));
    final String page = this.mvc.perform(post("/at/ColleagueRequest")
            .param("SAMLRequest", connector.encodePost(request, null)))
        .andExpect(status().isBadRequest())
        .andReturn().getResponse().getContentAsString();
    assertThat(page).contains("The AuthnRequest is not signed");

    this.mvc.perform(post("/xx/ColleagueRequest").param("SAMLRequest", "x"))
        .andExpect(status().isNotFound());
  }

  /**
   * Sends a request to a country and returns the path of the page where the tester answers it.
   */
  private String sendRequest(final String country, final MockHttpSession session) throws Exception {
    final AuthnRequest request = connector.createRequest(
        TestCountries.repository(country).getCountry(country));
    final MvcResult result = this.mvc.perform(post("/%s/ColleagueRequest".formatted(country)).session(session)
            .param("SAMLRequest", connector.encodePost(request))
            .param("RelayState", "relay"))
        .andExpect(status().is3xxRedirection())
        .andReturn();
    final String location = Objects.requireNonNull(result.getResponse().getRedirectedUrl());
    assertThat(location).startsWith("/%s/authn/".formatted(country));
    return location;
  }

  private static Response decode(final String samlResponse) throws Exception {
    return (Response) XMLObjectSupport.unmarshallFromInputStream(XMLObjectProviderRegistrySupport.getParserPool(),
        new ByteArrayInputStream(Base64.getDecoder().decode(samlResponse)));
  }

}
