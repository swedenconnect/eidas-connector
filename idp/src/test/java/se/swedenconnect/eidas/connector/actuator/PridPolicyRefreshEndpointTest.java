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

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.opensaml.xmlsec.signature.P;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import se.swedenconnect.eidas.connector.prid.service.PridPolicy;
import se.swedenconnect.eidas.connector.prid.service.PridService;
import se.swedenconnect.eidas.connector.prid.service.PridService.PridPolicyValidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test cases for PridPolicyRefreshEndpoint.
 */
public class PridPolicyRefreshEndpointTest {

  /*
  @Test
  void refresh_withSuccess() {
    PridService pridService = Mockito.mock(PridService.class);
    final PridPolicyRefreshEndpoint pridPolicyRefreshEndpoint = new PridPolicyRefreshEndpoint(pridService);

    PridPolicy pridPolicy = Mockito.mock(PridPolicy.class);
    when(pridPolicy.getPolicy()).thenReturn()

    when(pridService.getPolicy()).thenReturn(pridPolicy);

    PridPolicyValidation mockValidation = new PridPolicyValidation();
    when(pridService.updatePolicy()).thenReturn(mockValidation);

    PridPolicyRefreshEndpoint.RefreshStatus refreshStatus = pridPolicyRefreshEndpoint.refresh();

    assertThat(refreshStatus.getStatus()).isEqualTo("OK");
    verify(pridService, times(1)).updatePolicy();
  }

  @Test
  void refresh_withErrors() {
    PridService pridService = Mockito.mock(PridService.class);
    final PridPolicyRefreshEndpoint pridPolicyRefreshEndpoint =
        new PridPolicyRefreshEndpoint(pridService);

    PridPolicyValidation mockValidation = new PridPolicyValidation();
    mockValidation.getErrors().add("Error occurred");
    when(pridService.updatePolicy()).thenReturn(mockValidation);

    PridPolicyRefreshEndpoint.RefreshStatus refreshStatus = pridPolicyRefreshEndpoint.refresh();

    assertThat(refreshStatus.getStatus()).isEqualTo("ERROR");
    assertThat(refreshStatus.getErrors()).contains("Error occurred");
    verify(pridService, times(1)).updatePolicy();
  }

   */
}