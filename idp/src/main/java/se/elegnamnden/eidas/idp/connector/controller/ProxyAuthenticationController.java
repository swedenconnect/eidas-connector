/*
 * The eidas-connector project is the implementation of the Swedish eIDAS 
 * connector built on top of the Shibboleth IdP.
 *
 * More details on <https://github.com/elegnamnden/eidas-connector> 
 * Copyright (C) 2017 E-legitimationsnämnden
 * 
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.elegnamnden.eidas.idp.connector.controller;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.xmlsec.encryption.support.DecryptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.ExternalAuthenticationException;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import se.elegnamnden.eidas.idp.connector.controller.model.UiCountry;
import se.elegnamnden.eidas.idp.connector.service.AttributeProcessingService;
import se.elegnamnden.eidas.idp.connector.service.AuthenticationContextService;
import se.elegnamnden.eidas.idp.connector.sp.EidasAuthnRequestGenerator;
import se.elegnamnden.eidas.idp.connector.sp.EidasAuthnRequestGeneratorInput;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessingException;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessingInput;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessor;
import se.elegnamnden.eidas.idp.connector.sp.ResponseProcessor.IdpMetadataResolver;
import se.elegnamnden.eidas.metadataconfig.MetadataConfig;
import se.elegnamnden.eidas.metadataconfig.data.EndPointConfig;
import se.litsec.opensaml.saml2.common.request.RequestGenerationException;
import se.litsec.opensaml.saml2.common.request.RequestHttpObject;
import se.litsec.opensaml.saml2.metadata.PeerMetadataResolver;
import se.litsec.shibboleth.idp.authn.ExternalAutenticationErrorCodeException;
import se.litsec.shibboleth.idp.authn.context.ProxyIdpAuthenticationContext;
import se.litsec.shibboleth.idp.authn.controller.AbstractExternalAuthenticationController;
import se.litsec.swedisheid.opensaml.saml2.attribute.AttributeSet;
import se.litsec.swedisheid.opensaml.saml2.attribute.AttributeSetConstants;
import se.litsec.swedisheid.opensaml.saml2.signservice.dss.Message;
import se.litsec.swedisheid.opensaml.saml2.signservice.dss.SignMessage;

/**
 * The main controller for the Shibboleth external authentication flow implementing proxy functionality against the
 * eIDAS framework.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
@Controller
public class ProxyAuthenticationController extends AbstractExternalAuthenticationController implements InitializingBean {

  /** The name of the cookie that holds the selected country. */
  public static final String DEFAULT_SELECTED_COUNTRY_COOKIE_NAME = "selectedCountry";

  /** Symbolic name for the action parameter value of "cancel". */
  public static final String ACTION_CANCEL = "cancel";

  /** Symbolic name for the action parameter value of "authenticate". */
  public static final String ACTION_AUTHENTICATE = "authenticate";

  /** The default locale. */
  public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

  /** The attribute set that this IdP is implementing. */
  public static final AttributeSet IMPLEMENTED_ATTRIBUTE_SET = AttributeSetConstants.ATTRIBUTE_SET_EIDAS_NATURAL_PERSON;

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(ProxyAuthenticationController.class);

  /** The name of the authenticator. */
  private String authenticatorName;
  
  /** Generator for SP AuthnRequests. */
  private EidasAuthnRequestGenerator authnRequestGenerator;
  
  /** Processor for handling of SAML responses received from the foreign IdP. */
  private ResponseProcessor responseProcessor;

  /** Configurator for eIDAS metadata. */
  private MetadataConfig metadataConfig;

  /** Service for handling of Authentication Context URIs. */
  private AuthenticationContextService authenticationContextService;

  /** Service for handling mappings of attributes. */
  private AttributeProcessingService attributeProcessingService;

  /** The Spring message source holding localized UI message strings. */
  private MessageSource messageSource;

  /**
   * The name for the cookie that stores the selected country. Defaults to
   * {@value #DEFAULT_SELECTED_COUNTRY_COOKIE_NAME}.
   */
  private String selectedCountryCookieName;

  /** {@inheritDoc} */
  @Override
  protected ModelAndView doExternalAuthentication(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String key,
      ProfileRequestContext<?, ?> profileRequestContext) throws ExternalAuthenticationException, IOException {

    // Selected country from earlier session.
    //
    final String selectedCountry = this.getSelectedCountry(httpRequest);
    log.debug("Selected country from previous session: {}", selectedCountry != null ? selectedCountry : "none");

    // TMP
    if (this.isSignatureServicePeer(profileRequestContext)) {
      SignMessage signMessage = this.getSignMessage(profileRequestContext);
      if (signMessage.getEncryptedMessage() != null) {
        log.debug("SignMessage is available and encrypted. Decrypting ...");
        try {
          Message msg = this.decryptSignMessage(signMessage);
          log.debug("Decrypted SignMessage: {}", msg.getContent());
        }
        catch (DecryptionException e) {
          log.error("Failed to decrypt SignMessage", e);
        }
      }
      else {
        log.debug("SignMessage is available in cleartext: {}", signMessage.getMessage().getContent());
      }
    }

    // The first step is to prompt the user for which country to direct to.
    // If the request is made by a signature service and the selected country is known, we skip
    // the "select country" view.
    //
    if (this.isSignatureServicePeer(profileRequestContext) && selectedCountry != null) {
      log.info("Request is from a signature service. Will default to previously selected country: '{}'", selectedCountry);
      return this.processAuthentication(httpRequest, httpResponse, ACTION_AUTHENTICATE, selectedCountry);
    }

    ModelAndView modelAndView = new ModelAndView("country-select");
    modelAndView.addObject("authenticationKey", key);
    modelAndView.addObject("countries", this.getSelectableCountries());

    if (selectedCountry != null) {
      modelAndView.addObject("selectedCountry", selectedCountry);
    }

    // LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(httpRequest);
    // localeResolver.setLocale(httpRequest, httpResponse, Locale.forLanguageTag("en"));

    return modelAndView;
  }

  @RequestMapping(value = "/proxyauth", method = RequestMethod.POST)
  public ModelAndView processAuthentication(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
      @RequestParam("action") String action,
      @RequestParam("selectedCountry") String selectedCountry) throws ExternalAuthenticationException, IOException {

    if (ACTION_CANCEL.equals(action)) {
      log.info("User cancelled country selection - aborting authentication");
      this.cancel(httpRequest, httpResponse);
      return null;
    }

    log.debug("User selected country '{}'", selectedCountry);
    this.saveSelectedCountry(httpResponse, selectedCountry);

    // Get hold of all information needed for the foreign endpoint.
    //
    EndPointConfig endPoint = this.metadataConfig.getProxyServiceConfig(selectedCountry);
    if (endPoint == null) {
      log.error("No endpoint found for country '{}'", selectedCountry);
      this.error(httpRequest, httpResponse, AuthnEventIds.INVALID_AUTHN_CTX);
      return null;
    }

    final ProfileRequestContext<?, ?> context = this.getProfileRequestContext(httpRequest);

    // Next, generate an AuthnRequest and redirect or post the user there.
    //
    EidasAuthnRequestGeneratorInput spInput;
    try {
      spInput = this.createAuthnRequestInput(context, endPoint);
    }
    catch (ExternalAutenticationErrorCodeException e) {
      log.info("Error while building AuthnRequest - {} - {}", e.getMessage(), e.getActualMessage());
      this.error(httpRequest, httpResponse, e);
      return null;
    }

    try {
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
      this.error(httpRequest, httpResponse, AuthnEventIds.REQUEST_UNSUPPORTED); // TODO: change
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
   * @return an {@code AuthnRequestInput} object
   * @throws ExternalAutenticationErrorCodeException
   *           for errors creating the request
   */
  private EidasAuthnRequestGeneratorInput createAuthnRequestInput(ProfileRequestContext<?, ?> context, EndPointConfig endPoint)
      throws ExternalAutenticationErrorCodeException {

    EidasAuthnRequestGeneratorInput spInput = new EidasAuthnRequestGeneratorInput();
    
    spInput.setPeerEntityID(endPoint.getEntityID());
    spInput.setCountry(endPoint.getCountry());

    // Match assurance levels and calculate which LoA URI to use in the request.
    //
    String loaUriToRequest = this.authenticationContextService.getEidasAuthnContextURI(
      this.getRequestedAuthnContextClassRefs(context), endPoint.getAssuranceCertifications(), endPoint.isSupportsNonNotifiedConcept());
    if (loaUriToRequest == null) {
      throw new ExternalAutenticationErrorCodeException(AuthnEventIds.INVALID_AUTHN_CTX,
        "AuthnRequest did not contain a matching requested Authn Context URI");
    }
    spInput.setRequestedLevelOfAssurance(loaUriToRequest);

    // Relay state
    spInput.setRelayState(this.getRelayState(context));

    // Requested attributes
    // First get the default ones to request from the implemented attribute set.
    // Then check if the peer specifies any additional requested attributes in its metadata. If so, transform them
    // to eIDAS.
    //
    List<se.litsec.eidas.opensaml.ext.RequestedAttribute> requestedAttributes = new ArrayList<>();
    requestedAttributes.addAll(
      this.attributeProcessingService.getEidasRequestedAttributesFromAttributeSet(IMPLEMENTED_ATTRIBUTE_SET));
    requestedAttributes.addAll(
      this.attributeProcessingService.getEidasRequestedAttributesFromMetadata(this.getPeerMetadata(context), requestedAttributes));

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

    AuthenticationContext authenticationContext = this.authenticationContextLookupStrategy.apply(context);
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
    
    // this.serviceProvider.processAuthnResponse(samlResponse, relayState, input, idpEndpoint);
    try {
      IdpMetadataResolver idpMetadataResolver = (entityID) -> {
        return (entityID.equals(idpEndpoint.getEntityID())) ? idpEndpoint.getMetadataRecord() : null;
      };
      
      this.responseProcessor.processSamlResponse(samlResponse, relayState, input, idpMetadataResolver);
      log.debug("Successfully processed SAML response");
    }
    catch (ResponseProcessingException e) {
      log.error("Error while processing eIDAS response - {}", e.getMessage(), e);
      this.error(httpRequest, httpResponse, AuthnEventIds.REQUEST_UNSUPPORTED); // TODO: change
      return null;
    }
    
    return null;
  }

  private ResponseProcessingInput createResponseProcessingInput(ProfileRequestContext<?, ?> context, HttpServletRequest httpRequest)
      throws ExternalAuthenticationException {

    AuthenticationContext authenticationContext = this.authenticationContextLookupStrategy.apply(context);
    if (authenticationContext == null) {
      throw new ExternalAuthenticationException("No AuthenticationContext available");
    }
    ProxyIdpAuthenticationContext proxyContext = authenticationContext.getSubcontext(ProxyIdpAuthenticationContext.class);
    if (proxyContext == null) {
      throw new ExternalAuthenticationException("No ProxyIdpAuthenticationContext available");
    }
        
    return new ResponseProcessingInput() {
      @Override
      public AuthnRequest getAuthnRequest() {
        return proxyContext.getAuthnRequest();
      }

      @Override
      public String getRelayState() {
        return getRelayState();
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
    };
  }

  /**
   * Returns the selected country by inspecting the "selected country" cookie.
   * 
   * @param httpRequest
   *          the HTTP request
   * @return the selected country, or {@code null} if this information is not available
   */
  private String getSelectedCountry(HttpServletRequest httpRequest) {
    Cookie[] cookies = httpRequest.getCookies();
    return cookies != null ? Arrays.asList(cookies)
      .stream()
      .filter(c -> this.selectedCountryCookieName.equals(c.getName()))
      .map(c -> c.getValue())
      .findFirst()
      .orElse(null) : null;
  }

  /**
   * Saves the selected country in a user cookie.
   * 
   * @param httpResponse
   *          the HTTP response
   * @param selectedCountry
   *          the selected country
   */
  private void saveSelectedCountry(HttpServletResponse httpResponse, String selectedCountry) {
    Cookie cookie = new Cookie(this.selectedCountryCookieName, selectedCountry);
    cookie.setPath("/idp");
    httpResponse.addCookie(cookie);
  }

  /**
   * Returns a list of country objects for displaying in the list of possible countries in the UI. The list is sorted
   * according to the current locale.
   * 
   * @return a sorted list of country objects
   */
  private List<UiCountry> getSelectableCountries() {

    // Ask the metadata configurator about which countries that have an eIDAS Proxy service registered.
    //
    this.metadataConfig.recache();
    List<String> isoCodes = this.metadataConfig.getProxyServiceCountryList();

    // We want to avoid locale based sorting in the view, so we'll provide a sorted list based on each
    // country's localized display name.
    //
    Locale locale = LocaleContextHolder.getLocale();

    List<UiCountry> countries = new ArrayList<>();
    for (String code : isoCodes) {
      String displayName = null;
      try {
        displayName = this.messageSource.getMessage("connector.ui.country." + code.toUpperCase(), null, locale);
      }
      catch (NoSuchMessageException e) {
        // Maybe, there is no mapping for the given locale. Try the default locale also.
        if (!locale.equals(DEFAULT_LOCALE)) {
          try {
            displayName = this.messageSource.getMessage("connector.ui.country." + code.toUpperCase(), null, DEFAULT_LOCALE);
          }
          catch (NoSuchMessageException e2) {
          }
        }
      }

      if (displayName != null) {
        countries.add(new UiCountry(code, displayName));
      }
      else {
        // A fake country for test...
        displayName = this.messageSource.getMessage("connector.ui.country.TEST", new Object[] { code }, code + "Test Country", locale);
        countries.add(new UiCountry(code, displayName, false));
      }
    }

    Collator collator = Collator.getInstance(locale);

    countries.sort(new Comparator<UiCountry>() {

      @Override
      public int compare(UiCountry o1, UiCountry o2) {
        if (!o1.isRealCountry()) {
          return 1;
        }
        if (!o2.isRealCountry()) {
          return -1;
        }
        return collator.compare(o1.getName(), o2.getName());
      }
    });

    return countries;
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
   * Assigns the AuthnRequest generator bean.
   * @param authnRequestGenerator generator
   */
  public void setAuthnRequestGenerator(EidasAuthnRequestGenerator authnRequestGenerator) {
    this.authnRequestGenerator = authnRequestGenerator;
  }

  /**
   * Assigns the SAML response processor bean.
   * @param responseProcessor processor
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
   * Assigns the service for handling Authentication Context URI:s.
   * 
   * @param authenticationContextService
   *          the authentication context service
   */
  public void setAuthenticationContextService(AuthenticationContextService authenticationContextService) {
    this.authenticationContextService = authenticationContextService;
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
   * Assigns the message source holding the localized UI messages used.
   * 
   * @param messageSource
   *          Spring message source
   */
  public void setMessageSource(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /**
   * Assigns the name that should be used for the cookie that stores the selected country. Defaults to
   * {@value #DEFAULT_SELECTED_COUNTRY_COOKIE_NAME}.
   * 
   * @param selectedCountryCookieName
   *          the cookie name
   */
  public void setSelectedCountryCookieName(String selectedCountryCookieName) {
    this.selectedCountryCookieName = selectedCountryCookieName;
  }

  /** {@inheritDoc} */
  @Override
  public void afterPropertiesSet() throws Exception {
    super.afterPropertiesSet();
    Assert.hasText(this.authenticatorName, "The property 'authenticatorName' must be assigned");
    Assert.notNull(this.metadataConfig, "The property 'metadataConfig' must be assigned");
    Assert.notNull(this.authnRequestGenerator, "The property 'authnRequestGenerator' must be assigned");
    Assert.notNull(this.responseProcessor, "The property 'responseProcessor' must be assigned");
    Assert.notNull(this.messageSource, "The property 'messageSource' must be assigned");
    Assert.notNull(this.authenticationContextService, "The property 'authenticationContextService' must be assigned");
    Assert.notNull(this.attributeProcessingService, "The property 'attributeProcessingService' must be assigned");
    if (!StringUtils.hasText(this.selectedCountryCookieName)) {
      this.selectedCountryCookieName = DEFAULT_SELECTED_COUNTRY_COOKIE_NAME;
      log.debug("Name of cookie that holds selected country was not given - defaulting to {}", this.selectedCountryCookieName);
    }

    // TMP
    this.metadataConfig.recache();
  }

}
