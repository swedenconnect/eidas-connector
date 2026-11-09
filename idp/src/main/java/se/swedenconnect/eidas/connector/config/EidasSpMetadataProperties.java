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

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for the eIDAS SP metadata.
 *
 * @author Martin Lindström
 */
public class EidasSpMetadataProperties implements InitializingBean {

  /**
   * A template for the SP metadata.
   */
  @Setter
  @Getter
  private Resource template;

  /**
   * Tells how long the published SP metadata can remain in a cache.
   */
  @Setter
  @Getter
  private Duration cacheDuration;

  /**
   * Tells for how long a published metadata entry should be valid.
   */
  @Setter
  @Getter
  private Duration validityPeriod;

  /**
   * The {@code alg:DigestMethod} elements to include in the metadata.
   */
  @Setter
  @Getter
  private List<String> digestMethods;

  /**
   * Whether {@code alg:DigestMethod} elements should be placed in an {@code Extensions} element under the role
   * descriptor (i.e., the {@code SPSSODescriptor}). If {@code false}, the {@code alg:DigestMethod} elements are
   * included as elements in the {@code Extensions} element of the {@code EntityDescriptor}.
   */
  @Setter
  @Getter
  private boolean includeDigestMethodsUnderRole;

  /**
   * The {@code alg:SigningMethod} elements to include in the metadata.
   */
  @Setter
  @Getter
  private List<SigningMethod> signingMethods;

  /**
   * Whether {@code alg:SigningMethod} elements should be placed in an {@code Extensions} element under the role
   * descriptor (i.e., the {@code SPSSODescriptor}). If {@code false}, the {@code alg:SigningMethod} elements are
   * included as elements in the {@code Extensions} element of the {@code EntityDescriptor}.
   */
  @Setter
  @Getter
  private boolean includeSigningMethodsUnderRole;

  /**
   * The {@code md:EncryptionMethod} elements that should be included under the {@code md:KeyDescriptor} for the
   * encryption key. Note that these algorithms must match the configured encryption key.
   */
  @Setter
  @Getter
  private List<EncryptionMethod> encryptionMethods;

  /**
   * The metadata {@code Organization} element.
   */
  @Setter
  @Getter
  private Organization organization;

  /**
   * The metadata {@code ContactPerson} elements, keyed by type.
   */
  @Setter
  @Getter
  private Map<ContactPersonType, ContactPerson> contactPersons;

  /**
   * The value to insert for the eIDAS entity category
   * {@code http://eidas.europa.eu/entity-attributes/application-identifier}. The current version of the connector will
   * always be appended to this value.
   */
  @Getter
  @Setter
  private String applicationIdentifierPrefix;

  /**
   * The values to use for the eIDAS entity category {@code http://eidas.europa.eu/entity-attributes/protocol-version}.
   */
  @Getter
  private final List<String> protocolVersions = new ArrayList<>();

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
  @Data
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
  }

  /**
   * Settings for {@code md:EncryptionMethod} elements.
   */
  @Data
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
  }

  /**
   * Settings for the {@code Organization} metadata element.
   */
  @Data
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
  @Data
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
  }

}
