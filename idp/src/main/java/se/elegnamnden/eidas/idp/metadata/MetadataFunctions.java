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
package se.elegnamnden.eidas.idp.metadata;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;

import se.litsec.eidas.opensaml.ext.NodeCountry;
import se.litsec.opensaml.saml2.attribute.AttributeUtils;
import se.litsec.opensaml.saml2.metadata.MetadataUtils;
import se.litsec.swedisheid.opensaml.saml2.metadata.entitycategory.EntityCategoryMetadataHelper;

/**
 * Utility functions for handling metadata.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class MetadataFunctions {

  /** Entity category value for "hide from discovery". */
  public static final String HIDE_FROM_DISCOVERY_ENTITY_CATEGORY = "http://refeds.org/category/hide-from-discovery";

  /**
   * The attribute name for the assurance certification attribute stored as an attribute in the entity attributes
   * extension.
   */
  public static final String ASSURANCE_CERTIFICATION_ATTRIBUTE_NAME = "urn:oasis:names:tc:SAML:attribute:assurance-certification";

  /**
   * Gets the node country extension value from the metadata.
   * 
   * @param ed
   *          the metadata entry
   * @return the node country (or null)
   */
  public static String getNodeCountry(final EntityDescriptor ed) {
    return MetadataUtils.getMetadataExtension(Optional.ofNullable(ed.getIDPSSODescriptor(SAMLConstants.SAML20P_NS))
      .map(IDPSSODescriptor::getExtensions)
      .orElse(null), NodeCountry.class)
      .map(NodeCountry::getNodeCountry)
      .orElse(null);
  }

  /**
   * Method that returns {@code true} if the "hide from discovery" entity category is present in the supplied metadata
   * and {@code false} otherwise.
   * 
   * @param ed
   *          the metadata entry
   * @return true if hide-from-discovery is declared, and false otherwise
   */
  public static boolean getHideFromDiscovery(final EntityDescriptor ed) {
    return EntityCategoryMetadataHelper.getEntityCategories(ed)
      .stream()
      .filter(a -> HIDE_FROM_DISCOVERY_ENTITY_CATEGORY.equals(a))
      .findFirst()
      .isPresent();
  }

  /**
   * Gets the declared assurance levels from the supplied metadata entry.
   * 
   * @param ed
   *          the metadata entry
   * @return a list of assurance levels
   */
  public static List<String> getAssuranceLevels(final EntityDescriptor ed) {
    return Optional.ofNullable(
      MetadataUtils.getEntityAttributes(ed).map(EntityAttributes::getAttributes).orElse(Collections.emptyList()))
      .orElse(Collections.emptyList())
      .stream()
      .filter(ec -> ASSURANCE_CERTIFICATION_ATTRIBUTE_NAME.equals(ec.getName()))
      .map(a -> AttributeUtils.getAttributeStringValues(a))
      .flatMap(List::stream)
      .distinct()
      .collect(Collectors.toList());
  }

  private MetadataFunctions() {
  }

}
