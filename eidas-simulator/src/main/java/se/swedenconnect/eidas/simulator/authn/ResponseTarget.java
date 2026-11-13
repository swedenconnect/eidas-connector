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
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import java.util.Objects;

/**
 * Where and how a response to an authentication request is sent. It is known once the connector has been
 * identified and its assertion consumer service established.
 *
 * @param connectorMetadata the metadata of the connector
 * @param assertionConsumerServiceUrl the URL that the response is posted to
 * @param inResponseTo the ID of the authentication request
 * @param relayState the relay state, or {@code null}
 * @author Martin Lindström
 */
public record ResponseTarget(@NonNull EntityDescriptor connectorMetadata, @NonNull String assertionConsumerServiceUrl,
    @NonNull String inResponseTo, @Nullable String relayState) {

  /**
   * Constructor.
   *
   * @param connectorMetadata the metadata of the connector
   * @param assertionConsumerServiceUrl the URL that the response is posted to
   * @param inResponseTo the ID of the authentication request
   * @param relayState the relay state, or {@code null}
   */
  public ResponseTarget {
    Objects.requireNonNull(connectorMetadata, "connectorMetadata must not be null");
    Objects.requireNonNull(assertionConsumerServiceUrl, "assertionConsumerServiceUrl must not be null");
    Objects.requireNonNull(inResponseTo, "inResponseTo must not be null");
  }

  /**
   * Gets the entityID of the connector.
   *
   * @return the entityID
   */
  public @NonNull String getConnectorEntityId() {
    return this.connectorMetadata.getEntityID();
  }

}
