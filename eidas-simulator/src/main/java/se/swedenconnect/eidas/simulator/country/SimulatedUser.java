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
package se.swedenconnect.eidas.simulator.country;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

/**
 * A simulated natural person. It is used both for the fixed users of the configuration and for the form where the
 * tester edits the attribute values before a response is sent.
 * <p>
 * The person identifier holds only the identifier part of the eIDAS PersonIdentifier. The country codes in front of
 * it ({@code XX/YY/}) are added when the attribute is released.
 * </p>
 * <p>
 * Names in a non-Latin script are given in the {@code *NonLatin} properties, and the Latin transliteration in the
 * ordinary properties.
 * </p>
 *
 * @author Martin Lindström
 */
public class SimulatedUser {

  /** The identity of the user within the country. */
  private @Nullable String id;

  /** The identifier part of the PersonIdentifier. */
  private @Nullable String personIdentifier;

  /** The current given name(s), in Latin script. */
  private @Nullable String givenName;

  /** The current given name(s), in non-Latin script. */
  private @Nullable String givenNameNonLatin;

  /** The current family name(s), in Latin script. */
  private @Nullable String familyName;

  /** The current family name(s), in non-Latin script. */
  private @Nullable String familyNameNonLatin;

  /** The date of birth, YYYY-MM-DD. */
  private @Nullable String dateOfBirth;

  /** The birth name, in Latin script. */
  private @Nullable String birthName;

  /** The birth name, in non-Latin script. */
  private @Nullable String birthNameNonLatin;

  /** The place of birth. */
  private @Nullable String placeOfBirth;

  /** The gender, {@code Male}, {@code Female} or {@code Unspecified}. */
  private @Nullable String gender;

  /** The current address. */
  private @NonNull Address address = new Address();

  /** The nationality, as an ISO 3166-1 alpha-2 country code. */
  private @Nullable String nationality;

  /** The country of birth, as an ISO 3166-1 alpha-2 country code. */
  private @Nullable String countryOfBirth;

  /** The town of birth. */
  private @Nullable String townOfBirth;

  /** The country of residence, as an ISO 3166-1 alpha-2 country code. */
  private @Nullable String countryOfResidence;

  /** The e-mail address. */
  private @Nullable String emailAddress;

  /** The phone number. */
  private @Nullable String phoneNumber;

  /**
   * Creates a copy of this user.
   *
   * @return a copy
   */
  public @NonNull SimulatedUser copy() {
    final SimulatedUser c = new SimulatedUser();
    c.id = this.id;
    c.personIdentifier = this.personIdentifier;
    c.givenName = this.givenName;
    c.givenNameNonLatin = this.givenNameNonLatin;
    c.familyName = this.familyName;
    c.familyNameNonLatin = this.familyNameNonLatin;
    c.dateOfBirth = this.dateOfBirth;
    c.birthName = this.birthName;
    c.birthNameNonLatin = this.birthNameNonLatin;
    c.placeOfBirth = this.placeOfBirth;
    c.gender = this.gender;
    c.address = this.address.copy();
    c.nationality = this.nationality;
    c.countryOfBirth = this.countryOfBirth;
    c.townOfBirth = this.townOfBirth;
    c.countryOfResidence = this.countryOfResidence;
    c.emailAddress = this.emailAddress;
    c.phoneNumber = this.phoneNumber;
    return c;
  }

  /**
   * Gets a display name for the user, for example "Maximilian Huber".
   *
   * @return the display name
   */
  public @NonNull String getDisplayName() {
    final String name = String.join(" ",
        StringUtils.hasText(this.givenName) ? this.givenName : "",
        StringUtils.hasText(this.familyName) ? this.familyName : "").trim();
    return name.isEmpty() ? String.valueOf(this.id) : name;
  }

  /**
   * Gets the identity of the user within the country.
   *
   * @return the identity
   */
  public @Nullable String getId() {
    return this.id;
  }

  /**
   * Assigns the identity of the user within the country.
   *
   * @param id the identity
   */
  public void setId(final @Nullable String id) {
    this.id = id;
  }

  /**
   * Gets the identifier part of the PersonIdentifier.
   *
   * @return the identifier
   */
  public @Nullable String getPersonIdentifier() {
    return this.personIdentifier;
  }

  /**
   * Assigns the identifier part of the PersonIdentifier.
   *
   * @param personIdentifier the identifier
   */
  public void setPersonIdentifier(final @Nullable String personIdentifier) {
    this.personIdentifier = personIdentifier;
  }

  /**
   * Gets the current given name(s) in Latin script.
   *
   * @return the given name
   */
  public @Nullable String getGivenName() {
    return this.givenName;
  }

  /**
   * Assigns the current given name(s) in Latin script.
   *
   * @param givenName the given name
   */
  public void setGivenName(final @Nullable String givenName) {
    this.givenName = givenName;
  }

  /**
   * Gets the current given name(s) in non-Latin script.
   *
   * @return the given name
   */
  public @Nullable String getGivenNameNonLatin() {
    return this.givenNameNonLatin;
  }

  /**
   * Assigns the current given name(s) in non-Latin script.
   *
   * @param givenNameNonLatin the given name
   */
  public void setGivenNameNonLatin(final @Nullable String givenNameNonLatin) {
    this.givenNameNonLatin = givenNameNonLatin;
  }

  /**
   * Gets the current family name(s) in Latin script.
   *
   * @return the family name
   */
  public @Nullable String getFamilyName() {
    return this.familyName;
  }

  /**
   * Assigns the current family name(s) in Latin script.
   *
   * @param familyName the family name
   */
  public void setFamilyName(final @Nullable String familyName) {
    this.familyName = familyName;
  }

  /**
   * Gets the current family name(s) in non-Latin script.
   *
   * @return the family name
   */
  public @Nullable String getFamilyNameNonLatin() {
    return this.familyNameNonLatin;
  }

  /**
   * Assigns the current family name(s) in non-Latin script.
   *
   * @param familyNameNonLatin the family name
   */
  public void setFamilyNameNonLatin(final @Nullable String familyNameNonLatin) {
    this.familyNameNonLatin = familyNameNonLatin;
  }

  /**
   * Gets the date of birth.
   *
   * @return the date of birth, YYYY-MM-DD
   */
  public @Nullable String getDateOfBirth() {
    return this.dateOfBirth;
  }

  /**
   * Assigns the date of birth.
   *
   * @param dateOfBirth the date of birth, YYYY-MM-DD
   */
  public void setDateOfBirth(final @Nullable String dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  /**
   * Gets the birth name in Latin script.
   *
   * @return the birth name
   */
  public @Nullable String getBirthName() {
    return this.birthName;
  }

  /**
   * Assigns the birth name in Latin script.
   *
   * @param birthName the birth name
   */
  public void setBirthName(final @Nullable String birthName) {
    this.birthName = birthName;
  }

  /**
   * Gets the birth name in non-Latin script.
   *
   * @return the birth name
   */
  public @Nullable String getBirthNameNonLatin() {
    return this.birthNameNonLatin;
  }

  /**
   * Assigns the birth name in non-Latin script.
   *
   * @param birthNameNonLatin the birth name
   */
  public void setBirthNameNonLatin(final @Nullable String birthNameNonLatin) {
    this.birthNameNonLatin = birthNameNonLatin;
  }

  /**
   * Gets the place of birth.
   *
   * @return the place of birth
   */
  public @Nullable String getPlaceOfBirth() {
    return this.placeOfBirth;
  }

  /**
   * Assigns the place of birth.
   *
   * @param placeOfBirth the place of birth
   */
  public void setPlaceOfBirth(final @Nullable String placeOfBirth) {
    this.placeOfBirth = placeOfBirth;
  }

  /**
   * Gets the gender.
   *
   * @return {@code Male}, {@code Female}, {@code Unspecified} or {@code null}
   */
  public @Nullable String getGender() {
    return this.gender;
  }

  /**
   * Assigns the gender.
   *
   * @param gender {@code Male}, {@code Female} or {@code Unspecified}
   */
  public void setGender(final @Nullable String gender) {
    this.gender = gender;
  }

  /**
   * Gets the current address.
   *
   * @return the address
   */
  public @NonNull Address getAddress() {
    return this.address;
  }

  /**
   * Assigns the current address.
   *
   * @param address the address
   */
  public void setAddress(final @Nullable Address address) {
    this.address = address != null ? address : new Address();
  }

  /**
   * Gets the nationality.
   *
   * @return the country code
   */
  public @Nullable String getNationality() {
    return this.nationality;
  }

  /**
   * Assigns the nationality.
   *
   * @param nationality the country code
   */
  public void setNationality(final @Nullable String nationality) {
    this.nationality = nationality;
  }

  /**
   * Gets the country of birth.
   *
   * @return the country code
   */
  public @Nullable String getCountryOfBirth() {
    return this.countryOfBirth;
  }

  /**
   * Assigns the country of birth.
   *
   * @param countryOfBirth the country code
   */
  public void setCountryOfBirth(final @Nullable String countryOfBirth) {
    this.countryOfBirth = countryOfBirth;
  }

  /**
   * Gets the town of birth.
   *
   * @return the town of birth
   */
  public @Nullable String getTownOfBirth() {
    return this.townOfBirth;
  }

  /**
   * Assigns the town of birth.
   *
   * @param townOfBirth the town of birth
   */
  public void setTownOfBirth(final @Nullable String townOfBirth) {
    this.townOfBirth = townOfBirth;
  }

  /**
   * Gets the country of residence.
   *
   * @return the country code
   */
  public @Nullable String getCountryOfResidence() {
    return this.countryOfResidence;
  }

  /**
   * Assigns the country of residence.
   *
   * @param countryOfResidence the country code
   */
  public void setCountryOfResidence(final @Nullable String countryOfResidence) {
    this.countryOfResidence = countryOfResidence;
  }

  /**
   * Gets the e-mail address.
   *
   * @return the e-mail address
   */
  public @Nullable String getEmailAddress() {
    return this.emailAddress;
  }

  /**
   * Assigns the e-mail address.
   *
   * @param emailAddress the e-mail address
   */
  public void setEmailAddress(final @Nullable String emailAddress) {
    this.emailAddress = emailAddress;
  }

  /**
   * Gets the phone number.
   *
   * @return the phone number
   */
  public @Nullable String getPhoneNumber() {
    return this.phoneNumber;
  }

  /**
   * Assigns the phone number.
   *
   * @param phoneNumber the phone number
   */
  public void setPhoneNumber(final @Nullable String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  /**
   * The current address of a user, with the parts of the eIDAS {@code CurrentAddressType}.
   */
  public static class Address {

    /** The PO box. */
    private @Nullable String poBox;

    /** The locator designator, for example a house number. */
    private @Nullable String locatorDesignator;

    /** The locator name, for example a building name. */
    private @Nullable String locatorName;

    /** The address area, for example a district. */
    private @Nullable String cvAddressArea;

    /** The thoroughfare, for example a street name. */
    private @Nullable String thoroughfare;

    /** The post name, for example a town. */
    private @Nullable String postName;

    /** The first line of the administrative unit, for example a country code. */
    private @Nullable String adminUnitFirstLine;

    /** The second line of the administrative unit, for example a region. */
    private @Nullable String adminUnitSecondLine;

    /** The post code. */
    private @Nullable String postCode;

    /**
     * Creates a copy of this address.
     *
     * @return a copy
     */
    public @NonNull Address copy() {
      final Address c = new Address();
      c.poBox = this.poBox;
      c.locatorDesignator = this.locatorDesignator;
      c.locatorName = this.locatorName;
      c.cvAddressArea = this.cvAddressArea;
      c.thoroughfare = this.thoroughfare;
      c.postName = this.postName;
      c.adminUnitFirstLine = this.adminUnitFirstLine;
      c.adminUnitSecondLine = this.adminUnitSecondLine;
      c.postCode = this.postCode;
      return c;
    }

    /**
     * Tells whether any part of the address is assigned.
     *
     * @return {@code true} if the address has a value and {@code false} otherwise
     */
    public boolean hasValue() {
      return StringUtils.hasText(this.poBox) || StringUtils.hasText(this.locatorDesignator)
          || StringUtils.hasText(this.locatorName) || StringUtils.hasText(this.cvAddressArea)
          || StringUtils.hasText(this.thoroughfare) || StringUtils.hasText(this.postName)
          || StringUtils.hasText(this.adminUnitFirstLine) || StringUtils.hasText(this.adminUnitSecondLine)
          || StringUtils.hasText(this.postCode);
    }

    /**
     * Gets the PO box.
     *
     * @return the PO box
     */
    public @Nullable String getPoBox() {
      return this.poBox;
    }

    /**
     * Assigns the PO box.
     *
     * @param poBox the PO box
     */
    public void setPoBox(final @Nullable String poBox) {
      this.poBox = poBox;
    }

    /**
     * Gets the locator designator.
     *
     * @return the locator designator
     */
    public @Nullable String getLocatorDesignator() {
      return this.locatorDesignator;
    }

    /**
     * Assigns the locator designator.
     *
     * @param locatorDesignator the locator designator
     */
    public void setLocatorDesignator(final @Nullable String locatorDesignator) {
      this.locatorDesignator = locatorDesignator;
    }

    /**
     * Gets the locator name.
     *
     * @return the locator name
     */
    public @Nullable String getLocatorName() {
      return this.locatorName;
    }

    /**
     * Assigns the locator name.
     *
     * @param locatorName the locator name
     */
    public void setLocatorName(final @Nullable String locatorName) {
      this.locatorName = locatorName;
    }

    /**
     * Gets the address area.
     *
     * @return the address area
     */
    public @Nullable String getCvAddressArea() {
      return this.cvAddressArea;
    }

    /**
     * Assigns the address area.
     *
     * @param cvAddressArea the address area
     */
    public void setCvAddressArea(final @Nullable String cvAddressArea) {
      this.cvAddressArea = cvAddressArea;
    }

    /**
     * Gets the thoroughfare.
     *
     * @return the thoroughfare
     */
    public @Nullable String getThoroughfare() {
      return this.thoroughfare;
    }

    /**
     * Assigns the thoroughfare.
     *
     * @param thoroughfare the thoroughfare
     */
    public void setThoroughfare(final @Nullable String thoroughfare) {
      this.thoroughfare = thoroughfare;
    }

    /**
     * Gets the post name.
     *
     * @return the post name
     */
    public @Nullable String getPostName() {
      return this.postName;
    }

    /**
     * Assigns the post name.
     *
     * @param postName the post name
     */
    public void setPostName(final @Nullable String postName) {
      this.postName = postName;
    }

    /**
     * Gets the first line of the administrative unit.
     *
     * @return the first line
     */
    public @Nullable String getAdminUnitFirstLine() {
      return this.adminUnitFirstLine;
    }

    /**
     * Assigns the first line of the administrative unit.
     *
     * @param adminUnitFirstLine the first line
     */
    public void setAdminUnitFirstLine(final @Nullable String adminUnitFirstLine) {
      this.adminUnitFirstLine = adminUnitFirstLine;
    }

    /**
     * Gets the second line of the administrative unit.
     *
     * @return the second line
     */
    public @Nullable String getAdminUnitSecondLine() {
      return this.adminUnitSecondLine;
    }

    /**
     * Assigns the second line of the administrative unit.
     *
     * @param adminUnitSecondLine the second line
     */
    public void setAdminUnitSecondLine(final @Nullable String adminUnitSecondLine) {
      this.adminUnitSecondLine = adminUnitSecondLine;
    }

    /**
     * Gets the post code.
     *
     * @return the post code
     */
    public @Nullable String getPostCode() {
      return this.postCode;
    }

    /**
     * Assigns the post code.
     *
     * @param postCode the post code
     */
    public void setPostCode(final @Nullable String postCode) {
      this.postCode = postCode;
    }

  }

}
