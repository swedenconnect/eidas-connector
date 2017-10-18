#!/usr/bin/env bash

if [ $# -eq 1 ]; then
    myapp=$1
else
    echo "$0 Need name of app as argument"
    exit 1
fi

# Source common config
#
for i in `\ls /etc/common-config/*.conf 2>/dev/null`
do
    echo sourcing $i
    source $i
done

# Source this app's config
#
for i in `\ls /etc/${myapp}/env/*.conf 2>/dev/null`
do
    echo sourcing $i
    source $i
done

IDP_HOME=/opt/eidas-connector/shibboleth

: ${SSL_KEYSTORE_FILE:=`echo $IDP_SSL_KEYSTORE |sed s/file://`}
: ${IDP_SSL_PASSWORD:=secret}
: ${IDP_SSL_KEY_ALIAS:=ssl}
: ${IDP_CREDENTIALS:=/etc/$myapp/credentials}
: ${IDP_SEALER_STORE_RESOURCE:=$IDP_CREDENTIALS/sealer.jks}
: ${IDP_SEALER_PASSWORD:=localDevelopmentSealerPassword}
: ${IDP_SEALER_VERSION_RESOURCES:=$IDP_CREDENTIALS/sealer.kver}
: ${IDP_SIGNING_KEY:=$IDP_CREDENTIALS/idp-signing.key}
: ${IDP_SIGNING_CERT:=$IDP_CREDENTIALS/idp-signing.crt}
: ${IDP_ENCRYPTION_KEY:=$IDP_CREDENTIALS/idp-encryption.key}
: ${IDP_ENCRYPTION_CERT:=$IDP_CREDENTIALS/idp-encryption.crt}
: ${IDP_METADATA_SIGNING_KEY:=$IDP_CREDENTIALS/metadata-signer.key}
: ${IDP_METADATA_SIGNING_CERT:=$IDP_CREDENTIALS/metadata-signer.crt}
: ${IDP_BASE_URL:=https://${IDP_ENVIRONMENT}-bankid.litsec.se/idp}
: ${JVM_MAX_HEAP:=1536m}
: ${JVM_START_HEAP:=512m}
: ${JMX_PORT:=9152}
: ${JMX_ACCESS_FILE:=/etc/common-config/jmxremote.access}
: ${JMX_PASSWORD_FILE:=/etc/common-config/jmxremote.password}
: ${IDP_LITSEC_LOGLEVEL:=INFO}

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
          -Didp.envflag=$IDP_ENV_FLAG \
          -Dpm.ssl.keystore=$SSL_KEYSTORE_FILE \
          -Dpm.ssl.password=$IDP_SSL_PASSWORD \
          -Dpm.ssl.alias=$IDP_SSL_KEY_ALIAS \
          -Didp.sealer.storeResource=$IDP_SEALER_STORE_RESOURCE \
          -Didp.sealer.password=$IDP_SEALER_PASSWORD \
          -Didp.sealer.versionResource=$IDP_SEALER_VERSION_RESOURCES \
          -Didp.signing.key=$IDP_SIGNING_KEY \
          -Didp.signing.cert=$IDP_SIGNING_CERT \
          -Didp.encryption.key=$IDP_ENCRYPTION_KEY \
          -Didp.encryption.cert=$IDP_ENCRYPTION_CERT \
          -Didp.metadata.signing.key=$IDP_METADATA_SIGNING_KEY \
          -Didp.metadata.signing.cert=$IDP_METADATA_SIGNING_CERT \
          -Didp.baseurl=$IDP_BASE_URL \
          -Djavax.net.ssl.trustStore=${IDP_COMMON_TRUSTSTORE} \
          -Djavax.net.ssl.trustStorePassword=${IDP_COMMON_TRUSTSTORE_PASSWORD} \
          -Dcom.sun.management.jmxremote \
          -Dcom.sun.management.jmxremote.authenticate=true \
          -Dcom.sun.management.jmxremote.password.file=${JMX_PASSWORD_FILE} \
          -Dcom.sun.management.jmxremote.access.file=${JMX_ACCESS_FILE} \
          -Dcom.sun.management.jmxremote.ssl=false \
          -Dcom.sun.management.jmxremote.rmi.port=${JMX_PORT} \
          -Dcom.sun.management.jmxremote.port=${JMX_PORT} \
          -Dpensionsmyndigheten.home.url=${IDP_HOME_URL} \
          -Didp.litsec.loglevel=${IDP_LITSEC_LOGLEVEL} \
          ${JAVA_OPTS}"

export CATALINA_OPTS="\
          -Xmx${JVM_MAX_HEAP}\
          -Xms${JVM_START_HEAP}\
"

/opt/eidas-connector/tomcat/bin/catalina.sh run
