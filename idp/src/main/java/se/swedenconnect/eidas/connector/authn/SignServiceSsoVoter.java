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

import org.jspecify.annotations.NonNull;
import se.swedenconnect.opensaml.sweid.saml2.metadata.entitycategory.EntityCategoryConstants;
import se.swedenconnect.spring.authnserver.authentication.AuthenticationRequirements;
import se.swedenconnect.spring.authnserver.authentication.Requester;
import se.swedenconnect.spring.authnserver.authentication.UserAuthentication;
import se.swedenconnect.spring.authnserver.saml.authentication.SamlAuthenticationRequirements;
import se.swedenconnect.spring.authnserver.sso.SsoDecision;
import se.swedenconnect.spring.authnserver.sso.SsoDenialReason;
import se.swedenconnect.spring.authnserver.sso.SsoVoter;

import java.util.List;

/**
 * An SSO voter that never lets a signature service reuse a previous authentication. A user that signs always
 * authenticates and consents to the signature.
 *
 * @author Martin Lindström
 */
public class SignServiceSsoVoter implements SsoVoter {

  /** {@inheritDoc} */
  @Override
  public @NonNull SsoDecision vote(final @NonNull UserAuthentication previousAuthentication,
      final @NonNull AuthenticationRequirements requirements, final @NonNull Requester requester,
      final @NonNull List<String> allowedAuthnContexts) {
    return isSignatureService(requirements)
        ? SsoDecision.deny(SsoDenialReason.NOT_ALLOWED)
        : SsoDecision.abstain();
  }

  /**
   * Tells whether the requirements come from a signature service, that is, a Service Provider that declares the
   * signature service entity category.
   *
   * @param requirements the authentication requirements
   * @return {@code true} if the requester is a signature service, and {@code false} otherwise
   */
  public static boolean isSignatureService(final @NonNull AuthenticationRequirements requirements) {
    return requirements instanceof final SamlAuthenticationRequirements samlRequirements
        && samlRequirements.getEntityCategories()
        .contains(EntityCategoryConstants.SERVICE_TYPE_CATEGORY_SIGSERVICE.getUri());
  }

}
