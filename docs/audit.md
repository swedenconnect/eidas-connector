![Logo](images/sweden-connect.png)

# Swedish eIDAS Connector Audit Logging

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

The application produces audit log entries using [spring-audit-support](https://docs.swedenconnect.se/spring-audit-support).
How the audit events are stored is configured with the `audit.*` properties, see
[Audit Logging Configuration](configuration.html#audit-logging-configuration).

The audit events can be read through the Spring Boot Actuator `auditevents` endpoint, see
[Accessing Audit Logs](management.html#accessing-audit-logs).

The eIDAS Connector produces two types of Audit events: System Audit Events and User Audit Events.

All audit events have the common structure of spring-audit-support, see
[The audit event](https://docs.swedenconnect.se/spring-audit-support/usage.html#the-audit-event):

- `type` - The type of the audit entry, see below.

- `timestamp` - The timestamp of when the audit event entry was created.

- `application` - The application that produced the event, with `name` (`eidas-connector` unless
`spring.application.name` is changed) and `version`.

- `correlation_id` - Ties together all events of one authentication flow, from the authentication request sent by the
Swedish Service Provider to the response sent back. Not present for system events.

- `principal` - The "owner" of the entry. Depending on the type of audit event this is either
"eidas-connector" for system events or the ID SAML Service Provider that requested user authentication
(user events).

- `data` - Auditing data that is specific to the type of audit event.

## System Audit Events

System Audit Events are events that are logged to inform about events relating to the application/system
and not a specific user event.

<a name="CONNECTOR_EU_METADATA_CHANGE"></a>
### EU Metadata Change

**Type:** `CONNECTOR_EU_METADATA_CHANGE`

**Description:** The aggregated metadata containing the SAML metadata entities for all foreign eIDAS
Proxy Services are periodically updated. This event is signalled when something of the following happens:

- A country that previously appeared in the EU metadata can no longer be found.

- A country that was not present in older versions of the metadata has been added.

- A download error occurred.

**Audit data**: `eu-metadata-change`

| Parameter | Description | Type |
| :--- | :--- | :--- |
| `removed-countries` | A list of country codes for countries that previously appeared in the EU metadata, but were removed after the last update. If no removed countries were detected, this field is not included. | List of strings |
| `added-countries` | A list of country codes for countries that previously did not appear in the EU metadata, but were added in the last update. If no added countries were detected, this field is not included. | List of strings |
| `info` | Textual information from the update. Contains information that may be of interest. | String |
| `error-info` | Textual information from the update if an error occurred. | String |


## User Audit Events

User Audit Events are events that are logged as a result of an authentication request being processed.

The Swedish eIDAS Connector is built using the
[Spring Authentication Server](https://docs.swedenconnect.se/spring-authentication-server) library. The
[Auditing](https://docs.swedenconnect.se/spring-authentication-server/audit.html) page describes the audit events
that the library produces, and that are also logged by the eIDAS Connector. These are:

- `authn_request_received` - An authentication request has been received. No checks have been made yet.

- `authn_request_accepted` - The received authentication request has been successfully validated, and the user
authentication starts.

- `authn_user_authenticated` - The user has been authenticated. This can also be a re-use of a previously performed
authentication (SSO). In those cases this is reflected in the audit data.

- `authn_success_response` - A successful SAML response is sent. This means that the request has been processed, the
user authenticated and a SAML assertion created.

- `authn_error_response` - An error SAML response is sent. The error can represent a bad request or that the user
authentication failed. This event is also used to signal that the user cancelled the operation.

- `authn_unrecoverable_error` - An error that cannot be reported back to the Service Provider. The error is displayed in
the user interface.

spring-audit-support also logs the `system_started` and `system_shutdown` events, see
[Audit Events](https://docs.swedenconnect.se/spring-audit-support/audit-events.html).

Common for all User Audit Events is that the authentication data contains the following items:

- `sp-entity-id` - The "owner" of the entry. This will always the the SAML entityID of the Service Provider that requested authentication. If not available, `unknown` is used.
  
- `authn-request-id` - The ID of the authentication request that is being processed (`AuthnRequest`). If not available, `unknown` is used.

### Before Foreign Authentication

**Type:** `CONNECTOR_BEFORE_SAML_REQUEST`

**Description:** An event that is logged after the SAML authentication request that is to be sent to the foreign Identity Provider has been compiled, but before it is sent. This audit event will display in detail how the connector requests authentication based on the SAML request received from the Swedish SP and the country selection by the user.

**Audit data:** `eidas-authn-request`

| Parameter | Description | Type |
| :--- | :--- | :--- |
| `country` | The country code of the country that was selected by the user and to where the request is being sent. | String |
| `authn-request-id` | The ID of the authentication request that is sent to the foreign IdP. Note that this is **not** the same ID as the top-level `authn-request-id`, which is the ID for the original request from the Swedish SP. | String |
| `relay-state` | The RelayState variable used in the request. | String |
| `destination-url` | The URL to where the request is being sent. | String |
| `method` | Tells whether a redirect (`GET`) or a HTTP POST (`POST`) is used to send the request. | String |
| `requested-authn-context` | The requested authentication context of the authentication request. Contains the `comparison` field telling `exact` or `minimum` and a `authn-context-class-refs` field that is a list of URI:s for each requested authentication context class ref URI. | See desc. |
| `eidas-sp-type` | The type of SP that we are requesting authentication for (`public` or `private`) | String |
| `requested-attributes` | The SAML attributes that are requested to be delivered in the assertion. | A list of objects holding the fields `name` (for attribute name) and `is-required` (telling whether the attribute is required to be present). |

### Foreign Authentication Success

**Type:** `CONNECTOR_SUCCESS_RESPONSE`

**Description:** An event that is logged when a successful SAML response message has been received from the foreign IdP. Note that this does not necessarily mean that the entire operation succeeded, since the received assertion needs to be translated into a Swedish response. The `authn_success_response` and `EIDAS_SUCCESS_RESPONSE` events will indicate a successful authentication overall.

**Audit data:** `saml-response` - See [SAML Response Data](#saml-response-data).

**Audit data:** `saml-assertion` - See [SAML Assertion Data](#saml-assertion-data).

**Audit data:** `eu-peer-country`

| Parameter | Description | Type |
| :--- | :--- | :--- |
| `country` | The country code of the country that authenticated the user. | String |

### Foreign Authentication Failure

**Type:** `CONNECTOR_ERROR_RESPONSE`

**Description:** A SAML error response was received from the foreign IdP.

**Audit data:** `saml-response` - See [SAML Response Data](#saml-response-data).

### Processing Error of Foreign Response

**Type:** `CONNECTOR_PROCESSING_ERROR`

**Description:** If the processing of the response received fails this event is logged. This typically happens if the foreign IdP has issued a SAML assertion that does not meet the requirements. Note that this event may follow a `CONNECTOR_SUCCESS_RESPONSE`. This may happen in cases where the SAML processing is successful, but further processing such as mapping contexts and attributes fails.

**Audit data:** `processing-error`

| Parameter | Description | Type |
| :--- | :--- | :--- |
| `error-message` | Textual description of the error. | String |

**Audit data:** `saml-response` - See [SAML Response Data](#saml-response-data). 

> **Note:** If `CONNECTOR_SUCCESS_RESPONSE` was previously logged, this data is not included.

### Overall Authentication Success

**Type:** `EIDAS_SUCCESS_RESPONSE`

**Description:** Logged when a successful SAML response is sent to the Swedish Service Provider, that is, when the
authentication succeeded overall. It holds the response and the assertion that was sent, with the add-on that the
peer country is logged as well.

**Audit data:** `saml-response` - See [SAML Response Data](#saml-response-data).

**Audit data:** `saml-assertion` - See [SAML Assertion Data](#saml-assertion-data).

**Audit data:** `eu-peer-country`

| Parameter | Description | Type |
| :--- | :--- | :--- |
| `country` | The country code of the country that authenticated the user. | String |

### Result of Signature Consent

**Type:** `CONNECTOR_SIGNATURE_CONSENT_RESULT`

**Description:** When an authentication is requested from a signature service, the user will be displayed a signature consent dialogue. This event is logged to state the result of this dialogue (consent or reject).

**Audit data:** `signature-consent-result`

| Parameter | Description | Type |
| :--- | :--- | :--- |
| `signature-consented` | Tells whether the user consented to the signature. | Boolean |
| `user` | The eIDAS PersonIdentifier of the user that accepted/rejected the signature. | String |

### Result of Identity Matching Consent

**Type:** `CONNECTOR_IDM_CONSENT_RESULT`

**Description:** If the user has an Identity Matching record stored at the Identity Matching service, the connector needs to obtain the user's consent before reading this record and including the Swedish identity number in the resulting SAML assertion. This event is logged to tell whether the user gave his or hers consent to this, or whether the consent was rejected.

**Audit data:** `idm-consent-result`

| Parameter | Description | Type |
| :--- | :--- | :--- |
| `idm-consented` | Tells whether the user consented to releasing his/her Identity Matching record to the connector. | Boolean |
| `user` | The eIDAS PersonIdentifier of the user that consented/rejected the IdM query. | String |

### Identity Matching Record Obtained

**Type:** `CONNECTOR_IDM_RECORD`

**Description:** If the user has an Identity Matching record at the Identity Matching service, and has consented to that the contents of this record (i.e., the Swedish identity number) may be used in the resulting SAML assertion, this event is logged to signal that the record was obtained.

**Audit data:** `idm-record`

| Parameter | Description | Type |
| :--- | :--- | :--- |
| `user` | The eIDAS PersonIdentifier of the user. | String |
| `swedish-id` | The Swedish identity number of the user read from the IdM record. | String |
| `record-id` | The unique ID of the IdM record. | String |

### Identity Matching Record Error

**Type:** `CONNECTOR_IDM_ERROR`

**Description:** If there is a problem communicating with the Identity Matching service this will not be visible in the flow. Therefore, a separate audit event is created.

**Audit data:** `idm-error`

| Parameter | Description | Type |
| :--- | :--- | :--- |
| `user` | The eIDAS PersonIdentifier of the user. | String |
| `error-message` | The error message. | String |


## Common Audit Data

<a name="saml-response-data"></a>
### SAML Response Data

**Audit data:** `saml-response`

| Parameter | Description | Type |
| :--- | :--- | :--- |
| `id` | The ID of the `Response`. | String |
| `in-response-to` | The ID of the `AuthnRequest` that the response answers. | String |
| `status.code` | The main status code. | String |
| `status.subordinate-code` | The subordinate status code. Not present if there is none. | String |
| `status.message` | The status message. Not present if there is none. | String |
| `issued-at` | The `IssueInstant` of the response. | String |
| `destination` | The destination, i.e., where the response is sent. | String |
| `is-signed` | Whether the response is signed. | Boolean |

<a name="saml-assertion-data"></a>
### SAML Assertion Data

**Audit data:** `saml-assertion`

| Parameter | Description | Type |
| :--- | :--- | :--- |
| `id` | The ID of the assertion. | String |
| `in-response-to` | The ID of the `AuthnRequest` that the assertion answers. | String |
| `is-signed` | Whether the assertion is signed. | Boolean |
| `is-encrypted` | Whether the assertion is encrypted in the response. | Boolean |
| `issued-at` | The `IssueInstant` of the assertion. | String |
| `issuer` | The entityID of the issuer. | String |
| `authn-instant` | The authentication instant. | String |
| `subject-id` | The `NameID` of the subject. | String |
| `subject-locality` | The IP address of the user. | String |
| `authn-context-class-ref` | The authentication context class reference. | String |
| `authn-authority` | The first `AuthenticatingAuthority`. Not present if there is none. | String |
| `attributes` | The attributes of the assertion. Each element has `name` and `value`, where `value` is the first value of the attribute. | List |

---

Copyright &copy; 2017-2026, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
