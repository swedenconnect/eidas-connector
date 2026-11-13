#!/usr/bin/env bash
#
# Copyright 2017-2026 Sweden Connect
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Generates the keys, key stores and certificates of the compose setup.
#
# TEST CREDENTIALS ONLY. Every key generated here is committed to a public repository and offers no protection
# whatsoever. Never use them for anything but the compose setup.
#
# Usage: compose/scripts/generate-credentials.sh [all | tls | aggregator | connector | simulator | test-my-eid |
#                                                 test-client] ...
#
# Without arguments, everything is generated. Running the script again replaces the files. The trust store in
# config/common is rebuilt whenever the TLS certificate is replaced.
#
# The simulator target generates the metadata signing key of the simulator and the keys of each simulated country.
# The key of a country has the same type and size as the key of the country's entry in the production metadata. The
# key store is also copied to the tests of the simulator (eidas-simulator/src/test/resources).
#
# After new SAML keys have been generated for the connector, Test my eID or the test client, their metadata must be
# refreshed in the metadata aggregator, see scripts/refresh-metadata.sh.
#
# Requires keytool (part of any Java JDK, version 17 or later).
#

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CONFIG="${ROOT}/config"
SIMULATOR_TEST_RESOURCES="${ROOT}/../eidas-simulator/src/test/resources"

HOST="local.eidas.swedenconnect.se"
PASSWORD="secret"
VALIDITY_DAYS=3650
DN_SUFFIX="OU=TEST ONLY - eIDAS Compose Setup, O=Sweden Connect, C=SE"

#
# The signing key and, for some countries, the encryption key of each simulated country, as in the production
# metadata: "RSA <size>" or "EC <curve>".
#
COUNTRY_KEYS=(
  "AT:RSA 3072:"
  "BE:RSA 4096:"
  "CY:RSA 4096:"
  "CZ:EC secp521r1:"
  "DE:RSA 4096:"
  "DK:RSA 3072:RSA 3072"
  "EE:EC secp256r1:"
  "ES:RSA 4096:"
  "FI:RSA 3072:"
  "FR:RSA 3072:"
  "HR:RSA 4096:"
  "IT:EC secp384r1:RSA 3072"
  "LI:RSA 4096:"
  "LT:RSA 4096:"
  "LU:RSA 4096:RSA 4096"
  "LV:RSA 4096:RSA 4096"
  "MT:RSA 3072:RSA 3072"
  "NL:RSA 4096:RSA 4096"
  "PL:EC secp256r1:"
  "PT:RSA 3072:RSA 3072"
  "RO:RSA 4096:"
  "SI:RSA 4096:RSA 4096"
  "SK:RSA 4096:"
)

if ! command -v keytool > /dev/null; then
  echo "keytool was not found. Install a Java JDK (17 or later) and make sure keytool is in the PATH." >&2
  exit 1
fi

#
# Runs keytool, and only shows its output if it fails.
#
run_keytool() {
  local output
  if ! output="$(keytool "$@" 2>&1)"; then
    echo "${output}" >&2
    exit 1
  fi
}

#
# Generates a key pair with a self-signed certificate.
#
# $1 - the key store file
# $2 - the alias
# $3 - the common name of the certificate
# $4 - the key: "RSA <size>" or "EC <curve>"
# $5 - optional keytool -ext argument
#
genkey() {
  local store="$1" alias="$2" cn="$3" key="$4" ext="${5:-}"
  local type="${key%% *}" param="${key#* }"
  local args=(-genkeypair -keystore "${store}" -storetype JKS -storepass "${PASSWORD}" -keypass "${PASSWORD}"
    -alias "${alias}" -validity "${VALIDITY_DAYS}" -dname "CN=${cn}, ${DN_SUFFIX}")
  case "${type}" in
    RSA) args+=(-keyalg RSA -keysize "${param}" -sigalg SHA256withRSA) ;;
    EC)
      case "${param}" in
        secp256r1) args+=(-keyalg EC -groupname secp256r1 -sigalg SHA256withECDSA) ;;
        secp384r1) args+=(-keyalg EC -groupname secp384r1 -sigalg SHA384withECDSA) ;;
        secp521r1) args+=(-keyalg EC -groupname secp521r1 -sigalg SHA512withECDSA) ;;
      esac
      ;;
  esac
  if [[ -n "${ext}" ]]; then
    args+=(-ext "${ext}")
  fi
  run_keytool "${args[@]}"
}

#
# Exports a certificate in PEM format.
#
# $1 - the key store file
# $2 - the alias
# $3 - the certificate file
#
exportcert() {
  run_keytool -exportcert -rfc -keystore "$1" -storepass "${PASSWORD}" -alias "$2" -file "$3"
}

generate_tls() {
  echo "Generating the TLS key store for ${HOST} ..."
  local store="${CONFIG}/common/tls.jks"
  rm -f "${store}"
  genkey "${store}" tls "${HOST}" "RSA 3072" "SAN=DNS:${HOST},DNS:localhost,IP:127.0.0.1"
  exportcert "${store}" tls "${CONFIG}/common/tls.crt"
}

generate_truststore() {
  echo "Building the trust store ..."
  local store="${CONFIG}/common/trust.jks"
  rm -f "${store}"
  run_keytool -importcert -noprompt -keystore "${store}" -storetype JKS -storepass "${PASSWORD}" \
    -alias tls -file "${CONFIG}/common/tls.crt"
}

generate_aggregator() {
  echo "Generating the metadata signing key of the metadata aggregator ..."
  local store="${CONFIG}/metadata-aggregator/metadata-signing.jks"
  rm -f "${store}"
  genkey "${store}" metadata "eIDAS Compose Setup Metadata Signing" "RSA 3072"
  exportcert "${store}" metadata "${CONFIG}/common/aggregator-metadata-signing.crt"
}

generate_connector() {
  echo "Generating the keys of the eIDAS connector ..."
  local store="${CONFIG}/connector/connector-keys.jks"
  rm -f "${store}"
  genkey "${store}" idp-sign "eIDAS Connector IdP Signing" "RSA 3072"
  genkey "${store}" idp-encrypt "eIDAS Connector IdP Encryption" "RSA 3072"
  genkey "${store}" metadata-sign "eIDAS Connector Metadata Signing" "RSA 3072"
  genkey "${store}" sp-sign "eIDAS Connector SP Signing" "RSA 3072"
  genkey "${store}" sp-encrypt "eIDAS Connector SP Encryption" "RSA 3072"
  exportcert "${store}" metadata-sign "${CONFIG}/common/connector-metadata-signing.crt"
}

generate_simulator() {
  echo "Generating the keys of the eIDAS simulator ..."
  local store="${CONFIG}/simulator/simulator-keys.jks"
  rm -f "${store}"
  genkey "${store}" metadata "eIDAS Simulator Metadata Signing" "RSA 3072"
  exportcert "${store}" metadata "${CONFIG}/common/simulator-metadata-signing.crt"

  local entry code sign encrypt lower
  for entry in "${COUNTRY_KEYS[@]}"; do
    IFS=":" read -r code sign encrypt <<< "${entry}"
    lower="$(echo "${code}" | tr '[:upper:]' '[:lower:]')"
    echo "  ${code} ..."
    genkey "${store}" "${lower}-sign" "Simulated eIDAS node ${code} Signing" "${sign}"
    if [[ -n "${encrypt}" ]]; then
      genkey "${store}" "${lower}-encrypt" "Simulated eIDAS node ${code} Encryption" "${encrypt}"
    fi
  done

  if [[ -d "${SIMULATOR_TEST_RESOURCES}" ]]; then
    cp "${store}" "${SIMULATOR_TEST_RESOURCES}/simulator-keys.jks"
  fi
}

generate_test_my_eid() {
  echo "Generating the SAML keys of Test my eID ..."
  local store="${CONFIG}/test-my-eid/sp-keys.jks"
  rm -f "${store}"
  genkey "${store}" sign "Test my eID SAML Signing" "RSA 3072"
  genkey "${store}" encrypt "Test my eID SAML Encryption" "RSA 3072"
  genkey "${store}" metadata-sign "Test my eID Metadata Signing" "RSA 3072"
}

generate_test_client() {
  echo "Generating the SAML keys of the test client ..."
  local store="${CONFIG}/test-client/sp-keys.jks"
  rm -f "${store}"
  genkey "${store}" sign "Test Client SAML Signing" "RSA 3072"
  genkey "${store}" encrypt "Test Client SAML Encryption" "RSA 3072"
}

targets=("$@")
if [[ ${#targets[@]} -eq 0 ]]; then
  targets=(all)
fi

mkdir -p "${CONFIG}/common" "${CONFIG}/metadata-aggregator" "${CONFIG}/connector" "${CONFIG}/simulator" \
  "${CONFIG}/test-my-eid" "${CONFIG}/test-client"

rebuild_trust=false
for target in "${targets[@]}"; do
  case "${target}" in
    all)
      generate_tls
      generate_aggregator
      generate_connector
      generate_simulator
      generate_test_my_eid
      generate_test_client
      rebuild_trust=true
      ;;
    tls)
      generate_tls
      rebuild_trust=true
      ;;
    aggregator) generate_aggregator ;;
    connector) generate_connector ;;
    simulator) generate_simulator ;;
    test-my-eid) generate_test_my_eid ;;
    test-client) generate_test_client ;;
    *)
      echo "Unknown target: ${target}" >&2
      echo "Usage: $0 [all | tls | aggregator | connector | simulator | test-my-eid | test-client] ..." >&2
      exit 1
      ;;
  esac
done

if [[ "${rebuild_trust}" == "true" ]]; then
  generate_truststore
fi

echo "Done. The password of every key store and key is '${PASSWORD}'."
