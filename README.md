![Logo](https://github.com/swedenconnect/technical-framework/blob/master/img/sweden-connect.png)

# eidas-connector

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Swedish eIDAS Connector Service

---

TODO: Add documentation

### Statistics from the eIDAS Connector

The eIDAS connector produces statistics in a separate log file. The logs are in JSON-format and
are written so that analyzing tools can produce nice statistics concerning the number of processed
requests and so.

#### Events

The following events are produced:

- `received-authnrequest` - A request from a Swedish Service Provider was received.
- `cancelled-country-selection` - If the user cancels the operation at the country selection dialogue page.
- `directed-user-to-foreign-idp` - The user was directed to the foreign IdP (after country selection).
- `received-eidas-response` - A successful response was received from the foreign IdP. This means that the user successfully authenticated at the foreign IdP.
- `sign-message-display` - If the calling SP is a signature service this event is logged after `received-eidas-response` event and before the user is directed to the "approve signature message" dialogue page.
- `rejected-signature` - If the user did not approve the presented signature message (for signature services only).
- `authn-sucess` - Event that is logged before the user is directed back to the Swedish Service Provider after a successful authentication.

The following error events may occur:

- `error-authnrequest` - If a request received from a Swedish Service Provider can not be successfully processed.
- `error-config-metadata` - If the connector has bad configuration.
- `error-response-processing` - Event that is logged if the connector received a successful response from the foreign IdP but its processing of it finds errors (which leads to an error being reported back to the user).
- `error-eidas-error-response` - If an error response is received from the foreign IdP (i.e., the authentication failed on the foreign IdP).

#### Log data

Each event has associated log data. Depending on the event type, the contents may differ. 
All events have the following attributes:

- `type` - The event type. See above.
- `timestamp` - The timestamp (in millis since 1970) when the event was logged.
- `initTimestamp` - The timestamp for when the operation was initiated (i.e., when the user first came to the connector).
- `requestId` - The SAML ID for the authentication request that was received from the Swedish Service Provider.
- `requester` - The entityID of the requesting Service Provider.
- `pingFlag` - A flag telling whether this request is a "eIDAS ping" request (`true`) or a normal authentication request (`false`).

Below follows the attributes that may be present (depending on the type of event):

- `preSelectedCountry` - If the country was provided in the actual authentication request from the Service Provider. In these cases no "country selection" dialogue is presented.
- `country` - The country code for the selected country. This is present in all events that are logged after the user has selected the country in the "country selection" dialogue.
- `responseId` - The SAML ID for the response message received from the foreign IdP (both for successful and error responses).
- `eidasAssertionId` - The ID of the SAML assertion received from the foreign IdP (for successful authentication).
- `eidasLoa` - The eIDAS Level of Assurance URI. Received from the foreign IdP (for successful authentication).
- `loa` - The Swedish Level of Assurance URI. For successful authentications.

If an error event is logged, the following is logged:

- `error.errorCode` - The SAML error code (that is sent back to the Swedish SP).
- `error.errorMessage` - The textual error message.

For example: 
```
"error" : {
  "errorCode" : "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed",
  "errorMessage" : "Failure received from foreign service: The user cancelled authentication"
},
```


#### Example

The eIDAS Connector in the Sweden Connect Sandbox-federation publishes its statistics logs at the URL <https://con.sandbox.swedenconnect.se/idp/logs/stats>. Check this log for an example of a statistics log file.


---

Copyright &copy; 2016-2020, [Sweden Connect](https://swedenconnect.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
