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
package se.swedenconnect.eidas.simulator.authn;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import se.swedenconnect.eidas.simulator.country.SimulatedUser;

/**
 * The form of the page where the tester answers a request.
 *
 * @author Martin Lindström
 */
public class AuthnForm {

  /** Sends a success response. */
  public static final @NonNull String ACTION_AUTHENTICATE = "authenticate";

  /** Sends an error response. */
  public static final @NonNull String ACTION_ERROR = "error";

  /** The action, {@link #ACTION_AUTHENTICATE} or {@link #ACTION_ERROR}. */
  private @Nullable String action;

  /** The identity of the selected user. */
  private @Nullable String userId;

  /** The attribute values of the user, as edited by the tester. */
  private @NonNull SimulatedUser user = new SimulatedUser();

  /** Whether the user should be saved, so that it can be selected again. */
  private boolean saveUser;

  /** The selected level of assurance. */
  private @Nullable String loa;

  /** The selected error response. */
  private @Nullable ErrorResponseOption errorOption;

  /** The status message of the error response. */
  private @Nullable String statusMessage;

  /**
   * Gets the action.
   *
   * @return the action
   */
  public @Nullable String getAction() {
    return this.action;
  }

  /**
   * Assigns the action.
   *
   * @param action the action
   */
  public void setAction(final @Nullable String action) {
    this.action = action;
  }

  /**
   * Gets the identity of the selected user.
   *
   * @return the identity
   */
  public @Nullable String getUserId() {
    return this.userId;
  }

  /**
   * Assigns the identity of the selected user.
   *
   * @param userId the identity
   */
  public void setUserId(final @Nullable String userId) {
    this.userId = userId;
  }

  /**
   * Gets the attribute values of the user.
   *
   * @return the user
   */
  public @NonNull SimulatedUser getUser() {
    return this.user;
  }

  /**
   * Assigns the attribute values of the user.
   *
   * @param user the user
   */
  public void setUser(final @Nullable SimulatedUser user) {
    this.user = user != null ? user : new SimulatedUser();
  }

  /**
   * Tells whether the user should be saved.
   *
   * @return whether the user should be saved
   */
  public boolean isSaveUser() {
    return this.saveUser;
  }

  /**
   * Assigns whether the user should be saved.
   *
   * @param saveUser whether the user should be saved
   */
  public void setSaveUser(final boolean saveUser) {
    this.saveUser = saveUser;
  }

  /**
   * Gets the selected level of assurance.
   *
   * @return the level of assurance URI
   */
  public @Nullable String getLoa() {
    return this.loa;
  }

  /**
   * Assigns the selected level of assurance.
   *
   * @param loa the level of assurance URI
   */
  public void setLoa(final @Nullable String loa) {
    this.loa = loa;
  }

  /**
   * Gets the selected error response.
   *
   * @return the error response
   */
  public @Nullable ErrorResponseOption getErrorOption() {
    return this.errorOption;
  }

  /**
   * Assigns the selected error response.
   *
   * @param errorOption the error response
   */
  public void setErrorOption(final @Nullable ErrorResponseOption errorOption) {
    this.errorOption = errorOption;
  }

  /**
   * Gets the status message of the error response.
   *
   * @return the status message
   */
  public @Nullable String getStatusMessage() {
    return this.statusMessage;
  }

  /**
   * Assigns the status message of the error response.
   *
   * @param statusMessage the status message
   */
  public void setStatusMessage(final @Nullable String statusMessage) {
    this.statusMessage = statusMessage;
  }

}
