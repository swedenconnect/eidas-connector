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
package se.elegnamnden.eidas.idp.connector.controller;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.ext.saml2mdui.Logo;
import org.opensaml.saml.ext.saml2mdui.UIInfo;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.LocalizedName;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;

import se.elegnamnden.eidas.idp.connector.controller.model.SpInfo;
import se.litsec.opensaml.saml2.metadata.MetadataUtils;

/**
 * Handler class for constructing a {@link SpInfo} model object.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class SpInfoHandler {

  /**
   * Builds a {@link SpInfo} model object given an entity's metadata entry.
   * 
   * @param metadata
   *          the metadata
   * @param language
   *          the preferred language
   * @param fallbackLanguages
   *          fallback languages
   * @return a {@code SpInfo} object
   */
  public static SpInfo buildSpInfo(EntityDescriptor metadata, String language, List<String> fallbackLanguages) {

    if (metadata == null) {
      return null;
    }
    SPSSODescriptor descriptor = metadata.getSPSSODescriptor(SAMLConstants.SAML20P_NS);
    if (descriptor == null) {
      return null;
    }
    UIInfo uiInfo = MetadataUtils.getMetadataExtension(descriptor.getExtensions(), UIInfo.class).orElse(null);
    if (uiInfo == null) {
      return null;
    }

    SpInfo spInfo = new SpInfo();

    spInfo.setDisplayName(getLocalizedName(uiInfo.getDisplayNames(), language, fallbackLanguages));
    if (spInfo.getDisplayName() == null) {
      // OK, it seems like the SP did not specify a UIInfo. Pick the name from the organization element instead.
      //
      if (metadata.getOrganization() != null) {
        spInfo.setDisplayName(getLocalizedName(metadata.getOrganization().getDisplayNames(), language, fallbackLanguages));
      }
    }
    spInfo.setDescription(getLocalizedName(uiInfo.getDescriptions(), language, fallbackLanguages));

    // Try to find something larger than 80px and less than 120px first
    //
    Logo small = null;
    Logo large = null;
    for (Logo logo : uiInfo.getLogos()) {
      if (logo.getHeight() == null) {
        continue;
      }
      if (logo.getHeight() > 80 && logo.getHeight() < 120) {
        spInfo.setDefaultLogoUrl(logo.getURL());
        break;
      }
      else if (logo.getHeight() < 80) {
        if (small == null || (small != null && logo.getHeight() > small.getHeight())) {
          small = logo;
        }
      }
      else if (logo.getHeight() > 120) {
        if (large == null || (large != null && logo.getHeight() < large.getHeight())) {
          large = logo;
        }
      }
    }
    if (spInfo.getDefaultLogoUrl() == null) {
      if (large != null) {
        spInfo.setDefaultLogoUrl(large.getURL());
      }
      else if (small != null) {
        spInfo.setDefaultLogoUrl(small.getURL());
      }
      else if (!uiInfo.getLogos().isEmpty()) {
        spInfo.setDefaultLogoUrl(uiInfo.getLogos().get(0).getURL());
      }
    }

    return spInfo;
  }

  private static <T extends LocalizedName> String getLocalizedName(List<T> names, String language, List<String> fallbackLanguages) {
    if (names == null || names.isEmpty()) {
      return null;
    }

    Function<String, String> getDisplayName = (lang) -> names.stream()
      .filter(n -> lang.equals(n.getXMLLang()))
      .map(LocalizedName::getValue)
      .findFirst()
      .orElse(null);

    String displayName = getDisplayName.apply(language);
    if (displayName != null) {
      return displayName;
    }
    displayName = fallbackLanguages.stream()
      .filter(l -> !language.equals(l))
      .map(getDisplayName)
      .filter(Objects::nonNull)
      .findFirst()
      .orElse(null);
    
    if (displayName != null) {
      return displayName;
    }
    // OK, then just pick the first.
    return names.get(0).getValue();
  }

}
