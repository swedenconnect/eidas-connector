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
package se.swedenconnect.eidas.connector.actuator;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.eidas.connector.config.ConnectorCredentials;
import se.swedenconnect.security.credential.ReloadablePkiCredential;
import se.swedenconnect.security.credential.monitoring.DefaultCredentialMonitorBean;

/**
 * A {@link HealthIndicator} that is active if the connector has been configured with credentials that are hardware
 * based. The health indicator will assert that the connection against the HSM is still open by making a signature.
 *
 * @author Martin Lindström
 */
@Component("credential")
@Slf4j
public class Pkcs11HealthIndicator implements HealthIndicator {

  private final List<ReloadablePkiCredential> credsToMonitor;

  /**
   *
   * @param credentials
   */
  public Pkcs11HealthIndicator(final ConnectorCredentials credentials) {

    this.credsToMonitor = credentials.getHardwareCredentials();
    if (this.credsToMonitor.isEmpty()) {
      log.info("Only software based credentials are being used, {} will be inactive",
          Pkcs11HealthIndicator.class.getSimpleName());
    }
  }

  /** {@inheritDoc} */
  @Override
  public Health health() {
    if (this.credsToMonitor.isEmpty()) {
      return Health.up().build();
    }

    final DefaultCredentialMonitorBean monitor = new DefaultCredentialMonitorBean();
    monitor.setCredential(this.credsToMonitor.get(0));
    if (this.credsToMonitor.size() > 1) {
      monitor.setAdditionalForReload(this.credsToMonitor.subList(1, this.credsToMonitor.size()));
    }

    final Health.Builder builder = new Health.Builder();
    builder.up()
      .withDetail("test-status", TestStatus.builder()
          .result("success")
          .credentialName(this.credsToMonitor.get(0).getName())
          .build());

    final BiFunction<ReloadablePkiCredential, Exception, Boolean> failureCallback = (c, e) -> {
      builder
        .status(CustomStatus.WARNING)
        .withDetail("test-status", TestStatus.builder()
          .result("failed")
          .credentialName(c.getName())
          .errorMsg(e.getMessage())
          .exception(e.getClass().getName())
          .build());

      return Boolean.TRUE;
    };

    final Consumer<ReloadablePkiCredential> reloadSuccessCallback = (c) -> {
      builder.withDetail("reload-status", ReloadStatus.builder()
          .result("success")
          .credentialName(c.getName())
          .build());
    };

    final BiConsumer<ReloadablePkiCredential, Exception> reloadFailureCallback = (c, e) -> {
      builder
        .down()
        .withDetail("reload-status", ReloadStatus.builder()
            .result("failed")
            .credentialName(c.getName())
            .errorMsg(e.getMessage())
            .exception(e.getClass().getName())
            .build());

    };

    monitor.setFailureCallback(failureCallback);
    monitor.setReloadSuccessCallback(reloadSuccessCallback);
    monitor.setReloadFailureCallback(reloadFailureCallback);
    try {
      monitor.afterPropertiesSet();
    }
    catch (final Exception e) {
      log.error("Failed to perform monitoring of hardware credentials", e);
      return Health.down(e).build();
    }

    monitor.test();

    return builder.build();
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public static class TestStatus {

    @JsonProperty("result")
    private String result;

    @JsonProperty("credential-name")
    private String credentialName;

    @JsonProperty("error-msg")
    private String errorMsg;

    @JsonProperty("exception")
    private String exception;

  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public static class ReloadStatus {

    @JsonProperty("result")
    private String result;

    @JsonProperty("credential-name")
    private String credentialName;

    @JsonProperty("error-msg")
    private String errorMsg;

    @JsonProperty("exception")
    private String exception;

  }

}
