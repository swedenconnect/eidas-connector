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
package se.swedenconnect.eidas.connector.config.tomcat;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * Configuration properties for Tomcat AJP.
 *
 * @author Martin Lindström
 * @author Felix Hellman
 */
@Setter
@Getter
@ConfigurationProperties("server.tomcat.ajp")
public class TomcatAjpConfigurationProperties implements InitializingBean {

  /**
   * Is AJP enabled?
   */
  private boolean enabled = false;

  /**
   * The Tomcat AJP port.
   */
  private int port = 8009;

  /**
   * AJP secret.
   */
  private String secret;

  /**
   * Is AJP secret required?
   */
  private boolean secretRequired = false;

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    if (this.enabled && !this.secretRequired) {
      Assert.hasText(this.secret, "server.tomcat.ajp.secret must be assigned since secret-required is set");
    }
  }

}
