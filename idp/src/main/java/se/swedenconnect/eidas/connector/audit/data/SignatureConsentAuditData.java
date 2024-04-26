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
package se.swedenconnect.eidas.connector.audit.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.eidas.connector.events.SignatureConsentEvent;

/**
 * Audit data for signature consent results.
 *
 * @author Martin Lindström
 */
public class SignatureConsentAuditData extends ConnectorAuditData {

  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** Tells whether the user consented to the signature. */
  @Getter
  @Setter
  @JsonProperty("signature-consented")
  private boolean signatureConsented;

  /** The user identity (personal identifier) of the user that consented/rejected the signature. */
  @Getter
  @Setter
  @JsonProperty("user")
  private String user;

  /** {@inheritDoc} */
  @Override
  public String getName() {
    return "signature-consent-result";
  }

  /**
   * Creates a {@link SignatureConsentAuditData} given a {@link SignatureConsentEvent}.
   *
   * @param event a {@link SignatureConsentEvent}
   * @return a {@link SignatureConsentAuditData}
   */
  public static SignatureConsentAuditData of(final SignatureConsentEvent event) {
    final SignatureConsentAuditData data = new SignatureConsentAuditData();
    data.setSignatureConsented(event.isSignatureConsented());
    data.setUser((String) event.getEidasToken().getPrincipal());
    return data;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return String.format("signature-consented='%s', user='%s'", this.signatureConsented, this.user);
  }

}
