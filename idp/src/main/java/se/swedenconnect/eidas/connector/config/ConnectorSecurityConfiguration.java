/*
 * Copyright 2017-2024 Sweden Connect
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

import org.opensaml.xmlsec.DecryptionConfiguration;
import org.opensaml.xmlsec.SignatureValidationConfiguration;
import se.swedenconnect.opensaml.eidas.xmlsec.Eidas_1_4_SecurityConfiguration;
import se.swedenconnect.opensaml.eidas.xmlsec.RelaxedEidasSecurityConfiguration;
import se.swedenconnect.opensaml.xmlsec.config.SecurityConfiguration;

/**
 * {@link SecurityConfiguration} for maximum eIDAS interoperability.
 *
 * @author Martin Lindström
 */
public class ConnectorSecurityConfiguration extends Eidas_1_4_SecurityConfiguration {

  /** Relaxed configuration for what we receive. */
  private final RelaxedConfiguration relaxedConfiguration = new RelaxedConfiguration();

  /** {@inheritDoc} */
  @Override
  public String getProfileName() {
    return "eidas-connector";
  }

  /** {@inheritDoc} */
  @Override
  protected DecryptionConfiguration createDefaultDecryptionConfiguration() {
    return this.relaxedConfiguration.createDecryptionConfiguration();
  }

  /** {@inheritDoc} */
  @Override
  protected SignatureValidationConfiguration createDefaultSignatureValidationConfiguration() {
    return this.relaxedConfiguration.createSignatureValidationConfiguration();
  }

  private static class RelaxedConfiguration extends RelaxedEidasSecurityConfiguration {

    public DecryptionConfiguration createDecryptionConfiguration() {
      return this.createDefaultDecryptionConfiguration();
    }

    public SignatureValidationConfiguration createSignatureValidationConfiguration() {
      return this.createDefaultSignatureValidationConfiguration();
    }

  }

}
