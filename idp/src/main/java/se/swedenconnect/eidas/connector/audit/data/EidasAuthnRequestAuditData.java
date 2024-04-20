/*
 * Copyright 2017-2024 Sweden Connect
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
package se.swedenconnect.eidas.connector.audit.data;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.eidas.connector.events.BeforeEidasAuthenticationEvent;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttribute;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttributes;
import se.swedenconnect.opensaml.eidas.ext.SPType;
import se.swedenconnect.opensaml.eidas.ext.SPTypeEnumeration;

/**
 * Audit event data representing an authentication request being sent to a foreign IdP.
 *
 * @author Martin Lindström
 */
@JsonInclude(Include.NON_NULL)
public class EidasAuthnRequestAuditData extends ConnectorAuditData {

  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The receiving country. */
  @Getter
  @Setter
  @JsonProperty("country")
  private String country;

  /** The URL to which the authentication request is sent. */
  @Getter
  @Setter
  @JsonProperty("destination-url")
  private String destinationUrl;

  /** GET or POST. */
  @Getter
  @Setter
  @JsonProperty("method")
  private String method;

  /** The requested authentication context. */
  @Getter
  @Setter
  @JsonProperty("requested-authn-context")
  private RequestedAuthnContextData requestedAuthnContext;

  /** The type of SP - public or private. */
  @Getter
  @Setter
  @JsonProperty("eidas-sp-type")
  private String eidasSpType;

  /** The requested attributes. */
  @Getter
  @Setter
  @JsonProperty("requested-attributes")
  private List<RequestedAttributeData> requestedAttributes;

  /** {@inheritDoc} */
  @Override
  public String getName() {
    return "eidas-authn-request";
  }

  /**
   * Creates an {@link EidasAuthnRequestAuditData} given a {@link BeforeEidasAuthenticationEvent}.
   *
   * @param event the {@link BeforeEidasAuthenticationEvent}
   * @return an {@link EidasAuthnRequestAuditData}
   */
  public static EidasAuthnRequestAuditData of(final BeforeEidasAuthenticationEvent event) {
    if (event == null) {
      return null;
    }
    final AuthnRequest authnRequest = event.getAuthnRequest();

    final EidasAuthnRequestAuditData data = new EidasAuthnRequestAuditData();
    data.setCountry(event.getCountry());
    data.setDestinationUrl(authnRequest.getDestination());
    data.setMethod(event.getMethod());
    data.setRequestedAuthnContext(RequestedAuthnContextData.of(authnRequest));
    data.setEidasSpType(Optional.ofNullable(authnRequest.getExtensions())
        .map(e -> e.getUnknownXMLObjects(SPType.DEFAULT_ELEMENT_NAME))
        .filter(v -> v != null)
        .filter(v -> !v.isEmpty())
        .map(v -> v.get(0))
        .map(SPType.class::cast)
        .map(SPType::getType)
        .map(SPTypeEnumeration::toString)
        .orElse(null));
    data.setRequestedAttributes(
        Optional.ofNullable(authnRequest.getExtensions())
            .map(e -> e.getUnknownXMLObjects(RequestedAttributes.DEFAULT_ELEMENT_NAME))
            .filter(v -> v != null)
            .filter(v -> !v.isEmpty())
            .map(v -> v.get(0))
            .map(RequestedAttributes.class::cast)
            .map(ras -> ras.getRequestedAttributes().stream()
                .map(ra -> RequestedAttributeData.of(ra))
                .toList())
            .orElse(null));

    return data;
  }

  /**
   * Represents a requested attribute.
   */
  @JsonInclude(Include.NON_NULL)
  public static class RequestedAttributeData {

    /** The attribute name. */
    @Getter
    @Setter
    @JsonProperty("name")
    private String name;

    /** Whether the attribute is required. */
    @Getter
    @Setter
    @JsonProperty("is-required")
    private Boolean isRequired;

    /**
     * Creates a {@link RequestedAttributeData} given a {@link RequestedAttribute}.
     *
     * @param requestedAttribute the {@link RequestedAttribute}
     * @return a {@link RequestedAttributeData}
     */
    public static RequestedAttributeData of(final RequestedAttribute requestedAttribute) {
      final RequestedAttributeData rad = new RequestedAttributeData();
      rad.setName(requestedAttribute.getName());
      rad.setIsRequired(requestedAttribute.isRequired());
      return rad;
    }

  }

  /**
   * Represents the {@code RequestedAuthnContext} element.
   */
  @JsonInclude(Include.NON_NULL)
  public static class RequestedAuthnContextData {

    /** Comparison flag. */
    @Getter
    @Setter
    @JsonProperty("comparison")
    private String comparison;

    /** URI:s for AuthnContextClassRef. */
    @Getter
    @Setter
    @JsonProperty("authn-context-class-refs")
    private List<String> authnContextClassRefs;

    /**
     * Creates a {@link RequestedAuthnContext}.
     *
     * @param authnRequest the authentication request
     * @return a {@link RequestedAuthnContext}
     */
    public static RequestedAuthnContextData of(final AuthnRequest authnRequest) {
      if (authnRequest.getRequestedAuthnContext() == null) {
        return null;
      }
      final RequestedAuthnContextData rac = new RequestedAuthnContextData();
      rac.setComparison(Optional.ofNullable(authnRequest.getRequestedAuthnContext())
          .map(RequestedAuthnContext::getComparison)
          .map(AuthnContextComparisonTypeEnumeration::toString)
          .orElse(null));

      final List<String> uris =
          Optional.ofNullable(authnRequest.getRequestedAuthnContext().getAuthnContextClassRefs())
              .map(refs -> refs.stream()
                  .map(AuthnContextClassRef::getURI)
                  .toList())
              .orElseGet(() -> Collections.emptyList());
      if (!uris.isEmpty()) {
        rac.setAuthnContextClassRefs(uris);
      }
      return rac;
    }

  }

}
