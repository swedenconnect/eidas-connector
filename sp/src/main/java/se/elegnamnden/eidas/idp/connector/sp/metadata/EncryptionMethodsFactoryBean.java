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

import java.util.ArrayList;
import java.util.List;

import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.metadata.EncryptionMethod;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.EncryptionConfiguration;
import org.opensaml.xmlsec.algorithm.AlgorithmDescriptor;
import org.opensaml.xmlsec.algorithm.AlgorithmRegistry;
import org.opensaml.xmlsec.algorithm.AlgorithmSupport;
import org.opensaml.xmlsec.encryption.OAEPparams;
import org.opensaml.xmlsec.encryption.support.RSAOAEPParameters;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import lombok.extern.slf4j.Slf4j;
import net.shibboleth.utilities.java.support.logic.Constraint;
import se.swedenconnect.opensaml.xmlsec.ExtendedEncryptionConfiguration;
import se.swedenconnect.opensaml.xmlsec.algorithm.ExtendedAlgorithmSupport;
import se.swedenconnect.opensaml.xmlsec.config.SecurityConfiguration;
import se.swedenconnect.opensaml.xmlsec.encryption.ConcatKDFParams;
import se.swedenconnect.opensaml.xmlsec.encryption.KeyDerivationMethod;
import se.swedenconnect.opensaml.xmlsec.encryption.support.EcEncryptionConstants;

/**
 * Factory bean that creates a list of encryption method objects for insertion in SP metadata.
 * 
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
public class EncryptionMethodsFactoryBean extends AbstractFactoryBean<List<EncryptionMethod>> {

  /** The SP security configuration. */
  private SecurityConfiguration spSecurityConfiguration;

  /** The SP encryption credential. */
  private Credential encryptionCredential;

  /**
   * Constructor setting up the factory bean using the security configuration for the SP and encryption credential.
   * 
   * @param spSecurityConfiguration
   *          the SP security configuration
   * @param encryptionCredential
   *          the SP encryption credential
   */
  public EncryptionMethodsFactoryBean(SecurityConfiguration spSecurityConfiguration, Credential encryptionCredential) {
    this.spSecurityConfiguration = Constraint.isNotNull(spSecurityConfiguration, "'spSecurityConfiguration' must not be null");
    this.encryptionCredential = Constraint.isNotNull(encryptionCredential, "'encryptionCredential' must not be null");
  }

  /** {@inheritDoc} */
  @Override
  public Class<?> getObjectType() {
    return List.class;
  }

  /** {@inheritDoc} */
  @Override
  protected List<EncryptionMethod> createInstance() throws Exception {
    final EncryptionConfiguration encryptionConfiguration = this.spSecurityConfiguration.getEncryptionConfiguration();
    List<EncryptionMethod> encryptionMethods = new ArrayList<>();

    // Add supported data encryption algorithms.
    //
    for (String alg : encryptionConfiguration.getDataEncryptionAlgorithms()) {
      EncryptionMethod em = (EncryptionMethod) XMLObjectSupport.buildXMLObject(EncryptionMethod.DEFAULT_ELEMENT_NAME);
      em.setAlgorithm(alg);
      encryptionMethods.add(em);
    }

    boolean keyAgreement = false;

    if (this.encryptionCredential.getPublicKey().getAlgorithm().equals("EC")) {
      if (ExtendedEncryptionConfiguration.class.isInstance(encryptionConfiguration)) {
        ExtendedEncryptionConfiguration extEncryptionConfiguration = ExtendedEncryptionConfiguration.class.cast(encryptionConfiguration);
        for (String alg : extEncryptionConfiguration.getAgreementMethodAlgorithms()) {
          EncryptionMethod em = (EncryptionMethod) XMLObjectSupport.buildXMLObject(EncryptionMethod.DEFAULT_ELEMENT_NAME);
          em.setAlgorithm(alg);
          if (alg.equals(EcEncryptionConstants.ALGO_ID_KEYAGREEMENT_ECDH_ES)) {
            KeyDerivationMethod kdm = (KeyDerivationMethod) XMLObjectSupport.buildXMLObject(KeyDerivationMethod.DEFAULT_ELEMENT_NAME);
            kdm.setAlgorithm(EcEncryptionConstants.ALGO_ID_KEYDERIVATION_CONCAT);
            ConcatKDFParams cparams = extEncryptionConfiguration.getConcatKDFParameters().toXMLObject();
            kdm.getUnknownXMLObjects().add(cparams);
            em.getUnknownXMLObjects().add(kdm);
          }
          encryptionMethods.add(em);
        }
        keyAgreement = true;
      }
    }
    else if (this.encryptionCredential.getPublicKey().getAlgorithm().equals("RSA")) {
      for (String alg : encryptionConfiguration.getKeyTransportEncryptionAlgorithms()) {
        if (AlgorithmSupport.isRSAOAEP(alg)) {
          EncryptionMethod em = (EncryptionMethod) XMLObjectSupport.buildXMLObject(EncryptionMethod.DEFAULT_ELEMENT_NAME);
          em.setAlgorithm(alg);
          RSAOAEPParameters pars = encryptionConfiguration.getRSAOAEPParameters();
          if (pars.getOAEPParams() != null) {
            OAEPparams oaepParams = (OAEPparams) XMLObjectSupport.buildXMLObject(OAEPparams.DEFAULT_ELEMENT_NAME);
            oaepParams.setValue(pars.getOAEPParams());
            em.setOAEPparams(oaepParams);
          }
          encryptionMethods.add(em);
        }
      }
    }
    else {
      log.info("Unknown encryption credential '{}' - will not map to encryption method",
        this.encryptionCredential.getPublicKey().getAlgorithm());
    }

    // If we declared a key agreement algorithm, also declare key wrapping algos.
    //
    final AlgorithmRegistry algorithmRegistry = AlgorithmSupport.getGlobalAlgorithmRegistry();
    if (keyAgreement) {
      for (String alg : encryptionConfiguration.getKeyTransportEncryptionAlgorithms()) {
        AlgorithmDescriptor ad = algorithmRegistry.get(alg);
        if (ad != null && ExtendedAlgorithmSupport.isKeyWrappingAlgorithm(ad)) {
          EncryptionMethod em = (EncryptionMethod) XMLObjectSupport.buildXMLObject(EncryptionMethod.DEFAULT_ELEMENT_NAME);
          em.setAlgorithm(alg);
          encryptionMethods.add(em);
        }
      }
    }

    return encryptionMethods;
  }

}
