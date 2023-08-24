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
package se.swedenconnect.eidas.connector.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;

import lombok.extern.slf4j.Slf4j;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;
import net.shibboleth.utilities.java.support.httpclient.HttpClientSupport;
import net.shibboleth.utilities.java.support.httpclient.TLSSocketFactoryBuilder;
import se.swedenconnect.eidas.connector.config.ConnectorConfigurationProperties.EuMetadataConfiguration;
import se.swedenconnect.opensaml.saml2.metadata.provider.AbstractMetadataProvider;
import se.swedenconnect.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import se.swedenconnect.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import se.swedenconnect.opensaml.saml2.metadata.provider.MetadataProvider;
import se.swedenconnect.opensaml.saml2.metadata.provider.StaticMetadataProvider;

/**
 * Utility methods for handling metadata providers.
 *
 * @author Martin Lindström
 */
@Slf4j
public class MetadataProviderUtils {

  /**
   * Based on an {@link EuMetadataConfiguration} a
   *
   * @param config configuration
   * @return a {@link MetadataResolver}
   * @throws Exception for setup errors
   */
  public static MetadataProvider createMetadataProvider(final EuMetadataConfiguration config) throws Exception {

    AbstractMetadataProvider provider;
    if (UrlResource.class.isInstance(config.getLocation())) {
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
    else if (FileSystemResource.class.isInstance(config.getLocation())) {
      provider = new FilesystemMetadataProvider(config.getLocation().getFile());
    }
    else {
      final Document doc =
          XMLObjectProviderRegistrySupport.getParserPool().parse(config.getLocation().getInputStream());
      provider = new StaticMetadataProvider(doc.getDocumentElement());
    }
    provider.setPerformSchemaValidation(false);

    return provider;
  }

  /**
   * Creates a HTTP client to use for the {@link MetadataProvider}.
   *
   * @param config the configuration
   * @return a HttpClient
   */
  private static HttpClient createHttpClient(final EuMetadataConfiguration config) {
    try {
      final List<TrustManager> managers = Arrays.asList(HttpClientSupport.buildNoTrustX509TrustManager());
      final HostnameVerifier hnv = new DefaultHostnameVerifier();

      HttpClientBuilder builder = new HttpClientBuilder();
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
