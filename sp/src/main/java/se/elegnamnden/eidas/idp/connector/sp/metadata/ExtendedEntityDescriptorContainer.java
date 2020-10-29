/*
 * Copyright 2017-2020 Sweden Connect
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

import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.xmlsec.signature.support.SignatureException;

import lombok.extern.slf4j.Slf4j;
import se.litsec.opensaml.saml2.metadata.EntityDescriptorContainer;
import se.litsec.opensaml.utils.SignatureUtils;
import se.swedenconnect.opensaml.xmlsec.config.SecurityConfiguration;

/**
 * Extends the {@link EntityDescriptorContainer} with the possibility to set the security configuration to use while
 * signing.
 * 
 * @author Martin Lindström (martin@litsec.se)
 */
@Slf4j
public class ExtendedEntityDescriptorContainer extends EntityDescriptorContainer {

  /** The security configuration to use when signing metadata (optional). */
  private SecurityConfiguration securityConfiguration;

  /**
   * Constructor.
   * 
   * @param descriptor
   *          the entity descriptor
   * @param signatureCredentials
   *          the signature credentials
   */
  public ExtendedEntityDescriptorContainer(final EntityDescriptor descriptor, final X509Credential signatureCredentials) {
    super(descriptor, signatureCredentials);
  }
  
  /** {@inheritDoc} */
  @Override
  public synchronized EntityDescriptor sign() throws SignatureException, MarshallingException {
    if (this.securityConfiguration == null) {
      return super.sign();
    }
    log.trace("Signing descriptor '{}' with security configuration {} ...", this.getLogString(this.descriptor), this.securityConfiguration.getProfileName());

    if (this.getID(this.descriptor) == null || this.descriptor.getValidUntil() == null) {
      return this.update(true);
    }
    
    SignatureUtils.sign(this.descriptor, this.signatureCredentials, this.securityConfiguration.getSignatureSigningConfiguration());
    
    log.debug("Descriptor '{}' successfully signed.", this.getLogString(this.descriptor));

    return this.descriptor;
  }

  /**
   * Assigns the security configuration to use when signing metadata.
   * 
   * @param securityConfiguration
   *          security configuration
   */
  public void setSecurityConfiguration(final SecurityConfiguration securityConfiguration) {
    this.securityConfiguration = securityConfiguration;
  }

}
