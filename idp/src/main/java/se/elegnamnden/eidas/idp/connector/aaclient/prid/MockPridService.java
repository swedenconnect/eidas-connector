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

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;
import se.elegnamnden.eidas.idp.connector.aaclient.AttributeAuthorityException;

/**
 * Implementation that communicates with the mock/test PRID service.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
@Slf4j
public class MockPridService implements PridService, InitializingBean {
  
  /** The URI this implementation uses for providing the personalIdentityNumberBinding attribute. */
  public static final String MOCK_PERSONAL_IDENTITY_NUMBER_BINDING = "http://id.elegnamnden.se/pnrb/1.0/mock-binding";  

  /** The Spring REST template that we use to communicate with the service. */
  private RestTemplate restTemplate;
  
  /** The URL to the mocked AA service. */
  private String aaUrl;  
  
  /**
   * Constructor.
   */
  public MockPridService() {
    this.restTemplate = new RestTemplate();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PridResponse getPrid(String eidasPersonIdentifier, String country) throws AttributeAuthorityException {
    try {      
      URI uri = new URI(String.format("%s/query?id=%s&c=%s", this.aaUrl, eidasPersonIdentifier, country));
      
      log.debug("Sending request to AA: {}", uri);
      PridResponse response = this.restTemplate.getForObject(uri, PridResponse.class);
      log.debug("AA response: {}", response);
            
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
      if (response.getPersonalIdNumber() != null && response.getPersonalIdNumberBinding() == null) {
        response.setPersonalIdNumberBinding(MOCK_PERSONAL_IDENTITY_NUMBER_BINDING);
      } 
      
      return response;
    }
    catch (URISyntaxException | RestClientException e) {
      log.error("Failure communicating with AA service - {}", e.getMessage(), e);
      throw new AttributeAuthorityException("Failure querying AA service", e);
    }
  }

  /**
   * Assigns the URL to the mocked AA service.
   * 
   * @param aaUrl
   *          URL to AA service
   */
  public void setAaUrl(String aaUrl) {
    this.aaUrl = aaUrl;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.hasText(this.aaUrl, "Property 'aaUrl' must be assigned");
  }
    
}
