![Logo](images/sweden-connect.png)

# Swedish eIDAS Connector Audit Logging

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

The application produces audit log entries using Spring Boot's auditing support, see 
[Spring Boot Authentication Auditing Support](https://www.baeldung.com/spring-boot-authentication-audit).

If you want to be able to obtain audit logs via Spring Boot Actuator you need to:

- Set the property `management.auditevents.enabled` to `true`.

- Include the string `auditevents` among the list specified by the setting 
`management.endpoints.web.exposure.include`.

- Make sure a `org.springframework.boot.actuate.audit.AuditEventRepository` bean exists.

The eIDAS Connector produces two types of Audit events: System Audit Events and User Audit Events.

All audit events will contain the following fields:

- `type` - The type of the audit entry, see below.

- `timestamp` - The timestamp of when the audit event entry was created.

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

The Swedish eIDAS Connector is built using the [Spring Security SAML Identity Provider](https://github.com/swedenconnect/saml-identity-provider) library. The [Identity Provider Auditing](https://docs.swedenconnect.se/saml-identity-provider/audit.html) page describes all the SAML-related audit events that are logged also by the eIDAS Connector. 

These are:

- [Authentication Request Received](https://docs.swedenconnect.se/saml-identity-provider/audit.html#SAML2_REQUEST_RECEIVED) - `SAML2_REQUEST_RECEIVED` - An event that is created when a SAML
`AuthnRequest` has been received. At this point the IdP has not performed any checks to validate the
correctness of the message.

- [Before User Authentication](https://docs.swedenconnect.se/saml-identity-provider/audit.html#SAML2_BEFORE_USER_AUTHN) - `SAML2_BEFORE_USER_AUTHN` - The received authentication request has been successfully validated.

- [After User Authentication](https://docs.swedenconnect.se/saml-identity-provider/audit.html#SAML2_AFTER_USER_AUTHN) - `SAML2_AFTER_USER_AUTHN` - The Identity Provider has successfully 
authenticated the user. This can also be a re-use of a previously performed authentication (SSO). In those
cases this is reflected in the audit data.

- [Successful SAML Response](https://docs.swedenconnect.se/saml-identity-provider/audit.html#SAML2_SUCCESS_RESPONSE) - `SAML2_SUCCESS_RESPONSE` - An event that is created before a
success SAML response is sent. This means that the request has been processed, the user authenticated 
and a SAML assertion created.

- [Error SAML Response](https://docs.swedenconnect.se/saml-identity-provider/audit.html#SAML2_AUDIT_ERROR_RESPONSE) - `SAML2_AUDIT_ERROR_RESPONSE` - An event that is created before an error SAML response is sent. The error can represent a bad request or that the user authentication failed.
This event is also used to signal that the user cancelled the operation.

- [Unrecoverable Error](https://docs.swedenconnect.se/saml-identity-provider/audit.html#SAML2_UNRECOVERABLE_ERROR) - `SAML2_UNRECOVERABLE_ERROR` - If an error occurs during processing
of an request and the IdP has no means of posting a SAML error response back, this error is displayed in 
the user interface. In these cases this is also audited.

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
| `destination-url` | The URL to where the request is being sent. | String |
| `method` | Tells whether a redirect (`GET`) or a HTTP POST (`POST`) is used to send the request. | String |
| `requested-authn-context` | The requested authentication context of the authentication request. Contains the `comparison` field telling `exact` or `minimum` and a `authn-context-class-refs` field that is a list of URI:s for each requested authentication context class ref URI. | See desc. |
| `eidas-sp-type` | The type of SP that we are requesting authentication for (`public` or `private`) | String |
| `requested-attributes` | The SAML attributes that are requested to be delivered in the assertion. | A list of objects holding the fields `name` (for attribute name) and `is-required` (telling whether the attribute is required to be present). |

### Foreign Authentication Success

> TODO

### Foreign Authentication Failure

> TODO




---

Copyright &copy; 2017-2024, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).