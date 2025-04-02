![Logo](images/sweden-connect.png)

# Configuration of the Swedish eIDAS Connector

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

## Table of Contents

1. [**Overview**](#overview)

    1.1. [Configuration References](#configuration-references)
    
    1.2. [Sensitive Configuration Settings](#sensitive-configuration-settings)

2. [**Server Configuration**](#server-configuration)

    2.1. [TLS Server Credentials](#tls-server-credentials)

    2.1.1. [Configuring using KeyStore or PKCS#12 Files](#configuring-using-keystore-or-pkcs12-files)
    
    2.1.2. [Configuring using PEM Files](#configuring-using-pem-files)

    2.2. [Configuring Certificate Trust](#configuring-certificate-trust)

    2.3. [Credential Bundles](#credential-bundles)

    2.4. [Web Server Settings](#web-server-settings)
    
    2.4.1. [Base Settings](#base-settings)

    2.4.2. [Enabling TLS](#enabling-tls)

    2.4.3. [Tomcat Settings](#tomcat-settings)

    2.5. [Redis Configuration](#redis-configuration)
    
    2.6. [Logging Configuration](#logging-configuration)
    
    2.7. [The Management Endpoints](#the-management-endpoints)
    
3. [**SAML Identity Provider Configuration**](#saml-identity-provider-configuration)

    3.1. [Metadata Provider Configuration](#idp-metadata-provider-configuration)

    3.1.1. [Sweden Connect Environments](#sweden-connect-environments)

    3.2. [Credentials Configuration](#credentials-configuration)
    
    3.2.1. [Key Rollover](#key-rollover)
    
    3.2.2. [PKCS#11 and HSM:s](#pkcs11-and-hsms)

    3.3. [Audit Logging Configuration](#audit-logging-configuration)
    
    3.1.1. [Audit Logging to Syslog](#audit-logging-to-syslog)

4. [**eIDAS Connector Configuration**](#eidas-connector-configuration)

    4.1. [Base Settings for the Connector](#base-settings-for-the-connector)

    4.2. [Additional Identity Provider Settings](#additional-identity-provider-settings)
    
    4.3. [eIDAS Authentication Configuration](#eidas-authentication-configuration)
    
    4.3.1. [Service Provider Credentials Configuration](#service-provider-credentials-configuration)
    
    4.3.2. [Service Provider Metadata Configuration](#service-provider-metadata-configuration)
    
    4.3.3. [EU Metadata Configuration](#eu-metadata-configuration)

    4.3.4. [PRID Configuration](#prid-configuration)

    4.4. [Identity Matching Configuration](#identity-matching-configuration)

---

<a name="overview"></a>
## 1. Overview

The Swedish eIDAS Connector is a Spring Boot application and its configuration is supplied using a set of YAML-files. This document provides a step-through of the required settings for setting up the application.

The application contains a base [application.yml](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/resources/application.yml) file containing base configuration for settings that are common for all deployments and are unlikely to change.

When deploying the connector for a specific environment, a Spring profile is used, and a corresponding `application-<profile-name>.yml` file is created. This file extends, and overrides, the settings from the base `application.yml` file. 

<a name="configuration-references"></a>
### 1.1. Configuration References

Even though this guide should supply sufficient information for how to configure the eIDAS Connector, additional information about detailed configuration settings may be needed. Therefore, a listing of configuration resources are supplied below:

- [Spring Boot Common Application Properties](https://docs.spring.io/spring-boot/appendix/application-properties/index.html) - A listing of all Spring Boot base properties.

- [Sweden Connect - SAML Identity Provider Configuration and Deployment](https://docs.swedenconnect.se/saml-identity-provider/configuration.html) - The Swedish eIDAS Connector is built using the [Spring Security SAML Identity Provider](https://github.com/swedenconnect/saml-identity-provider) open source libraries.

- [Configuration Reference for the Swedish eIDAS Connector](https://docs.swedenconnect.se/eidas-connector/configuration-reference.html) - A complete configuration reference for the eIDAS Connector.

<a name="sensitive-configuration-settings"></a>
### 1.2. Sensitive Configuration Settings

Some settings in the application file are sensitive (such as passwords), and we may not want to store those in cleartext in the YAML-file. Depending how the application is installed there are a number of mechanisms for handling sensitive settings settings such as Sealed Secrets or Vaults for Kubernetes or Secrets for Docker Compose.

Independently how sensitive settings are protected, Spring offers a few ways to externalize the configuration - See <https://docs.spring.io/spring-boot/reference/features/external-config.html>.

Suppose the we want to avoid having the password for our TLS keystore (see section [2.1.1](#configuring-using-keystore-or-pkcs12-files) below). In our case the configuration setting is `spring.ssl.bundle.jks.connector.keystore.password`.

**Environment variables**

By defining an environment variable where the same setting is in uppercase letters and `.` are replaced with `_`, Spring will pick this value up when starting the application.

```
export SPRING_SSL_BUNDLE_JKS_CONNECTOR_KEYSTORE_PASSWORD=secret
```

:exclamation: If the application setting contains a hyphen (`-`) this is ignored when transforming to environment variable format. So, the setting `foo-bar.value` is translated into `FOOBAR_VALUE`.

**Java Start-up Parameters**

If the start-script for the application is well protected, sensitive settings can be supplied that was. Example:

```
java -Dspring.ssl.bundle.jks.connector.keystore.password=secret -jar eidas-connector.jar
```

<a name="server-configuration"></a>
## 2. Server Configuration

This section covers the configuration settings for the actual server component including TLS, context paths, management, log settings, and such.

<a name="tls-server-credentials"></a>
### 2.1. TLS Server Credentials

To configure TLS server credentials for the eIDAS Connector, we follow Spring's [SSL Configuration Guidelines](https://docs.spring.io/spring-boot/reference/features/ssl.html#features.ssl).

For configuring a server TLS credential (private key and certificate), the following options are available:

- Suplying a Java KeyStore (JKS) containing a key pair (and certificate path),
- supplying a PKCS#12 file containing a key pair (and certificate path),
- supplying PEM-encoded certificates and private keys.

See section [2.4.2](#enabling-tls), [Enabling TLS](#enabling-tls), below, for how to configure the built in Tomcat container to using the configured TLS credentials.

<a name="configuring-using-keystore-or-pkcs12-files"></a>
#### 2.1.1. Configuring using KeyStore or PKCS#12 Files

Below is an example of how we configure the TLS bundle (credential) when the credential is stored in a Java KeyStore.

```yaml
spring:
  ssl:
    bundle:
      jks:
1.      connector:
2.        reload-on-update: true
          keystore:
3.          location: file:/etc/config/ssl/tls.jks
4.          password: secret
5.          type: JKS
          key:
6.          alias: tls
7.          password: secret
```

1. The name of the bundle. This name will be used later when enabling TLS, see [Enabling TLS](#enabling-tls) below. In the example, the name **connector** was chosen.
2. By setting the `reload-on-update` to `true` the embedded Tomcat will automatically reload key material if the KeyStore file changes.<br /><br />Note: Only enable this feature if you change key material regularly, for example, if Let's Encrypt is used.
3. The location of the JKS (or PKCS#12) file. Must be prefixed with `file:`.
4. The password to unlock the above file. See also section [1.2](#sensitive-configuration-settings), [Sensitive Configuration Settings](#sensitive-configuration-settings).
5. The type of file used - `JKS` for Java KeyStore files and `PKCS12` for PKCS#12 files. 
6. The alias to the key pair within the JKS/PKCS#12 file.
7. The password to unlock the above key (is usually the same as the keystore password). See also section [1.2](#sensitive-configuration-settings), [Sensitive Configuration Settings](#sensitive-configuration-settings).

<a name="configuring-using-pem-files"></a>
#### 2.1.2. Configuring using PEM Files

Below is an example of how we configure the TLS bundle (credential) when we have the server key and certificate in PEM format.

```yaml
spring:
  ssl:
    bundle:
      pem:
1.      connector:
2.        reload-on-update: true
          keystore:
3.          certificate: file:/etc/config/ssl/tls.crt
4.          private-key: file:/etc/config/ssl/tls.key
5.          private-key-password: secret
```

1. The name of the bundle. This name will be used later when enabling TLS, see [Enabling TLS](#enabling-tls) below. In the example, the name **connector** was chosen.
2. By setting the `reload-on-update` to `true` the embedded Tomcat will automatically reload key material if the KeyStore file changes.<br /><br />Note: Only enable this feature if you change key material regularly, for example, if Let's Encrypt is used.
3. The location of the file holding the TLS server certificate (or certificate chain). Must be prefixed with `file:`.
4. The location of the file holding the PEM-encoded private key for the TLS credential. Must be prefixed with `file:`.
5. If the above key file is protected by a password, this setting holds this password to unlock the key.
See also section [1.2](#sensitive-configuration-settings), [Sensitive Configuration Settings](#sensitive-configuration-settings).

<a name="configuring-certificate-trust"></a>
### 2.2. Configuring Certificate Trust

Depending on how the eIDAS Connector is configured, and which features that are enabled, certificate trust for the connector may have to be configured. This is done in the same manner as above.

Examples:

```yaml
spring:
  ssl:
    bundle:
      pem:
        idmtrust:
          truststore:
            certificate: file:/etc/config/idm/trusted.pem
```

The example above illustrates how a trust bundle named **idmtrust** is created and it points at the `trusted.pem` file containing one (or several) PEM encoded certificate(s).

```yaml
spring:
  ssl:
    bundle:
      idmtrust:
        truststore:
          location: file:/etc/config/idm/trust.jks
          password: secret
          type: JKS
```

Example of how a JKS containing trusted certificates are configured.

:point_right: If no certificate trust is specified, the default trust list from the Java environment will be used.

<a name="credential-bundles"></a>
### 2.3. Credential Bundles

Section [2.1](#tls-server-credentials), [TLS Server Credentials](#tls-server-credentials), described how Spring's [SSL Bundles](https://docs.spring.io/spring-boot/reference/features/ssl.html) is used to configure credentials for TLS. Apart from TLS credentials, the connector uses credentials since it acts both as a SAML IdP and a SAML SP. These credentials are configured using [Credential Bundles](https://docs.swedenconnect.se/credentials-support/#credential-bundles-and-configuration-support).

A typical configuration of a credential bundle may look something like:

```yaml
credential:
  bundles:
    keystore:
      idp-keystore:
        location: file:/opt/keys/idp.p12
        password: secret
        type: PKCS12
    jks:
      sign:
        store-reference: idp-keystore
        name: "IdP Signing"
        key:
          alias: sign
          key-password: secret
      oauth2:
        store-reference: idp-keystore
        name: "IdP OAuth2 Credential"
        key:
          alias: oauth2
          key-password: secret
    pem:
      encrypt:
        certificates: file:/opt/keys/idp-encrypt.crt
        private-key: file:/opt/keys/idp-encrypt.key
        name: "IdP Encryption"
```

The above example illustrates how we can configure credentials both from key stores (JKS, PKCS#12 or PKCS#11) and from PEM-encoded files.

The bundle can later be referenced in the configuration where a specific credential is needed.

:raised_hand: Go to the [Credentials Support](https://docs.swedenconnect.se/credentials-support/#credential-bundles-and-configuration-support) pages to read more about the configuration of credentials bundles and how a PKCS#11 credential is set up. This resource also describes "credential monitoring" which is important when HSM:s are being used.

<a name="web-server-settings"></a>
### 2.4. Web Server Settings

The eIDAS Connector application is running as a web application, and under Spring's `server` key, general settings for the web server is configured.

<a name="base-settings"></a>
#### 2.4.1. Base Settings

The default Spring server settings are as follows:

```yaml
server:
  port: 8443
  servlet:
    context-path: /idp
    session:
      timeout: 60m
      tracking-modes:
        - cookie
      cookie:
        name: EIDASSESSION
        domain: ${connector.domain}
        max-age: 60m
        same-site: NONE
        http-only: true
        secure: true
```

The `server.port` property is set to 8443 as default, since we expect the application to run within a container and in those cases it is common to use 8443 (and map it to 443). Other properties should not have to be changed unless there are very specific reasons to do so.

See [Spring Boot Common Application Properties - Server Settings](https://docs.spring.io/spring-boot/appendix/application-properties/index.html#appendix.application-properties.server) for additional server settings.

<a name="enabling-tls"></a>
#### 2.4.2. Enabling TLS

In order to enable server TLS the following needs to be present:

```yaml
server:
  ...
  ssl:
    enabled: true
    bundle: connector
```

Make sure to reference the bundle configured. See section [2.1](#tls-server-credentials), [TLS Server Credentials](#tls-server-credentials), above.


<a name="tomcat-settings"></a>
#### 2.4.3. Tomcat Settings

The Spring default settings for the embedded Tomcat server is sensible, and should not have to be changed. However, if access logging is required, or if the maximum size of requests sizes are to be changed, or any other detail, check the `server.tomcat.*` settings in [Spring Boot Common Application Properties - Server Settings](https://docs.spring.io/spring-boot/appendix/application-properties/index.html#appendix.application-properties.server).

Some defaults that are good to know about:

```yaml
server:
  tomcat:
    ...
    remoteip:
      host-header: X-Forwarded-Host
      port-header: X-Forwarded-Port
      remote-ip-header: X-Forwarded-For
      ...
    accesslog:
      enabled: false
      directory: logs
      ...
```	

If the deployment uses an Apache server in front of the eIDAS connector application, and the [AJP - Apache JServ Protocol](https://tomcat.apache.org/connectors-doc/ajp/ajpv13a.html) is used a few properties need to be assigned under the `server.tomcat.ajp.*` property.

```yaml
server:
  tomcat:
    ajp:
      enabled: true
      port: 8009
      secret-required: true
      secret: <the secret>   # The same secret as used by Apache
```

<a name="redis-configuration"></a>
### 2.5. Redis Configuration

If more than one instance of the eIDAS Connector is deployed, it is recommended to use Redis for storing session data. For this purpose, Spring's Redis features need to be configured. 

In its simplest form it would look like:

```yaml
spring:
  data:
    redis:
      host: redis.example.com
      port: 6379
      password: <pwd>
      ssl:
        enabled: true
        bundle: redis-tls-trust
```

The above example illustrates how we use a [SSL Trust Bundle](#configuring-certificate-trust) for the TLS connection against the Redis server.

For details about Redis configuration, see the [Identity Provider Configuration and Deployment](https://docs.swedenconnect.se/saml-identity-provider/configuration.html#redis-configuration) documentation.

<a name="logging-configuration"></a>
### 2.6. Logging Configuration

Section [3.3](#audit-logging-configuration), [Audit Logging Configuration](#audit-logging-configuration)  covers how the audit logging of the eIDAS Connector is configured, but in general, we want an application to produce ordinary logs as well. These logs can be used for error analysis, detailed monitoring and much more.

The Spring reference page about [Logging](https://docs.spring.io/spring-boot/reference/features/logging.html) gives a complete configuration reference of how to set up logging.

The default logging settings are as follows:

```yaml
logging:
  include-application-name: false
  level:
    root: warn
    se.swedenconnect: info
```

This means that logging is only performed to the console, and that all log producers are filtered for log level "warning", except for those under the `se.swedenconnect` package who are filtered for log level "info". The `include-application-name` that is set to `false` tells Spring not to include the configured application name in each log entry. 

It is also possible to change log levels, or even add specific log levels for a certain package, using environment variables in the same was as described in section [1.2](#sensitive-configuration-settings) above.

```
LOGGING_LEVEL_SE_SWEDENCONNECT=DEBUG
LOGGING_LEVEL_SE_SWEDENCONNECT_EIDAS=TRACE
```

By setting the above environment variables and restarting the application, we change the log level for `se.swedenconnect` to "debug" and also add even more fine grained trace logging for `se.swedenconnect.eidas`.

To configure logging to file:

```yaml
logging:
  level:
    root: warn
    ...
  file:
    name: /var/log/connector.log
```

To set up file rotation:

```yaml
logging:
    ...
  file:
    name: /var/log/connector.log
  logback:
    rollingpolicy:
      file-name-pattern: connector-%d{yyyy-MM-dd}.log 
      max-file-size: 10M
```

To make more advances changes to log entries, see the Spring reference page about [Logging](https://docs.spring.io/spring-boot/reference/features/logging.html).

<a name="the-management-endpoints"></a>
### 2.7. The Management Endpoints

The Spring resource [Monitoring and Management Over HTTP](https://docs.spring.io/spring-boot/reference/actuator/monitoring.html) contains a reference how the Spring Boot Actuator should be configured.

The default settings for the eIDAS Connector are as follows:

```yaml
management:
  server:
    port: 8444
  endpoint:
    health:
      status:
        order:
          - DOWN
          - OUT_OF_SERVICE
          - UP
          - WARNING
          - UNKNOWN
        http-mapping:
          WARNING: 503
      show-details: always
    info:
      enabled: true
  endpoints:
    web:
      exposure:
        include: info, health, metrics, loggers, refreshprid, logfile, auditevents
```

By default, the management endpoints are exposed under `/actuator/<endpoint>`. This can be changed using the `management.endpoints.web.base-path` setting.

See the [Management using the Actuator](https://docs.swedenconnect.se/eidas-connector/starting-and-running.html#management-using-the-actuator) section of the [Starting and Running the Swedish eIDAS Connector](https://docs.swedenconnect.se/eidas-connector/starting-and-running.html) page for a description of each exposed management endpoint.

:raised_hand: Redis is available in the classpath, so if you are not using Redis make sure to disable the Redis health check by setting the `management.health.redis.enabled` setting to `false`.

:exclamation: Make sure not to expose the management endpoints publicly. See [Starting and Running the Swedish eIDAS Connector](https://docs.swedenconnect.se/eidas-connector/starting-and-running.html) for deployment details.

<a name="saml-identity-provider-configuration"></a>
## 3. SAML Identity Provider Configuration

The Swedish eIDAS Connector is built upon the [Spring Security SAML Identity Provider](https://github.com/swedenconnect/saml-identity-provider) libraries, and the resource [Identity Provider Configuration and Deployment](https://docs.swedenconnect.se/saml-identity-provider/configuration.html) contains a complete reference of how to configure the Identity Provider.

This section will only cover the very few IdP-settings that need to be changed given the default values. By default, we assume that the eIDAS Connector will be installed in a Sweden Connect environment, and that it will be acting as the official Swedish eIDAS Connector (for customized deployments, additional changes will have to be made).

The default IdP configuration can be viewed under the `saml.idp`-key in the [application.yml](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/resources/application.yml). This configuration needs to be extended with the following settings:

- `saml.idp.entity-id` - The unique SAML entityID that the IdP-part of the eIDAS Connector should have in the federation.

- `saml.idp.metadata-providers.*` - Configuration for how the eIDAS Connector IdP downloads trusted SAML metadata for the federation. See section [Metadata Provider Configuration](#idp-metadata-provider-configuration) below.

- `saml.idp.credentials.*` - Configuration for the credentials (i.e., keys and certificates) used by the IdP. See [Credentials Configuration](#credentials-configuration) below.

- `saml.idp.audit.*` - Audit configuration for the application. See [Audit Logging Configuration](#audit-logging-configuration) below.

<a name="idp-metadata-provider-configuration"></a>
### 3.1. Metadata Provider Configuration

To configure the federation metadata provider for the IdP-part of the eIDAS Connector (i.e., the part facing the Swedish federation) we need to provide the following:

- `location` - The URL to from where we download metadata.
- `backup-location` - A pointer to a file where the connector caches downloaded metadata (makes the IdP more resilient against temporary network problems).
- `validation-certificate` - The certificate to use when validating the signature of the downloaded metadata.

Optionally, the `https-trust-bundle` property may be set to a trust bundle (see [2.2](#configuring-certificate-trust) above). If not set, the TLS trust will be the same as the Java environment's.

Example:

```yaml
saml:
  idp:
    ...
    metadata-providers:
      - location: https://md.swedenconnect.se/role/sp.xml
        backup-location: ${connector.backup-directory}/metadata/sc-cache.xml
        validation-certificate: file:/etc/config/metadata/sc-metadata-signing.crt
```

More than one metadata provider can be configured. This may be useful to allow additional Service Providers (not in the federation) to use the connector.

See the [Metadata Provider Configuration](https://docs.swedenconnect.se/saml-identity-provider/configuration.html#metadata-provider-configuration) section of the [Identity Provider Configuration and Deployment](https://docs.swedenconnect.se/saml-identity-provider/configuration.html) page for details.

<a name=sweden-connect-environments"></a>
#### 3.1.1. Sweden Connect Environments

- The web page https://www.swedenconnect.se/anslut/saml-metadata contains URL:s and certificates for Sweden Connect Production and QA environments.

- The web page https://eid.svelegtest.se/mdreg/home contains URL:s and certificates for the Sweden Connect Sandbox environment.

:point_right: The IdP is only interested in Service Provider metadata, so make sure to use the URL:s exposing SP metadata only.

<a name="credentials-configuration"></a>
### 3.2. Credentials Configuration

The IdP uses PKI credentials (private keys and certificates) in the following scenarios:

- To sign SAML responses, and possibly also SAML assertions.

- To decrypt encrypted data passed from the Service Provider. This happens when the SP passes an encrypted  `SignMessage` extension in an authentication request.

- To sign the SAML metadata that it exposes.

See the [Credentials Configuration](https://docs.swedenconnect.se/saml-identity-provider/configuration.html#credentials-configuration) section of the [Identity Provider Configuration and Deployment](https://docs.swedenconnect.se/saml-identity-provider/configuration.html) page for details on how to configure each credential.

:point_right: It is possible to use the same credential for several purposes, and by assigning the `saml.idp.credentials.default-credential.*` property, this will be used if a specific purpose is not assigned.

Example where the [Credential Bundles](#credential-bundles) are referenced (this is the recommended way of configuring credentials):

```yaml
saml:
  idp:
    ...
    credentials:
      sign:
        bundle: idp-sign
      encrypt:
        bundle: idp-encrypt
      metadata-sign:
        bundle: md-sign
```

It is also possible to configure each credential "inline":

```yaml
saml:
  idp:
    ...
    credentials:
      sign:
        name: "IdP Signing"
        pem:
          certificates: file:/etc/config/keys/sign.crt
          private-key: file:/etc/config/keys/sign.key
          key-password: <password>
      encrypt:
        name: "IdP Encrypt
        pem:
          certificate: file:/etc/config/keys/encrypt.crt
          private-key: file:/etc/config/keys/encrypt.key
```

The example above also illustrates that an encrypted key is used for the signing key, and therefore the password to unlock this file needs to be supplied.

<a name="key-rollover"></a>
#### 3.2.1. Key Rollover

Periodically, an IdP's keys need to changed, and in order for such a "key rollover" to cause minimal disturbance within the federation it is essential that the IdP plans ahead.

When the IdP is about to change its signing key, it should make sure that the new certificate for the new key is published in its metadata several weeks before the actual change is made. This is so that the SAML Service Providers of the federation is given enough time to download updated metadata (containing both the present and the future certificate for signing).

In order for the eIDAS connector to produce SAML metadata containing the new signing certificate the following needs to be configured:

```yaml
credential:
  bundles:
    ...
    pem:
      sign:
        certificates: file:/opt/keys/idp-sign.crt
        private-key: file:/opt/keys/idp-sign.key
        name: "IdP Sign"
      sign2:
        certificates: file:/opt/keys/idp-sign2.crt
        private-key: file:/opt/keys/idp-sign2.key
        name: "IdP Sign 2"

saml:
  idp:
    ...
    credentials:
      sign:
        bundle: sign
      ...
      future-sign: file:/opt/keys/idp-sign2.crt
      ...
```

In the example above, the `future-sign` property points at the certificate resource that is the certificate for the new signing certificate. After the actual change has been made, change to configuration to:

```yaml
saml:
  idp:
    ...
    credentials:
      sign:
        bundle: sign2
      # future-sign no longer needed
```

When the IdP changes its encryption key (actually, decryption key would be a better name), we don't have to advertise anything in advance, but instead allow the "old" key to be used for a period (until all SP:s have downloaded the new metadata and have access to the updated certificate).

Therefore, when changing the encryption key, the following needs to be configured:

```yaml
credential:
  bundles:
    ...
    pem:
      encrypt:
        certificates: file:/opt/keys/idp-encrypt.crt
        private-key: file:/opt/keys/idp-encrypt.key
        name: "IdP Encrypt"
      encrypt2:
        certificates: file:/opt/keys/idp-encrypt2.crt
        private-key: file:/opt/keys/idp-encrypt2.key
        name: "IdP Encrypt 2"
        
saml:
  idp:
    ...
    credentials:
      ...
      encrypt:
        bundle: encrypt2
      previous-encrypt:
        bundle: encrypt
```

When enough time has passed (i.e., until all SP:s have access to the new key), the `previous-encrypt` property may be removed from the configuration (and also the `encrypt` bundle).


<a name="pkcs11-and-hsms"></a>
#### 3.2.2. PKCS#11 and HSM:s

A credential being used by the IdP may also reside on a Hardware Security Module (HSM) and be accessed using the PKCS#11 protocol. The [Credentials Support](https://docs.swedenconnect.se/credentials-support/) library explains in detail how this works.

Below is an example of where we configure an IdP key to use a key residing on an HSM:

```yaml
credential:
  bundles:
    jks:
      hsm:
        name: "IdP HSM Key"
        store:
          type: PKCS11
          provider: SunPKCS11
          password: 1234
          pkcs11:
            configuration-file: file:/etc/config/keys/p11.conf
        key:
          alias: idp
          key-password: 1234
          certificates: file:/etc/config/keys/p11-cert.crt
        monitor: true
    ...
    monitoring:
      enabled: true
      test-interval: 10m
      health-endpoint-enabled: true    

saml:
  idp:
    ...
    credentials:
      sign:
        bundle: hsm
      encrypt:
        bundle: hsm
``` 

The references PKCS#11 configuration file should be formatted according to [PKCS#11 Reference Guide](https://docs.oracle.com/en/java/javase/17/security/pkcs11-reference-guide1.html).

:point_right: If the certificate is accessible from the HSM, it does not need to be configured.

:raised_hand: Also note how we configure monitoring of the HSM credential. This will periodically test, and if connection against the HSM has been lost, reload the credential. Read more about monitoring of credentials [here](https://docs.swedenconnect.se/credentials-support/#monitoring).


<a name="audit-logging-configuration"></a>
### 3.3. Audit Logging Configuration

The default is to only hold audit entries in memory and expose them via the [Spring Boot Actuator](https://www.baeldung.com/spring-boot-actuators) endpoint for audit (see [Accessing Audit Logs](https://docs.swedenconnect.se/eidas-connector/starting-and-running.html#accessing-audit-logs)). In a production environment we probably want to persist audit entries.

Section [Audit Configuration](https://docs.swedenconnect.se/saml-identity-provider/configuration.html#audit-configuration) for the [Identity Provider Configuration and Deployment](https://docs.swedenconnect.se/saml-identity-provider/configuration.html) contains a full configuration reference.

By default, the IdP audit configuration looks like:

```yaml
saml:
  idp:
    ...
    audit:
      in-memory:
        capacity: 1000
```

So, for audit logging to file, add the following:

```yaml
saml:
  idp:
    ...
    audit:
      in-memory:
        capacity: 1000
      file:
        log-file: file:/etc/var/logging/connector-audit.log
```

The audit logger will now also write to the `connector-audit.log` file. It uses a rolling file appender that creates a new log file every day, and saves the old ones as `<file-name>-<date>.<ext>`.

In the example above we keep the in-memory logging which can be a good idea to allow for the management endpoints to view log files, see [Accessing Audit Logs](https://docs.swedenconnect.se/eidas-connector/starting-and-running.html#accessing-audit-logs).

It is also possible to persist audit entries to Redis:

```yaml
saml:
  idp:
    ...
    audit:
      in-memory:
        capacity: 1000
      redis:
        name: "connector-audit"
        type: list
```

When using Redis for audit logging, Spring's Redis support must also be configured, see section [2.5](#redis-configuration), [Redis Configuration](#redis-configuration).

The Swedish eIDAS Connector uses [Logback](https://logback.qos.ch) as the underlying log framework, and it is possible to configure audit logging to be directed to this log system.

```yaml
saml:
  idp:
    ...
    audit:
      in-memory:
        capacity: 1000
      log-system:
        logger-name: "AUDIT"

...

logging:
  level:
    ...       
  config: file:/opt/logs/logback.xml

```

By providing a `logback.xml` file placed according to the `logging.config` property, an specific appender can be added, for example for Syslog - see [Logback Syslog Appender](https://logback.qos.ch/manual/appenders.html#SyslogAppender). See full details in section [3.1.1](#audit-logging-to-syslog) below.

:grey_exclamation: See also the page [Swedish eIDAS Connector Audit Logging](https://docs.swedenconnect.se/eidas-connector/audit.html) for a full reference for all audit events produced by the eIDAS Connector.

<a name="audit-logging-to-syslog"></a>
#### 3.1.1. Audit Logging to Syslog

In order to audit log to a Syslog server, the following steps need to be taken:

Create a `logback.xml` file according to the following:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

  <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
  <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>
  <include resource="org/springframework/boot/logging/logback/file-appender.xml"/>

  <springProperty scope="context" name="appName" source="spring.application.name" defaultValue="eIDAS Connector"/>

  <appender name="SYSLOG" class="ch.qos.logback.classic.net.SyslogAppender">
    <syslogHost>replace-with-your-syslog-host</syslogHost>
    <port>514</port>
    <facility>USER</facility>
    <suffixPattern>%msg</suffixPattern>
    <throwableExcluded>true</throwableExcluded>
  </appender>

  <root level="info">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
  </root>

  <logger name="AUDIT" level="info" additivity="false">
    <appender-ref ref="SYSLOG"/>
  </logger>

</configuration>
```

The file can also be downloaded [here](logback.xml). Make sure to assign `syslogHost` with the correct hostname.

Should you want several Syslog servers, simply create sevaral appenders, with different names and configurations, and add them under the audit logger.

```xml
  <appender name="SYSLOG1" class="ch.qos.logback.classic.net.SyslogAppender">
    ...
  </appender>
  
  <appender name="SYSLOG2" class="ch.qos.logback.classic.net.SyslogAppender">
    ...
  </appender>

  ...

  <logger name="AUDIT" level="info" additivity="false">
    <appender-ref ref="SYSLOG1"/>
    <appender-ref ref="SYSLOG2"/>
  </logger>
```

Next, point at this new `logback.xml` file in your connector configuration:

```yaml
logging:
  level:
    ...       
  config: file:/opt/logs/logback.xml
```

Finally, configure the audit logging to use log system logging (as described above):

```yaml
saml:
  idp:
    ...
    audit:
      ...
      log-system:
        logger-name: "AUDIT"
```

:grey_exclamation: The `logger-name` must correspond to the Syslog logger name given in the `logback.xml` file.


<a name="eidas-connector-configuration"></a>
## 4. eIDAS Connector Configuration

The previous chapter describes how the SAML Identity Provider-part of the eIDAS Connector is configured. This section will describe how the application's parts that integrate against the eIDAS framework is configured, and how we handle identities received from the foreign country's IdP.

<a name="base-settings-for-the-connector"></a>
### 4.1. Base Settings for the Connector

The base settings for the connector are:

- `connector.domain` - The domain for the service, for example `connector.eidas.swedenconnect.se`.

- `connector.country` - The country code for the connector. The default is `SE`.

- `connector.base-url` - The base URL of the Connector, including protocol, domain and context path. The default which is `https://${connector.domain}${server.servlet.context-path}` should be correct for all deployments except for local installations.

- `connector.backup-directory` - Directory where caches and backup files are stored during execution.

See the [eIDAS Connector Configuration](configuration-reference.html#eidas-connector-configuration) section of the [Configuration Reference for the Swedish eIDAS Connector](configuration-reference.html) for details.

<a name="additional-identity-provider-settings"></a>
### 4.2. Additional Identity Provider Settings

The configuration properties under `saml.idp` are extended with some additional configuration settings for the Identity Provider part of the Connector. See the [Connector IdP Configuration](configuration-reference.html#connector-idp-configuration) of the [Configuration Reference for the Swedish eIDAS Connector](configuration-reference.html).

<a name="eidas-authentication-configuration"></a>
### 4.3. eIDAS Authentication Configuration

The [eIDAS Authentication Configuration](configuration-reference.html#eidas-authentication-configuration) section of the [Configuration Reference for the Swedish eIDAS Connector](configuration-reference.html) documents the configuration settings for how eIDAS authentication is performed, i.e., how the SAML Service Provider part of the Connector is set up.

The `connector.eidas.entity-id` setting is assigned the SAML entityID of the SAML Service Provider entityID used by the Connector in eIDAS authentications. Care should be taken if changing this value from its defaults since many eIDAS countries expect the entityID to be the same as the metadata location (which is fixed). Thus, keep the default which is `${connector.base-url}/metadata/sp`.

<a name="service-provider-credentials-configuration"></a>
#### 4.3.1. Service Provider Credentials Configuration

Under the `connector.eidas.credentials` key, the credentials that are to be used by the SP part of the eIDAS Connector are configured. If not assigned, the keys configured for the SAML IdP will be used also for the SP (see section [3.2](#credentials-configuration) above). 

If specific credentials for the SP part should be set up, the same type of configuration as for the IdP credentials is used (see section [3.2](#credentials-configuration) above), with the exception that the configuration prefix is `connector.eidas.credentials` instead of `saml.idp.credentials`.

<a name="service-provider-metadata-configuration"></a>
#### 4.3.2. Service Provider Metadata Configuration

The Service Provider part of the Connector publishes its metadata under `${connector.base-url}/metadata/sp`. The settings under the `connector.eidas.metadata` key configure how the metadata is compiled. The [application.yml](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/resources/application.yml) provides sensible defaults.

See the [eIDAS SP Metadata Configuration](configuration-reference.html#eidas-sp-metadata-configuration) section of the [Configuration Reference for the Swedish eIDAS Connector](configuration-reference.html) for a listing of possible configuration settings.

<a name="eu-metadata-configuration"></a>
#### 4.3.3. EU Metadata Configuration

Under the `connector.eu-metadata` key, the configuration for how SAML metadata for Identity Providers for the eIDAS countries are accessed is supplied.

The [EU Metadata Configuration](configuration-reference.html#eu-metadata-configuration) section of the [Configuration Reference for the Swedish eIDAS Connector](configuration-reference.html) lists all possible configuration settings. Important settings are:

- `connector.eidas.eu-metadata.location` - The URL for where the aggregated EU metadata is downloaded from. The current locations are:

    - Sweden Connect Sandbox - `https://mdsl.sandbox.swedenconnect.se/nodeconfig/metadata`
    
    - Sweden Connect QA - `https://qa.md.eidas.swedenconnect.se/role/idp.xml` 
    
    - Sweden Connect Production - `https://md.eidas.swedenconnect.se/role/idp.xml`
    
- `connector.eidas.eu-metadata.validation-certificate` - A resource pointing at the certificate used to validate the metadata, for example, `file:/opt/connector/metadata/eu-metadata-signing.crt`.

- `https-trust-bundle` - This setting may be used to specify a [Spring SSL Bundle](https://spring.io/blog/2023/06/07/securing-spring-boot-applications-with-ssl) that specifies the trusted root certificates to be used for TLS server certificate verification. If no bundle is given, the Java trust defaults will be used. See section [2.2](#configuring-certificate-trust) above.

<a name="prid-configuration"></a>
#### 4.3.4. PRID Configuration

The configuration for the PRID (Provisional Identifier) calculation is supplied under the `connector.prid` key. Two settings are available:

- `connector.prid.policy-resource` - A Resource pointing at the file containing the PRID configuration, see [eIDAS Connector Provisional Identifier (PRID) Calculation](prid.html). Example: `file:/opt/connector/prid/prid-policy.yml`.

- `connector.prid.update-interval` - Setting for how often the service should re-read the above policy file. The default is `600` (600 seconds). 

<a name="identity-matching-configuration"></a>
### 4.4. Identity Matching Configuration

The Identity Matching feature of the Swedish eIDAS Connector enables the connector to provide a Swedish identity in issued assertions (if there is an existing binding).

The [Identity Matching Configuration](configuration-reference.html#idm-configuration) section of the [Configuration Reference for the Swedish eIDAS Connector](configuration-reference.html) describes how the integration against the Identity Matching service is done.

**Example for the Sandbox environment:**

```yaml
connector:
  idm:
    active: true
    api-base-url: https://sandbox.swedenconnect.se/idm
    service-url: https://sandbox.swedenconnect.se/idm
    oauth2:
      resource-id: https://sandbox.swedenconnect.se/idm
      client-id: ${saml.idp.entity-id}
      check-scopes:
        - ${connector.idm.oauth2.resource-id}/idrecord_check
      get-scopes:
        - ${connector.idm.oauth2.resource-id}/idrecord_get
      server:
        issuer: ${saml.idp.entity-id}/as
      credential:
        bundle: idp-oauth2
```

---

Copyright &copy; 2017-2025, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).