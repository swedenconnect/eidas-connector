/*
 * Copyright 2023 Sweden Connect
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
package se.swedenconnect.eidas.attributes;

import java.util.Collection;
import java.util.List;

import org.opensaml.saml.saml2.core.Attribute;

import se.swedenconnect.spring.saml.idp.attributes.RequestedAttribute;

/**
 * Handles mappings between Swedish eID attributes and eIDAS attributes.
 * 
 * @author Martin Lindström
 */
public interface AttributeMappingService {

  /**
   * The eIDAS minimum data set for natural persons.
   */
  static List<String> NATURAL_PERSON_MINIMUM_DATASET = List.of(
      se.litsec.eidas.opensaml.ext.attributes.AttributeConstants.EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME,
      se.litsec.eidas.opensaml.ext.attributes.AttributeConstants.EIDAS_CURRENT_GIVEN_NAME_ATTRIBUTE_NAME,
      se.litsec.eidas.opensaml.ext.attributes.AttributeConstants.EIDAS_CURRENT_FAMILY_NAME_ATTRIBUTE_NAME,
      se.litsec.eidas.opensaml.ext.attributes.AttributeConstants.EIDAS_DATE_OF_BIRTH_ATTRIBUTE_NAME);

  /**
   * Maps an attribute released by a Swedish eID IdP into its corresponding eIDAS attribute.
   * 
   * @param swedishAttribute the Swedish eID attribute
   * @return an eIDAS attribute or {@code null} if no mapping exists
   */
  Attribute toEidasAttribute(final Attribute swedishAttribute);

  /**
   * Given a {@link se.swedenconnect.spring.saml.idp.attributes.RequestedAttribute
   * se.swedenconnect.spring.saml.idp.attributes.RequestedAttribute}  calculated by the SAML IdP base, the method builds
   * an eIDAS {@link se.litsec.eidas.opensaml.ext.RequestedAttribute} element to be used in an eIDAS AuthnRequest.
   * <p>
   * Note: The implementation may not assign the {@code isRequired} attribute to the resulting object even if it is set
   * in the supplied metadata requested attribute. For interoperability reasons only attributes part of the eIDAS
   * minimum data set should be assigned the {@code isRequired} attribute.
   * </p>
   * 
   * @param requestedBySwedishSp the requested attribute from the Swedish SP
   * @return an eIDAS {@code RequestedAttribute} object or {@code null}
   */
  se.litsec.eidas.opensaml.ext.RequestedAttribute toEidasRequestedAttribute(
      final RequestedAttribute requestedBySwedishSp);

  /**
   * Gets a list of eIDAS {@link se.litsec.eidas.opensaml.ext.RequestedAttribute}s based on the requested attributes set
   * by the Swedish SP. If the {@code includeMinimumDataSet} is set, the minimum data set is always returned.
   * 
   * @param requestedBySwedishSp the requested attributes from the Swedish SP
   * @param includeMinimumDataSet whether the minimum data set should be included (independently of what is passed)
   * @return a list of eIDAS {@link se.litsec.eidas.opensaml.ext.RequestedAttribute RequestedAttribute} objects
   */
  List<se.litsec.eidas.opensaml.ext.RequestedAttribute> toEidasRequestedAttributes(
      final Collection<RequestedAttribute> requestedBySwedishSp, final boolean includeMinimumDataSet);

  /**
   * Maps an attribute released by an eIDAS IdP into its corresponding Swedish eID attribute.
   * 
   * @param eidasAttribute an eIDAS attribute
   * @return a Swedish eID attribute or {@code null}
   */
  Attribute toSwedishEidAttribute(final Attribute eidasAttribute);

}
