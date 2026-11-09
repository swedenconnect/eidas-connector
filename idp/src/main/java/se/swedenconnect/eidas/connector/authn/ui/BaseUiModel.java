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

import java.util.Objects;

/**
 * Base UI model class.
 *
 * @author Martin Lindström
 */
public class BaseUiModel {

  /**
   * The SP UI info.
   */
  private SpInfo spInfo;

  /**
   * The accessibility report URL.
   */
  private String accessibilityUrl;

  /**
   * Gets the SP UI info.
   *
   * @return the SP UI info
   */
  public SpInfo getSpInfo() {
    return this.spInfo;
  }

  /**
   * Assigns the SP UI info.
   *
   * @param spInfo the SP UI info
   */
  public void setSpInfo(final SpInfo spInfo) {
    this.spInfo = spInfo;
  }

  /**
   * Gets the accessibility report URL.
   *
   * @return the accessibility report URL
   */
  public String getAccessibilityUrl() {
    return this.accessibilityUrl;
  }

  /**
   * Assigns the accessibility report URL.
   *
   * @param accessibilityUrl the accessibility report URL
   */
  public void setAccessibilityUrl(final String accessibilityUrl) {
    this.accessibilityUrl = accessibilityUrl;
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
    final BaseUiModel that = (BaseUiModel) o;
    return Objects.equals(this.spInfo, that.spInfo) && Objects.equals(this.accessibilityUrl, that.accessibilityUrl);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Objects.hash(this.spInfo, this.accessibilityUrl);
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "spInfo=[" + this.spInfo + "], accessibilityUrl=" + this.accessibilityUrl;
  }

  /**
   * SP UI Info.
   */
  public static class SpInfo {

    /**
     * The SP display name.
     */
    private String displayName;

    /**
     * The SP logotype URL.
     */
    private String logoUrl;

    /**
     * Gets the SP display name.
     *
     * @return the SP display name
     */
    public String getDisplayName() {
      return this.displayName;
    }

    /**
     * Assigns the SP display name.
     *
     * @param displayName the SP display name
     */
    public void setDisplayName(final String displayName) {
      this.displayName = displayName;
    }

    /**
     * Gets the SP logotype URL.
     *
     * @return the SP logotype URL
     */
    public String getLogoUrl() {
      return this.logoUrl;
    }

    /**
     * Assigns the SP logotype URL.
     *
     * @param logoUrl the SP logotype URL
     */
    public void setLogoUrl(final String logoUrl) {
      this.logoUrl = logoUrl;
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
      final SpInfo that = (SpInfo) o;
      return Objects.equals(this.displayName, that.displayName) && Objects.equals(this.logoUrl, that.logoUrl);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return Objects.hash(this.displayName, this.logoUrl);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return "displayName=" + this.displayName + ", logoUrl=" + this.logoUrl;
    }
  }

}
