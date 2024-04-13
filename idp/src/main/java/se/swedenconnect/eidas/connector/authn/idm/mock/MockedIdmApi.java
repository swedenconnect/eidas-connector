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

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.eidas.connector.authn.idm.IdmQueryResponse;

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

  @Setter
  @Autowired
  private MockedIdmData mockData;

  @GetMapping(path = "/{prid}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public IdmQueryResponse getRecord(
      final HttpServletRequest request,
      @RequestHeader(HttpHeaders.AUTHORIZATION) final String authorizationHeader,
      @PathVariable(name = "prid") final String prid) {

    if (!authorizationHeader.startsWith("Bearer ")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid");
    }

    // Authorize - We allow an access token containing the PRID as the subject but we also
    // have a direct trust to the eIDAS connector.
    //
    try {
      final SignedJWT signedJwt = SignedJWT.parse(authorizationHeader.substring("Bearer ".length()));

      if (this.mockData.getAsCertificate() != null) {
        final RSASSAVerifier verifier = new RSASSAVerifier((RSAPublicKey) this.mockData.getAsCertificate().getPublicKey());
        if (!signedJwt.verify(verifier)) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid access token - signature validation failed");
        }
        final JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
        final String subject = claims.getSubject();
        if (subject == null) {
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing sub claim");
        }
        if (Objects.equals(subject, this.mockData.getConnectorId())) {
          log.debug("Mocked IdM: Invoked by connector with direct access token");
        }
        else if (!Objects.equals(prid, subject)) {
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid subject: " + subject);
        }
      }

    }
    catch (ParseException | JOSEException e) {
      log.info("Mocked IdM: Invalid Authorization header - Failed to parse access token", e);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Failed to parse access token");
    }

    final IdmQueryResponse mrecord = this.mockData.getRecord(prid);
    if (mrecord != null) {
      log.info("Mocked IdM: {} -> {}", prid, mrecord);
      return mrecord;
    }
    else {
      log.info("Mocked IdM: {} -> not found", prid, mrecord);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

  }

}
