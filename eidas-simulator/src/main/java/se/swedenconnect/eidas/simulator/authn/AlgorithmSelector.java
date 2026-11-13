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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.ext.saml2alg.DigestMethod;
import org.opensaml.saml.ext.saml2alg.SigningMethod;
import org.opensaml.saml.saml2.metadata.EncryptionMethod;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.encryption.support.EncryptionConstants;
import se.swedenconnect.eidas.simulator.config.CountryProperties;
import se.swedenconnect.eidas.simulator.country.SimulatedCountry;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Selects the algorithms of a response: algorithms that both the simulated country's settings and the connector's
 * metadata allow.
 *
 * @author Martin Lindström
 */
public class AlgorithmSelector {

  /** The data encryption algorithms used when a country does not state any, in order of preference. */
  public static final @NonNull List<String> DEFAULT_DATA_ENCRYPTION_ALGORITHMS = List.of(
      EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256_GCM,
      EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES192_GCM,
      EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128_GCM);

  /**
   * Selects the signature algorithm: the first signing method of the country that fits the country's signing key and
   * that the connector metadata allows.
   *
   * @param country the simulated country
   * @param connectorMetadata the connector metadata
   * @return the algorithm URI
   * @throws IllegalStateException if there is no such algorithm
   */
  public @NonNull String selectSignatureAlgorithm(final @NonNull SimulatedCountry country,
      final @NonNull EntityDescriptor connectorMetadata) throws IllegalStateException {

    final PublicKey key = country.getSigningCredential().getPublicKey();
    final String keyType = keyType(key);
    final int keySize = keySize(key);
    final List<SigningMethod> connectorMethods = findAlgorithmElements(connectorMetadata, SigningMethod.class);

    for (final CountryProperties.SigningMethod method : country.getSettings().getSigningMethods()) {
      final String algorithm = method.getAlgorithm();
      if (algorithm == null || !keyType.equals(algorithmKeyType(algorithm))
          || !inRange(keySize, method.getMinKeySize(), method.getMaxKeySize())) {
        continue;
      }
      if (connectorMethods.isEmpty()) {
        return algorithm;
      }
      final boolean allowed = connectorMethods.stream()
          .anyMatch(c -> algorithm.equals(c.getAlgorithm()) && inRange(keySize, c.getMinKeySize(), c.getMaxKeySize()));
      if (allowed) {
        return algorithm;
      }
    }
    throw new IllegalStateException("%s and the connector have no signature algorithm in common for a %s key of %d bits"
        .formatted(country.getName(), keyType, keySize));
  }

  /**
   * Selects the digest algorithm of signature references: the first digest method of the country that the connector
   * metadata allows.
   *
   * @param country the simulated country
   * @param connectorMetadata the connector metadata
   * @return the algorithm URI
   * @throws IllegalStateException if there is no such algorithm
   */
  public @NonNull String selectDigestAlgorithm(final @NonNull SimulatedCountry country,
      final @NonNull EntityDescriptor connectorMetadata) throws IllegalStateException {
    final List<String> connectorMethods = findAlgorithmElements(connectorMetadata, DigestMethod.class).stream()
        .map(DigestMethod::getAlgorithm)
        .toList();
    return country.getSettings().getDigestMethods().stream()
        .filter(d -> connectorMethods.isEmpty() || connectorMethods.contains(d))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "%s and the connector have no digest algorithm in common".formatted(country.getName())));
  }

  /**
   * Selects the data encryption algorithm: the first algorithm that the country supports and that the connector's
   * encryption key allows. The country's algorithms are those of its supported encryption algorithms entity
   * attribute, or of its encryption key, or else the eIDAS default AES-GCM algorithms.
   *
   * @param country the simulated country
   * @param connectorMetadata the connector metadata
   * @return the algorithm URI
   * @throws IllegalStateException if there is no such algorithm
   */
  public @NonNull String selectDataEncryptionAlgorithm(final @NonNull SimulatedCountry country,
      final @NonNull EntityDescriptor connectorMetadata) throws IllegalStateException {

    final CountryProperties settings = country.getSettings();
    List<String> countryAlgorithms = settings.getSupportedEncryptionAlgorithms();
    if (countryAlgorithms.isEmpty() && settings.getEncryptionKey() != null) {
      countryAlgorithms = settings.getEncryptionKey().getEncryptionMethods();
    }
    if (countryAlgorithms.isEmpty()) {
      countryAlgorithms = DEFAULT_DATA_ENCRYPTION_ALGORITHMS;
    }

    final List<String> connectorAlgorithms = new ArrayList<>();
    final SPSSODescriptor sp = connectorMetadata.getSPSSODescriptor(SAMLConstants.SAML20P_NS);
    if (sp != null) {
      sp.getKeyDescriptors().stream()
          .filter(kd -> kd.getUse() != UsageType.SIGNING)
          .flatMap(kd -> kd.getEncryptionMethods().stream())
          .map(EncryptionMethod::getAlgorithm)
          .forEach(connectorAlgorithms::add);
    }

    return countryAlgorithms.stream()
        .filter(a -> connectorAlgorithms.isEmpty() || connectorAlgorithms.contains(a))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "%s and the connector have no data encryption algorithm in common".formatted(country.getName())));
  }

  /**
   * Finds algorithm support elements ({@code alg:SigningMethod} or {@code alg:DigestMethod}) of the connector
   * metadata, under the {@code EntityDescriptor} or under the {@code SPSSODescriptor}.
   *
   * @param metadata the connector metadata
   * @param type the element type
   * @param <T> the element type
   * @return the elements
   */
  private static <T extends XMLObject> @NonNull List<T> findAlgorithmElements(final @NonNull EntityDescriptor metadata,
      final @NonNull Class<T> type) {
    final List<T> elements = new ArrayList<>(findExtensions(metadata.getExtensions(), type));
    final SPSSODescriptor sp = metadata.getSPSSODescriptor(SAMLConstants.SAML20P_NS);
    if (sp != null) {
      elements.addAll(findExtensions(sp.getExtensions(), type));
    }
    return elements;
  }

  /**
   * Finds extension elements of a given type.
   *
   * @param extensions the extensions, may be {@code null}
   * @param type the element type
   * @param <T> the element type
   * @return the elements
   */
  private static <T extends XMLObject> @NonNull List<T> findExtensions(final @Nullable Extensions extensions,
      final @NonNull Class<T> type) {
    return Optional.ofNullable(extensions)
        .map(e -> e.getUnknownXMLObjects().stream().filter(type::isInstance).map(type::cast).toList())
        .orElse(List.of());
  }

  /**
   * Tells the key type that a signature algorithm is used with.
   *
   * @param algorithm the algorithm URI
   * @return {@code EC}, {@code RSA} or {@code other}
   */
  private static @NonNull String algorithmKeyType(final @NonNull String algorithm) {
    final String lower = algorithm.toLowerCase();
    if (lower.contains("ecdsa")) {
      return "EC";
    }
    if (lower.contains("rsa")) {
      return "RSA";
    }
    return "other";
  }

  /**
   * Tells the type of a key.
   *
   * @param key the key
   * @return {@code EC}, {@code RSA} or the algorithm name of the key
   */
  private static @NonNull String keyType(final @NonNull PublicKey key) {
    if (key instanceof ECPublicKey) {
      return "EC";
    }
    if (key instanceof RSAPublicKey) {
      return "RSA";
    }
    return key.getAlgorithm();
  }

  /**
   * Tells the size of a key in bits.
   *
   * @param key the key
   * @return the size, or 0 if it is not known
   */
  private static int keySize(final @NonNull PublicKey key) {
    if (key instanceof final RSAPublicKey rsa) {
      return rsa.getModulus().bitLength();
    }
    if (key instanceof final ECPublicKey ec) {
      return ec.getParams().getCurve().getField().getFieldSize();
    }
    return 0;
  }

  /**
   * Tells whether a key size is within the given limits.
   *
   * @param size the key size
   * @param min the minimum size, or {@code null}
   * @param max the maximum size, or {@code null}
   * @return {@code true} if the size is within the limits and {@code false} otherwise
   */
  private static boolean inRange(final int size, final @Nullable Integer min, final @Nullable Integer max) {
    return (min == null || size >= min) && (max == null || size <= max);
  }

}
