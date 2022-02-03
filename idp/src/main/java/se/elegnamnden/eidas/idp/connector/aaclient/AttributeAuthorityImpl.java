/*
 * Copyright 2017-2022 Sweden Connect
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
  public List<Attribute> resolveAttributes(final String id, final String country) throws AttributeAuthorityException {
    log.warn("Invocation of deprecated method - change implementation!");
    return this.resolveAttributes(Arrays.asList(AttributeBuilder.builder(AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER)
      .value(id)
      .build()), country);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Attribute> resolveAttributes(final List<Attribute> attributes, final String country) throws AttributeAuthorityException {

    // Get the PRID.
    //

    // The ID to supply to the AA service is the 'eidasPersonIdentifier'. Let's look that up ...
    final Attribute eidasPersonIdentifier = getAttribute.apply(AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER, attributes);
    if (eidasPersonIdentifier == null) {
      final String msg = String.format("Attribute '%s' (%s) was not received in Assertion - can not proceed",
        AttributeConstants.ATTRIBUTE_FRIENDLY_NAME_EIDAS_PERSON_IDENTIFIER, AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER);
      throw new AttributeAuthorityException(msg);
    }

    final String eidasId = AttributeUtils.getAttributeStringValue(eidasPersonIdentifier);
    final PridResponse pridResponse = this.pridService.getPrid(eidasId, country);

    // Map to attributes
    //
    final List<Attribute> resolvedAttributes = new ArrayList<>();
    resolvedAttributes.add(AttributeConstants.ATTRIBUTE_TEMPLATE_PRID.createBuilder().value(pridResponse.getProvisionalId()).build());
    resolvedAttributes.add(AttributeConstants.ATTRIBUTE_TEMPLATE_PRID_PERSISTENCE.createBuilder()
      .value(pridResponse.getPidQuality())
      .build());

    if (pridResponse.getPersonalIdNumber() != null) {
      resolvedAttributes.add(AttributeConstants.ATTRIBUTE_TEMPLATE_MAPPED_PERSONAL_IDENTITY_NUMBER.createBuilder()
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
    else if ("SE".equalsIgnoreCase(country) || "XE".equalsIgnoreCase(country)) {

      // In test we have the possibility to loop back to SE. In those cases, we release the personalIdentityNumber
      // as an attribute since this information is known.
      //
      final String prid = pridResponse.getProvisionalId();
      final String pnr = prid.substring(3);

      log.info("Adding attribute personalIdentityNumber '{}' for user '{}'", pnr, eidasId);
      resolvedAttributes.add(AttributeConstants.ATTRIBUTE_TEMPLATE_MAPPED_PERSONAL_IDENTITY_NUMBER.createBuilder().value(pnr).build());
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
  public void setPridService(final PridService pridService) {
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
