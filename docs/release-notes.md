![Logo](images/sweden-connect.png)

# Release Notes - Swedish eIDAS Connector

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

### Version 2.0.0

**Release date:** 2025-01-15

Completely new IdP-base where we use Spring Boot instead of Shibboleth.

No changes in supported features.

### Version 1.7.1

**Release date:** 2023-08-08

##### Assertion from foreign country may now be released as an attribute

The assertion that the connector receives from the foreign country may now be delivered in the `authServerSignature` (`urn:oid:1.2.752.201.3.13`)
attribute. The contents of this attribute is a Base64-encoded string.

**Note:** In order to receive this attribute a Swedish SP must declare the attribute as "requested"
in its metadata record (see below).

```
<md:RequestedAttribute Name="urn:oid:1.2.752.201.3.13" isRequired="false"/>
```

### Version 1.7.0

**Release date:** 2022-02-09

##### Fixes to be compliant with the latest version of the Swedish eID Framework

- Support for sigmessage AuthnContextClassRef URI:s has been dropped.
- Changes in how a personal identity number is delivered (as a mappedPersonalIdentityNumber
attribut).

### Version 1.6.8

**Release date:** 2021-12-16

##### Upgraded Tomcat version

Tomcat was updated along with dependencies to jQuery.

### Version 1.6.7

**Release date:** 2020-10-29

##### Filtering of countries based on requested AuthnContextClassRef(s)

Given the AuthnContextClassRef(s) requested by the SP, we now check all countries capabilities and only
those that meet the SP requirements are selectable. Those that are not will be greyed out in the UI.

##### Signing of SP metadata is done according to eIDAS crypto reqs

Signing of SP metadata is done according to the eIDAS crypto requirements.

### Version 1.6.6

**Release date:** 2020-10-05

##### Bugfix in configuration

If no AuthnContext was specified in an AuthnRequest, the default AuthnContext:s used was wrong. This configuration fix has been fixed.

### Version 1.6.5

**Release date:** 2020-09-29

##### Support for eIDAS "ping"

We have implemented support for sending eIDAS "ping" authentication requests. This feature has to be configured so that only SP:s that are white-listed are allowed to send such requests. This is controlled by the `IDP_PING_WHITELIST` setting. See section "IdP interoperability and test settings" in [docs/configuration.md](docs/configuration.md).

##### Work-around for DK interop

Currently, the DK eIDAS Proxy Service cannot process requests that contain the `Scoping` element. Therefore a setting, `IDP_SP_REQUEST_SKIP_SCOPING_FOR`, has been introduced to implement a work-around. See section "IdP interoperability and test settings" in [docs/configuration.md](docs/configuration.md).

##### Fix for UK interoperability

UK cannot handle the `ProtocolBinding` attribute in an authentication request. The specs says that we should not use that attribute, so this has been removed.

##### Accessibility fixes

The connector UI has been updated for accessibility.

##### Removed support for MDSL

MDSL is no longer supported. The settings `EIDAS_METADATA_SERVICE_LIST_URL` and `EIDAS_METADATA_SERVICE_LIST_VALIDATION_CERT` must no longer be used.

### Version 1.6.4

**Release date:** 2020-09-18

##### Added link to an accessibility report

We need to have a link to an accessibility report in the UI. This was added. See the `IDP_ACCESSIBILITY_URL` setting in [docs/configuration.md](docs/configuration.md).

### Version 1.6.3

**Release date:** 2020-06-08

##### Compilance fix for UK (no 2)

When receiving an Assertion from the UK Proxy Service we don't receive a `SubjectConfirmation`
element as part of the `Subject` element. This is in so many ways wrong, but since the eIDAS specs don't explicitly state the the `SubjectConfirmation` element MUST be set we are forgiving and accept these assertions. However, UK should be informed that they should fix this anyway.

### Version 1.6.2

**Release date:** 2020-06-08

##### Compilance fix for UK

The UK Proxy Service is very picky when processing AuthnRequest and won't accept the ProtocolBinding
attribute. The eIDAS spec states that this SHOULD NOT be set, so we fixed this.

### Version 1.6.1

**Release date:** 2020-06-01

##### Bugfix for HSM:s when signing with RSA-PSS

When the connector signs a SAML message with the RSA-PSS algorithm we got a PKCS#11 related error.
The reason was that an underlying library used the wrong RSA mode. This has been fixed in the underlying
library, and the 1.6.1 version of the connector now uses this updated version.

### Version 1.6.0

**Release date:** 2020-02-26

##### Compliance with the Swedish eID Framework (Jan 2020) release and eIDAS crypto specs

Includes:
- Support for the `signMessageDigest` attribute.
- Support for the `PrincipalSelection` extension.
- Releases country as an attribute.
- Support for all algorithms defined in the eIDAS crypto specification.
- The SP part of the connector now analyzes peer crypto requirements when sending an AuthnRequest.

##### Statistics Logging

The eIDAS connector now produces statistics logs. See [docs/logging.md](docs/logging.md) for a description of the new environment variables `IDP_STATS_SYSLOG_HOST`, `IDP_STATS_SYSLOG_PORT` and
`IDP_STATS_SYSLOG_FACILITY`.

##### Bug fix in start.sh

There was a bug in the start script in where the value of `IDP_PROCESS_SYSLOG_HOST` was not read.
Instead the syslog configuration for process logging used the same host as for audit (`IDP_SYSLOG_HOST`). This is probably what was intended so the bug never triggered. However, from version 1.6.0 on the host name for process syslog MUST be set to the intended host.

##### Cookie mixup bug in QA and production

Since the QA domain is a superset of production a person using his or hers browser in production first and then in QA (for example an integrator) will get the browser cookies from production sent to QA instances also. This has led to a number of stale request errors.

The solution is to use different names on the session cookies. Therefore, in QA the variable `TOMCAT_SESSION_COOKIE_NAME` should be assigned another value than `JSESSIONID.CONNECTOR`, for example `JSESSIONID.CONNECTOR.QA`.

See [docs/configuration.md](docs/configuration.md).

##### Metadata publishing updates

The eIDAS connector now includes the `<psc:RequestedPrincipalSelection>` extension in its metadata. Therefore, the metadata in Sweden Connect needs to be updated for the connector.

##### Minor UI fixes

We received some accessibility comments about the connector UI. These have been fixed.

### Version 1.5.4

**Release date:** 2019-11-19

##### Algorithm Configuration

Configured the use of `http://www.w3.org/2007/05/xmldsig-more#sha256-rsa-MGF1" (and 384 and 512) as well. This is needed for interop with Estonia.

### Version 1.5.3

**Release date:** 2019-11-06

##### Cookie fixes

Added SameSite=None for all connector cookies. We need to avoid newer versions of Chrome and Firefox
to assume SameSite=Lax which means that our cookies will not be passed in POST requests from eIDAS
nodes.

In order to get this functionality we had to update to Tomcat 8.5.42 (from 8.5.37). This version has
been fully tested in the sandbox.

### Version 1.5.2

**Release date:** 2019-01-17

##### Bugfix for MDSL

MDSL-files was not read due to a classpath error. Shibboleth 3.4.3 had removed commons-io from the classpath
and that lib was used by MDSL-code ...

### Version 1.5.1

**Release date:** 2019-01-17

##### Upgraded to Shibboleth 3.4.3

We are now using Shibboleth 3.4.3 and OpenSAML 3.4.2.

##### Resolved security issues

- All cookies are now marked as HTTP-only and secure.

- No 500-stack traces are displayed in the UI.

- Fixed JSTL 1.2 vulnerability

- Acted on a number of Snyk-reports concerning vulnerabilities on dependencies.

##### Configured logging to be less verbose

- We now filter away Shib's error logging of things that we don't think are errors.

##### Implemented "HideFromDiscovery"

- The connector now hides countries that are marked as HideFromDiscovery in aggregated metadata (https://refeds.org/category/hide-from-discovery).

### Version 1.5.0

**Release date:** 2018-10-17

##### Upgraded to Shibboleth 3.4.0

The connector now runs on Shibboleth 3.4.0.

### Version 1.4.0

**Release date:** 2018-10-15

##### Updated Tomcat version

The connector now runs on Tomcat version 8.5.34.

##### Updated logotypes

We are now using the official Sweden Connect logotype with blue background.

### Version 1.3.9

**Release date:** 2018-09-25

##### Updated logotypes

We are now using the official Sweden Connect logotype.

### Version 1.3.8

**Release date:** 2018-09-21

##### The process log can not be sent to syslog

By setting the `IDP_PROCESS_SYSLOG_HOST` variable the connector process log will be sent to syslog. The variables `IDP_PROCESS_SYSLOG_PORT` and `IDP_PROCESS_SYSLOG_FACILITY` controls port and facility.

See [eIDAS Connector Logging](docs/logging.md) for details.

##### Scoping/RequestID is not sent by SP

For future use (private sector support) the connector now includes a `Scoping` element holding a `RequesterID` element that is the requesting SP's entityID.

### Version 1.3.7

**Release date:** 2018-09-13

##### PKCS#11 fixes

New configuration for PKCS#11 support. Compared with 1.3.6 we introduce `IDP_PKCS11_PIN` and remove all specific PIN variables per key.

##### Changed handling of "no countries available"

Instead of returning a SAML error response when there are no available countries, we now display a message to the user.

### Version 1.3.6

**Release date:** 2018-09-10

##### No need to read MDSL-file

If the aggregated EU metadata contains the `eidas:NodeCountry` extension, the eIDAS connector no longer has to be configured for downloading MDSL-files. So, `EIDAS_METADATA_SERVICE_LIST_URL` and `EIDAS_METADATA_SERVICE_LIST_VALIDATION_CERT` should not be set anymore in these cases.

##### PKCS#11 fixes

New configuration for PKCS#11 support.

### Version 1.3.5

**Release date:** 2018-08-31

##### UI fixes

Fixes for Microsoft Internet Explorer.

##### Cover up for metadata aggregation problems

Default is eidas-low and eidas-substantial if we don't get loa from metadata.

##### 404-page

Added 404-page and index-page.


### Version 1.3.4

**Release date:** 2018-08-09

##### Bugfix for several metadata sources

The previous version had a bug where only the secondary metadata source was consumed. This has been fixed.

##### Changes based on updates to the eIDAS specifications

The Connector SP metadata now contains:

- Application identifier entity attribute
- Protocol version entity attribute
- `eidas:NodeCountry` extension.

##### Bugfix for handling CurrentAddress attribute

The eIDAS attribute CurrentAddress was not processed correctly.

##### Added support for Scoping in AuthnRequests

The eIDAS connector now supports the `Scoping` element in `AuthnRequest` messages. Using this a SP may give the required country in the request.

##### UI fixes

Fixes for the connector UI (removed E-legitimationsnämnden logo, etc).


### Version 1.3.3

**Release date:** 2018-06-15

##### HSM support

Support for HSM:s has been added.

### Version 1.3.2

**Release date:** 2018-05-28

##### Redis server storage support

Support for using Redis as a storage service has been added.

Check the description of Redis in [Configuration](docs/configuration.md) for details.

### Version 1.3.1

**Release date:** 2018-05-03

##### UI bugfixes

Now making local includes for Javascript and CSS.

### Version 1.3.0

**Release date:** 2018-05-03

##### New UI for the connector

The connector now has the Sweden Connect UI.

##### Updated LoA-matching rules

Updated handling of LoA according to the latest eIDAS (draft) specifications.

##### Explicitly setting domain on cookies

We have had problems with cookies not being sent to the IdP. This probably has to do with a cookie domain issue
so we set this explicitly.

Note: `TOMCAT_HOSTNAME` must be set!

##### Changed name on session cookie

In order to not interfer with other services we have changed name on the JSESSIONID cookie to JSESSIONID.CONNECTOR.

##### Representative attributes are blocked

We will not support the eIDAS representative attributes and therefore block those assertions.

### Version 1.2.2

**Release date:** 2018-04-10

##### Verbose messages for error Status elements

When running tests a Service Provider would benefit from getting a bit more information from the `Status` element of the SAML Response in case of a failed authentication. If the environment variable `IDP_ERRORS_VERBOSE` is set to `true` the eIDAS Connector will include more information in the `StatusMessage` of the error status.

### Version 1.2.1

**Release date:** 2018-04-09

##### SP-type default

If a SP-type was missing from SP metadata we sent an `AuthnRequest` with an empty SP-type extension. This has been fixed.

##### Mismatching issue instant on response and assertion

The EU-software issues assertions with issue instants that are newer than the issue instant of the response. This has been reported, but we add a work-around for the time being.

### Version 1.2.0

**Release date:** 2018-04-09

##### SP-type requirement

This version will log warnings if the SP lacks the SP-type entity category. The next will refuse.

##### Full validation of eIDAS-responses

The SP part of the connector will now perform a full SAML validation of responses and assertions. 

##### DUO bugfixes for Metadata service list

##### Bugfix for Shibboleth attribute release

Shibboleth would sometimes re-use the attribute set from a previous authentication. This is not
a good idea for a Proxy-IdP.


### Version 1.1.0

**Release date:** 2018-03-15

##### The Connector IdP can now be configured to consume federation metadata from two sources

By assigning the variables `SECONDARY_FEDERATION_METADATA_URL` and `SECONDARY_FEDERATION_METADATA_VALIDATION_CERT` to a secondary metadata URL and validation certificate respectively, the connector consumes metadata from two sources.

##### Support for the Signature Activation Protocol has been added

The connector now supports processing of the `SADRequest` extension in authentication requests sent from Signature Service SP:s. It also supports issuance of a SAD.

##### Minor fixes

* Support for new representations of LoA URI:s.
* Client state is now stored on the server instead of in a user cookie.
* Added logging for custom remote IP valve to track down IP bug.


### Version 1.0.9

**Release date:** 2018-02-23

##### Tomcat now uses customized Tomcat valve to get hold of user IP address

Instead of using the Tomcat default RemoteIpValve to obtain the user IP address (as described below), we use a customized RemoteIpValve that reads a shared secret from a header (by default, X-Proxy-Authenticate), and if that matches, uses the remote IP address.

The variable `TOMCAT_PROXY_SHARED_SECRET` needs to be set when starting the connector. The value assigned to this variable should also be set to the X-Proxy-Authenticate header by the front-end server(s).

##### Bug fix concerning SSO

A Shibboleth config setting prevented us from using SSO. This has been fixed.

### Version 1.0.8

**Release date:** 2018-01-17

A rebuild of the same functionality as in 1.0.7, but with the correct dependencies shipped (bad metadata fix).

### Version 1.0.7

**Release date:** 2018-01-10

##### Fixes for getting hold of user IP address

The setting `TOMCAT_INTERNAL_PROXIES` was introduced. Its purpose is to configure the Tomcat RemoteIpValve with a list of "internal proxies". In order for the RemoteIpValve to consider the value
passed in the X-Forwarded-For header, the remote address for the Tomcat request must match the regexp assigned to `TOMCAT_INTERNAL_PROXIES`.

The default value for `TOMCAT_INTERNAL_PROXIES` is:

```
10\.\d{1,3}\.\d{1,3}\.\d{1,3}|192\.168\.\d{1,3}\.\d{1,3}|169\.254\.\d{1,3}\.\d{1,3}|127\.\d{1,3}\.\d{1,3}\.\d{1,3}|172\.1[6-9]{1}\.\d{1,3}\.\d{1,3}|172\.2[0-9]{1}\.\d{1,3}\.\d{1,3}|172\.3[0-1]{1}\.\d{1,3}\.\d{1,3}
```

This corresponds to: `10/8, 192.168/16, 169.254/16, 127/8 and 172.16/12`

Regular expression vs. IP address blocks: `mod_remoteip` allows to use address blocks (e.g. `192.168/16`) to configure RemoteIPInternalProxy and RemoteIPTrustedProxy ; as Tomcat doesn't have a library similar to `apr_ipsubnet_test`, RemoteIpValve uses regular expression to configure internalProxies and trustedProxies in the same fashion as RequestFilterValve does.

Check the description of `TOMCAT_INTERNAL_PROXIES` in [Configuration](docs/configuration.md) for details.

##### Bugfix for bad metadata

A threading issue caused metadata without a valid `<SignatureValue>` to be created. This has been fixed. 


---

Copyright &copy; 2017-2025, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
