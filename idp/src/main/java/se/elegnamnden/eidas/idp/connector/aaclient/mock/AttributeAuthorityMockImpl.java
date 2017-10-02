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
package se.elegnamnden.eidas.idp.connector.aaclient.mock;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import se.elegnamnden.eidas.idp.connector.aaclient.AttributeAuthority;
import se.elegnamnden.eidas.idp.connector.aaclient.AttributeAuthorityException;
import se.litsec.swedisheid.opensaml.saml2.attribute.AttributeConstants;

/**
 * Implementation that communicates with a mocked implementation of the AA service.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class AttributeAuthorityMockImpl implements AttributeAuthority, InitializingBean {

  /** The URI this implementation uses for providing the personalIdentityNumberBinding attribute. */
  public static final String MOCK_PERSONAL_IDENTITY_NUMBER_BINDING = "http://id.elegnamnden.se/pnrb/1.0/mock-binding";

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(AttributeAuthorityMockImpl.class);

  /** The URL to the mocked AA service. */
  private String aaUrl;

  /** The Spring REST template that we use to communicate with the AA service. */
  private RestTemplate restTemplate;

  /**
   * Constructor.
   */
  public AttributeAuthorityMockImpl() {
    this.restTemplate = new RestTemplate();
  }

  /** {@inheritDoc} */
  @Override
  public List<Attribute> resolveAttributes(String id, String country) throws AttributeAuthorityException {

    try {      
      URI uri = new URI(String.format("%s/query?id=%s&c=%s", this.aaUrl, id, country));
      
      log.debug("Sending request to AA: {}", uri);      
      AAResponse response = this.restTemplate.getForObject(uri, AAResponse.class);
      log.debug("AA response: {}", response);
      
      List<Attribute> attributes = new ArrayList<>();
      
      if (response.getProvisionalId() != null) {
        attributes.add(AttributeConstants.ATTRIBUTE_TEMPLATE_PRID.createBuilder().value(response.getProvisionalId()).build());
      }
      else {
        log.warn("No 'prid' attribute received from AA for user '{}'", id);
      }
      if (response.getPidQuality() != null) {
        attributes.add(AttributeConstants.ATTRIBUTE_TEMPLATE_PRID_PERSISTENCE.createBuilder().value(response.getPidQuality()).build());
      }
      else {
        log.warn("No 'pridPersistence' attribute received from AA for user '{}'", id);
      }
      if (response.getPersonalIdNumber() != null) {
        attributes.add(AttributeConstants.ATTRIBUTE_TEMPLATE_PERSONAL_IDENTITY_NUMBER.createBuilder().value(response.getPersonalIdNumber()).build());
        String bindingUri = response.getPersonalIdNumberBinding() != null ? response.getPersonalIdNumberBinding() : MOCK_PERSONAL_IDENTITY_NUMBER_BINDING;
        attributes.add(AttributeConstants.ATTRIBUTE_TEMPLATE_PERSONAL_IDENTITY_NUMBER_BINDING.createBuilder().value(bindingUri).build());
      } 
      
      return attributes;
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
