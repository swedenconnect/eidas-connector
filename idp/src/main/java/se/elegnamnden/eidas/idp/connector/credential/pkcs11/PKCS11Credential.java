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
package se.elegnamnden.eidas.idp.connector.credential.pkcs11;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.opensaml.security.x509.BasicX509Credential;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;
import net.shibboleth.ext.spring.factory.PKCS11PrivateKeyFactoryBean;

/**
 * A simple PKCS#11 implementation of a {@code Credential}.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
@Slf4j
public class PKCS11Credential extends BasicX509Credential {

  /** The PKCS#11 config file. */
  protected String pkcs11ConfigFile;

  /** The key alias. */
  protected String alias;

  /** The key password/PIN. */
  protected String pin;

  /**
   * Constructor setting up the credential by loading the PKCS#11 private key using the supplied parameters.
   * 
   * @param entityCertificate
   *          the certificate
   * @param pkcs11ConfigFile
   *          the PKCS#11 config file
   * @param alias
   *          the key alias
   * @param pin
   *          the key password/PIN
   * @throws Exception
   *           if loading of the private key fails
   */
  public PKCS11Credential(X509Certificate entityCertificate, String pkcs11ConfigFile, String alias, String pin) throws Exception {
    super(entityCertificate);

    Assert.hasText(pkcs11ConfigFile, "pkcs11ConfigFile must not be null or empty");
    this.pkcs11ConfigFile = pkcs11ConfigFile;
    Assert.hasText(alias, "alias must not be null or empty");
    this.alias = alias;
    Assert.hasText(pin, "pin must not be null or empty");
    this.pin = pin;

    this.loadPrivateKey();
  }

  /**
   * Loads the PKCS#11 private key.
   * 
   * @throws Exception
   *           for load errors
   */
  private void loadPrivateKey() throws Exception {
    log.debug("Loading PKCS#11 private key [{}]", this.toString());

    // Use Shibboleth's built in factory for loading the private key ...
    //
    PKCS11PrivateKeyFactoryBean factory = new PKCS11PrivateKeyFactoryBean();
    factory.setKeyAlias(this.alias);
    factory.setPkcs11Config(this.pkcs11ConfigFile);
    factory.setKeyPassword(this.pin);

    try {
      PrivateKey privateKey = factory.getObject();
      this.setPrivateKey(privateKey);
      log.debug("PKCS#11 private key loaded successfully [{}]", this.toString());
    }
    catch (Exception e) {
      log.error("Failed to load PKCS#11 private key [{}] - {}", this.toString(), e.getMessage(), e);
      throw e;
    }
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return String.format("entity-id='%s', cfg='%s', alias='%s', pin='%s'", 
      this.getEntityId(), this.pkcs11ConfigFile, this.alias, this.pin != null ? "*****" : "not-set");
  }

}
