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
package se.swedenconnect.eidas.connector.authn;

import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import se.swedenconnect.eidas.connector.OpenSamlTestBase;
import se.swedenconnect.opensaml.saml2.core.build.AuthnRequestBuilder;
import se.swedenconnect.opensaml.saml2.core.build.NameIDPolicyBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.EntityDescriptorBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.SPSSODescriptorBuilder;
import se.swedenconnect.spring.authnserver.attributes.AttributeIdentifiers;
import se.swedenconnect.spring.authnserver.attributes.GenericAttribute;
import se.swedenconnect.spring.authnserver.authentication.AuthenticatedUser;
import se.swedenconnect.spring.authnserver.authentication.AuthenticationProtocol;
import se.swedenconnect.spring.authnserver.authentication.Requester;
import se.swedenconnect.spring.authnserver.saml.nameid.DefaultNameIDGeneratorFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts that the persistent {@code NameID} for a given user and Service Provider is the same as the one that version
 * 2.1.0 of the connector issued. The base configuration turns the subject identifier secret off, so the authentication
 * server computes the identifier without a secret.
 *
 * @author Martin Lindström
 */
class PersistentNameIdCompatibilityTest extends OpenSamlTestBase {

  private static final String IDP = "https://connector.eidas.swedenconnect.se/eidas";

  private static final String SP = "https://sp.example.com";

  private static final String PRID = "ES:12345678Z";

  @Test
  void testPersistentNameIdFromMetadata() throws Exception {
    final NameID nameId = nameId(AuthnRequestBuilder.builder().id("_1").issuer(SP).build());
    assertThat(nameId.getFormat()).isEqualTo(NameID.PERSISTENT);
    assertThat(nameId.getValue()).isEqualTo(saml2IdpNameId(SP, PRID));
    assertThat(nameId.getNameQualifier()).isEqualTo(IDP);
    assertThat(nameId.getSPNameQualifier()).isEqualTo(SP);
  }

  @Test
  void testPersistentNameIdFromPolicy() throws Exception {
    final NameID nameId = nameId(AuthnRequestBuilder.builder().id("_1").issuer(SP)
        .nameIDPolicy(NameIDPolicyBuilder.builder().format(NameID.PERSISTENT).build())
        .build());
    assertThat(nameId.getValue()).isEqualTo(saml2IdpNameId(SP, PRID));
  }

  @Test
  void testPersistentNameIdWithSpNameQualifier() throws Exception {
    final String group = "https://group.example.com";
    final NameID nameId = nameId(AuthnRequestBuilder.builder().id("_1").issuer(SP)
        .nameIDPolicy(NameIDPolicyBuilder.builder().format(NameID.PERSISTENT).spNameQualifier(group).build())
        .build());
    assertThat(nameId.getValue()).isEqualTo(saml2IdpNameId(group, PRID));
  }

  private static NameID nameId(final AuthnRequest authnRequest) {
    final EntityDescriptor spMetadata = EntityDescriptorBuilder.builder()
        .entityID(SP)
        .ssoDescriptor(SPSSODescriptorBuilder.builder().nameIDFormats(NameID.PERSISTENT).build())
        .build();

    // No secret is assigned, as with an empty authn-server.saml.subject-identifier.secret ...
    final DefaultNameIDGeneratorFactory factory = new DefaultNameIDGeneratorFactory(IDP);

    final AuthenticatedUser user = new AuthenticatedUser(
        List.of(GenericAttribute.of(AttributeIdentifiers.PRID, PRID)), AttributeIdentifiers.PRID,
        "http://id.elegnamnden.se/loa/1.0/eidas-sub", Instant.now(), "127.0.0.1");
    return factory.getNameIDGenerator(authnRequest, spMetadata)
        .getNameID(user, new Requester(AuthenticationProtocol.SAML, SP));
  }

  /**
   * The computation of saml-identity-provider, the library that version 2.1.0 of the connector was built on.
   */
  private static String saml2IdpNameId(final String spNameQualifier, final String userId) throws Exception {
    final MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(spNameQualifier.getBytes(StandardCharsets.UTF_8));
    md.update((byte) '!');
    md.update(IDP.getBytes(StandardCharsets.UTF_8));
    md.update((byte) '!');
    md.update(userId.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(md.digest());
  }

}
