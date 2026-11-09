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
package se.swedenconnect.eidas.connector.authn.ui;

import org.jspecify.annotations.NonNull;
import org.springframework.context.i18n.LocaleContextHolder;
import se.swedenconnect.eidas.connector.authn.EidasAuthenticationToken;
import se.swedenconnect.eidas.connector.authn.ui.SignUiModel.UserInfo;
import se.swedenconnect.eidas.connector.idp.SignMessageProcessor;
import se.swedenconnect.spring.authnserver.authentication.provider.UserAuthenticationInputToken;

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
  public SignUiModelFactory(final @NonNull UiLanguageHandler languageHandler, final @NonNull String accessibilityUrl) {
    super(languageHandler, accessibilityUrl);
  }

  /**
   * Creates a {@link SignUiModel}.
   *
   * @param inputToken the input token
   * @param eidasToken the eIDAS token
   * @return a {@link SignUiModel}
   */
  public @NonNull SignUiModel createUiModel(
      final @NonNull UserAuthenticationInputToken inputToken, final @NonNull EidasAuthenticationToken eidasToken) {

    final SignUiModel model = new SignUiModel();
    this.initModel(model, inputToken);

    model.setTextMessage(SignMessageProcessor.toDisplayMessage(
        inputToken.getAuthnRequirements().getSignMessage(), LocaleContextHolder.getLocale().getLanguage()));
    model.setUserInfo(new UserInfo(eidasToken));

    return model;
  }

}
