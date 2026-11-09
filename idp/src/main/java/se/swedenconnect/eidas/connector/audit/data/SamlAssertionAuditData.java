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
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AuthenticatingAuthority;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.core.SubjectLocality;
import se.swedenconnect.eidas.connector.ApplicationVersion;
import se.swedenconnect.eidas.connector.attributes.EidasAttributeConverter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Audit data for a SAML assertion.
 *
 * @author Martin Lindström
 */
@JsonInclude(Include.NON_EMPTY)
public class SamlAssertionAuditData extends ConnectorAuditData {

  @Serial
  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /** The assertion ID. */
  @JsonProperty(value = "id")
  private @Nullable String id;

  /** Holds the ID for the corresponding AuthnRequest. */
  @JsonProperty(value = "in-response-to")
  private @Nullable String inResponseTo;

  /** Whether the assertion is signed. */
  @JsonProperty(value = "is-signed")
  private boolean signed;

  /** Whether the assertion is encrypted. */
  @JsonProperty(value = "is-encrypted")
  private boolean encrypted;

  /** The issuance time for the assertion. */
  @JsonProperty(value = "issued-at")
  private @Nullable Instant issuedAt;

  /** The entityID of the issuing entity. */
  @JsonProperty(value = "issuer")
  private @Nullable String issuer;

  /** The authentication instant. */
  @JsonProperty(value = "authn-instant")
  private @Nullable Instant authnInstant;

  /** The subject's (assigned) ID. */
  @JsonProperty(value = "subject-id")
  private @Nullable String subjectId;

  /** The subject locality (IP). */
  @JsonProperty(value = "subject-locality")
  private @Nullable String subjectLocality;

  /** The LoA URI (level of assurance). */
  @JsonProperty(value = "authn-context-class-ref")
  private @Nullable String authnContextClassRef;

  /** Optional ID for authenticating authority. */
  @JsonProperty(value = "authn-authority")
  private @Nullable String authnAuthority;

  /** The SAML attributes. */
  @JsonProperty(value = "attributes")
  private @Nullable List<SamlAttribute> attributes;

  /**
   * Gets the assertion ID.
   *
   * @return the assertion ID
   */
  public @Nullable String getId() {
    return this.id;
  }

  /**
   * Assigns the assertion ID.
   *
   * @param id the assertion ID
   */
  public void setId(final @Nullable String id) {
    this.id = id;
  }

  /**
   * Gets the ID for the corresponding AuthnRequest.
   *
   * @return the ID for the corresponding AuthnRequest
   */
  public @Nullable String getInResponseTo() {
    return this.inResponseTo;
  }

  /**
   * Assigns the ID for the corresponding AuthnRequest.
   *
   * @param inResponseTo the ID for the corresponding AuthnRequest
   */
  public void setInResponseTo(final @Nullable String inResponseTo) {
    this.inResponseTo = inResponseTo;
  }

  /**
   * Gets whether the assertion is signed.
   *
   * @return whether the assertion is signed
   */
  public boolean isSigned() {
    return this.signed;
  }

  /**
   * Assigns whether the assertion is signed.
   *
   * @param signed whether the assertion is signed
   */
  public void setSigned(final boolean signed) {
    this.signed = signed;
  }

  /**
   * Gets whether the assertion is encrypted.
   *
   * @return whether the assertion is encrypted
   */
  public boolean isEncrypted() {
    return this.encrypted;
  }

  /**
   * Assigns whether the assertion is encrypted.
   *
   * @param encrypted whether the assertion is encrypted
   */
  public void setEncrypted(final boolean encrypted) {
    this.encrypted = encrypted;
  }

  /**
   * Gets the issuance time for the assertion.
   *
   * @return the issuance time for the assertion
   */
  public @Nullable Instant getIssuedAt() {
    return this.issuedAt;
  }

  /**
   * Assigns the issuance time for the assertion.
   *
   * @param issuedAt the issuance time for the assertion
   */
  public void setIssuedAt(final @Nullable Instant issuedAt) {
    this.issuedAt = issuedAt;
  }

  /**
   * Gets the entityID of the issuing entity.
   *
   * @return the entityID of the issuing entity
   */
  public @Nullable String getIssuer() {
    return this.issuer;
  }

  /**
   * Assigns the entityID of the issuing entity.
   *
   * @param issuer the entityID of the issuing entity
   */
  public void setIssuer(final @Nullable String issuer) {
    this.issuer = issuer;
  }

  /**
   * Gets the authentication instant.
   *
   * @return the authentication instant
   */
  public @Nullable Instant getAuthnInstant() {
    return this.authnInstant;
  }

  /**
   * Assigns the authentication instant.
   *
   * @param authnInstant the authentication instant
   */
  public void setAuthnInstant(final @Nullable Instant authnInstant) {
    this.authnInstant = authnInstant;
  }

  /**
   * Gets the subject's (assigned) ID.
   *
   * @return the subject's (assigned) ID
   */
  public @Nullable String getSubjectId() {
    return this.subjectId;
  }

  /**
   * Assigns the subject's (assigned) ID.
   *
   * @param subjectId the subject's (assigned) ID
   */
  public void setSubjectId(final @Nullable String subjectId) {
    this.subjectId = subjectId;
  }

  /**
   * Gets the subject locality (IP).
   *
   * @return the subject locality (IP)
   */
  public @Nullable String getSubjectLocality() {
    return this.subjectLocality;
  }

  /**
   * Assigns the subject locality (IP).
   *
   * @param subjectLocality the subject locality (IP)
   */
  public void setSubjectLocality(final @Nullable String subjectLocality) {
    this.subjectLocality = subjectLocality;
  }

  /**
   * Gets the LoA URI (level of assurance).
   *
   * @return the LoA URI (level of assurance)
   */
  public @Nullable String getAuthnContextClassRef() {
    return this.authnContextClassRef;
  }

  /**
   * Assigns the LoA URI (level of assurance).
   *
   * @param authnContextClassRef the LoA URI (level of assurance)
   */
  public void setAuthnContextClassRef(final @Nullable String authnContextClassRef) {
    this.authnContextClassRef = authnContextClassRef;
  }

  /**
   * Gets the optional ID for authenticating authority.
   *
   * @return the optional ID for authenticating authority
   */
  public @Nullable String getAuthnAuthority() {
    return this.authnAuthority;
  }

  /**
   * Assigns the optional ID for authenticating authority.
   *
   * @param authnAuthority the optional ID for authenticating authority
   */
  public void setAuthnAuthority(final @Nullable String authnAuthority) {
    this.authnAuthority = authnAuthority;
  }

  /**
   * Gets the SAML attributes.
   *
   * @return the SAML attributes
   */
  public @Nullable List<SamlAttribute> getAttributes() {
    return this.attributes;
  }

  /**
   * Assigns the SAML attributes.
   *
   * @param attributes the SAML attributes
   */
  public void setAttributes(final @Nullable List<SamlAttribute> attributes) {
    this.attributes = attributes;
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull String getName() {
    return "saml-assertion";
  }

  /**
   * Creates a {@link SamlAssertionAuditData} given an {@link Assertion}.
   *
   * @param assertion the SAML assertion
   * @param encrypted whether this assertion is encrypted (when placed in response)
   * @return a {@link SamlAssertionAuditData}, or {@code null} if no assertion is given
   */
  public static @Nullable SamlAssertionAuditData of(final @Nullable Assertion assertion, final boolean encrypted) {
    if (assertion == null) {
      return null;
    }
    final SamlAssertionAuditData data = new SamlAssertionAuditData();
    data.setId(assertion.getID());
    data.setSigned(assertion.isSigned());
    data.setEncrypted(encrypted);
    data.setIssuedAt(assertion.getIssueInstant());
    data.setIssuer(Optional.ofNullable(assertion.getIssuer()).map(Issuer::getValue).orElse(null));
    final Subject subject = assertion.getSubject();
    if (subject != null) {
      data.setSubjectId(Optional.ofNullable(subject.getNameID()).map(NameID::getValue).orElse(null));
      data.setInResponseTo(
          subject.getSubjectConfirmations().stream()
              .map(SubjectConfirmation::getSubjectConfirmationData)
              .filter(Objects::nonNull)
              .map(SubjectConfirmationData::getInResponseTo)
              .findFirst()
              .orElse(null));
    }
    final AuthnStatement authnStatement = assertion.getAuthnStatements().stream().findFirst().orElse(null);
    if (authnStatement != null) {
      data.setAuthnInstant(authnStatement.getAuthnInstant());
      data.setSubjectLocality(Optional.ofNullable(authnStatement.getSubjectLocality())
          .map(SubjectLocality::getAddress)
          .orElse(null));
      data.setAuthnContextClassRef(Optional.ofNullable(authnStatement.getAuthnContext())
          .map(AuthnContext::getAuthnContextClassRef)
          .map(AuthnContextClassRef::getURI)
          .orElse(null));
      data.setAuthnAuthority(Optional.ofNullable(authnStatement.getAuthnContext())
          .map(AuthnContext::getAuthenticatingAuthorities)
          .flatMap(a -> a.stream().map(AuthenticatingAuthority::getURI).findFirst())
          .orElse(null));
    }
    assertion.getAttributeStatements().stream().findFirst()
        .ifPresent(attributeStatement -> data.setAttributes(attributeStatement.getAttributes().stream()
            .map(a -> new SamlAttribute(a.getName(), getAttributeValue(a)))
            .toList()));
    return data;
  }

  /**
   * Gets attribute value as a string. If multivalued, the first value is read.
   *
   * @param attribute the attribute
   * @return value as a String
   */
  private static String getAttributeValue(final Attribute attribute) {
    try {
      return EidasAttributeConverter.getValues(attribute).stream().findFirst().orElse(null);
    }
    catch (final Exception e) {
      return null;
    }
  }

  /** {@inheritDoc} */
  @Override
  public @NonNull String toString() {
    return String.format(
        "id='%s', in-response-to='%s', is-signed='%s', is-encrypted='%s', issued-at='%s', issuer='%s', subject-id='%s'"
            + ", authn-instant='%s', subject-locality='%s', authn-context-class-ref='%s', authn-authority='%s', attributes=%s",
        this.id, this.inResponseTo, this.signed, this.encrypted, this.issuedAt, this.issuer, this.subjectId,
        this.authnInstant, this.subjectLocality, this.authnContextClassRef, this.authnAuthority, this.attributes);
  }

  /**
   * Representation of a SAML attribute.
   */
  @JsonInclude(Include.NON_EMPTY)
  public static class SamlAttribute implements Serializable {

    @Serial
    private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

    /** The attribute name. */
    @JsonProperty(value = "name")
    private @Nullable String name;

    /** The attribute value. */
    @JsonProperty(value = "value")
    private @Nullable String value;

    /**
     * Default constructor.
     */
    public SamlAttribute() {
    }

    /**
     * Constructor assigning all fields.
     *
     * @param name the attribute name
     * @param value the attribute value
     */
    public SamlAttribute(final @Nullable String name, final @Nullable String value) {
      this.name = name;
      this.value = value;
    }

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
     * Gets the attribute value.
     *
     * @return the attribute value
     */
    public @Nullable String getValue() {
      return this.value;
    }

    /**
     * Assigns the attribute value.
     *
     * @param value the attribute value
     */
    public void setValue(final @Nullable String value) {
      this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull String toString() {
      return String.format("%s='%s'", this.name, this.value);
    }
  }

}
