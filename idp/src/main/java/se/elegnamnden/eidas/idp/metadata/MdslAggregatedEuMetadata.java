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
package se.elegnamnden.eidas.idp.metadata;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import se.elegnamnden.eidas.metadataconfig.MetadataConfig;
import se.elegnamnden.eidas.metadataconfig.data.EndPointConfig;

/**
 * Aggregated EU metadata using the legacy {@code MetadataConfig} implementation.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
@Slf4j
public class MdslAggregatedEuMetadata extends AggregatedEuMetadataImpl {
  
  /** The URL for the MDSL file. */
  @Setter
  private String mdslUrl;
  
  /** The certificate for validating MDSL signatures. */
  @Setter
  private String mdslValidationCertificate;
  
  /** Metadata config. */
  private MetadataConfig config;

  /** {@inheritDoc} */
  @Override
  public Collection<String> getCountries() {
    if (this.config != null) {
      Collection<String> list = this.config.getProxyServiceCountryList(); 
      return list != null ? list : Collections.emptyList();
    }
    else {
      return super.getCountries();
    }
  }

  /** {@inheritDoc} */
  @Override
  public EntityDescriptor getProxyServiceIdp(String country) {
    if (this.config != null) {
      EndPointConfig ep = this.config.getProxyServiceConfig(country);
      return ep != null ? ep.getMetadataRecord() : null;
    }
    else {
      return super.getProxyServiceIdp(country);
    }
  }
  
  /**
   * Refreshes the metadata from source.
   */
  public void refresh() {
    if (this.config != null) {
      this.config.recache();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void destroy() throws Exception {
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (!StringUtils.hasText(this.mdslUrl)) {
      log.info("No MDSL URL assigned - assuming Aggregated EU metadata contains NodeCountry extension");
      this.mdslUrl = null;
    }
    else {
      if (!StringUtils.hasText(this.mdslValidationCertificate)) {
        this.mdslValidationCertificate = null;
      }
      if (!this.ignoreSignatureValidation) {
        Assert.notNull(this.mdslValidationCertificate, "The property 'mdslValidationCertificate' must be assigned");
      }
    }
    super.afterPropertiesSet();
  }

  /** {@inheritDoc} */
  @Override
  protected void init() throws Exception {
    if (StringUtils.hasText(this.mdslUrl)) {
      this.config = new MetadataConfig(new File(this.cacheDirectory), 
        this.mdslUrl, new File(this.mdslValidationCertificate), this.euMetadataUrl, new File(this.euMetadataValidationCertificate), null);
      this.config.setIgnoreSignatureValidation(this.ignoreSignatureValidation);
    }
    else {
      super.init();
    }
  }

}
