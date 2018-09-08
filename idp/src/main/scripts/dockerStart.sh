#!/usr/bin/env bash

myapp="eidas-connector"

mkdir -p /var/log/eidas-connector

# Source this app's config
#
for i in /etc/${myapp}/*.conf; do
    echo sourcing $i
    . ${i}
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

export IDP_SERVER_SCHEME IDP_SERVER_HOSTNAME IDP_SERVER_PORT IDP_SERVER_SERVLET_NAME

export IDP_SERVER_PORT_SUFFIX=":${IDP_SERVER_PORT}"

if [ "x$IDP_SERVER_SCHEME" = "xhttps" -a "x$IDP_SERVER_PORT" = "x443" ]; then
  export IDP_SERVER_PORT_SUFFIX=""
fi

if [ "x$IDP_SERVER_SCHEME" = "xhttp" -a "x$IDP_SERVER_PORT" = "x80" ]; then
  export IDP_SERVER_PORT_SUFFIX=""
fi

export IDP_BASE_URL=${IDP_SERVER_SCHEME}://${IDP_SERVER_HOSTNAME}${IDP_SERVER_PORT_SUFFIX}/${IDP_SERVER_SERVLET_NAME}

#
# Tomcat settings
#
: ${TOMCAT_TLS_PORT:=8443}
export TOMCAT_TLS_PORT
: ${TOMCAT_AJP_PORT:=8009}
export TOMCAT_AJP_PORT
: ${TOMCAT_HOSTNAME:=localhost}
export TOMCAT_HOSTNAME

: ${TOMCAT_TLS_SERVER_KEY:=/etc/eidas-connector/credentials/tomcat/tomcat-key.pem}
: ${TOMCAT_TLS_SERVER_CERTIFICATE:=/etc/eidas-connector/credentials/tomcat/tomcat-cert.pem}
: ${TOMCAT_TLS_SERVER_CERTIFICATE_CHAIN:=/etc/eidas-connector/credentials/tomcat/tomcat-chain.pem}
: ${TOMCAT_TLS_SERVER_KEY_TYPE:=RSA} 
export TOMCAT_TLS_SERVER_KEY TOMCAT_TLS_SERVER_CERTIFICATE TOMCAT_TLS_SERVER_CERTIFICATE_CHAIN TOMCAT_TLS_SERVER_KEY_TYPE

if [ ! -f "$TOMCAT_TLS_SERVER_CERTIFICATE_CHAIN" ]; then
  export TOMCAT_TLS_SERVER_CERTIFICATE_CHAIN=$TOMCAT_HOME/conf/dummy-chain.pem
fi

: ${TOMCAT_PROXY_SHARED_SECRET_HEADER:=X-Proxy-Authenticate}
export TOMCAT_PROXY_SHARED_SECRET_HEADER

if [ -z "$TOMCAT_PROXY_SHARED_SECRET" ]; then
  echo "No proxy secret for Tomcat remote IP valve assigned (TOMCAT_PROXY_SHARED_SECRET)" >&2
  exit 1
fi

# Default is: 10/8, 192.168/16, 169.254/16, 127/8 and 172.16/12
# But unfortunately we have to use Java RegExp:s.
#
: ${TOMCAT_INTERNAL_PROXIES:="'10\.\d{1,3}\.\d{1,3}\.\d{1,3}|192\.168\.\d{1,3}\.\d{1,3}|169\.254\.\d{1,3}\.\d{1,3}|127\.\d{1,3}\.\d{1,3}\.\d{1,3}|172\.1[6-9]{1}\.\d{1,3}\.\d{1,3}|172\.2[0-9]{1}\.\d{1,3}\.\d{1,3}|172\.3[0-1]{1}\.\d{1,3}\.\d{1,3}'"}

export TOMCAT_INTERNAL_PROXIES

#
# IdP settings
#
: ${IDP_ENTITY_ID:=https://eunode.eidastest.se/eidas}
export IDP_ENTITY_ID

: ${IDP_CREDENTIALS:=/etc/eidas-connector/credentials}
: ${IDP_SEALER_STORE_RESOURCE:=$IDP_CREDENTIALS/sealer.jks}
: ${IDP_SEALER_PASSWORD:=changeme}
: ${IDP_SEALER_VERSION_RESOURCES:=$IDP_CREDENTIALS/sealer.kver}
: ${IDP_SIGNING_KEY:=$IDP_CREDENTIALS/idp-signing.key}
: ${IDP_SIGNING_CERT:=$IDP_CREDENTIALS/idp-signing.crt}
: ${IDP_ENCRYPTION_KEY:=$IDP_CREDENTIALS/idp-encryption.key}
: ${IDP_ENCRYPTION_CERT:=$IDP_CREDENTIALS/idp-encryption.crt}
: ${IDP_METADATA_SIGNING_KEY:=$IDP_CREDENTIALS/metadata-signing.key}
: ${IDP_METADATA_SIGNING_CERT:=$IDP_CREDENTIALS/metadata-signing.crt}

export IDP_CREDENTIALS IDP_SEALER_STORE_RESOURCE IDP_SEALER_PASSWORD IDP_SEALER_VERSION_RESOURCES IDP_SIGNING_KEY IDP_SIGNING_CERT IDP_ENCRYPTION_KEY IDP_ENCRYPTION_CERT IDP_METADATA_SIGNING_KEY IDP_METADATA_SIGNING_CERT

: ${IDP_PRID_SERVICE_URL:=https://prid-1.qa.sveidas.se/prid}
export IDP_PRID_SERVICE_URL

: ${IDP_PERSISTENT_ID_SALT:=jkio98gbnmklop0Pr5WTvCgh}
export IDP_PERSISTENT_ID_SALT

: ${IDP_METADATA_VALIDITY_MINUTES:=10800}
: ${IDP_METADATA_CACHEDURATION_MILLIS:=3600000}
export IDP_METADATA_VALIDITY_MINUTES IDP_METADATA_CACHEDURATION_MILLIS

# Verification that all IdP credentials are in place ...
if [ ! -f "$IDP_SEALER_STORE_RESOURCE" ]; then
	echo "$IDP_SEALER_STORE_RESOURCE does not exist" >&2
    exit 1
fi
if [ ! -f "$IDP_SIGNING_KEY" ]; then
  if [ "$IDP_PKCS11_ENABLED" == true ]; then
    IDP_SIGNING_KEY = $IDP_HOME/conf/credentials/dummy.key
  else
    echo "IdP signature key - $IDP_SIGNING_KEY - does not exist" >&2
    exit 1
  fi
fi
if [ ! -f "$IDP_SIGNING_CERT" ]; then
	echo "IdP signature certificate - $IDP_SIGNING_CERT - does not exist" >&2
  exit 1
fi

if [ ! -f "$IDP_ENCRYPTION_KEY" ]; then
  if [ "$IDP_PKCS11_ENABLED" == true ]; then
    IDP_ENCRYPTION_KEY = $IDP_HOME/conf/credentials/dummy.key
  else
    echo "IdP encryption key - $IDP_ENCRYPTION_KEY - does not exist" >&2
    exit 1  
  fi	
fi
if [ ! -f "$IDP_ENCRYPTION_CERT" ]; then
	echo "IdP encryption certificate - $IDP_ENCRYPTION_CERT - does not exist" >&2
  exit 1
fi

if [ ! -f "$IDP_METADATA_SIGNING_KEY" ]; then
  if [ "$IDP_METADATA_SIGNING_PKCS11_ENABLED" == true ]; then
    IDP_METADATA_SIGNING_KEY = $IDP_HOME/conf/credentials/dummy.key
  else  
    echo "IdP metadata signing key - $IDP_METADATA_SIGNING_KEY - does not exist" >&2
    exit 1
  fi
fi
if [ ! -f "$IDP_METADATA_SIGNING_CERT" ]; then
	echo "IdP metadata signing certificate - $IDP_METADATA_SIGNING_CERT - does not exist" >&2
  exit 1
fi

: ${IDP_ERRORS_VERBOSE:=false}
export IDP_ERRORS_VERBOSE

#
# SP settings
#
: ${SP_ENTITY_ID:=https://eunode.eidastest.se/idp/metadata/sp}
export SP_ENTITY_ID

: ${SP_CREDENTIALS:=$IDP_CREDENTIALS/sp}
export SP_CREDENTIALS

: ${SP_SIGNING_KEY:=$SP_CREDENTIALS/sp-signing.key}
: ${SP_SIGNING_CERT:=$SP_CREDENTIALS/sp-signing.crt}
: ${SP_ENCRYPTION_KEY:=$SP_CREDENTIALS/sp-encryption.key}
: ${SP_ENCRYPTION_CERT:=$SP_CREDENTIALS/sp-encryption.crt}
: ${SP_METADATA_SIGNING_KEY:=$SP_CREDENTIALS/metadata-signing.key}
: ${SP_METADATA_SIGNING_CERT:=$SP_CREDENTIALS/metadata-signing.crt}
export SP_SIGNING_KEY SP_SIGNING_CERT SP_ENCRYPTION_KEY SP_ENCRYPTION_CERT SP_METADATA_SIGNING_KEY SP_METADATA_SIGNING_CERT

: ${SP_METADATA_VALIDITY_MINUTES:=10800}
: ${SP_METADATA_CACHEDURATION_MILLIS:=3600000}
export SP_METADATA_VALIDITY_MINUTES SP_METADATA_CACHEDURATION_MILLIS

# Verification that all SP credentials are in place ...
if [ ! -f "$SP_SIGNING_KEY" ]; then
  if [ "$IDP_PKCS11_ENABLED" == true ]; then
    SP_SIGNING_KEY = $IDP_HOME/conf/credentials/dummy.key
  else
    echo "SP signature key - $SP_SIGNING_KEY - does not exist" >&2
    exit 1
  fi
fi
if [ ! -f "$SP_SIGNING_CERT" ]; then
	echo "SP signature certificate - $SP_SIGNING_CERT - does not exist" >&2
  exit 1
fi

if [ ! -f "$SP_ENCRYPTION_KEY" ]; then
  if [ "$IDP_PKCS11_ENABLED" == true ]; then
    SP_ENCRYPTION_KEY = $IDP_HOME/conf/credentials/dummy.key
  else
    echo "SP encryption key - $SP_ENCRYPTION_KEY - does not exist" >&2
    exit 1
  fi
fi
if [ ! -f "$SP_ENCRYPTION_CERT" ]; then
	echo "SP encryption certificate - $SP_ENCRYPTION_CERT - does not exist" >&2
  exit 1
fi

if [ ! -f "$SP_METADATA_SIGNING_KEY" ]; then
  if [ "$SP_METADATA_SIGNING_PKCS11_ENABLED" == true ]; then
    SP_METADATA_SIGNING_KEY = $IDP_HOME/conf/credentials/dummy.key
  else
	  echo "SP metadata signing key - $SP_METADATA_SIGNING_KEY - does not exist" >&2
    exit 1
  fi
fi
if [ ! -f "$SP_METADATA_SIGNING_CERT" ]; then
	echo "SP metadata signing certificate - $SP_METADATA_SIGNING_CERT - does not exist" >&2
  exit 1
fi

#
# Metadata
#
IDP_METADATA_RESOURCES_BEAN=shibboleth.MetadataResolverResources
if [ -n "$SECONDARY_FEDERATION_METADATA_URL" ]; then
  if [ -z "$SECONDARY_FEDERATION_METADATA_VALIDATION_CERT" ]; then
    echo "SECONDARY_FEDERATION_METADATA_VALIDATION_CERT must be set" >&2
    exit 1
  fi
  IDP_METADATA_RESOURCES_BEAN=shibboleth.MetadataResolverResources2
fi
export IDP_METADATA_RESOURCES_BEAN

: ${FEDERATION_METADATA_URL:=https://qa.md.swedenconnect.se/entities}
: ${FEDERATION_METADATA_VALIDATION_CERT:=$IDP_HOME/metadata/sc-qa-metadata-validation-cert.crt}
: ${EIDAS_METADATA_SERVICE_LIST_URL:=""}
: ${EIDAS_METADATA_SERVICE_LIST_VALIDATION_CERT:=${IDP_HOME}/metadata/eidas-servicelist-validation-cert.crt}
: ${EIDAS_METADATA_URL:=https://eid.svelegtest.se/nodeconfig/metadata}
: ${EIDAS_METADATA_VALIDATION_CERT:=$IDP_HOME/metadata/eidas-metadata-validation-cert.crt}
: ${EIDAS_METADATA_IGNORE_SIGNATURE_VALIDATION:=false}
export FEDERATION_METADATA_URL FEDERATION_METADATA_VALIDATION_CERT EIDAS_METADATA_SERVICE_LIST_URL EIDAS_METADATA_SERVICE_LIST_VALIDATION_CERT EIDAS_METADATA_URL EIDAS_METADATA_VALIDATION_CERT EIDAS_METADATA_IGNORE_SIGNATURE_VALIDATION

#
# Log settings
#

# Log settings may be overridden by setting IDP_LOG_SETTINGS_FILE to point to a logback include file. 
: ${IDP_LOG_SETTINGS_FILE:=""}
export IDP_LOG_SETTINGS_FILE

: ${IDP_SYSLOG_PORT:=514}
export IDP_SYSLOG_PORT

export IDP_AUDIT_APPENDER=IDP_AUDIT
export IDP_FTICKS_APPENDER=IDP_PROCESS
export IDP_SYSLOG_HOST_INT=localhost

if [ -n "$IDP_SYSLOG_HOST" ]; then
  export IDP_AUDIT_APPENDER=IDP_AUDIT_SYSLOG
  export IDP_FTICKS_APPENDER=IDP_FTICKS
  export IDP_SYSLOG_HOST_INT=$IDP_SYSLOG_HOST
fi

: ${IDP_FTICKS_SYSLOG_FACILITY:=AUTH}
: ${IDP_AUDIT_SYSLOG_FACILITY:=AUTH}
export IDP_FTICKS_SYSLOG_FACILITY IDP_AUDIT_SYSLOG_FACILITY

: ${IDP_FTICKS_ALGORITHM:=SHA-256}
: ${IDP_FTICKS_SALT:=kdssdjas987ghdasn}
export IDP_FTICKS_ALGORITHM IDP_FTICKS_SALT

: ${IDP_LOG_CONSOLE:=false}
export IDP_LOG_CONSOLE
export IDP_PROCESS_APPENDER=IDP_PROCESS
if [ "$IDP_LOG_CONSOLE" = true ]; then
  export IDP_PROCESS_APPENDER=CONSOLE
fi

#
# Devel only
#

: ${IDP_DEVEL_MODE:=false}
: ${DEVEL_TEST_SP_METADATA:=https://docker.for.mac.localhost:8443/svelegtest-sp/metadata/all-metadata.xml}
export IDP_DEVEL_MODE DEVEL_TEST_SP_METADATA

if [ "$IDP_DEVEL_MODE" == "true" ]; then
  export IDP_METADATA_RESOURCES_BEAN="shibboleth.DevelMetadataResolverResources"
else
  export IDP_DEVEL_MODE=false
fi


#
# JVM and JMX
#
: ${JVM_MAX_HEAP:=1536m}
: ${JVM_START_HEAP:=512m}
#: ${JMX_PORT:=9152}
#: ${JMX_ACCESS_FILE:=/etc/common-config/jmxremote.access}
#: ${JMX_PASSWORD_FILE:=/etc/common-config/jmxremote.password}

export JVM_MAX_HEAP JVM_START_HEAP

export JAVA_OPTS="\
          -Djava.net.preferIPv4Stack=true \
          -Didp.home=$IDP_HOME \
          -Didp.domain=$IDP_SERVER_HOSTNAME \
          -Didp.baseurl=$IDP_BASE_URL \
          -Didp.devel.mode=$IDP_DEVEL_MODE \
          -Didp.errors.verbose=$IDP_ERRORS_VERBOSE \
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
          -Didp.prid-service.url=${IDP_PRID_SERVICE_URL} \
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

#
# Secondary metadata source
#
if [ -n "$SECONDARY_FEDERATION_METADATA_URL" ]; then
  export JAVA_OPTS="${JAVA_OPTS} -Didp.metadata.secondary.federation.url=${SECONDARY_FEDERATION_METADATA_URL} -Didp.metadata.secondary.federation.validation-certificate=${SECONDARY_FEDERATION_METADATA_VALIDATION_CERT}"
fi

# F-Ticks

if [ -n "$IDP_FTICKS_FEDERATION_ID" ]; then
  export JAVA_OPTS="${JAVA_OPTS} -Didp.fticks.federation=${IDP_FTICKS_FEDERATION_ID}"
fi

#
# Redis
#

: ${REDIS_HOST:=""}

if [ -n "$REDIS_HOST" ]; then

  : ${REDIS_PORT:=6379}
  : ${REDIS_DATABASE:=0}
  : ${REDIS_POOL_MAX:=20}
  : ${REDIS_USE_TLS:=false}
  : ${REDIS_TIMEOUT:=5000}
  : ${REDIS_PASSWORD:=""}

  export JAVA_OPTS="${JAVA_OPTS} \
    -Didp.session.StorageService=shibboleth.RedisStorageService \
    -Didp.replayCache.StorageService=shibboleth.RedisStorageService \
    -Dredis.host=${REDIS_HOST} \
    -Dredis.port=${REDIS_PORT} \
    -Dredis.database=${REDIS_DATABASE} \
    -Dredis.tls=${REDIS_USE_TLS} \
    -Dredis.timeout=${REDIS_TIMEOUT} \
    -Dredis.password=${REDIS_PASSWORD} \
    -Dredis.pool.max-total=${REDIS_POOL_MAX}"
fi

#
# HSM support
#
: ${IDP_PKCS11_ENABLED:=false}
: ${IDP_METADATA_SIGNING_PKCS11_ENABLED:=false}
: ${SP_METADATA_SIGNING_PKCS11_ENABLED:=false}

if [ "$IDP_PKCS11_ENABLED" == true ]; then

  if [ -z "$IDP_PKCS11_LIBRARY" ]; then
    echo "IDP_PKCS11_LIBRARY must be set" >&2
    exit 1
  fi
  
  : ${IDP_PKCS11_NAME:=connector}
  : ${IDP_PKCS11_SLOT:=""}
  : ${IDP_PKCS11_SLOT_LIST_INDEX:=0}
  : ${IDP_PKCS11_SLOT_LIST_INDEX_MAX_RANGE:=-1}
  
  if [ -z "$IDP_SIGNING_PKCS11_ALIAS" ]; then
    echo "IDP_SIGNING_PKCS11_ALIAS must be set" >&2
    exit 1
  fi
  if [ -z "$IDP_SIGNING_PKCS11_PIN" ]; then
    echo "IDP_SIGNING_PKCS11_PIN must be set" >&2
    exit 1
  fi
  if [ -z "$IDP_ENCRYPTION_PKCS11_ALIAS" ]; then
    echo "IDP_ENCRYPTION_PKCS11_ALIAS must be set" >&2
    exit 1
  fi
  if [ -z "$IDP_ENCRYPTION_PKCS11_PIN" ]; then
    echo "IDP_ENCRYPTION_PKCS11_PIN must be set" >&2
    exit 1
  fi
  if [ -z "$SP_SIGNING_PKCS11_ALIAS" ]; then
    echo "SP_SIGNING_PKCS11_ALIAS must be set" >&2
    exit 1
  fi
  if [ -z "$SP_SIGNING_PKCS11_PIN" ]; then
    echo "SP_SIGNING_PKCS11_PIN must be set" >&2
    exit 1
  fi
  if [ -z "$SP_ENCRYPTION_PKCS11_ALIAS" ]; then
    echo "SP_ENCRYPTION_PKCS11_ALIAS must be set" >&2
    exit 1
  fi
  if [ -z "$SP_ENCRYPTION_PKCS11_PIN" ]; then
    echo "SP_ENCRYPTION_PKCS11_PIN must be set" >&2
    exit 1
  fi
  
  # For testing only
  : ${IDP_PKCS11_SOFTHSM_KEYLOCATION:=""}
  : ${IDP_PKCS11_SOFTHSM_PIN:=""}
  
  export JAVA_OPTS="${JAVA_OPTS} \
    -Didp.pkcs11.enabled=true \
    -Didp.pkcs11.library=$IDP_PKCS11_LIBRARY \
    -Didp.pkcs11.name=$IDP_PKCS11_NAME \
    -Didp.pkcs11.slot=$IDP_PKCS11_SLOT \
    -Didp.pkcs11.slotListIndex=$IDP_PKCS11_SLOT_LIST_INDEX \
    -Didp.pkcs11.slotListIndexMaxRange=$IDP_PKCS11_SLOT_LIST_INDEX_MAX_RANGE \
    -Didp.signing.pkcs11.alias=$IDP_SIGNING_PKCS11_ALIAS \
    -Didp.signing.pkcs11.pin=$IDP_SIGNING_PKCS11_PIN \
    -Didp.encryption.pkcs11.alias=$IDP_ENCRYPTION_PKCS11_ALIAS \
    -Didp.encryption.pkcs11.pin=$IDP_ENCRYPTION_PKCS11_PIN \
    -Didp.sp.signing.pkcs11.alias=$SP_SIGNING_PKCS11_ALIAS \
    -Didp.sp.signing.pkcs11.pin=$SP_SIGNING_PKCS11_PIN \
    -Didp.sp.encryption.pkcs11.alias=$SP_ENCRYPTION_PKCS11_ALIAS \
    -Didp.sp.encryption.pkcs11.pin=$SP_ENCRYPTION_PKCS11_PIN \
    -Didp.pkcs11.soft.keyLocation=$IDP_PKCS11_SOFTHSM_KEYLOCATION \
    -Didp.pkcs11.soft.pin=$IDP_PKCS11_SOFTHSM_PIN"
    
  
  if [ "$IDP_METADATA_SIGNING_PKCS11_ENABLED" == true ]; then

    if [ -z "$IDP_METADATA_SIGNING_PKCS11_ALIAS" ]; then
      echo "IDP_METADATA_SIGNING_PKCS11_ALIAS must be set" >&2
      exit 1
    fi
    if [ -z "$IDP_METADATA_SIGNING_PKCS11_PIN" ]; then
      echo "IDP_METADATA_SIGNING_PKCS11_PIN must be set" >&2
      exit 1
    fi
    
    export JAVA_OPTS="${JAVA_OPTS} \
      -Didp.metadata.signing.pkcs11.enabled=true \
      -Didp.metadata.signing.pkcs11.alias=$IDP_METADATA_SIGNING_PKCS11_ALIAS \
      -Didp.metadata.signing.pkcs11.pin=$IDP_METADATA_SIGNING_PKCS11_PIN"
      
  fi
  
  if [ "$SP_METADATA_SIGNING_PKCS11_ENABLED" == true ]; then

    if [ -z "$SP_METADATA_SIGNING_PKCS11_ALIAS" ]; then
      echo "SP_METADATA_SIGNING_PKCS11_ALIAS must be set" >&2
      exit 1
    fi
    if [ -z "$SP_METADATA_SIGNING_PKCS11_PIN" ]; then
      echo "SP_METADATA_SIGNING_PKCS11_PIN must be set" >&2
      exit 1
    fi
    
    export JAVA_OPTS="${JAVA_OPTS} \
      -Didp.sp.metadata.signing.pkcs11.enabled=true \
      -Didp.sp.metadata.signing.pkcs11.alias=$SP_METADATA_SIGNING_PKCS11_ALIAS \
      -Didp.sp.metadata.signing.pkcs11.pin=$SP_METADATA_SIGNING_PKCS11_PIN"

  fi

fi

#
# Truststore for TLS
#
# Given a PEM file containing certificates that should be trusted
# by TLS connections, we build a trust JKS
#
: ${IDP_TLS_TRUST_KEYSTORE_PASSWORD:=changeit}

if [ -n "$IDP_TLS_TRUSTED_CERTS" ]; then
  IDP_TLS_TRUST_KEYSTORE=${TOMCAT_HOME}/conf/tls-trust.jks
  rm -rf ${IDP_TLS_TRUST_KEYSTORE}
  
  TMP_CA_STORE=/tmp/trustedcas
  rm -rf ${TMP_CA_STORE}
  mkdir ${TMP_CA_STORE}
  
  csplit -s -f ${TMP_CA_STORE}/ca- ${IDP_TLS_TRUSTED_CERTS} '/-----BEGIN CERTIFICATE-----/'
  
  for cafile in `\ls ${TMP_CA_STORE}/* 2>/dev/null`
  do
    keytool -import -file $cafile -alias `basename $cafile` -trustcacerts -keystore ${IDP_TLS_TRUST_KEYSTORE} -storepass ${IDP_TLS_TRUST_KEYSTORE_PASSWORD} -noprompt 2>/dev/null
  done
  
  if ! [ "$(ls -A $TMP_CA_STORE)" ]; then
    echo "$IDP_TLS_TRUST_KEYSTORE contains no (valid) certificates - at least one is required" >&2
    exit 1
  fi
  
  export JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.trustStore=$IDP_TLS_TRUST_KEYSTORE -Djavax.net.ssl.trustStorePassword=$IDP_TLS_TRUST_KEYSTORE_PASSWORD"
  
else
  echo "No TLS trust set, will use system defaults"
fi

export CATALINA_OPTS="\
          -Xmx${JVM_MAX_HEAP}\
          -Xms${JVM_START_HEAP}\
          -Dtomcat.hostname=$TOMCAT_HOSTNAME \
          -Dtomcat.tls.port=$TOMCAT_TLS_PORT \
          -Dtomcat.ajp.port=$TOMCAT_AJP_PORT \
          -Dtomcat.tls.server-key=$TOMCAT_TLS_SERVER_KEY \
          -Dtomcat.tls.server-key-type=$TOMCAT_TLS_SERVER_KEY_TYPE \
          -Dtomcat.tls.server-certificate=$TOMCAT_TLS_SERVER_CERTIFICATE \
          -Dtomcat.tls.certificate-chain=$TOMCAT_TLS_SERVER_CERTIFICATE_CHAIN \
          -Dtomcat.proxy.shared-secret-header=$TOMCAT_PROXY_SHARED_SECRET_HEADER \
          -Dtomcat.proxy.shared-secret=$TOMCAT_PROXY_SHARED_SECRET \
          -Dtomcat.internal-proxies=$TOMCAT_INTERNAL_PROXIES \
"

${TOMCAT_HOME}/bin/catalina.sh run
