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
package se.swedenconnect.eidas.simulator.metadata;

import net.shibboleth.shared.xml.XMLParserException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Downloads the eIDAS SP metadata of the connector and keeps it for a while.
 * <p>
 * The connector and the simulator need each other's metadata, so the simulator cannot require the connector
 * metadata when it starts. The metadata is downloaded when it is first needed, and again when the refresh interval
 * has passed. If a download fails, the metadata already held is used as long as it is valid.
 * </p>
 *
 * @author Martin Lindström
 */
public class ConnectorMetadataResolver {

  /** Logger. */
  private static final Logger log = LoggerFactory.getLogger(ConnectorMetadataResolver.class);

  /** The metadata location, an URL or a Spring resource string. */
  private final @NonNull String location;

  /** The certificate that validates the metadata signature, or {@code null}. */
  private final @Nullable X509Certificate validationCertificate;

  /** How often the metadata is downloaded again. */
  private final @NonNull Duration refreshInterval;

  /** For loading the metadata. */
  private final DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

  /** The clock, for testing. */
  private @NonNull Clock clock = Clock.systemUTC();

  /** The metadata, once downloaded. */
  private @Nullable EntityDescriptor metadata;

  /** When the metadata was downloaded. */
  private @Nullable Instant downloaded;

  /**
   * Constructor.
   *
   * @param location the metadata location, an URL or a Spring resource string
   * @param validationCertificate the certificate that validates the metadata signature, or {@code null} for no
   *     validation
   * @param refreshInterval how often the metadata is downloaded again
   */
  public ConnectorMetadataResolver(final @NonNull String location,
      final @Nullable X509Certificate validationCertificate, final @NonNull Duration refreshInterval) {
    this.location = Objects.requireNonNull(location, "location must not be null");
    this.validationCertificate = validationCertificate;
    this.refreshInterval = Objects.requireNonNull(refreshInterval, "refreshInterval must not be null");
  }

  /**
   * Gets the connector metadata, downloading it if it is not held or has become too old.
   *
   * @return the metadata, or {@code null} if it is not available
   */
  public synchronized @Nullable EntityDescriptor getMetadata() {
    final Instant now = this.clock.instant();
    if (this.metadata == null || this.downloaded == null
        || now.isAfter(this.downloaded.plus(this.refreshInterval))) {
      this.refresh();
    }
    if (this.metadata != null && this.metadata.getValidUntil() != null
        && now.isAfter(this.metadata.getValidUntil())) {
      log.warn("Connector metadata from {} has expired", this.location);
      return null;
    }
    return this.metadata;
  }

  /**
   * Gets the connector metadata if the connector has the given entityID. If the metadata held is for another
   * entityID, it is downloaded again, since the connector may have been reconfigured.
   *
   * @param entityId the entityID
   * @return the metadata, or {@code null} if the connector does not have the entityID or the metadata is not
   *     available
   */
  public synchronized @Nullable EntityDescriptor getMetadata(final @NonNull String entityId) {
    EntityDescriptor md = this.getMetadata();
    if (md != null && entityId.equals(md.getEntityID())) {
      return md;
    }
    this.refresh();
    md = this.getMetadata();
    return md != null && entityId.equals(md.getEntityID()) ? md : null;
  }

  /**
   * Downloads the metadata. On failure, the metadata already held is kept.
   */
  private void refresh() {
    try {
      final Resource resource = this.resourceLoader.getResource(this.location);
      final XMLObject object;
      try (final InputStream in = resource.getInputStream()) {
        object = XMLObjectSupport.unmarshallFromInputStream(
            Objects.requireNonNull(XMLObjectProviderRegistrySupport.getParserPool()), in);
      }
      final EntityDescriptor descriptor = this.findSpDescriptor(object);
      if (descriptor == null) {
        log.warn("Connector metadata from {} holds no SP entity", this.location);
        return;
      }
      if (this.validationCertificate != null) {
        this.validateSignature((SignableSAMLObject) object);
      }
      this.metadata = descriptor;
      this.downloaded = this.clock.instant();
      log.debug("Connector metadata for '{}' downloaded from {}", descriptor.getEntityID(), this.location);
    }
    catch (final IOException | XMLParserException | UnmarshallingException | SignatureException e) {
      log.warn("Failed to download connector metadata from {} - {}", this.location, e.getMessage());
      log.debug("", e);
    }
  }

  /**
   * Finds the SP entity of the metadata.
   *
   * @param object the metadata
   * @return the SP entity, or {@code null}
   */
  private @Nullable EntityDescriptor findSpDescriptor(final @NonNull XMLObject object) {
    if (object instanceof final EntityDescriptor ed) {
      return ed.getSPSSODescriptor(SAMLConstants.SAML20P_NS) != null ? ed : null;
    }
    if (object instanceof final EntitiesDescriptor eds) {
      return eds.getEntityDescriptors().stream()
          .filter(ed -> ed.getSPSSODescriptor(SAMLConstants.SAML20P_NS) != null)
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  /**
   * Validates the signature of the metadata.
   *
   * @param object the metadata
   * @throws SignatureException if the metadata is not signed or the signature is not valid
   */
  private void validateSignature(final @NonNull SignableSAMLObject object) throws SignatureException {
    if (object.getSignature() == null) {
      throw new SignatureException("Connector metadata is not signed");
    }
    new SAMLSignatureProfileValidator().validate(object.getSignature());
    SignatureValidator.validate(object.getSignature(),
        new BasicX509Credential(Objects.requireNonNull(this.validationCertificate)));
  }

  /**
   * Assigns the clock. For testing.
   *
   * @param clock the clock
   */
  void setClock(final @NonNull Clock clock) {
    this.clock = clock;
  }

}
