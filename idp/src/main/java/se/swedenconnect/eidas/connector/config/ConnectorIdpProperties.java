/*
 * Copyright 2023-2024 Sweden Connect
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.opensaml.sweid.saml2.authn.LevelOfAssuranceUris;

/**
 * Configuration properties for the IdP part of the eIDAS Connector.
 * 
 * @author Martin Lindström
 */
@Slf4j
public class ConnectorIdpProperties implements InitializingBean {

  /**
   * The supported LoA:s.
   */
  @Getter
  private List<String> supportedLoas = new ArrayList<>();

  /**
   * The SAML entity categories this IdP declares.
   */
  @Getter
  private List<String> entityCategories = new ArrayList<>();

  /**
   * A list of SAML entityID:s for the SP:s that are allowed to send special "eIDAS ping" authentication requests to the
   * connector. If the list is empty, no ping requests will be served.
   */
  @Getter
  private List<String> pingWhitelist = new ArrayList<>();

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    if (this.supportedLoas.isEmpty()) {
      this.supportedLoas = List.of(
          LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_LOW,
          LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_LOW_NF,
          LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL,
          LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_SUBSTANTIAL_NF,
          LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH,
          LevelOfAssuranceUris.AUTHN_CONTEXT_URI_EIDAS_HIGH_NF);
      log.debug("connector.idp.supported-loas not assigned, defaulting to {}", this.supportedLoas);
    }
  }

}
