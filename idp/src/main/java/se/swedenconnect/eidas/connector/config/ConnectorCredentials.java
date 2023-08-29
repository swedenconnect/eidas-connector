/*
 * Copyright 2023 Sweden Connect
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import lombok.Setter;
import se.swedenconnect.security.credential.PkiCredential;

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

  /**
   * Gets the SP signing credential to use.
   * 
   * @return the {@link PkiCredential}
   * @throws IllegalArgumentException if no credential is found
   */
  public PkiCredential getSpSigningCredential() {
    return List.of(this.spSignCredential, this.spDefaultCredential, this.signCredential, this.defaultCredential)
        .stream()
        .filter(c -> c != null)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No signing credential is available"));
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
   * Gets the metadata signing credential.
   * 
   * @return the {@link PkiCredential}
   * @throws IllegalArgumentException if no credential is found
   */
  public PkiCredential getSpMetadataSigningCredential() {
    return List.of(this.spMetadataSignCredential, this.spDefaultCredential,
        this.metadataSignCredential, this.defaultCredential, this.spSignCredential, this.signCredential)
        .stream()
        .filter(c -> c != null)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No metadata signing credential is available"));
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

}
