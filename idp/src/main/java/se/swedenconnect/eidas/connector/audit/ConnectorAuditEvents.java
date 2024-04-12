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
package se.swedenconnect.eidas.connector.audit;

/**
 * Symbolic contants for eIDAS Connector audit events.
 *
 * @author Martin Lindström
 */
public class ConnectorAuditEvents {

  /** Event for indicating that the EU metadata has changed (country disappeared, ...). */
  public static final String CONNECTOR_AUDIT_EU_METADATA_CHANGED = "CONNECTOR_EU_METADATA_CHANGE";


  // hidden
  private ConnectorAuditEvents() {
  }

}
