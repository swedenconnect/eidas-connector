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

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import se.swedenconnect.eidas.simulator.country.SimulatedUser;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Test cases for {@link SavedUsersCookie}.
 *
 * @author Martin Lindström
 */
class SavedUsersCookieTest {

  private final SavedUsersCookie savedUsers = new SavedUsersCookie("/simulator");

  @Test
  void testSaveAndRead() {
    final SimulatedUser user = user("u1", "Ελένη");
    user.getAddress().setPostName("Λευκωσία");
    final Cookie cookie = this.save("cy", user, null);

    assertThat(cookie.getName()).isEqualTo("savedUsers-cy");
    assertThat(cookie.getPath()).isEqualTo("/simulator");
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getSecure()).isTrue();

    final List<SimulatedUser> users = this.read("cy", cookie);
    assertThat(users).hasSize(1);
    assertThat(users.getFirst().getId()).isEqualTo("u1");
    assertThat(users.getFirst().getGivenNameNonLatin()).isEqualTo("Ελένη");
    assertThat(users.getFirst().getAddress().getPostName()).isEqualTo("Λευκωσία");

    // Another country does not see the user
    assertThat(this.read("gr", cookie)).isEmpty();
  }

  @Test
  void testReplaceUserWithSameId() {
    Cookie cookie = this.save("pt", user("u1", "A"), null);
    cookie = this.save("pt", user("u2", "B"), cookie);
    cookie = this.save("pt", user("u1", "C"), cookie);

    assertThat(this.read("pt", cookie))
        .extracting(SimulatedUser::getId, SimulatedUser::getGivenNameNonLatin)
        .containsExactly(tuple("u2", "B"), tuple("u1", "C"));
  }

  @Test
  void testOldestUsersAreDropped() {
    Cookie cookie = null;
    for (int i = 0; i < SavedUsersCookie.MAX_USERS + 5; i++) {
      cookie = this.save("pt", user("u" + i, "Name " + i), cookie);
    }
    final List<SimulatedUser> users = this.read("pt", Objects.requireNonNull(cookie));
    assertThat(users).hasSize(SavedUsersCookie.MAX_USERS);
    assertThat(users.getFirst().getId()).isEqualTo("u5");
    assertThat(users.getLast().getId()).isEqualTo("u" + (SavedUsersCookie.MAX_USERS + 4));
  }

  @Test
  void testCookieSizeIsLimited() {
    Cookie cookie = null;
    for (int i = 0; i < SavedUsersCookie.MAX_USERS; i++) {
      // Random values do not compress, so the size limit is reached before the user limit
      final SimulatedUser user = user("u" + i, randomText());
      user.setEmailAddress(randomText());
      cookie = this.save("pt", user, cookie);
      assertThat(cookie.getValue().length()).isLessThanOrEqualTo(SavedUsersCookie.MAX_VALUE_LENGTH);
    }
    final List<SimulatedUser> users = this.read("pt", Objects.requireNonNull(cookie));
    assertThat(users).hasSizeLessThan(SavedUsersCookie.MAX_USERS);
    assertThat(users.getLast().getId()).isEqualTo("u" + (SavedUsersCookie.MAX_USERS - 1));
  }

  @Test
  void testInvalidCookieIsIgnored() {
    assertThat(this.read("pt", new Cookie("savedUsers-pt", "not-valid"))).isEmpty();
    assertThat(this.read("pt", new Cookie("savedUsers-pt", "AAAA"))).isEmpty();
  }

  private Cookie save(final String country, final SimulatedUser user, final Cookie existing) {
    final MockHttpServletRequest request = new MockHttpServletRequest();
    if (existing != null) {
      request.setCookies(existing);
    }
    final MockHttpServletResponse response = new MockHttpServletResponse();
    this.savedUsers.saveUser(country, user, request, response);
    return Objects.requireNonNull(response.getCookie(SavedUsersCookie.getCookieName(country)));
  }

  private List<SimulatedUser> read(final String country, final Cookie cookie) {
    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(cookie);
    return this.savedUsers.getUsers(country, request);
  }

  private static String randomText() {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 4; i++) {
      sb.append(UUID.randomUUID());
    }
    return sb.toString();
  }

  private static SimulatedUser user(final String id, final String nonLatinName) {
    final SimulatedUser user = new SimulatedUser();
    user.setId(id);
    user.setPersonIdentifier("12345");
    user.setGivenName("Given");
    user.setGivenNameNonLatin(nonLatinName);
    user.setFamilyName("Family");
    user.setDateOfBirth("1990-01-01");
    return user;
  }

}
