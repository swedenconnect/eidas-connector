/*
 * Copyright 2017-2018 E-legitimationsnämnden
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
package se.elegnamnden.eidas.idp.connector.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.base.Function;
import com.google.common.base.Functions;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.ExternalAuthenticationException;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import se.elegnamnden.eidas.idp.connector.service.AttributeProcessingException;
import se.elegnamnden.eidas.idp.connector.service.AttributeProcessingService;
import se.elegnamnden.eidas.idp.connector.service.EidasAuthnContextService;
import se.elegnamnden.eidas.idp.connector.sp.EidasAuthnRequestGenerator;
import se.elegnamnden.eidas.idp.connector.sp.EidasAuthnRequestGeneratorInput;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessingException;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessingInput;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessingResult;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessor;
import se.elegnamnden.eidas.idp.connector.sp.ResponseStatusErrorException;
import se.elegnamnden.eidas.metadataconfig.MetadataConfig;
import se.elegnamnden.eidas.metadataconfig.data.EndPointConfig;
import se.litsec.eidas.opensaml.ext.SPTypeEnumeration;
import se.litsec.opensaml.saml2.common.request.RequestGenerationException;
import se.litsec.opensaml.saml2.common.request.RequestHttpObject;
import se.litsec.opensaml.saml2.metadata.PeerMetadataResolver;
import se.litsec.opensaml.utils.ObjectUtils;
import se.litsec.shibboleth.idp.authn.ExternalAutenticationErrorCodeException;
import se.litsec.shibboleth.idp.authn.context.ProxyIdpAuthenticationContext;
import se.litsec.shibboleth.idp.authn.context.SignMessageContext;
import se.litsec.shibboleth.idp.authn.context.SignatureActivationDataContext;
import se.litsec.shibboleth.idp.authn.context.strategy.AuthenticationContextLookup;
import se.litsec.shibboleth.idp.authn.context.strategy.ProxyIdpAuthenticationContextLookup;
import se.litsec.shibboleth.idp.authn.controller.AbstractExternalAuthenticationController;
import se.litsec.swedisheid.opensaml.saml2.attribute.AttributeConstants;
import se.litsec.swedisheid.opensaml.saml2.attribute.AttributeSet;
import se.litsec.swedisheid.opensaml.saml2.attribute.AttributeSetConstants;
import se.litsec.swedisheid.opensaml.saml2.metadata.entitycategory.EntityCategoryConstants;
import se.litsec.swedisheid.opensaml.saml2.metadata.entitycategory.EntityCategoryMetadataHelper;

/**
 * The main controller for the Shibboleth external authentication flow implementing proxy functionality against the
 * eIDAS framework.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
@Controller
public class ProxyAuthenticationController extends AbstractExternalAuthenticationController implements InitializingBean {

  /** Symbolic name for the action parameter value of "cancel". */
  public static final String ACTION_CANCEL = "cancel";

  /** Symbolic name for OK. */
  public static final String ACTION_OK = "ok";

  /** The default locale. */
  public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

  /** The attribute set that this IdP is implementing. */
  public static final AttributeSet IMPLEMENTED_ATTRIBUTE_SET = AttributeSetConstants.ATTRIBUTE_SET_EIDAS_NATURAL_PERSON;

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(ProxyAuthenticationController.class);

  /** Strategy that gives us the AuthenticationContext. */
  @SuppressWarnings("rawtypes")
  private static Function<ProfileRequestContext, AuthenticationContext> authenticationContextLookupStrategy = new AuthenticationContextLookup();

  /** Strategy used to locate the ProxyIdpAuthenticationContext. */
  @SuppressWarnings("rawtypes")
  private static Function<ProfileRequestContext, ProxyIdpAuthenticationContext> proxyIdpAuthenticationContextLookupStrategy = Functions
    .compose(new ProxyIdpAuthenticationContextLookup(), authenticationContextLookupStrategy);

  /** The name of the authenticator. */
  private String authenticatorName;

  /** Service for processing AuthnContext class URI:s. */
  private EidasAuthnContextService eidasAuthnContextService;

  /** Generator for SP AuthnRequests. */
  private EidasAuthnRequestGenerator authnRequestGenerator;

  /** Processor for handling of SAML responses received from the foreign IdP. */
  private ResponseProcessor responseProcessor;

  /** Configurator for eIDAS metadata. */
  private MetadataConfig metadataConfig;

  /** Service for handling mappings of attributes. */
  private AttributeProcessingService attributeProcessingService;

  /** Helper bean for handling selected country. */
  private CountrySelectionHandler countrySelectionHandler;

  /** Helper bean for handling view of Sign message consent. */
  private SignMessageUiHandler signMessageUiHandler;

  /** Helper bean for UI language select. */
  private UiLanguageHandler uiLanguageHandler;

  /** Flag telling whether we should include verbose status messages in response messages. Default is {@code false}. */
  private boolean verboseStatusMessage = false;

  /**
   * The first step for the connector authentication is to prompt the user for the eID country.
   */
  @Override
  protected ModelAndView doExternalAuthentication(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      String key,
      ProfileRequestContext<?, ?> profileRequestContext) throws ExternalAuthenticationException, IOException {

    return this.countrySelect(httpRequest, httpResponse, null);
  }

  /**
   * Controller method for starting the authentication flow.
   * 
   * @param httpRequest
   *          the request
   * @param httpResponse
   *          the response
   * @param language
   *          optional parameter holding the selected language
   * @return a model and view object
   * @throws ExternalAuthenticationException
   *           for Shibboleth session errors
   * @throws IOException
   *           for IO errors
   */
  @RequestMapping(value = "/start", method = RequestMethod.POST)
  public ModelAndView countrySelect(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      @RequestParam(name = "language", required = false) String language) throws ExternalAuthenticationException, IOException {

    final ProfileRequestContext<?, ?> context = this.getProfileRequestContext(httpRequest);

    if (language != null) {
      this.uiLanguageHandler.setUiLanguage(httpRequest, httpResponse, language);
    }

    // Selected country from this session.
    //
    String selectedCountry = this.countrySelectionHandler.getSelectedCountry(httpRequest, true);
    log.debug("Selected country from this session: {}", selectedCountry != null ? selectedCountry : "none");

    // The first step is to prompt the user for which country to direct to.
    // If the request is made by a signature service and the selected country is known, we skip
    // the "select country" view.
    //
    if (this.getSignSupportService().isSignatureServicePeer(context) && selectedCountry != null) {
      log.info("Request is from a signature service. Will default to previously selected country: '{}'", selectedCountry);
      return this.processAuthentication(httpRequest, httpResponse, selectedCountry);
    }

    List<String> availableCountries = this.metadataConfig.getProxyServiceCountryList();
    if (availableCountries.isEmpty()) {
      log.error("No available countries");
      this.error(httpRequest, httpResponse, StatusCode.RESPONDER, StatusCode.NO_AVAILABLE_IDP,
        "No countries available for authentication", null);
      return null;
    }

    ModelAndView modelAndView = new ModelAndView("country-select2");
    modelAndView.addObject("countries", this.countrySelectionHandler.getSelectableCountries(availableCountries));
    modelAndView.addObject("spInfo", this.countrySelectionHandler.getSpInfo(this.getPeerMetadata(context)));
    modelAndView.addObject("uiLanguages", this.uiLanguageHandler.getUiLanguages());

    if (selectedCountry == null) {
      selectedCountry = this.countrySelectionHandler.getSelectedCountry(httpRequest, false);
      log.debug("Selected country from previous session: {}", selectedCountry != null ? selectedCountry : "none");
    }
    
    if (selectedCountry != null) {
      modelAndView.addObject("selectedCountry", this.countrySelectionHandler.getSelectedCountry(httpRequest, false));
    }

    return modelAndView;
  }

  /**
   * Controller method that initiates the authentication against the foreign Proxy Service ending with an authentication
   * being sent.
   * 
   * @param httpRequest
   *          the request
   * @param httpResponse
   *          the response
   * @param selectedCountry
   *          the country code for the selected country
   * @return a model and view for POST or redirect of the request
   * @throws ExternalAuthenticationException
   *           for Shibboleth session errors
   * @throws IOException
   *           for IO errors
   */
  @RequestMapping(value = "/proxyauth", method = RequestMethod.POST)
  public ModelAndView processAuthentication(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      @RequestParam(name = "selectedCountry") String selectedCountry) throws ExternalAuthenticationException, IOException {

    if (ACTION_CANCEL.equals(selectedCountry)) {
      log.info("User cancelled country selection - aborting authentication");
      this.cancel(httpRequest, httpResponse);
      return null;
    }

    log.debug("User selected country '{}'", selectedCountry);

    // Get hold of all information needed for the foreign endpoint.
    //
    EndPointConfig endPoint = this.metadataConfig.getProxyServiceConfig(selectedCountry);
    if (endPoint == null) {
      log.error("No endpoint found for country '{}'", selectedCountry);
      this.error(httpRequest, httpResponse, StatusCode.RESPONDER, StatusCode.NO_AVAILABLE_IDP,
        "No services available for selected country", null);
      return null;
    }
    
    this.countrySelectionHandler.saveSelectedCountry(httpResponse, selectedCountry);

    final ProfileRequestContext<?, ?> context = this.getProfileRequestContext(httpRequest);

    // Next, generate an AuthnRequest and redirect or post the user there.
    //
    try {
      EidasAuthnRequestGeneratorInput spInput = this.createAuthnRequestInput(context, endPoint, this.getAuthnRequest(context));

      PeerMetadataResolver metadataResolver = (entityID) -> {
        return (entityID.equals(endPoint.getEntityID())) ? endPoint.getMetadataRecord() : null;
      };

      RequestHttpObject<AuthnRequest> authnRequest = this.authnRequestGenerator.generateRequest(spInput, metadataResolver);
      this.saveProxyIdpState(context, authnRequest.getRequest(), endPoint);

      if (SAMLConstants.POST_METHOD.equals(authnRequest.getMethod())) {
        ModelAndView modelAndView = new ModelAndView("post-request");
        modelAndView.addObject("action", authnRequest.getSendUrl());
        modelAndView.addAllObjects(authnRequest.getRequestParameters());
        return modelAndView;
      }
      else {
        return new ModelAndView("redirect:" + authnRequest.getSendUrl());
      }
    }
    catch (RequestGenerationException e) {
      log.error("Error while creating eIDAS AuthnContext - {}", e.getMessage(), e);
      this.error(httpRequest, httpResponse, StatusCode.REQUESTER, StatusCode.REQUEST_UNSUPPORTED,
        "Can not create valid request to foreign service", e.getMessage());
      return null;
    }
  }

  /**
   * Builds the input needed for the SP to create the AuthnRequest.
   * 
   * @param context
   *          the profile request
   * @param endPoint
   *          the recipient endpoint info
   * @param authnRequest
   *          the {@code AuthnRequest}
   * @return an {@code AuthnRequestInput} object
   * @throws RequestGenerationException
   *           for errors creating the request
   */
  private EidasAuthnRequestGeneratorInput createAuthnRequestInput(ProfileRequestContext<?, ?> context,
      EndPointConfig endPoint, AuthnRequest authnRequest) throws RequestGenerationException {

    EidasAuthnRequestGeneratorInput spInput = new EidasAuthnRequestGeneratorInput();

    spInput.setPeerEntityID(endPoint.getEntityID());
    spInput.setCountry(endPoint.getCountry());

    // Match assurance levels and calculate which LoA URI to use in the request.
    //
    try {
      spInput.setRequestedAuthnContext(this.eidasAuthnContextService.getSendRequestedAuthnContext(
        context, endPoint.getAssuranceCertifications()));
    }
    catch (ExternalAutenticationErrorCodeException e) {
      throw new RequestGenerationException(e.getActualMessage() != null ? e.getActualMessage() : e.getMessage(), e);
    }

    // Relay state
    spInput.setRelayState(this.getRelayState(context));

    // SP type
    //
    EntityDescriptor spMetadata = this.getPeerMetadata(context);
    if (spMetadata != null) {
      List<String> entityCategories = EntityCategoryMetadataHelper.getEntityCategories(spMetadata);
      if (entityCategories.contains(EntityCategoryConstants.SERVICE_TYPE_CATEGORY_PUBLIC_SECTOR_SP.getUri())) {
        spInput.setSpType(SPTypeEnumeration.PUBLIC);
      }
      else if (entityCategories.contains(EntityCategoryConstants.SERVICE_TYPE_CATEGORY_PRIVATE_SECTOR_SP.getUri())) {
        spInput.setSpType(SPTypeEnumeration.PRIVATE);
      }
      else {
        log.warn("Entity '{}' does not specify entity category for public or private SP", spMetadata.getEntityID());
      }
    }

    // Requested attributes
    // First get the default ones to request from the implemented attribute set.
    // Then check if the peer specifies any additional requested attributes in its metadata. If so, transform them
    // to eIDAS.
    //
    List<se.litsec.eidas.opensaml.ext.RequestedAttribute> requestedAttributes = new ArrayList<>();
    requestedAttributes.addAll(
      this.attributeProcessingService.getEidasRequestedAttributesFromAttributeSet(IMPLEMENTED_ATTRIBUTE_SET));
    requestedAttributes.addAll(
      this.attributeProcessingService.getEidasRequestedAttributesFromMetadata(this.getPeerMetadata(context), authnRequest,
        requestedAttributes));

    spInput.setRequestedAttributeList(requestedAttributes);

    return spInput;
  }

  /**
   * Saves the authentication state for the Proxy IdP SP part.
   * 
   * @param context
   *          the profile context
   * @param authnRequest
   *          the AuthnRequest object that is sent
   * @param endpoint
   *          the IdP/endpoint information
   * @throws ExternalAuthenticationException
   *           for session errors
   */
  private void saveProxyIdpState(ProfileRequestContext<?, ?> context, AuthnRequest authnRequest, EndPointConfig endpoint)
      throws ExternalAuthenticationException {

    ProxyIdpAuthenticationContext proxyContext = new ProxyIdpAuthenticationContext(authnRequest);

    // In order not to store too much data in the session, we only store the country, and look up the rest
    // when we process the response.
    proxyContext.addAdditionalData("country", endpoint.getCountry());

    AuthenticationContext authenticationContext = authenticationContextLookupStrategy.apply(context);
    if (authenticationContext == null) {
      throw new ExternalAuthenticationException("No AuthenticationContext available");
    }
    authenticationContext.addSubcontext(proxyContext, true);
  }

  /**
   * Endpoint for receiving SAML responses from the foreign IdP. The response will be validated and a response to be
   * sent back to the Swedish SP that issued the original request will be compiled.
   * 
   * @param httpRequest
   *          the HTTP request
   * @param httpResponse
   *          the HTTP response
   * @param samlResponse
   *          the SAML response
   * @param relayState
   *          the SAML RelayState variable
   * @return a redirect URL
   * @throws ExternalAuthenticationException
   *           for Shibboleth session errors
   * @throws IOException
   *           for IO errors
   */
  @RequestMapping(value = "/saml2/post", method = RequestMethod.POST)
  public ModelAndView samlResponse(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
      @RequestParam("SAMLResponse") String samlResponse,
      @RequestParam(value = "RelayState", required = false) String relayState) throws ExternalAuthenticationException, IOException {

    log.debug("Received SAML response [client-ip-address='{}']", httpRequest.getRemoteAddr());

    // Pick up the context from the session. If the request is stale, we'll get an exception.
    //
    final ProfileRequestContext<?, ?> context = this.getProfileRequestContext(httpRequest);

    // Process the SAML response
    //
    ResponseProcessingInput input = this.createResponseProcessingInput(context, httpRequest);
    EndPointConfig idpEndpoint = this.metadataConfig.getProxyServiceConfig(input.getCountry());

    try {
      PeerMetadataResolver idpMetadataResolver = (entityID) -> {
        return (entityID.equals(idpEndpoint.getEntityID())) ? idpEndpoint.getMetadataRecord() : null;
      };

      ResponseProcessingResult result = this.responseProcessor.processSamlResponse(samlResponse, relayState, input, idpMetadataResolver);
      log.debug("Successfully processed SAML response");

      // Perform the attribute relase ...
      //
      List<Attribute> attributes = this.attributeProcessingService.performAttributeRelease(result);

      ProxyIdpAuthenticationContext proxyContext = proxyIdpAuthenticationContextLookupStrategy.apply(context);
      if (proxyContext == null) {
        throw new ExternalAuthenticationException("No ProxyIdpAuthenticationContext available");
      }
      proxyContext.addAdditionalData("result", result);
      proxyContext.addAdditionalData("attributes", attributes);

      if (result.getAssertion() != null) {
        proxyContext.setAssertion(result.getAssertion());
      }

      return this.completeAuthentication(httpRequest, httpResponse, null, null);
    }
    catch (ResponseProcessingException e) {
      log.warn("Error while processing eIDAS response - {}", e.getMessage(), e);
      this.error(httpRequest, httpResponse, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED,
        "Validation failure of response from foreign service", e.getMessage());
      return null;
    }
    catch (ResponseStatusErrorException e) {
      log.info("Received non successful status: {}", e.getMessage());

      // Let's update the status message - It may be in any language, so we add
      // a text telling that the failure was received from the foreign service.
      //
      Status status = e.getStatus();
      if (status.getStatusMessage() != null) {
        String msg = String.format("Failure received from foreign service: %s", status.getStatusMessage().getMessage());
        status.getStatusMessage().setMessage(msg);
      }
      else {
        StatusMessage sm = ObjectUtils.createSamlObject(StatusMessage.class);
        sm.setMessage("Failure received from foreign service");
        status.setStatusMessage(sm);
      }

      this.error(httpRequest, httpResponse, status);
      return null;
    }
    catch (AttributeProcessingException e) {
      log.warn("Error during attribute release process - {}", e.getMessage(), e);
      this.error(httpRequest, httpResponse, StatusCode.REQUESTER, StatusCode.REQUEST_DENIED,
        "Attribute release error", e.getMessage());
      return null;
    }
  }

  /**
   * Method that is invoked in order to complate an authentication process.
   * 
   * @param httpRequest
   *          the HTTP request
   * @param httpResponse
   *          the HTTP response
   * @param action
   *          the action - {@link #ACTION_OK} or {@value #ACTION_CANCEL} (if invoked from sign consent view),
   *          {@code null} otherwise
   * @param language
   *          language tag (for UI)
   * @return a {@code ModelAndView} or {@code null} if the authentication is done
   * @throws ExternalAuthenticationException
   *           for Shibboleth session errors
   * @throws IOException
   *           for IO errors
   */
  @RequestMapping(value = "/proxyauth/complete", method = RequestMethod.POST)
  public ModelAndView completeAuthentication(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      @RequestParam(value = "action", required = false) String action,
      @RequestParam(value = "language", required = false) String language) throws ExternalAuthenticationException, IOException {

    // Pick up the context from the session. If the request is stale, we'll get an exception.
    //
    final ProfileRequestContext<?, ?> context = this.getProfileRequestContext(httpRequest);

    if (language != null) {
      this.uiLanguageHandler.setUiLanguage(httpRequest, httpResponse, language);
    }

    ProxyIdpAuthenticationContext proxyContext = proxyIdpAuthenticationContextLookupStrategy.apply(context);

    ResponseProcessingResult result = (ResponseProcessingResult) proxyContext.getAdditionalData("result");
    @SuppressWarnings("unchecked")
    List<Attribute> attributes = (List<Attribute>) proxyContext.getAdditionalData("attributes");
    if (result == null || attributes == null) {
      throw new ExternalAuthenticationException("No result in ProxyIdpAuthenticationContext");
    }

    // If 'action' is null we were called internally.
    if (action == null) {

      if (this.getSignSupportService().isSignatureServicePeer(context)) {
        ModelAndView modelAndView = new ModelAndView("sign-consent2");
        modelAndView.addObject("uiLanguages", this.uiLanguageHandler.getUiLanguages());

        SignMessageContext signMessageContext = this.getSignSupportService().getSignMessageContext(context);

        if (signMessageContext != null && signMessageContext.isDoDisplayMessage()) {
          modelAndView.addObject("signMessageConsent", this.signMessageUiHandler.getSignMessageConsentModel(
            signMessageContext.getMessageToDisplay(), attributes,
            (String) proxyContext.getAdditionalData("country"), this.getPeerMetadata(context)));

          return modelAndView;
        }
        else {
          modelAndView.addObject("signMessageConsent", this.signMessageUiHandler.getSignMessageConsentModel(
            null, attributes, (String) proxyContext.getAdditionalData("country"), this.getPeerMetadata(context)));
          return modelAndView;
        }
      }
    }

    if (ACTION_CANCEL.equals(action)) {
      // User did not approve to the sign consent.
      log.info("User did not approve to sign consent");
      this.cancel(httpRequest, httpResponse);
      return null;
    }

    boolean signMessageDisplayed = ACTION_OK.equals(action);
    try {
      String loaToIssue = this.eidasAuthnContextService.getReturnAuthnContextClassRef(context, result.getAuthnContextClassUri(),
        signMessageDisplayed);

      // Check if we should issue a SAD attribute.
      //
      SignatureActivationDataContext sadContext = this.getSignSupportService().getSadContext(context);
      if (sadContext != null && signMessageDisplayed && this.getSignSupportService().isSignatureServicePeer(context)) {
        String sad = this.getSignSupportService().issueSAD(context, attributes,
          this.attributeProcessingService.getPrincipalAttributeName(), loaToIssue);

        attributes.add(AttributeConstants.ATTRIBUTE_TEMPLATE_SAD.createBuilder().value(sad).build());
      }

      this.success(httpRequest, httpResponse, this.attributeProcessingService.getPrincipal(attributes), attributes, loaToIssue, result
        .getAuthnInstant(), null);
      return null;
    }
    catch (ExternalAutenticationErrorCodeException e) {
      log.warn("Error during ProxyIdP assertion process - {} - {}", e.getMessage(), e.getActualMessage(), e);
      this.error(httpRequest, httpResponse, e);
      return null;
    }
    catch (AttributeProcessingException e) {
      log.warn("Error during attribute release process - {}", e.getMessage(), e);
      this.error(httpRequest, httpResponse, AuthnEventIds.AUTHN_EXCEPTION);
      return null;
    }
  }

  /**
   * Method that will build an error status and pass back to the requester.
   * 
   * @param httpRequest
   *          the HTTP request
   * @param httpResponse
   *          the HTTP response
   * @param statusCode
   *          the main SAML status code
   * @param subStatusCode
   *          the sub status code
   * @param statusMessage
   *          the status message to include
   * @param verboseStatusMessage
   *          verbose message (included in {@link #verboseStatusMessage} is set)
   * @throws ExternalAuthenticationException
   *           for Shibboleth session errors
   * @throws IOException
   *           for IO errors
   */
  protected void error(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
      String statusCode,
      String subStatusCode,
      String statusMessage,
      String verboseStatusMessage)
          throws ExternalAuthenticationException, IOException {

    Status status = ObjectUtils.createSamlObject(Status.class);
    StatusCode _statusCode = ObjectUtils.createSamlObject(StatusCode.class);
    _statusCode.setValue(statusCode);
    status.setStatusCode(_statusCode);

    if (subStatusCode != null) {
      StatusCode _subStatusCode = ObjectUtils.createSamlObject(StatusCode.class);
      _subStatusCode.setValue(subStatusCode);
      status.getStatusCode().setStatusCode(_subStatusCode);
    }

    StatusMessage _statusMessage = ObjectUtils.createSamlObject(StatusMessage.class);
    String msg = null;

    if (!this.verboseStatusMessage && statusMessage != null) {
      msg = statusMessage;
    }
    else if (this.verboseStatusMessage) {
      StringBuilder sb = new StringBuilder();
      if (statusMessage != null) {
        sb.append(statusMessage);
      }
      if (verboseStatusMessage != null) {
        if (sb.length() > 0) {
          sb.append(" - ");
        }
        sb.append(verboseStatusMessage);
      }
      msg = sb.toString();
      if (sb.length() > 0) {
        msg = sb.toString();
      }
    }

    if (msg != null) {
      _statusMessage.setMessage(msg);
      status.setStatusMessage(_statusMessage);
    }

    this.error(httpRequest, httpResponse, status);
  }

  /**
   * Creates a {@link ResponseProcessingInput} for response processing.
   * 
   * @param context
   *          the profile context
   * @param httpRequest
   *          the HTTP request
   * @return a {@link ResponseProcessingInput} object
   * @throws ExternalAuthenticationException
   *           if there is no {@code ProxyIdpAuthenticationContext} available
   */
  private ResponseProcessingInput createResponseProcessingInput(ProfileRequestContext<?, ?> context, HttpServletRequest httpRequest)
      throws ExternalAuthenticationException {

    ProxyIdpAuthenticationContext proxyContext = proxyIdpAuthenticationContextLookupStrategy.apply(context);
    if (proxyContext == null) {
      throw new ExternalAuthenticationException("No ProxyIdpAuthenticationContext available");
    }

    final String relayState = this.getRelayState(context);

    return new ResponseProcessingInput() {
      @Override
      public AuthnRequest getAuthnRequest() {
        return proxyContext.getAuthnRequest();
      }

      @Override
      public String getRelayState() {
        return relayState;
      }

      @Override
      public String getReceiveURL() {
        return httpRequest.getRequestURL().toString();
      }

      @Override
      public String getClientIpAddress() {
        return httpRequest.getRemoteAddr();
      }

      @Override
      public String getCountry() {
        return (String) proxyContext.getAdditionalData("country");
      }

      @Override
      public long getReceiveInstant() {
        return System.currentTimeMillis();
      }
    };
  }

  /** {@inheritDoc} */
  @Override
  public String getAuthenticatorName() {
    return this.authenticatorName;
  }

  /**
   * Assigns the name of this authenticator.
   * 
   * @param authenticatorName
   *          the name
   */
  public void setAuthenticatorName(String authenticatorName) {
    this.authenticatorName = authenticatorName;
  }

  /**
   * Assigns the service for processing AuthnContext class URI:s.
   * 
   * @param eidasAuthnContextService
   *          service
   */
  public void setEidasAuthnContextService(EidasAuthnContextService eidasAuthnContextService) {
    super.setAuthnContextService(eidasAuthnContextService);
    this.eidasAuthnContextService = eidasAuthnContextService;
  }

  /**
   * Assigns the AuthnRequest generator bean.
   * 
   * @param authnRequestGenerator
   *          generator
   */
  public void setAuthnRequestGenerator(EidasAuthnRequestGenerator authnRequestGenerator) {
    this.authnRequestGenerator = authnRequestGenerator;
  }

  /**
   * Assigns the SAML response processor bean.
   * 
   * @param responseProcessor
   *          processor
   */
  public void setResponseProcessor(ResponseProcessor responseProcessor) {
    this.responseProcessor = responseProcessor;
  }

  /**
   * Assigns the configurator object holding information about the eIDAS metadata.
   * 
   * @param metadataConfig
   *          configurator object
   */
  public void setMetadataConfig(MetadataConfig metadataConfig) {
    this.metadataConfig = metadataConfig;
  }

  /**
   * Assigns the service for handling mappings of attributes.
   * 
   * @param attributeProcessingService
   *          the attribute processing service
   */
  public void setAttributeProcessingService(AttributeProcessingService attributeProcessingService) {
    this.attributeProcessingService = attributeProcessingService;
  }

  /**
   * Assigns the helper bean for handling country selection.
   * 
   * @param countrySelectionHandler
   *          country selection handler
   */
  public void setCountrySelectionHandler(CountrySelectionHandler countrySelectionHandler) {
    this.countrySelectionHandler = countrySelectionHandler;
  }

  /**
   * Assigns the helper bean for displaying sign message consent.
   * 
   * @param signMessageUiHandler
   *          sign message consent handler
   */
  public void setSignMessageUiHandler(SignMessageUiHandler signMessageUiHandler) {
    this.signMessageUiHandler = signMessageUiHandler;
  }

  /**
   * Assigns the helper bean for handling user UI language.
   * 
   * @param uiLanguageHandler
   *          the UI language handler
   */
  public void setUiLanguageHandler(UiLanguageHandler uiLanguageHandler) {
    this.uiLanguageHandler = uiLanguageHandler;
  }

  /**
   * Assigns the flag telling whether we should include verbose status messages in response messages. Default is
   * {@code false}.
   * 
   * @param verboseStatusMessage
   *          {@code true} for verbose status messages
   */
  public void setVerboseStatusMessage(boolean verboseStatusMessage) {
    this.verboseStatusMessage = verboseStatusMessage;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    super.afterPropertiesSet();
    Assert.hasText(this.authenticatorName, "The property 'authenticatorName' must be assigned");
    Assert.notNull(this.metadataConfig, "The property 'metadataConfig' must be assigned");
    Assert.notNull(this.eidasAuthnContextService, "The property 'eidasAuthnContextService' must be assigned");
    Assert.notNull(this.authnRequestGenerator, "The property 'authnRequestGenerator' must be assigned");
    Assert.notNull(this.responseProcessor, "The property 'responseProcessor' must be assigned");
    Assert.notNull(this.countrySelectionHandler, "The property 'countrySelectionHandler' must be assigned");
    Assert.notNull(this.signMessageUiHandler, "The property 'signMessageUiHandler' must be assigned");
    Assert.notNull(this.uiLanguageHandler, "The property 'uiLanguageHandler' must be assigned");
    Assert.notNull(this.attributeProcessingService, "The property 'attributeProcessingService' must be assigned");
  }

}
