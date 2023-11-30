/*
 * Copyright 2023 Sweden Connect
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
package se.swedenconnect.eidas.connector.authn.ui;

import java.util.Optional;

import se.swedenconnect.eidas.connector.authn.EidasAuthenticationToken;
import se.swedenconnect.eidas.connector.authn.ui.SignUiModel.UserInfo;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;
import se.swedenconnect.spring.saml.idp.extensions.SignatureMessageExtension;

/**
 * Factory for creating model objects for the sign consent view.
 *
 * @author Martin Lindström
 */
public class SignUiModelFactory extends AbstractUiModelFactory<SignUiModel> {

  /**
   * Constructor.
   *
   * @param languageHandler the UI language handler
   * @param accessibilityUrl the accessibility URL
   */
  public SignUiModelFactory(final UiLanguageHandler languageHandler, final String accessibilityUrl) {
    super(languageHandler, accessibilityUrl);
  }

  /**
   * Creates a {@link SignUiModel}.
   *
   * @param inputToken the SAML input token
   * @param eidasToken the eIDAS token
   * @return a {@link SignUiModel}
   */
  public SignUiModel createUiModel(
      final Saml2UserAuthenticationInputToken inputToken, final EidasAuthenticationToken eidasToken) {

    final SignUiModel model = new SignUiModel();
    this.initModel(model, inputToken);

    model.setTextMessage(
        Optional.ofNullable(inputToken.getAuthnRequirements().getSignatureMessageExtension())
            .map(SignatureMessageExtension::getProcessedMessage)
            .orElse(null));
    model.setUserInfo(new UserInfo(eidasToken.getAttributes()));

    return model;
  }

}
