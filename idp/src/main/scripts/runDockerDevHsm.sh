#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

rm -rf /tmp/eidas-connector-credentials 2>/dev/null
cp -r ${SCRIPT_DIR}/../../../credentials /tmp/eidas-connector-credentials
# cp /tmp/eidas-connector-credentials/sp/metadata-signing.crt /tmp/eidas-connector-credentials/sp-metadata-signing.crt
cp /tmp/eidas-connector-credentials/sp/sp-*.* /tmp/eidas-connector-credentials/ 

# To run against local Redis add: -e REDIS_HOST=docker.for.mac.localhost

docker run -d --name eidas-connector-hsm --restart=always \
  -p 9200:8443 \
  -e IDP_DEVEL_MODE=true \
  -e IDP_ERRORS_VERBOSE=true \
  -e IDP_SERVER_HOSTNAME=localhost -e IDP_SERVER_PORT=9200 \
  -e IDP_ENTITY_ID="https://eunode.eidastest.se/idp2" \
  -e SP_ENTITY_ID="https://localhost:9200/idp/metadata/sp" \
  -e FEDERATION_METADATA_URL="https://qa.md.swedenconnect.se/entities" \
  -e FEDERATION_METADATA_VALIDATION_CERT=/etc/eidas-connector-credentials/metadata/sc-qa-metadata-validation-cert.crt \
  -e SECONDARY_FEDERATION_METADATA_URL="https://eid.svelegtest.se/metadata/feed" \
  -e SECONDARY_FEDERATION_METADATA_VALIDATION_CERT=/etc/eidas-connector-credentials/metadata/sveleg-metadata-validation-cert.crt \
  -e EIDAS_METADATA_URL=file:///etc/eidas-connector/eidas/eu-metadata/metadata.xml \
  -e EIDAS_METADATA_IGNORE_SIGNATURE_VALIDATION=true \
  -e IDP_LOG_SETTINGS_FILE=/opt/eidas-connector/shibboleth/conf/logback-devel.xml \
  -e IDP_LOG_CONSOLE=true \
  -e IDP_CREDENTIALS=/etc/eidas-connector-credentials \
  -e SP_CREDENTIALS=/etc/eidas-connector-credentials/sp \
  -e TOMCAT_TLS_SERVER_KEY=/etc/eidas-connector-credentials/tomcat/tomcat-key.pem \
  -e TOMCAT_TLS_SERVER_CERTIFICATE=/etc/eidas-connector-credentials/tomcat/tomcat-cert.pem \
  -e TOMCAT_TLS_SERVER_CERTIFICATE_CHAIN=/etc/eidas-connector-credentials/tomcat/tomcat-chain.pem \
  -e IDP_TLS_TRUSTED_CERTS=/etc/eidas-connector-credentials/tlstrust.pem \
  -e IDP_PRID_SERVICE_URL=https://docker.for.mac.localhost:9443/prid \
  -e IDP_SEALER_PASSWORD=3eifrUFrujUefIo8FJN4 \
  -e TOMCAT_PROXY_SHARED_SECRET=123456 \
  -e IDP_PKCS11_ENABLED=true \
  -e IDP_PKCS11_SOFTHSM=true \
  -e IDP_METADATA_SIGNING_PKCS11_ENABLED=true \
  -e SP_METADATA_SIGNING_PKCS11_ENABLED=true \
  -e IDP_PKCS11_PIN=111111 \
  -e IDP_SIGNING_PKCS11_ALIAS=idp-signing \
  -e IDP_SIGNING_PKCS11_CFG=/etc/eidas-connector-credentials/softhsm.cfg \
  -e IDP_ENCRYPTION_PKCS11_ALIAS=idp-encryption \
  -e IDP_ENCRYPTION_PKCS11_CFG=/etc/eidas-connector-credentials/softhsm.cfg \
  -e SP_SIGNING_PKCS11_ALIAS=sp-signing \
  -e SP_SIGNING_PKCS11_CFG=/etc/eidas-connector-credentials/softhsm.cfg \
  -e SP_ENCRYPTION_PKCS11_ALIAS=sp-encryption \
  -e SP_ENCRYPTION_PKCS11_CFG=/etc/eidas-connector-credentials/softhsm.cfg \
  -e IDP_METADATA_SIGNING_PKCS11_ALIAS=metadata-signing \
  -e IDP_METADATA_SIGNING_PKCS11_CFG=/etc/eidas-connector-credentials/softhsm.cfg \
  -e SP_METADATA_SIGNING_PKCS11_ALIAS=sp-metadata-signing \
  -e SP_METADATA_SIGNING_PKCS11_CFG=/etc/eidas-connector-credentials/softhsm.cfg \
  -v /tmp/eidas-connector-credentials:/etc/eidas-connector-credentials \
  -v /tmp/eidas-connector:/var/log/eidas-connector \
  -v ${LOCAL_ENV_PATH}:/etc/eidas-connector \
  docker.eidastest.se:5000/eidas-connector-idp-hsm
    
docker logs -f eidas-connector-hsm
# -v /private/etc/eidas-connector:/etc/eidas-connector \

