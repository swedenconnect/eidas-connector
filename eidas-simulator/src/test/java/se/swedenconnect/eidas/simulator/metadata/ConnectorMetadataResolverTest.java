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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.swedenconnect.eidas.simulator.OpenSamlTestBase;
import se.swedenconnect.eidas.simulator.TestConnector;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for {@link ConnectorMetadataResolver}.
 *
 * @author Martin Lindström
 */
class ConnectorMetadataResolverTest extends OpenSamlTestBase {

  private static TestConnector connector;

  @TempDir
  Path tempDir;

  @BeforeAll
  static void setup() throws Exception {
    connector = new TestConnector();
  }

  @Test
  void testUnsignedMetadata() throws Exception {
    final Path file = connector.writeMetadata(this.tempDir, false);
    final ConnectorMetadataResolver resolver =
        new ConnectorMetadataResolver(file.toUri().toString(), null, Duration.ofMinutes(1));
    assertThat(resolver.getMetadata()).isNotNull();
    assertThat(resolver.getMetadata(TestConnector.ENTITY_ID)).isNotNull();
    assertThat(resolver.getMetadata("https://other.example.com")).isNull();
  }

  @Test
  void testSignatureValidation() throws Exception {
    final Path signed = connector.writeMetadata(this.tempDir, true);
    assertThat(new ConnectorMetadataResolver(signed.toUri().toString(),
        connector.getSignCredential().getCertificate(), Duration.ofMinutes(1)).getMetadata()).isNotNull();

    // Signed with another key than the one expected
    assertThat(new ConnectorMetadataResolver(signed.toUri().toString(),
        connector.getOtherCredential().getCertificate(), Duration.ofMinutes(1)).getMetadata()).isNull();

    // Not signed at all
    final Path unsigned = connector.writeMetadata(this.tempDir, false);
    assertThat(new ConnectorMetadataResolver(unsigned.toUri().toString(),
        connector.getSignCredential().getCertificate(), Duration.ofMinutes(1)).getMetadata()).isNull();
  }

  @Test
  void testNotAvailableAtFirst() throws Exception {
    // The connector is not up when the simulator starts
    final Path file = this.tempDir.resolve("later.xml");
    final ConnectorMetadataResolver resolver =
        new ConnectorMetadataResolver(file.toUri().toString(), null, Duration.ofMinutes(1));
    assertThat(resolver.getMetadata()).isNull();

    Files.copy(connector.writeMetadata(this.tempDir, false), file);
    assertThat(resolver.getMetadata()).isNotNull();
  }

  @Test
  void testRefreshAndFailedDownload() throws Exception {
    final Path file = connector.writeMetadata(this.tempDir, false);
    final ConnectorMetadataResolver resolver =
        new ConnectorMetadataResolver(file.toUri().toString(), null, Duration.ofMinutes(10));
    final Instant now = Instant.now();
    resolver.setClock(Clock.fixed(now, ZoneOffset.UTC));
    assertThat(resolver.getMetadata()).isNotNull();

    // The file disappears, the metadata already held is used after the refresh interval
    Files.delete(file);
    resolver.setClock(Clock.fixed(now.plus(Duration.ofMinutes(11)), ZoneOffset.UTC));
    assertThat(resolver.getMetadata()).isNotNull();
  }

}
