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
package se.swedenconnect.eidas.connector.events;

import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;

/**
 * A publisher for eIDAS Connector events.
 *
 * @author Martin Lindström
 */
public class EidasConnectorEventPublisher {

  /** The system's event publisher. */
  private final ApplicationEventPublisher publisher;

  /**
   * Constructor.
   *
   * @param publisher the system's event publisher
   */
  public EidasConnectorEventPublisher(final ApplicationEventPublisher publisher) {
    this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
  }

  /**
   * Generic method publishing an eIDAS Connector event.
   * @param <T> the event type
   * @param event the event to publish
   */
  public <T extends AbstractEidasConnectorEvent> void publish(final T event) {
    this.publisher.publishEvent(event);
  }

}
