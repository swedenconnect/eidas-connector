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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import se.litsec.eidas.opensaml.ext.NodeCountry;
import se.litsec.opensaml.saml2.metadata.MetadataUtils;
import se.litsec.opensaml.saml2.metadata.provider.AbstractMetadataProvider;
import se.litsec.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import se.litsec.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import se.litsec.opensaml.saml2.metadata.provider.MetadataProvider;
import se.litsec.opensaml.utils.X509CertificateUtils;

/**
 * Implementation of aggregated EU metadata where the metadata contains {@code eidas:NodeCountry} elements for each IdP.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
@Slf4j
public class AggregatedEuMetadataImpl implements AggregatedEuMetadata, InitializingBean, DisposableBean {

  /** File name for caching downloaded metadata. */
  protected static final String CACHE_FILE = "eu-metadata.xml";

  /** The download URL for EU metadata. */
  @Setter
  protected String euMetadataUrl;

  /** The signature validation certificate for downloaded metadata. */
  @Setter
  protected String euMetadataValidationCertificate;

  /** Should we ignore signature validation? For test only. */
  @Setter
  protected boolean ignoreSignatureValidation = false;

  /** The directory holding the metadata caches. */
  @Setter
  protected String cacheDirectory;

  /** The metadata provider responsible of downloading the aggregated metadata. */
  protected MetadataProvider metadataProvider;

  /** An index of country codes and their respective entity descriptors. */
  private Map<String, EntityDescriptor> countries = Collections.emptyMap();

  /** The last time the country list was indexed. */
  private long countryIndexingTime = 0L;

  /** Given an {@code EntityDescriptor} the method returns its node country. */
  private static Function<EntityDescriptor, String> getNodeCountry = (e) -> {
    IDPSSODescriptor sso = e.getIDPSSODescriptor(SAMLConstants.SAML20P_NS);
    if (sso == null) {
      return null;
    }
    return MetadataUtils.getMetadataExtension(sso.getExtensions(), NodeCountry.class).map(NodeCountry::getNodeCountry).orElse(null);
  };

  /**
   * Performs a re-index of the country list every time new metadata has been downloaded.
   */
  @Override
  public synchronized Collection<String> getCountries() {
    if (this.metadataProvider.getLastUpdate().orElse(new DateTime()).isAfter(this.countryIndexingTime)) {
      try {
        Map<String, EntityDescriptor> cm = new HashMap<>();
        for (EntityDescriptor ed : this.metadataProvider.getIdentityProviders()) {
          String countryCode = getNodeCountry.apply(ed);
          if (countryCode != null) {
            cm.put(countryCode.toUpperCase(), ed);
          }
          else {
            log.error("Found IdP '{}' in EU metadata that does not have NodeCountry extension", ed.getEntityID());
          }
        }
        this.countries = cm;
      }
      catch (ResolverException e) {
        log.error("Failed to list metadata from {}", this.metadataProvider.getID(), e);
      }
    }
    return this.countries.keySet().stream().sorted().collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public EntityDescriptor getProxyServiceIdp(String country) {
    if (!StringUtils.hasText(country)) {
      return null;
    }
    return this.countries.get(country.toUpperCase());
  }

  /** {@inheritDoc} */
  @Override
  public void destroy() throws Exception {
    if (this.metadataProvider != null) {
      this.metadataProvider.destroy();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.hasText(this.euMetadataUrl, "The property 'euMetadataUrl' must be assigned");
    if (!this.ignoreSignatureValidation) {
      Assert.hasText(this.euMetadataValidationCertificate, "The property 'euMetadataValidationCertificate' must be assigned");
    }
    Assert.hasText(this.cacheDirectory, "The property 'cacheDirectory' must be assigned");

    Path _cacheDirectory = Paths.get(this.cacheDirectory);
    if (Files.exists(_cacheDirectory)) {
      Assert.isTrue(Files.isDirectory(_cacheDirectory), String.format("%s is not a directory", this.cacheDirectory));
      Assert.isTrue(Files.isWritable(_cacheDirectory), String.format("Cache directory '%s' is not writable", this.cacheDirectory));
    }
    else {
      log.info("Cache directory {} does not exist - creating ...");
      Files.createDirectories(_cacheDirectory);
    }

    this.init();
  }

  /**
   * Initializes the metadata download provider.
   * 
   * @throws Exception
   *           for init errors
   */
  protected void init() throws Exception {
    AbstractMetadataProvider _metadataProvider;

    if (this.euMetadataUrl.startsWith("file://")) {
      _metadataProvider = new FilesystemMetadataProvider(new File(new URI(this.euMetadataUrl)));
    }
    else {
      HttpClientSecurityParameters tlsPars = new HttpClientSecurityParameters();
      tlsPars.setTLSTrustEngine((token, trustBasisCriteria) -> true);

      Path backupFile = Paths.get(this.cacheDirectory, CACHE_FILE);
      _metadataProvider = new HTTPMetadataProvider(this.euMetadataUrl, backupFile.toFile().getAbsolutePath(), tlsPars);
    }
    _metadataProvider.setFailFastInitialization(false);
    _metadataProvider.setPerformSchemaValidation(false);
    _metadataProvider.setRequireValidMetadata(true);
    if (!this.ignoreSignatureValidation) {
      X509Certificate validationCert;
      try {
        validationCert = X509CertificateUtils.decodeCertificate(new File(this.euMetadataValidationCertificate));
      }
      catch (Exception e) {
        log.error("Failed to load certificate validation certificate for EU metadata ({}) - {}", this.euMetadataValidationCertificate, e
          .getMessage());
        throw e;
      }
      _metadataProvider.setSignatureVerificationCertificate(validationCert);
    }
    else {
      log.warn("Signature validation of EU metadata turned off");
    }
    
    this.metadataProvider = _metadataProvider;
    this.metadataProvider.initialize();
  }

}
