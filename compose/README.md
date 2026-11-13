![Logo](../docs/images/sweden-connect.png)

# Compose Setup for the eIDAS Connector

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A Docker Compose setup that runs the eIDAS connector of this repository together with the [eIDAS simulator](../eidas-simulator/README.md) and the Sweden Connect test services, so that a complete flow can be run on a developer machine:

Test my eID or the test client → connector → simulated foreign eIDAS node → connector → Test my eID or the test client.

> All keys, key stores and certificates in this directory are test credentials. They are public and offer no protection. Never use them for anything but this setup.

1. [Setting up](#setting-up)
2. [Starting and stopping](#starting-and-stopping)
3. [Running a test flow](#running-a-test-flow)
4. [Services](#services)
5. [Keys and certificates](#keys-and-certificates)
6. [Metadata](#metadata)
7. [Troubleshooting](#troubleshooting)

<a name="setting-up"></a>
## 1. Setting up

You need:

- Docker with Docker Compose.
- Java 21 and Maven 3.8 or later, to build the images of the connector and the simulator.

### The hosts file

All services are reached through the host name `local.eidas.swedenconnect.se`. Add it to your hosts file, `/etc/hosts` on macOS and Linux, or `C:\Windows\System32\drivers\etc\hosts` on Windows:

```
127.0.0.1   local.eidas.swedenconnect.se
```

The containers reach each other through the same name, so all URLs work both from your browser and from the services themselves.

### Building the images

The connector and the simulator run from images built from this repository. Build them, from the root of the repository, with:

```
compose/scripts/build-images.sh
```

The script installs the modules in your local Maven repository and builds the images `eidas-connector` and `eidas-simulator` into your local Docker daemon with Jib. Run it again after changing the code. Any arguments are passed on to Maven, for example `-o` to build offline.

To use other images, set `CONNECTOR_IMAGE` or `SIMULATOR_IMAGE`, for example `SIMULATOR_IMAGE=ghcr.io/swedenconnect/eidas-simulator:latest`. The images of the other services can be changed with `METADATA_AGGREGATOR_IMAGE`, `TEST_MY_EID_IMAGE` and `TEST_CLIENT_IMAGE`.

<a name="starting-and-stopping"></a>
## 2. Starting and stopping

In the `compose` directory:

```
docker compose up -d
```

The first start takes a minute or so. The connector starts when the metadata aggregator and the simulator are healthy. Check the state with `docker compose ps`.

When everything runs, open the start page, [start-page.html](start-page.html), directly from disk in your browser. It links to every service.

Stop the setup with:

```
docker compose down
```

The setup does not overlap with the [Sweden Connect local federation](https://github.com/swedenconnect/local-federation): it has its own host name, ports, network and keys, so both can run at the same time.

<a name="running-a-test-flow"></a>
## 3. Running a test flow

All HTTPS services use the same self-signed certificate. Your browser shows a warning for it the first time you open each service. Open the connector, the simulator, Test my eID and the test client (see the start page) and accept the warning for each of them **before** you log in, or a login stops halfway with a certificate error.

### With Test my eID

1. Open Test my eID, https://local.eidas.swedenconnect.se:12030/testmyeid, and choose "Foreign eID".
2. The connector shows its country selection. Choose a country. All simulated countries are listed.
3. The page of the simulated country shows the request from the connector and lets you:
   - pick one of the users of the country, or "New user ..." to enter a user of your own,
   - change the attribute values before the response is sent, and save them as a user of your own,
   - pick the level of assurance, among those that the country supports and the request allows,
   - send an error response instead, for example a cancelled or failed authentication.
4. Press "Authenticate" or "Send error response". The response is sent to the connector, which sends its own response to Test my eID.
5. Test my eID shows the attributes that it received, among them the PRID calculated by the connector, or the error.

Only the attributes that the connector requested, that the country supports and that the user has a value for are released. The page of the simulated country only shows those attributes.

### With the test client

The test client builds SAML authentication requests in detail and shows the response and the assertion.

1. Open the test client, https://local.eidas.swedenconnect.se:12040.
2. Select the SP "Test SP - SP for eIDAS authentication" and the Identity Provider "Foreign eID" (the connector), and continue.
3. Change the request if needed, for example the requested levels of assurance, and send it.
4. Continue at the connector and the simulator as above. The test client then shows the response.

<a name="services"></a>
## 4. Services

### Port ranges

| Ports | Service |
| :--- | :--- |
| `12000-12009` | [Metadata aggregator](#metadata-aggregator) |
| `12010-12019` | [eIDAS connector](#eidas-connector) |
| `12020-12029` | [eIDAS simulator](#eidas-simulator) |
| `12030-12039` | [Test my eID](#test-my-eid) |
| `12040-12049` | [Sweden Connect test client](#test-client) |

There are no base services. Redis is not used, the connector keeps its state in memory.

<a name="metadata-aggregator"></a>
### Metadata aggregator

The [metadata aggregator](https://github.com/swedenconnect/local-federation) (`ghcr.io/swedenconnect/metadata-aggregator`) publishes the Sweden Connect side of the setup: the IdP metadata of the connector and the SP metadata of Test my eID and the test client. It reads the entity metadata files in `config/metadata-aggregator/metadata`, and signs the feed with the key in `config/metadata-aggregator/metadata-signing.jks`.

**Port:** `12000`

**URLs:**

- https://local.eidas.swedenconnect.se:12000/metadata/mdx/role/idp.xml – The Identity Providers, that is, the connector.
- https://local.eidas.swedenconnect.se:12000/metadata/mdx/role/sp.xml – The Service Providers.

**Configuration:** `config/metadata-aggregator`

<a name="eidas-connector"></a>
### eIDAS connector

The connector built from this repository (image `eidas-connector`). It uses the aggregated metadata of the simulator as its EU metadata, and the metadata of the metadata aggregator for the Sweden Connect side.

**Ports:** `12010` (the application) and `12011` (management)

**EntityIDs:**

- `https://local.eidas.swedenconnect.se/eidas` – The Identity Provider in Sweden Connect.
- `https://local.eidas.swedenconnect.se:12010/idp/metadata/sp` – The eIDAS Service Provider.

**URLs:**

- https://local.eidas.swedenconnect.se:12010/idp/metadata/idp.xml – The IdP metadata.
- https://local.eidas.swedenconnect.se:12010/idp/metadata/sp – The eIDAS SP metadata, read by the simulator.
- https://local.eidas.swedenconnect.se:12011/actuator/health – Health.
- https://local.eidas.swedenconnect.se:12011/actuator/info – Information, among others the countries of the EU metadata.

**Configuration:** `config/connector`, with the profile `compose` (`application-compose.yml`). The PRID policy is `config/connector/prid-policy.yml`.

<a name="eidas-simulator"></a>
### eIDAS simulator

The [eIDAS simulator](../eidas-simulator/README.md) (image `eidas-simulator`) acts as the eIDAS proxy services of the countries that are available in production. It reads the eIDAS SP metadata of the connector from the connector.

**Ports:** `12020` (the application) and `12021` (management)

**EntityIDs:** `https://local.eidas.swedenconnect.se:12020/simulator/<country>/ServiceMetadata`, where `<country>` is the country code in lower case, for example https://local.eidas.swedenconnect.se:12020/simulator/at/ServiceMetadata. The entityID is also the URL of the metadata of the country.

**URLs:**

- https://local.eidas.swedenconnect.se:12020/simulator/ – A page listing the simulated countries.
- https://local.eidas.swedenconnect.se:12020/simulator/metadata – The signed, aggregated metadata of all countries, the EU metadata of the connector.
- https://local.eidas.swedenconnect.se:12021/actuator/health – Health.

**Configuration:** `config/simulator`, with the profile `compose` (`application-compose.yml`). To simulate only some countries, list them under `simulator.active-countries`.

<a name="test-my-eid"></a>
### Test my eID

[Test my eID](https://github.com/swedenconnect/test-my-eid) (`ghcr.io/swedenconnect/test-my-eid`), with SAML only.

**Port:** `12030`

**EntityIDs:**

- `https://local.eidas.swedenconnect.se/testmyeid`
- `https://local.eidas.swedenconnect.se/testmyeid-sign` – For signing a test message.

**URLs:**

- https://local.eidas.swedenconnect.se:12030/testmyeid – The application.
- https://local.eidas.swedenconnect.se:12030/testmyeid/metadata and https://local.eidas.swedenconnect.se:12030/testmyeid/metadata/sign – The SP metadata.

**Configuration:** `config/test-my-eid`

<a name="test-client"></a>
### Sweden Connect test client

The [Sweden Connect test client](https://github.com/swedenconnect/sweden-connect-test-client) (`ghcr.io/swedenconnect/sweden-connect-test-client`), with SAML only.

**Port:** `12040`

**SPs:**

| EntityID | Description |
| :--- | :--- |
| `https://local.eidas.swedenconnect.se/test-client/eidas` | A public sector SP that requests the PRID attribute. |
| `https://local.eidas.swedenconnect.se/test-client/eidas-private` | A private sector SP. The connector sends `SPType` private to the country. |
| `https://local.eidas.swedenconnect.se/test-client/eidas-attributes` | A public sector SP that also requests attributes beyond the eIDAS minimum data set, such as birth name, place of birth, address, e-mail and phone number. |

**URLs:**

- https://local.eidas.swedenconnect.se:12040 – The application.
- https://local.eidas.swedenconnect.se:12040/saml/metadata/eidas – The metadata of the public sector SP.
- https://local.eidas.swedenconnect.se:12040/saml/metadata/eidas-private – The metadata of the private sector SP.
- https://local.eidas.swedenconnect.se:12040/saml/metadata/eidas-attributes – The metadata of the SP requesting more attributes.

**Configuration:** `config/test-client`

<a name="keys-and-certificates"></a>
## 5. Keys and certificates

The keys are committed to the repository, so a fresh clone runs without extra steps. The password of every key store and key is `secret`.

| File | Holds |
| :--- | :--- |
| `config/common/tls.jks` | The TLS key of `local.eidas.swedenconnect.se`, used by all services. The certificate is `config/common/tls.crt`. |
| `config/common/trust.jks` | The trust store of all services, holding the TLS certificate. |
| `config/metadata-aggregator/metadata-signing.jks` | The metadata signing key of the aggregator. The certificate is `config/common/aggregator-metadata-signing.crt`. |
| `config/connector/connector-keys.jks` | The keys of the connector: `idp-sign`, `idp-encrypt`, `sp-sign`, `sp-encrypt` and `metadata-sign`. The certificate of the metadata signing key is `config/common/connector-metadata-signing.crt`. |
| `config/simulator/simulator-keys.jks` | The metadata signing key of the simulator (`metadata`), and the keys of each country (`<country>-sign` and, for some countries, `<country>-encrypt`). The type and size of the key of a country are the same as in the production metadata. The certificate of the metadata signing key is `config/common/simulator-metadata-signing.crt`. |
| `config/test-my-eid/sp-keys.jks` | The SAML keys of Test my eID. |
| `config/test-client/sp-keys.jks` | The SAML keys of the test client. |

To generate new keys, run:

```
compose/scripts/generate-credentials.sh [all | tls | aggregator | connector | simulator | test-my-eid | test-client] ...
```

Without arguments, all keys are generated. The `simulator` target also copies the key store to the tests of the simulator. After new keys for the connector, Test my eID or the test client, refresh their metadata in the aggregator, see [Metadata](#metadata). Restart the services after new keys.

<a name="metadata"></a>
## 6. Metadata

The metadata of the connector (its IdP role), Test my eID and the test client is kept as files in `config/metadata-aggregator/metadata`, which the metadata aggregator publishes. When the metadata of one of these services changes, for example after new keys or a configuration change, fetch it again from the running services with:

```
compose/scripts/refresh-metadata.sh [connector | test-my-eid | test-client] ...
```

The aggregator reads the files again within a minute. The services download the aggregated metadata again within ten minutes, or at once when they are restarted.

The connector and the simulator read each other's metadata directly: the connector reads the aggregated metadata of the simulator, and the simulator reads the eIDAS SP metadata of the connector.

<a name="troubleshooting"></a>
## 7. Troubleshooting

- **A login stops with a certificate error.** Accept the certificate warning of each service in your browser first, see [Running a test flow](#running-a-test-flow).
- **The connector is not healthy, `saml-metadata` is `OUT_OF_SERVICE`.** The connector has no valid SP metadata from the aggregator. Check that `config/metadata-aggregator/metadata` holds the metadata files, see [Metadata](#metadata).
- **Test my eID says that no identity providers are available.** It started before the aggregator published the connector metadata. Restart it with `docker compose restart test-my-eid`.
- **The simulator shows "The request cannot be processed".** The simulator could not establish the connector or trust the request, for example because the connector metadata could not be read or the signature of the request is not valid. The page tells why, and the log of the simulator (`docker logs eidas-simulator`) has the details.

---

Copyright &copy; 2017-2026, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
