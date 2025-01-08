/*
 * Copyright 2017-2025 Sweden Connect
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
package se.swedenconnect.eidas.connector.authn.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;

import se.swedenconnect.opensaml.eidas.ext.NodeCountry;
import se.swedenconnect.opensaml.saml2.attribute.AttributeConstants;
import se.swedenconnect.opensaml.saml2.attribute.AttributeUtils;
import se.swedenconnect.opensaml.saml2.metadata.EntityDescriptorUtils;

/**
 * Utility functions for handling metadata.
 *
 * @author Martin Lindström
 */
public class MetadataFunctions {

  /** Entity category value for "hide from discovery". */
  public static final String HIDE_FROM_DISCOVERY_ENTITY_CATEGORY = "http://refeds.org/category/hide-from-discovery";

  /**
   * Gets the node country extension value from the metadata.
   *
   * @param ed the metadata entry
   * @return the node country (or {@code null})
   */
  public static String getNodeCountry(final EntityDescriptor ed) {
    final Extensions exts = Optional.ofNullable(ed.getIDPSSODescriptor(SAMLConstants.SAML20P_NS))
        .map(IDPSSODescriptor::getExtensions)
        .orElse(null);

    if (exts != null) {
      return Optional.ofNullable(EntityDescriptorUtils.getMetadataExtension(exts, NodeCountry.class))
          .map(NodeCountry::getNodeCountry)
          .orElse(null);
    }
    return null;
  }

  /**
   * Method that returns {@code true} if the "hide from discovery" entity category is present in the supplied metadata
   * and {@code false} otherwise.
   *
   * @param ed the metadata entry
   * @return {@code true} if hide-from-discovery is declared, and {@code false} otherwise
   */
  public static boolean getHideFromDiscovery(final EntityDescriptor ed) {

    return EntityDescriptorUtils.getEntityCategories(ed)
        .stream()
        .anyMatch(HIDE_FROM_DISCOVERY_ENTITY_CATEGORY::equals);
  }

  /**
   * Gets the declared assurance levels from the supplied metadata entry.
   *
   * @param ed the metadata entry
   * @return a list of assurance levels
   */
  public static List<String> getAssuranceLevels(final EntityDescriptor ed) {
    // Very defensive implementation. We can handle several EntityAttributes extensions
    // and that several matching attributes ...
    //
    final Extensions extensions = ed.getExtensions();
    if (extensions == null) {
      return Collections.emptyList();
    }
    final List<String> uris = new ArrayList<>();
    for (final XMLObject xml : extensions.getUnknownXMLObjects()) {
      if (xml instanceof EntityAttributes ea) {
        ea.getAttributes().stream()
            .filter(a -> AttributeConstants.ASSURANCE_CERTIFICATION_ATTRIBUTE_NAME.equals(a.getName()))
            .map(AttributeUtils::getAttributeStringValues)
            .flatMap(List::stream)
            .forEach(a -> {
              if (!uris.contains(a)) {
                uris.add(a);
              }
            });
      }
    }
    return uris;
  }

  private MetadataFunctions() {
  }

}
