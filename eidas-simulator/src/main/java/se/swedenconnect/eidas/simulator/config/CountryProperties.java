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
package se.swedenconnect.eidas.simulator.config;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import se.swedenconnect.eidas.simulator.country.SimulatedUser;

import java.util.ArrayList;
import java.util.List;

/**
 * The settings of one simulated country. They mirror the country's entry in the production eIDAS metadata, except
 * for URLs and keys.
 *
 * @author Martin Lindström
 */
public class CountryProperties {

  /**
   * The English name of the country.
   */
  private @Nullable String name;

  /**
   * The values of the {@code http://eidas.europa.eu/entity-attributes/protocol-version} entity attribute.
   */
  private @NonNull List<String> protocolVersions = new ArrayList<>();

  /**
   * The value of the {@code http://eidas.europa.eu/entity-attributes/application-identifier} entity attribute. No
   * attribute is published if not set.
   */
  private @Nullable String applicationIdentifier;

  /**
   * The values of the {@code urn:oasis:names:tc:SAML:attribute:assurance-certification} entity attribute, that is,
   * the levels of assurance that the country delivers.
   */
  private @NonNull List<String> assuranceCertifications = new ArrayList<>();

  /**
   * The values of the {@code http://eidas.europa.eu/entity-attributes/supported-encryption-algorithms} entity
   * attribute. No attribute is published if empty.
   */
  private @NonNull List<String> supportedEncryptionAlgorithms = new ArrayList<>();

  /**
   * The values of the {@code http://macedir.org/entity-category} entity attribute. No attribute is published if
   * empty.
   */
  private @NonNull List<String> entityCategories = new ArrayList<>();

  /**
   * The digest methods that the country supports ({@code alg:DigestMethod}).
   */
  private @NonNull List<String> digestMethods = new ArrayList<>();

  /**
   * The signing methods that the country supports ({@code alg:SigningMethod}).
   */
  private @NonNull List<SigningMethod> signingMethods = new ArrayList<>();

  /**
   * The NameID formats that the country supports.
   */
  private @NonNull List<String> nameIdFormats = new ArrayList<>();

  /**
   * The bindings of the {@code SingleSignOnService} endpoints.
   */
  private @NonNull List<String> bindings = new ArrayList<>();

  /**
   * The encryption key of the country. If not set, the metadata has no encryption key.
   */
  private @Nullable EncryptionKey encryptionKey;

  /**
   * The attributes that the country supports, as published in its metadata.
   */
  private @NonNull List<MetadataAttribute> attributes = new ArrayList<>();

  /**
   * The fixed users of the country.
   */
  private @NonNull List<SimulatedUser> users = new ArrayList<>();

  /**
   * Gets the English name of the country.
   *
   * @return the name
   */
  public @Nullable String getName() {
    return this.name;
  }

  /**
   * Assigns the English name of the country.
   *
   * @param name the name
   */
  public void setName(final @Nullable String name) {
    this.name = name;
  }

  /**
   * Gets the protocol versions.
   *
   * @return the protocol versions
   */
  public @NonNull List<String> getProtocolVersions() {
    return this.protocolVersions;
  }

  /**
   * Assigns the protocol versions.
   *
   * @param protocolVersions the protocol versions
   */
  public void setProtocolVersions(final @NonNull List<String> protocolVersions) {
    this.protocolVersions = protocolVersions;
  }

  /**
   * Gets the application identifier.
   *
   * @return the application identifier
   */
  public @Nullable String getApplicationIdentifier() {
    return this.applicationIdentifier;
  }

  /**
   * Assigns the application identifier.
   *
   * @param applicationIdentifier the application identifier
   */
  public void setApplicationIdentifier(final @Nullable String applicationIdentifier) {
    this.applicationIdentifier = applicationIdentifier;
  }

  /**
   * Gets the levels of assurance that the country delivers.
   *
   * @return the level of assurance URIs
   */
  public @NonNull List<String> getAssuranceCertifications() {
    return this.assuranceCertifications;
  }

  /**
   * Assigns the levels of assurance that the country delivers.
   *
   * @param assuranceCertifications the level of assurance URIs
   */
  public void setAssuranceCertifications(final @NonNull List<String> assuranceCertifications) {
    this.assuranceCertifications = assuranceCertifications;
  }

  /**
   * Gets the values of the supported encryption algorithms entity attribute.
   *
   * @return the algorithm URIs
   */
  public @NonNull List<String> getSupportedEncryptionAlgorithms() {
    return this.supportedEncryptionAlgorithms;
  }

  /**
   * Assigns the values of the supported encryption algorithms entity attribute.
   *
   * @param supportedEncryptionAlgorithms the algorithm URIs
   */
  public void setSupportedEncryptionAlgorithms(final @NonNull List<String> supportedEncryptionAlgorithms) {
    this.supportedEncryptionAlgorithms = supportedEncryptionAlgorithms;
  }

  /**
   * Gets the entity categories.
   *
   * @return the entity categories
   */
  public @NonNull List<String> getEntityCategories() {
    return this.entityCategories;
  }

  /**
   * Assigns the entity categories.
   *
   * @param entityCategories the entity categories
   */
  public void setEntityCategories(final @NonNull List<String> entityCategories) {
    this.entityCategories = entityCategories;
  }

  /**
   * Gets the supported digest methods.
   *
   * @return the algorithm URIs
   */
  public @NonNull List<String> getDigestMethods() {
    return this.digestMethods;
  }

  /**
   * Assigns the supported digest methods.
   *
   * @param digestMethods the algorithm URIs
   */
  public void setDigestMethods(final @NonNull List<String> digestMethods) {
    this.digestMethods = digestMethods;
  }

  /**
   * Gets the supported signing methods.
   *
   * @return the signing methods
   */
  public @NonNull List<SigningMethod> getSigningMethods() {
    return this.signingMethods;
  }

  /**
   * Assigns the supported signing methods.
   *
   * @param signingMethods the signing methods
   */
  public void setSigningMethods(final @NonNull List<SigningMethod> signingMethods) {
    this.signingMethods = signingMethods;
  }

  /**
   * Gets the supported NameID formats.
   *
   * @return the NameID formats
   */
  public @NonNull List<String> getNameIdFormats() {
    return this.nameIdFormats;
  }

  /**
   * Assigns the supported NameID formats.
   *
   * @param nameIdFormats the NameID formats
   */
  public void setNameIdFormats(final @NonNull List<String> nameIdFormats) {
    this.nameIdFormats = nameIdFormats;
  }

  /**
   * Gets the bindings of the {@code SingleSignOnService} endpoints.
   *
   * @return the binding URIs
   */
  public @NonNull List<String> getBindings() {
    return this.bindings;
  }

  /**
   * Assigns the bindings of the {@code SingleSignOnService} endpoints.
   *
   * @param bindings the binding URIs
   */
  public void setBindings(final @NonNull List<String> bindings) {
    this.bindings = bindings;
  }

  /**
   * Gets the encryption key settings.
   *
   * @return the encryption key settings, or {@code null} if the country has no encryption key
   */
  public @Nullable EncryptionKey getEncryptionKey() {
    return this.encryptionKey;
  }

  /**
   * Assigns the encryption key settings.
   *
   * @param encryptionKey the encryption key settings
   */
  public void setEncryptionKey(final @Nullable EncryptionKey encryptionKey) {
    this.encryptionKey = encryptionKey;
  }

  /**
   * Gets the supported attributes.
   *
   * @return the attributes
   */
  public @NonNull List<MetadataAttribute> getAttributes() {
    return this.attributes;
  }

  /**
   * Assigns the supported attributes.
   *
   * @param attributes the attributes
   */
  public void setAttributes(final @NonNull List<MetadataAttribute> attributes) {
    this.attributes = attributes;
  }

  /**
   * Gets the fixed users.
   *
   * @return the users
   */
  public @NonNull List<SimulatedUser> getUsers() {
    return this.users;
  }

  /**
   * Assigns the fixed users.
   *
   * @param users the users
   */
  public void setUsers(final @NonNull List<SimulatedUser> users) {
    this.users = users;
  }

  /**
   * A signing method, {@code alg:SigningMethod}.
   */
  public static class SigningMethod {

    /**
     * The algorithm URI.
     */
    private @Nullable String algorithm;

    /**
     * The minimum key size, if given.
     */
    private @Nullable Integer minKeySize;

    /**
     * The maximum key size, if given.
     */
    private @Nullable Integer maxKeySize;

    /**
     * Gets the algorithm URI.
     *
     * @return the algorithm URI
     */
    public @Nullable String getAlgorithm() {
      return this.algorithm;
    }

    /**
     * Assigns the algorithm URI.
     *
     * @param algorithm the algorithm URI
     */
    public void setAlgorithm(final @Nullable String algorithm) {
      this.algorithm = algorithm;
    }

    /**
     * Gets the minimum key size.
     *
     * @return the minimum key size, or {@code null}
     */
    public @Nullable Integer getMinKeySize() {
      return this.minKeySize;
    }

    /**
     * Assigns the minimum key size.
     *
     * @param minKeySize the minimum key size
     */
    public void setMinKeySize(final @Nullable Integer minKeySize) {
      this.minKeySize = minKeySize;
    }

    /**
     * Gets the maximum key size.
     *
     * @return the maximum key size, or {@code null}
     */
    public @Nullable Integer getMaxKeySize() {
      return this.maxKeySize;
    }

    /**
     * Assigns the maximum key size.
     *
     * @param maxKeySize the maximum key size
     */
    public void setMaxKeySize(final @Nullable Integer maxKeySize) {
      this.maxKeySize = maxKeySize;
    }

  }

  /**
   * The encryption key of a country.
   */
  public static class EncryptionKey {

    /**
     * The algorithms of the {@code md:EncryptionMethod} elements of the key.
     */
    private @NonNull List<String> encryptionMethods = new ArrayList<>();

    /**
     * Gets the algorithms of the {@code md:EncryptionMethod} elements.
     *
     * @return the algorithm URIs
     */
    public @NonNull List<String> getEncryptionMethods() {
      return this.encryptionMethods;
    }

    /**
     * Assigns the algorithms of the {@code md:EncryptionMethod} elements.
     *
     * @param encryptionMethods the algorithm URIs
     */
    public void setEncryptionMethods(final @NonNull List<String> encryptionMethods) {
      this.encryptionMethods = encryptionMethods;
    }

  }

  /**
   * A supported attribute, as published in the metadata.
   */
  public static class MetadataAttribute {

    /**
     * The attribute name.
     */
    private @Nullable String name;

    /**
     * The friendly name. Not published if not set.
     */
    private @Nullable String friendlyName;

    /**
     * The name format. The default is {@code urn:oasis:names:tc:SAML:2.0:attrname-format:uri}. An empty value means
     * that no name format is published.
     */
    private @Nullable String nameFormat;

    /**
     * Gets the attribute name.
     *
     * @return the attribute name
     */
    public @Nullable String getName() {
      return this.name;
    }

    /**
     * Assigns the attribute name.
     *
     * @param name the attribute name
     */
    public void setName(final @Nullable String name) {
      this.name = name;
    }

    /**
     * Gets the friendly name.
     *
     * @return the friendly name
     */
    public @Nullable String getFriendlyName() {
      return this.friendlyName;
    }

    /**
     * Assigns the friendly name.
     *
     * @param friendlyName the friendly name
     */
    public void setFriendlyName(final @Nullable String friendlyName) {
      this.friendlyName = friendlyName;
    }

    /**
     * Gets the name format.
     *
     * @return the name format, an empty string for none, or {@code null} for the default
     */
    public @Nullable String getNameFormat() {
      return this.nameFormat;
    }

    /**
     * Assigns the name format.
     *
     * @param nameFormat the name format, an empty string for none, or {@code null} for the default
     */
    public void setNameFormat(final @Nullable String nameFormat) {
      this.nameFormat = nameFormat;
    }

  }

}
