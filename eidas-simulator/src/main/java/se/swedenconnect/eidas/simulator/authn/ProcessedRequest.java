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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * An authentication request that has passed the checks, with what is needed to answer it.
 *
 * @param id the identity of the request within the simulator
 * @param countryCode the code of the simulated country that received the request
 * @param target where the response is sent
 * @param spType the SP type of the request, {@code public} or {@code private}
 * @param requesterId the requester ID, or {@code null}
 * @param providerName the provider name of the request, or {@code null}
 * @param requestedAttributes the requested attributes
 * @param nameIdFormat the NameID format of the response
 * @param allowedLoas the levels of assurance that the response may have, from the lowest to the highest
 * @param destinationCountry the country code of the connector, used in PersonIdentifier values
 * @author Martin Lindström
 */
public record ProcessedRequest(@NonNull String id, @NonNull String countryCode, @NonNull ResponseTarget target,
    @NonNull String spType, @Nullable String requesterId, @Nullable String providerName,
    @NonNull List<RequestedAttribute> requestedAttributes, @NonNull String nameIdFormat,
    @NonNull List<String> allowedLoas, @NonNull String destinationCountry) {

  /**
   * Constructor.
   *
   * @param id the identity of the request within the simulator
   * @param countryCode the code of the simulated country that received the request
   * @param target where the response is sent
   * @param spType the SP type of the request, {@code public} or {@code private}
   * @param requesterId the requester ID, or {@code null}
   * @param providerName the provider name of the request, or {@code null}
   * @param requestedAttributes the requested attributes
   * @param nameIdFormat the NameID format of the response
   * @param allowedLoas the levels of assurance that the response may have, from the lowest to the highest
   * @param destinationCountry the country code of the connector, used in PersonIdentifier values
   */
  public ProcessedRequest {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(countryCode, "countryCode must not be null");
    Objects.requireNonNull(target, "target must not be null");
    Objects.requireNonNull(spType, "spType must not be null");
    requestedAttributes = List.copyOf(requestedAttributes);
    Objects.requireNonNull(nameIdFormat, "nameIdFormat must not be null");
    allowedLoas = List.copyOf(allowedLoas);
    Objects.requireNonNull(destinationCountry, "destinationCountry must not be null");
  }

  /**
   * Tells whether an attribute is requested as required.
   *
   * @param name the attribute name
   * @return {@code true} if the attribute is requested as required and {@code false} otherwise
   */
  public boolean isRequired(final @NonNull String name) {
    return this.requestedAttributes.stream().anyMatch(a -> a.required() && a.name().equals(name));
  }

  /**
   * Tells whether an attribute is requested.
   *
   * @param name the attribute name
   * @return {@code true} if the attribute is requested and {@code false} otherwise
   */
  public boolean isRequested(final @NonNull String name) {
    return this.requestedAttributes.stream().anyMatch(a -> a.name().equals(name));
  }

  /**
   * A requested attribute.
   *
   * @param name the attribute name
   * @param required whether the attribute is required
   */
  public record RequestedAttribute(@NonNull String name, boolean required) {
  }

}
