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
import se.swedenconnect.eidas.connector.events.ResponseProcessingErrorEvent;

import java.io.Serial;

/**
 * Audit data class for processing errors.
 *
 * @author Martin Lindström
 */
public class ProcessingErrorAuditData extends ConnectorAuditData {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The error message. */
  @JsonProperty("error-message")
  private @Nullable String errorMessage;

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
    return "processing-error";
  }

  /**
   * Creates a {@link ProcessingErrorAuditData} given a {@link ResponseProcessingErrorEvent}.
   *
   * @param event a {@link ResponseProcessingErrorEvent}
   * @return a {@link ProcessingErrorAuditData}
   */
  public static @Nullable ProcessingErrorAuditData of(final @Nullable ResponseProcessingErrorEvent event) {
    if (event == null || event.getErrorMsg() == null) {
      return null;
    }
    final ProcessingErrorAuditData data = new ProcessingErrorAuditData();
    data.setErrorMessage(event.getErrorMsg());
    return data;
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull String toString() {
    return String.format("error-message='%s'", this.errorMessage);
  }

}
