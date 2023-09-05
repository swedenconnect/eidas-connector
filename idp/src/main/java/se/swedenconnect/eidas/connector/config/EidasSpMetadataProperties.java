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

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import se.swedenconnect.spring.saml.idp.autoconfigure.settings.MetadataConfigurationProperties;

/**
 * Configuration properties for the eIDAS SP metadata.
 *
 * @author Martin Lindström
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EidasSpMetadataProperties extends MetadataConfigurationProperties implements InitializingBean {

  /**
   * The value to insert for the eIDAS entity category
   * {@code http://eidas.europa.eu/entity-attributes/application-identifier}. The current version of the
   * connector will always be appended to this value.
   */
  private String applicationIdentifierPrefix;

  /**
   * The values to use for the eIDAS entity category {@code http://eidas.europa.eu/entity-attributes/protocol-version}.
   */
  private List<String> protocolVersions;

  /**
   * The node country extension to include. Defaults to SE.
   */
  private String nodeCountry;

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    if (!StringUtils.hasText(this.applicationIdentifierPrefix)) {
      this.applicationIdentifierPrefix = "SE:connector:";
    }
    if (!StringUtils.hasText(this.nodeCountry)) {
      this.nodeCountry = "SE";
    }
    if (this.getCacheDuration() == null) {
      this.setCacheDuration(Duration.ofDays(1));
    }
    if (this.getValidityPeriod() == null) {
      this.setValidityPeriod(Duration.ofDays(7));
    }
  }

}
