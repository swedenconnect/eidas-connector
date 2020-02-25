/*
 * Copyright 2017-2020 Sweden Connect
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
package se.elegnamnden.eidas.idp.statistics;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Representation of the different types of statistics entries.
 * 
 * @author Martin Lindström (martin@litsec.se)
 */
public enum StatisticsEventType {

  /** An AuthnRequest was received. */
  RECEIVED_AUTHN_REQUEST("received-authnrequest"),
  
  /** The user was directed to the foreign IdP. */
  DIRECTED_USER_TO_FOREIGN_IDP("directed-user-to-foreign-idp"),
  
  /** Authentication was cancelled at the country selection dialogue. */
  CANCELLED_COUNTRY_SELECTION("cancelled-country-selection"),
  
  /** A successful response message was received from the foreign IdP. */
  RECEIVED_EIDAS_RESPONSE("received-eidas-response"),
  
  /** Sign message display. */
  DISPLAY_SIGN_MESSAGE("sign-message-display"),
  
  /** User did not approve signature message. */
  REJECTED_SIGNATURE("rejected-signature"),
  
  /** Issued for authentication success. */
  AUTHN_SUCCESS("authn-success"),
  
  /** A bad request was received from Swedish SP. */
  ERROR_BAD_REQUEST("error-authnrequest"),
  
  /** A country to which we have no metadata was selected. */
  ERROR_CONFIG("error-config-metadata"),
  
  /** Error during processing of response from eIDAS country. */
  ERROR_RESPONSE_PROCESSING("error-response-processing"),
  
  /** Received error status code from eIDAS country. */
  ERROR_EIDAS_ERROR_RESPONSE("error-eidas-error-response");

  /**
   * Gets the event in its textual form.
   * 
   * @return the event
   */
  @JsonValue
  public String getEvent() {
    return this.event;
  }

  /** The event in textual format. */
  private String event;

  /**
   * Constructor.
   * 
   * @param event
   *          the event in textual format
   */
  private StatisticsEventType(final String event) {
    this.event = event;
  }

}
