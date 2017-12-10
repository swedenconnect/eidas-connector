/*
 * The eidas-connector project is the implementation of the Swedish eIDAS 
 * connector built on top of the Shibboleth IdP.
 *
 * More details on <https://github.com/elegnamnden/eidas-connector> 
 * Copyright (C) 2017 E-legitimationsnämnden
 * 
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.elegnamnden.eidas.idp.connector.aaclient.prid;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
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

  /** Flag for development mode. */
  private boolean develMode = false;

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

  /**
   * Assigns whether we are running in devel mode.
   * 
   * @param develMode
   *          devel mode flag
   */
  public void setDevelMode(boolean develMode) {
    this.develMode = develMode;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.hasText(this.pridServiceUrl, "Property 'pridServiceUrl' must be assigned");
    if (this.pridServiceUrl.endsWith("/")) {
      this.pridServiceUrl = this.pridServiceUrl.substring(0, this.pridServiceUrl.length() - 1);
    }

    if (this.develMode) {
      CloseableHttpClient httpClient = HttpClients.custom().setHostnameVerifier(new NullHostnameVerifier()).build();
      HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
      requestFactory.setHttpClient(httpClient);
      this.restTemplate = new RestTemplate(requestFactory);
    }
    else {
      this.restTemplate = new RestTemplate();
    }
  }
  
  public static class NullHostnameVerifier implements X509HostnameVerifier {

    @Override
    public boolean verify(String hostname, SSLSession session) {
      return true;
    }

    @Override
    public void verify(String host, SSLSocket ssl) throws IOException {
    }

    @Override
    public void verify(String host, X509Certificate cert) throws SSLException {
    }

    @Override
    public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
    }

  }

  private RestTemplate createTrustAllRestTemplate() {
    try {
      TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

      SSLContext sslContext = SSLContexts.custom()
        .loadTrustMaterial(null, acceptingTrustStrategy)
        .build();

      SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

      CloseableHttpClient httpClient = HttpClients.custom()
        .setSSLSocketFactory(csf)
        .build();

      HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
      requestFactory.setHttpClient(httpClient);

      return new RestTemplate(requestFactory);
    }
    catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
      throw new SecurityException(e);
    }
  }
  

}
