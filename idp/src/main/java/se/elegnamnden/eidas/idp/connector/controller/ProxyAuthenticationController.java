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
import se.elegnamnden.eidas.idp.connector.controller.model.UiCountry;
import se.elegnamnden.eidas.idp.connector.service.AuthenticationContextService;
import se.elegnamnden.eidas.idp.connector.sp.AuthnRequestInput;
import se.elegnamnden.eidas.idp.connector.sp.ProxyAuthenticationServiceProvider;
import se.elegnamnden.eidas.idp.connector.sp.ProxyAuthenticationServiceProviderException;
import se.elegnamnden.eidas.metadataconfig.MetadataConfig;
import se.elegnamnden.eidas.metadataconfig.data.EndPointConfig;
import se.litsec.opensaml.saml2.authentication.RequestHttpObject;
import se.litsec.shibboleth.idp.authn.controller.AbstractExternalAuthenticationController;
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

  /** Logging instance. */
  private final Logger log = LoggerFactory.getLogger(ProxyAuthenticationController.class);

  /** The name of the authenticator. */
  private String authenticatorName;

  /** The ProxyIdP Service Provider. */
  private ProxyAuthenticationServiceProvider serviceProvider;

  /** Configurator for eIDAS metadata. */
  private MetadataConfig metadataConfig;

  /** Service for handling of Authentication Context URIs. */
  private AuthenticationContextService authenticationContextService;

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
    EndPointConfig endPointConfig = this.metadataConfig.getProxySeriveConfig(selectedCountry);
    if (endPointConfig == null) {
      log.error("No endpoint found for country '{}'", selectedCountry);
      this.error(httpRequest, httpResponse, AuthnEventIds.INVALID_AUTHN_CTX);
      return null;
    }

    final ProfileRequestContext<?, ?> context = this.getProfileRequestContext(httpRequest);

    // Match assurance levels and calculate which LoA URI to use in the request.
    //
    String loaUriToRequest = this.authenticationContextService.getEidasAuthnContextURI(
      this.getRequestedAuthnContextClassRefs(context), endPointConfig.getAssuranceCertifications(), endPointConfig
        .isSupportsNonNotifiedConcept());
    if (loaUriToRequest == null) {
      log.info("AuthnRequest did not contain a matching requested Authn Context URI");
      this.error(httpRequest, httpResponse, AuthnEventIds.INVALID_AUTHN_CTX);
      return null;
    }

    // Next, generate an AuthnRequest and redirect or post the user there.
    //
    AuthnRequestInput spInput = new AuthnRequestInput();
    spInput.setRelayState(this.getRelayState(context));
    spInput.setRequestedLevelOfAssurance(loaUriToRequest);
    
    try {
      RequestHttpObject<AuthnRequest> authnRequest = this.serviceProvider.generateAuthnRequest(endPointConfig, spInput);
      
      if (SAMLConstants.POST_METHOD.equals(authnRequest.getMethod())) {
        ModelAndView modelAndView = new ModelAndView("post-request");
        modelAndView.addObject("action", authnRequest.getSendUrl());
        modelAndView.addAllObjects(authnRequest.getRequestParameters());
        return modelAndView;
      }
      else { // GET
        return new ModelAndView("redirect:" + authnRequest.getSendUrl());
      }
    }
    catch (ProxyAuthenticationServiceProviderException e) {
      log.error("Error while creating eIDAS AuthnContext - {}", e.getMessage(), e);
      this.error(httpRequest, httpResponse, AuthnEventIds.REQUEST_UNSUPPORTED); // TODO: change
      return null;
    }
  }

  @RequestMapping(value = "/saml2/post", method = RequestMethod.POST)
  public ModelAndView samlResponse(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
      @RequestParam("SAMLResponse") String samlResponse,
      @RequestParam("RelayState") String relayState) throws ExternalAuthenticationException, IOException {

    return null;
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
    List<String> isoCodes = Arrays.asList("AT", "CZ", "EE", "FR", "DE", "IS", "NL", "NO", "XX"); // this.metadataConfig.getProxyServiceCountryList();

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
   * Assigns the Proxy IdP service provider.
   * 
   * @param serviceProvider
   *          the SP instance to assign
   */
  public void setServiceProvider(ProxyAuthenticationServiceProvider serviceProvider) {
    this.serviceProvider = serviceProvider;
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
    Assert.notNull(this.messageSource, "The property 'messageSource' must be assigned");
    Assert.notNull(this.authenticationContextService, "The property 'authenticationContextService' must be assigned");
    if (!StringUtils.hasText(this.selectedCountryCookieName)) {
      this.selectedCountryCookieName = DEFAULT_SELECTED_COUNTRY_COOKIE_NAME;
      log.debug("Name of cookie that holds selected country was not given - defaulting to {}", this.selectedCountryCookieName);
    }
    
    // TMP
    this.metadataConfig.recache();
  }

}
