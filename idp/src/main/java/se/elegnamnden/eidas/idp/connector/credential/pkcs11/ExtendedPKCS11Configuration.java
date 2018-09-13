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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import se.elegnamnden.eidas.idp.connector.credential.CredentialsConfiguration;

/**
 * A singleton for PKCS#11 configuration.
 * <p>
 * Note: this class is only required for initializing Soft HSM (which is used for testing).
 * </p>
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
@Slf4j
public class ExtendedPKCS11Configuration extends PKCS11Configuration {

  /** Default PKCS#11 library for SoftHSM. */
  public static final String DEFAULT_SOFTHSM_PKCS11_LIB = "/usr/lib/softhsm/libsofthsm2.so";

  /** Default PKCS#11 label. */
  public static final String DEFAULT_PKCS11_LABEL = "connector";

  /** Default PKCS#11 slot to use. */
  public static final int DEFAULT_SLOT = 0;

  /** Is SoftHSM enabled? */
  private boolean softHsmEnabled;

  /** Should the IdP metadata be signed with a HSM key? */
  private boolean idpMetadataSigningEnabled;

  /** Should the SP metadata be signed with a HSM key? */
  private boolean spMetadataSigningEnabled;

  /** The library to use for SoftHSM. */
  @Setter
  private String softHsmLib;

  /** The label to use when adding entries to the PKCS#11 token. */
  @Setter
  private String pkcs11label;

  /** The slot to use. */
  @Setter
  private int pkcs11slot = DEFAULT_SLOT;
  
  /** IdP signing. */
  @Setter
  private CredentialsConfiguration idpSigningCredential;
  
  /** IdP encryption. */
  @Setter
  private CredentialsConfiguration idpEncryptionCredential;
  
  /** SP signing. */
  @Setter
  private CredentialsConfiguration spSigningCredential;
  
  /** SP encryption. */
  @Setter
  private CredentialsConfiguration spEncryptionCredential;
  
  /** IdP metadata signing. */
  @Setter
  private CredentialsConfiguration idpMetadataSigningCredential;
  
  /** SP metadata signing. */
  @Setter
  private CredentialsConfiguration spMetadataSigningCredential;

  /** Random number generator. */
  private static final Random rnd = new SecureRandom(String.valueOf(System.currentTimeMillis()).getBytes());

  /**
   * Constructor.
   * 
   * @param pkcs11enabled
   *          is PKCS#11 enabled?
   * @param softHsmEnabled
   *          is SoftHSM enabled?
   * @param idpMetadataSigningEnabled
   *          should the IdP metadata be signed with a HSM key?
   * @param spMetadataSigningEnabled
   *          should the SP metadata be signed with a HSM key?
   */
  public ExtendedPKCS11Configuration(boolean pkcs11enabled, boolean softHsmEnabled, boolean idpMetadataSigningEnabled,
      boolean spMetadataSigningEnabled) {
    super(pkcs11enabled);
    this.softHsmEnabled = pkcs11enabled && softHsmEnabled;
    this.idpMetadataSigningEnabled = idpMetadataSigningEnabled;
    this.spMetadataSigningEnabled = spMetadataSigningEnabled;
  }

  /**
   * Initializes the SoftHSM support.
   */
  private void initializeSoftHSM() throws Exception {

    // Initialize key slot
    //
    // pkcs11-tool --module /usr/lib/softhsm/libsofthsm2.so --init-token --slot <slot> --so-pin 0000 --init-pin --pin
    // 1234 --label <label>
    StringBuilder b = new StringBuilder();
    b.append("pkcs11-tool --module ")
      .append(this.softHsmLib)
      .append(" --init-token --slot ")
      .append(this.pkcs11slot)
      .append(" --so-pin ")
      .append(new BigInteger(24, rnd).toString())
      .append(" --init-pin --pin ")
      .append(this.getPkcs11pin())
      .append(" --label ")
      .append(this.pkcs11label);
    executeCommand(b.toString());
    log.info("Initialized PKCS11 SoftHSM key slot {} - label {}", this.pkcs11slot, this.pkcs11label);
    
    // Load keys and certificates
    //
    int index = 0;
    this.initKey(
      this.idpSigningCredential.getPrivateKeyResource().getFile().getAbsolutePath(),
      this.idpSigningCredential.getCertificateResource().getFile().getAbsolutePath(),
      this.idpSigningCredential.getAlias(),
      new BigInteger("aaaa", 16).add(new BigInteger(String.valueOf(index++))).toString(16));
    
    this.initKey(
      this.idpEncryptionCredential.getPrivateKeyResource().getFile().getAbsolutePath(),
      this.idpEncryptionCredential.getCertificateResource().getFile().getAbsolutePath(),
      this.idpEncryptionCredential.getAlias(),
      new BigInteger("aaaa", 16).add(new BigInteger(String.valueOf(index++))).toString(16));
    
    this.initKey(
      this.spSigningCredential.getPrivateKeyResource().getFile().getAbsolutePath(),
      this.spSigningCredential.getCertificateResource().getFile().getAbsolutePath(),
      this.spSigningCredential.getAlias(),
      new BigInteger("aaaa", 16).add(new BigInteger(String.valueOf(index++))).toString(16));
    
    this.initKey(
      this.spEncryptionCredential.getPrivateKeyResource().getFile().getAbsolutePath(),
      this.spEncryptionCredential.getCertificateResource().getFile().getAbsolutePath(),
      this.spEncryptionCredential.getAlias(),
      new BigInteger("aaaa", 16).add(new BigInteger(String.valueOf(index++))).toString(16));
    
    if (this.idpMetadataSigningEnabled) {
      this.initKey(
        this.idpMetadataSigningCredential.getPrivateKeyResource().getFile().getAbsolutePath(),
        this.idpMetadataSigningCredential.getCertificateResource().getFile().getAbsolutePath(),
        this.idpMetadataSigningCredential.getAlias(),
        new BigInteger("aaaa", 16).add(new BigInteger(String.valueOf(index++))).toString(16));
    }
    
    if (this.spMetadataSigningEnabled) {
      this.initKey(
        this.spMetadataSigningCredential.getPrivateKeyResource().getFile().getAbsolutePath(),
        this.spMetadataSigningCredential.getCertificateResource().getFile().getAbsolutePath(),
        this.spMetadataSigningCredential.getAlias(),
        new BigInteger("aaaa", 16).add(new BigInteger(String.valueOf(index++))).toString(16));
    }
  }

  /**
   * Performs the command line instructions to load the key and certificate into the SoftHSM slot.
   *
   * @param keyLocation
   *          The path to the key file
   * @param certLocation
   *          The path to the certificate file
   * @param alias
   *          The alias of the key and certificate
   * @param id
   *          The id of the key and certificate
   */
  private void initKey(String keyLocation, String certLocation, String alias, String id) {

    // pkcs11-tool --module /usr/lib/softhsm/libsofthsm2.so -p 1234 -l -w /root/key.pem -y privkey -a key1 -d aaaa
    // --usage-sign --usage-decrypt
    // pkcs11-tool --module /usr/lib/softhsm/libsofthsm2.so -p 1234 -l -w /opt/cert.crt -y cert -a key1 -d aaaa
    StringBuilder b = new StringBuilder();
    b.append("pkcs11-tool --module ").append(this.softHsmLib)
      .append(" -p ").append(this.getPkcs11pin())
      .append(" -l -w ").append(keyLocation)
      .append(" -y privkey -a ").append(alias)
      .append(" -d ").append(id)
      .append(" --usage-sign --usage-decrypt");
    executeCommand(b.toString());

    b = new StringBuilder();
    b.append("pkcs11-tool --module ").append(this.softHsmLib)
      .append(" -p ").append(this.getPkcs11pin())
      .append(" -l -w ").append(certLocation)
      .append(" -y cert -a ").append(alias)
      .append(" -d ").append(id);
    
    executeCommand(b.toString());

    log.info("PKCS#11 SoftHSM loaded with key ({}) and certificate ({}) for alias: {}", keyLocation, certLocation, alias);
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    super.afterPropertiesSet();
    if (!this.softHsmEnabled) {
      log.debug("SoftHSM is not enabled - no PKCS#11 setup has to be performed");
      return;
    }
    if (!StringUtils.hasText(this.softHsmLib)) {
      log.info("softHsmLib not assigned - using default {}", DEFAULT_SOFTHSM_PKCS11_LIB);
      this.softHsmLib = DEFAULT_SOFTHSM_PKCS11_LIB;
    }
    if (!StringUtils.hasText(this.pkcs11label)) {
      log.info("pkcs11label not assigned - using default {}", DEFAULT_PKCS11_LABEL);
      this.pkcs11label = DEFAULT_PKCS11_LABEL;
    }
    
    Assert.notNull(this.idpSigningCredential, "Property 'idpSigningCredential' must be assigned");
    Assert.notNull(this.idpEncryptionCredential, "Property 'idpEncryptionCredential' must be assigned");
    Assert.notNull(this.spSigningCredential, "Property 'spSigningCredential' must be assigned");
    Assert.notNull(this.spEncryptionCredential, "Property 'spEncryptionCredential' must be assigned");
    if (this.idpMetadataSigningEnabled) {
      Assert.notNull(this.idpMetadataSigningCredential, "Property 'idpMetadataSigningCredential' must be assigned");
    }
    if (this.spMetadataSigningEnabled) {
      Assert.notNull(this.spMetadataSigningCredential, "Property 'spMetadataSigningCredential' must be assigned");
    }

    if (!this.isSoftHsmInitialized()) {
      this.initializeSoftHSM();
    }
    else {
      log.info("SoftHSM has already been initialized");
    }
  }

  /**
   * Checks if SoftHSM has been initialized.
   * 
   * @return {@code true} if SoftHSM already has been initialized, and {@code false} otherwise
   */
  private boolean isSoftHsmInitialized() {
    /*
     * Test command: pkcs11-tool --module {lib} -T
     * 
     * Response for not initialized: Slot 0 (0x0): SoftHSM slot ID 0x0 token state: uninitialized
     * 
     * Response for initialized: Available slots: Slot 0 (0x5e498485): SoftHSM slot ID 0x5e498485 token label : softhsm
     */
    StringBuilder b = new StringBuilder();
    b.append("pkcs11-tool --module ").append(this.softHsmLib).append(" -T");
    String console = executeCommand(b.toString());
    boolean uninitialized = console.indexOf("Slot 0 (0x0)") > -1 && console.indexOf("token state:   uninitialized") > -1;
    log.info("Initialized state of PKCS11 SoftHSM: {}", uninitialized ? "uninitialized" : "initialized");
    return !uninitialized;
  }

  /**
   * Execute a command line command on the host.
   *
   * @param command
   *          the command to execute
   * @return the response from the host
   */
  private static String executeCommand(String command) {

    log.info("Executing command: {}", command);

    try {
      Process p = Runtime.getRuntime().exec(command);
      p.waitFor();
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

      StringBuffer output = new StringBuffer();
      String line = "";
      while ((line = reader.readLine()) != null) {
        output.append(line + "\n");
      }
      log.info("Command output: {}", output.toString());
      return output.toString();
    }
    catch (Exception e) {
      log.error("Failed to execute command: {}", e.getMessage(), e);
      return "";
    }
  }

}
