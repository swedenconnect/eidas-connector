/*
 * Copyright 2017-2026 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.swedenconnect.eidas.connector;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import se.oidc.nimbus.claims.ParameterConstants;
import se.oidc.nimbus.signrequest.SignRequest;
import se.oidc.nimbus.usermessage.UserMessage;
import se.swedenconnect.eidas.connector.audit.ConnectorAuditEvents;
import se.swedenconnect.eidas.connector.authn.ui.EidasUiModel;
import se.swedenconnect.eidas.connector.authn.ui.SignUiModel;
import se.swedenconnect.opensaml.eidas.ext.SPType;
import se.swedenconnect.opensaml.eidas.ext.SPTypeEnumeration;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttribute;
import se.swedenconnect.opensaml.eidas.ext.RequestedAttributes;

import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Runs an OpenID Connect authentication through the connector: a Relying Party sends an authentication request, the
 * user selects a country, the connector sends an eIDAS authentication request, the foreign country answers, and the
 * Relying Party gets its tokens and the UserInfo.
 *
 * @author Martin Lindström
 */
class OidcAuthenticationFlowTest extends AbstractAuthenticationFlowTest {

  private static final String EIDAS_NATURAL_PERSON_IDENTITY =
      "https://id.swedenconnect.se/scope/eidasNaturalPersonIdentity";

  private static final String SIGN_APPROVAL = "https://id.oidc.se/scope/signApproval";

  private static final String EIDAS_COUNTRY_CLAIM = "https://id.swedenconnect.se/claim/eidasCountry";

  private static final String PERSON_IDENTIFIER_CLAIM =
      "http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier";

  private static final String EIDAS_TEST = "http://eidas.europa.eu/LoA/test";

  @Test
  void testDiscovery() throws Exception {
    final MvcResult result = this.mockMvc.perform(
        get(REQUEST_BASE_URL + "/.well-known/openid-configuration").contextPath("/idp")).andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    final Map<String, Object> discovery = JSONObjectUtils.parse(result.getResponse().getContentAsString());

    assertThat(discovery).containsEntry("issuer", BASE_URL);
    assertThat(JSONObjectUtils.getStringList(discovery, "scopes_supported")).containsExactlyInAnyOrder(
        "openid", EIDAS_NATURAL_PERSON_IDENTITY, "https://id.oidc.se/scope/naturalPersonInfo", "profile",
        SIGN_APPROVAL);
    assertThat(JSONObjectUtils.getStringList(discovery, "claims_supported")).contains(
        "https://id.swedenconnect.se/claim/prid", "https://id.swedenconnect.se/claim/pridPersistence",
        PERSON_IDENTIFIER_CLAIM, EIDAS_COUNTRY_CLAIM, "family_name", "given_name", "name", "birthdate", "gender",
        "birth_family_name", "birth_given_name", "place_of_birth", "nationalities", "address", "txn");
    assertThat(JSONObjectUtils.getStringList(discovery, "claims_supported"))
        .doesNotContain("https://id.swedenconnect.se/claim/mappedPersonalIdentityNumber");
    assertThat(JSONObjectUtils.getStringList(discovery, "ui_locales_supported")).containsExactly("sv", "en");
    assertThat(discovery).containsEntry(ParameterConstants.USER_MESSAGE_SUPPORTED_PARAM_NAME, true);
  }

  @Test
  void testAuthentication() throws Exception {
    final Browser session = new Browser();
    final Map<String, String> params = authzParams(RP_CLIENT_ID, RP_REDIRECT_URI,
        "openid " + EIDAS_NATURAL_PERSON_IDENTITY + " https://id.oidc.se/scope/naturalPersonInfo");
    params.put("claims", """
        {"userinfo": {"%s": null, "address": null}}""".formatted(EIDAS_COUNTRY_CLAIM));
    params.put("ui_locales", "en");

    final String authnId = authnId(this.authorize(session, params));

    // The name of the Relying Party is shown ...
    //
    final MvcResult countrySelect = this.mockMvc.perform(get(REQUEST_BASE_URL + "/extauth").contextPath("/idp")
        .param("authnId", authnId).with(session)).andDo(session).andReturn();
    assertThat(view(countrySelect).getViewName()).isEqualTo("country-select");
    assertThat(((EidasUiModel) view(countrySelect).getModel().get("ui")).getSpInfo().getDisplayName())
        .isIn("The test service", "Testtjänsten");

    // The eIDAS request is for a public sector SP, and asks for the attributes of the requested claims ...
    //
    final EidasRequest eidasRequest = this.selectCountryForOidc(session, authnId);
    assertThat(spType(eidasRequest.request())).isEqualTo(SPTypeEnumeration.PUBLIC);
    assertThat(requestedAttributes(eidasRequest.request())).contains(
        "http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier",
        "http://eidas.europa.eu/attributes/naturalperson/CurrentAddress",
        "http://eidas.europa.eu/attributes/naturalperson/CountryOfResidence");

    final Map<String, String> response = this.completeForeignAuthentication(session, eidasRequest);
    assertThat(response).containsEntry("state", "state-1").containsKey("code");

    // The tokens ...
    //
    final Map<String, Object> tokens = this.token(RP_CLIENT_ID, RP_SECRET, RP_REDIRECT_URI, response.get("code"));
    final JWTClaimsSet idToken = SignedJWT.parse((String) tokens.get("id_token")).getJWTClaimsSet();
    assertThat(idToken.getIssuer()).isEqualTo(BASE_URL);
    assertThat(idToken.getAudience()).containsExactly(RP_CLIENT_ID);
    assertThat(idToken.getStringClaim("acr")).isEqualTo("http://id.elegnamnden.se/loa/1.0/eidas-nf-sub");
    assertThat(idToken.getStringClaim("nonce")).isEqualTo("nonce-1");
    assertThat(idToken.getStringClaim("https://id.swedenconnect.se/claim/prid"))
        .isEqualTo(this.pridService.generatePrid(PERSON_IDENTIFIER, COUNTRY).prid());

    // ... and the UserInfo.
    //
    final Map<String, Object> userInfo = this.userInfo((String) tokens.get("access_token"));
    assertThat(userInfo).containsEntry("sub", idToken.getSubject());
    assertThat(userInfo).containsEntry(PERSON_IDENTIFIER_CLAIM, PERSON_IDENTIFIER);
    assertThat(userInfo).containsEntry(EIDAS_COUNTRY_CLAIM, COUNTRY);
    assertThat(userInfo).containsEntry("name", "Kalle Kula");

    // The audit events of the connector belong to the same flow as those of the authentication server, also the
    // events of the token and UserInfo calls ...
    //
    final List<se.swedenconnect.spring.audit.AuditEvent> events = this.auditEventRepository.find(null, null, null)
        .stream()
        .filter(se.swedenconnect.spring.audit.AuditEvent.class::isInstance)
        .map(se.swedenconnect.spring.audit.AuditEvent.class::cast)
        .filter(e -> e.getCorrelationId() != null)
        .toList();
    final String correlationId = events.stream()
        .filter(e -> "authn_userinfo_delivered".equals(e.getType()))
        .reduce((first, second) -> second)
        .map(e -> e.getCorrelationId().getValue())
        .orElseThrow();
    assertThat(events.stream()
        .filter(e -> correlationId.equals(e.getCorrelationId().getValue()))
        .map(se.swedenconnect.spring.audit.AuditEvent::getType)
        .toList()).contains(
        "authn_request_received", "authn_request_accepted",
        ConnectorAuditEvents.CONNECTOR_BEFORE_SAML_REQUEST, ConnectorAuditEvents.CONNECTOR_SUCCESS_RESPONSE,
        "authn_user_authenticated", "authn_authorization_response", "authn_success_response",
        "authn_userinfo_delivered");
  }

  @Test
  void testPrivateSector() throws Exception {
    final Browser session = new Browser();
    final String authnId = authnId(this.authorize(session,
        authzParams(PRIVATE_RP_CLIENT_ID, PRIVATE_RP_REDIRECT_URI, "openid " + EIDAS_NATURAL_PERSON_IDENTITY)));
    assertThat(spType(this.selectCountry(session, authnId))).isEqualTo(SPTypeEnumeration.PRIVATE);
  }

  @Test
  void testCancel() throws Exception {
    final Browser session = new Browser();
    final String authnId = authnId(this.authorize(session, defaultParams()));

    final MvcResult cancelled = this.mockMvc.perform(post(REQUEST_BASE_URL + "/extauth/proxyauth")
        .contextPath("/idp").param("authnId", authnId).param("selectedCountry", "cancel").with(session))
        .andDo(session).andReturn();
    final Map<String, String> response = this.resumeOidc(session, cancelled.getResponse().getRedirectedUrl());
    assertThat(response).containsEntry("error", "access_denied").containsEntry("state", "state-1");
  }

  @Test
  void testRequestedCountry() throws Exception {
    // A country given by authnProvider ...
    //
    final Browser session = new Browser();
    final Map<String, String> params = defaultParams();
    params.put(ParameterConstants.REQUESTED_PROVIDER_PARAM_NAME,
        "http://id.swedenconnect.se/eidas/1.0/proxy-service/" + OTHER_COUNTRY.toLowerCase());
    final MvcResult result = this.mockMvc.perform(get(REQUEST_BASE_URL + "/extauth").contextPath("/idp")
        .param("authnId", authnId(this.authorize(session, params))).with(session)).andDo(session).andReturn();
    assertThat(view(result).getViewName()).isEqualTo("post-request");
    assertThat(view(result).getModel()).containsEntry("action", OTHER_COUNTRY_SSO);

    // ... and by the eidasCountry claim.
    //
    final Browser session2 = new Browser();
    final Map<String, String> params2 = defaultParams();
    params2.put("claims", """
        {"id_token": {"%s": {"essential": true, "value": "%s"}}}""".formatted(EIDAS_COUNTRY_CLAIM, OTHER_COUNTRY));
    final MvcResult result2 = this.mockMvc.perform(get(REQUEST_BASE_URL + "/extauth").contextPath("/idp")
        .param("authnId", authnId(this.authorize(session2, params2))).with(session2)).andDo(session2).andReturn();
    assertThat(view(result2).getViewName()).isEqualTo("post-request");
    assertThat(view(result2).getModel()).containsEntry("action", OTHER_COUNTRY_SSO);
  }

  @Test
  void testEidasTestUriInAcrValues() throws Exception {
    // The eIDAS test URI in acr_values is not supported, so it is removed and the request is an ordinary
    // authentication, also for a client on the ping whitelist ...
    //
    final Browser session = new Browser();
    final Map<String, String> params = defaultParams();
    params.put("acr_values", EIDAS_TEST);
    final MvcResult result = this.mockMvc.perform(get(REQUEST_BASE_URL + "/extauth").contextPath("/idp")
        .param("authnId", authnId(this.authorize(session, params))).with(session)).andDo(session).andReturn();
    assertThat(view(result).getModel()).containsEntry("pingFlag", false);
  }

  @Test
  void testEssentialAcrIsDowngraded() throws Exception {
    // eidas-nf-sub is requested as essential, and the foreign country authenticates the user at eIDAS high. The ID
    // token holds the requested value ...
    //
    final Browser session = new Browser();
    final Map<String, String> params = defaultParams();
    params.put("claims", """
        {"id_token": {"acr": {"essential": true, "value": "http://id.elegnamnden.se/loa/1.0/eidas-nf-sub"}}}""");
    final String authnId = authnId(this.authorize(session, params));
    final EidasRequest eidasRequest = this.selectCountryForOidc(session, authnId);

    final MvcResult foreignResponse = this.mockMvc.perform(post(REQUEST_BASE_URL + "/extauth/saml2/post")
            .contextPath("/idp").with(session)
            .param("SAMLResponse", encode(this.eidasResponse(eidasRequest.request(), successStatus(), true,
                "http://eidas.europa.eu/LoA/high")))
            .param("RelayState", eidasRequest.relayState()))
        .andDo(session).andReturn();
    final Map<String, String> response =
        this.resumeOidc(session, foreignResponse.getResponse().getRedirectedUrl());

    final Map<String, Object> tokens = this.token(RP_CLIENT_ID, RP_SECRET, RP_REDIRECT_URI, response.get("code"));
    assertThat(SignedJWT.parse((String) tokens.get("id_token")).getJWTClaimsSet().getStringClaim("acr"))
        .isEqualTo("http://id.elegnamnden.se/loa/1.0/eidas-nf-sub");
  }

  @Test
  void testUnsupportedAcrValues() throws Exception {
    final Browser session = new Browser();
    final Map<String, String> params = defaultParams();
    params.put("acr_values", "http://id.elegnamnden.se/loa/1.0/loa3");
    final MvcResult result = this.authorize(session, params);
    assertThat(result.getResponse().getRedirectedUrl()).startsWith("/idp/extauth?authnId=");
  }

  @Test
  void testUserMessageWithoutLanguage() throws Exception {
    final Browser session = new Browser();
    final Map<String, String> params = defaultParams();
    params.put(ParameterConstants.USER_MESSAGE_PARAM_NAME,
        new UserMessage(List.of(new UserMessage.Message("Hello from the RP")), UserMessage.TEXT_MIME_TYPE)
            .toJSONObject().toJSONString());
    final MvcResult result = this.mockMvc.perform(get(REQUEST_BASE_URL + "/extauth").contextPath("/idp")
        .param("authnId", authnId(this.authorize(session, params))).with(session)).andDo(session).andReturn();
    assertThat(((EidasUiModel) view(result).getModel().get("ui")).getUserMessage()).contains("Hello from the RP");
  }

  @Test
  void testSignApproval() throws Exception {
    final Browser session = new Browser();
    final Map<String, String> params = authzParams(RP_CLIENT_ID, RP_REDIRECT_URI,
        "openid " + EIDAS_NATURAL_PERSON_IDENTITY + " " + SIGN_APPROVAL);
    params.put("prompt", "login consent");
    params.put(ParameterConstants.SIGN_REQUEST_PARAM_NAME,
        signRequest(new UserMessage(List.of(new UserMessage.Message("I *approve*", "en")),
            UserMessage.MARKDOWN_MIME_TYPE)));

    final String authnId = authnId(this.authorize(session, params));
    final EidasRequest eidasRequest = this.selectCountryForOidc(session, authnId);

    // After the authentication at the foreign country, the user is asked to consent to the signature ...
    //
    final MvcResult consent = this.foreignResponse(session, eidasRequest);
    assertThat(view(consent).getViewName()).isEqualTo("sign-consent");
    final SignUiModel model = (SignUiModel) view(consent).getModel().get("signMessageConsent");
    assertThat(model.getTextMessage()).contains("<em>approve</em>");

    final MvcResult signed = this.mockMvc.perform(post(REQUEST_BASE_URL + "/extauth/signed").contextPath("/idp")
        .param("authnId", authnId).param("action", "ok").with(session)).andDo(session).andReturn();
    final Map<String, String> response = this.resumeOidc(session, signed.getResponse().getRedirectedUrl());
    assertThat(response).containsKey("code");

    final Map<String, Object> tokens = this.token(RP_CLIENT_ID, RP_SECRET, RP_REDIRECT_URI, response.get("code"));
    assertThat(tokens).containsKey("id_token");
  }

  @Test
  void testUserMessageAndSignRequestIsRefused() throws Exception {
    final Browser session = new Browser();
    final Map<String, String> params = authzParams(RP_CLIENT_ID, RP_REDIRECT_URI,
        "openid " + EIDAS_NATURAL_PERSON_IDENTITY + " " + SIGN_APPROVAL);
    params.put("prompt", "login consent");
    params.put(ParameterConstants.SIGN_REQUEST_PARAM_NAME,
        signRequest(new UserMessage(List.of(new UserMessage.Message("I approve")), UserMessage.TEXT_MIME_TYPE)));
    params.put(ParameterConstants.USER_MESSAGE_PARAM_NAME,
        new UserMessage(List.of(new UserMessage.Message("Hello")), UserMessage.TEXT_MIME_TYPE)
            .toJSONObject().toJSONString());

    final Map<String, String> response = queryParams(this.authorize(session, params).getResponse()
        .getRedirectedUrl());
    assertThat(response).containsEntry("error", "invalid_request");
  }

  @Test
  void testSso() throws Exception {
    final Browser session = new Browser();
    final String authnId = authnId(this.authorize(session, defaultParams()));
    this.completeForeignAuthentication(session, this.selectCountryForOidc(session, authnId));

    // The same client asking for the same scopes gets single sign-on ...
    //
    final Map<String, String> params = defaultParams();
    params.put("prompt", "none");
    assertThat(queryParams(this.authorize(session, params).getResponse().getRedirectedUrl())).containsKey("code");

    // ... another client does not ...
    //
    final Map<String, String> other =
        authzParams(PRIVATE_RP_CLIENT_ID, PRIVATE_RP_REDIRECT_URI, "openid " + EIDAS_NATURAL_PERSON_IDENTITY);
    other.put("prompt", "none");
    assertThat(queryParams(this.authorize(session, other).getResponse().getRedirectedUrl()))
        .containsEntry("error", "login_required");

    // ... and neither does the same client asking for other claims.
    //
    final Map<String, String> otherScopes = authzParams(RP_CLIENT_ID, RP_REDIRECT_URI,
        "openid " + EIDAS_NATURAL_PERSON_IDENTITY + " profile");
    otherScopes.put("prompt", "none");
    assertThat(queryParams(this.authorize(session, otherScopes).getResponse().getRedirectedUrl()))
        .containsEntry("error", "interaction_required");
  }

  @Test
  void testNoSsoAfterSamlAuthentication() throws Exception {
    final Browser session = new Browser();
    this.authenticate(session);

    final Map<String, String> params = defaultParams();
    params.put("prompt", "none");
    assertThat(queryParams(this.authorize(session, params).getResponse().getRedirectedUrl()))
        .containsEntry("error", "login_required");
  }

  private MvcResult authorize(final Browser session, final Map<String, String> params) throws Exception {
    final MockHttpServletRequestBuilder builder =
        get(REQUEST_BASE_URL + "/oidc/authorize").contextPath("/idp").with(session);
    params.forEach(builder::param);
    return this.mockMvc.perform(builder).andDo(session).andReturn();
  }

  /**
   * Selects the country and gives the eIDAS request, and the RelayState that the connector sent with it.
   */
  private EidasRequest selectCountryForOidc(final Browser session, final String authnId) throws Exception {
    final MvcResult result = this.postCountry(session, authnId, COUNTRY);
    final String relayState = (String) view(result).getModel().get("RelayState");
    assertThat(relayState).isNotBlank();
    final AuthnRequest authnRequest = (AuthnRequest) XMLObjectSupport.unmarshallFromInputStream(
        Objects.requireNonNull(XMLObjectProviderRegistrySupport.getParserPool()),
        new ByteArrayInputStream(Base64.getDecoder().decode((String) view(result).getModel().get("SAMLRequest"))));
    return new EidasRequest(authnRequest, relayState);
  }

  private MvcResult foreignResponse(final Browser session, final EidasRequest eidasRequest) throws Exception {
    return this.mockMvc.perform(post(REQUEST_BASE_URL + "/extauth/saml2/post").contextPath("/idp").with(session)
            .param("SAMLResponse", encode(this.eidasResponse(eidasRequest.request(), successStatus(), true)))
            .param("RelayState", eidasRequest.relayState()))
        .andDo(session).andReturn();
  }

  private Map<String, String> completeForeignAuthentication(final Browser session, final EidasRequest eidasRequest)
      throws Exception {
    return this.resumeOidc(session, this.foreignResponse(session, eidasRequest).getResponse().getRedirectedUrl());
  }

  private Map<String, String> resumeOidc(final Browser session, final String resumePath) throws Exception {
    final MvcResult result = this.mockMvc.perform(get("https://www.example.com:443" + resumePath).contextPath("/idp")
        .with(session)).andDo(session).andReturn();
    final String location = result.getResponse().getRedirectedUrl();
    assertThat(location).startsWith("https://");
    return queryParams(location);
  }

  private Map<String, Object> token(final String clientId, final String secret, final String redirectUri,
      final String code) throws Exception {
    final MvcResult result = this.mockMvc.perform(post(REQUEST_BASE_URL + "/oidc/token").contextPath("/idp")
            .header("Authorization",
                new ClientSecretBasic(new ClientID(clientId), new Secret(secret)).toHTTPAuthorizationHeader())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("grant_type", "authorization_code")
            .param("code", code)
            .param("redirect_uri", redirectUri))
        .andReturn();
    assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
    return JSONObjectUtils.parse(result.getResponse().getContentAsString());
  }

  private Map<String, Object> userInfo(final String accessToken) throws Exception {
    final MvcResult result = this.mockMvc.perform(get(REQUEST_BASE_URL + "/oidc/userinfo").contextPath("/idp")
        .header("Authorization", "Bearer " + accessToken)).andReturn();
    assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
    final String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    return body.startsWith("{")
        ? JSONObjectUtils.parse(body)
        : SignedJWT.parse(body).getJWTClaimsSet().toJSONObject();
  }

  private static Map<String, String> defaultParams() {
    return authzParams(RP_CLIENT_ID, RP_REDIRECT_URI, "openid " + EIDAS_NATURAL_PERSON_IDENTITY);
  }

  private static Map<String, String> authzParams(final String clientId, final String redirectUri,
      final String scope) {
    final Map<String, String> params = new LinkedHashMap<>();
    params.put("response_type", "code");
    params.put("client_id", clientId);
    params.put("redirect_uri", redirectUri);
    params.put("scope", scope);
    params.put("state", "state-1");
    params.put("nonce", "nonce-1");
    return params;
  }

  private static String signRequest(final UserMessage signMessage) throws Exception {
    final JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
        .issuer(RP_CLIENT_ID)
        .audience(BASE_URL)
        .issueTime(Date.from(Instant.now()));
    new SignRequest(signMessage).toJSONObject().forEach(claims::claim);
    final SignedJWT jwt = new SignedJWT(
        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(RP_KEY.getKeyID()).build(), claims.build());
    jwt.sign(new RSASSASigner(RP_KEY));
    return jwt.serialize();
  }

  private static Map<String, String> queryParams(final String url) {
    final MultiValueMap<String, String> params = UriComponentsBuilder.fromUriString(url).build().getQueryParams();
    final Map<String, String> result = new LinkedHashMap<>();
    params.forEach((k, v) -> result.put(k, URLDecoder.decode(v.getFirst(), StandardCharsets.UTF_8)));
    return result;
  }

  private static SPTypeEnumeration spType(final AuthnRequest eidasRequest) {
    return ((SPType) eidasRequest.getExtensions().getUnknownXMLObjects(SPType.DEFAULT_ELEMENT_NAME).getFirst())
        .getType();
  }

  /**
   * An eIDAS request and its RelayState.
   */
  private record EidasRequest(AuthnRequest request, String relayState) {
  }

  private static List<String> requestedAttributes(final AuthnRequest eidasRequest) {
    return ((RequestedAttributes) eidasRequest.getExtensions()
        .getUnknownXMLObjects(RequestedAttributes.DEFAULT_ELEMENT_NAME).getFirst()).getRequestedAttributes().stream()
        .map(RequestedAttribute::getName)
        .toList();
  }

}
