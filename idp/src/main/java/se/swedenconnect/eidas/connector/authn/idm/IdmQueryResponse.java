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
package se.swedenconnect.eidas.connector.authn.idm;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representation of a response to a record query.
 *
 * @author Martin Lindström
 */
@JsonInclude(Include.NON_NULL)
public class IdmQueryResponse {

  /**
   * Unique ID for the matched record.
   */
  @JsonProperty("rid")
  private String recordId;

  /**
   * The eIDAS PersonalIdentifier.
   */
  @JsonProperty("eidas-user-id")
  private String eidasUserId;

  /**
   * The Swedish personal identity number.
   */
  @JsonProperty("swedish-id")
  private String swedishId;

  /** The binding level. */
  @JsonProperty("binding-level")
  private String bindingLevel;

  /**
   * A list of one or more URI:s identifying the "bindings", i.e., the processes that were applied to create the match
   * record.
   */
  @JsonProperty("bindings")
  private List<String> bindings;

  /**
   * A timestamp (seconds since epoch) for when the record was created.
   */
  @JsonProperty("created")
  private long created;

  /**
   * Default constructor.
   */
  public IdmQueryResponse() {
  }

  /**
   * Constructor assigning all fields.
   *
   * @param recordId the unique ID for the matched record
   * @param eidasUserId the eIDAS PersonalIdentifier
   * @param swedishId the Swedish personal identity number
   * @param bindingLevel the binding level
   * @param bindings the URI:s identifying the processes applied to create the match record
   * @param created the timestamp (seconds since epoch) for when the record was created
   */
  public IdmQueryResponse(final String recordId, final String eidasUserId, final String swedishId,
      final String bindingLevel, final List<String> bindings, final long created) {
    this.recordId = recordId;
    this.eidasUserId = eidasUserId;
    this.swedishId = swedishId;
    this.bindingLevel = bindingLevel;
    this.bindings = bindings;
    this.created = created;
  }

  /**
   * Creates a builder for {@link IdmQueryResponse} objects.
   *
   * @return a builder
   */
  public static IdmQueryResponseBuilder builder() {
    return new IdmQueryResponseBuilder();
  }

  /**
   * Gets the unique ID for the matched record.
   *
   * @return the unique ID for the matched record
   */
  public String getRecordId() {
    return this.recordId;
  }

  /**
   * Assigns the unique ID for the matched record.
   *
   * @param recordId the unique ID for the matched record
   */
  public void setRecordId(final String recordId) {
    this.recordId = recordId;
  }

  /**
   * Gets the eIDAS PersonalIdentifier.
   *
   * @return the eIDAS PersonalIdentifier
   */
  public String getEidasUserId() {
    return this.eidasUserId;
  }

  /**
   * Assigns the eIDAS PersonalIdentifier.
   *
   * @param eidasUserId the eIDAS PersonalIdentifier
   */
  public void setEidasUserId(final String eidasUserId) {
    this.eidasUserId = eidasUserId;
  }

  /**
   * Gets the Swedish personal identity number.
   *
   * @return the Swedish personal identity number
   */
  public String getSwedishId() {
    return this.swedishId;
  }

  /**
   * Assigns the Swedish personal identity number.
   *
   * @param swedishId the Swedish personal identity number
   */
  public void setSwedishId(final String swedishId) {
    this.swedishId = swedishId;
  }

  /**
   * Gets the binding level.
   *
   * @return the binding level
   */
  public String getBindingLevel() {
    return this.bindingLevel;
  }

  /**
   * Assigns the binding level.
   *
   * @param bindingLevel the binding level
   */
  public void setBindingLevel(final String bindingLevel) {
    this.bindingLevel = bindingLevel;
  }

  /**
   * Gets the URI:s identifying the processes applied to create the match record.
   *
   * @return the URI:s identifying the processes applied to create the match record
   */
  public List<String> getBindings() {
    return this.bindings;
  }

  /**
   * Assigns the URI:s identifying the processes applied to create the match record.
   *
   * @param bindings the URI:s identifying the processes applied to create the match record
   */
  public void setBindings(final List<String> bindings) {
    this.bindings = bindings;
  }

  /**
   * Gets the timestamp (seconds since epoch) for when the record was created.
   *
   * @return the timestamp (seconds since epoch) for when the record was created
   */
  public long getCreated() {
    return this.created;
  }

  /**
   * Assigns the timestamp (seconds since epoch) for when the record was created.
   *
   * @param created the timestamp (seconds since epoch) for when the record was created
   */
  public void setCreated(final long created) {
    this.created = created;
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof final IdmQueryResponse other)) {
      return false;
    }
    return this.created == other.created
        && Objects.equals(this.recordId, other.recordId)
        && Objects.equals(this.eidasUserId, other.eidasUserId)
        && Objects.equals(this.swedishId, other.swedishId)
        && Objects.equals(this.bindingLevel, other.bindingLevel)
        && Objects.equals(this.bindings, other.bindings);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Objects.hash(this.recordId, this.eidasUserId, this.swedishId, this.bindingLevel, this.bindings,
        this.created);
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "recordId=" + this.recordId + ", eidasUserId=" + this.eidasUserId
        + ", swedishId=" + this.swedishId + ", bindingLevel=" + this.bindingLevel
        + ", bindings=" + this.bindings + ", created=" + this.created;
  }

  /**
   * Builder for {@link IdmQueryResponse} objects.
   */
  public static final class IdmQueryResponseBuilder {

    private String recordId;
    private String eidasUserId;
    private String swedishId;
    private String bindingLevel;
    private List<String> bindings;
    private long created;

    /**
     * Constructor.
     */
    IdmQueryResponseBuilder() {
    }

    /**
     * Assigns the unique ID for the matched record.
     *
     * @param recordId the unique ID for the matched record
     * @return the builder
     */
    public IdmQueryResponseBuilder recordId(final String recordId) {
      this.recordId = recordId;
      return this;
    }

    /**
     * Assigns the eIDAS PersonalIdentifier.
     *
     * @param eidasUserId the eIDAS PersonalIdentifier
     * @return the builder
     */
    public IdmQueryResponseBuilder eidasUserId(final String eidasUserId) {
      this.eidasUserId = eidasUserId;
      return this;
    }

    /**
     * Assigns the Swedish personal identity number.
     *
     * @param swedishId the Swedish personal identity number
     * @return the builder
     */
    public IdmQueryResponseBuilder swedishId(final String swedishId) {
      this.swedishId = swedishId;
      return this;
    }

    /**
     * Assigns the binding level.
     *
     * @param bindingLevel the binding level
     * @return the builder
     */
    public IdmQueryResponseBuilder bindingLevel(final String bindingLevel) {
      this.bindingLevel = bindingLevel;
      return this;
    }

    /**
     * Assigns the URI:s identifying the processes applied to create the match record.
     *
     * @param bindings the URI:s identifying the processes applied to create the match record
     * @return the builder
     */
    public IdmQueryResponseBuilder bindings(final List<String> bindings) {
      this.bindings = bindings;
      return this;
    }

    /**
     * Assigns the timestamp (seconds since epoch) for when the record was created.
     *
     * @param created the timestamp (seconds since epoch) for when the record was created
     * @return the builder
     */
    public IdmQueryResponseBuilder created(final long created) {
      this.created = created;
      return this;
    }

    /**
     * Builds the {@link IdmQueryResponse} object.
     *
     * @return an {@link IdmQueryResponse} object
     */
    public IdmQueryResponse build() {
      return new IdmQueryResponse(this.recordId, this.eidasUserId, this.swedishId, this.bindingLevel, this.bindings,
          this.created);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return "recordId=" + this.recordId + ", eidasUserId=" + this.eidasUserId
          + ", swedishId=" + this.swedishId + ", bindingLevel=" + this.bindingLevel
          + ", bindings=" + this.bindings + ", created=" + this.created;
    }

  }

}
