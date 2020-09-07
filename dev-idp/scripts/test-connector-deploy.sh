#!/bin/bash

echo Pulling Docker image ...
docker pull docker.eidastest.se:5000/eidas-connector-dev-idp2

echo Undeploying eidas-connector-dev-idp2 ...
docker rm eidas-connector-dev-idp2 --force

echo Re-deploying eidas-connector-dev-idp2 ...

docker run -d --name eidas-connector-dev-idp2 --restart=always \
  -p 8417:8443 \
  -p 8017:8009 \
  -e IDP_SERVER_HOSTNAME=con.sandbox.swedenconnect.se \
  -e IDP_BASE_URL=https://con.sandbox.swedenconnect.se/internal/idp \
  -e IDP_ENTITY_ID=https://internal-dev.connector.swedenconnect.se/eidas \
  -e SP_ENTITY_ID=https://con.sandbox.swedenconnect.se/internal/idp/metadata/sp \
  -e SP_SECURITY_CONFIG_BEAN=eidas.EidasSecurityConfiguration \
  -e IDP_PRID_SERVICE_URL=https://con.sandbox.swedenconnect.se/prid \
  -e FEDERATION_METADATA_URL=https://eid.svelegtest.se/metadata/feed \
  -e FEDERATION_METADATA_VALIDATION_CERT=/opt/eidas-connector/credentials/metadata/sveleg-metadata-validation-cert.crt \
  -e EIDAS_METADATA_URL=https://mdsl.sandbox.swedenconnect.se/nodeconfig/metadata \
  -e EIDAS_METADATA_VALIDATION_CERT=/opt/eidas-connector/shibboleth/metadata/eidas-metadata-validation-cert.crt \
  -e TOMCAT_TLS_SERVER_KEY=/opt/eidas-connector/credentials/tomcat/tomcat-key.pem \
  -e TOMCAT_TLS_SERVER_CERTIFICATE=/opt/eidas-connector/credentials/tomcat/tomcat-cert.pem \
  -e TOMCAT_TLS_SERVER_CERTIFICATE_CHAIN=/opt/eidas-connector/credentials/tomcat/tomcat-chain.pem \
  -e TOMCAT_PROXY_SHARED_SECRET=dummy \
  -e TOMCAT_SESSION_COOKIE_NAME=JSESSIONID.CONNECTOR.SAND2 \
  -e IDP_CREDENTIALS=/opt/eidas-connector/credentials \
  -e SP_ENCRYPTION_KEY=/opt/eidas-connector/credentials/sp/sp-ec-encryption.key \
  -e SP_ENCRYPTION_CERT=/opt/eidas-connector/credentials/sp/sp-ec-encryption.crt \
  -e IDP_SEALER_PASSWORD=3eifrUFrujUefIo8FJN4 \
  -e IDP_PERSISTENT_ID_SALT=jkio98gbnmklop0Pr5WTvCgh \
  -e IDP_ERRORS_VERBOSE=true \
  -e IDP_DEVEL_MODE=true \
  -e IDP_PING_WHITELIST=http://sandbox.swedenconnect.se/testmyeid,https://eid.litsec.se/sp/eidas,https://eid.litsec.se/sp/eidas-sigservice \
  -e IDP_LOG_CONSOLE=false \
  -e IDP_LOG_PUBLISH_ENABLED=true \
  -e IDP_LOG_PUBLISH_PATH=/var/log/eidas-connector/idp-process.log \
  -e IDP_STATS_PUBLISH_PATH=/var/log/eidas-connector/idp-stats.log \
  -v /etc/localtime:/etc/localtime:ro \
  -v /opt/docker/test-eidas-connector/logs:/var/log/eidas-connector \
  -v /opt/docker/test-eidas-connector/etc:/etc/eidas-connector \
  docker.eidastest.se:5000/eidas-connector-dev-idp2
