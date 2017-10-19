#!/usr/bin/env bash

#
# Start-up script for running the eIDAS connector locally in development mode in a Docker container
#

docker run -d --name eidas-connector --restart=always \
  -p 9200:8443 \
  -e IDP_DEVEL_MODE=true \
  -e IDP_SERVER_HOSTNAME=localhost -e IDP_SERVER_PORT=9200 \
  -e IDP_ENTITY_ID="https://eunode.eidastest.se/idp2" \
  -e SP_ENTITY_ID="https://eunode.eidastest.se/connector-sp" \
  -e EIDAS_METADATA_SERVICE_LIST_URL=file:///etc/eidas-connector/mock/metadataList.xml \
  -e EIDAS_METADATA_URL=file:///etc/eidas-connector/mock/metadata.xml \
  -e EIDAS_METADATA_IGNORE_SIGNATURE_VALIDATION=true \
  -v /tmp/eidas-connector:/var/log/eidas-connector \
  -v /private/etc/eidas-connector:/etc/eidas-connector \
  docker.eidastest.se:5000/eidas-connector-idp
  