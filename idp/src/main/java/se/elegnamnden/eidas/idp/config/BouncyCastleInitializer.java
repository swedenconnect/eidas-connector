/*
 * Copyright 2017-2019 Sweden Connect
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
package se.elegnamnden.eidas.idp.config;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import lombok.extern.slf4j.Slf4j;

/**
 * Simple bean for initializing the Bouncy Castle provider.
 * 
 * @author Martin Lindström (martin@idsec.se)
 */
@Slf4j
public class BouncyCastleInitializer {

  /**
   * Constructor.
   */
  public BouncyCastleInitializer() {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      log.info("Crypto provider '{}' is not installed, installing it ...", BouncyCastleProvider.PROVIDER_NAME);
      Security.addProvider(new BouncyCastleProvider());
      log.info("{}: Crypto provider '{}' was installed", BouncyCastleProvider.PROVIDER_NAME);
    }
    else {
      log.debug("Crypto provider '{}' is already installed", BouncyCastleProvider.PROVIDER_NAME);
    }
  }

}
