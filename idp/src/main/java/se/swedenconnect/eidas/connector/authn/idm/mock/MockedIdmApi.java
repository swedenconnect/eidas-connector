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
package se.swedenconnect.eidas.connector.authn.idm.mock;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.eidas.connector.authn.idm.IdmQueryResponse;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Objects;

/**
 * Mocked Identity Matching API.
 *
 * @author Martin Lindström
 */
@RestController
@RequestMapping("/api/v1/mrecord")
@Profile("idmmock")
@Slf4j
public class MockedIdmApi {

  private final MockedIdmData mockData;

  /**
   * Constructs a new MockedIdmApi object.
   *
   * @param mockData the mocked Idm data object
   */
  public MockedIdmApi(final MockedIdmData mockData) {
    this.mockData = mockData;
  }

  @RequestMapping(path = "/{prid}", method = RequestMethod.HEAD)
  ResponseEntity<Void> checkRecord(final HttpServletRequest request,
      @RequestHeader(HttpHeaders.AUTHORIZATION) final String authorizationHeader,
      @PathVariable(name = "prid") final String prid) {

    if (!authorizationHeader.startsWith("Bearer ")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid");
    }
    this.authorize(authorizationHeader, this.mockData.getConnectorId());
    final IdmQueryResponse mrecord = this.mockData.getRecord(prid);
    if (mrecord != null) {
      log.info("Mocked IdM: {} -> {}", prid, mrecord);
      return new ResponseEntity<>(HttpStatus.OK);
    }
    else {
      log.info("Mocked IdM: {} -> not found", prid);
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping(path = "/{prid}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public IdmQueryResponse getRecord(
      final HttpServletRequest request,
      @RequestHeader(HttpHeaders.AUTHORIZATION) final String authorizationHeader,
      @PathVariable(name = "prid") final String prid) {

    if (!authorizationHeader.startsWith("Bearer ")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid");
    }

    this.authorize(authorizationHeader, prid);

    final IdmQueryResponse mrecord = this.mockData.getRecord(prid);
    if (mrecord != null) {
      log.info("Mocked IdM: {} -> {}", prid, mrecord);
      return mrecord;
    }
    else {
      log.info("Mocked IdM: {} -> not found", prid);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

  }

  private void authorize(final String authorizationHeader, final String expectedSubject)
      throws ResponseStatusException {
    try {
      final SignedJWT signedJwt = SignedJWT.parse(authorizationHeader.substring("Bearer ".length()));

      if (this.mockData.getAsCertificate() != null) {
        final RSASSAVerifier verifier =
            new RSASSAVerifier((RSAPublicKey) this.mockData.getAsCertificate().getPublicKey());
        if (!signedJwt.verify(verifier)) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid access token - signature validation failed");
        }
        final JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
        final String subject = claims.getSubject();
        if (subject == null) {
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing sub claim");
        }
        if (!Objects.equals(expectedSubject, subject)) {
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
              "Invalid subject: '%s'. Expected '%s'".formatted(subject, expectedSubject));
        }
      }
    }
    catch (final ParseException | JOSEException e) {
      log.info("Mocked IdM: Invalid Authorization header - Failed to parse access token", e);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Failed to parse access token");
    }
  }

}
