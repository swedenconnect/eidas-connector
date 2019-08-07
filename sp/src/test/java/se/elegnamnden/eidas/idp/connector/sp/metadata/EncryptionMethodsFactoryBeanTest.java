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
package se.elegnamnden.eidas.idp.connector.sp.metadata;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.opensaml.saml.saml2.metadata.EncryptionMethod;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.xmlsec.encryption.support.EncryptionConstants;
import org.springframework.core.io.ClassPathResource;

import se.elegnamnden.eidas.idp.connector.sp.TestBase;
import se.litsec.eidas.opensaml.xmlsec.EidasSecurityConfiguration;
import se.swedenconnect.opensaml.xmlsec.config.SecurityConfiguration;
import se.swedenconnect.opensaml.xmlsec.encryption.support.EcEncryptionConstants;

/**
 * Test cases for {@code EncryptionMethodsFactoryBean}.
 * 
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public class EncryptionMethodsFactoryBeanTest extends TestBase {

  @Test
  public void testRSA() throws Exception {
    X509Credential rsaCredential = TestBase.loadKeyStoreCredential(
      new ClassPathResource("rsakey.jks").getInputStream(), "Test1234", "key1", "Test1234");

    SecurityConfiguration config = new EidasSecurityConfiguration();

    EncryptionMethodsFactoryBean bean = new EncryptionMethodsFactoryBean(config, rsaCredential);
    bean.afterPropertiesSet();

    List<EncryptionMethod> encryptionMethods = bean.getObject();

    validate(encryptionMethods, EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256_GCM,
      EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES192_GCM,
      EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128_GCM,
      EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP,
      EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP11);
  }

  @Test
  public void testEC() throws Exception {
    X509Credential ecCredential = TestBase.loadKeyStoreCredential(
      new ClassPathResource("eckey.jks").getInputStream(), "Test1234", "key1", "Test1234");

    SecurityConfiguration config = new EidasSecurityConfiguration();

    EncryptionMethodsFactoryBean bean = new EncryptionMethodsFactoryBean(config, ecCredential);
    bean.afterPropertiesSet();

    List<EncryptionMethod> encryptionMethods = bean.getObject();

    validate(encryptionMethods, EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256_GCM,
      EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES192_GCM,
      EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128_GCM,
      EcEncryptionConstants.ALGO_ID_KEYAGREEMENT_ECDH_ES,
      EncryptionConstants.ALGO_ID_KEYWRAP_AES256,
      EncryptionConstants.ALGO_ID_KEYWRAP_AES128);
  }

  private void validate(List<EncryptionMethod> encryptionMethods, String... expectedAlgorithms) {
    for (String alg : expectedAlgorithms) {
      if (!encryptionMethods.stream().map(EncryptionMethod::getAlgorithm).filter(a -> a.equals(alg)).findFirst().isPresent()) {
        Assert.fail("Expected algorithm '" + alg + "' but it was not found in list of encryption methods");
      }
    }
    for (EncryptionMethod em : encryptionMethods) {
      if (!Arrays.stream(expectedAlgorithms).filter(a -> a.equals(em.getAlgorithm())).findFirst().isPresent()) {
        Assert.fail("Algorithm '" + em.getAlgorithm() + "' was found among encryption methods but listed as expected");
      }
    }
  }

}
