#!/usr/bin/env bash

#
# Start-up script for running the eIDAS connector locally in development mode in a Docker container
#

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

rm -rf /tmp/eidas-connector-credentials 2>/dev/null
cp -r ${SCRIPT_DIR}/../../../credentials /tmp/eidas-connector-credentials

docker run -d --name eidas-connector --restart=always \
  -p 9200:8443 \
  -e IDP_DEVEL_MODE=true \
  -e IDP_SERVER_HOSTNAME=localhost -e IDP_SERVER_PORT=9200 \
  -e IDP_ENTITY_ID="https://eunode.eidastest.se/idp2" \
  -e SP_ENTITY_ID="https://eunode.eidastest.se/connector-sp" \
  -e EIDAS_METADATA_SERVICE_LIST_URL=file:///etc/eidas-connector/mock/metadataList.xml \
  -e EIDAS_METADATA_URL=file:///etc/eidas-connector/mock/metadata.xml \
  -e EIDAS_METADATA_IGNORE_SIGNATURE_VALIDATION=true \
  -e IDP_LOG_SETTINGS_FILE=/opt/eidas-connector/shibboleth/conf/logback-devel \
  -e IDP_LOG_CONSOLE=true \
  -e IDP_CREDENTIALS=/etc/eidas-connector-credentials \
  -e SP_CREDENTIALS=/etc/eidas-connector-credentials/sp \
  -v /tmp/eidas-connector-credentials:/etc/eidas-connector-credentials \
  -v /tmp/eidas-connector:/var/log/eidas-connector \
  -v /private/etc/eidas-connector:/etc/eidas-connector \
  docker.eidastest.se:5000/eidas-connector-idp
 
# docker logs -f eidas-connector
