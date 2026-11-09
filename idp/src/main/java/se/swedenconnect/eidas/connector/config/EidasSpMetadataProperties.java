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
package se.swedenconnect.eidas.connector.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration properties for the eIDAS SP metadata.
 *
 * @author Martin Lindström
 */
public class EidasSpMetadataProperties implements InitializingBean {

  /**
   * A template for the SP metadata.
   */
  private Resource template;

  /**
   * Tells how long the published SP metadata can remain in a cache.
   */
  private Duration cacheDuration;

  /**
   * Tells for how long a published metadata entry should be valid.
   */
  private Duration validityPeriod;

  /**
   * The {@code alg:DigestMethod} elements to include in the metadata.
   */
  private List<String> digestMethods;

  /**
   * Whether {@code alg:DigestMethod} elements should be placed in an {@code Extensions} element under the role
   * descriptor (i.e., the {@code SPSSODescriptor}). If {@code false}, the {@code alg:DigestMethod} elements are
   * included as elements in the {@code Extensions} element of the {@code EntityDescriptor}.
   */
  private boolean includeDigestMethodsUnderRole;

  /**
   * The {@code alg:SigningMethod} elements to include in the metadata.
   */
  private List<SigningMethod> signingMethods;

  /**
   * Whether {@code alg:SigningMethod} elements should be placed in an {@code Extensions} element under the role
   * descriptor (i.e., the {@code SPSSODescriptor}). If {@code false}, the {@code alg:SigningMethod} elements are
   * included as elements in the {@code Extensions} element of the {@code EntityDescriptor}.
   */
  private boolean includeSigningMethodsUnderRole;

  /**
   * The {@code md:EncryptionMethod} elements that should be included under the {@code md:KeyDescriptor} for the
   * encryption key. Note that these algorithms must match the configured encryption key.
   */
  private List<EncryptionMethod> encryptionMethods;

  /**
   * The metadata {@code Organization} element.
   */
  private Organization organization;

  /**
   * The metadata {@code ContactPerson} elements, keyed by type.
   */
  private Map<ContactPersonType, ContactPerson> contactPersons;

  /**
   * The value to insert for the eIDAS entity category
   * {@code http://eidas.europa.eu/entity-attributes/application-identifier}. The current version of the connector will
   * always be appended to this value.
   */
  private String applicationIdentifierPrefix;

  /**
   * The values to use for the eIDAS entity category {@code http://eidas.europa.eu/entity-attributes/protocol-version}.
   */
  private final List<String> protocolVersions = new ArrayList<>();

  /**
   * Gets a template for the SP metadata.
   *
   * @return a template for the SP metadata
   */
  public Resource getTemplate() {
    return this.template;
  }

  /**
   * Assigns a template for the SP metadata.
   *
   * @param template a template for the SP metadata
   */
  public void setTemplate(final Resource template) {
    this.template = template;
  }

  /**
   * Gets how long the published SP metadata can remain in a cache.
   *
   * @return how long the published SP metadata can remain in a cache
   */
  public Duration getCacheDuration() {
    return this.cacheDuration;
  }

  /**
   * Assigns how long the published SP metadata can remain in a cache.
   *
   * @param cacheDuration how long the published SP metadata can remain in a cache
   */
  public void setCacheDuration(final Duration cacheDuration) {
    this.cacheDuration = cacheDuration;
  }

  /**
   * Gets for how long a published metadata entry should be valid.
   *
   * @return for how long a published metadata entry should be valid
   */
  public Duration getValidityPeriod() {
    return this.validityPeriod;
  }

  /**
   * Assigns for how long a published metadata entry should be valid.
   *
   * @param validityPeriod for how long a published metadata entry should be valid
   */
  public void setValidityPeriod(final Duration validityPeriod) {
    this.validityPeriod = validityPeriod;
  }

  /**
   * Gets the {@code alg:DigestMethod} elements to include in the metadata.
   *
   * @return the {@code alg:DigestMethod} elements to include in the metadata
   */
  public List<String> getDigestMethods() {
    return this.digestMethods;
  }

  /**
   * Assigns the {@code alg:DigestMethod} elements to include in the metadata.
   *
   * @param digestMethods the {@code alg:DigestMethod} elements to include in the metadata
   */
  public void setDigestMethods(final List<String> digestMethods) {
    this.digestMethods = digestMethods;
  }

  /**
   * Gets whether {@code alg:DigestMethod} elements go under the role descriptor.
   *
   * @return whether {@code alg:DigestMethod} elements go under the role descriptor
   */
  public boolean isIncludeDigestMethodsUnderRole() {
    return this.includeDigestMethodsUnderRole;
  }

  /**
   * Assigns whether {@code alg:DigestMethod} elements go under the role descriptor.
   *
   * @param includeDigestMethodsUnderRole whether {@code alg:DigestMethod} elements go under the role descriptor
   */
  public void setIncludeDigestMethodsUnderRole(final boolean includeDigestMethodsUnderRole) {
    this.includeDigestMethodsUnderRole = includeDigestMethodsUnderRole;
  }

  /**
   * Gets the {@code alg:SigningMethod} elements to include in the metadata.
   *
   * @return the {@code alg:SigningMethod} elements to include in the metadata
   */
  public List<SigningMethod> getSigningMethods() {
    return this.signingMethods;
  }

  /**
   * Assigns the {@code alg:SigningMethod} elements to include in the metadata.
   *
   * @param signingMethods the {@code alg:SigningMethod} elements to include in the metadata
   */
  public void setSigningMethods(final List<SigningMethod> signingMethods) {
    this.signingMethods = signingMethods;
  }

  /**
   * Gets whether {@code alg:SigningMethod} elements go under the role descriptor.
   *
   * @return whether {@code alg:SigningMethod} elements go under the role descriptor
   */
  public boolean isIncludeSigningMethodsUnderRole() {
    return this.includeSigningMethodsUnderRole;
  }

  /**
   * Assigns whether {@code alg:SigningMethod} elements go under the role descriptor.
   *
   * @param includeSigningMethodsUnderRole whether {@code alg:SigningMethod} elements go under the role descriptor
   */
  public void setIncludeSigningMethodsUnderRole(final boolean includeSigningMethodsUnderRole) {
    this.includeSigningMethodsUnderRole = includeSigningMethodsUnderRole;
  }

  /**
   * Gets the {@code md:EncryptionMethod} elements for the encryption key.
   *
   * @return the {@code md:EncryptionMethod} elements for the encryption key
   */
  public List<EncryptionMethod> getEncryptionMethods() {
    return this.encryptionMethods;
  }

  /**
   * Assigns the {@code md:EncryptionMethod} elements for the encryption key.
   *
   * @param encryptionMethods the {@code md:EncryptionMethod} elements for the encryption key
   */
  public void setEncryptionMethods(final List<EncryptionMethod> encryptionMethods) {
    this.encryptionMethods = encryptionMethods;
  }

  /**
   * Gets the metadata {@code Organization} element.
   *
   * @return the metadata {@code Organization} element
   */
  public Organization getOrganization() {
    return this.organization;
  }

  /**
   * Assigns the metadata {@code Organization} element.
   *
   * @param organization the metadata {@code Organization} element
   */
  public void setOrganization(final Organization organization) {
    this.organization = organization;
  }

  /**
   * Gets the metadata {@code ContactPerson} elements, keyed by type.
   *
   * @return the metadata {@code ContactPerson} elements, keyed by type
   */
  public Map<ContactPersonType, ContactPerson> getContactPersons() {
    return this.contactPersons;
  }

  /**
   * Assigns the metadata {@code ContactPerson} elements, keyed by type.
   *
   * @param contactPersons the metadata {@code ContactPerson} elements, keyed by type
   */
  public void setContactPersons(final Map<ContactPersonType, ContactPerson> contactPersons) {
    this.contactPersons = contactPersons;
  }

  /**
   * Gets the prefix for the eIDAS application-identifier entity category.
   *
   * @return the prefix for the eIDAS application-identifier entity category
   */
  public String getApplicationIdentifierPrefix() {
    return this.applicationIdentifierPrefix;
  }

  /**
   * Assigns the prefix for the eIDAS application-identifier entity category.
   *
   * @param applicationIdentifierPrefix the prefix for the eIDAS application-identifier entity category
   */
  public void setApplicationIdentifierPrefix(final String applicationIdentifierPrefix) {
    this.applicationIdentifierPrefix = applicationIdentifierPrefix;
  }

  /**
   * Gets the values for the eIDAS protocol-version entity category.
   *
   * @return the values for the eIDAS protocol-version entity category
   */
  public List<String> getProtocolVersions() {
    return this.protocolVersions;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() {
    if (!StringUtils.hasText(this.applicationIdentifierPrefix)) {
      this.applicationIdentifierPrefix = "SE:connector:";
    }
    if (this.cacheDuration == null) {
      this.cacheDuration = Duration.ofDays(1);
    }
    if (this.validityPeriod == null) {
      this.validityPeriod = Duration.ofDays(7);
    }
  }

  /**
   * Settings for {@code alg:SigningMethod} elements.
   */
  public static class SigningMethod {

    /**
     * Identifies the algorithm by means of the URL defined for its use with the XML Signature specification.
     */
    private String algorithm;

    /**
     * The smallest key size, in bits, that the entity supports in conjunction with the algorithm. If omitted, no
     * minimum is implied.
     */
    private Integer minKeySize;

    /**
     * The largest key size, in bits, that the entity supports in conjunction with the algorithm. If omitted, no
     * maximum is implied.
     */
    private Integer maxKeySize;

    /**
     * Gets the algorithm URI.
     *
     * @return the algorithm URI
     */
    public String getAlgorithm() {
      return this.algorithm;
    }

    /**
     * Assigns the algorithm URI.
     *
     * @param algorithm the algorithm URI
     */
    public void setAlgorithm(final String algorithm) {
      this.algorithm = algorithm;
    }

    /**
     * Gets the smallest key size (in bits).
     *
     * @return the smallest key size (in bits)
     */
    public Integer getMinKeySize() {
      return this.minKeySize;
    }

    /**
     * Assigns the smallest key size (in bits).
     *
     * @param minKeySize the smallest key size (in bits)
     */
    public void setMinKeySize(final Integer minKeySize) {
      this.minKeySize = minKeySize;
    }

    /**
     * Gets the largest key size (in bits).
     *
     * @return the largest key size (in bits)
     */
    public Integer getMaxKeySize() {
      return this.maxKeySize;
    }

    /**
     * Assigns the largest key size (in bits).
     *
     * @param maxKeySize the largest key size (in bits)
     */
    public void setMaxKeySize(final Integer maxKeySize) {
      this.maxKeySize = maxKeySize;
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
      final SigningMethod other = (SigningMethod) o;
      return Objects.equals(this.algorithm, other.algorithm)
          && Objects.equals(this.minKeySize, other.minKeySize)
          && Objects.equals(this.maxKeySize, other.maxKeySize);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return Objects.hash(this.algorithm, this.minKeySize, this.maxKeySize);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return "algorithm=" + this.algorithm
          + ", minKeySize=" + this.minKeySize
          + ", maxKeySize=" + this.maxKeySize;
    }
  }

  /**
   * Settings for {@code md:EncryptionMethod} elements.
   */
  public static class EncryptionMethod {

    /**
     * The algorithm URI of the encryption method.
     */
    private String algorithm;

    /**
     * The key size.
     */
    private Integer keySize;

    /**
     * The OAEP parameters (in Base64-encoding).
     */
    private String oaepParams;

    /**
     * If {@code algorithm} indicates a key transport algorithm where the digest algorithm needs to be given, this
     * field should be set to this algorithm URI.
     */
    private String digestMethod;

    /**
     * Gets the algorithm URI of the encryption method.
     *
     * @return the algorithm URI of the encryption method
     */
    public String getAlgorithm() {
      return this.algorithm;
    }

    /**
     * Assigns the algorithm URI of the encryption method.
     *
     * @param algorithm the algorithm URI of the encryption method
     */
    public void setAlgorithm(final String algorithm) {
      this.algorithm = algorithm;
    }

    /**
     * Gets the key size.
     *
     * @return the key size
     */
    public Integer getKeySize() {
      return this.keySize;
    }

    /**
     * Assigns the key size.
     *
     * @param keySize the key size
     */
    public void setKeySize(final Integer keySize) {
      this.keySize = keySize;
    }

    /**
     * Gets the OAEP parameters (in Base64-encoding).
     *
     * @return the OAEP parameters (in Base64-encoding)
     */
    public String getOaepParams() {
      return this.oaepParams;
    }

    /**
     * Assigns the OAEP parameters (in Base64-encoding).
     *
     * @param oaepParams the OAEP parameters (in Base64-encoding)
     */
    public void setOaepParams(final String oaepParams) {
      this.oaepParams = oaepParams;
    }

    /**
     * Gets the digest method algorithm URI.
     *
     * @return the digest method algorithm URI
     */
    public String getDigestMethod() {
      return this.digestMethod;
    }

    /**
     * Assigns the digest method algorithm URI.
     *
     * @param digestMethod the digest method algorithm URI
     */
    public void setDigestMethod(final String digestMethod) {
      this.digestMethod = digestMethod;
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
      final EncryptionMethod other = (EncryptionMethod) o;
      return Objects.equals(this.algorithm, other.algorithm)
          && Objects.equals(this.keySize, other.keySize)
          && Objects.equals(this.oaepParams, other.oaepParams)
          && Objects.equals(this.digestMethod, other.digestMethod);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return Objects.hash(this.algorithm, this.keySize, this.oaepParams, this.digestMethod);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return "algorithm=" + this.algorithm
          + ", keySize=" + this.keySize
          + ", oaepParams=" + this.oaepParams
          + ", digestMethod=" + this.digestMethod;
    }
  }

  /**
   * Settings for the {@code Organization} metadata element.
   */
  public static class Organization {

    /**
     * The {@code OrganizationName}. The map key is the language tag and value is display name for that language.
     */
    private Map<String, String> names;

    /**
     * The {@code OrganizationDisplayName}. The map key is the language tag and value is display name for that
     * language.
     */
    private Map<String, String> displayNames;

    /**
     * The {@code OrganizationURL}. The map key is the language tag and value is display name for that language.
     */
    private Map<String, String> urls;

    /**
     * Gets the {@code OrganizationName} values (keyed by language tag).
     *
     * @return the {@code OrganizationName} values (keyed by language tag)
     */
    public Map<String, String> getNames() {
      return this.names;
    }

    /**
     * Assigns the {@code OrganizationName} values (keyed by language tag).
     *
     * @param names the {@code OrganizationName} values (keyed by language tag)
     */
    public void setNames(final Map<String, String> names) {
      this.names = names;
    }

    /**
     * Gets the {@code OrganizationDisplayName} values (keyed by language tag).
     *
     * @return the {@code OrganizationDisplayName} values (keyed by language tag)
     */
    public Map<String, String> getDisplayNames() {
      return this.displayNames;
    }

    /**
     * Assigns the {@code OrganizationDisplayName} values (keyed by language tag).
     *
     * @param displayNames the {@code OrganizationDisplayName} values (keyed by language tag)
     */
    public void setDisplayNames(final Map<String, String> displayNames) {
      this.displayNames = displayNames;
    }

    /**
     * Gets the {@code OrganizationURL} values (keyed by language tag).
     *
     * @return the {@code OrganizationURL} values (keyed by language tag)
     */
    public Map<String, String> getUrls() {
      return this.urls;
    }

    /**
     * Assigns the {@code OrganizationURL} values (keyed by language tag).
     *
     * @param urls the {@code OrganizationURL} values (keyed by language tag)
     */
    public void setUrls(final Map<String, String> urls) {
      this.urls = urls;
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
      final Organization other = (Organization) o;
      return Objects.equals(this.names, other.names)
          && Objects.equals(this.displayNames, other.displayNames)
          && Objects.equals(this.urls, other.urls);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return Objects.hash(this.names, this.displayNames, this.urls);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return "names=" + this.names
          + ", displayNames=" + this.displayNames
          + ", urls=" + this.urls;
    }
  }

  /**
   * The type of contact person.
   */
  public enum ContactPersonType {
    /** Technical contact. */
    technical,
    /** Support contact. */
    support,
    /** Administrative contact. */
    administrative,
    /** Billing contact. */
    billing,
    /** Other contact. */
    other,
    /** Security contact, published as an "other" contact. */
    security
  }

  /**
   * Settings for the {@code ContactPerson} metadata element.
   */
  public static class ContactPerson {

    /**
     * The {@code Company} element.
     */
    private String company;

    /**
     * The {@code GivenName} element.
     */
    private String givenName;

    /**
     * The {@code SurName} element.
     */
    private String surname;

    /**
     * The {@code EmailAddress} elements.
     */
    private List<String> emailAddresses;

    /**
     * The {@code TelephoneNumber} elements.
     */
    private List<String> telephoneNumbers;

    /**
     * Gets the {@code Company} element.
     *
     * @return the {@code Company} element
     */
    public String getCompany() {
      return this.company;
    }

    /**
     * Assigns the {@code Company} element.
     *
     * @param company the {@code Company} element
     */
    public void setCompany(final String company) {
      this.company = company;
    }

    /**
     * Gets the {@code GivenName} element.
     *
     * @return the {@code GivenName} element
     */
    public String getGivenName() {
      return this.givenName;
    }

    /**
     * Assigns the {@code GivenName} element.
     *
     * @param givenName the {@code GivenName} element
     */
    public void setGivenName(final String givenName) {
      this.givenName = givenName;
    }

    /**
     * Gets the {@code SurName} element.
     *
     * @return the {@code SurName} element
     */
    public String getSurname() {
      return this.surname;
    }

    /**
     * Assigns the {@code SurName} element.
     *
     * @param surname the {@code SurName} element
     */
    public void setSurname(final String surname) {
      this.surname = surname;
    }

    /**
     * Gets the {@code EmailAddress} elements.
     *
     * @return the {@code EmailAddress} elements
     */
    public List<String> getEmailAddresses() {
      return this.emailAddresses;
    }

    /**
     * Assigns the {@code EmailAddress} elements.
     *
     * @param emailAddresses the {@code EmailAddress} elements
     */
    public void setEmailAddresses(final List<String> emailAddresses) {
      this.emailAddresses = emailAddresses;
    }

    /**
     * Gets the {@code TelephoneNumber} elements.
     *
     * @return the {@code TelephoneNumber} elements
     */
    public List<String> getTelephoneNumbers() {
      return this.telephoneNumbers;
    }

    /**
     * Assigns the {@code TelephoneNumber} elements.
     *
     * @param telephoneNumbers the {@code TelephoneNumber} elements
     */
    public void setTelephoneNumbers(final List<String> telephoneNumbers) {
      this.telephoneNumbers = telephoneNumbers;
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
      final ContactPerson other = (ContactPerson) o;
      return Objects.equals(this.company, other.company)
          && Objects.equals(this.givenName, other.givenName)
          && Objects.equals(this.surname, other.surname)
          && Objects.equals(this.emailAddresses, other.emailAddresses)
          && Objects.equals(this.telephoneNumbers, other.telephoneNumbers);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return Objects.hash(this.company, this.givenName, this.surname, this.emailAddresses, this.telephoneNumbers);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return "company=" + this.company
          + ", givenName=" + this.givenName
          + ", surname=" + this.surname
          + ", emailAddresses=" + this.emailAddresses
          + ", telephoneNumbers=" + this.telephoneNumbers;
    }
  }

}
