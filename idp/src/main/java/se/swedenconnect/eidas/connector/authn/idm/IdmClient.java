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
package se.swedenconnect.eidas.connector.authn.idm;

import se.swedenconnect.eidas.connector.authn.EidasAuthenticationToken;

/**
 * Interface defining the eIDAS Identity Matching client.
 *
 * @author Martin Lindström
 */
public interface IdmClient {

  /**
   * Tells whether the IdM-feature is active.
   *
   * @return {@code true} if the Identity Matching feature is active and {@code false} otherwise
   */
  boolean isActive();

  /**
   * Tells whether the user identified by {@code token} has a record at the Identity Matching service.
   *
   * @param token the user authentication token
   * @return {@code true} if the user has a record and {@code false otherwise}
   * @throws IdmException for errors communicating with the Identity Matching service
   */
  boolean hasRecord(final EidasAuthenticationToken token) throws IdmException;

  /**
   * The method will attempt to get the Identity Matching record for the authenticated user, and if a valid record
   * exists update the supplied token with attributes for the Swedish identity and information about the identity
   * matching binding used.
   *
   * @param token the authentication token
   * @return the {@link IdmRecord} for the binding
   * @throws IdmException for errors communicating with the Identity Matching service or if no record was available
   */
  IdmRecord getRecord(final EidasAuthenticationToken token) throws IdmException;

}
