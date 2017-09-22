#!/bin/bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Replace /cygdrive/c with c:/ (if running on Windows)
SCRIPT_DIR_WIN=`echo $SCRIPT_DIR | sed 's/\/cygdrive\/c/c:/g'`

# Remove /src/main/scripts
BASE_DIR_WIN=`echo $SCRIPT_DIR_WIN | sed 's/\/src\/main\/scripts//g'`

# Tomcat
TOMCAT_HOME=$BASE_DIR_WIN/target/dependency/apache-tomcat-8.5.9
CATALINA_HOME=$TOMCAT_HOME

# Home
IDP_ENV_FLAG=dev
IDP_HOME=$BASE_DIR_WIN/target/shibboleth

#
# Set up the IdP server URL
#
IDP_SERVER_SCHEME=https
IDP_SERVER_HOSTNAME=localhost
IDP_SERVER_PORT=9200
IDP_SERVER_PORT_SUFFIX=":${IDP_SERVER_PORT}"
IDP_SERVER_SERVLET_NAME=idp

if [ "$IDP_SERVER_SCHEME" == "https" ] && [ "$IDP_SERVER_PORT" == "443" ]; then
  IDP_SERVER_PORT_SUFFIX=""
fi
if [ "$IDP_SERVER_SCHEME" == "http" ] && [ "$IDP_SERVER_PORT" == "80" ]; then
  IDP_SERVER_PORT_SUFFIX=""
fi

IDP_BASE_URL=${IDP_SERVER_SCHEME}://${IDP_SERVER_HOSTNAME}${IDP_SERVER_PORT_SUFFIX}/${IDP_SERVER_SERVLET_NAME}

#
# Tomcat settings
#
TOMCAT_TLS_PORT=9200
TOMCAT_HOSTNAME=$IDP_SERVER_HOSTNAME
TOMCAT_TLS_KEYSTORE=$TOMCAT_HOME/conf/tls-test-localhost.jks
TOMCAT_TLS_PASSWORD=secret
TOMCAT_TLS_ALIAS=localhost

#
# IdP settings
#
IDP_ENTITY_ID=https://eunode.eidastest.se/idp2
SP_ENTITY_ID=https://eunode.eidastest.se/sp

TEST_SP_METADATA=https://localhost:8443/svelegtest-sp/metadata/all-metadata.xml

IDP_CREDENTIALS=$IDP_HOME/credentials

export JAVA_OPTS="-Didp.entityID=$IDP_ENTITY_ID \
-Didp.sealer.storeResource=$IDP_CREDENTIALS/sealer.jks \
-Didp.sealer.versionResource=$IDP_CREDENTIALS/sealer.kver \
-Didp.sealer.password=3eifrUFrujUefIo8FJN4 \
-Didp.signing.key=$IDP_CREDENTIALS/idp-signing.key \
-Didp.signing.cert=$IDP_CREDENTIALS/idp-signing.crt \
-Didp.encryption.key=$IDP_CREDENTIALS/idp-encryption.key \
-Didp.encryption.cert=$IDP_CREDENTIALS/idp-encryption.crt \
-Didp.metadata.signing.key=$IDP_CREDENTIALS/metadata-signing.key \
-Didp.metadata.signing.cert=$IDP_CREDENTIALS/metadata-signing.crt \
-Didp.sp.entityID=$SP_ENTITY_ID \
-Didp.sp.signing.key=$IDP_CREDENTIALS/sp/sp-signing.key \
-Didp.sp.signing.cert=$IDP_CREDENTIALS/sp/sp-signing.crt \
-Didp.sp.encryption.key=$IDP_CREDENTIALS/sp/sp-signing.key \
-Didp.sp.encryption.cert=$IDP_CREDENTIALS/sp/sp-signing.crt \
-Didp.sp.metadata.signing.key=$IDP_CREDENTIALS/metadata-signing.key \
-Didp.sp.metadata.signing.cert=$IDP_CREDENTIALS/metadata-signing.crt \
-Didp.home=$IDP_HOME \
-Didp.envflag=dev \
-Djava.net.preferIPv4Stack=true \
-Dtomcat.hostname=$TOMCAT_HOSTNAME \
-Dtomcat.tls.keystore=$TOMCAT_TLS_KEYSTORE \
-Dtomcat.tls.password=secret \
-Dtomcat.tls.alias=localhost \
-Dtomcat.tls.port=${TOMCAT_TLS_PORT} \
-Didp.hostname=${IDP_SERVER_HOSTNAME}${IDP_SERVER_PORT_SUFFIX} \
-Didp.baseurl=${IDP_BASE_URL} \
-Didp.test.sp.metadata=$TEST_SP_METADATA \
-Didp.litsec.loglevel=DEBUG"

#JVM settings should go in CATALINA_OPTS
export CATALINA_OPTS="-Xms512m -Xmx1536m"

echo "JAVA_OPTS=$JAVA_OPTS"
echo "CATALINA_OPTS=$CATALINA_OPTS"

export JPDA_ADDRESS=8788
export JPDA_TRANSPORT=dt_socket

if [ "$1" == "-d" ]; then
    echo "Running in debug"    
    $CATALINA_HOME/bin/catalina.sh jpda run
else
    $CATALINA_HOME/bin/catalina.sh run
fi



