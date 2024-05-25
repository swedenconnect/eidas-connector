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
package se.swedenconnect.eidas.connector.config;

import lombok.extern.slf4j.Slf4j;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.httpclient.HttpClientBuilder;
import net.shibboleth.shared.httpclient.HttpClientSupport;
import net.shibboleth.shared.httpclient.TLSSocketFactoryBuilder;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import se.swedenconnect.eidas.connector.config.ConnectorConfigurationProperties.EuMetadataProperties;
import se.swedenconnect.opensaml.OpenSAMLInitializer;
import se.swedenconnect.opensaml.saml2.metadata.provider.AbstractMetadataProvider;
import se.swedenconnect.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import se.swedenconnect.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import se.swedenconnect.opensaml.saml2.metadata.provider.MetadataProvider;
import se.swedenconnect.opensaml.saml2.metadata.provider.StaticMetadataProvider;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.TrustManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Utility methods for handling metadata providers.
 *
 * @author Martin Lindström
 */
@Slf4j
public class MetadataProviderUtils {

  /**
   * Based on an {@link EuMetadataProperties} a
   *
   * @param config configuration
   * @return a {@link MetadataResolver}
   * @throws Exception for setup errors
   */
  public static MetadataProvider createMetadataProvider(final EuMetadataProperties config) throws Exception {

    final AbstractMetadataProvider provider;
    if (config.getLocation() instanceof final UrlResource urlResource && !urlResource.isFile()) {
      if (config.getBackupLocation() == null) {
        log.warn("No backup-location for metadata source {} - Using a backup file is strongly recommended",
            config.getLocation());
      }
      provider = new HTTPMetadataProvider(config.getLocation().getURL().toString(),
          preProcessBackupFile(config.getBackupLocation()), createHttpClient(config));

      if (config.getValidationCertificate() != null) {
        provider.setSignatureVerificationCertificate(config.getValidationCertificate());
      }
      else {
        log.warn("No validation certificate assigned for metadata source {} "
            + "- downloaded metadata can not be trusted", config.getLocation());
      }
    }
    else if (config.getLocation() instanceof FileSystemResource) {
      provider = new FilesystemMetadataProvider(config.getLocation().getFile());
    }
    else {
      final Document doc = Optional.ofNullable(XMLObjectProviderRegistrySupport.getParserPool())
          .orElseGet(() -> {
            try {
              return OpenSAMLInitializer.createDefaultParserPool();
            }
            catch (final ComponentInitializationException e) {
              throw new RuntimeException(e);
            }
          })
          .parse(config.getLocation().getInputStream());

      provider = new StaticMetadataProvider(doc.getDocumentElement());
    }
    provider.setPerformSchemaValidation(false);

    return provider;
  }

  /**
   * Creates an HTTP client to use for the {@link MetadataProvider}.
   *
   * @param config the configuration
   * @return a HttpClient
   */
  private static HttpClient createHttpClient(final EuMetadataProperties config) {
    try {
      final List<TrustManager> managers = List.of(HttpClientSupport.buildNoTrustX509TrustManager());
      final HostnameVerifier hnv = Optional.ofNullable(config.getSkipHostnameVerification())
          .map(b -> b ? NoopHostnameVerifier.INSTANCE : new DefaultHostnameVerifier())
          .orElseGet(DefaultHostnameVerifier::new);

      final HttpClientBuilder builder = new HttpClientBuilder();
      builder.setUseSystemProperties(true);
      if (config.getHttpProxy() != null) {
        if (config.getHttpProxy().getHost() == null || config.getHttpProxy().getPort() == null) {
          throw new IllegalArgumentException("Invalid HTTP proxy configuration for metadata source " +
              config.getLocation());
        }
        builder.setConnectionProxyHost(config.getHttpProxy().getHost());
        builder.setConnectionProxyPort(config.getHttpProxy().getPort());
        if (StringUtils.hasText(config.getHttpProxy().getUserName())) {
          builder.setConnectionProxyUsername(config.getHttpProxy().getUserName());
        }
        if (StringUtils.hasText(config.getHttpProxy().getPassword())) {
          builder.setConnectionProxyPassword(config.getHttpProxy().getPassword());
        }
      }
      builder.setTLSSocketFactory(new TLSSocketFactoryBuilder()
          .setHostnameVerifier(hnv)
          .setTrustManagers(managers)
          .build());

      return builder.buildClient();
    }
    catch (final Exception e) {
      throw new IllegalArgumentException("Failed to initialize HttpClient", e);
    }
  }

  /**
   * Makes sure that all parent directories for the supplied file exists and returns the backup file as an absolute
   * path.
   *
   * @param backupFile the backup file
   * @return the absolute path of the backup file
   */
  private static String preProcessBackupFile(final File backupFile) {
    if (backupFile == null) {
      return null;
    }
    preProcessBackupDirectory(backupFile.getParentFile());
    return backupFile.getAbsolutePath();
  }

  /**
   * Makes sure that all parent directories exists and returns the directory as an absolute path.
   *
   * @param backupDirectory the backup directory
   * @return the absolute path of the backup directory
   */
  private static String preProcessBackupDirectory(final File backupDirectory) {
    if (backupDirectory == null) {
      return null;
    }
    try {
      final Path path = backupDirectory.toPath();
      Files.createDirectories(path);
      return path.toFile().getAbsolutePath();
    }
    catch (final IOException e) {
      throw new IllegalArgumentException("Invalid backup-location");
    }
  }

  // Hidden ctor
  private MetadataProviderUtils() {
  }

}
