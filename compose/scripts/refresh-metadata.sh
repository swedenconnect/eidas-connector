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
# Fetches the metadata of the connector (its IdP role), Test my eID and the test client from the running services,
# and writes it to the metadata directory of the metadata aggregator (config/metadata-aggregator/metadata).
#
# Run it when the metadata of one of these services has changed, for example after new keys have been generated or
# the configuration has been changed. The files are committed to the repository, so a fresh clone runs without it.
#
# Usage: compose/scripts/refresh-metadata.sh [connector | test-my-eid | test-client] ...
#
# Without arguments, the metadata of all three is fetched. The services must be running.
#

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
METADATA_DIR="${ROOT}/config/metadata-aggregator/metadata"
CA_FILE="${ROOT}/config/common/tls.crt"
HOST="local.eidas.swedenconnect.se"

#
# Fetches the metadata of one entity and writes it to a file.
#
# $1 - the port of the service
# $2 - the path of the metadata
# $3 - the name of the file under config/metadata-aggregator/metadata
#
fetch() {
  local port="$1" path="$2" file="$3"
  local url="https://${HOST}:${port}${path}"
  local tmp
  tmp="$(mktemp)"

  echo "Fetching ${url} ..."
  # The host name is resolved to 127.0.0.1 here, so that the script also works without the hosts file entry.
  if ! curl -fsS --cacert "${CA_FILE}" --resolve "${HOST}:${port}:127.0.0.1" \
      -H "Accept: application/samlmetadata+xml, application/xml" -o "${tmp}" "${url}"; then
    rm -f "${tmp}"
    echo "Failed to fetch ${url}. Is the service running?" >&2
    return 1
  fi

  # Removes the signature of the entity descriptor, and its validUntil and cacheDuration attributes.
  perl -0777 -pe '
    s{<(\w+:)?Signature\b[^>]*>.*?</(\w+:)?Signature>\s*}{}s;
    s{(<(\w+:)?EntityDescriptor\b[^>]*?)\s+validUntil="[^"]*"}{$1}s;
    s{(<(\w+:)?EntityDescriptor\b[^>]*?)\s+cacheDuration="[^"]*"}{$1}s;
  ' "${tmp}" > "${METADATA_DIR}/${file}"
  rm -f "${tmp}"
  echo "  -> config/metadata-aggregator/metadata/${file}"
}

refresh_connector() {
  fetch 12010 /idp/metadata/idp.xml eidas-connector.xml
}

refresh_test_my_eid() {
  fetch 12030 /testmyeid/metadata test-my-eid.xml
  fetch 12030 /testmyeid/metadata/sign test-my-eid-sign.xml
}

refresh_test_client() {
  fetch 12040 /saml/metadata/eidas test-client-eidas.xml
}

services=("$@")
if [[ ${#services[@]} -eq 0 ]]; then
  services=(connector test-my-eid test-client)
fi

mkdir -p "${METADATA_DIR}"

for service in "${services[@]}"; do
  case "${service}" in
    connector) refresh_connector ;;
    test-my-eid) refresh_test_my_eid ;;
    test-client) refresh_test_client ;;
    *)
      echo "Unknown service: ${service}" >&2
      echo "Usage: $0 [connector | test-my-eid | test-client] ..." >&2
      exit 1
      ;;
  esac
done

echo "Done. The metadata aggregator reads the files again within a minute."
