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
 * Implementation of the {@link IdmClient} that is used if the Identity Matching feature is not turned on.
 *
 * @author Martin Lindström
 */
public class NoopIdmClient implements IdmClient {

  /**
   * Returns {@code false}.
   */
  @Override
  public boolean isActive() {
    return false;
  }

  @Override
  public boolean hasRecord(final EidasAuthenticationToken token) {
    return false;
  }

  /**
   * Will always return {@code false}.
   */
  @Override
  public IdmRecord getRecord(final EidasAuthenticationToken token) throws IdmException {
    throw new IdmException("NoopIdmClient can not get record");
  }

}
