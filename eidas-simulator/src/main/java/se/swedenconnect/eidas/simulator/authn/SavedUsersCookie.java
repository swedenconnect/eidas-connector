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

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;
import se.swedenconnect.eidas.simulator.country.SimulatedUser;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Keeps the users that the tester has added in the tester's browser, in one cookie per country, so that they are
 * still there after the HTTP session has ended or the simulator has been restarted.
 * <p>
 * The users are stored as compressed JSON. A cookie holds at most {@link #MAX_USERS} users, and at most
 * {@link #MAX_VALUE_LENGTH} characters. When a new user does not fit, the oldest users are dropped.
 * </p>
 *
 * @author Martin Lindström
 */
public class SavedUsersCookie {

  /** Logger. */
  private static final Logger log = LoggerFactory.getLogger(SavedUsersCookie.class);

  /** The prefix of the cookie name. The country code, in lower case, follows it. */
  public static final @NonNull String COOKIE_NAME_PREFIX = "savedUsers-";

  /** The largest number of users that are kept for a country. */
  public static final int MAX_USERS = 20;

  /** The largest length of the cookie value, keeping the cookie below the 4096 bytes that browsers accept. */
  public static final int MAX_VALUE_LENGTH = 3800;

  /** How long the cookie is kept. */
  private static final Duration MAX_AGE = Duration.ofDays(365);

  /** Reads and writes the users as JSON. */
  private static final JsonMapper jsonMapper = JsonMapper.builder()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .changeDefaultPropertyInclusion(i -> JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY,
          JsonInclude.Include.NON_EMPTY))
      .build();

  /** The cookie path. */
  private final @NonNull String path;

  /**
   * Constructor.
   *
   * @param path the cookie path, {@code /} if empty or {@code null}
   */
  public SavedUsersCookie(final @Nullable String path) {
    this.path = StringUtils.hasText(path) ? path : "/";
  }

  /**
   * Gets the users that the tester has added for a country. A cookie that cannot be read gives an empty list.
   *
   * @param country the country code
   * @param request the HTTP request
   * @return the users, oldest first
   */
  public @NonNull List<SimulatedUser> getUsers(final @NonNull String country,
      final @NonNull HttpServletRequest request) {
    final String value = this.getValue(country, request);
    if (value == null) {
      return List.of();
    }
    try {
      final List<SimulatedUser> users =
          jsonMapper.readValue(inflate(Base64.getUrlDecoder().decode(value)), new TypeReference<>() {
          });
      return users.stream().filter(u -> StringUtils.hasText(u.getId())).toList();
    }
    catch (final IllegalArgumentException | JacksonException | DataFormatException e) {
      log.debug("Ignoring invalid saved users cookie for {}: {}", country, e.getMessage());
      return List.of();
    }
  }

  /**
   * Saves a user that the tester has added, by writing the cookie of the country again. A user with the same identity
   * is replaced. When the cookie is full, the oldest users are dropped.
   *
   * @param country the country code
   * @param user the user to save
   * @param request the HTTP request
   * @param response the HTTP response
   */
  public void saveUser(final @NonNull String country, final @NonNull SimulatedUser user,
      final @NonNull HttpServletRequest request, final @NonNull HttpServletResponse response) {
    Objects.requireNonNull(user.getId(), "user must have an id");
    final List<SimulatedUser> users = new ArrayList<>(this.getUsers(country, request));
    users.removeIf(u -> user.getId().equals(u.getId()));
    users.add(user);
    while (users.size() > MAX_USERS) {
      users.removeFirst();
    }
    String value = encode(users);
    while (value.length() > MAX_VALUE_LENGTH && users.size() > 1) {
      users.removeFirst();
      value = encode(users);
    }
    if (value.length() > MAX_VALUE_LENGTH) {
      log.info("{}: User '{}' is too large to be saved in a cookie", country, user.getDisplayName());
      return;
    }
    response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(getCookieName(country), value)
        .path(this.path)
        .maxAge(MAX_AGE)
        .httpOnly(true)
        .secure(true)
        .sameSite("Lax")
        .build()
        .toString());
  }

  /**
   * Gets the name of the cookie of a country.
   *
   * @param country the country code
   * @return the cookie name
   */
  public static @NonNull String getCookieName(final @NonNull String country) {
    return COOKIE_NAME_PREFIX + country.toLowerCase(Locale.ROOT);
  }

  /**
   * Gets the value of the cookie of a country.
   *
   * @param country the country code
   * @param request the HTTP request
   * @return the value, or {@code null} if the request does not carry the cookie
   */
  private @Nullable String getValue(final @NonNull String country, final @NonNull HttpServletRequest request) {
    final Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    final String name = getCookieName(country);
    for (final Cookie cookie : cookies) {
      if (name.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
        return cookie.getValue();
      }
    }
    return null;
  }

  /**
   * Encodes users as compressed JSON in Base64url.
   *
   * @param users the users
   * @return the encoding
   */
  private static @NonNull String encode(final @NonNull List<SimulatedUser> users) {
    final byte[] json = jsonMapper.writeValueAsBytes(users);
    final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
    try {
      deflater.setInput(json);
      deflater.finish();
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final byte[] buffer = new byte[1024];
      while (!deflater.finished()) {
        out.write(buffer, 0, deflater.deflate(buffer));
      }
      return Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
    }
    finally {
      deflater.end();
    }
  }

  /**
   * Inflates compressed data.
   *
   * @param data the compressed data
   * @return the inflated data
   * @throws DataFormatException if the data is not valid
   */
  private static byte @NonNull [] inflate(final byte @NonNull [] data) throws DataFormatException {
    final Inflater inflater = new Inflater();
    try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      inflater.setInput(data);
      final byte[] buffer = new byte[1024];
      while (!inflater.finished()) {
        final int n = inflater.inflate(buffer);
        if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
          throw new DataFormatException("Truncated data");
        }
        out.write(buffer, 0, n);
        if (out.size() > 64 * 1024) {
          throw new DataFormatException("Data too large");
        }
      }
      return out.toByteArray();
    }
    catch (final IOException e) {
      throw new DataFormatException(e.getMessage());
    }
    finally {
      inflater.end();
    }
  }

}
