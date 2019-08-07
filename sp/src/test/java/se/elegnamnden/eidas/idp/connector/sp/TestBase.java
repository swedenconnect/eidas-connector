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
package se.elegnamnden.eidas.idp.connector.sp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.junit.BeforeClass;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.security.x509.impl.KeyStoreX509CredentialAdapter;
import org.w3c.dom.Element;

import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import se.litsec.eidas.opensaml.xmlsec.RelaxedEidasSecurityConfiguration;
import se.swedenconnect.opensaml.OpenSAMLInitializer;
import se.swedenconnect.opensaml.OpenSAMLSecurityDefaultsConfig;
import se.swedenconnect.opensaml.OpenSAMLSecurityExtensionConfig;

/**
 * Abstract base class that initializes OpenSAML for test classes.
 * 
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public abstract class TestBase {

  /**
   * Initializes the OpenSAML library.
   * 
   * @throws Exception
   *           for init errors
   */
  @BeforeClass
  public static void initializeOpenSAML() throws Exception {
    OpenSAMLInitializer.getInstance()
    .initialize(
      new OpenSAMLSecurityDefaultsConfig(new RelaxedEidasSecurityConfiguration()),
      new OpenSAMLSecurityExtensionConfig());
  }
  
  /**
   * Loads a {@link KeyStore} based on the given arguments.
   * 
   * @param keyStorePath
   *          the path to the key store
   * @param keyStorePassword
   *          the key store password
   * @param keyStoreType
   *          the type of the keystore (if {@code null} the default keystore type will be assumed)
   * @return a {@code KeyStore} instance
   * @throws KeyStoreException
   *           for errors loading the keystore
   * @throws IOException
   *           for IO errors
   */
  public static KeyStore loadKeyStore(String keyStorePath, String keyStorePassword, String keyStoreType) throws KeyStoreException,
      IOException {
    return loadKeyStore(new FileInputStream(keyStorePath), keyStorePassword, keyStoreType);
  }

  public static KeyStore loadKeyStore(InputStream keyStoreStream, String keyStorePassword, String keyStoreType) throws KeyStoreException,
      IOException {
    try {
      KeyStore keyStore = keyStoreType != null ? KeyStore.getInstance(keyStoreType) : KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(keyStoreStream, keyStorePassword.toCharArray());
      return keyStore;
    }
    catch (NoSuchAlgorithmException | CertificateException e) {
      throw new KeyStoreException(e);
    }
  }

  public static X509Credential loadKeyStoreCredential(InputStream keyStoreStream, String keyStorePassword, String alias, String keyPassword)
      throws KeyStoreException, IOException {
    KeyStore keyStore = loadKeyStore(keyStoreStream, keyStorePassword, "jks");
    return new KeyStoreX509CredentialAdapter(keyStore, alias, keyPassword.toCharArray());
  }
  
  /**
   * Returns the given SAML object in its "pretty print" XML string form.
   * 
   * @param <T>
   *          the type of object to "print"
   * @param object
   *          the object to display as a string
   * @return the XML as a string
   * @throws MarshallingException
   *           for marshalling errors
   */
  public static <T extends XMLObject> String toString(T object) throws MarshallingException {
    Element elm = XMLObjectSupport.marshall(object);
    return SerializeSupport.prettyPrintXML(elm);
  }

}
