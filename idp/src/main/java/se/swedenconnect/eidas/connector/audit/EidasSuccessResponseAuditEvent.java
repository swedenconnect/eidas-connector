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
package se.swedenconnect.eidas.connector.audit;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.eidas.connector.events.AbstractEidasConnectorEvent;
import se.swedenconnect.opensaml.common.utils.SerializableOpenSamlObject;

import java.io.Serial;
import java.util.Objects;

/**
 * Event that is published when a successful SAML response has been sent to the Swedish SP. It is audited as
 * {@value ConnectorAuditEvents#EIDAS_AUDIT_SUCCESSFUL_RESPONSE}.
 *
 * @author Martin Lindström
 */
public class EidasSuccessResponseAuditEvent extends AbstractEidasConnectorEvent {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The SAML response. */
  private final SerializableOpenSamlObject<Response> response;

  /** The SAML assertion, before it was encrypted. */
  private final SerializableOpenSamlObject<Assertion> assertion;

  /**
   * Constructor.
   *
   * @param spEntityId the entityID of the Swedish SP
   * @param response the SAML response
   * @param assertion the SAML assertion, before it was encrypted
   */
  public EidasSuccessResponseAuditEvent(final String spEntityId, final Response response, final Assertion assertion) {
    super(Objects.requireNonNull(spEntityId, "spEntityId must not be null"));
    this.response = new SerializableOpenSamlObject<>(Objects.requireNonNull(response, "response must not be null"));
    this.assertion = new SerializableOpenSamlObject<>(Objects.requireNonNull(assertion, "assertion must not be null"));
  }

  /**
   * Gets the entityID of the Swedish SP.
   *
   * @return the entityID
   */
  public String getSpEntityId() {
    return (String) this.getSource();
  }

  /**
   * Gets the SAML response.
   *
   * @return the {@link Response}
   */
  public Response getResponse() {
    return this.response.get();
  }

  /**
   * Gets the SAML assertion, before it was encrypted.
   *
   * @return the {@link Assertion}
   */
  public Assertion getAssertion() {
    return this.assertion.get();
  }

}
