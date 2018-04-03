/*
 * Copyright 2017-2018 E-legitimationsnämnden
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
package se.elegnamnden.eidas.idp.connector.controller;

import java.util.List;
import java.util.Locale;

import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.Assert;

import se.elegnamnden.eidas.idp.connector.controller.model.SignMessageConsent;
import se.litsec.opensaml.saml2.attribute.AttributeUtils;
import se.litsec.swedisheid.opensaml.saml2.attribute.AttributeConstants;

/**
 * Handler class for managing the Sign Message consent view.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class SignMessageUiHandler implements InitializingBean {

  /** Fallback languages to be used in the currently selected language can not be used. */
  private List<String> fallbackLanguages;

  /**
   * Based on a ready to display message and attributes from the authentication, the method builds a
   * {@code SignMessageConsent} model object.
   * 
   * @param message
   *          the message to display
   * @param attributes
   *          the attributes
   * @param country
   *          the country
   * @param metadata
   *          the metadata for the SP
   * @return a model object
   */
  public SignMessageConsent getSignMessageConsentModel(String message, List<Attribute> attributes, String country,
      EntityDescriptor metadata) {

    Locale locale = LocaleContextHolder.getLocale();

    SignMessageConsent model = new SignMessageConsent();

    if (message != null) {
      model.setTextMessage(message);
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
