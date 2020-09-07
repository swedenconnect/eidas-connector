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

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Representation of a statistics entry.
 * 
 * @author Martin Lindström (martin@litsec.se)
 */
@ToString
public class StatisticsEntry {

  /** The event type. */
  @Getter
  public StatisticsEventType type;

  /** The timestamp for the event. */
  @Getter
  private long timestamp;

  /** The timestamp for when the operation was initiated. */
  @Getter
  private final long initTimestamp;

  /** The SAML RequestID. */
  @Getter
  private String requestId;

  /** The requesting entity (entityID). */
  @Getter
  private String requester;

  /** If a country was selected in the call. */
  @Getter
  private String preSelectedCountry;

  /** The selected country. */
  @Getter
  private String country;

  /** The ID for the response received from the foreign eIDAS node. */
  @Getter
  @Setter
  private String responseId;

  /** The ID for the assertion received from the foreign eIDAS node. */
  @Getter
  @Setter
  private String eidasAssertionId;

  /** The LoA received from the eIDAS country. */
  @Getter
  private String eidasLoa;

  /** The issued LoA. */
  @Getter
  private String loa;

  /** Whether this is an eIDAS ping? */
  @Getter
  private boolean ping = false;

  /** Error information. */
  @Getter
  @Setter
  private ErrorInformation error;

  /**
   * Constructor.
   */
  public StatisticsEntry() {
    this.initTimestamp = System.currentTimeMillis();
  }

  /**
   * Assigns the event type.
   * 
   * @param type
   *          the type
   * @return this object
   */
  public StatisticsEntry type(final StatisticsEventType type) {
    this.type = type;
    return this;
  }

  /**
   * Assigns the current time to this object.
   * 
   * @return this object
   */
  public StatisticsEntry timestamp() {
    this.timestamp = System.currentTimeMillis();
    return this;
  }

  /**
   * Sets the pre selected country.
   * 
   * @param preSelectedCountry
   *          pre selected country
   * @return this object
   */
  public StatisticsEntry preSelectedCountry(final String preSelectedCountry) {
    this.preSelectedCountry = preSelectedCountry;
    return this;
  }

  /**
   * Assigns the {@code AuthnRequest} message for the operation. It sets the request ID and requester.
   * 
   * @param authnRequest
   *          the AuthnRequest message
   * @return this object
   */
  public StatisticsEntry authnRequest(final AuthnRequest authnRequest) {
    if (authnRequest == null) {
      return this;
    }
    this.requestId = authnRequest.getID();
    if (authnRequest.getIssuer() != null) {
      this.requester = authnRequest.getIssuer().getValue();
    }
    return this;
  }

  /**
   * Assigns the selected country.
   * 
   * @param country
   *          the country
   * @return this object
   */
  public StatisticsEntry country(final String country) {
    this.country = country;
    return this;
  }

  /**
   * Assigns information for a response received from the foreign eIDAS node.
   * 
   * @param responseID
   *          the response ID
   * @param assertion
   *          the assertion
   * @return this object
   */
  public StatisticsEntry eidasResponse(final String responseID, final Assertion assertion) {
    this.responseId = responseID;
    this.eidasAssertionId = assertion != null ? assertion.getID() : null;
    if (assertion != null) {
      try {
        this.eidasLoa = assertion.getAuthnStatements().get(0).getAuthnContext().getAuthnContextClassRef().getAuthnContextClassRef();
      }
      catch (Exception e) {
      }
    }
    return this;
  }

  /**
   * Assigns the LoA for the authentication.
   * 
   * @param loa
   *          the LoA
   * @return this object
   */
  public StatisticsEntry loa(final String loa) {
    this.loa = loa;
    return this;
  }

  /**
   * Tells whether this is a ping request.
   * 
   * @param flag
   *          boolean
   * @return this object
   */
  public StatisticsEntry ping(final boolean flag) {
    this.ping = flag;
    return this;
  }

  /**
   * Represents error information.
   */
  @ToString
  @AllArgsConstructor
  public static class ErrorInformation {

    /** The second level SAML status code. */
    @Getter
    private String errorCode;

    /** The error message. */
    @Getter
    private String errorMessage;
  }
}
