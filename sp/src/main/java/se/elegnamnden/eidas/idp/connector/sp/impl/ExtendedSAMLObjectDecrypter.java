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
package se.elegnamnden.eidas.idp.connector.sp.impl;

import java.util.List;

import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.DecryptionConfiguration;
import org.opensaml.xmlsec.DecryptionParameters;

import se.litsec.opensaml.xmlsec.SAMLObjectDecrypter;
import se.swedenconnect.opensaml.xmlsec.config.SecurityConfiguration;
import se.swedenconnect.opensaml.xmlsec.encryption.support.DecryptionUtils;

/**
 * An extension of the {@link SAMLObjectDecrypter} that allows us to give both decryption parameters and a security
 * configuration.
 * 
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public class ExtendedSAMLObjectDecrypter extends SAMLObjectDecrypter {

  /**
   * Constructor.
   * 
   * @param decryptionCredentials
   *          decryption credentials
   * @param securityConfiguration
   *          the security configuration
   */
  public ExtendedSAMLObjectDecrypter(List<Credential> decryptionCredentials, SecurityConfiguration securityConfiguration) {
    super(createDecryptionParameters(decryptionCredentials, securityConfiguration.getDecryptionConfiguration()));
  }

  private static DecryptionParameters createDecryptionParameters(List<Credential> decryptionCredentials,
      DecryptionConfiguration decryptionConfiguration) {
    DecryptionParameters parameters = new DecryptionParameters();
    parameters.setDataKeyInfoCredentialResolver(decryptionConfiguration.getDataKeyInfoCredentialResolver());
    parameters.setKEKKeyInfoCredentialResolver(DecryptionUtils.createKeyInfoCredentialResolver(decryptionCredentials.toArray(
      new Credential[decryptionCredentials.size()])));
    parameters.setEncryptedKeyResolver(decryptionConfiguration.getEncryptedKeyResolver());
    parameters.setBlacklistedAlgorithms(decryptionConfiguration.getBlacklistedAlgorithms());
    parameters.setWhitelistedAlgorithms(decryptionConfiguration.getWhitelistedAlgorithms());
    return parameters;
  }

  public ExtendedSAMLObjectDecrypter(Credential decryptionCredential) {
    super(decryptionCredential);
  }

  public ExtendedSAMLObjectDecrypter(List<Credential> decryptionCredentials) {
    super(decryptionCredentials);
  }

  public ExtendedSAMLObjectDecrypter(DecryptionParameters decryptionParameters) {
    super(decryptionParameters);
  }

  public ExtendedSAMLObjectDecrypter(DecryptionConfiguration decryptionConfiguration) {
    super(decryptionConfiguration);
  }

}
