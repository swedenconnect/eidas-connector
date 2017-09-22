#!/usr/bin/env bash

if [ $# -eq 1 ]; then
    myapp=$1
else
    echo "$0 Need name of app as argument"
    exit 1
fi

# Source common config
#
#for i in `\ls /etc/common-config/*.conf 2>/dev/null`
#do
#    echo sourcing $i
#    source $i
#done

# Source this app's config
#
#for i in `\ls /etc/${myapp}/env/*.conf 2>/dev/null`
#do
#    echo sourcing $i
#    source $i
#done

IDP_HOME=/opt/${myapp}/shibboleth
TOMCAT_HOME=/opt/${myapp}/tomcat

#
# Set up the IdP server URL
#
: ${IDP_SERVER_SCHEME:=https}
: ${IDP_SERVER_HOSTNAME:=localhost}
: ${IDP_SERVER_PORT:=9160}
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
: ${TOMCAT_HOSTNAME:=localhost}
: ${TOMCAT_TLS_KEYSTORE:=$TOMCAT_HOME/conf/tls-test-localhost.jks}
: ${TOMCAT_TLS_PASSWORD:=secret}
: ${TOMCAT_TLS_ALIAS:=localhost}

#
# IdP settings
#
: ${IDP_ENTITY_ID:=https://idp.svelegtest.se/idpref}

: ${IDP_CREDENTIALS:=$IDP_HOME/credentials}
: ${IDP_SEALER_STORE_RESOURCE:=$IDP_CREDENTIALS/sealer.jks}
: ${IDP_SEALER_PASSWORD:=JeiferDRIoOplYy89}
: ${IDP_SEALER_VERSION_RESOURCES:=$IDP_CREDENTIALS/sealer.kver}
: ${IDP_SIGNING_KEY:=$IDP_CREDENTIALS/idp-signing.key}
: ${IDP_SIGNING_CERT:=$IDP_CREDENTIALS/idp-signing.crt}
: ${IDP_ENCRYPTION_KEY:=$IDP_CREDENTIALS/idp-encryption.key}
: ${IDP_ENCRYPTION_CERT:=$IDP_CREDENTIALS/idp-encryption.crt}
: ${IDP_METADATA_SIGNING_KEY:=$IDP_CREDENTIALS/metadata-signing.key}
: ${IDP_METADATA_SIGNING_CERT:=$IDP_CREDENTIALS/metadata-signing.crt}

#
# SP settings
#
: ${SP_ENTITY_ID:=https://eunode.eidastest.se/sp}

: ${SP_CREDENTIALS:=$IDP_HOME/credentials/sp}
: ${SP_SIGNING_KEY:=$SP_CREDENTIALS/sp-signing.key}
: ${SP_SIGNING_CERT:=$IDP_CREDENTIALS/sp-signing.crt}
: ${SP_ENCRYPTION_KEY:=$SP_CREDENTIALS/sp-signing.key}
: ${SP_ENCRYPTION_CERT:=$IDP_CREDENTIALS/sp-signing.crt}
: ${SP_METADATA_SIGNING_KEY:=$IDP_CREDENTIALS/metadata-signing.key}
: ${SP_METADATA_SIGNING_CERT:=$IDP_CREDENTIALS/metadata-signing.crt}


TEST_SP_METADATA=https://docker.for.mac.localhost:8443/svelegtest-sp/metadata/all-metadata.xml

#: ${SSL_KEYSTORE_FILE:=`echo $IDP_SSL_KEYSTORE |sed s/file://`}

: ${JVM_MAX_HEAP:=1536m}
: ${JVM_START_HEAP:=512m}

## The IDP_ENV_FLAG is set to 'dev' for all environment except
## for production when it is set to 'p'. This variable is used
## for conditional inclusion of Spring resources.
IDP_ENV_FLAG=dev
if [ "$IDP_ENVIRONMENT" == "p" ]; then
  IDP_HOME_URL=https://bankid.litsec.se
  IDP_ENV_FLAG=p
fi

export JAVA_OPTS="\
          -Didp.home=$IDP_HOME \
          -Didp.entityID=$IDP_ENTITY_ID \
          -Didp.envflag=$IDP_ENV_FLAG \
          -Dtomcat.tls.keystore=$TOMCAT_TLS_KEYSTORE \
          -Dtomcat.tls.password=$TOMCAT_TLS_PASSWORD \
          -Dtomcat.tls.alias=$TOMCAT_TLS_ALIAS \
          -Dtomcat.tls.port=$TOMCAT_TLS_PORT \
          -Dtomcat.hostname=$TOMCAT_HOSTNAME \
          -Didp.sealer.storeResource=$IDP_SEALER_STORE_RESOURCE \
          -Didp.sealer.password=$IDP_SEALER_PASSWORD \
          -Didp.sealer.versionResource=$IDP_SEALER_VERSION_RESOURCES \
          -Didp.signing.key=$IDP_SIGNING_KEY \
          -Didp.signing.cert=$IDP_SIGNING_CERT \
          -Didp.encryption.key=$IDP_ENCRYPTION_KEY \
          -Didp.encryption.cert=$IDP_ENCRYPTION_CERT \
          -Didp.metadata.signing.key=$IDP_METADATA_SIGNING_KEY \
          -Didp.metadata.signing.cert=$IDP_METADATA_SIGNING_CERT \
          -Didp.sp.entityID=$SP_ENTITY_ID \
          -Didp.sp.signing.key=$SP_SIGNING_KEY \
          -Didp.sp.signing.cert=$SP_SIGNING_CERT \
          -Didp.sp.encryption.key=$SP_ENCRYPTION_KEY \
          -Didp.sp.encryption.cert=$SP_ENCRYPTION_CERT \
          -Didp.sp.metadata.signing.key=$SP_METADATA_SIGNING_KEY \
          -Didp.sp.metadata.signing.cert=$SP_METADATA_SIGNING_CERT \          
          -Didp.baseurl=$IDP_BASE_URL \
          -Didp.litsec.loglevel=${IDP_LITSEC_LOGLEVEL} \
          -Didp.test.sp.metadata=${TEST_SP_METADATA} \
          ${JAVA_OPTS}"

export CATALINA_OPTS="\
          -Xmx${JVM_MAX_HEAP}\
          -Xms${JVM_START_HEAP}\
"

${TOMCAT_HOME}/bin/catalina.sh run
