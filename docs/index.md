![Logo](images/sweden-connect.png)

# The Swedish eIDAS Connector

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

The Swedish eIDAS Connector is an Open Source component, and is being used as an eIDAS connector for the Swedish participation of the [eIDAS federation](https://digital-strategy.ec.europa.eu/en/policies/eidas-regulation).

The Swedish eIDAS Connector is a [Spring Boot](https://spring.io/projects/spring-boot) application built on top of the [Spring Security SAML Identity Provider](https://github.com/swedenconnect/saml-identity-provider) library. It acts as a SAML Identity Provider in the [Sweden Connect](https://www.swedenconnect.se) federation, and as a SAML Service Provider against the eIDAS federation.

- [Release Notes](https://docs.swedenconnect.se/eidas-connector/release-notes.html)

The following resources are available for information about configuration, deployment and maintenance of the service:

- [Building and Deploying the eIDAS Connector](https://docs.swedenconnect.se/eidas-connector/installation.html) - How to build and deploy the service.

- [Configuration of the Swedish eIDAS Connector](https://docs.swedenconnect.se/eidas-connector/configuration.html) - A step-by-step guide for how to configure the Connector service.

- [Configuration Reference for the Swedish eIDAS Connector](https://docs.swedenconnect.se/eidas-connector/configuration-reference.html) - A configuration reference with descriptions of all settings for the Swedish eIDAS Connector.

- [Swedish eIDAS Connector Audit Logging](https://docs.swedenconnect.se/eidas-connector/audit.html) - A reference for all the audit events produced by the Swedish eIDAS Connector.

- [Management using the Actuator](https://docs.swedenconnect.se/eidas-connector/management.html) - The Spring Boot [Actuator Endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html) can be used for supervision of the application. This resource supplies information of relevant endpoints for management and supervision of the Swedish eIDAS Connector.

- [eIDAS Connector Provisional Identifier (PRID) Calculation](https://docs.swedenconnect.se/eidas-connector/prid.html) - Description of how to configure the Provisional Identifier Calculation for the different countries that are connected to the Swedish eIDAS Connector. Also, see the [eIDAS Constructed Attributes Specification for the Swedish eID Framework](https://docs.swedenconnect.se/technical-framework/latest/11_-_eIDAS_Constructed_Attributes_Specification_for_the_Swedish_eID_Framework.html).

<img align="right" src="images/eu-funded.png"/>
<br />
---

Copyright &copy; 2017-2025, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
