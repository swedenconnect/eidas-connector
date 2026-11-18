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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.swedenconnect.opensaml.sweid.saml2.metadata.entitycategory.EntityCategoryConstants;
import se.swedenconnect.spring.authnserver.authentication.AuthenticationProtocol;
import se.swedenconnect.spring.authnserver.authentication.provider.UserAuthenticationInputToken;
import se.swedenconnect.spring.authnserver.registry.ClientRegistry;
import se.swedenconnect.spring.authnserver.registry.ClientRegistryException;
import se.swedenconnect.spring.authnserver.registry.RequesterRecord;
import se.swedenconnect.spring.authnserver.saml.authentication.SamlAuthenticationRequirements;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Looks up what the client registry knows about the requester of an authentication, a Swedish SAML Service Provider
 * or OpenID Connect Relying Party.
 *
 * @author Martin Lindström
 */
public class RequesterLookup {

  /** Logger instance. */
  private static final Logger log = LoggerFactory.getLogger(RequesterLookup.class);

  /**
   * The trust mark that an OpenID Connect Relying Party in the public sector holds, see the Sweden Connect OpenID
   * Federation Rules, Section 5.2.2.
   */
  public static final @NonNull String PUBLIC_BODY_TRUST_MARK = "https://id.swedenconnect.se/tm-prop/public-body";

  /** Gives the client registry, or {@code null} if the server has not been configured yet. */
  private final @NonNull Supplier<@Nullable ClientRegistry> clientRegistry;

  /**
   * Constructor.
   *
   * @param clientRegistry gives the client registry, or {@code null} if the server has not been configured yet
   */
  public RequesterLookup(final @NonNull Supplier<@Nullable ClientRegistry> clientRegistry) {
    this.clientRegistry = Objects.requireNonNull(clientRegistry, "clientRegistry must not be null");
  }

  /**
   * Gets the record of the requester.
   *
   * @param token the input token
   * @return the {@link RequesterRecord}, or {@code null} if it cannot be found
   */
  public @Nullable RequesterRecord getRecord(final @NonNull UserAuthenticationInputToken token) {
    final ClientRegistry registry = this.clientRegistry.get();
    if (registry == null) {
      return null;
    }
    try {
      return registry.lookup(token.getRequester());
    }
    catch (final ClientRegistryException e) {
      log.warn("Failed to look up requester: {} [{}]", e.getMessage(), token.getLogString());
      return null;
    }
  }

  /**
   * Tells whether the requester belongs to the public sector.
   * <p>
   * A SAML Service Provider belongs to the public sector unless it declares the private sector entity category. An
   * OpenID Connect Relying Party belongs to the public sector if it holds the {@value #PUBLIC_BODY_TRUST_MARK} trust
   * mark. The trust mark is asked for if the Relying Party does not hold it, and if it cannot be obtained, the Relying
   * Party belongs to the private sector.
   * </p>
   *
   * @param token the input token
   * @return {@code true} for the public sector and {@code false} for the private sector
   */
  public boolean isPublicSector(final @NonNull UserAuthenticationInputToken token) {
    if (token.getAuthnRequirements() instanceof final SamlAuthenticationRequirements requirements) {
      return !requirements.getEntityCategories()
          .contains(EntityCategoryConstants.SERVICE_TYPE_CATEGORY_PRIVATE_SECTOR_SP.getUri());
    }
    if (token.getRequester().protocol() != AuthenticationProtocol.OIDC) {
      return true;
    }
    final ClientRegistry registry = this.clientRegistry.get();
    if (registry == null) {
      return false;
    }
    try {
      final RequesterRecord record = registry.requestMark(token.getRequester(), PUBLIC_BODY_TRUST_MARK);
      final boolean publicSector = record != null && record.hasMark(PUBLIC_BODY_TRUST_MARK);
      log.debug("Relying Party is in the {} sector [{}]", publicSector ? "public" : "private", token.getLogString());
      return publicSector;
    }
    catch (final ClientRegistryException e) {
      log.warn("Failed to obtain the {} trust mark - the Relying Party is treated as private sector: {} [{}]",
          PUBLIC_BODY_TRUST_MARK, e.getMessage(), token.getLogString());
      return false;
    }
  }

}
