![Logo](images/sweden-connect.png)

# Building and Deploying the eIDAS Connector

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

## Table of Contents

1. [**Overview**](#overview)

2. [**Building from Source**](#building-from-source)

    2.1. [Prerequisites for Building](#prerequisites-for-building)
    
    2.2. [Getting the Source](#getting-the-source)

3. [**Maven Central**](#maven-central)

4. [**Building a Docker Image**](#building-a-docker-image)

    4.1. [Building using Jib](#building-using-jib)

    4.2. [Building from Dockerfile](#building-from-dockerfile)

5. [**Example Deployment**](#example-deployment)

    5.1. [Creating an Application Profile File](#creating-an-application-profile-file)
    
    5.2. [Connector Start Script](#connector-start-script)

6. [**Post-deployment Steps**](#post-deployment-steps)

    6.1. [Publishing Identity Provider Metadata](#publishing-identity-provider-metadata)
    
    6.2. [Publishing Service Provider Metadata to eIDAS](#publishing-service-provider-metadata-to-eidas)

---

<a name="overview"></a>
## 1. Overview

This document discusses how the eIDAS Connector is built and gives examples of how it can be deployed.

:grey_exclamation: Detailed knowledge of the role of the eIDAS Connector within both the domestic and the eIDAS-federation will be required, but this information is not provided here.

<a name="building-from-source"></a>
## 2. Building from Source

The eIDAS Connector can be built from source, but also downloaded from [Maven Central](#maven-central).

<a name="prerequisites-for-building"></a>
### 2.1. Prerequisites for Building

- Java Development Kit (JDK) 21, or later.
    - See [Oracle Java Downloads](https://www.oracle.com/se/java/technologies/downloads/), or [OpenJDK](https://openjdk.org).

- [Maven](https://maven.apache.org/download.cgi), version 3.8 or later.

- [Git](https://git-scm.com/downloads) - Needed to clone the eIDAS connector repository.

- And optionally, [Docker](https://docs.docker.com/get-started/get-docker/) which is needed to build a Docker image.

<a name="getting-the-source"></a>
### 2.2. Getting the Source

The source can be downloaded as a zip-archive from https://github.com/swedenconnect/eidas-connector
under the "Code"-button, and the "Download ZIP" archive.

A better choice is to clone or [fork](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/working-with-forks/fork-a-repo) the eIDAS Connector repository.

To clone the repository, do:

```
$ git clone https://github.com/swedenconnect/eidas-connector.git
```

<a name="building"></a>
### 2.3. Building

In the directory where you cloned, or downloaded, the eIDAS Connector source code, do:

```
$ mvn clean compile
```

The resulting jar-file containing the eIDAS Connector Spring Boot-application (with a bundled Tomcat) is found under `idp/target`, and named `eidas-connector-<version>.jar`.

<a name="maven-central"></a>
## 3. Maven Central

As an alternative to building the eIDAS Connector from source, it can be downloaded from [Maven Central](https://central.sonatype.com).

Go to https://repo1.maven.org/maven2/se/swedenconnect/eidas/eidas-connector and download the version you need.

<a name="building-a-docker-image"></a>
## 4. Building a Docker Image

If your deployment uses Docker, an eIDAS Connector Docker image will be needed. 

<a name="building-using-jib"></a>
### 4.1. Building using Jib

If you have access to the source code, a Docker image can be built using the [Jib Maven plugin](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin).

Check the [pom.xml](https://github.com/swedenconnect/eidas-connector/blob/master/idp/pom.xml) under the `idp`-directory for the Jib-settings used.

To build an image do:

```
$ cd idp
$ mvn jib:dockerBuild
```

<a name="building-from-dockerfile"></a>
### 4.2. Building from Dockerfile

The more conventional way of building a Docker image is to have a Dockerfile and build the image using docker commands.

See an example Dockerfile under [examples/Dockerfile](examples/Dockerfile).

Suppose that we have built the source as described above, then do the following:

```
$ cd idp
$ docker -f <your-path>/Dockerfile -t eidas-connector-image .
```

<a name="example-deployment"></a>
## 5. Example Deployment

This section illustrates how the eIDAS Connector is deployed to the Sweden Connect Sandbox-environment. We do not include keys and passwords, but all other settings used for the Sandbox-environment are present in the example.

The example assumes that a Docker image named `swedenconnect/eidas-connector` has been built.

<a name="creating-an-application-profile-file"></a>
### 5.1. Creating an Application Profile File

See [Configuration of the Swedish eIDAS Connector](configuration.html) for how to create a YML-configuration file. It is wise to use the base settings of the default [application.yml](https://github.com/swedenconnect/eidas-connector/blob/master/idp/src/main/resources/application.yml) and create a profile that extends and changes the default settings.

The [examples/sandbox/application-sandbox.yml](examples/sandbox/application-sandbox.yml) file illustrates how the profile `sandbox` is created and where we override default settings for the Sweden Connect Sandbox environment (passwords and credentials are not displayed).

<a name="connector-start-script"></a>
### 5.2. Connector Start Script

```
#!/bin/bash

#
# Start and deploy script for the eIDAS Connector in the Sandbox environment
#
echo Pulling swedenconnect/eidas-connector docker image ...
docker pull ghcr.io/swedenconnect/eidas-connector

echo Undeploying eidas-connector container ...
docker rm eidas-connector --force

CONNECTOR_HOME=/opt/connector
CONNECTOR_HTTPS_PORT=8414
CONNECTOR_ACTUATOR_PORT=8415
CONNECTOR_AJP_PORT=8014

AJP_SECRET="TODO:insert-secret"

echo Redeploying docker container eidas-connector ...
docker run -d --name eidas-connector --restart=always \
  -p ${CONNECTOR_AJP_PORT}:8009 \
  -p ${CONNECTOR_HTTPS_PORT}:8443 \
  -p ${CONNECTOR_ACTUATOR_PORT}:8444 \
  -e SPRING_CONFIG_ADDITIONAL_LOCATION=${CONNECTOR_HOME}/ \
  -e SPRING_PROFILES_ACTIVE=sandbox \
  -e CONNECTOR_DIRECTORY=${CONNECTOR_HOME} \
  -e TOMCAT_AJP_SECRET=${AJP_SECRET} \
  -e CONNECTOR_IDM_ACTIVE=true \
  -e "TZ=Europe/Stockholm" \
  -v /etc/localtime:/etc/localtime:ro \
  -v /opt/docker/eidas-connector:${CONNECTOR_HOME} \
  ghcr.io/swedenconnect/eidas-connector

echo Done!
```

<a name="post-deployment-steps"></a>
## 6. Post-deployment Steps

When the eIDAS Connector has successfully started<sup>*</sup>, it is time to perform some additional steps to connect it to the federations<sup>2</sup>.

> **\[1\]:** Check the logs that everything looks good, and make a call to the [Health-endpoint](https://docs.swedenconnect.se/management.html#the-health-endpoint) to ensure there are nothing that needs to be corrected.

> **\[2\]:** If, the installation is an update to an already existing installation of the eIDAS Connector the SAML metadata may not have to be published.

<a name="publishing-identity-provider-metadata"></a>
### 6.1. Publishing Identity Provider Metadata

To obtain the SAML metadata for the IdP part of the eIDAS Connector, open the URL `https://<domain>/idp/metadata/idp.xml` in a browser and save the metadata in an XML-file.

Depending on which environment/federation the metadata is to be published, the steps are a bit different.

- For Sweden Connect Production or QA, see https://www.swedenconnect.se/anslut.

- For Sweden Connect Sandbox, see https://eid.svelegtest.se/mdreg/home.

<a name="publishing-service-provider-metadata-to-eidas"></a>
### 6.2. Publishing Service Provider Metadata to eIDAS

To obtain the SAML metadata for the SP part of the eIDAS Connector, open the URL `https://<domain>/idp/metadata/sp` in a browser and save the metadata in an XML-file.

Next, Sweden Connect/Digg operations need to be involved to publish the metadata for the eIDAS federation. The distribution of metadata among the eIDAS countries is a manual process and requires many steps (outside of the scope for this document).


---

Copyright &copy; 2017-2025, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).