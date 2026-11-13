![Logo](../docs/images/sweden-connect.png)

# eIDAS Simulator

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A Spring Boot application that simulates the foreign eIDAS proxy services (the foreign IdPs) of the countries that are available in production, for testing the Swedish eIDAS connector. One running instance acts as one IdP per simulated country, each with its own entityID, endpoints, keys and metadata entry.

The simulator is built on OpenSAML and the Sweden Connect libraries `opensaml-eidas`, `opensaml-addons` and `opensaml-security-ext`. It speaks the eIDAS SAML dialect as given by the eIDAS technical specifications v1.4.

It is a test tool. The [compose setup](../compose/README.md) runs it together with the connector.

1. [Simulated countries](#simulated-countries)
2. [Endpoints](#endpoints)
3. [Request processing](#request-processing)
4. [Responses](#responses)
5. [The user interface](#the-user-interface)
6. [Configuration](#configuration)

<a name="simulated-countries"></a>
## 1. Simulated countries

The simulated countries are Austria (AT), Belgium (BE), Croatia (HR), Cyprus (CY), Czech Republic (CZ), Denmark (DK), Estonia (EE), Finland (FI), France (FR), Germany (DE), Italy (IT), Latvia (LV), Liechtenstein (LI), Lithuania (LT), Luxembourg (LU), Malta (MT), Netherlands (NL), Poland (PL), Portugal (PT), Romania (RO), Slovakia (SK), Slovenia (SI) and Spain (ES).

Each country is configured in its own file under [src/main/resources/countries](src/main/resources/countries). The settings mirror the country's entry in the production metadata, https://md.eidas.swedenconnect.se/role/idp.xml, except URLs and keys: protocol versions, application identifier, levels of assurance, supported signing, digest and encryption algorithms, NameID formats, bindings, supported attributes and whether the entry has an encryption key. The entity attributes that hold hashes of production URLs or keys are left out, and the organization and contact details mark the entry as simulated.

The keys of a country have the same type and size as the keys of its production entry.

### Users

Each country has 3 to 5 fixed natural persons, configured in the country's file. Each user has the four mandatory attributes, and many have optional attributes as well. Cyprus has users with names in Greek script together with their Latin transliteration.

The identifier part of the PersonIdentifier follows the country's format as given in [Vägledning förlitande parter](https://www.swedenconnect.se/driftinformation-och-support/vagledning-forlitande-parter). Countries for which no format is given use opaque values. The released value is `<country>/<connector country>/<identifier>`, for example `AT/SE/RebPiWIOw0RI+NSiPqF4zLryif1r`, where the connector country is the `NodeCountry` of the connector metadata.

A user is configured like this, where all attributes but the four mandatory ones are optional:

```yaml
users:
  - id: "maximilian"
    person-identifier: "UmViUGlXSU93MFJJK05TaVBx"
    given-name: "Maximilian"
    family-name: "Huber"
    date-of-birth: "1984-06-12"
    birth-name: "Maximilian Huber"
    place-of-birth: "Wien"
    gender: "Male"
    address:
      locator-designator: "14"
      thoroughfare: "Mariahilfer Straße"
      post-code: "1060"
      post-name: "Wien"
      admin-unit-first-line: "AT"
    nationality: "AT"
    country-of-birth: "AT"
    town-of-birth: "Wien"
    country-of-residence: "AT"
    email-address: "maximilian.huber@example.at"
    phone-number: "+43 1 234 5678"
```

Names in a non-Latin script are given in `given-name-non-latin`, `family-name-non-latin` and `birth-name-non-latin`, with the Latin transliteration in `given-name`, `family-name` and `birth-name`.

Only natural persons are simulated.

<a name="endpoints"></a>
## 2. Endpoints

The URLs are relative to `simulator.base-url`, and `<country>` is the country code in lower case.

| URL | Description |
| :--- | :--- |
| `/` | A page listing the simulated countries. |
| `/metadata` | The aggregated metadata (`EntitiesDescriptor`) of all active countries, signed with the metadata signing key. |
| `/<country>/ServiceMetadata` | The signed metadata of one country. This URL is also the entityID of the country. |
| `/<country>/ColleagueRequest` | The single sign-on service of a country, for HTTP-POST and HTTP-Redirect. |

<a name="request-processing"></a>
## 3. Request processing

An authentication request is processed as a production eIDAS node does. The simulator reads the eIDAS SP metadata of the connector from `simulator.connector.metadata-url`, when it is first needed and then at the refresh interval.

Until the connector is identified and its assertion consumer service is established, a failed check shows an error page, since no response can be sent:

- the binding is not supported by the country,
- the issuer is not the connector, or the connector metadata is not available,
- the request is not signed, or the signature cannot be validated against the signing keys of the connector metadata,
- the `AssertionConsumerServiceURL` or `AssertionConsumerServiceIndex` does not match the connector metadata.

After that, a failed check is answered with an error response:

| Check | Status |
| :--- | :--- |
| The SAML version is not 2.0. | `VersionMismatch` |
| `Destination` is not the single sign-on service of the country. | `Requester` / `RequestDenied` |
| `IssueInstant` is older than `simulator.request.max-age`, or in the future (with `simulator.request.clock-skew`). | `Requester` / `RequestDenied` |
| `IsPassive` is `true`. | `Responder` / `NoPassive` |
| `ForceAuthn` is not `true`. | `Requester` / `RequestDenied` |
| `SPType` is in both the request and the connector metadata, or in neither. | `Requester` / `RequestDenied` |
| `SPType` is `private`, the country requires the RequesterID of non-public relying parties (entity category `http://eidas.europa.eu/entity-attributes/termsofaccess/requesterid`), and there is no RequesterID. | `Requester` / `RequestDenied` |
| No attributes are requested, natural and legal person attributes are mixed, or a mandatory natural person attribute is not requested. | `Requester` / `RequestDenied` |
| A required attribute is not supported by the country. Optional attributes that are not supported are ignored. | `Responder` / `RequestDenied` |
| A required attribute is supported but not a natural person attribute (only natural persons are simulated). | `Responder` / `RequestUnsupported` |
| The requested NameID format is not supported by the country, and `simulator.request.strict-name-id-policy` is `true`. Otherwise a supported format is used. | `Requester` / `InvalidNameIDPolicy` |
| There is no `RequestedAuthnContext`. | `Requester` / `RequestDenied` |
| The comparison is `minimum` with other than one notified level of assurance, or the comparison is `better` or `maximum`. | `Requester` / `RequestUnsupported` |
| None of the requested levels of assurance can be delivered by the country. | `Responder` / `NoAuthnContext` |

For the comparison `minimum`, the country may deliver the requested level or a higher notified level. For `exact`, which is also the default, it may deliver one of the requested levels.

<a name="responses"></a>
## 4. Responses

A success response is signed, and holds a signed assertion that is encrypted for the connector. Error responses are signed. The algorithms are ones that both the country's settings and the connector metadata allow:

- The signature algorithm is the first signing method of the country that fits the country's key and that the connector metadata allows, also with regard to key sizes.
- The digest algorithm is the first digest method of the country that the connector metadata allows.
- The data encryption algorithm is the first of the country's algorithms (its supported encryption algorithms entity attribute, or else the encryption methods of its encryption key, or else AES-GCM) that the encryption key of the connector metadata allows.

If the country and the connector have no algorithm in common, an error page is shown.

The released attributes are the requested attributes that the country supports and that the user has a value for. The NameID of a persistent or unspecified format is the PersonIdentifier of the user, and a transient NameID is random.

<a name="the-user-interface"></a>
## 5. The user interface

When a request has been accepted, a page shows which country is simulated and which service the request came from. The tester can:

- pick one of the country's users, or enter a user of their own,
- change the attribute values of the user before the response is sent, and save them as a user of their own (kept for the browser session),
- pick the level of assurance, among those the country supports and the request allows,
- send an error response instead: a cancelled authentication, a failed authentication, no consent, a level of assurance that cannot be delivered, an unknown user, an attribute that cannot be delivered, an unsupported request or an internal error, with a status message of their own.

<a name="configuration"></a>
## 6. Configuration

The base configuration is [src/main/resources/application.yml](src/main/resources/application.yml). See [compose/config/simulator](../compose/config/simulator) for a complete example.

| Property | Description | Default |
| :--- | :--- | :--- |
| `simulator.base-url` | The base URL of the simulator, including protocol, host, port and context path. The URLs and entityIDs of the countries are derived from it. | - |
| `simulator.keystore.location` | The key store holding the metadata signing key and the keys of the countries. The signing key of a country has the alias `<country>-sign` and its encryption key `<country>-encrypt`, in lower case. | - |
| `simulator.keystore.password` | The password of the key store and its keys. | - |
| `simulator.keystore.type` | The key store type. | `JKS` |
| `simulator.metadata.signing-key-alias` | The alias of the metadata signing key. | `metadata` |
| `simulator.metadata.validity` | For how long the published metadata is valid. | `7d` |
| `simulator.metadata.cache-duration` | For how long the published metadata may be cached. | `1h` |
| `simulator.connector.metadata-url` | The URL of the eIDAS SP metadata of the connector. | - |
| `simulator.connector.validation-certificate` | The certificate that validates the signature of the connector metadata. If not set, the signature is not checked. | - |
| `simulator.connector.refresh-interval` | How often the connector metadata is downloaded again. | `10m` |
| `simulator.request.max-age` | The maximum age of an authentication request. | `5m` |
| `simulator.request.clock-skew` | The allowed clock skew. | `30s` |
| `simulator.request.strict-name-id-policy` | Whether a requested NameID format that the country does not support is answered with `InvalidNameIDPolicy`. | `false` |
| `simulator.active-countries` | The country codes of the active countries. If empty, all configured countries are active. | All |
| `simulator.countries.<country>.*` | The settings and users of a country, see the files under [src/main/resources/countries](src/main/resources/countries). Adding or removing a country is a matter of configuration and keys. | - |

The server listens on port `8443` with the context path `/simulator`, and the management endpoints on port `8444`.

---

Copyright &copy; 2017-2026, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
