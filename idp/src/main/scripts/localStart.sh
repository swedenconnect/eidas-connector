#!/bin/bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Replace /cygdrive/c with c:/ (if running on Windows)
SCRIPT_DIR_WIN=`echo $SCRIPT_DIR | sed 's/\/cygdrive\/c/c:/g'`

# Remove /src/main/scripts
BASE_DIR_WIN=`echo $SCRIPT_DIR_WIN | sed 's/\/src\/main\/scripts//g'`

# Tomcat
TOMCAT_HOME=$BASE_DIR_WIN/target/dependency/apache-tomcat-8.5.23
CATALINA_HOME=$TOMCAT_HOME

# Home
IDP_HOME=$BASE_DIR_WIN/target/shibboleth
CREDENTIALS_BASE=$BASE_DIR_WIN/target/credentials

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

TOMCAT_TLS_SERVER_KEY=$CREDENTIALS_BASE/tomcat/tomcat-key.pem
TOMCAT_TLS_SERVER_KEY_TYPE=RSA
TOMCAT_TLS_SERVER_CERTIFICATE=$CREDENTIALS_BASE/tomcat/tomcat-cert.pem
TOMCAT_TLS_SERVER_CERTIFICATE_CHAIN=$CREDENTIALS_BASE/tomcat/tomcat-chain.pem 

if [ ! -f "$TOMCAT_TLS_SERVER_CERTIFICATE_CHAIN" ]; then
  echo "$TOMCAT_TLS_SERVER_CERTIFICATE_CHAIN does not exist, using dummy chain ..." >&2
  TOMCAT_TLS_SERVER_CERTIFICATE_CHAIN=$TOMCAT_HOME/conf/dummy-chain.pem
fi

#
# IdP and SP settings
#
IDP_ENTITY_ID=https://eunode.eidastest.se/idp2
SP_ENTITY_ID=https://eunode.eidastest.se/connector-sp

IDP_PRID_SERVICE_URL=https://localhost:9443/prid

TEST_SP_METADATA=https://localhost:8443/svelegtest-sp/metadata/all-metadata.xml

IDP_CREDENTIALS=$CREDENTIALS_BASE
SP_CREDENTIALS=$IDP_CREDENTIALS/sp

IDP_PERSISTENT_ID_SALT=jkio98gbnmklop0Pr5WTvCgh

#
# Metadata
#
FEDERATION_METADATA_URL=https://eid.svelegtest.se/metadata/feed
FEDERATION_METADATA_VALIDATION_CERT=${IDP_HOME}/metadata/metadata-validation-cert.crt

# https://eid.svelegtest.se/nodeconfig/mdservicelist
EIDAS_METADATA_SERVICE_LIST_URL=file:///private/etc/eidas-connector/mock/metadataList.xml
EIDAS_METADATA_SERVICE_LIST_VALIDATION_CERT=${IDP_HOME}/metadata/eidas-servicelist-validation-cert.crt
# https://eid.svelegtest.se/nodeconfig/metadata
EIDAS_METADATA_URL=file:///private/etc/eidas-connector/mock/metadata.xml
EIDAS_METADATA_VALIDATION_CERT=${IDP_HOME}/metadata/eidas-metadata-validation-cert.crt
# false
EIDAS_METADATA_IGNORE_SIGNATURE_VALIDATION=true

#
# Logging
#
IDP_LOG_SETTINGS_FILE=${IDP_HOME}/conf/logback-devel.xml
IDP_FTICKS_FEDERATION_ID=eIDAS

: ${IDP_SYSLOG_PORT:=514}

IDP_AUDIT_APPENDER=IDP_AUDIT
IDP_FTICKS_APPENDER=IDP_PROCESS
IDP_SYSLOG_HOST_INT=localhost

if [ -n "$IDP_SYSLOG_HOST" ]; then
  IDP_AUDIT_APPENDER=IDP_AUDIT_SYSLOG
  IDP_FTICKS_APPENDER=IDP_FTICKS
  IDP_SYSLOG_HOST_INT=$IDP_SYSLOG_HOST
fi

IDP_FTICKS_SYSLOG_FACILITY=AUTH
IDP_AUDIT_SYSLOG_FACILITY=AUTH

IDP_FTICKS_ALGORITHM=SHA-256
IDP_FTICKS_SALT=kdssdjas987ghdasn

: ${IDP_LOG_CONSOLE:=false}
IDP_PROCESS_APPENDER=IDP_PROCESS
if [ "$IDP_LOG_CONSOLE" = true ]; then
  IDP_PROCESS_APPENDER=CONSOLE
fi

#
# Assign all values
#
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
-Didp.metadata.federation.url=${FEDERATION_METADATA_URL} \
-Didp.metadata.federation.validation-certificate=${FEDERATION_METADATA_VALIDATION_CERT} \
-Didp.metadata.eidas.service-list.url=${EIDAS_METADATA_SERVICE_LIST_URL} \
-Didp.metadata.eidas.service-list.validation-certificate=${EIDAS_METADATA_SERVICE_LIST_VALIDATION_CERT} \
-Didp.metadata.eidas.federation.url=${EIDAS_METADATA_URL} \
-Didp.metadata.eidas.federation.validation-certificate=${EIDAS_METADATA_VALIDATION_CERT} \
-Didp.metadata.eidas.ignore-signature-validation=${EIDAS_METADATA_IGNORE_SIGNATURE_VALIDATION} \
-Didp.persistentId.salt.value=${IDP_PERSISTENT_ID_SALT} \
-Didp.metadata.validity=10800 \
-Didp.metadata.cacheDuration=3600000 \
-Didp.prid-service.url=$IDP_PRID_SERVICE_URL \
-Didp.sp.entityID=$SP_ENTITY_ID \
-Didp.sp.signing.key=$SP_CREDENTIALS/sp-signing.key \
-Didp.sp.signing.cert=$SP_CREDENTIALS/sp-signing.crt \
-Didp.sp.encryption.key=$SP_CREDENTIALS/sp-encryption.key \
-Didp.sp.encryption.cert=$SP_CREDENTIALS/sp-encryption.crt \
-Didp.sp.metadata.signing.key=$SP_CREDENTIALS/metadata-signing.key \
-Didp.sp.metadata.signing.cert=$SP_CREDENTIALS/metadata-signing.crt \
-Didp.sp.metadata.validity=10800 \
-Didp.sp.metadata.cacheDuration=3600000 \
-Didp.home=$IDP_HOME \
-Didp.envflag=dev \
-Djava.net.preferIPv4Stack=true \
-Dtomcat.hostname=$TOMCAT_HOSTNAME \
-Dtomcat.tls.port=${TOMCAT_TLS_PORT} \
-Dtomcat.ajp.port=8099 \
-Dtomcat.tls.server-key=$TOMCAT_TLS_SERVER_KEY \
-Dtomcat.tls.server-key-type=$TOMCAT_TLS_SERVER_KEY_TYPE \
-Dtomcat.tls.server-certificate=$TOMCAT_TLS_SERVER_CERTIFICATE \
-Dtomcat.tls.certificate-chain=$TOMCAT_TLS_SERVER_CERTIFICATE_CHAIN \
-Didp.hostname=${IDP_SERVER_HOSTNAME}${IDP_SERVER_PORT_SUFFIX} \
-Didp.baseurl=${IDP_BASE_URL} \
-Didp.test.sp.metadata=$TEST_SP_METADATA \
-Didp.service.metadata.resources=shibboleth.DevelMetadataResolverResources \
-Didp.log-settings.file=$IDP_LOG_SETTINGS_FILE \
-Didp.audit.appender=$IDP_AUDIT_APPENDER \
-Didp.syslog.host=$IDP_SYSLOG_HOST_INT \
-Didp.syslog.facility=$IDP_AUDIT_SYSLOG_FACILITY \
-Didp.syslog.port=$IDP_SYSLOG_PORT \
-Didp.fticks.appender=$IDP_FTICKS_APPENDER \
-Didp.fticks.loghost=$IDP_SYSLOG_HOST_INT \
-Didp.fticks.facility=$IDP_FTICKS_SYSLOG_FACILITY \
-Didp.fticks.algorithm=$IDP_FTICKS_ALGORITHM \
-Didp.fticks.salt=$IDP_FTICKS_SALT \
-Didp.consent.appender=NOOP_APPENDER \
-Didp.process.appender=$IDP_PROCESS_APPENDER \
"

# F-TICKS

if [ -n "$IDP_FTICKS_FEDERATION_ID" ]; then

  export JAVA_OPTS="${JAVA_OPTS} \
    -Didp.fticks.federation=${IDP_FTICKS_FEDERATION_ID}"
fi

#JVM settings should go in CATALINA_OPTS
export CATALINA_OPTS="-Xms512m -Xmx1536m"

export JPDA_ADDRESS=8788
export JPDA_TRANSPORT=dt_socket

if [ "$1" == "-d" ]; then
    echo "Running in debug"    
    $CATALINA_HOME/bin/catalina.sh jpda run
else
    $CATALINA_HOME/bin/catalina.sh run
fi



