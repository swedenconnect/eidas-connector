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
package se.swedenconnect.eidas.connector.authn.sp;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import se.litsec.eidas.opensaml.ext.RequestedAttribute;
import se.litsec.eidas.opensaml.ext.SPTypeEnumeration;

/**
 * Factory for building {@link EidasAuthnRequestGeneratorContext} objects.
 * 
 * @author Martin Lindström
 */
@Component
public class EidasAuthnRequestGeneratorContextFactory {

  /** The name to add to the field {@code providerName}. */
  private final String providerName;

  /**
   * Constructor.
   * 
   * @param providerName the name to add to the field {@code providerName}.
   */
  public EidasAuthnRequestGeneratorContextFactory(final String providerName) {
    this.providerName =
        Optional.ofNullable(providerName).orElseGet(() -> EidasAuthnRequestGeneratorContext.DEFAULT_PROVIDER_NAME);
  }

  /**
   * Creates a builder for creating {@link EidasAuthnRequestGeneratorContext} objects.
   * 
   * @return {@link EidasAuthnRequestGeneratorContext} builder
   */
  public Builder builder() {
    return new Builder(this.providerName);
  }

  /**
   * A builder for creating {@link EidasAuthnRequestGeneratorContext} objects.
   */
  public static class Builder {

    /** The name to add to the field {@code providerName}. */
    private final String providerName;

    /** Country code. */
    private String country;

    /** The entityID of the national SP that requested authentication. */
    private String nationalSpEntityId;

    /** The type of SP that makes the request. */
    private SPTypeEnumeration spType;

    /** The requested attributes. */
    private List<RequestedAttribute> requestedAttributes;

    private Builder(final String providerName) {
      this.providerName = providerName;
    }

    /**
     * Builds the {@link EidasAuthnRequestGeneratorContext}.
     * 
     * @return an {@link EidasAuthnRequestGeneratorContext}
     */
    public EidasAuthnRequestGeneratorContext build() {
      return new EidasAuthnRequestGeneratorContext(this.country, this.nationalSpEntityId, this.spType,
          this.requestedAttributes, this.providerName);
    }

    /**
     * Assigns the country code.
     * 
     * @param country the country code
     * @return the builder
     */
    public Builder country(final String country) {
      this.country = country;
      return this;
    }

    /**
     * Assigns entityID of the national SP that requested authentication.
     * 
     * @param entityId SAML entityID
     * @return the builder
     */
    public Builder nationalSpEntityId(final String entityId) {
      this.nationalSpEntityId = entityId;
      return this;
    }

    /**
     * Assigns the SP type.
     * 
     * @param spType the SP type
     * @return the builder
     */
    public Builder spType(final SPTypeEnumeration spType) {
      this.spType = spType;
      return this;
    }

    /**
     * Assigns the requested SAML attributes.
     * 
     * @param requestedAttributes the requested SAML attributes
     * @return the builder
     */
    public Builder requestedAttributes(final List<RequestedAttribute> requestedAttributes) {
      this.requestedAttributes = requestedAttributes;
      return this;
    }

  }

}
