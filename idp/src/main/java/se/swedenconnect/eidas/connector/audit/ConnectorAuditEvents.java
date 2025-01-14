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
package se.swedenconnect.eidas.connector.audit;

/**
 * Symbolic contants for eIDAS Connector audit events.
 *
 * @author Martin Lindström
 */
public class ConnectorAuditEvents {

  /** Event for indicating that the EU metadata has changed (country disappeared, ...). */
  public static final String CONNECTOR_AUDIT_EU_METADATA_CHANGED = "CONNECTOR_EU_METADATA_CHANGE";

  /** Event logged before a SAML authentication request is sent to a foreign IdP. */
  public static final String CONNECTOR_BEFORE_SAML_REQUEST = "CONNECTOR_BEFORE_SAML_REQUEST";

  /** Event logged when a successful SAML response is received from the foreign IdP. */
  public static final String CONNECTOR_SUCCESS_RESPONSE = "CONNECTOR_SUCCESS_RESPONSE";

  /** Event logged when an erroneous SAML response is received from the foreign IdP. */
  public static final String CONNECTOR_ERROR_RESPONSE = "CONNECTOR_ERROR_RESPONSE";

  /** Event logged for processing errors. */
  public static final String CONNECTOR_PROCESSING_ERROR = "CONNECTOR_PROCESSING_ERROR";

  /** Event logged when the user has consented/rejected a signature. */
  public static final String CONNECTOR_SIGNATURE_CONSENT_RESULT = "CONNECTOR_SIGNATURE_CONSENT_RESULT";

  /** Event logged when the user has consented/rejected the connector to query for the user's IdM record. */
  public static final String CONNECTOR_IDM_CONSENT_RESULT = "CONNECTOR_IDM_CONSENT_RESULT";

  /** Event logged when the connector has obtained an Identity Matching record for the user. */
  public static final String CONNECTOR_IDM_RECORD = "CONNECTOR_IDM_RECORD";

  /** Event logged when there are errors communicating with the IdM service. */
  public static final String CONNECTOR_IDM_ERROR = "CONNECTOR_IDM_ERROR";

  // hidden
  private ConnectorAuditEvents() {
  }

}
