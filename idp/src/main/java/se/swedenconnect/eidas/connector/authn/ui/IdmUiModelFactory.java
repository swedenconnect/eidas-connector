/*
 * Copyright 2017-2024 Sweden Connect
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

import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;

/**
 * Factory for creating {@link IdmUiModel} objects.
 *
 * @author Martin Lindström
 */
public class IdmUiModelFactory extends AbstractUiModelFactory<IdmUiModel> {

  /**
   * Constructor.
   *
   * @param languageHandler the UI language handler
   * @param accessibilityUrl the accessibility URL
   */
  public IdmUiModelFactory(final UiLanguageHandler languageHandler, final String accessibilityUrl) {
    super(languageHandler, accessibilityUrl);
  }

  /**
   * Creates an {@link IdmUiModel}.
   *
   * @param inputToken the input token
   * @return an {@link IdmUiModel}
   */
  public IdmUiModel createUiModel(final Saml2UserAuthenticationInputToken inputToken) {
    final IdmUiModel model = new IdmUiModel();
    this.initModel(model, inputToken);
    return model;
  }

}
