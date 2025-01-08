/*
 * Copyright 2017-2025 Sweden Connect
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

/**
 * An interface defining the method that gets us an OAuth2 access token.
 *
 * @author Martin Lindström
 */
public interface OAuth2Handler {

  /**
   * Gets an OAuth2 access token for the HEAD call to the IdM Query API.
   * @return a serialized access token
   * @throws IdmException for errors getting the access token
   */
  String getCheckAccessToken() throws IdmException;

  /**
   * Gets the OAuth2 access token needed for our GET call to the IdM Query API.
   *
   * @param prid the user identity
   * @return a serialized access token
   * @throws IdmException for errors getting the access token
   */
  String getGetAccessToken(final String prid) throws IdmException;

}
