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
package se.swedenconnect.eidas.connector.events;

import java.io.Serial;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEvent;

import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.spring.authnserver.authentication.provider.UserAuthenticationInputToken;

/**
 * An {@link ApplicationEvent} that is issued when a valid AuthnRequest has been received and the connector has
 * calculated which countries that are selectable.
 *
 * @author Martin Lindström
 */
public class BeforeCountrySelectionEvent extends AbstractEidasConnectorEvent {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** A list of countries that was displayed for the user to select from. */
  private @Nullable List<String> selectableCountries;

  /** A pre-selected country. */
  private @Nullable String preSelectedCountry;

  /** The reason that no country selection view was displayed. */
  private @Nullable NoDisplayReason noDisplayReason;

  /**
   * Constructor used when the user was directed to the country selection view.
   *
   * @param token the {@link UserAuthenticationInputToken}
   * @param countries a list of countries that was displayed for the user to select from
   */
  public BeforeCountrySelectionEvent(
      final @NonNull UserAuthenticationInputToken token, final @NonNull List<String> countries) {
    super(token);
    this.selectableCountries = Objects.requireNonNull(countries, "countries must not be null");
  }

  /**
   * Constructor used when the country selection view was skipped.
   *
   * @param token the {@link UserAuthenticationInputToken}
   * @param country the pre-selected country
   * @param reason the reason why no country selection view was displayed
   */
  public BeforeCountrySelectionEvent(
      final @NonNull UserAuthenticationInputToken token, final @NonNull String country,
      final @NonNull NoDisplayReason reason) {
    super(token);
    this.preSelectedCountry = Objects.requireNonNull(country, "country must not be null");
    this.noDisplayReason = Objects.requireNonNull(reason, "reason must not be null");
  }

  /**
   * Gets the {@link UserAuthenticationInputToken} associated to the event.
   *
   * @return the {@link UserAuthenticationInputToken}
   */
  public @NonNull UserAuthenticationInputToken getInputToken() {
    return (UserAuthenticationInputToken) this.getSource();
  }

  /**
   * Gets a list of the countries that the user could select from.
   * <p>
   * If a pre-selection was active, {@code null} is returned. In these cases, see {@link #getPreSelectedCountry()} and
   * {@link #getNoDisplayReason()}.
   * </p>
   *
   * @return a list of countries or {@code null}
   */
  public @Nullable List<String> getSelectableCountries() {
    return this.selectableCountries;
  }

  /**
   * If no country selection view was displayed for the user it means that the country to use could be obtained by other
   * means. This method will return this country. See also {@link #getNoDisplayReason()}.
   *
   * @return the pre-selected country, or {@code null} if the country selection view was displayed
   */
  public @Nullable String getPreSelectedCountry() {
    return this.preSelectedCountry;
  }

  /**
   * If no country selection view was displayed for the user it means that the country to use could be obtained by other
   * means. This method tells what this reason was.
   *
   * @return a {@link NoDisplayReason}, or {@code null} if the country selection view was displayed
   */
  public @Nullable NoDisplayReason getNoDisplayReason() {
    return this.noDisplayReason;
  }

  /**
   * Represents the different reasons that no country selection view was displayed.
   */
  public enum NoDisplayReason {
    /**
     * The request was made from a signature service and country that was used from the authentication was found in a
     * session cookie.
     */
    SIGN_SERVICE_COUNTRY_FROM_SESSION,

    /**
     * The authentication request contained a {@code PrincipalSelection} extension or a {@code Scopes} element giving
     * the country to use.
     */
    FROM_AUTHN_REQUEST
  }

}
