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

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.core.StatusCode;
import org.springframework.util.SerializationUtils;
import se.swedenconnect.spring.authnserver.error.AuthenticationErrorException;
import se.swedenconnect.spring.authnserver.oidc.error.OidcErrorMapping;
import se.swedenconnect.spring.authnserver.saml.error.SamlErrorStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EidasAuthenticationError}.
 *
 * @author Martin Lindström
 */
class EidasAuthenticationErrorTest {

  @Test
  void testSamlStatus() {
    assertThat(SamlErrorStatus.of(EidasAuthenticationError.NO_AVAILABLE_COUNTRY))
        .isEqualTo(new SamlErrorStatus(StatusCode.REQUESTER, StatusCode.NO_AVAILABLE_IDP));
    assertThat(SamlErrorStatus.of(EidasAuthenticationError.REQUEST_UNSUPPORTED))
        .isEqualTo(new SamlErrorStatus(StatusCode.REQUESTER, StatusCode.REQUEST_UNSUPPORTED));
    assertThat(SamlErrorStatus.of(EidasAuthenticationError.REQUEST_DENIED))
        .isEqualTo(new SamlErrorStatus(StatusCode.REQUESTER, StatusCode.REQUEST_DENIED));
    assertThat(SamlErrorStatus.of(EidasAuthenticationError.foreignStatus(StatusCode.RESPONDER, StatusCode.NO_PASSIVE)))
        .isEqualTo(new SamlErrorStatus(StatusCode.RESPONDER, StatusCode.NO_PASSIVE));
  }

  @Test
  void testOidcError() {
    final ErrorObject error = OidcErrorMapping.of(
        new AuthenticationErrorException(EidasAuthenticationError.REQUEST_UNSUPPORTED, "Both messages given"));
    assertThat(error.getCode()).isEqualTo(OAuth2Error.INVALID_REQUEST_CODE);
    assertThat(error.getDescription()).isEqualTo("Both messages given");

    assertThat(OidcErrorMapping.of(EidasAuthenticationError.NO_AVAILABLE_COUNTRY, null).getCode())
        .isEqualTo(OAuth2Error.ACCESS_DENIED_CODE);
    assertThat(OidcErrorMapping.of(EidasAuthenticationError.SIGN_MESSAGE_NOT_SUPPORTED, null).getCode())
        .isEqualTo(OAuth2Error.INVALID_REQUEST_CODE);
    assertThat(OidcErrorMapping.of(
        EidasAuthenticationError.foreignStatus(StatusCode.RESPONDER, StatusCode.AUTHN_FAILED), null).getCode())
        .isEqualTo(OAuth2Error.ACCESS_DENIED_CODE);
  }

  @Test
  void testSerialization() {
    final EidasAuthenticationError error =
        EidasAuthenticationError.foreignStatus(StatusCode.RESPONDER, StatusCode.AUTHN_FAILED);
    @SuppressWarnings("deprecation")
    final Object copy = SerializationUtils.deserialize(SerializationUtils.serialize(error));
    assertThat(copy).isEqualTo(error);
  }

}
