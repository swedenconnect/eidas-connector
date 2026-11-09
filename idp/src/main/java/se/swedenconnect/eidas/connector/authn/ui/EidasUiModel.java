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
package se.swedenconnect.eidas.connector.authn.ui;

import java.util.List;
import java.util.Objects;

/**
 * Model object for the eiDAS Connector "choose country" UI view.
 *
 * @author Martin Lindström
 */
public class EidasUiModel extends BaseUiModel {

  /**
   * Identity Matching info.
   */
  private IdmInfo idm;

  /**
   * The countries to display.
   */
  private List<UiCountry> countries;

  /**
   * Gets the Identity Matching info.
   *
   * @return the Identity Matching info
   */
  public IdmInfo getIdm() {
    return this.idm;
  }

  /**
   * Assigns the Identity Matching info.
   *
   * @param idm the Identity Matching info
   */
  public void setIdm(final IdmInfo idm) {
    this.idm = idm;
  }

  /**
   * Gets the countries to display.
   *
   * @return the countries to display
   */
  public List<UiCountry> getCountries() {
    return this.countries;
  }

  /**
   * Assigns the countries to display.
   *
   * @param countries the countries to display
   */
  public void setCountries(final List<UiCountry> countries) {
    this.countries = countries;
  }

  /**
   * Predicate that tells whether we have any "disabled" countries.
   *
   * @return if at least one country is "disabled", i.e., can't be used for authentication, {@code true} is returned
   */
  public boolean hasDisabledCountry() {
    if (this.countries == null) {
      return false;
    }
    return this.countries.stream().anyMatch(UiCountry::isDisabled);
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    final EidasUiModel that = (EidasUiModel) o;
    return Objects.equals(this.idm, that.idm) && Objects.equals(this.countries, that.countries);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), this.idm, this.countries);
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "idm=[" + this.idm + "], countries=" + this.countries;
  }

  /**
   * UI info for Identity Matching.
   */
  public static class IdmInfo {

    /**
     * Is the IDM-feature active?
     */
    private boolean active;

    /**
     * Should the banner be displayed?
     */
    private boolean showBanner;

    /**
     * The URL to the IdM service.
     */
    private String serviceUrl;

    /**
     * Default constructor.
     */
    public IdmInfo() {
    }

    /**
     * Constructor assigning all fields.
     *
     * @param active whether the IdM feature is active
     * @param showBanner whether the banner should be displayed
     * @param serviceUrl the URL to the IdM service
     */
    public IdmInfo(final boolean active, final boolean showBanner, final String serviceUrl) {
      this.active = active;
      this.showBanner = showBanner;
      this.serviceUrl = serviceUrl;
    }

    /**
     * Tells whether the IdM feature is active.
     *
     * @return {@code true} if the IdM feature is active, and {@code false} otherwise
     */
    public boolean isActive() {
      return this.active;
    }

    /**
     * Assigns whether the IdM feature is active.
     *
     * @param active whether the IdM feature is active
     */
    public void setActive(final boolean active) {
      this.active = active;
    }

    /**
     * Tells whether the banner should be displayed.
     *
     * @return {@code true} if the banner should be displayed, and {@code false} otherwise
     */
    public boolean isShowBanner() {
      return this.showBanner;
    }

    /**
     * Assigns whether the banner should be displayed.
     *
     * @param showBanner whether the banner should be displayed
     */
    public void setShowBanner(final boolean showBanner) {
      this.showBanner = showBanner;
    }

    /**
     * Gets the URL to the IdM service.
     *
     * @return the URL to the IdM service
     */
    public String getServiceUrl() {
      return this.serviceUrl;
    }

    /**
     * Assigns the URL to the IdM service.
     *
     * @param serviceUrl the URL to the IdM service
     */
    public void setServiceUrl(final String serviceUrl) {
      this.serviceUrl = serviceUrl;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || this.getClass() != o.getClass()) {
        return false;
      }
      final IdmInfo that = (IdmInfo) o;
      return this.active == that.active && this.showBanner == that.showBanner
          && Objects.equals(this.serviceUrl, that.serviceUrl);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return Objects.hash(this.active, this.showBanner, this.serviceUrl);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return "active=" + this.active + ", showBanner=" + this.showBanner + ", serviceUrl="
          + this.serviceUrl;
    }

  }

}
