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
package se.elegnamnden.eidas.idp.connector.controller.model;

import java.util.List;

import org.opensaml.saml.saml2.core.Attribute;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import se.litsec.opensaml.saml2.attribute.AttributeUtils;
import se.litsec.swedisheid.opensaml.saml2.attribute.AttributeConstants;

/**
 * Model for a SignMessage consent.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
@Data
@NoArgsConstructor
@ToString
public class SignMessageConsent {
  
  private UserInfo userInfo;
  
  public SignMessageConsent(List<Attribute> attributes, String country) {
    this.userInfo = new UserInfo(attributes, country);
  }
  
  @Data
  @NoArgsConstructor
  @ToString
  public static class UserInfo {
    private String name;
    private String swedishId;
    private String internationalId;
    private String dateOfBirth;
    private String country;
    
    public UserInfo(List<Attribute> attributes, String country) {
      this.country = country;
      
      String givenName = null;
      String surName = null;
      
      for (Attribute attr : attributes) {
        if (AttributeConstants.ATTRIBUTE_NAME_PERSONAL_IDENTITY_NUMBER.equals(attr.getName())) {
          this.swedishId = AttributeUtils.getAttributeStringValue(attr);
        }
        else if (AttributeConstants.ATTRIBUTE_NAME_GIVEN_NAME.equals(attr.getName())) {
          givenName = AttributeUtils.getAttributeStringValue(attr);
        }
        else if (AttributeConstants.ATTRIBUTE_NAME_SN.equals(attr.getName())) {
          surName = AttributeUtils.getAttributeStringValue(attr);
        }
        else if (AttributeConstants.ATTRIBUTE_NAME_DISPLAY_NAME.equals(attr.getName())) {
          this.name = AttributeUtils.getAttributeStringValue(attr);
        }
        else if (AttributeConstants.ATTRIBUTE_NAME_DATE_OF_BIRTH.equals(attr.getName())) {
          this.dateOfBirth = AttributeUtils.getAttributeStringValue(attr);
        }
        else if (AttributeConstants.ATTRIBUTE_NAME_EIDAS_PERSON_IDENTIFIER.equals(attr.getName())) {
          this.internationalId = AttributeUtils.getAttributeStringValue(attr);
        }
      }
      if (this.name == null) {
        this.name = String.format("%s %s", givenName != null ? givenName : "", surName != null ? surName : "");
      }
    }
    
  }

}
