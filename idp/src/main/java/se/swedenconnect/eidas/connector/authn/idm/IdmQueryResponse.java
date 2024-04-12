/*
 * Copyright 2023-2024 Sweden Connect
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representation of a response to a record query.
 *
 * @author Martin Lindström
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

}
