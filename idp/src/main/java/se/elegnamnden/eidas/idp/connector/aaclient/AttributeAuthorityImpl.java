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
package se.elegnamnden.eidas.idp.connector.aaclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import org.opensaml.saml.saml2.core.Attribute;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;
import se.elegnamnden.eidas.idp.connector.aaclient.prid.PridResponse;
import se.elegnamnden.eidas.idp.connector.aaclient.prid.PridService;
import se.litsec.opensaml.saml2.attribute.AttributeBuilder;
import se.litsec.opensaml.saml2.attribute.AttributeUtils;
import se.litsec.swedisheid.opensaml.saml2.attribute.AttributeConstants;

/**
 * Implementation for the AA interface.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
@Slf4j
public class AttributeAuthorityImpl implements AttributeAuthority, InitializingBean {

  /** The URI used when providing the personalIdentityNumberBinding attribute for an unspecified binding. */
  public static final String UNSPECIFIED_PERSONAL_IDENTITY_NUMBER_BINDING = "http://id.elegnamnden.se/pnrb/1.0/unspecified";

  /**
   * The URI used when providing the personalIdentityNumberBinding attribute for a static binding (received directly
   * from IdP).
   */
  public static final String STATIC_PERSONAL_IDENTITY_NUMBER_BINDING = "http://id.elegnamnden.se/pnrb/1.0/from-se-idp";

  /** The PRID service. */
  private PridService pridService;

  /** Function that finds an attribute with a given name from a list of attributes. */
  private static BiFunction<String, List<Attribute>, Attribute> getAttribute = (name, list) -> {
    return list.stream().filter(a -> name.equals(a.getName())).findFirst().orElse(null);
  };

  /**
   * Deprecated - will map to {@link #resolveAttributes(List, String)}.
   */
  @Override
  public List<Attribute> resolveAttributes(String id, String country) throws AttributeAuthorityException {
    log.warn("Invocation of deprecated method - change implementation!");
    return this.resolveAttributes(Arrays.asList(AttributeBuilder.builder(AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER)
      .value(id)
      .build()), country);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Attribute> resolveAttributes(List<Attribute> attributes, String country) throws AttributeAuthorityException {

    // Get the PRID.
    //

    // The ID to supply to the AA service is the 'eidasPersonIdentifier'. Let's look that up ...
    Attribute eidasPersonIdentifier = getAttribute.apply(AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER, attributes);
    if (eidasPersonIdentifier == null) {
      final String msg = String.format("Attribute '%s' (%s) was not received in Assertion - can not proceed",
        AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_EIDAS_PERSON_IDENTIFIER, AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER);
      throw new AttributeAuthorityException(msg);
    }

    final String eidasId = AttributeUtils.getAttributeStringValue(eidasPersonIdentifier);
    PridResponse pridResponse = this.pridService.getPrid(eidasId, country);

    // Map to attributes
    //
    List<Attribute> resolvedAttributes = new ArrayList<>();
    resolvedAttributes.add(AttributeConstants.ATTRIBUTE_TEMPLATE_PRID.createBuilder().value(pridResponse.getProvisionalId()).build());
    resolvedAttributes.add(AttributeConstants.ATTRIBUTE_TEMPLATE_PRID_PERSISTENCE.createBuilder()
      .value(pridResponse.getPidQuality())
      .build());

    if (pridResponse.getPersonalIdNumber() != null) {
      resolvedAttributes.add(AttributeConstants.ATTRIBUTE_TEMPLATE_PERSONAL_IDENTITY_NUMBER.createBuilder()
        .value(pridResponse.getPersonalIdNumber())
        .build());

      if (pridResponse.getPersonalIdNumberBinding() != null) {
        resolvedAttributes.add(AttributeConstants.ATTRIBUTE_TEMPLATE_PERSONAL_IDENTITY_NUMBER_BINDING.createBuilder()
          .value(pridResponse.getPersonalIdNumberBinding())
          .build());
      }
      else {
        log.warn("PRID service returned personal identity number, but no corresponding binding URI - using '{}'",
          UNSPECIFIED_PERSONAL_IDENTITY_NUMBER_BINDING);
        resolvedAttributes.add(AttributeConstants.ATTRIBUTE_TEMPLATE_PERSONAL_IDENTITY_NUMBER_BINDING.createBuilder()
          .value(UNSPECIFIED_PERSONAL_IDENTITY_NUMBER_BINDING)
          .build());
      }
    }
    else if (pridResponse.getPersonalIdNumberBinding() != null) {
      log.warn("Received personal identity number binding URI from PRID service, but no personal identity number. Ignoring attribute");
    }
    else if ("SE".equalsIgnoreCase(country)) {

      // In test we have the possibility to loop back to SE. In those cases, we release the personalIdentityNumber
      // as an attribute since this information is known.
      //
      final String prid = pridResponse.getProvisionalId();
      final String pnr = prid.substring(3);

      log.info("Adding attribute personalIdentityNumber '{}' for user '{}'", pnr, eidasId);
      resolvedAttributes.add(AttributeConstants.ATTRIBUTE_TEMPLATE_PERSONAL_IDENTITY_NUMBER.createBuilder().value(pnr).build());
      resolvedAttributes.add(AttributeConstants.ATTRIBUTE_TEMPLATE_PERSONAL_IDENTITY_NUMBER_BINDING.createBuilder()
        .value(STATIC_PERSONAL_IDENTITY_NUMBER_BINDING)
        .build());
    }

    return resolvedAttributes;
  }

  /**
   * Assigns the PRID service.
   * 
   * @param pridService
   *          the PRID service
   */
  public void setPridService(PridService pridService) {
    this.pridService = pridService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(this.pridService, "Attribute 'pridService' must be assigned");
  }

}
