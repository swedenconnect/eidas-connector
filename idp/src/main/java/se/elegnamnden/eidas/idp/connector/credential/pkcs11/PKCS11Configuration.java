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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import lombok.Getter;
import lombok.Setter;

/**
 * A singleton for PKCS#11 configuration.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class PKCS11Configuration implements InitializingBean {
  
  /** Is PKCS#11 usage enabled? */
  private boolean pkcs11enabled;

  /** PIN for the PKCS#11 token that is being used. */
  @Setter
  @Getter
  private String pkcs11pin;
  
  /**
   * Constructor.
   * 
   * @param pkcs11enabled is PKCS#11 usage enabled?
   */
  public PKCS11Configuration(boolean pkcs11enabled) {
    this.pkcs11enabled = pkcs11enabled;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    if (this.pkcs11enabled) {
      Assert.hasText(this.pkcs11pin, "Property 'pkcs11pin' must be assigned");
    }
  }

}
