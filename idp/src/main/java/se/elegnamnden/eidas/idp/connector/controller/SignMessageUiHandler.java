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
import java.util.Locale;

import org.apache.commons.lang.StringEscapeUtils;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.Assert;

import se.elegnamnden.eidas.idp.connector.controller.model.SignMessageConsent;
import se.litsec.opensaml.saml2.attribute.AttributeUtils;
import se.litsec.swedisheid.opensaml.saml2.attribute.AttributeConstants;
import se.litsec.swedisheid.opensaml.saml2.signservice.dss.SignMessageMimeTypeEnum;

/**
 * Handler class for managing the Sign Message consent view.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class SignMessageUiHandler implements InitializingBean {

  /** Fallback languages to be used in the currently selected language can not be used. */
  private List<String> fallbackLanguages;

  public SignMessageConsent getSignMessageConsentModel(String message, SignMessageMimeTypeEnum messageType,
      List<Attribute> attributes, String country, EntityDescriptor metadata) {

    Locale locale = LocaleContextHolder.getLocale();

    SignMessageConsent model = new SignMessageConsent();

    // The sign message
    //

    // Filter to protect against XSS
    //
    if (message != null) {      
      String filteredMessage = StringEscapeUtils.escapeHtml(message);
      
      // Replace NL with <br />
      filteredMessage = filteredMessage.replaceAll("(\r\n|\n\r|\r|\n)", "<br />");
      
      // Replace tabs with &emsp;
      filteredMessage = filteredMessage.replaceAll("\t", "&emsp;");

      model.setTextMessage(filteredMessage);
    }

    // SP Info
    model.setSpInfo(SpInfoHandler.buildSpInfo(metadata, locale.getLanguage(), this.fallbackLanguages));

    // User info
    //
    SignMessageConsent.UserInfo userInfo = new SignMessageConsent.UserInfo();
    userInfo.setCountry(country);

    String givenName = null;
    String surName = null;

    for (Attribute attr : attributes) {
      if (AttributeConstants.ATTRIBUTE_NAME_PERSONAL_IDENTITY_NUMBER.equals(attr.getName())) {
        userInfo.setSwedishId(AttributeUtils.getAttributeStringValue(attr));
      }
      else if (AttributeConstants.ATTRIBUTE_NAME_GIVEN_NAME.equals(attr.getName())) {
        givenName = AttributeUtils.getAttributeStringValue(attr);
      }
      else if (AttributeConstants.ATTRIBUTE_NAME_SN.equals(attr.getName())) {
        surName = AttributeUtils.getAttributeStringValue(attr);
      }
      else if (AttributeConstants.ATTRIBUTE_NAME_DISPLAY_NAME.equals(attr.getName())) {
        userInfo.setName(AttributeUtils.getAttributeStringValue(attr));
      }
      else if (AttributeConstants.ATTRIBUTE_NAME_DATE_OF_BIRTH.equals(attr.getName())) {
        userInfo.setDateOfBirth(AttributeUtils.getAttributeStringValue(attr));
      }
      else if (AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER.equals(attr.getName())) {
        userInfo.setInternationalId(AttributeUtils.getAttributeStringValue(attr));
      }
    }
    if (userInfo.getName() == null) {
      userInfo.setName(String.format("%s %s", givenName != null ? givenName : "", surName != null ? surName : ""));
    }
    model.setUserInfo(userInfo);

    return model;
  }

  /**
   * Assigns the fallback languages to be used in the currently selected language can not be used.
   * 
   * @param fallbackLanguages
   *          a list of country codes
   */
  public void setFallbackLanguages(List<String> fallbackLanguages) {
    this.fallbackLanguages = fallbackLanguages;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(this.fallbackLanguages, "The property 'fallbackLanguages' must be assigned");
  }

}
