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
package se.swedenconnect.eidas.connector.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.security.config.Customizer;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import se.swedenconnect.spring.authnserver.audit.events.AuthnSuccessResponseEvent;

import java.util.Objects;
import java.util.Optional;

/**
 * Publishes the {@link EidasSuccessResponseAuditEvent} when the authentication server has sent a successful SAML
 * response to the Swedish SP.
 * <p>
 * The assertion and the response are picked up by the customizers that the authentication server calls when it builds
 * them, and the event is published when the server reports the response.
 * </p>
 *
 * @author Martin Lindström
 */
public class EidasSuccessResponseAuditPublisher {

  /** Request attribute holding the assertion of the response being built. */
  private static final String ASSERTION_ATTRIBUTE = EidasSuccessResponseAuditPublisher.class.getName() + ".Assertion";

  /** Request attribute holding the response being built. */
  private static final String RESPONSE_ATTRIBUTE = EidasSuccessResponseAuditPublisher.class.getName() + ".Response";

  /** The event publisher. */
  private final @NonNull ApplicationEventPublisher eventPublisher;

  /**
   * Constructor.
   *
   * @param eventPublisher the event publisher
   */
  public EidasSuccessResponseAuditPublisher(final @NonNull ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
  }

  /**
   * Gets a customizer that picks up the assertion before it is signed and encrypted.
   *
   * @return a {@link Customizer} for assertions
   */
  public @NonNull Customizer<Assertion> assertionCustomizer() {
    return assertion -> getCurrentRequest().ifPresent(r -> r.setAttribute(ASSERTION_ATTRIBUTE, assertion));
  }

  /**
   * Gets a customizer that picks up the response before it is signed.
   *
   * @return a {@link Customizer} for responses
   */
  public @NonNull Customizer<Response> responseCustomizer() {
    return response -> getCurrentRequest().ifPresent(r -> r.setAttribute(RESPONSE_ATTRIBUTE, response));
  }

  /**
   * Publishes the {@link EidasSuccessResponseAuditEvent} for a successful response.
   *
   * @param event the event from the authentication server
   */
  @EventListener
  public void onSuccessResponse(final @NonNull AuthnSuccessResponseEvent event) {
    final HttpServletRequest request = getCurrentRequest().orElse(null);
    if (request == null) {
      return;
    }
    final Object assertion = request.getAttribute(ASSERTION_ATTRIBUTE);
    final Object response = request.getAttribute(RESPONSE_ATTRIBUTE);
    request.removeAttribute(ASSERTION_ATTRIBUTE);
    request.removeAttribute(RESPONSE_ATTRIBUTE);

    if (assertion instanceof final Assertion a && response instanceof final Response r) {
      this.eventPublisher.publishEvent(new EidasSuccessResponseAuditEvent(
          Optional.ofNullable(event.getRequester().identifier()).orElse(event.getRequester().getPrincipal()), r, a));
    }
  }

  /**
   * Gets the HTTP servlet request that is being processed.
   *
   * @return the request, or an empty {@link Optional}
   */
  private static Optional<HttpServletRequest> getCurrentRequest() {
    return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
        .filter(ServletRequestAttributes.class::isInstance)
        .map(ServletRequestAttributes.class::cast)
        .map(ServletRequestAttributes::getRequest);
  }

}
