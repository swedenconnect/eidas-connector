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
package se.swedenconnect.eidas.connector.authn;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.opensaml.saml.saml2.core.IDPEntry;
import org.opensaml.saml.saml2.core.Scoping;
import org.springframework.util.StringUtils;

import se.swedenconnect.opensaml.sweid.saml2.attribute.AttributeConstants;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthentication;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthenticationInputToken;
import se.swedenconnect.spring.saml.idp.authentication.provider.SsoVoter;

/**
 * An SSO voter that prevents SSO if one or more countries were selected and those do not match the country from the
 * previous authentication.
 *
 * @author Martin Lindström
 */
public class EidasSsoVoter implements SsoVoter {

  /** {@inheritDoc} */
  @Override
  public Vote mayReuse(final Saml2UserAuthentication userAuthn, final Saml2UserAuthenticationInputToken token,
      final Collection<String> allowedAuthnContexts) {

    final List<String> requestedCountries =
        Optional.ofNullable(token.getAuthnRequestToken().getAuthnRequest().getScoping())
            .map(Scoping::getIDPList)
            .map(list -> list.getIDPEntrys().stream()
                .map(IDPEntry::getProviderID)
                .filter(p -> p.startsWith(EidasCountryHandler.COUNTRY_URI_PREFIX))
                .map(p -> p.substring(EidasCountryHandler.COUNTRY_URI_PREFIX.length(), p.length()))
                .filter(c -> StringUtils.hasText(c))
                .toList())
            .orElse(Collections.emptyList());

    if (requestedCountries.isEmpty()) {
      return Vote.OK;
    }

    final String previousCountry = userAuthn.getSaml2UserDetails().getAttributes().stream()
        .filter(a -> AttributeConstants.ATTRIBUTE_NAME_C.equals(a.getId()))
        .filter(a -> !a.getValues().isEmpty())
        .map(a -> String.class.cast(a.getValues().get(0)))
        .findFirst()
        .orElse(null);

    if (previousCountry == null) {
      return Vote.DONT_KNOW;
    }

    if (requestedCountries.contains(previousCountry)) {
      return Vote.OK;
    }
    else {
      return Vote.DENY;
    }
  }

}
