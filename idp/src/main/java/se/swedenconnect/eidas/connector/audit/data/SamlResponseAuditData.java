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
package se.swedenconnect.eidas.connector.audit.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import se.swedenconnect.eidas.connector.ApplicationVersion;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;

/**
 * Audit data for a SAML response.
 *
 * @author Martin Lindström
 */
@JsonInclude(Include.NON_EMPTY)
public class SamlResponseAuditData extends ConnectorAuditData {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The Response ID. */
  @JsonProperty(value = "id")
  private @Nullable String id;

  /** The ID matching the AuthnRequest ID. */
  @JsonProperty(value = "in-response-to")
  private @Nullable String inResponseTo;

  /** The status. */
  @JsonProperty(value = "status")
  private @Nullable SamlStatus status;

  /** The response issuance time. */
  @JsonProperty(value = "issued-at")
  private @Nullable Instant issuedAt;

  /** The destination, i.e., where the response is being sent. */
  @JsonProperty(value = "destination")
  private @Nullable String destination;

  /** Tells whether the response is signed. */
  @JsonProperty(value = "is-signed")
  private boolean signed;

  /**
   * Gets the Response ID.
   *
   * @return the Response ID
   */
  public @Nullable String getId() {
    return this.id;
  }

  /**
   * Assigns the Response ID.
   *
   * @param id the Response ID
   */
  public void setId(final @Nullable String id) {
    this.id = id;
  }

  /**
   * Gets the ID matching the AuthnRequest ID.
   *
   * @return the ID matching the AuthnRequest ID
   */
  public @Nullable String getInResponseTo() {
    return this.inResponseTo;
  }

  /**
   * Assigns the ID matching the AuthnRequest ID.
   *
   * @param inResponseTo the ID matching the AuthnRequest ID
   */
  public void setInResponseTo(final @Nullable String inResponseTo) {
    this.inResponseTo = inResponseTo;
  }

  /**
   * Gets the status.
   *
   * @return the status
   */
  public @Nullable SamlStatus getStatus() {
    return this.status;
  }

  /**
   * Assigns the status.
   *
   * @param status the status
   */
  public void setStatus(final @Nullable SamlStatus status) {
    this.status = status;
  }

  /**
   * Gets the response issuance time.
   *
   * @return the response issuance time
   */
  public @Nullable Instant getIssuedAt() {
    return this.issuedAt;
  }

  /**
   * Assigns the response issuance time.
   *
   * @param issuedAt the response issuance time
   */
  public void setIssuedAt(final @Nullable Instant issuedAt) {
    this.issuedAt = issuedAt;
  }

  /**
   * Gets the destination, i.e., where the response is being sent.
   *
   * @return the destination, i.e., where the response is being sent
   */
  public @Nullable String getDestination() {
    return this.destination;
  }

  /**
   * Assigns the destination, i.e., where the response is being sent.
   *
   * @param destination the destination, i.e., where the response is being sent
   */
  public void setDestination(final @Nullable String destination) {
    this.destination = destination;
  }

  /**
   * Gets whether the response is signed.
   *
   * @return whether the response is signed
   */
  public boolean isSigned() {
    return this.signed;
  }

  /**
   * Assigns whether the response is signed.
   *
   * @param signed whether the response is signed
   */
  public void setSigned(final boolean signed) {
    this.signed = signed;
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull String getName() {
    return "saml-response";
  }

  /**
   * Creates a {@link SamlResponseAuditData} given a {@link Response} object.
   *
   * @param response the SAML response
   * @return a {@link SamlResponseAuditData}, or {@code null} if no response is given
   */
  public static @Nullable SamlResponseAuditData of(final @Nullable Response response) {
    if (response == null) {
      return null;
    }
    final SamlResponseAuditData data = new SamlResponseAuditData();
    data.setId(response.getID());
    data.setInResponseTo(response.getInResponseTo());
    final Status status = response.getStatus();
    if (status != null) {
      final SamlStatus samlStatus = new SamlStatus();
      samlStatus.setStatusCode(Optional.ofNullable(status.getStatusCode()).map(StatusCode::getValue).orElse(null));
      samlStatus.setSubordinateStatusCode(Optional.ofNullable(status.getStatusCode())
          .map(StatusCode::getStatusCode)
          .map(StatusCode::getValue)
          .orElse(null));
      samlStatus.setStatusMessage(Optional.ofNullable(status.getStatusMessage())
          .map(StatusMessage::getValue)
          .orElse(null));
      data.setStatus(samlStatus);
    }
    data.setIssuedAt(response.getIssueInstant());
    data.setDestination(response.getDestination());
    data.setSigned(response.isSigned());
    return data;
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull String toString() {
    return String.format(
        "id='%s', in-response-to='%s', status=[%s], issued-at='%s', destination='%s', signed='%s']", this.id,
        this.inResponseTo, this.status, this.issuedAt, this.destination, this.signed);
  }

  /**
   * Represents a SAML {@code Status}.
   */
  @JsonInclude(Include.NON_EMPTY)
  public static class SamlStatus implements Serializable {

    @Serial
    private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

    /** The main status code. */
    @JsonProperty(value = "code")
    private @Nullable String statusCode;

    /** The subordinate status code. */
    @JsonProperty(value = "subordinate-code")
    private @Nullable String subordinateStatusCode;

    /** The status message. */
    @JsonProperty(value = "message")
    private @Nullable String statusMessage;

    /**
     * Gets the main status code.
     *
     * @return the main status code
     */
    public @Nullable String getStatusCode() {
      return this.statusCode;
    }

    /**
     * Assigns the main status code.
     *
     * @param statusCode the main status code
     */
    public void setStatusCode(final @Nullable String statusCode) {
      this.statusCode = statusCode;
    }

    /**
     * Gets the subordinate status code.
     *
     * @return the subordinate status code
     */
    public @Nullable String getSubordinateStatusCode() {
      return this.subordinateStatusCode;
    }

    /**
     * Assigns the subordinate status code.
     *
     * @param subordinateStatusCode the subordinate status code
     */
    public void setSubordinateStatusCode(final @Nullable String subordinateStatusCode) {
      this.subordinateStatusCode = subordinateStatusCode;
    }

    /**
     * Gets the status message.
     *
     * @return the status message
     */
    public @Nullable String getStatusMessage() {
      return this.statusMessage;
    }

    /**
     * Assigns the status message.
     *
     * @param statusMessage the status message
     */
    public void setStatusMessage(final @Nullable String statusMessage) {
      this.statusMessage = statusMessage;
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull String toString() {
      final StringBuilder sb = new StringBuilder("code='");
      sb.append(this.statusCode).append("'");
      if (this.subordinateStatusCode != null) {
        sb.append(", subordinate-code='").append(this.subordinateStatusCode).append("'");
      }
      if (this.statusMessage != null) {
        sb.append("', message='").append(this.statusMessage).append("'");
      }
      return sb.toString();
    }
  }

}
