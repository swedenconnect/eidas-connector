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

Configuration specific for the eIDAS Connector.

| Property | Description | Type | Default value |
| :--- | :--- | :--- | :--- |
| `connector.eu-metadata` | Configuration for retrieval of aggregated EU metadata. See [EU Metadata Configuration](#eu-metadata-configuration) below. | [EuMetadataConfiguration](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/java/se/swedenconnect/eidas/connector/config/ConnectorConfigurationProperties.java) | - |


<a name="eu-metadata-configuration"></a>
### EU Metadata Configuration

| Property | Description | Type | Default value |
| :--- | :--- | :--- | :--- |
| `location` | The location of the metadata. Can be an URL, a file, or even a classpath resource. | [Resource](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/Resource.html) | - |
| `backup-location` | If the `location` setting is an URL, a "backup location" may be assigned to store downloaded metadata. | [File](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/File.html) | - |
| `validation-certificate` | The certificate used to validate the metadata. | [Resource](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/Resource.html) pointing at the certificate resource. | - |
| `http-proxy.*` | If the `location` setting is an URL and a HTTP proxy is required this setting configures this proxy. | [MetadataProviderConfigurationProperties.HttpProxy](https://github.com/swedenconnect/saml-identity-provider/blob/main/autoconfigure/src/main/java/se/swedenconnect/spring/saml/idp/autoconfigure/settings/MetadataProviderConfigurationProperties.java) | - |

---

Copyright &copy; 2017-2023, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).