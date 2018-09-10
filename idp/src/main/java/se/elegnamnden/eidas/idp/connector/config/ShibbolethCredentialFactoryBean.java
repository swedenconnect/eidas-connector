/*
 * Copyright 2017-2018 E-legitimationsnämnden
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
package se.elegnamnden.eidas.idp.connector.config;

import java.security.cert.X509Certificate;
import java.util.List;

import org.opensaml.security.x509.BasicX509Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;

import net.shibboleth.idp.profile.spring.factory.BasicX509CredentialFactoryBean;
import se.elegnamnden.eidas.pkcs11.PKCS11CredentialFactoryBean;
import se.elegnamnden.eidas.pkcs11.SimplePKCS11CredentialFactoryBean;

/**
 * Utility factory bean that is used to simplify the Shibboleth Spring context files when setting up either soft keys or
 * HSM keys. The same factory can be used for both types.
 * <p>
 * See also {@link PKCS11CredentialFactoryBean}.
 * </p>
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class ShibbolethCredentialFactoryBean extends BasicX509CredentialFactoryBean {

  /** Logger instance. */
  private final Logger log = LoggerFactory.getLogger(ShibbolethCredentialFactoryBean.class);

  /** Flag telling whether PKCS#11 is enabled. */
  private boolean pkcs11Enabled = false;

  /** The name of the security provider holding the private key object. */
//  private List<String> providerNameList;

  /** The private key alias. */
  private String alias;

  /** The private key PIN. */
  private String pin;
  
  /** PKCS#11 configuration. */
  private String pkcs11cfg;

  /** {@inheritDoc} */
  @Override
  protected BasicX509Credential doCreateInstance() throws Exception {
    if (!pkcs11Enabled) {
      log.debug("PKCS#11 is not enabled - reverting to soft key implementation");
      return super.doCreateInstance();
    }
    else {
      SimplePKCS11CredentialFactoryBean p11Factory = new SimplePKCS11CredentialFactoryBean();
      
      final List<X509Certificate> certificates = this.getCertificates();
      if (null == certificates || certificates.isEmpty()) {
        log.error("No certificates provided");
        throw new BeanCreationException("No certificates provided");
      }
      X509Certificate entityCertificate = this.getEntityCertificate();
      if (null == entityCertificate) {
        entityCertificate = certificates.get(0);
      }
      p11Factory.setEntityCertificate(entityCertificate);
      
      p11Factory.setAlias(this.alias);
      p11Factory.setPin(this.pin);
      p11Factory.setPkcs11ConfigFile(this.pkcs11cfg);
      p11Factory.setEntityID(this.getEntityID());
      p11Factory.afterPropertiesSet();
      
      return p11Factory.getObject();
      
//      if (this.providerNameList == null || this.providerNameList.isEmpty()) {
//        throw new BeanCreationException("Property 'providerNameList' is empty");
//      }
//      if (!StringUtils.hasText(this.alias)) {
//        throw new BeanCreationException("Property 'alias' has not been assigned");
//      }
//      if (!StringUtils.hasText(this.pin)) {
//        throw new BeanCreationException("Property 'pin' has not been assigned");
//      }
//
//      final List<X509Certificate> certificates = this.getCertificates();
//      if (null == certificates || certificates.isEmpty()) {
//        log.error("No certificates provided");
//        throw new BeanCreationException("No certificates provided");
//      }
//      X509Certificate entityCertificate = this.getEntityCertificate();
//      if (null == entityCertificate) {
//        entityCertificate = certificates.get(0);
//      }
//
//      PKCS11Credential credential = new PKCS11Credential(entityCertificate, this.providerNameList, this.alias, this.pin);
//      final String entityID = this.getEntityID();
//      if (entityID != null) {
//        credential.setEntityId(entityID);
//      }
//      return credential;
    }
  }

  /**
   * Assigns whether PKCS#11 should be enabled or not.
   * 
   * @param pkcs11Enabled
   *          is PKCS#11 enabled?
   */
  public void setPkcs11Enabled(boolean pkcs11Enabled) {
    this.pkcs11Enabled = pkcs11Enabled;
  }

  /**
   * Assigns the list of provider names.
   * 
   * @param providerNameList
   *          provider names
   */
//  public void setProviderNameList(List<String> providerNameList) {
//    this.providerNameList = providerNameList;
//  }
  
  

  /**
   * Assigns the PKCS#11 alias.
   * 
   * @param alias
   *          the PKCS#11 alias
   */
  public void setAlias(String alias) {
    this.alias = alias;
  }

  public void setPkcs11cfg(String pkcs11cfg) {
    this.pkcs11cfg = pkcs11cfg;
  }

  /**
   * Assigns the PKCS#11 PIN.
   * 
   * @param pin
   *          the PKCS#11 PIN
   */
  public void setPin(String pin) {
    this.pin = pin;
  }

}
