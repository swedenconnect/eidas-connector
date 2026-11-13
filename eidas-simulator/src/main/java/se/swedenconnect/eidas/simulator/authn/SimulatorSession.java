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
package se.swedenconnect.eidas.simulator.authn;

import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import se.swedenconnect.eidas.simulator.country.SimulatedUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The state of the simulator for one browser: the requests waiting for the tester, and the users that the tester
 * has added. It is kept in the HTTP session.
 *
 * @author Martin Lindström
 */
public class SimulatorSession {

  /** The name of the session attribute. */
  private static final @NonNull String SESSION_ATTRIBUTE = SimulatorSession.class.getName();

  /** The largest number of requests that are kept. */
  private static final int MAX_REQUESTS = 20;

  /** The requests waiting for the tester, keyed by their identity, oldest first. */
  private final Map<String, ProcessedRequest> requests = new LinkedHashMap<>();

  /** The users added by the tester, keyed by country code. */
  private final Map<String, List<SimulatedUser>> customUsers = new HashMap<>();

  /**
   * Gets the simulator state of an HTTP session, creating it if needed.
   *
   * @param session the HTTP session
   * @return the simulator state
   */
  public static @NonNull SimulatorSession get(final @NonNull HttpSession session) {
    synchronized (session) {
      SimulatorSession s = (SimulatorSession) session.getAttribute(SESSION_ATTRIBUTE);
      if (s == null) {
        s = new SimulatorSession();
        session.setAttribute(SESSION_ATTRIBUTE, s);
      }
      return s;
    }
  }

  /**
   * Adds a request. If too many requests are held, the oldest is removed.
   *
   * @param request the request
   */
  public synchronized void addRequest(final @NonNull ProcessedRequest request) {
    this.requests.put(request.id(), request);
    while (this.requests.size() > MAX_REQUESTS) {
      this.requests.remove(this.requests.keySet().iterator().next());
    }
  }

  /**
   * Gets a request.
   *
   * @param id the identity of the request
   * @return the request, or {@code null}
   */
  public synchronized @Nullable ProcessedRequest getRequest(final @NonNull String id) {
    return this.requests.get(id);
  }

  /**
   * Removes a request, once it has been answered.
   *
   * @param id the identity of the request
   */
  public synchronized void removeRequest(final @NonNull String id) {
    this.requests.remove(id);
  }

  /**
   * Gets the users that the tester has added for a country.
   *
   * @param country the country code
   * @return the users
   */
  public synchronized @NonNull List<SimulatedUser> getCustomUsers(final @NonNull String country) {
    return List.copyOf(this.customUsers.getOrDefault(country, List.of()));
  }

  /**
   * Saves a user added by the tester. A user with the same identity is replaced.
   *
   * @param country the country code
   * @param user the user
   */
  public synchronized void saveCustomUser(final @NonNull String country, final @NonNull SimulatedUser user) {
    final List<SimulatedUser> users = this.customUsers.computeIfAbsent(country, c -> new ArrayList<>());
    users.removeIf(u -> u.getId() != null && u.getId().equals(user.getId()));
    users.add(user);
  }

}
