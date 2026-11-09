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
import org.springframework.util.StringUtils;
import se.swedenconnect.spring.authnserver.attributes.AttributeIdentifiers;
import se.swedenconnect.spring.authnserver.attributes.GenericAttribute;
import se.swedenconnect.spring.authnserver.authentication.AuthenticationRequirements;
import se.swedenconnect.spring.authnserver.authentication.Requester;
import se.swedenconnect.spring.authnserver.authentication.UserAuthentication;
import se.swedenconnect.spring.authnserver.sso.SsoDecision;
import se.swedenconnect.spring.authnserver.sso.SsoDenialReason;
import se.swedenconnect.spring.authnserver.sso.SsoVoter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An SSO voter that prevents SSO if one or more countries were selected and those do not match the country from the
 * previous authentication.
 *
 * @author Martin Lindström
 */
public class EidasSsoVoter implements SsoVoter {

  /** {@inheritDoc} */
  @Override
  public @NonNull SsoDecision vote(final @NonNull UserAuthentication previousAuthentication,
      final @NonNull AuthenticationRequirements requirements, final @NonNull Requester requester,
      final @NonNull List<String> allowedAuthnContexts) {

    final List<String> requestedCountries = requirements.getRequestedAuthnProviders().stream()
        .filter(Objects::nonNull)
        .filter(p -> p.startsWith(EidasCountryHandler.COUNTRY_URI_PREFIX))
        .map(p -> p.substring(EidasCountryHandler.COUNTRY_URI_PREFIX.length()))
        .filter(StringUtils::hasText)
        .toList();

    if (requestedCountries.isEmpty()) {
      return SsoDecision.allow();
    }

    final String previousCountry = Optional.ofNullable(
            previousAuthentication.getAuthenticatedUser().getAttribute(AttributeIdentifiers.COUNTRY))
        .map(GenericAttribute::getStringValues)
        .filter(v -> !v.isEmpty())
        .map(List::getFirst)
        .orElse(null);

    if (previousCountry == null) {
      return SsoDecision.abstain();
    }

    return requestedCountries.contains(previousCountry)
        ? SsoDecision.allow()
        : SsoDecision.deny(SsoDenialReason.NOT_ALLOWED);
  }

}
