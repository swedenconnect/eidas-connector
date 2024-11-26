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

4. [**The Info Endpoint**](#the-info-endpoint)

---

<a name="introduction"></a>
## 1. Introduction

> - supervision

<a name="accessing-audit-logs"></a>
## 2. Accessing Audit Logs

> - audit logs

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

Apart from Spring's standard health components, the eIDAS Connector delivers health information about the components described below.

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




<a name="the-info-endpoint"></a>
## 4. The Info Endpoint

---

Copyright &copy; 2017-2024, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).