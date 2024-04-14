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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import com.fasterxml.jackson.databind.ObjectMapper;

import se.swedenconnect.eidas.connector.config.ConnectorCredentials;
import se.swedenconnect.security.credential.ReloadablePkiCredential;

/**
 * Test cases for Pkcs11HealthIndicator.
 *
 * @author Martin Lindström
 */
public class Pkcs11HealthIndicatorTest {

  private static ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testSoftwareCredentials() {
    final ConnectorCredentials connectorCredentials = Mockito.mock(ConnectorCredentials.class);
    Mockito.when(connectorCredentials.getHardwareCredentials()).thenReturn(Collections.emptyList());

    final Pkcs11HealthIndicator indicator = new Pkcs11HealthIndicator(connectorCredentials);
    final Health health = indicator.health();
    Assertions.assertEquals(Status.UP, health.getStatus());
  }

  @Test
  public void testSingleCredentialSuccess() {

    final ReloadablePkiCredential cred = Mockito.mock(ReloadablePkiCredential.class);
    Mockito.when(cred.getName()).thenReturn("test-credential");
    Mockito.when(cred.isHardwareCredential()).thenReturn(true);
    Mockito.when(cred.getTestFunction()).thenReturn(() -> null);

    final ConnectorCredentials connectorCredentials = Mockito.mock(ConnectorCredentials.class);
    Mockito.when(connectorCredentials.getHardwareCredentials()).thenReturn(List.of(cred));

    final Pkcs11HealthIndicator indicator = new Pkcs11HealthIndicator(connectorCredentials);
    final Health health = indicator.health();
    Assertions.assertEquals(Status.UP, health.getStatus());
    Assertions.assertNotNull(health.getDetails().get("test-status"));
    final Map<String, Object> status = toMap(health.getDetails().get("test-status"));
    Assertions.assertEquals("success", status.get("result"));
    Assertions.assertEquals(cred.getName(), status.get("credential-name"));
  }

  @Test
  public void testReloadSuccess() throws Exception {
    final ReloadablePkiCredential cred = Mockito.mock(ReloadablePkiCredential.class);
    Mockito.when(cred.getName()).thenReturn("test-credential");
    Mockito.when(cred.isHardwareCredential()).thenReturn(true);

    Mockito.when(cred.getTestFunction()).thenAnswer(new Answer<Supplier<Exception>>() {

      private int calls = 0;

      @Override
      public Supplier<Exception> answer(InvocationOnMock invocation) throws Throwable {
        if (this.calls < 2) {
          this.calls++;
          return () -> new SecurityException("Connection lost");
        }
        else {
          return () -> null;
        }
      }

    });
    Mockito.doNothing().when(cred).reload();

    final ConnectorCredentials connectorCredentials = Mockito.mock(ConnectorCredentials.class);
    Mockito.when(connectorCredentials.getHardwareCredentials()).thenReturn(List.of(cred));

    final Pkcs11HealthIndicator indicator = new Pkcs11HealthIndicator(connectorCredentials);
    final Health health = indicator.health();
    Assertions.assertEquals(CustomStatus.WARNING, health.getStatus());

    Assertions.assertNotNull(health.getDetails().get("test-status"));
    final Map<String, Object> status = toMap(health.getDetails().get("test-status"));
    Assertions.assertEquals("failed", status.get("result"));
    Assertions.assertEquals(cred.getName(), status.get("credential-name"));
    Assertions.assertEquals("Connection lost", status.get("error-msg"));
    Assertions.assertEquals(SecurityException.class.getName(), status.get("exception"));

    Assertions.assertNotNull(health.getDetails().get("reload-status"));
    final Map<String, Object> rstatus = toMap(health.getDetails().get("reload-status"));
    Assertions.assertEquals("success", rstatus.get("result"));
    Assertions.assertEquals(cred.getName(), rstatus.get("credential-name"));
  }

  @Test
  public void testReloadFailed() throws Exception {
    final ReloadablePkiCredential cred = Mockito.mock(ReloadablePkiCredential.class);
    Mockito.when(cred.getName()).thenReturn("test-credential");
    Mockito.when(cred.isHardwareCredential()).thenReturn(true);
    Mockito.when(cred.getTestFunction()).thenReturn(() -> new SecurityException("Connection lost"));
    Mockito.doThrow(new SecurityException("Reload failed")).when(cred).reload();

    final ConnectorCredentials connectorCredentials = Mockito.mock(ConnectorCredentials.class);
    Mockito.when(connectorCredentials.getHardwareCredentials()).thenReturn(List.of(cred));

    final Pkcs11HealthIndicator indicator = new Pkcs11HealthIndicator(connectorCredentials);
    final Health health = indicator.health();
    Assertions.assertEquals(Status.DOWN, health.getStatus());

    Assertions.assertNotNull(health.getDetails().get("test-status"));
    final Map<String, Object> status = toMap(health.getDetails().get("test-status"));
    Assertions.assertEquals("failed", status.get("result"));
    Assertions.assertEquals(cred.getName(), status.get("credential-name"));
    Assertions.assertEquals("Connection lost", status.get("error-msg"));
    Assertions.assertEquals(SecurityException.class.getName(), status.get("exception"));

    Assertions.assertNotNull(health.getDetails().get("reload-status"));
    final Map<String, Object> rstatus = toMap(health.getDetails().get("reload-status"));
    Assertions.assertEquals("failed", rstatus.get("result"));
    Assertions.assertEquals(cred.getName(), rstatus.get("credential-name"));
    Assertions.assertEquals("Reload failed", rstatus.get("error-msg"));
    Assertions.assertEquals(SecurityException.class.getName(), rstatus.get("exception"));
  }

  @Test
  public void testTestFailedAfterReload() throws Exception {
    final ReloadablePkiCredential cred = Mockito.mock(ReloadablePkiCredential.class);
    Mockito.when(cred.getName()).thenReturn("test-credential");
    Mockito.when(cred.isHardwareCredential()).thenReturn(true);
    Mockito.when(cred.getTestFunction()).thenReturn(() -> new SecurityException("Connection lost"));
    Mockito.doNothing().when(cred).reload();

    final ConnectorCredentials connectorCredentials = Mockito.mock(ConnectorCredentials.class);
    Mockito.when(connectorCredentials.getHardwareCredentials()).thenReturn(List.of(cred));

    final Pkcs11HealthIndicator indicator = new Pkcs11HealthIndicator(connectorCredentials);
    final Health health = indicator.health();
    Assertions.assertEquals(Status.DOWN, health.getStatus());

    Assertions.assertNotNull(health.getDetails().get("test-status"));
    final Map<String, Object> status = toMap(health.getDetails().get("test-status"));
    Assertions.assertEquals("failed", status.get("result"));
    Assertions.assertEquals(cred.getName(), status.get("credential-name"));
    Assertions.assertEquals("Connection lost", status.get("error-msg"));
    Assertions.assertEquals(SecurityException.class.getName(), status.get("exception"));

    Assertions.assertNotNull(health.getDetails().get("reload-status"));
    final Map<String, Object> rstatus = toMap(health.getDetails().get("reload-status"));
    Assertions.assertEquals("failed", rstatus.get("result"));
    Assertions.assertEquals(cred.getName(), rstatus.get("credential-name"));
    Assertions.assertEquals("Connection lost", rstatus.get("error-msg"));
    Assertions.assertEquals(SecurityException.class.getName(), rstatus.get("exception"));
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> toMap(final Object object) {
    try {
      final String json = mapper.writeValueAsString(object);
      return mapper.readValue(json, Map.class);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
