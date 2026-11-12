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
package se.swedenconnect.eidas.connector.config.tomcat;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * Configuration properties for Tomcat AJP.
 *
 * @author Martin Lindström
 * @author Felix Hellman
 */
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
  private @Nullable String secret;

  /**
   * Is AJP secret required?
   */
  private boolean secretRequired = false;

  /**
   * Gets whether AJP is enabled.
   *
   * @return whether AJP is enabled
   */
  public boolean isEnabled() {
    return this.enabled;
  }

  /**
   * Assigns whether AJP is enabled.
   *
   * @param enabled whether AJP is enabled
   */
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Gets the Tomcat AJP port.
   *
   * @return the Tomcat AJP port
   */
  public int getPort() {
    return this.port;
  }

  /**
   * Assigns the Tomcat AJP port.
   *
   * @param port the Tomcat AJP port
   */
  public void setPort(final int port) {
    this.port = port;
  }

  /**
   * Gets the AJP secret.
   *
   * @return the AJP secret
   */
  public @Nullable String getSecret() {
    return this.secret;
  }

  /**
   * Assigns the AJP secret.
   *
   * @param secret the AJP secret
   */
  public void setSecret(final @Nullable String secret) {
    this.secret = secret;
  }

  /**
   * Gets whether the AJP secret is required.
   *
   * @return whether the AJP secret is required
   */
  public boolean isSecretRequired() {
    return this.secretRequired;
  }

  /**
   * Assigns whether the AJP secret is required.
   *
   * @param secretRequired whether the AJP secret is required
   */
  public void setSecretRequired(final boolean secretRequired) {
    this.secretRequired = secretRequired;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() {
    if (this.enabled && this.secretRequired) {
      Assert.hasText(this.secret, "server.tomcat.ajp.secret must be assigned since secret-required is set");
    }
  }

}
