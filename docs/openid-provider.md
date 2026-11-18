![Logo](images/sweden-connect.png)

# The OpenID Provider of the Swedish eIDAS Connector

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

The Swedish eIDAS Connector is also an OpenID Provider in the Sweden Connect federation. A Relying Party gets the same
eIDAS authentication as a SAML Service Provider: the user selects a country and is authenticated at the eIDAS node of
that country. This page describes what a Relying Party can ask for.

The OpenID Provider follows the
[OpenID Connect Profile for Sweden Connect](https://docs.swedenconnect.se/technical-framework/latest/OpenID_Connect_Profile_for_Sweden_Connect.html)
and the
[OpenID Connect Claims and Scopes Specification for Sweden Connect](https://docs.swedenconnect.se/technical-framework/latest/OpenID_Connect_Claims_and_Scopes_Specification.html).

## Table of Contents

1. [**Endpoints**](#endpoints)

2. [**Scopes and Claims**](#scopes-and-claims)

3. [**Authentication Contexts**](#authentication-contexts)

4. [**Requesting a Country**](#requesting-a-country)

5. [**User Messages**](#user-messages)

6. [**Signature Approval**](#signature-approval)

7. [**Single Sign-On**](#single-sign-on)

---

<a name="endpoints"></a>
## 1. Endpoints

The issuer is the base URL of the connector, for example `https://connector.eidas.swedenconnect.se/idp`. The discovery
document is published at the issuer followed by `/.well-known/openid-configuration`, and the OpenID Federation entity
configuration at the issuer followed by `/.well-known/openid-federation`. The other endpoints are given in the
discovery document.

Only the authorization code flow is supported, and the client authenticates with `private_key_jwt` at the token
endpoint.

<a name="scopes-and-claims"></a>
## 2. Scopes and Claims

| Scope | Claims |
| :--- | :--- |
| `https://id.swedenconnect.se/scope/eidasNaturalPersonIdentity` | `https://id.swedenconnect.se/claim/prid`, `https://id.swedenconnect.se/claim/pridPersistence` and `http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier`. |
| `https://id.oidc.se/scope/naturalPersonInfo` | `family_name`, `given_name` and `birthdate`. `name` and `middle_name` are not delivered. |
| `profile` | `family_name`, `given_name`, `birthdate` and `gender`. The other claims of the scope are not delivered. |
| `https://id.oidc.se/scope/signApproval` | See [Signature Approval](#signature-approval). |
| `https://id.swedenconnect.se/scope/eidasSwedishIdentity` | `https://id.swedenconnect.se/claim/mappedPersonalIdentityNumber`, `https://id.swedenconnect.se/claim/mappedCoordinationNumber` and `https://id.swedenconnect.se/claim/identityBinding`, delivered from the UserInfo endpoint. Only offered when Identity Matching is active. |

The following claims may also be requested with the `claims` parameter. The foreign country is only asked for the
attributes that are requested, apart from the eIDAS minimum data set, which is always asked for. A country does not
have to deliver attributes outside the minimum data set.

| Claim | eIDAS attribute |
| :--- | :--- |
| `https://id.swedenconnect.se/claim/eidasCountry` | The country of the eIDAS node that authenticated the user. |
| `birth_family_name`, `birth_given_name` | BirthName. The last word of the name is the family name, and the words before it are the given names. |
| `place_of_birth` | PlaceOfBirth, or CountryOfBirth and TownOfBirth. |
| `nationalities` | Nationality. |
| `address` | CurrentAddress. CountryOfResidence gives `country` when the address has no country. |
| `phone_number` | PhoneNumber. |
| `email` | EmailAddress. |
| `gender` | Gender. |
| `txn` | The ID of the assertion from the foreign country. |
| `https://id.oidc.se/claim/authnEvidence` | The assertion from the foreign country. |
| `https://id.oidc.se/claim/authnProvider` | The eIDAS node of the foreign country. |

The identity record of Identity Matching is only looked up, and the user only asked for consent, when the Relying
Party asks for the claims of `https://id.swedenconnect.se/scope/eidasSwedishIdentity`.

<a name="authentication-contexts"></a>
## 3. Authentication Contexts

The OpenID Provider delivers the eIDAS authentication contexts of the Sweden Connect registry, see
`acr_values_supported` in the discovery document. Values in `acr_values` that are not supported are ignored. An
essential `acr` in the `claims` parameter that is not supported gives the error `unmet_authentication_requirements`.

:raised_hand: The eIDAS test context, `http://eidas.europa.eu/LoA/test`, used for "ping" requests, is only available
for SAML Service Providers. It cannot be requested over OpenID Connect.

<a name="requesting-a-country"></a>
## 4. Requesting a Country

A Relying Party that knows the user's country can skip the country selection by giving it in the
`https://id.oidc.se/param/authnProvider` parameter, as
`http://id.swedenconnect.se/eidas/1.0/proxy-service/<country code>`, for example
`http://id.swedenconnect.se/eidas/1.0/proxy-service/de`. A country may also be requested as a value of the
`https://id.swedenconnect.se/claim/eidasCountry` claim in the `claims` parameter.

The authentication fails with `access_denied` if the requested country is not available.

<a name="user-messages"></a>
## 5. User Messages

A message in the `https://id.oidc.se/param/userMessage` parameter is displayed before the user is sent to the foreign
country. Plain text and Markdown messages are displayed. A message for the language of the page is used, then a
message without a language, and last the first message. No message is displayed for `prompt=none`. A request that
holds both a user message and a signature request gets `invalid_request`.

<a name="signature-approval"></a>
## 6. Signature Approval

A signature service requests the `https://id.oidc.se/scope/signApproval` scope and gives the sign message in the
`https://id.oidc.se/param/signRequest` parameter, see
[Signature Extension for OpenID Connect](https://www.oidc.se/specifications/oidc-signature-extension.html). After the
foreign authentication, the user is shown the sign message and asked to approve it. Plain text and Markdown sign
messages are displayed. The connector cannot sign, so the `https://id.oidc.se/scope/sign` scope is not offered.

When the `claims` parameter holds essential values for the user's identity, the sign message is only shown if the
authenticated user matches them.

<a name="single-sign-on"></a>
## 7. Single Sign-On

An authentication is reused for 60 minutes, only for the Relying Party that it was made for, and only when the same
claims are requested. It is never reused for a signature approval, and never across SAML and OpenID Connect. A request
with `prompt=none` that cannot be answered with single sign-on gets `login_required`, or `interaction_required` when
other claims are requested.

---

Copyright &copy; 2017-2026, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
