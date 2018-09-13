/*
 * Copyright 2017-2018 Sweden Connect
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
package se.elegnamnden.eidas.idp.connector.credential;

import java.security.cert.X509Certificate;
import java.util.List;

import org.opensaml.security.x509.BasicX509Credential;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.util.Assert;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.shibboleth.idp.profile.spring.factory.BasicX509CredentialFactoryBean;
import se.elegnamnden.eidas.idp.connector.credential.pkcs11.PKCS11Configuration;
import se.elegnamnden.eidas.idp.connector.credential.pkcs11.PKCS11Credential;

/**
 * Factory bean for creating credentials for the connector.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
@Slf4j
public class ConnectorCredentialFactoryBean extends BasicX509CredentialFactoryBean {

  /** The credential configuration. */
  @Setter
  private CredentialsConfiguration credentialsConfig;

  /** Is PKCS#11 usage enabled? */
  @Setter
  private boolean pkcs11enabled;

  /** PKCS#11 configuration. */
  @Setter
  private PKCS11Configuration pkcs11Configuration;

  /** {@inheritDoc} */
  @Override
  protected BasicX509Credential doCreateInstance() throws Exception {
    if (this.pkcs11enabled) {
      PKCS11Credential pkcs11Credential = new PKCS11Credential(
        this.getCertificate(), this.credentialsConfig.getPkcs11Config(), this.credentialsConfig.getAlias(), this.pkcs11Configuration
          .getPkcs11pin());
      pkcs11Credential.setEntityId(this.credentialsConfig.getEntityId());

      return pkcs11Credential;
    }
    else {
      log.debug("PKCS#11 is not enabled - reverting to soft key implementation");
      return super.doCreateInstance();
    }
  }

  /**
   * Returns the entity certificate.
   * 
   * @return the entity certificate
   */
  private X509Certificate getCertificate() {
    final List<X509Certificate> certificates = this.getCertificates();
    if (null == certificates || certificates.isEmpty()) {
      log.error("{}: No Certificates provided", getConfigDescription());
      throw new BeanCreationException("No certificates provided");
    }

    X509Certificate entityCertificate = this.getEntityCertificate();
    if (null == entityCertificate) {
      entityCertificate = certificates.get(0);
    }
    return entityCertificate;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(this.credentialsConfig, "The property 'credentialsConfig' must be assigned");
    if (this.pkcs11enabled) {
      Assert.notNull(this.pkcs11Configuration, "The property 'pkcs11Configuration' must be assigned");
      this.credentialsConfig.assertPKCS11Usage();
    }
    else {
      this.credentialsConfig.assertSoftUsage();
    }

    this.setEntityId(this.credentialsConfig.getEntityId());
    this.setCertificateResource(this.credentialsConfig.getCertificateResource());

    if (!this.pkcs11enabled) {
      this.setPrivateKeyResource(this.credentialsConfig.getPrivateKeyResource());
    }
    super.afterPropertiesSet();
  }

}
