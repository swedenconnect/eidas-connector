# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

The Swedish eIDAS Connector — an Open Source Spring Boot application used as the Swedish eIDAS connector node. It is simultaneously:

- a SAML **Identity Provider** in the Sweden Connect federation (domestic side), and
- a SAML **Service Provider** towards the eIDAS federation (foreign side).

It is built on top of [`saml-idp-spring-boot-starter`](https://github.com/swedenconnect/saml-identity-provider) (`se.swedenconnect.spring.saml.idp`). User-facing documentation lives at https://docs.swedenconnect.se/eidas-connector and under `docs/` in this repo.

## Build, test, run

This is a multi-module Maven project. Java 21+ and Maven 3.8+ required.

```bash
# Build all modules
mvn clean install

# Compile only
mvn clean compile

# Run the full test suite
mvn test

# Run tests for a single module
mvn -pl idp test
mvn -pl prid test
mvn -pl attribute-handling test

# Run a single test class
mvn -pl idp test -Dtest=AuthnContextClassRefMapperTest

# Run a single test method
mvn -pl idp test -Dtest=AuthnContextClassRefMapperTest#methodName

# JaCoCo coverage report (after tests)
mvn jacoco:report

# Build a local Docker image via Jib (no daemon push)
cd idp && mvn jib:dockerBuild

# Build & push multi-arch image to ghcr.io (requires GITHUB_USER and
# GITHUB_ACCESS_TOKEN env vars)
./scripts/publish-image.sh

# Release build (signs artifacts, attaches sources/javadoc, publishes to Central)
mvn -Prelease deploy
```

Build enforces dependency convergence via `maven-enforcer-plugin` — version conflicts between transitive dependencies will fail the build.

## Module layout

The reactor has three modules that build in this order:

1. **`attribute-handling/`** — `eidas-attribute-handling`. Conversion of attribute sets between the eIDAS attribute namespace and the Swedish eID attribute namespace. Pure library, no Spring Boot.
2. **`prid/`** — `eidas-prid`. Provisional Identifier (PRID) calculation logic (see `docs/prid.md` and the eIDAS Constructed Attributes Specification). Pluggable `PridGenerator` strategies (`PridGenDefaultEidas`, `PridGenColResistEidas`, `PridGenBase64Eidas`, `PridGenTestEidas`) selected per country via policy.
3. **`idp/`** — `eidas-connector`. The Spring Boot application proper. Depends on the other two modules.

The parent POM owns dependency management for Spring Boot, Spring Framework, OpenSAML/Sweden Connect SAML libraries, Bouncy Castle, JUnit, and the credentials-support libraries. Submodule POMs only declare what they use.

## Architecture (idp module)

Entry point: `EidasConnectorApplication` (`@SpringBootApplication`, `@EnableScheduling`).

The flow is roughly: a Swedish SP sends a SAML AuthnRequest → the Connector's IdP side receives it → the user picks a country → the Connector's SP side issues an eIDAS AuthnRequest to the chosen foreign IdP → the eIDAS response is processed, attributes are mapped, a PRID is computed (and optionally enriched via Identity Matching) → the Connector's IdP side issues a SAML assertion back to the Swedish SP.

Key packages under `idp/src/main/java/se/swedenconnect/eidas/connector/`:

- **`authn/`** — orchestration of an authentication flow.
  - `EidasAuthenticationProvider` extends `AbstractUserRedirectAuthenticationProvider` from the SAML IdP starter. It is the bridge between the IdP-side request and the SP-side foreign authentication. Handles attribute release, PRID generation, IdM lookup, error mapping.
  - `EidasAuthenticationController` (`/extauth/...`) — the external authentication endpoint. Hosts country selection, signature consent, IdM consent screens, and the eIDAS assertion consumer at `/extauth/saml2/post`.
  - `EidasAuthenticationToken` — carries state across the multi-step authentication.
  - `EidasCountryHandler`, `EidasSsoVoter` — country selection rules and SSO eligibility.
  - **`authn/sp/`** — SAML SP role: `EidasAuthnRequestGenerator`, `EidasResponseProcessor`, `EidasSpMetadataController` (publishes SP metadata at `/metadata/sp`), `AuthnContextClassRefMapper` (Swedish ↔ eIDAS LoA mapping), and response validators under `validators/`.
  - **`authn/metadata/`** — `EuMetadataProvider` / `DefaultEuMetadataProvider` aggregate the eIDAS country metadata; `CountryMetadata` is the per-country view used by the UI and SP.
  - **`authn/idm/`** — eIDAS Identity Matching client. `IdmClient` interface with `DefaultIdmClient` (real OAuth2-protected service) and `NoopIdmClient` (when IdM is disabled). `OAuth2Handler`/`OAuth2Server`/`AbstractOAuth2Handler`/`BearerAccessTokenHolder` handle token acquisition and caching.
  - **`authn/ui/`** — Thymeleaf model factories (`EidasUiModelFactory`, `SignUiModelFactory`, `IdmUiModelFactory`) and the `UiLanguageHandler`. Templates live in `src/main/resources/templates/`.
- **`config/`** — Spring configuration and `@ConfigurationProperties`. `ConnectorConfiguration` is the top-level wiring (depends on `openSAML` bean), `ConnectorSecurityConfiguration` defines the security filter chain, `ConnectorConfigurationProperties` is the typed root of `connector.*` properties, with sub-properties for `idp`, `eidas`, `idm`, `ui`, and SP metadata. `tomcat/` contains custom Tomcat connector tweaks (e.g., AJP).
- **`idp/`** — extensions to the SAML IdP library, currently `ConnectorSignatureMessagePreprocessor` (massages signature messages before display).
- **`audit/`** — audit logging. `ConnectorAuditPublisher` emits Spring Boot `AuditEvent`s defined in `ConnectorAuditEvents`. `data/` holds the structured payload classes.
- **`events/`** — internal Spring `ApplicationEvent`s (`BeforeCountrySelectionEvent`, `BeforeEidasAuthenticationEvent`, `SuccessEidasResponseEvent`, `ErrorEidasResponseEvent`, `IdentityMatching*Event`, `SignatureConsentEvent`, `EuMetadataEvent`, …). Audit publishing and other cross-cutting concerns subscribe to these — prefer adding a new event to coupling new logic into `EidasAuthenticationProvider`.
- **`actuator/`** — Spring Boot Actuator extensions: `CountriesInfoContributor`, `PridPolicyInfoContributor`, custom `Health` indicators (`IdmHealthIndicator`, `PridHealthIndicator`, `SamlMetadataHealthIndicator`), and `PridPolicyRefreshEndpoint` (`/actuator/refreshprid`). `CustomStatus` defines a `WARNING` health status mapped to HTTP 503.

Key resources under `idp/src/main/resources/`:

- `application.yml` — base configuration. Default ports: `8443` (servlet, context `/idp`) and `8444` (management). Most settings come from environment-specific profiles supplied at deploy time (see `docs/installation.md` and `docs/configuration.md`).
- `templates/` — Thymeleaf views (country select, sign consent, IdM consent, post-binding response).
- `messages.properties`, `messages_sv.properties`, `idp-errors/`, `custom-saml-errors.properties` — i18n (Swedish + English) and SAML error mapping.
- `env/localhost/`, `env/localdev/` — keystores for local development only. Never deploy these.

## Configuration

Configuration is driven by Spring `@ConfigurationProperties` rooted at `connector.*` (see `ConnectorConfigurationProperties` and the property files under `idp/src/main/java/.../config/`). The `configuration-processor` annotation processor generates `META-INF/spring-configuration-metadata.json` so editor autocomplete and `/docs/configuration-reference.md` stay accurate.

Sessions can be Redis-backed via `spring-session-data-redis` + `redisson-spring-boot-starter`. The default `application.yml` keeps `management.health.redis.enabled: false` — flip it on in deployments that use Redis.

Profile files live outside the repo at deploy time (see `docs/examples/`). Common env vars: `SPRING_CONFIG_ADDITIONAL_LOCATION`, `SPRING_PROFILES_ACTIVE`, `CONNECTOR_DIRECTORY`, `TOMCAT_AJP_SECRET`, `CONNECTOR_IDM_ACTIVE`.

## Coding conventions

- **Java 21**, source/target via `<release>`.
- **Lombok** is on the build path (`provided` scope) and used widely — chiefly `@Slf4j`, `@Getter`/`@Setter`, `@RequiredArgsConstructor`. The annotation processor is configured in the parent POM.
- **License header**: every Java file starts with the Apache 2.0 header `Copyright 2017-2026 Sweden Connect`. Keep it on new files and update the year range as needed.
- **Package-info**: every package has a `package-info.java` with a one-line Javadoc describing the package's purpose. Add one when introducing a new package.
- **`ApplicationVersion`** in the root package holds `MAJOR`/`MINOR`/`PATCH` constants and a `SERIAL_VERSION_UID` derived from them. Bump it when releasing.
- **Tests**: JUnit 5 + Mockito + AssertJ. The convenience base class `OpenSamlTestBase` bootstraps OpenSAML for tests that need it. Surefire is the runner; JaCoCo agent runs in `prepare-agent`. Coverage excludes `**/*Exception.class`.
- **Testing the Spring Boot app** uses `spring-boot-starter-test` with a dedicated `application.yml` under `idp/src/test/resources/`; properties resources are filtered by Maven (resource filtering is disabled for everything else).

Follow code style as specified in .claude/instructions/CodeStyle-Spring.xml and also inspections as given in
.claude/instructions/Inspections_SwedenConnect.xml.

## Release process

Versions are managed manually across all four POMs and `ApplicationVersion`. The recent commit pattern is a single commit/PR titled with the version (e.g. "2.0.10"). Release artifacts are published to Maven Central via `central-publishing-maven-plugin` under the `release` profile (signs with GPG, attaches sources + javadoc + aggregated javadoc). The Docker image is published separately to `ghcr.io/swedenconnect/eidas-connector` via `scripts/publish-image.sh`.

## Working with this code

- When changing the authentication flow, prefer publishing a new event in `events/` and subscribing from the cross-cutting code (audit, metrics) over inlining behaviour in `EidasAuthenticationProvider`/`EidasAuthenticationController`.
- When changing PRID logic, the change probably belongs in the `prid/` module, not `idp/`. The same goes for attribute mapping — that lives in `attribute-handling/`.
- When adding a configuration property, add it to the right `*Properties` class, document it in `docs/configuration-reference.md`, and ensure the `spring-boot-configuration-processor` picks it up (Lombok-generated setters are fine).
- The IdM (Identity Matching) feature can be entirely disabled — `NoopIdmClient` is wired in when inactive. Don't assume `IdmClient.isActive()` is `true`.
- Health indicators feed into a deployment-monitored `/actuator/health` endpoint; a misbehaving downstream (eIDAS metadata stale, IdM down) should surface as `WARNING` (HTTP 503) rather than failing requests.
