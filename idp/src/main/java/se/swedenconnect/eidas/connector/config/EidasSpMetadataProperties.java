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

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;
import se.swedenconnect.spring.saml.idp.autoconfigure.settings.IdentityProviderConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the eIDAS SP metadata.
 *
 * @author Martin Lindström
 */
public class EidasSpMetadataProperties extends IdentityProviderConfigurationProperties.MetadataConfigurationProperties
    implements InitializingBean {

  /**
   * The value to insert for the eIDAS entity category
   * {@code http://eidas.europa.eu/entity-attributes/application-identifier}. The current version of the connector will
   * always be appended to this value.
   */
  @Getter
  @Setter
  private String applicationIdentifierPrefix;

  /**
   * The values to use for the eIDAS entity category {@code http://eidas.europa.eu/entity-attributes/protocol-version}.
   */
  @Getter
  private final List<String> protocolVersions = new ArrayList<>();

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() {
    if (!StringUtils.hasText(this.applicationIdentifierPrefix)) {
      this.applicationIdentifierPrefix = "SE:connector:";
    }
    if (this.getCacheDuration() == null) {
      this.setCacheDuration(Duration.ofDays(1));
    }
    if (this.getValidityPeriod() == null) {
      this.setValidityPeriod(Duration.ofDays(7));
    }
  }

}
