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
package se.swedenconnect.eidas.simulator;

import org.junit.jupiter.api.BeforeAll;
import se.swedenconnect.opensaml.OpenSAMLInitializer;
import se.swedenconnect.opensaml.OpenSAMLSecurityDefaultsConfig;
import se.swedenconnect.opensaml.OpenSAMLSecurityExtensionConfig;
import se.swedenconnect.opensaml.eidas.xmlsec.Eidas_1_4_SecurityConfiguration;

/**
 * Base class for tests that need OpenSAML.
 *
 * @author Martin Lindström
 */
public abstract class OpenSamlTestBase {

  /**
   * Initializes OpenSAML as the simulator does.
   *
   * @throws Exception for initialization errors
   */
  @BeforeAll
  static void initializeOpenSaml() throws Exception {
    if (!OpenSAMLInitializer.getInstance().isInitialized()) {
      OpenSAMLInitializer.getInstance().initialize(
          new OpenSAMLSecurityDefaultsConfig(new Eidas_1_4_SecurityConfiguration()),
          new OpenSAMLSecurityExtensionConfig());
    }
  }

}
