/*
 * Copyright 2017-2026 Sweden Connect
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.eidas.connector.events.BeforeEidasAuthenticationEvent;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttribute;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttributes;
import se.swedenconnect.opensaml.eidas.ext.SPType;
import se.swedenconnect.opensaml.eidas.ext.SPTypeEnumeration;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Audit event data representing an authentication request being sent to a foreign IdP.
 *
 * @author Martin Lindström
 */
public class EidasAuthnRequestAuditData extends ConnectorAuditData {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The receiving country. */
  @JsonProperty("country")
  private @Nullable String country;

  /** The ID for the AuthnRequest. */
  @JsonProperty("authn-request-id")
  private @Nullable String authnRequestId;

  /** The relay state. */
  @JsonProperty("relay-state")
  private @Nullable String relayState;

  /** The URL to which the authentication request is sent. */
  @JsonProperty("destination-url")
  private @Nullable String destinationUrl;

  /** GET or POST. */
  @JsonProperty("method")
  private @Nullable String method;

  /** The requested authentication context. */
  @JsonProperty("requested-authn-context")
  private @Nullable RequestedAuthnContextData requestedAuthnContext;

  /** The type of SP - public or private. */
  @JsonProperty("eidas-sp-type")
  private @Nullable String eidasSpType;

  /** The requested attributes. */
  @JsonProperty("requested-attributes")
  private @Nullable List<RequestedAttributeData> requestedAttributes;

  /**
   * Gets the receiving country.
   *
   * @return the receiving country
   */
  public @Nullable String getCountry() {
    return this.country;
  }

  /**
   * Assigns the receiving country.
   *
   * @param country the receiving country
   */
  public void setCountry(final @Nullable String country) {
    this.country = country;
  }

  /**
   * Gets the ID for the AuthnRequest.
   *
   * @return the ID for the AuthnRequest
   */
  public @Nullable String getAuthnRequestId() {
    return this.authnRequestId;
  }

  /**
   * Assigns the ID for the AuthnRequest.
   *
   * @param authnRequestId the ID for the AuthnRequest
   */
  public void setAuthnRequestId(final @Nullable String authnRequestId) {
    this.authnRequestId = authnRequestId;
  }

  /**
   * Gets the relay state.
   *
   * @return the relay state
   */
  public @Nullable String getRelayState() {
    return this.relayState;
  }

  /**
   * Assigns the relay state.
   *
   * @param relayState the relay state
   */
  public void setRelayState(final @Nullable String relayState) {
    this.relayState = relayState;
  }

  /**
   * Gets the URL to which the authentication request is sent.
   *
   * @return the URL to which the authentication request is sent
   */
  public @Nullable String getDestinationUrl() {
    return this.destinationUrl;
  }

  /**
   * Assigns the URL to which the authentication request is sent.
   *
   * @param destinationUrl the URL to which the authentication request is sent
   */
  public void setDestinationUrl(final @Nullable String destinationUrl) {
    this.destinationUrl = destinationUrl;
  }

  /**
   * Gets the HTTP method (GET or POST).
   *
   * @return the HTTP method (GET or POST)
   */
  public @Nullable String getMethod() {
    return this.method;
  }

  /**
   * Assigns the HTTP method (GET or POST).
   *
   * @param method the HTTP method (GET or POST)
   */
  public void setMethod(final @Nullable String method) {
    this.method = method;
  }

  /**
   * Gets the requested authentication context.
   *
   * @return the requested authentication context
   */
  public @Nullable RequestedAuthnContextData getRequestedAuthnContext() {
    return this.requestedAuthnContext;
  }

  /**
   * Assigns the requested authentication context.
   *
   * @param requestedAuthnContext the requested authentication context
   */
  public void setRequestedAuthnContext(final @Nullable RequestedAuthnContextData requestedAuthnContext) {
    this.requestedAuthnContext = requestedAuthnContext;
  }

  /**
   * Gets the type of SP (public or private).
   *
   * @return the type of SP (public or private)
   */
  public @Nullable String getEidasSpType() {
    return this.eidasSpType;
  }

  /**
   * Assigns the type of SP (public or private).
   *
   * @param eidasSpType the type of SP (public or private)
   */
  public void setEidasSpType(final @Nullable String eidasSpType) {
    this.eidasSpType = eidasSpType;
  }

  /**
   * Gets the requested attributes.
   *
   * @return the requested attributes
   */
  public @Nullable List<RequestedAttributeData> getRequestedAttributes() {
    return this.requestedAttributes;
  }

  /**
   * Assigns the requested attributes.
   *
   * @param requestedAttributes the requested attributes
   */
  public void setRequestedAttributes(final @Nullable List<RequestedAttributeData> requestedAttributes) {
    this.requestedAttributes = requestedAttributes;
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull String getName() {
    return "eidas-authn-request";
  }

  /**
   * Creates an {@link EidasAuthnRequestAuditData} given a {@link BeforeEidasAuthenticationEvent}.
   *
   * @param event the {@link BeforeEidasAuthenticationEvent}
   * @return an {@link EidasAuthnRequestAuditData}
   */
  public static @Nullable EidasAuthnRequestAuditData of(final @Nullable BeforeEidasAuthenticationEvent event) {
    if (event == null) {
      return null;
    }
    final AuthnRequest authnRequest = event.getAuthnRequest();

    final EidasAuthnRequestAuditData data = new EidasAuthnRequestAuditData();
    data.setCountry(event.getCountry());
    data.setAuthnRequestId(authnRequest.getID());
    data.setRelayState(event.getRelayState());
    data.setDestinationUrl(authnRequest.getDestination());
    data.setMethod(event.getMethod());
    data.setRequestedAuthnContext(RequestedAuthnContextData.of(authnRequest));
    data.setEidasSpType(Optional.ofNullable(authnRequest.getExtensions())
        .map(e -> e.getUnknownXMLObjects(SPType.DEFAULT_ELEMENT_NAME))
        .filter(Objects::nonNull)
        .filter(v -> !v.isEmpty())
        .map(List::getFirst)
        .map(SPType.class::cast)
        .map(SPType::getType)
        .map(SPTypeEnumeration::toString)
        .orElse(null));
    data.setRequestedAttributes(
        Optional.ofNullable(authnRequest.getExtensions())
            .map(e -> e.getUnknownXMLObjects(RequestedAttributes.DEFAULT_ELEMENT_NAME))
            .filter(Objects::nonNull)
            .filter(v -> !v.isEmpty())
            .map(List::getFirst)
            .map(RequestedAttributes.class::cast)
            .map(ras -> ras.getRequestedAttributes().stream()
                .map(RequestedAttributeData::of)
                .toList())
            .orElse(null));

    return data;
  }

  @Override
  public @NonNull String toString() {
    return String.format(
        "country='%s', authn-request-id='%s', relay-state='%s', destination-url='%s', method='%s', "
            + "requested-authn-context=[%s], eidas-sp-type='%s', requested-attributes=%s",
        this.country, this.authnRequestId, this.relayState, this.destinationUrl, this.method,
        this.requestedAuthnContext, this.eidasSpType, this.requestedAttributes);
  }

  /**
   * Represents a requested attribute.
   */
  @JsonInclude(Include.NON_NULL)
  public static class RequestedAttributeData {

    /** The attribute name. */
    @JsonProperty("name")
    private @Nullable String name;

    /** Whether the attribute is required. */
    @JsonProperty("is-required")
    private @Nullable Boolean isRequired;

    /**
     * Gets the attribute name.
     *
     * @return the attribute name
     */
    public @Nullable String getName() {
      return this.name;
    }

    /**
     * Assigns the attribute name.
     *
     * @param name the attribute name
     */
    public void setName(final @Nullable String name) {
      this.name = name;
    }

    /**
     * Gets whether the attribute is required.
     *
     * @return whether the attribute is required
     */
    public @Nullable Boolean getIsRequired() {
      return this.isRequired;
    }

    /**
     * Assigns whether the attribute is required.
     *
     * @param isRequired whether the attribute is required
     */
    public void setIsRequired(final @Nullable Boolean isRequired) {
      this.isRequired = isRequired;
    }

    /**
     * Creates a {@link RequestedAttributeData} given a {@link RequestedAttribute}.
     *
     * @param requestedAttribute the {@link RequestedAttribute}
     * @return a {@link RequestedAttributeData}
     */
    public static @NonNull RequestedAttributeData of(final @NonNull RequestedAttribute requestedAttribute) {
      final RequestedAttributeData rad = new RequestedAttributeData();
      rad.setName(requestedAttribute.getName());
      rad.setIsRequired(requestedAttribute.isRequired());
      return rad;
    }

    @Override
    public @NonNull String toString() {
      return String.format("name='%s', is-required='%s'", this.name, this.isRequired);
    }

  }

  /**
   * Represents the {@code RequestedAuthnContext} element.
   */
  @JsonInclude(Include.NON_NULL)
  public static class RequestedAuthnContextData implements Serializable {

    @Serial
    private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

    /** Comparison flag. */
    @JsonProperty("comparison")
    private @Nullable String comparison;

    /** URI:s for AuthnContextClassRef. */
    @JsonProperty("authn-context-class-refs")
    private @Nullable List<String> authnContextClassRefs;

    /**
     * Gets the comparison flag.
     *
     * @return the comparison flag
     */
    public @Nullable String getComparison() {
      return this.comparison;
    }

    /**
     * Assigns the comparison flag.
     *
     * @param comparison the comparison flag
     */
    public void setComparison(final @Nullable String comparison) {
      this.comparison = comparison;
    }

    /**
     * Gets the URI:s for AuthnContextClassRef.
     *
     * @return the URI:s for AuthnContextClassRef
     */
    public @Nullable List<String> getAuthnContextClassRefs() {
      return this.authnContextClassRefs;
    }

    /**
     * Assigns the URI:s for AuthnContextClassRef.
     *
     * @param authnContextClassRefs the URI:s for AuthnContextClassRef
     */
    public void setAuthnContextClassRefs(final @Nullable List<String> authnContextClassRefs) {
      this.authnContextClassRefs = authnContextClassRefs;
    }

    /**
     * Creates a {@link RequestedAuthnContext}.
     *
     * @param authnRequest the authentication request
     * @return a {@link RequestedAuthnContext}
     */
    public static @Nullable RequestedAuthnContextData of(final @NonNull AuthnRequest authnRequest) {
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
              .orElseGet(Collections::emptyList);
      if (!uris.isEmpty()) {
        rac.setAuthnContextClassRefs(uris);
      }
      return rac;
    }

    @Override
    public @NonNull String toString() {
      return String.format("comparison='%s', authn-context-class-refs=%s", this.comparison, this.authnContextClassRefs);
    }

  }

}
