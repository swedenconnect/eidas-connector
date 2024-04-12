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
package se.swedenconnect.eidas.connector.events;

import org.springframework.context.ApplicationEvent;

import se.swedenconnect.eidas.connector.ApplicationVersion;

/**
 * Abstract base class for all events published by the eIDAS Connector.
 *
 * @author Martin Lindström
 */
public abstract class AbstractEidasConnectorEvent extends ApplicationEvent {

  private static final long serialVersionUID = ApplicationVersion.SERIAL_VERSION_UID;

  /**
   * Constructor.
   *
   * @param source the object with which the event is associated (never {@code null})
   */
  public AbstractEidasConnectorEvent(final Object source) {
    super(source);
  }

}
