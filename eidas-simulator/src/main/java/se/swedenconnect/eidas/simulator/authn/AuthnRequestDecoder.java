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

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Decodes an authentication request received over the HTTP-POST or the HTTP-Redirect binding.
 *
 * @author Martin Lindström
 */
public class AuthnRequestDecoder {

  /** The largest inflated request that is accepted. */
  private static final int MAX_INFLATED_SIZE = 1024 * 1024;

  /**
   * Decodes the authentication request of an HTTP request. A GET request is decoded as the HTTP-Redirect binding and
   * a POST request as the HTTP-POST binding.
   *
   * @param request the HTTP request
   * @return the decoded request
   * @throws UnrecoverableRequestException if the request cannot be decoded
   */
  public @NonNull DecodedRequest decode(final @NonNull HttpServletRequest request)
      throws UnrecoverableRequestException {
    return "GET".equalsIgnoreCase(request.getMethod())
        ? this.decodeRedirect(request)
        : this.decodePost(request);
  }

  /**
   * Decodes a request received over the HTTP-POST binding.
   *
   * @param request the HTTP request
   * @return the decoded request
   * @throws UnrecoverableRequestException if the request cannot be decoded
   */
  private @NonNull DecodedRequest decodePost(final @NonNull HttpServletRequest request)
      throws UnrecoverableRequestException {
    final String encoded = request.getParameter("SAMLRequest");
    if (encoded == null || encoded.isBlank()) {
      throw new UnrecoverableRequestException("No SAMLRequest parameter in the request");
    }
    final byte[] xml;
    try {
      xml = Base64.getMimeDecoder().decode(encoded);
    }
    catch (final IllegalArgumentException e) {
      throw new UnrecoverableRequestException("The SAMLRequest parameter is not valid Base64", e);
    }
    return new DecodedRequest(unmarshall(xml), SAMLConstants.SAML2_POST_BINDING_URI,
        request.getParameter("RelayState"), null, null, null);
  }

  /**
   * Decodes a request received over the HTTP-Redirect binding. The signature, if any, is kept so that it can be
   * validated against the connector metadata.
   *
   * @param request the HTTP request
   * @return the decoded request
   * @throws UnrecoverableRequestException if the request cannot be decoded
   */
  private @NonNull DecodedRequest decodeRedirect(final @NonNull HttpServletRequest request)
      throws UnrecoverableRequestException {
    final String query = request.getQueryString();
    if (query == null || query.isBlank()) {
      throw new UnrecoverableRequestException("No SAMLRequest parameter in the request");
    }
    final Map<String, String> raw = new HashMap<>();
    for (final String part : query.split("&")) {
      final int pos = part.indexOf('=');
      if (pos > 0) {
        raw.putIfAbsent(part.substring(0, pos), part.substring(pos + 1));
      }
    }
    final String rawRequest = raw.get("SAMLRequest");
    if (rawRequest == null || rawRequest.isBlank()) {
      throw new UnrecoverableRequestException("No SAMLRequest parameter in the request");
    }

    final byte[] xml;
    try {
      xml = inflate(Base64.getMimeDecoder().decode(urlDecode(rawRequest)));
    }
    catch (final IllegalArgumentException | DataFormatException e) {
      throw new UnrecoverableRequestException("The SAMLRequest parameter could not be decoded", e);
    }

    final String rawRelayState = raw.get("RelayState");
    final String rawSigAlg = raw.get("SigAlg");
    final String rawSignature = raw.get("Signature");

    byte[] signedContent = null;
    byte[] signature = null;
    String sigAlg = null;
    if (rawSigAlg != null && rawSignature != null) {
      final StringBuilder sb = new StringBuilder("SAMLRequest=").append(rawRequest);
      if (rawRelayState != null) {
        sb.append("&RelayState=").append(rawRelayState);
      }
      sb.append("&SigAlg=").append(rawSigAlg);
      signedContent = sb.toString().getBytes(StandardCharsets.UTF_8);
      sigAlg = urlDecode(rawSigAlg);
      try {
        signature = Base64.getMimeDecoder().decode(urlDecode(rawSignature));
      }
      catch (final IllegalArgumentException e) {
        throw new UnrecoverableRequestException("The Signature parameter is not valid Base64", e);
      }
    }

    return new DecodedRequest(unmarshall(xml), SAMLConstants.SAML2_REDIRECT_BINDING_URI,
        rawRelayState != null ? urlDecode(rawRelayState) : null, signedContent, signature, sigAlg);
  }

  /**
   * Unmarshalls an authentication request.
   *
   * @param xml the XML
   * @return the authentication request
   * @throws UnrecoverableRequestException if the XML is not an authentication request
   */
  private static @NonNull AuthnRequest unmarshall(final byte @NonNull [] xml) throws UnrecoverableRequestException {
    final XMLObject object;
    try {
      object = XMLObjectSupport.unmarshallFromInputStream(
          Objects.requireNonNull(XMLObjectProviderRegistrySupport.getParserPool()), new ByteArrayInputStream(xml));
    }
    catch (final Exception e) {
      throw new UnrecoverableRequestException("The SAMLRequest parameter is not a valid SAML message", e);
    }
    if (object instanceof final AuthnRequest authnRequest) {
      return authnRequest;
    }
    throw new UnrecoverableRequestException("The SAMLRequest parameter is not an AuthnRequest");
  }

  /**
   * Inflates data compressed with the DEFLATE algorithm, without a zlib header.
   *
   * @param data the compressed data
   * @return the inflated data
   * @throws DataFormatException if the data cannot be inflated
   */
  private static byte @NonNull [] inflate(final byte @NonNull [] data) throws DataFormatException {
    final Inflater inflater = new Inflater(true);
    try {
      inflater.setInput(data);
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final byte[] buffer = new byte[4096];
      while (!inflater.finished()) {
        final int n = inflater.inflate(buffer);
        if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
          break;
        }
        out.write(buffer, 0, n);
        if (out.size() > MAX_INFLATED_SIZE) {
          throw new DataFormatException("Inflated request is too large");
        }
      }
      return out.toByteArray();
    }
    finally {
      inflater.end();
    }
  }

  /**
   * URL-decodes a value.
   *
   * @param value the value
   * @return the decoded value
   */
  private static @NonNull String urlDecode(final @NonNull String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }

  /**
   * A decoded authentication request.
   *
   * @param authnRequest the authentication request
   * @param binding the binding that the request was received over
   * @param relayState the relay state, or {@code null}
   * @param signedContent for the HTTP-Redirect binding, the signed part of the query string, otherwise {@code null}
   * @param signature for the HTTP-Redirect binding, the signature, otherwise {@code null}
   * @param signatureAlgorithm for the HTTP-Redirect binding, the signature algorithm, otherwise {@code null}
   */
  public record DecodedRequest(@NonNull AuthnRequest authnRequest, @NonNull String binding,
      @Nullable String relayState, byte @Nullable [] signedContent, byte @Nullable [] signature,
      @Nullable String signatureAlgorithm) {
  }

}
