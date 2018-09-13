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

import org.opensaml.security.x509.BasicX509Credential;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import lombok.Data;

/**
 * Configuration class for creating a {@link BasicX509Credential} object.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
@Data
public class CredentialsConfiguration implements InitializingBean {

  /** The entityID for the credential. */
  private String entityId;

  /** The certificate. */
  private Resource certificateResource;

  /** The private key resource (for soft private keys). */
  private Resource privateKeyResource;

  /** The alias (when using the keystore concept, or PKCS#11). */
  private String alias;

  /** Path to PKCS#11 config file. */
  private String pkcs11Config;

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.hasText(this.entityId, "The property 'entityId' must be assigned");
    Assert.notNull(this.certificateResource, "The property 'certificateResource' must be assigned");
  }

  /**
   * Checks that all required properties have been assigned for soft key usage.
   * 
   * @throws Exception
   *           for errors
   */
  public void assertSoftUsage() throws Exception {
    Assert.notNull(this.getPrivateKeyResource(), "The property 'privateKeyResource' must be assigned");
  }

  /**
   * Checks that all required properties have been assigned for HSM usage.
   * 
   * @throws Exception
   *           for errors
   */
  public void assertPKCS11Usage() throws Exception {
    Assert.hasText(this.alias, "The property 'alias' must be assigned");
    Assert.hasText(this.pkcs11Config, "The property 'pkcs11Config' must be assigned");
  }

}
