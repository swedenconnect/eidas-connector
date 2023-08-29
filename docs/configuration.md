![Logo](images/sweden-connect.png)

# Configuration of the Swedish eIDAS Connector

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

The Swedish eIDAS Connector is built using the [Spring Security SAML Identity Provider](https://github.com/swedenconnect/saml-identity-provider) libraries. Therefore, all the configuration of the SAML IdP part of
the Connector is done according to this library's configuration, see below.

## SAML IdP Configuration

See the [Configuration](https://docs.swedenconnect.se/saml-identity-provider/configuration.html) for the 
[Spring Security SAML Identity Provider](https://github.com/swedenconnect/saml-identity-provider).

## eIDAS Connector Configuration

**Description:** Configuration specific for the eIDAS Connector.

**Java class:** [ConnectorConfigurationProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/ConnectorConfigurationProperties.java)

| Property | Description | Type | Default value |
| :--- | :--- | :--- | :--- | 
| `connector.backup-directory` | Directory where caches and backup files are stored during execution. | [File](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/File.html) | - |
| `connector.idp.*` | Configuration for the IdP part of the eIDAS Connector. See [Connector IdP Configuration](#connector-idp-configuration) below. | [ConnectorIdpProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/ConnectorIdpProperties.java) | - |
| `connector.eidas.*` | The configuration for the eIDAS authentication. See [eIDAS Authentication Configuration](#eidas-authentication-configuration) below. | [EidasAuthenticationProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/EidasAuthenticationProperties.java) | - |
| `connector.eu-metadata.*` | Configuration for retrieval of aggregated EU metadata. See [EU Metadata Configuration](#eu-metadata-configuration) below. | [EuMetadataProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/ConnectorConfigurationProperties.java) | - |

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
| `credentials.*` | The credentials for the SP part of the eIDAS Connector. If not assigned, the keys configured for the SAML IdP will be used also for the SP. See [Credentials Configuration](https://docs.swedenconnect.se/saml-identity-provider/configuration.html#credentials-configuration) for how to configure the different credentials. | [CredentialConfigurationProperties](https://github.com/swedenconnect/saml-identity-provider/blob/main/autoconfigure/src/main/java/se/swedenconnect/spring/saml/idp/autoconfigure/settings/CredentialConfigurationProperties.java) | - |

<a name="eu-metadata-configuration"></a>
### EU Metadata Configuration

**Description:** Configuration for retrieval of aggregated EU metadata.

**Java class:** [EuMetadataProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/ConnectorConfigurationProperties.java)

| Property | Description | Type | Default value |
| :--- | :--- | :--- | :--- |
| `location` | The location of the metadata. Can be an URL, a file, or even a classpath resource. | [Resource](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/Resource.html) | - |
| `backup-location` | If the `location` setting is an URL, a "backup location" may be assigned to store downloaded metadata. | [File](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/File.html) | - |
| `validation-certificate` | The certificate used to validate the metadata. | [Resource](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/Resource.html) pointing at the certificate resource. | - |
| `http-proxy.*` | If the `location` setting is an URL and a HTTP proxy is required this setting configures this proxy. | [MetadataProviderConfigurationProperties.HttpProxy](https://github.com/swedenconnect/saml-identity-provider/blob/main/autoconfigure/src/main/java/se/swedenconnect/spring/saml/idp/autoconfigure/settings/MetadataProviderConfigurationProperties.java) | - |

## eIDAS Connector UI Configuration

**Description:** Configuration specific for the UI of the eIDAS Connector.

**Java class:** [UiConfigurationProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/UiConfigurationProperties.java)

| Property | Description | Type | Default value |
| :--- | :--- | :--- | :--- | 
| `ui.languages[].*` | A list of the supported languages where the fields are `tag` containing the two-letter ISO-language code and `text` contains the text to display in the UI for changing to this language. | List of language | - |
| `ui.selected-`<br />`country-cookie.*` | Cookie settings for the cookie that is used to remember a user's selection of a country (in between sessions). See [Cookie Configuration](#cookie-configuration) below. | [UiConfigurationProperties.Cookie](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/UiConfigurationProperties.java) | Default settings for the cookie with the name set to `selectedCountry` |
| `ui.selected-country-`<br />`session-cookie.*` | Cookie settings for the cookie that is used to remember a user's selection of a country within a session. Used for signing services. See [Cookie Configuration](#cookie-configuration) below. | [UiConfigurationProperties.Cookie](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/UiConfigurationProperties.java) | Default settings for the cookie with the name set to `selectedCountrySession` |
| `ui.accessibility-url` | URL to the eIDAS Connector web accessibility report. | String | - |
| `ui.idm.*` | Configuration for the Identity Matching feature. If this feature should be active, the `ui.idm.active` flag is set to `true` and the `service-url` is set to point to the eIDAS Identity Matching service. | IdM setting. | - |

<a name="cookie-configuration"></a>
### Cookie Configuration

**Description:** Configuration for a Connector cookie.

**Java class:** [UiConfigurationProperties.Cookie](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/UiConfigurationProperties.java)

**Java class:** [UiConfigurationProperties](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/UiConfigurationProperties.java)

| Property | Description | Type | Default value |
| :--- | :--- | :--- | :--- | 
| `name` | The cookie name. | String | - |
| `domain` | The cookie domain. | String | - |
| `path` | The cookie path. | String | `"/"` |

---

Copyright &copy; 2017-2023, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).