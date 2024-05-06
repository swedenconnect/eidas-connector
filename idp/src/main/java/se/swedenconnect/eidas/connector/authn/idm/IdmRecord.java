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

import java.util.Objects;

/**
 * Representation of an Identity Matching record.
 *
 * @author Martin Lindström
 */
public class IdmRecord {

  /** The ID for the record. */
  private final String id;

  /** The Swedish identity. */
  private final String swedishIdentity;

  /** The binding URI/URI:s. */
  private final String binding;

  /**
   * Creates a new instance of IdmRecord.
   *
   * @param id the ID for the record
   * @param swedishIdentity the Swedish identity
   * @param binding the binding URI/URI:s
   */
  public IdmRecord(final String id, final String swedishIdentity, final String binding) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.swedishIdentity = Objects.requireNonNull(swedishIdentity, "swedishIdentity must not be null");
    this.binding = Objects.requireNonNull(binding, "binding must not be null");
  }

  /**
   * Returns the ID for the IdM record.
   *
   * @return the ID for the record
   */
  public String getId() {
    return this.id;
  }

  /**
   * Returns the Swedish identity of the record.
   *
   * @return the Swedish identity
   */
  public String getSwedishIdentity() {
    return this.swedishIdentity;
  }

  /**
   * Returns the binding URI/URI:s of the record.
   *
   * @return the binding URI/URI:s
   */
  public String getBinding() {
    return this.binding;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "id='%s', swedish-identity='%s', binding='%s'".formatted(this.id, this.swedishIdentity, this.binding);
  }

}
