![Logo](images/sweden-connect.png)

# Management using the Actuator

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

## Table of Contents

1. [**Introduction**](#introduction)

2. [**Accessing Audit Logs**](#accessing-audit-logs)

3. [**The Health Endpoint**](#the-health-endpoint)

    3.1. [SAML Metadata Health](#saml-metadata-health)

    3.2. [PRID Health](#prid-health)
    
    3.3. [Credential Monitoring](#credential-monitoring)

    3.4. [Identity Matching Health](#identity-matching-health)

4. [**The Info Endpoint**](#the-info-endpoint)

    4.1. [Country Information](#country-information)

    4.2. [Current PRID Policy Configuration](#current-prid-policy-configuration)

5. [**The PRID Configuration Refresh Endpoint**](#the-prid-configuration-refresh-endpoint)

---

<a name="introduction"></a>
## 1. Introduction

The Spring Boot [Actuator Endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html) can be used for supervision of the application. It defines a set of endpoints which are described in the sections below.

<a name="accessing-audit-logs"></a>
## 2. Accessing Audit Logs

**Path:** `actuator/auditevents`

**Reference:** https://docs.spring.io/spring-boot/api/rest/actuator/auditevents.html

Displays audit events.

Depending on how Audit logging is configured (see [Audit Logging Configuration](configuration.html#audit-logging-configuration)), the audit endpoint may not display all available events. For example, if Redis is not used to save events, the settings for in-memory logging will be used.

See [Swedish eIDAS Connector Audit Logging](audit.html) for a listing of which audit events that are logged by the Connector.

**Note:** If audit logging has been configured to write audit events to files, of course the audit events may be accessed that was as well.

<a name="the-health-endpoint"></a>
## 3. The Health Endpoint

**Path:** `/actuator/health`

**Reference:** https://docs.spring.io/spring-boot/api/rest/actuator/health.html

The Health-endpoint should be invoked periodically in order to monitor the "health" of the eIDAS application. The overall health-status can be one of:

- `UP` - Everything is looking good. 
- `DOWN` - As the name states, the service is down and no features are available.
- `OUT_OF_SERVICE` - The application is running, but can one or more features are out of service.
- `WARNING` - The application is running, but at least one of the health-components have issued a warning, that should be paid attention to.
- `UNKNOWN` - Health status could not be reported.

:exclamation: Simple monitoring services that can not interpret the body of the health-call, should at least trigger on the HTTP status, where 200 means `UP` and 50X, meaning "not ok".

Apart from Spring's standard health components<sup>1</sup>, the eIDAS Connector delivers health information about the components described below.

> **\[1\]:** Standard health components include indicators for disk space, Redis and SSL/TLS.

<a name="saml-metadata-health"></a>
### 3.1. SAML Metadata Health

**Key:** `saml-metadata`

**Description:** Asserts that the connector has access to valid SAML metadata.

If the connector has access to valid metadata, the component will display the following information:

```json
  ...
  "saml-metadata" : {
    "status" : "UP",
    "details" : {
      "id" : "https://md.swedenconnect.se/role/sp.xml"
    }
  },
  ...
```

If the connector does not have access to valid SAML metadata, the connector will not be able to serve any requests, and the connection against the metadata download URL needs to be checked.

```json
  ...
  "saml-metadata" : {  
    "status" : "OUT_OF_SERVICE",
    "details" : { 
      "id" : "https://md.swedenconnect.se/role/sp.xml",
      "error-message" : "No valid SAML metadata available"
    }
  },
  ...  
```

<a name="prid-health"></a>
### 3.2. PRID Health

**Key:** `prid`

**Description:** Checks that the configuration for [eIDAS Connector Provisional Identifier (PRID) Calculation](prid.html) is correct. The main purpose of the PRID health-component is to make sure that there is a configuration for all countries, and the component reads the EU metadata and makes sure that all countries published to the aggregated EU metadata has a corresponding PRID configuration.

If the PRID-configuration is correct, the following will be returned:

```json
  ...
  "prid" : {
    "status" : "UP",
    "details" : {
      "prid-policy-status" : "ok" }
    }
  },
  ...
```

If there are countries in the EU SAML metadata that do not have a PRID-configuration:

```json
  ...
  "prid" : { 
    "status" : "WARNING",
    "details" : { 
      "prid-policy-status" : {
        "missing-prid-config" : [ "NO" ] 
      }
    }
  },
  ...
```

The warning above states that metadata for Norway was found in the EU aggregated metadata, but the connector does not have a PRID configuration for Norway.

The PRID-endpoint also warns for invalid PRID configurations. Suppose that an administrator noted that Norway was missing from the configuration and added an entry, but made a mistake. This will look something like:

```json
  ...
  "prid" : {
    "status" : "WARNING",
      "details" : { 
        "prid-policy-status" : {
          "config-validation" : [ "Invalid algorithm (defaultX-eIDAS) for country NO" ],
          "missing-prid-config": [ "NO" ]
    }
  },

```

<a name="credential-monitoring"></a>
### 3.3. Credential Monitoring

**Key:** `credential-monitor`

**Description:** If hardware based credentials are used (i.e., HSM:s), monitoring of their status can be configured (see  [Credentials Monitoring](https://docs.swedenconnect.se/credentials-support/#monitoring)).

See [Credential Monitoring Health Endpoint](https://docs.swedenconnect.se/credentials-support/#credential-monitoring-health-endpoint) for details about the monitored credentials.

<a name="identity-matching-health"></a>
### 3.4. Identity Matching Health

**Key:** `idm`

**Description:** Health indicator for checking the connectivity against the Identity Matching Service.

<a name="the-info-endpoint"></a>
## 4. The Info Endpoint

**Path:** `/actuator/info`

**Reference:** https://docs.spring.io/spring-boot/api/rest/actuator/info.html

The `info` endpoint provides general information about the application, and apart from Spring's standard elements, information about the following will be provided:

<a name="country-information"></a>
### 4.1. Country Information

**Key:** `countries`

**Description:** Provides information about the countries that are connected to the Swedish eIDAS Connector.

For each country its country code, its SAML entityID and a list of supported assurance levels will be displayed.

Example:

```json
{
  ...
  "countries" : [ { 
    "country-code" : "NO",
    "entity-id" : "https://eidas-proxy-service.no/eidas-ps/ServiceMetadata",
    "assurance-levels" : [
      "http://eidas.europa.eu/LoA/high",
      "http://eidas.europa.eu/LoA/substantial",
      "http://eidas.europa.eu/LoA/low" ]
  },
  {
    "country-code" : "FR",
    "entity-id" : "https://eidas-cef-xa.fr/EidasNodeProxy/ServiceMetadata",
    ...
```

<a name="current-prid-policy-configuration"></a>
### 4.2. Current PRID Policy Configuration

**Key:** `prid-policy`

**Description:** Displays a the current PRID policy (see [eIDAS Connector Provisional Identifier (PRID) Calculation](prid.html)).

Example:

```json
{
  ...
  "prid-policy" : { 
    "NO" : {
      "algorithm" : "default-eIDAS",
      "persistenceClass" : "A"
    },"
    FR": { 
      "algorithm" : "default-eIDAS",
      "persistenceClass" : "B"
    },
    ...
  },    
  ...
}
```

<a name="the-prid-configuration-refresh-endpoint"></a>
## 5. The PRID Configuration Refresh Endpoint

**Path:** `/actuator/refreshprid`

**Description:** The PRID service will reload the PRID policy file every 10 minutes. It is also possible to force a reload and verify that the update was correct. This is done by invoking the `/actuator/refreshprid`.

As its output, the endpoint will respond with a status and a list of the policy configuration, see [4.2](#current-prid-policy-configuration) above.

Make sure to inspect the result of the call, since the endpoint will analyze the newly updated PRID configuration file, and it it detects any errors report these.

---

Copyright &copy; 2017-2025, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).