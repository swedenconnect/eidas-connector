/*
 * Copyright 2017-2025 Sweden Connect
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
package se.swedenconnect.eidas.connector.authn.idm.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration class for the mocked IdM API.
 *
 * @author Martin Lindström
 */
@Profile("idmmock")
@Configuration
public class MockedIdmConfiguration {

  @Bean
  @Order(3)
  SecurityFilterChain mockedIdmSecurityFilterChain(final HttpSecurity http) throws Exception {
    http.authorizeHttpRequests((authorize) -> authorize
        .requestMatchers(HttpMethod.GET, "/api/v1/mrecord/**").permitAll()
        .requestMatchers(HttpMethod.HEAD, "/api/v1/mrecord/**").permitAll()
        .anyRequest().denyAll());

    return http.build();
  }

}
