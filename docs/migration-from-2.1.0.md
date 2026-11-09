![Logo](images/sweden-connect.png)

# Migration from eIDAS Connector 2.1.0

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

Version 3.0.0 of the Swedish eIDAS Connector is built on the
[Spring Authentication Server](https://docs.swedenconnect.se/spring-authentication-server) library instead of the
[Spring Security SAML Identity Provider](https://github.com/swedenconnect/saml-identity-provider) library. This page is
for operators upgrading a deployment from version 2.1.0. It describes what to change in the configuration, and what
behaves differently.

The URLs that Service Providers and the federation see are unchanged: the SSO endpoints, the IdP metadata location, the
SP metadata location and the `/extauth` pages. The entityIDs are unchanged, and so are the persistent `NameID`s that the
connector issues.

## Table of Contents

1. [**Before you start**](#before-you-start)

2. [**Changed properties**](#changed-properties)

3. [**Redis**](#redis)

4. [**Audit logging**](#audit-logging)

5. [**SAML status codes**](#saml-status-codes)

6. [**Other changes in behaviour**](#other-changes-in-behaviour)

    6.1. [Single sign-on](#single-sign-on)

    6.2. [Released attributes](#released-attributes)

    6.3. [Persistent NameID](#persistent-nameid)

    6.4. [IdP metadata](#idp-metadata)

    6.5. [The Actuator](#the-actuator)

    6.6. [The pages of the connector](#the-pages-of-the-connector)

7. [**Removed artifact**](#removed-artifact)

<a name="before-you-start"></a>
## 1. Before you start

The base configuration of the connector,
[application.yml](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/resources/application.yml),
has been converted, so most deployments only need to rename the settings of their own profile, see
[Section 2](#changed-properties). The base configuration sets the following, so that the connector behaves as version
2.1.0 did. Do not change these settings unless you want the behaviour to change:

- `authn-server.saml.path` is empty and the SAML endpoints are given in full, so that the SSO endpoints and the IdP
  metadata location are the same as before.

- `authn-server.saml.subject-identifier.secret` is empty, so that the persistent `NameID`s are computed as before, see
  [Section 6.3](#persistent-nameid).

- `authn-server.sso.same-requester-required` is `false`, so that single sign-on works across Service Providers as
  before, see [Section 6.1](#single-sign-on).

- `connector.eidas.entity-id` defaults to `${authn-server.base-url}/metadata/sp`, which gives the same value as before.

- `spring.application.name` is `eidas-connector`. It is required by the audit logging.

<a name="changed-properties"></a>
## 2. Changed properties

All `saml.idp.*` settings move to `authn-server.*`. The `connector.*` and `ui.*` settings keep their names and meaning,
except for the two settings of `connector.eidas.metadata` noted at the end of the table.

See [Configuration](https://docs.swedenconnect.se/spring-authentication-server/configuration.html) of the Spring
Authentication Server for a full reference, and in particular
[Migrating from saml-identity-provider](https://docs.swedenconnect.se/spring-authentication-server/configuration.html#migrating-from-saml-identity-provider).

| 2.1.0 | 3.0.0 | Note |
| :--- | :--- | :--- |
| `saml.idp.entity-id` | `authn-server.saml.entity-id` | Now optional. It defaults to the base URL, so set it to keep the entityID. |
| `saml.idp.base-url` | `authn-server.base-url` | The base configuration sets it to `${connector.base-url}`. |
| `saml.idp.hok-base-url` | `authn-server.saml.hok-base-url` | |
| `saml.idp.requires-signed-requests` | `authn-server.saml.requires-signed-requests` | The base configuration sets it to `true`. |
| `saml.idp.clock-skew-adjustment` | `authn-server.clock-skew` | `authn-server.saml.clock-skew` for SAML only. Also used when validating responses from the foreign countries. Default 30 seconds. |
| `saml.idp.max-message-age` | `authn-server.saml.max-message-age` | Also used when validating responses from the foreign countries. Default 3 minutes. |
| `saml.idp.sso-duration-limit` | `authn-server.sso.time-limit` | Default 60 minutes. A value of 0 used to turn single sign-on off; now set `authn-server.sso.enabled` to `false`. |
| `saml.idp.supports-user-message` | `authn-server.supports-user-message` | |
| `saml.idp.credentials.*` | `authn-server.saml.credentials.*` | The same credentials and fallback rules. See the note below the table. |
| `saml.idp.endpoints.*` | `authn-server.saml.endpoints.*` | The base configuration gives the endpoints. Only change them if you have changed them before. They are now relative to `authn-server.saml.path`, which the base configuration sets to an empty value. |
| `saml.idp.metadata.*` | `authn-server.saml.metadata.*` | The digest, signing and encryption methods and the requested principal selection are given here. |
| `saml.idp.metadata.ui-info.*` | `authn-server.entity-information.ui-info.*` | The base configuration gives the values. `authn-server.saml.metadata.ui-info.*` also works. |
| `saml.idp.metadata.organization.*` | `authn-server.entity-information.organization.*` | The base configuration gives the values. `authn-server.saml.metadata.organization.*` also works. |
| `saml.idp.metadata.contact-persons.*` | `authn-server.entity-information.contact-persons.*` | The base configuration gives the values. `authn-server.saml.metadata.contact-persons.*` also works. |
| `saml.idp.metadata-providers[]` | `authn-server.saml.metadata-providers[]` | The same properties. |
| `saml.idp.authn-context.*` | `authn-server.saml.authn-context.*` | The base configuration holds the eIDAS mappings for `minimum` comparison, as before. |
| `saml.idp.assertions.*` | `authn-server.saml.assertions.*` | |
| `saml.idp.replay.*` | `authn-server.saml.replay.*` | `type` now follows `authn-server.storage.type` when it is not set, see [Section 3](#redis). |
| `saml.idp.session.module` | `authn-server.storage.type` | `memory` (default) or `redis`, see [Section 3](#redis). |
| `saml.idp.audit.*` | `audit.repository.*` | See [Section 4](#audit-logging). |
| `spring.data.redis.ssl-ext.enable-hostname-verification` | `authn-server.redis.ssl.skip-hostname-verification` | Inverted: set to `true` to skip the check. |
| `spring.data.redis.ssl-ext.credential.*`, `spring.data.redis.ssl-ext.trust.*` | `spring.ssl.bundle.*` and `spring.data.redis.ssl.bundle` | Key and trust stores are given as a Spring Boot SSL bundle. |
| `spring.data.redis.cluster-ext.nat-translation[]` | `authn-server.redis.cluster.nat-translation[]` | |
| `spring.data.redis.cluster-ext.read-mode` | `spring.data.redis.lettuce.read-from` | `MASTER` is `upstream`, `SLAVE` is `replica-preferred` and `MASTER_SLAVE` is `any`. |
| `connector.eidas.metadata.ui-info.*`, `connector.eidas.metadata.requested-principal-selection` | - | Removed. They had no effect on the SP metadata. |

**Credentials.** The IdP credentials may also be given as beans. The bean names change from
`saml.idp.credentials.<Name>` to `authn-server.saml.credentials.<Name>`. When no SP credentials are configured under
`connector.eidas.credentials.*`, the IdP credentials are used for the SP, as before.

**Settings that refer to other settings.** A profile that refers to the old settings must be updated, for example
`client-id: ${saml.idp.entity-id}` under `connector.idm.oauth2` becomes `client-id: ${authn-server.saml.entity-id}`.

<a name="redis"></a>
## 3. Redis

Redisson is no longer used, and its settings have no effect. The connector includes Spring Data Redis and Spring
Session for Redis, so Redis is enabled by configuration alone:

```yaml
authn-server:
  storage:
    type: redis

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

- The default is still to keep everything in memory.

- With `redis`, the HTTP session, the IDs of received authentication requests and the IDs of received eIDAS responses
  are kept in Redis. In 2.1.0, the replay IDs were kept in Redis as soon as Redis was available, even when the session
  was not. Now they follow `authn-server.storage.type`, unless `authn-server.saml.replay.type` is set.

- The connection is configured with Spring Boot's own `spring.data.redis.*` settings, see
  [The Redis connection](https://docs.swedenconnect.se/spring-authentication-server/configuration.html#the-redis-connection).

- All keys start with `authn-server`, and the sessions are kept under `authn-server:sessions:`, unless
  `authn-server.redis.key-prefix` or `spring.session.data.redis.namespace` says otherwise. Sessions created by version
  2.1.0 cannot be read by version 3.0.0, so users that are in the middle of an authentication when a node is upgraded
  need to start over.

- The Redis health indicator is still turned off in the base configuration. Set `management.health.redis.enabled` to
  `true` when Redis is used.

<a name="audit-logging"></a>
## 4. Audit logging

Audit logging is now done with [spring-audit-support](https://docs.swedenconnect.se/spring-audit-support). The
repositories are configured under `audit.repository.*`, see
[Configuration](https://docs.swedenconnect.se/spring-audit-support/configuration.html).

| 2.1.0 | 3.0.0 | Note |
| :--- | :--- | :--- |
| `saml.idp.audit.in-memory.capacity` | `audit.repository.in-memory.capacity` | The base configuration sets 1000, as before. |
| `saml.idp.audit.file.log-file` | `audit.repository.file.log-file` | A file path. Each event is written as one JSON line. |
| `saml.idp.audit.redis.*` | `audit.repository.redis.key` | The events are kept in a Redis sorted set. |
| `saml.idp.audit.log-system.*` | `audit.repository.syslog.*` | There is no repository that writes to a Logback logger. To send audit events to a Syslog server, use the syslog repository. |
| `saml.idp.audit.include-events`, `saml.idp.audit.exclude-events` | `audit.repository.include-events`, `audit.repository.exclude-events` | The base configuration excludes `AUTHENTICATION_SUCCESS` and `AUTHORIZATION_FAILURE`, as before. |

**Changed events.** The `SAML2_*` events of the old library are replaced by the events of the Spring Authentication
Server, see [Auditing](https://docs.swedenconnect.se/spring-authentication-server/audit.html):

| 2.1.0 | 3.0.0 |
| :--- | :--- |
| `SAML2_REQUEST_RECEIVED` | `authn_request_received` |
| `SAML2_BEFORE_USER_AUTHN` | `authn_request_accepted` |
| `SAML2_AFTER_USER_AUTHN` | `authn_user_authenticated` |
| `SAML2_SUCCESS_RESPONSE` | `authn_success_response` |
| `SAML2_AUDIT_ERROR_RESPONSE` | `authn_error_response` |
| `SAML2_UNRECOVERABLE_ERROR` | `authn_unrecoverable_error` |

The library also logs `client_added` and `client_removed` when a Service Provider appears in or disappears from the
SAML metadata. At the first start of version 3.0.0, every Service Provider of the federation metadata is reported as
added. Exclude the events with `audit.repository.exclude-events` if they are not wanted. spring-audit-support logs
`system_started` and `system_shutdown`, which can be turned off with `audit.log-lifecycle-events: false`.

**Connector events.** The connector's own events, `CONNECTOR_*` and `EIDAS_SUCCESS_RESPONSE`, keep their types and their
data, field names included. What spring-audit-support adds to every event is new:

- `application`, holding `name` and `version`.

- `correlation_id`, which is the same for all events of one authentication flow, the connector's and the library's.

See [Swedish eIDAS Connector Audit Logging](audit.html).

**The Actuator endpoint.** The `auditevents` endpoint still works and is still exposed. It returns the events of the
in-memory repository, or of another repository that can be queried, see
[Accessing Audit Logs](management.html#accessing-audit-logs).

<a name="saml-status-codes"></a>
## 5. SAML status codes

The errors that the connector reports to the Swedish Service Provider keep their status codes and status messages.
This includes errors received from the foreign country, errors when processing the foreign response, a user that
cancels, a signature that the user does not consent to, a country that is not available and a sign message that is not
plain text.

The following statuses have changed:

| Situation | 2.1.0 | 3.0.0 |
| :--- | :--- | :--- |
| A Service Provider that is not accepted (requester acceptance) | `Responder` / `AuthnFailed` | `Responder` / `RequestDenied` |
| The Service Provider requires an attribute value, through a required `RequestedAttribute` with values in its metadata, and the user has another value | No error | `Requester` / `UnknownPrincipal` |

The old `Saml2ServiceProviderFilter` is replaced by requester acceptance, `authn-server.saml.requester-acceptance.*`,
see [Requester acceptance](https://docs.swedenconnect.se/spring-authentication-server/configuration.html#requester-acceptance).
The connector does not configure any requester acceptance rules.

<a name="other-changes-in-behaviour"></a>
## 6. Other changes in behaviour

<a name="single-sign-on"></a>
### 6.1. Single sign-on

Single sign-on works as before: a previous authentication may be reused for 60 minutes, by any Service Provider, under
an authentication context that the new request accepts. It is never reused for a signature service, never when the
request asks for another country than the one the user authenticated at, and never after a signature consent.

The library's default is to only reuse an authentication for the Service Provider that it was made for. The base
configuration sets `authn-server.sso.same-requester-required` to `false` to keep the earlier behaviour. The library
also refuses single sign-on when a Service Provider asks for another set of attributes than the original
authentication was made for. The connector turns that rule off.

One thing is new: an authentication that ends in an error, for example when the user cancels, removes any earlier
authentication from the session. The next request then authenticates the user again.

<a name="released-attributes"></a>
### 6.2. Released attributes

For the same eIDAS response, the connector releases the same Swedish attributes with the same values as before,
including the place of birth, the gender, the current address and the transliterated names. `eJusticeNaturalPersonRole`
is requested and released as before.

A Swedish Service Provider may request `TownOfBirth`, `CountryOfBirth` and `eJusticeNaturalPersonRole` by their eIDAS
attribute names. They are passed on in the eIDAS request, and released under the same names. In 2.1.0, any eIDAS
attribute that a Service Provider requested by its eIDAS name was released as received. Now only these three are.

The attributes may come in another order in the assertion. Duplicate values of `eJusticeNaturalPersonRole` are
released once.

<a name="persistent-nameid"></a>
### 6.3. Persistent NameID

The persistent `NameID`s are the same as before, since the base configuration turns off the subject identifier secret
for SAML. The library logs a warning about this at startup:

```
The subject identifier secret is turned off for SAML - persistent NameIDs can be traced back to the users they were issued for
```

The warning is expected. Assigning a secret gives all Service Providers new `NameID`s for their users.

In one case the `NameID` differs: a request with a `NameIDPolicy` that holds an `SPNameQualifier` but no `Format`. In
2.1.0, the `SPNameQualifier` was ignored in that case, and now it is used.

<a name="idp-metadata"></a>
### 6.4. IdP metadata

The IdP metadata is published at the same location with the same content: entity categories, algorithms, requested
principal selection, UI info, organization, contact persons and keys. The contact persons may come in another order.

<a name="the-actuator"></a>
### 6.5. The Actuator

- The `saml-metadata` health indicator is the connector's, as before. The library's own indicator with the same key is
  not used. The `id` detail holds the identifiers of all configured metadata sources.

- The `info` endpoint has two new entries from the library, `authn-server` and `client-registry`, see
  [The info endpoint](https://docs.swedenconnect.se/spring-authentication-server/management.html#the-info-endpoint).

- The library adds a `clients` endpoint. It is not exposed by the base configuration.

- The `WARNING` health status still gives HTTP 503, since the base configuration maps it so.

<a name="the-pages-of-the-connector"></a>
### 6.6. The pages of the connector

The pages and the flow are the same. Each page now carries the identifier of the authentication in progress, as the
`authnId` parameter, which makes authentications in several browser tabs independent of each other. A page that is
opened without that parameter, for example from a bookmark, shows the error page.

Errors that cannot be reported to the Service Provider show the connector's error page with the same messages as
before. The page is now also shown for requests that do not accept HTML.

<a name="removed-artifact"></a>
## 7. Removed artifact

The `se.swedenconnect.eidas:eidas-attribute-handling` artifact is no longer built. The conversion between eIDAS
attributes and Swedish eID attributes is now part of the connector application.

---

Copyright &copy; 2017-2026, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
