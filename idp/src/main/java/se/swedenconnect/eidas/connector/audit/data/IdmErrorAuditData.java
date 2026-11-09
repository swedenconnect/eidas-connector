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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.eidas.connector.events.IdentityMatchingErrorEvent;

import java.io.Serial;

/**
 * Audit data for errors during Identity Matching.
 *
 * @author Martin Lindström
 */
public class IdmErrorAuditData extends ConnectorAuditData {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The user identity (eIDAS personal identifier) of the user. */
  @JsonProperty("user")
  private @Nullable String user;

  /** The error message. */
  @JsonProperty("error-message")
  private @Nullable String errorMessage;

  /**
   * Gets the user identity (eIDAS personal identifier) of the user.
   *
   * @return the user identity (eIDAS personal identifier) of the user
   */
  public @Nullable String getUser() {
    return this.user;
  }

  /**
   * Assigns the user identity (eIDAS personal identifier) of the user.
   *
   * @param user the user identity (eIDAS personal identifier) of the user
   */
  public void setUser(final @Nullable String user) {
    this.user = user;
  }

  /**
   * Gets the error message.
   *
   * @return the error message
   */
  public @Nullable String getErrorMessage() {
    return this.errorMessage;
  }

  /**
   * Assigns the error message.
   *
   * @param errorMessage the error message
   */
  public void setErrorMessage(final @Nullable String errorMessage) {
    this.errorMessage = errorMessage;
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull String getName() {
    return "idm-error";
  }

  /**
   * Creates an {@link IdmRecordAuditData} from a {@link IdentityMatchingErrorEvent}.
   *
   * @param event the evebt
   * @return an {@link IdmRecordAuditData} object
   */
  public static @NonNull IdmErrorAuditData of(final @NonNull IdentityMatchingErrorEvent event) {
    final IdmErrorAuditData auditData = new IdmErrorAuditData();
    auditData.setUser((String) event.getEidasToken().getPrincipal());
    auditData.setErrorMessage(event.getError().getMessage());
    return auditData;
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull String toString() {
    return String.format("user='%s', error-message='%s'", this.user, this.errorMessage);
  }

}
