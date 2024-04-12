/*
 * Copyright 2023-2024 Sweden Connect
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

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.opensaml.saml.ext.saml2alg.DigestMethod;
import org.opensaml.saml.saml2.metadata.EncryptionMethod;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.UsageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.Setter;
import se.swedenconnect.opensaml.saml2.metadata.build.DigestMethodBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.EncryptionMethodBuilder;
import se.swedenconnect.opensaml.saml2.metadata.build.KeyDescriptorBuilder;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.spring.saml.idp.autoconfigure.settings.MetadataConfigurationProperties;

/**
 * A helper bean that gives us the connector credentials.
 *
 * @author Martin Lindström
 */
@Component
public class ConnectorCredentials {

  @Setter
  @Autowired(required = false)
  @Qualifier("saml.idp.credentials.Default")
  private PkiCredential defaultCredential;

  @Setter
  @Autowired(required = false)
  @Qualifier("saml.idp.credentials.Sign")
  private PkiCredential signCredential;

  @Setter
  @Autowired(required = false)
  @Qualifier("saml.idp.credentials.FutureSign")
  private X509Certificate futureSignCertificate;

  @Setter
  @Autowired(required = false)
  @Qualifier("saml.idp.credentials.Encrypt")
  private PkiCredential encryptCredential;

  @Setter
  @Autowired(required = false)
  @Qualifier("saml.idp.credentials.PreviousEncrypt")
  private PkiCredential previousEncryptCredential;

  @Setter
  @Autowired(required = false)
  @Qualifier("saml.idp.credentials.MetadataSign")
  private PkiCredential metadataSignCredential;

  @Setter
  @Autowired(required = false)
  @Qualifier("connector.sp.credentials.Default")
  private PkiCredential spDefaultCredential;

  @Setter
  @Autowired(required = false)
  @Qualifier("connector.sp.credentials.Sign")
  private PkiCredential spSignCredential;

  @Setter
  @Autowired(required = false)
  @Qualifier("connector.sp.credentials.FutureSign")
  private X509Certificate spFutureSignCertificate;

  @Setter
  @Autowired(required = false)
  @Qualifier("connector.sp.credentials.Encrypt")
  private PkiCredential spEncryptCredential;

  @Setter
  @Autowired(required = false)
  @Qualifier("connector.sp.credentials.PreviousEncrypt")
  private PkiCredential spPreviousEncryptCredential;

  @Setter
  @Autowired(required = false)
  @Qualifier("connector.sp.credentials.MetadataSign")
  private PkiCredential spMetadataSignCredential;

  @Setter
  @Autowired(required = false)
  @Qualifier("connector.idm.oauth2.Credential")
  private PkiCredential oauth2Credential;

  /**
   * Gets the SP signing credential to use.
   *
   * @return the {@link PkiCredential}
   * @throws IllegalArgumentException if no credential is found
   */
  public PkiCredential getSpSigningCredential() {
    final PkiCredential[] creds = { this.spSignCredential, this.spDefaultCredential, this.signCredential, this.defaultCredential };
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
    List<PkiCredential> creds = new ArrayList<>();
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
      final List<MetadataConfigurationProperties.EncryptionMethod> encryptionMethods) {

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
        KeyDescriptorBuilder.builder()
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
        KeyDescriptorBuilder.builder()
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
      final List<MetadataConfigurationProperties.EncryptionMethod> encryptionMethods) {

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
   * @return the {@link PkiCredential}
   * @throws IllegalArgumentException if no credential is found
   */
  public PkiCredential getSpMetadataSigningCredential() {
    final PkiCredential[] creds = { this.spMetadataSignCredential, this.spDefaultCredential,
        this.metadataSignCredential, this.defaultCredential, this.spSignCredential, this.signCredential };
    for (final PkiCredential c : creds) {
      if (c != null) {
        return c;
      }
    }
    throw new IllegalArgumentException("No metadata signing credential is available");
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
    final PkiCredential[] creds = { this.oauth2Credential, this.defaultCredential, this.spDefaultCredential, this.signCredential, this.spSignCredential };
    for (final PkiCredential c : creds) {
      if (c != null) {
        return c;
      }
    }
    throw new IllegalArgumentException("No OAuth2 credential is available");
  }

}
