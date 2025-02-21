/*
 * Copyright 2017-2025 Sweden Connect
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

import jakarta.annotation.Nullable;
import org.opensaml.saml.ext.saml2alg.DigestMethod;
import org.opensaml.saml.saml2.metadata.EncryptionMethod;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.UsageType;
import org.springframework.util.StringUtils;
import se.swedenconnect.opensaml.saml2.metadata.build.DigestMethodBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.EncryptionMethodBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.KeyDescriptorBuilder;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.ReloadablePkiCredential;
import se.swedenconnect.spring.saml.idp.autoconfigure.settings.IdentityProviderConfigurationProperties;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A helper bean that gives us the connector credentials.
 *
 * @author Martin Lindström
 */
public class ConnectorCredentials {

  private final PkiCredential defaultCredential;

  private final PkiCredential signCredential;

  private final X509Certificate futureSignCertificate;

  private final PkiCredential encryptCredential;

  private final PkiCredential previousEncryptCredential;

  private final PkiCredential metadataSignCredential;

  private final PkiCredential spDefaultCredential;

  private final PkiCredential spSignCredential;

  private final X509Certificate spFutureSignCertificate;

  private final PkiCredential spEncryptCredential;

  private final PkiCredential spPreviousEncryptCredential;

  private final PkiCredential spMetadataSignCredential;

  private final PkiCredential oauth2Credential;

  /**
   * Initializes the ConnectorCredentials object with the provided credentials for various purposes.
   *
   * @param defaultCredential the default credential for the SAML IdP
   * @param signCredential the signing credential for the SAML IdP
   * @param futureSignCertificate the future signing certificate for the SAML IdP
   * @param encryptCredential the encryption credential for the SAML IdP
   * @param previousEncryptCredential the previous encryption credential for the SAML IdP
   * @param spDefaultCredential the default credential for the connector SP
   * @param spSignCredential the signing credential for the connector SP
   * @param spFutureSignCertificate the future signing certificate for the connector SP
   * @param spEncryptCredential the encryption credential for the connector SP
   * @param spPreviousEncryptCredential the previous encryption credential for the connector SP
   * @param metadataSignCredential the metadata signing credential for the SAML IdP
   * @param spMetadataSignCredential the metadata signing credential for the connector SP
   * @param oauth2Credential the PKI credential for OAuth2 use
   */
  public ConnectorCredentials(
      @Nullable final PkiCredential defaultCredential,
      @Nullable final PkiCredential signCredential,
      @Nullable final X509Certificate futureSignCertificate,
      @Nullable final PkiCredential encryptCredential,
      @Nullable final PkiCredential previousEncryptCredential,
      @Nullable final PkiCredential spDefaultCredential,
      @Nullable final PkiCredential spSignCredential,
      @Nullable final X509Certificate spFutureSignCertificate,
      @Nullable final PkiCredential spEncryptCredential,
      @Nullable final PkiCredential spPreviousEncryptCredential,
      @Nullable final PkiCredential metadataSignCredential,
      @Nullable final PkiCredential spMetadataSignCredential,
      @Nullable final PkiCredential oauth2Credential) {
    this.defaultCredential = defaultCredential;
    this.signCredential = signCredential;
    this.futureSignCertificate = futureSignCertificate;
    this.encryptCredential = encryptCredential;
    this.previousEncryptCredential = previousEncryptCredential;
    this.spDefaultCredential = spDefaultCredential;
    this.spSignCredential = spSignCredential;
    this.spFutureSignCertificate = spFutureSignCertificate;
    this.spEncryptCredential = spEncryptCredential;
    this.metadataSignCredential = metadataSignCredential;
    this.spMetadataSignCredential = spMetadataSignCredential;
    this.spPreviousEncryptCredential = spPreviousEncryptCredential;
    this.oauth2Credential = oauth2Credential;
  }

  /**
   * Gets the SP signing credential to use.
   *
   * @return the {@link PkiCredential}
   * @throws IllegalArgumentException if no credential is found
   */
  public PkiCredential getSpSigningCredential() {
    final PkiCredential[] creds =
        { this.spSignCredential, this.spDefaultCredential, this.signCredential, this.defaultCredential };
    for (final PkiCredential c : creds) {
      if (c != null) {
        return c;
      }
    }
    throw new IllegalArgumentException("No signing credential is available");
  }

  /**
   * Gets the SP encrypt credential(s) to use.
   *
   * @return a list of {@link PkiCredential}s
   * @throws IllegalArgumentException if no credential is found
   */
  public List<PkiCredential> getSpEncryptCredentials() {
    final List<PkiCredential> creds = new ArrayList<>();
    if (this.spEncryptCredential != null) {
      creds.add(this.spEncryptCredential);
      if (this.spPreviousEncryptCredential != null) {
        creds.add(this.spPreviousEncryptCredential);
      }
    }
    else if (this.spDefaultCredential != null) {
      creds.add(this.spDefaultCredential);
      if (this.spPreviousEncryptCredential != null) {
        creds.add(this.spPreviousEncryptCredential);
      }
    }
    else if (this.encryptCredential != null) {
      creds.add(this.encryptCredential);
      if (this.spPreviousEncryptCredential != null) {
        creds.add(this.spPreviousEncryptCredential);
      }
      else if (this.previousEncryptCredential != null) {
        creds.add(this.previousEncryptCredential);
      }
    }
    else if (this.defaultCredential != null) {
      creds.add(this.defaultCredential);
      if (this.spPreviousEncryptCredential != null) {
        creds.add(this.spPreviousEncryptCredential);
      }
      else if (this.previousEncryptCredential != null) {
        creds.add(this.previousEncryptCredential);
      }
    }
    if (creds.isEmpty()) {
      throw new IllegalArgumentException("No encrypt credential is available");
    }
    return creds;
  }

  /**
   * Gets a list of {@link KeyDescriptor} elements that should be included in the SP metadata.
   *
   * @param encryptionMethods the {@code md:EncryptionMethod} elements to use for encryption.
   * @return a list of {@link KeyDescriptor} elements
   */
  public List<KeyDescriptor> getSpKeyDescriptors(
      final List<IdentityProviderConfigurationProperties.MetadataConfigurationProperties.EncryptionMethod> encryptionMethods) {

    final List<KeyDescriptor> keyDescriptors = new ArrayList<>();
    KeyDescriptorBuilder unspecifiedBuilder = null;
    boolean unspecifiedSign = false;
    boolean unspecifiedEncrypt = false;

    if (this.spSignCredential != null) {
      keyDescriptors.add(KeyDescriptorBuilder.builder()
          .use(UsageType.SIGNING)
          .certificate(this.spSignCredential.getCertificate())
          .build());
      if (this.spFutureSignCertificate != null) {
        keyDescriptors.add(KeyDescriptorBuilder.builder()
            .use(UsageType.SIGNING)
            .certificate(this.spFutureSignCertificate)
            .build());
      }
    }
    else if (this.spDefaultCredential != null) {
      unspecifiedSign = true;
      unspecifiedBuilder = KeyDescriptorBuilder.builder()
          .certificate(this.spDefaultCredential.getCertificate());
    }
    else if (this.signCredential != null) {
      keyDescriptors.add(KeyDescriptorBuilder.builder()
          .use(UsageType.SIGNING)
          .certificate(this.signCredential.getCertificate())
          .build());
      if (this.futureSignCertificate != null) {
        keyDescriptors.add(KeyDescriptorBuilder.builder()
            .use(UsageType.SIGNING)
            .certificate(this.futureSignCertificate)
            .build());
      }
    }
    else if (this.defaultCredential != null) {
      unspecifiedSign = true;
      unspecifiedBuilder = KeyDescriptorBuilder.builder()
          .certificate(this.defaultCredential.getCertificate());
    }

    if (this.spEncryptCredential != null) {
      keyDescriptors.add(KeyDescriptorBuilder.builder()
          .use(UsageType.ENCRYPTION)
          .certificate(this.spEncryptCredential.getCertificate())
          .encryptionMethodsExt(this.createEncryptionMethods(encryptionMethods))
          .build());
    }
    else if (this.spDefaultCredential != null) {
      unspecifiedEncrypt = true;
      if (unspecifiedBuilder == null) {
        unspecifiedBuilder = KeyDescriptorBuilder.builder()
            .certificate(this.spDefaultCredential.getCertificate());
      }
      unspecifiedBuilder
          .encryptionMethodsExt(this.createEncryptionMethods(encryptionMethods));
    }
    else if (this.encryptCredential != null) {
      keyDescriptors.add(KeyDescriptorBuilder.builder()
          .use(UsageType.ENCRYPTION)
          .certificate(this.encryptCredential.getCertificate())
          .encryptionMethodsExt(this.createEncryptionMethods(encryptionMethods))
          .build());
    }
    else if (this.defaultCredential != null) {
      unspecifiedEncrypt = true;
      if (unspecifiedBuilder == null) {
        unspecifiedBuilder = KeyDescriptorBuilder.builder()
            .certificate(this.defaultCredential.getCertificate());
      }
      unspecifiedBuilder
          .encryptionMethodsExt(this.createEncryptionMethods(encryptionMethods));
    }

    if (unspecifiedBuilder != null) {
      if (unspecifiedSign && !unspecifiedEncrypt) {
        unspecifiedBuilder.use(UsageType.SIGNING);
      }
      else if (!unspecifiedSign && unspecifiedEncrypt) {
        unspecifiedBuilder.use(UsageType.ENCRYPTION);
      }
      else {
        unspecifiedBuilder.use(UsageType.UNSPECIFIED);
      }
      keyDescriptors.add(unspecifiedBuilder.build());
    }

    return keyDescriptors;
  }

  private List<EncryptionMethod> createEncryptionMethods(
      final List<IdentityProviderConfigurationProperties.MetadataConfigurationProperties.EncryptionMethod> encryptionMethods) {

    if (encryptionMethods != null && !encryptionMethods.isEmpty()) {
      return encryptionMethods.stream()
          .filter(e -> StringUtils.hasText(e.getAlgorithm()))
          .map(e -> {
            final EncryptionMethodBuilder builder = EncryptionMethodBuilder.builder()
                .algorithm(e.getAlgorithm());

            if (e.getKeySize() != null) {
              builder.keySize(e.getKeySize());
            }
            if (e.getOaepParams() != null) {
              builder.oAEPparams(e.getOaepParams());
            }
            final EncryptionMethod em = builder.build();

            if (StringUtils.hasText(e.getDigestMethod())) {
              final DigestMethod dm = DigestMethodBuilder.digestMethod(e.getDigestMethod());
              em.getUnknownXMLObjects().add(dm);
            }
            return em;
          })
          .toList();
    }
    else {
      return null;
    }
  }

  /**
   * Gets the metadata signing credential.
   *
   * @return the {@link PkiCredential}, or {@code null} if none is configured
   */
  @Nullable
  public PkiCredential getSpMetadataSigningCredential() {
    final PkiCredential[] creds = { this.spMetadataSignCredential, this.spDefaultCredential,
        this.metadataSignCredential, this.defaultCredential };
    for (final PkiCredential c : creds) {
      if (c != null) {
        return c;
      }
    }
    return null;
  }

  /**
   * Gets the future SP signing certificate.
   *
   * @return the certificate or {@code null}
   */
  public X509Certificate getSpFutureSigningCertificate() {
    if (this.spSignCredential != null || this.spDefaultCredential != null) {
      return this.spFutureSignCertificate;
    }
    else if (this.signCredential != null || this.defaultCredential != null) {
      return this.futureSignCertificate;
    }
    else {
      return null;
    }
  }

  /**
   * Gets the IdP signing credential to use.
   *
   * @return the {@link PkiCredential}
   * @throws IllegalArgumentException if no credential is found
   */
  public PkiCredential getIdpSigningCredential() {
    final PkiCredential[] creds = { this.signCredential, this.defaultCredential };
    for (final PkiCredential c : creds) {
      if (c != null) {
        return c;
      }
    }
    throw new IllegalArgumentException("No signing credential is available");
  }

  /**
   * Gets the OAuth2 credential to use.
   *
   * @return the {@link PkiCredential}
   * @throws IllegalArgumentException if no credential is found
   */
  public PkiCredential getOAuth2Credential() {
    final PkiCredential[] creds = { this.oauth2Credential, this.defaultCredential, this.spDefaultCredential,
        this.signCredential, this.spSignCredential };
    for (final PkiCredential c : creds) {
      if (c != null) {
        return c;
      }
    }
    throw new IllegalArgumentException("No OAuth2 credential is available");
  }

  /**
   * Gets a list of "hardware based" credentials. Those types of credentials may need to be monitored, and possibly
   * reloaded.
   *
   * @return a (possibly empty) list of {@link ReloadablePkiCredential}
   */
  public List<ReloadablePkiCredential> getHardwareCredentials() {

    final PkiCredential[] creds = { this.signCredential, this.spSignCredential, this.encryptCredential,
        this.spEncryptCredential, this.defaultCredential, this.spDefaultCredential, this.metadataSignCredential,
        this.spMetadataSignCredential, this.previousEncryptCredential, this.spPreviousEncryptCredential,
        this.oauth2Credential };

    return Arrays.stream(creds)
        .filter(Objects::nonNull)
        .filter(PkiCredential::isHardwareCredential)
        .filter(ReloadablePkiCredential.class::isInstance)
        .map(ReloadablePkiCredential.class::cast)
        .toList();
  }

}
