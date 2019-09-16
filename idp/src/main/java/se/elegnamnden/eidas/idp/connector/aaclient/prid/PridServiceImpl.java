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
package se.elegnamnden.eidas.idp.connector.aaclient.prid;

import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;
import se.elegnamnden.eidas.idp.connector.aaclient.AttributeAuthorityException;

/**
 * Implementation that communicates with the PRID service.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
@Slf4j
public class PridServiceImpl implements PridService, InitializingBean {

  /** The Spring REST template that we use to communicate with the service. */
  private RestTemplate restTemplate;

  /** The URL to the PRID service. */
  private String pridServiceUrl;

  /**
   * Constructor.
   */
  public PridServiceImpl() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PridResponse getPrid(String eidasPersonIdentifier, String country) throws AttributeAuthorityException {
    try {
      URI uri = new URI(String.format("%s/generate?id=%s&c=%s", this.pridServiceUrl, eidasPersonIdentifier, country));

      log.info("PRID. Trust store: " + System.getProperty("javax.net.ssl.trustStore"));

      try {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(new FileInputStream(System.getProperty("javax.net.ssl.trustStore")),
          System.getProperty("javax.net.ssl.trustStorePassword").toCharArray());
        Enumeration<String> aliases = trustStore.aliases();
        int i = 1;
        while (aliases.hasMoreElements()) {
          X509Certificate cert = (X509Certificate) trustStore.getCertificate(aliases.nextElement());
          log.info(String.format("Trust entry %d: %s", i++, cert.toString()));
        }
      }
      catch (Exception e) {
        log.error("", e);
      }

      log.debug("Sending request to PRID service: {}", uri);
      PridResponse response = this.restTemplate.getForObject(uri, PridResponse.class);
      log.debug("PRID service response: {}", response);

      if (response.getProvisionalId() == null) {
        String msg = String.format("No 'prid' attribute received from AA for user '%s'", eidasPersonIdentifier);
        log.warn(msg);
        throw new AttributeAuthorityException(msg);
      }
      if (response.getPidQuality() == null) {
        String msg = String.format("No 'pridPersistence' attribute received from AA for user '%s'", eidasPersonIdentifier);
        log.warn(msg);
        throw new AttributeAuthorityException(msg);
      }

      return response;
    }
    catch (URISyntaxException | RestClientException e) {
      log.error("Failure communicating with PRID service - {}", e.getMessage(), e);
      throw new AttributeAuthorityException("Failure querying PRID service", e);
    }
  }

  /**
   * Assigns the URL to the PRID service.
   * 
   * @param pridServiceUrl
   *          URL to PRID service
   */
  public void setPridServiceUrl(String pridServiceUrl) {
    this.pridServiceUrl = pridServiceUrl;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.hasText(this.pridServiceUrl, "Property 'pridServiceUrl' must be assigned");
    if (this.pridServiceUrl.endsWith("/")) {
      this.pridServiceUrl = this.pridServiceUrl.substring(0, this.pridServiceUrl.length() - 1);
    }
    this.restTemplate = new RestTemplate();
  }

}
