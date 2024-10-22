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
package se.swedenconnect.eidas.connector.prid.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import se.swedenconnect.eidas.connector.prid.generator.PridGenDefaultEidas;
import se.swedenconnect.eidas.connector.prid.generator.PridGenTestEidas;

import java.util.List;

/**
 * Test cases for {@link PridService}.
 *
 * @author Martin Lindström
 */
public class PridServiceTest {

  @Test
  void testProperties() throws Exception {
    final PridService service = new PridService(new ClassPathResource("policy.properties"),
        List.of(new PridGenDefaultEidas(), new PridGenTestEidas()));
    service.afterPropertiesSet();

    Assertions.assertNotNull(service.getPolicy("SE"));
    Assertions.assertEquals(new PridResult("SE:1234567890", "A"), service.generatePrid("SE/SE/1234567890", "SE"));
    Assertions.assertNotNull(service.getPolicy("NO"));
    Assertions.assertEquals(new PridResult("NO:1234567890", "A"), service.generatePrid("NO/SE/1234567890", "NO"));
    Assertions.assertNotNull(service.getPolicy("DE"));
    Assertions.assertEquals(new PridResult("DE:1234567890", "B"), service.generatePrid("DE/SE/1234567890", "DE"));
    Assertions.assertNotNull(service.getPolicy("XA"));

    Assertions.assertNull(service.getPolicy("DK"));

    Assertions.assertFalse(service.getLatestValidationResult().hasErrors());
  }

  @Test
  void testYaml() throws Exception {
    final PridService service = new PridService(new ClassPathResource("policy.yml"),
        List.of(new PridGenDefaultEidas(), new PridGenTestEidas()));
    service.afterPropertiesSet();

    Assertions.assertNotNull(service.getPolicy("SE"));
    Assertions.assertEquals(new PridResult("SE:1234567890", "A"), service.generatePrid("SE/SE/1234567890", "SE"));
    Assertions.assertNotNull(service.getPolicy("NO"));
    Assertions.assertEquals(new PridResult("NO:1234567890", "A"), service.generatePrid("NO/SE/1234567890", "NO"));
    Assertions.assertNotNull(service.getPolicy("DE"));
    Assertions.assertEquals(new PridResult("DE:1234567890", "B"), service.generatePrid("DE/SE/1234567890", "DE"));
    Assertions.assertNotNull(service.getPolicy("XA"));

    Assertions.assertNull(service.getPolicy("DK"));

    Assertions.assertFalse(service.getLatestValidationResult().hasErrors());
  }

  @Test
  void testValidationErrors() throws Exception {
    final PridService service = new PridService(new ClassPathResource("policy-missing.properties"),
        List.of(new PridGenDefaultEidas(), new PridGenTestEidas()));
    Assertions.assertThrows(IllegalArgumentException.class, service::afterPropertiesSet);

    final PridService.PridPolicyValidation result = service.getLatestValidationResult();
    Assertions.assertTrue(result.hasErrors());
    Assertions.assertEquals(2, result.getErrors().size());

    final PridService service2 = new PridService(new ClassPathResource("policy-missing.yml"),
        List.of(new PridGenDefaultEidas(), new PridGenTestEidas()));
    Assertions.assertThrows(IllegalArgumentException.class, service2::afterPropertiesSet);

    final PridService.PridPolicyValidation result2 = service2.getLatestValidationResult();
    Assertions.assertTrue(result2.hasErrors());
    Assertions.assertEquals(2, result2.getErrors().size());
  }
}
