![Logo](images/sweden-connect.png)

# Configuration Reference for the Swedish eIDAS Connector

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

The Swedish eIDAS Connector is built using the [Spring Security SAML Identity Provider](https://github.com/swedenconnect/saml-identity-provider) libraries. Therefore, all the configuration of the SAML IdP part of
the Connector is done according to this library's configuration, see below.

<a name="saml-idp-configuration"></a>
## SAML IdP Configuration

See the [Configuration](https://docs.swedenconnect.se/saml-identity-provider/configuration.html) for the 
[Spring Security SAML Identity Provider](https://github.com/swedenconnect/saml-identity-provider).

<a name="eidas-connector-configuration"></a>
## eIDAS Connector Configuration

**Description:** Configuration specific for the eIDAS Connector.

**Java class:** [ConnectorConfigurationProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/ConnectorConfigurationProperties.java)

| Property | Description | Type | Default value |
| :--- | :--- | :--- | :--- | 
| `connector.domain` | The domain for the eIDAS connector. | String | - |
| `connector.base-url` | The base URL of the Connector, including protocol, domain and context path. | String | `https://${connector.domain}`<br />`${server.servlet.context-path}` |
| `connector.backup-directory` | Directory where caches and backup files are stored during execution. | [File](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/File.html) | - |
| `connector.development-mode` | Tells whether we are running the connector in "development mode". This can mean that we allow any TLS server certificates or that other settings are setup with less security. | Boolean | `false` |
| `connector.country` | The country code for the eIDAS Connector. | String | `SE` |
| `connector.idp.*` | Configuration for the IdP part of the eIDAS Connector. See [Connector IdP Configuration](#connector-idp-configuration) below. | [ConnectorIdpProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/ConnectorIdpProperties.java) | - |
| `connector.eidas.*` | The configuration for the eIDAS authentication. See [eIDAS Authentication Configuration](#eidas-authentication-configuration) below. | [EidasAuthenticationProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/EidasAuthenticationProperties.java) | - |
| `connector.eu-metadata.*` | Configuration for retrieval of aggregated EU metadata. See [EU Metadata Configuration](#eu-metadata-configuration) below. | [EuMetadataProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/ConnectorConfigurationProperties.java) | - |
| `connector.prid.*` | Configuration for the [PRID Service](#prid-configuration). | [PridServiceProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/ConnectorConfigurationProperties.java) | - |
| `connector.idm.*` | Configuration for integration against the [Identity Matching Service](#idm-configuration). | [IdmProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/IdmProperties.java) | Not active |

<a name="connector-idp-configuration"></a>
### Connector IdP Configuration

**Description:** Configuration of the IdP part of the eIDAS Connector. Most part of this configuration
is performed by configuring the [Spring Security SAML Identity Provider](https://docs.swedenconnect.se/saml-identity-provider/configuration.html). This section describes additional settings concerning the
SAML IdP.

**Java class:** [ConnectorIdpProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/ConnectorIdpProperties.java)

| Property | Description | Type | Default value |
| :--- | :--- | :--- | :--- |
| `supported-loas` | The authentication context class reference URI:s (i.e., LoA:s or Level of Assurance URI:s) supported by this IdP. | List of strings | See below. |
| `entity-categories` | The SAML metadata entity categories that this SAML IdP declares. See [Entity Categories for the Swedish eID Framework](https://docs.swedenconnect.se/technical-framework/latest/06_-_Entity_Categories_for_the_Swedish_eID_Framework.html) for possible values. | List of strings | An empty list |
| `ping-whitelist` | A list of SAML entityID:s for the SP:s that are allowed to send special "eIDAS ping" authentication requests to the connector. If the list is empty, no ping requests will be served. | List of strings | An empty list (ping is disabled). |

By default the eIDAS Connector IdP will support the following authentication context class reference URI:s:

- `http://id.elegnamnden.se/loa/1.0/eidas-low`
- `http://id.elegnamnden.se/loa/1.0/eidas-nf-low`
- `http://id.elegnamnden.se/loa/1.0/eidas-sub`
- `http://id.elegnamnden.se/loa/1.0/eidas-nf-sub`
- `http://id.elegnamnden.se/loa/1.0/eidas-high`
- `http://id.elegnamnden.se/loa/1.0/eidas-nf-high`

<a name="eidas-authentication-configuration"></a>
### eIDAS Authentication Configuration

**Description:** The configuration for the eIDAS authentication.

**Java class:** [EidasAuthenticationProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/EidasAuthenticationProperties.java)

| Property | Description | Type | Default value |
| :--- | :--- | :--- | :--- |
| `entity-id` | The SAML entityID for the eIDAS SP.<br /><br />**Note:** Care should be taken if changing this value from its defaults since many eIDAS countries expect the entityID to be the same as the metadata location (which is fixed). | String | `${connector.base-url}`<br />`/metadata/sp` |
| `credentials.*` | The credentials for the SP part of the eIDAS Connector. If not assigned, the keys configured for the SAML IdP will be used also for the SP. See [Credentials Configuration](https://docs.swedenconnect.se/saml-identity-provider/configuration.html#credentials-configuration) for how to configure the different credentials. | [CredentialConfigurationProperties](https://github.com/swedenconnect/saml-identity-provider/blob/main/autoconfigure/src/main/java/se/swedenconnect/spring/saml/idp/autoconfigure/settings/CredentialConfigurationProperties.java) | - |
| `provider-name` | The "provider name" that we should include in `AuthnRequest` messages being sent to the foreign country. | String | "Swedish eIDAS Connector" |
| `requires-signed`<br />`-assertions` | Whether we require signed eIDAS assertions. | Boolean | `false` |
| `preferred-binding` | The preferred binding to use when sending authentication requests. Possible values are `urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST` and `urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect` | String | `urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST` |
| `supported-name-ids` | An ordered list of supported NameID formats. | List of strings | persistent, transient, unspecified<sup>*</sup> |
| `skip-scoping-for` | Some eIDAS countries can not handle the `Scoping` element in `AuthnRequest` messages. This setting contains the country codes for those countries that we should not include this element for. | List of strings | - |
| `metadata.*` | Configuration for eIDAS SP SAML metadata. See [eIDAS SP Metadata Configuration](#eidas-sp-metadata-configuration) below. | [EidasSpMetadataProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/EidasSpMetadataProperties.java) | - |

> **\[*\]**: `urn:oasis:names:tc:SAML:2.0:nameid-format:persistent`, `urn:oasis:names:tc:SAML:2.0:nameid-format:transient`, `urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified`.

<a name="eidas-sp-metadata-configuration"></a>
### eIDAS SP Metadata Configuration

**Description:** Configuration for the eIDAS SP SAML metadata.

**Java class:** [EidasSpMetadataProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/EidasSpMetadataProperties.java)

The metadata configuration inherits from the https://github.com/swedenconnect/saml-identity-provider
project and most of the configuration is documented for in the [Metadata Configuration](https://docs.swedenconnect.se/saml-identity-provider/configuration.html#metadata-configuration) section. 
Below follows the settings that extend the above configuration.

| Property | Description | Type | Default value |
| :--- | :--- | :--- | :--- |
| `application-identifier-prefix` | The value to insert for the eIDAS entity category `http://eidas.europa.eu/`<br />`entity-attributes/application-identifier`. The current version of the connector will always be appended to this value. | String | `SE:connector:` |
| `protocol-versions` | The values to use for the eIDAS entity category `http://eidas.europa.eu/`<br />`entity-attributes/protocol-version`. | A list of version strings. | - |
| `node-country` | The node country extension to include. | String | `SE` |

<a name="eu-metadata-configuration"></a>
### EU Metadata Configuration

**Description:** Configuration for retrieval of aggregated EU metadata.

**Java class:** [EuMetadataProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/ConnectorConfigurationProperties.java)

| Property | Description | Type | Default value |
| :--- | :--- | :--- | :--- |
| `location` | The location of the metadata. Can be an URL, a file, or even a classpath resource. | [Resource](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/Resource.html) | - |
| `https-trust-bundle` | If `location` is an HTTPS resource, this setting may be used to specify a [Spring SSL Bundle](https://spring.io/blog/2023/06/07/securing-spring-boot-applications-with-ssl) that specifies the trusted root certificates to be used for TLS server certificate verification. If no bundle is given, the Java trust defaults will be used. | String | - |
| `backup-location` | If the `location` setting is an URL, a "backup location" may be assigned to store downloaded metadata. | [File](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/File.html) | - |
| `validation-certificate` | The certificate used to validate the metadata. | [Resource](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/Resource.html) pointing at the certificate resource. | - |
| `skip-hostname-verification` | Whether to skip TLS hostname verification. Useful during testing. | Boolean | `false` |
| `http-proxy.*` | If the `location` setting is an URL and a HTTP proxy is required this setting configures this proxy. | [MetadataProviderConfigurationProperties.HttpProxy](https://github.com/swedenconnect/saml-identity-provider/blob/main/autoconfigure/src/main/java/se/swedenconnect/spring/saml/idp/autoconfigure/settings/MetadataProviderConfigurationProperties.java) | - | 

<a name="prid-configuration"></a>
### PRID Configuration

**Description:** Configuration for the PRID (Provisional Identifier) calculation.

**Java class:** [PridServiceProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/ConnectorConfigurationProperties.java)

| Property | Description | Type | Default value |
| :--- | :--- | :--- | :--- |
| `policy-resource` | A [Resource](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/Resource.html) pointing at the file containing the PRID configuration, see [eIDAS Connector Provisional Identifier (PRID) Calculation](https://docs.swedenconnect.se/eidas-connector/prid.html). | [Resource](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/Resource.html) | - |
| `update-interval` | Indicates how often the policy should be re-loaded (value is given in seconds). | Integer | `600` (10 minutes) |

<a name="idm-configuration"></a>
### Identity Matching Configuration

**Description:** Configuration for the integration against the Identity Matching service.

The connector needs to obtain a valid OAuth2 access token in order to invoke the Identity
Matching API. Therefore, OAuth2 configuration settings need to be supplied.

**Java class:** [IdmProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/IdmProperties.java)

| Property | Description | Type | Default value |
| :--- | :--- | :--- | :--- |
| `active` | Whether the IdM feature is active or not. | Boolean | `false` |
| `service-url` | The URL to the Identity Matching service. Will be displayed in the "select country" view. | String | - |
| `api-base-url` | The base URL for the Identity Matching Query API. Must not end with a '/'. | String | `service-url` |
| `trust-bundle` | A reference to a Spring Boot SSL Bundle holding the trust configuration for TLS-calls against the IdM server. If no bundle is set, the system defaults are used. | String | - |
| `oauth2.client-id` | The Connector OAuth2 client ID. Used for check calls. | String | - |
| `oauth2.check-scopes` | The scope(s) to request for making check calls the IdM Query API. | List of strings | - |
| `oauth2.get-scopes` | The scope(s) to request for making get calls the IdM Query API. | List of strings | - |
| `oauth2.resource-id` | The OAuth2 ID for the Identity Matching service. | String | - |
| `oauth2.credential.*` | The credential to use for authentication against the Authorization Server (if the connector acts as an OAuth2 client) OR for use of signing of access tokens (if the connector also acts as an OAuth2 Authorization Server). If not assigned, the connector default credential will be used.<br />See [credentials-support](https://docs.swedenconnect.se/credentials-support/) for how to configure credentials. | [PkiCredentialConfigurationProperties](https://github.com/swedenconnect/credentials-support/blob/main/credentials-support/src/main/java/se/swedenconnect/security/credential/config/properties/PkiCredentialConfigurationProperties.java) | The default IdP credential |
| `oauth2.server.issuer` | Assigned when the connector acts as an OAuth2 AS. The issuer ID to use for the issued access tokens. | String | - |
| `oauth2.server.lifetime` | The duration (lifetime) for issued access tokens. | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) | 1 hour |

**Note**: The connector either sends a token request to the configured OAuth2 Authorization Server in order to obtain the Access Token (`client`-settings should be supplied) OR the connector can act as an OAuth2 Authorization Server itself (`server`-settings should be set).

<a name="eidas-connector-ui-configuration"></a>
## eIDAS Connector UI Configuration

**Description:** Configuration specific for the UI of the eIDAS Connector.

**Java class:** [UiConfigurationProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/UiConfigurationProperties.java)

| Property | Description | Type | Default value |
| :--- | :--- | :--- | :--- | 
| `ui.languages[].*` | A list of the supported languages where the fields are `tag` containing the two-letter ISO-language code and `text` contains the text to display in the UI for changing to this language. | List of language | - |
| `ui.selected-`<br />`country-cookie.*` | Cookie settings for the cookie that is used to remember a user's selection of a country (in between sessions). See [Cookie Configuration](#cookie-configuration) below. | [UiConfigurationProperties.Cookie](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/UiConfigurationProperties.java) | Default settings for the cookie with the name set to `selectedCountry` |
| `ui.selected-country-`<br />`session-cookie.*` | Cookie settings for the cookie that is used to remember a user's selection of a country within a session. Used for signing services. See [Cookie Configuration](#cookie-configuration) below. | [UiConfigurationProperties.Cookie](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/UiConfigurationProperties.java) | Default settings for the cookie with the name set to `selectedCountrySession` |
| `ui.idm-consent-`<br />`session-cookie.*` | Cookie settings for the cookie that is used to remember a user's consent to obtaining the user's Identity Matching within a session. Used for signing services. See [Cookie Configuration](#cookie-configuration) below. | [UiConfigurationProperties.Cookie](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/UiConfigurationProperties.java) | Default settings for the cookie with the name set to `idmConsentSession` |
| `ui.idm-hide-`<br />`banner-cookie.*` | Cookie settings for the cookie that controls whether the IdM banner (at the country selection page) should be hidden. See [Cookie Configuration](#cookie-configuration) below. | [UiConfigurationProperties.Cookie](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/UiConfigurationProperties.java) | Default settings for the cookie with the name set to `idmHideBanner` |
| `ui.accessibility-url` | URL to the eIDAS Connector web accessibility report. | String | - |

<a name="cookie-configuration"></a>
### Cookie Configuration

**Description:** Configuration for a Connector cookie.

**Java class:** [UiConfigurationProperties.Cookie](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/UiConfigurationProperties.java)

**Java class:** [UiConfigurationProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/UiConfigurationProperties.java)

| Property | Description | Type | Default value |
| :--- | :--- | :--- | :--- | 
| `name` | The cookie name. | String | - |
| `domain` | The cookie domain. | String | `${connector.domain}` |
| `path` | The cookie path. | String | `${server.servlet.context-path}` |

---

Copyright &copy; 2017-2025, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).