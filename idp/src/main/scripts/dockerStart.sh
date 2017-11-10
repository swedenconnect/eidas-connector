#!/usr/bin/env bash

if [ $# -eq 1 ]; then
    myapp=$1
else
    echo "$0 Need name of app as argument"
    exit 1
fi

# Source this app's config
#
for i in `\ls /etc/${myapp}/env/*.conf 2>/dev/null`
do
    echo sourcing $i
    source $i
done

IDP_HOME=/opt/eidas-connector/shibboleth
TOMCAT_HOME=/opt/eidas-connector/tomcat

#
# Setup IdP URL
#
: ${IDP_SERVER_SCHEME:=https}
: ${IDP_SERVER_HOSTNAME:=eunode.eidastest.se}
: ${IDP_SERVER_PORT:=443}
: ${IDP_SERVER_SERVLET_NAME:=idp}

IDP_SERVER_PORT_SUFFIX=":${IDP_SERVER_PORT}"

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

: ${TOMCAT_TLS_PORT:=8443}
: ${TOMCAT_AJP_PORT:=8009}
: ${TOMCAT_HOSTNAME:=localhost}

: ${TOMCAT_TLS_SERVER_KEY:=/etc/eidas-connector/credentials/tomcat/tomcat-key.pem}
: ${TOMCAT_TLS_SERVER_CERTIFICATE:=/etc/eidas-connector/credentials/tomcat/tomcat-cert.pem}
: ${TOMCAT_TLS_SERVER_CERTIFICATE_CHAIN:=/etc/eidas-connector/credentials/tomcat/tomcat-chain.pem}
: ${TOMCAT_TLS_SERVER_KEY_TYPE:=RSA} 

if [ ! -f "$TOMCAT_TLS_SERVER_CERTIFICATE_CHAIN" ]; then
  TOMCAT_TLS_SERVER_CERTIFICATE_CHAIN=$TOMCAT_HOME/conf/dummy-chain.pem
fi

#
# IdP settings
#
: ${IDP_ENTITY_ID:=https://eunode.eidastest.se/eidas}

: ${IDP_CREDENTIALS:=/etc/eidas-connector/credentials}
: ${IDP_SEALER_STORE_RESOURCE:=$IDP_CREDENTIALS/sealer.jks}
: ${IDP_SEALER_PASSWORD:=3eifrUFrujUefIo8FJN4}
: ${IDP_SEALER_VERSION_RESOURCES:=$IDP_CREDENTIALS/sealer.kver}
: ${IDP_SIGNING_KEY:=$IDP_CREDENTIALS/idp-signing.key}
: ${IDP_SIGNING_CERT:=$IDP_CREDENTIALS/idp-signing.crt}
: ${IDP_ENCRYPTION_KEY:=$IDP_CREDENTIALS/idp-encryption.key}
: ${IDP_ENCRYPTION_CERT:=$IDP_CREDENTIALS/idp-encryption.crt}
: ${IDP_METADATA_SIGNING_KEY:=$IDP_CREDENTIALS/metadata-signing.key}
: ${IDP_METADATA_SIGNING_CERT:=$IDP_CREDENTIALS/metadata-signing.crt}

: ${IDP_AA_URL:=https://eunode.eidastest.se/eidas-aa}

: ${IDP_PERSISTENT_ID_SALT:=jkio98gbnmklop0Pr5WTvCgh}

: ${IDP_METADATA_VALIDITY_MINUTES:=10800}
: ${IDP_METADATA_CACHEDURATION_MILLIS:=3600000}

# Verification that all IdP credentials are in place ...
if [ ! -f "$IDP_SEALER_STORE_RESOURCE" ]; then
	echo "$IDP_SEALER_STORE_RESOURCE does not exist" >&2
    exit 1
fi
if [ ! -f "$IDP_SIGNING_KEY" ]; then
	echo "IdP signature key - $IDP_SIGNING_KEY - does not exist" >&2
  exit 1
fi
if [ ! -f "$IDP_SIGNING_CERT" ]; then
	echo "IdP signature certificate - $IDP_SIGNING_CERT - does not exist" >&2
  exit 1
fi
if [ ! -f "$IDP_ENCRYPTION_KEY" ]; then
	echo "IdP encryption key - $IDP_ENCRYPTION_KEY - does not exist" >&2
  exit 1
fi
if [ ! -f "$IDP_ENCRYPTION_CERT" ]; then
	echo "IdP encryption certificate - $IDP_ENCRYPTION_CERT - does not exist" >&2
  exit 1
fi
if [ ! -f "$IDP_METADATA_SIGNING_KEY" ]; then
	echo "IdP metadata signing key - $IDP_METADATA_SIGNING_KEY - does not exist" >&2
  exit 1
fi
if [ ! -f "$IDP_METADATA_SIGNING_CERT" ]; then
	echo "IdP metadata signing certificate - $IDP_METADATA_SIGNING_CERT - does not exist" >&2
  exit 1
fi

#
# SP settings
#
: ${SP_ENTITY_ID:=https://eunode.eidastest.se/idp/metadata/sp}

: ${SP_CREDENTIALS:=$IDP_CREDENTIALS/sp}
: ${SP_SIGNING_KEY:=$SP_CREDENTIALS/sp-signing.key}
: ${SP_SIGNING_CERT:=$SP_CREDENTIALS/sp-signing.crt}
: ${SP_ENCRYPTION_KEY:=$SP_CREDENTIALS/sp-encryption.key}
: ${SP_ENCRYPTION_CERT:=$SP_CREDENTIALS/sp-encryption.crt}
: ${SP_METADATA_SIGNING_KEY:=$SP_CREDENTIALS/metadata-signing.key}
: ${SP_METADATA_SIGNING_CERT:=$SP_CREDENTIALS/metadata-signing.crt}

: ${SP_METADATA_VALIDITY_MINUTES:=10800}
: ${SP_METADATA_CACHEDURATION_MILLIS:=3600000}

# Verification that all SP credentials are in place ...
if [ ! -f "$SP_SIGNING_KEY" ]; then
	echo "SP signature key - $SP_SIGNING_KEY - does not exist" >&2
  exit 1
fi
if [ ! -f "$SP_SIGNING_CERT" ]; then
	echo "SP signature certificate - $SP_SIGNING_CERT - does not exist" >&2
  exit 1
fi
if [ ! -f "$SP_ENCRYPTION_KEY" ]; then
	echo "SP encryption key - $SP_ENCRYPTION_KEY - does not exist" >&2
  exit 1
fi
if [ ! -f "$SP_ENCRYPTION_CERT" ]; then
	echo "SP encryption certificate - $SP_ENCRYPTION_CERT - does not exist" >&2
  exit 1
fi
if [ ! -f "$SP_METADATA_SIGNING_KEY" ]; then
	echo "SP metadata signing key - $SP_METADATA_SIGNING_KEY - does not exist" >&2
  exit 1
fi
if [ ! -f "$SP_METADATA_SIGNING_CERT" ]; then
	echo "SP metadata signing certificate - $SP_METADATA_SIGNING_CERT - does not exist" >&2
  exit 1
fi

#
# Metadata
#
IDP_METADATA_RESOURCES_BEAN=shibboleth.MetadataResolverResources

: ${FEDERATION_METADATA_URL:=https://eid.svelegtest.se/metadata/feed}
: ${FEDERATION_METADATA_VALIDATION_CERT:=$IDP_HOME/metadata/metadata-validation-cert.crt}
: ${EIDAS_METADATA_SERVICE_LIST_URL:=https://eid.svelegtest.se/nodeconfig/mdservicelist}
: ${EIDAS_METADATA_SERVICE_LIST_VALIDATION_CERT:=${IDP_HOME}/metadata/eidas-servicelist-validation-cert.crt}
: ${EIDAS_METADATA_URL:=https://eid.svelegtest.se/nodeconfig/metadata}
: ${EIDAS_METADATA_VALIDATION_CERT:=$IDP_HOME/metadata/eidas-metadata-validation-cert.crt}
: ${EIDAS_METADATA_IGNORE_SIGNATURE_VALIDATION:=false}

#
# Log settings
#

# Log settings may be overridden by setting IDP_LOG_SETTINGS_FILE to point to a logback include file. 
: ${IDP_LOG_SETTINGS_FILE:=""}

: ${IDP_SYSLOG_PORT:=514}

IDP_AUDIT_APPENDER=IDP_AUDIT
IDP_FTICKS_APPENDER=IDP_PROCESS
IDP_SYSLOG_HOST_INT=localhost

if [ -n "$IDP_SYSLOG_HOST" ]; then
  IDP_AUDIT_APPENDER=IDP_AUDIT_SYSLOG
  IDP_FTICKS_APPENDER=IDP_FTICKS
  IDP_SYSLOG_HOST_INT=$IDP_SYSLOG_HOST
fi

: ${IDP_FTICKS_SYSLOG_FACILITY:=AUTH}
: ${IDP_AUDIT_SYSLOG_FACILITY:=AUTH}

: ${IDP_FTICKS_ALGORITHM:=SHA-256}
: ${IDP_FTICKS_SALT:=kdssdjas987ghdasn}

: ${IDP_LOG_CONSOLE:=false}
IDP_PROCESS_APPENDER=IDP_PROCESS
if [ "$IDP_LOG_CONSOLE" = true ]; then
  IDP_PROCESS_APPENDER=CONSOLE
fi

#
# Devel only
#

: ${IDP_DEVEL_MODE:=false}
: ${DEVEL_TEST_SP_METADATA:=https://docker.for.mac.localhost:8443/svelegtest-sp/metadata/all-metadata.xml}

if [ "$IDP_DEVEL_MODE" == "true" ]; then
  IDP_METADATA_RESOURCES_BEAN="shibboleth.DevelMetadataResolverResources"
fi


#
# JVM and JMX
#
: ${JVM_MAX_HEAP:=1536m}
: ${JVM_START_HEAP:=512m}
#: ${JMX_PORT:=9152}
#: ${JMX_ACCESS_FILE:=/etc/common-config/jmxremote.access}
#: ${JMX_PASSWORD_FILE:=/etc/common-config/jmxremote.password}


export JAVA_OPTS="\
          -Dtomcat.hostname=$TOMCAT_HOSTNAME \
          -Dtomcat.tls.port=$TOMCAT_TLS_PORT \
          -Dtomcat.ajp.port=$TOMCAT_AJP_PORT \
          -Dtomcat.tls.server-key=$TOMCAT_TLS_SERVER_KEY \
          -Dtomcat.tls.server-key-type=$TOMCAT_TLS_SERVER_KEY_TYPE \
          -Dtomcat.tls.server-certificate=$TOMCAT_TLS_SERVER_CERTIFICATE \
          -Dtomcat.tls.certificate-chain=$TOMCAT_TLS_SERVER_CERTIFICATE_CHAIN \
          -Djava.net.preferIPv4Stack=true \
          -Didp.home=$IDP_HOME \
          -Didp.baseurl=$IDP_BASE_URL \
          -Didp.entityID=$IDP_ENTITY_ID \
          -Didp.sealer.storeResource=$IDP_SEALER_STORE_RESOURCE \
          -Didp.sealer.password=$IDP_SEALER_PASSWORD \
          -Didp.sealer.versionResource=$IDP_SEALER_VERSION_RESOURCES \
          -Didp.signing.key=$IDP_SIGNING_KEY \
          -Didp.signing.cert=$IDP_SIGNING_CERT \
          -Didp.encryption.key=$IDP_ENCRYPTION_KEY \
          -Didp.encryption.cert=$IDP_ENCRYPTION_CERT \
          -Didp.metadata.signing.key=$IDP_METADATA_SIGNING_KEY \
          -Didp.metadata.signing.cert=$IDP_METADATA_SIGNING_CERT \
          -Didp.metadata.validity=$IDP_METADATA_VALIDITY_MINUTES \
          -Didp.metadata.cacheDuration=$IDP_METADATA_CACHEDURATION_MILLIS \
          -Didp.sp.entityID=$SP_ENTITY_ID \
          -Didp.sp.signing.key=$SP_SIGNING_KEY \
          -Didp.sp.signing.cert=$SP_SIGNING_CERT \
          -Didp.sp.encryption.key=$SP_ENCRYPTION_KEY \
          -Didp.sp.encryption.cert=$SP_ENCRYPTION_CERT \
          -Didp.sp.metadata.signing.key=$SP_METADATA_SIGNING_KEY \
          -Didp.sp.metadata.signing.cert=$SP_METADATA_SIGNING_CERT \
          -Didp.sp.metadata.validity=$SP_METADATA_VALIDITY_MINUTES \
          -Didp.sp.metadata.cacheDuration=$SP_METADATA_CACHEDURATION_MILLIS \
          -Didp.aa.url=${IDP_AA_URL} \
          -Didp.persistentId.salt.value=${IDP_PERSISTENT_ID_SALT} \
          -Didp.metadata.federation.url=${FEDERATION_METADATA_URL} \
          -Didp.metadata.federation.validation-certificate=${FEDERATION_METADATA_VALIDATION_CERT} \
          -Didp.metadata.eidas.service-list.url=${EIDAS_METADATA_SERVICE_LIST_URL} \
          -Didp.metadata.eidas.service-list.validation-certificate=${EIDAS_METADATA_SERVICE_LIST_VALIDATION_CERT} \
          -Didp.metadata.eidas.federation.url=${EIDAS_METADATA_URL} \
          -Didp.metadata.eidas.federation.validation-certificate=${EIDAS_METADATA_VALIDATION_CERT} \
          -Didp.metadata.eidas.ignore-signature-validation=${EIDAS_METADATA_IGNORE_SIGNATURE_VALIDATION} \
          -Didp.test.sp.metadata=${DEVEL_TEST_SP_METADATA} \
          -Didp.service.metadata.resources=${IDP_METADATA_RESOURCES_BEAN} \
          -Didp.log-settings.file=$IDP_LOG_SETTINGS_FILE \
          -Didp.audit.appender=$IDP_AUDIT_APPENDER \
          -Didp.syslog.host=$IDP_SYSLOG_HOST_INT \
          -Didp.syslog.facility=$IDP_AUDIT_SYSLOG_FACILITY \
          -Didp.syslog.port=$IDP_SYSLOG_PORT \
          -Didp.fticks.appender=$IDP_FTICKS_APPENDER \
          -Didp.fticks.loghost=$IDP_SYSLOG_HOST_INT \
          -Didp.fticks.facility=$IDP_FTICKS_SYSLOG_FACILITY \
          -Didp.fticks.logport=$IDP_SYSLOG_PORT \
          -Didp.fticks.algorithm=$IDP_FTICKS_ALGORITHM \
          -Didp.fticks.salt=$IDP_FTICKS_SALT \
          -Didp.consent.appender=NOOP_APPENDER \
          -Didp.warn.appender=NOOP_APPENDER \
          -Didp.process.appender=$IDP_PROCESS_APPENDER \
          ${JAVA_OPTS}"


# F-Ticks

if [ -n "$IDP_FTICKS_FEDERATION_ID" ]; then
  export JAVA_OPTS="${JAVA_OPTS} -Didp.fticks.federation=${IDP_FTICKS_FEDERATION_ID}"
fi


# TODO:
#  We probably should set 
# -Djavax.net.ssl.trustStore=${IDP_COMMON_TRUSTSTORE}
# -Djavax.net.ssl.trustStorePassword=${IDP_COMMON_TRUSTSTORE_PASSWORD}


export CATALINA_OPTS="\
          -Xmx${JVM_MAX_HEAP}\
          -Xms${JVM_START_HEAP}\
"

${TOMCAT_HOME}/bin/catalina.sh run
