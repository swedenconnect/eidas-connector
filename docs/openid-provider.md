![Logo](images/sweden-connect.png)

# The OpenID Provider of the Swedish eIDAS Connector

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

The Swedish eIDAS Connector is also an OpenID Provider in the Sweden Connect federation. A Relying Party gets the same eIDAS authentication as a SAML Service Provider: the user selects a country and is authenticated at the eIDAS node of that country. This page describes what a Relying Party can ask for.

The OpenID Provider follows the [OpenID Connect Profile for Sweden Connect](https://docs.swedenconnect.se/technical-framework/latest/OpenID_Connect_Profile_for_Sweden_Connect.html) and the [OpenID Connect Claims and Scopes Specification for Sweden Connect](https://docs.swedenconnect.se/technical-framework/latest/OpenID_Connect_Claims_and_Scopes_Specification.html) specifications.

## Table of Contents

1. [**Requirements on the Relying Party**](#requirements-on-the-relying-party)

2. [**The OpenID Provider in the Federation**](#the-openid-provider-in-the-federation)

3. [**Scopes and Claims**](#scopes-and-claims)

4. [**Authentication Contexts**](#authentication-contexts)

5. [**Requesting a Specific Country**](#requesting-a-specific-country)

6. [**User Messages**](#user-messages)

7. [**Signature Approval**](#signature-approval)

8. [**Single Sign-On**](#single-sign-on)

---

<a name="requirements-on-the-relying-party"></a>
## 1. Requirements on the Relying Party

- **Membership of the federation.** The Relying Party must be registered in the Sweden Connect federation. How an organization joins Sweden Connect is described at [www.swedenconnect.se](https://www.swedenconnect.se/anslut), and how a Relying Party is registered in [Sweden Connect - OpenID Federation Rules](https://docs.swedenconnect.se/federation/oidf-rules.html), Section 7, "Joining the Federation".

- **Scopes.** To get the identity of a user authenticated through eIDAS, request the scopes `https://id.swedenconnect.se/scope/eidasNaturalPersonIdentity` and `https://id.oidc.se/scope/naturalPersonInfo`, see [OpenID Connect Claims and Scopes Specification for Sweden Connect](https://docs.swedenconnect.se/technical-framework/latest/OpenID_Connect_Claims_and_Scopes_Specification.html), Section 3.1.3. A Swedish personal identity number is never delivered for an eIDAS user. A Relying Party that needs the Swedish identity that is bound to the user requests `https://id.swedenconnect.se/scope/eidasSwedishIdentity`, see Section 3.1.2 of the same specification. However, the mapping between eIDAS users and Swedish identities is currently not available.

- **Public bodies.** The foreign country is told whether the request comes from a public sector or a private sector service. A Relying Party that is a public body must hold the trust mark
`https://id.swedenconnect.se/tm-prop/public-body`, see [Sweden Connect - OpenID Federation Rules](https://docs.swedenconnect.se/federation/oidf-rules.html), Section 5.2.2. A Relying Party without it is presented as private sector. If the trust mark is not in the entity configuration of the Relying Party, the OpenID Provider asks the trust mark issuer for it.

- **Name and logotype.** The name and logotype of the Relying Party are shown to the user. Publish `client_name` in Swedish and English, and a `logo_uri`, as [Sweden Connect - OpenID Connect Metadata Requirements](https://docs.swedenconnect.se/federation/oidc-metadata-requirements.html), Sections 3 and 4, requires.

<a name="the-openid-provider-in-the-federation"></a>
## 2. The OpenID Provider in the Federation

In production, the entity identifier and issuer of the OpenID Provider is `https://connector.eidas.swedenconnect.se/idp`. Its Entity Configuration is published at `https://connector.eidas.swedenconnect.se/idp/.well-known/openid-federation`.

A Relying Party gets the metadata of the OpenID Provider, among them its endpoints and keys, by resolving the OpenID Provider through the Sweden Connect federation. The resolved metadata is the metadata of the Entity Configuration with the metadata policies of the federation applied. It is recommended to use the resolve endpoint of the Trust Anchor, see [Sweden Connect - OpenID Federation Rules](https://docs.swedenconnect.se/federation/oidf-rules.html), Section 4.1, and [Sweden Connect - OpenID Federation Structure](https://docs.swedenconnect.se/federation/oidf-structure.html) for the endpoints of each environment.

Only the authorization code flow is supported, and the client authenticates with `private_key_jwt` at the token endpoint.

<a name="scopes-and-claims"></a>
## 3. Scopes and Claims

| Scope | Claims |
| :--- | :--- |
| `https://id.swedenconnect.se/scope/eidasNaturalPersonIdentity` | `https://id.swedenconnect.se/claim/prid`, `https://id.swedenconnect.se/claim/pridPersistence` and `http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier`. |
| `https://id.oidc.se/scope/naturalPersonInfo` | `family_name`, `given_name`, `name` and `birthdate`. `name` is the given name followed by the family name. `middle_name` is not delivered. |
| `profile` | `family_name`, `given_name`, `name`, `birthdate` and `gender`. The other claims of the scope are not delivered. |
| `https://id.oidc.se/scope/signApproval` | See [Signature Approval](#signature-approval). |
| `https://id.swedenconnect.se/scope/eidasSwedishIdentity` | `https://id.swedenconnect.se/claim/mappedPersonalIdentityNumber`, `https://id.swedenconnect.se/claim/mappedCoordinationNumber` and `https://id.swedenconnect.se/claim/identityBinding`, delivered from the UserInfo endpoint. Only offered when Identity Matching is active. |

The following claims may also be requested with the `claims` parameter. The foreign country is only asked for the attributes that are requested, apart from the eIDAS minimum data set, which is always asked for. A country does not have to deliver attributes outside the minimum data set.

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

The identity record of Identity Matching is only looked up, and the user only asked for consent, when the Relying Party asks for the claims of `https://id.swedenconnect.se/scope/eidasSwedishIdentity`.

<a name="authentication-contexts"></a>
## 4. Authentication Contexts

The OpenID Provider supports the eIDAS authentication contexts of the Sweden Connect registry. They are also given in `acr_values_supported` of the metadata of the OpenID Provider.

| `acr` | Level of assurance |
| :--- | :--- |
| `http://id.elegnamnden.se/loa/1.0/eidas-low` | eIDAS low |
| `http://id.elegnamnden.se/loa/1.0/eidas-nf-low` | eIDAS low, notified eID scheme |
| `http://id.elegnamnden.se/loa/1.0/eidas-sub` | eIDAS substantial |
| `http://id.elegnamnden.se/loa/1.0/eidas-nf-sub` | eIDAS substantial, notified eID scheme |
| `http://id.elegnamnden.se/loa/1.0/eidas-high` | eIDAS high |
| `http://id.elegnamnden.se/loa/1.0/eidas-nf-high` | eIDAS high, notified eID scheme |

When `acr` values are requested as essential in the `claims` parameter, the ID token holds one of them, or the authentication fails. If none of them is supported, the error is `unmet_authentication_requirements`. Values in `acr_values` that are not supported are ignored, and the ID token holds one of the supported values that remain. When no supported value is requested, the ID token holds the level that the user was authenticated at.

When the foreign country authenticates the user at a higher level than requested, the `acr` is lowered to a requested value. For example, a user authenticated at eIDAS high gives `http://id.elegnamnden.se/loa/1.0/eidas-nf-sub` when only that value is requested.

<a name="requesting-a-specific-country"></a>
## 5. Requesting a Specific Country

A Relying Party that knows the user's country can skip the country selection by giving it in the `https://id.oidc.se/param/authnProvider` parameter, as `http://id.swedenconnect.se/eidas/1.0/proxy-service/<country code>`, for example `http://id.swedenconnect.se/eidas/1.0/proxy-service/de`. A country may also be requested as a value of the `https://id.swedenconnect.se/claim/eidasCountry` claim in the `claims` parameter.

The authentication fails with `access_denied` if the requested country is not available.

<a name="user-messages"></a>
## 6. User Messages

A message in the `https://id.oidc.se/param/userMessage` parameter is displayed before the user is sent to the foreign country. Plain text and Markdown messages are displayed. No message is displayed for `prompt=none`.

<a name="signature-approval"></a>
## 7. Signature Approval

The OpenID Provider supports signature approval, see [Signature Extension for OpenID Connect](https://www.oidc.se/specifications/oidc-signature-extension.html). The feature is used by Relying Parties that are signature services, and that authenticate the user as part of a signature operation.

A signature service requests the `https://id.oidc.se/scope/signApproval` scope and gives the sign message in the `https://id.oidc.se/param/signRequest` parameter. The `prompt` parameter of the request must hold both `login` and `consent`. A request that does not, or that gives a `signRequest` without the `signApproval` scope, gets the error `invalid_request`. After the foreign authentication, the user is shown the sign message and asked to approve it. Plain text and Markdown sign messages are displayed. The OpenID Provider cannot sign, so the `https://id.oidc.se/scope/sign` scope is not offered.

When the `claims` parameter holds essential values for the user's identity, the sign message is only shown if the authenticated user matches them.

<a name="single-sign-on"></a>
## 8. Single Sign-On

An authentication is reused for 60 minutes, only for the Relying Party that it was made for, and only when the same claims are requested. It is never reused for a signature approval, and never across SAML and OpenID Connect. A request with `prompt=none` that cannot be answered with single sign-on gets `login_required`, or `interaction_required` when other claims are requested.

---

Copyright &copy; 2017-2026, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
