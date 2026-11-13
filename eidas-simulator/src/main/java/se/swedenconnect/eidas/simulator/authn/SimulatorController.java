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
package se.swedenconnect.eidas.simulator.authn;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.shibboleth.shared.xml.SerializeSupport;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.Organization;
import org.opensaml.saml.saml2.metadata.OrganizationDisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import se.swedenconnect.eidas.simulator.country.CountryRepository;
import se.swedenconnect.eidas.simulator.country.SimulatedCountry;
import se.swedenconnect.eidas.simulator.country.SimulatedUser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The endpoints and pages of the simulated countries: the single sign-on service that receives requests from the
 * connector, the page where the tester answers a request, and the start page.
 *
 * @author Martin Lindström
 */
@Controller
public class SimulatorController {

  /** Logger. */
  private static final Logger log = LoggerFactory.getLogger(SimulatorController.class);

  /** The value of the user selection for a new user. */
  public static final @NonNull String NEW_USER = "new";

  /** The active countries. */
  private final @NonNull CountryRepository countries;

  /** Decodes requests. */
  private final @NonNull AuthnRequestDecoder decoder;

  /** Processes requests. */
  private final @NonNull AuthnRequestProcessor processor;

  /** Releases attributes. */
  private final @NonNull AttributeReleaser attributeReleaser;

  /** Creates responses. */
  private final @NonNull ResponseFactory responseFactory;

  /** Keeps the users that the tester has added. */
  private final @NonNull SavedUsersCookie savedUsersCookie;

  /**
   * Constructor.
   *
   * @param countries the active countries
   * @param decoder decodes requests
   * @param processor processes requests
   * @param attributeReleaser releases attributes
   * @param responseFactory creates responses
   * @param savedUsersCookie keeps the users that the tester has added
   */
  public SimulatorController(final @NonNull CountryRepository countries, final @NonNull AuthnRequestDecoder decoder,
      final @NonNull AuthnRequestProcessor processor, final @NonNull AttributeReleaser attributeReleaser,
      final @NonNull ResponseFactory responseFactory, final @NonNull SavedUsersCookie savedUsersCookie) {
    this.countries = countries;
    this.decoder = decoder;
    this.processor = processor;
    this.attributeReleaser = attributeReleaser;
    this.responseFactory = responseFactory;
    this.savedUsersCookie = savedUsersCookie;
  }

  /**
   * The start page, listing the active countries.
   *
   * @return the page
   */
  @GetMapping("/")
  public @NonNull ModelAndView index() {
    final ModelAndView mav = new ModelAndView("index");
    mav.addObject("countries", this.countries.getCountries());
    return mav;
  }

  /**
   * The single sign-on service of a country, receiving requests over the HTTP-POST and HTTP-Redirect bindings.
   *
   * @param country the country code
   * @param request the HTTP request
   * @param response the HTTP response
   * @param session the HTTP session
   * @return the page where the tester answers the request, an error response, or an error page
   */
  @RequestMapping(value = "/{country}" + SimulatedCountry.SSO_PATH, method = { RequestMethod.GET, RequestMethod.POST })
  public @NonNull ModelAndView receiveRequest(final @PathVariable("country") @NonNull String country,
      final @NonNull HttpServletRequest request, final @NonNull HttpServletResponse response,
      final @NonNull HttpSession session) {

    final SimulatedCountry simulatedCountry = this.countries.getCountry(country);
    if (simulatedCountry == null) {
      return this.errorPage(response, HttpStatus.NOT_FOUND, "No simulated country with code " + country);
    }
    try {
      final ProcessedRequest processed =
          this.processor.process(simulatedCountry, this.decoder.decode(request));
      SimulatorSession.get(session).addRequest(processed);
      return new ModelAndView("redirect:/%s/authn/%s".formatted(simulatedCountry.getCode().toLowerCase(),
          processed.id()));
    }
    catch (final UnrecoverableRequestException e) {
      log.info("{}: Rejected request - {}", simulatedCountry.getCode(), e.getMessage());
      return this.errorPage(response, HttpStatus.BAD_REQUEST, e.getMessage());
    }
    catch (final ErrorResponseException e) {
      return this.sendErrorResponse(simulatedCountry, e.getTarget(), e.getStatus(), response);
    }
  }

  /**
   * The page where the tester answers a request.
   *
   * @param country the country code
   * @param id the identity of the request
   * @param userId the selected user, or {@code null} for the first user
   * @param httpRequest the HTTP request
   * @param response the HTTP response
   * @param session the HTTP session
   * @return the page
   */
  @GetMapping("/{country}/authn/{id}")
  public @NonNull ModelAndView showRequest(final @PathVariable("country") @NonNull String country,
      final @PathVariable("id") @NonNull String id, final @RequestParam(value = "user", required = false)
      @Nullable String userId, final @NonNull HttpServletRequest httpRequest,
      final @NonNull HttpServletResponse response, final @NonNull HttpSession session) {

    final SimulatedCountry simulatedCountry = this.countries.getCountry(country);
    final SimulatorSession simulatorSession = SimulatorSession.get(session);
    final ProcessedRequest request = simulatorSession.getRequest(id);
    if (simulatedCountry == null || request == null || !request.countryCode().equals(simulatedCountry.getCode())) {
      return this.errorPage(response, HttpStatus.NOT_FOUND,
          "The request is not known. It may already have been answered.");
    }

    final List<SimulatedUser> users = this.getUsers(simulatedCountry, httpRequest);
    final AuthnForm form = new AuthnForm();
    final SimulatedUser selected = NEW_USER.equals(userId)
        ? null
        : users.stream()
            .filter(u -> userId == null || userId.equals(u.getId()))
            .findFirst()
            .orElse(null);
    if (selected != null) {
      form.setUserId(selected.getId());
      form.setUser(selected.copy());
    }
    else {
      form.setUserId(NEW_USER);
    }
    form.setLoa(request.allowedLoas().getLast());

    return this.authnPage(simulatedCountry, request, users, form, null);
  }

  /**
   * Answers a request, with a success response for the user or an error response.
   *
   * @param country the country code
   * @param id the identity of the request
   * @param form the form
   * @param httpRequest the HTTP request
   * @param response the HTTP response
   * @param session the HTTP session
   * @return a page posting the response to the connector, or the authentication page if the form is not complete
   */
  @PostMapping("/{country}/authn/{id}")
  public @NonNull ModelAndView answerRequest(final @PathVariable("country") @NonNull String country,
      final @PathVariable("id") @NonNull String id, final @ModelAttribute("form") @NonNull AuthnForm form,
      final @NonNull HttpServletRequest httpRequest, final @NonNull HttpServletResponse response,
      final @NonNull HttpSession session) {

    final SimulatedCountry simulatedCountry = this.countries.getCountry(country);
    final SimulatorSession simulatorSession = SimulatorSession.get(session);
    final ProcessedRequest request = simulatorSession.getRequest(id);
    if (simulatedCountry == null || request == null || !request.countryCode().equals(simulatedCountry.getCode())) {
      return this.errorPage(response, HttpStatus.NOT_FOUND,
          "The request is not known. It may already have been answered.");
    }

    if (AuthnForm.ACTION_ERROR.equals(form.getAction())) {
      final ErrorResponseOption option =
          Optional.ofNullable(form.getErrorOption()).orElse(ErrorResponseOption.CANCEL);
      simulatorSession.removeRequest(id);
      log.info("{}: Tester sent error response {} for request '{}'", simulatedCountry.getCode(), option,
          request.target().inResponseTo());
      return this.sendErrorResponse(simulatedCountry, request.target(), option.toStatus(form.getStatusMessage()),
          response);
    }

    final SimulatedUser user = form.getUser();
    final List<String> problems = new ArrayList<>();
    if (form.getLoa() == null || !request.allowedLoas().contains(form.getLoa())) {
      problems.add("Select a level of assurance");
    }
    this.attributeReleaser.getMissingRequiredAttributes(simulatedCountry, request, user).stream()
        .map(n -> "A value is required for " + n.substring(n.lastIndexOf('/') + 1))
        .forEach(problems::add);
    if (!problems.isEmpty()) {
      return this.authnPage(simulatedCountry, request, this.getUsers(simulatedCountry, httpRequest), form,
          problems);
    }

    if (form.isSaveUser()) {
      if (!StringUtils.hasText(user.getId()) || NEW_USER.equals(form.getUserId())
          || simulatedCountry.getUser(form.getUserId()) != null) {
        user.setId("custom-" + UUID.randomUUID().toString().substring(0, 8));
      }
      this.savedUsersCookie.saveUser(simulatedCountry.getCode(), user.copy(), httpRequest, response);
    }

    final List<Attribute> attributes = this.attributeReleaser.releaseAttributes(simulatedCountry, request, user);
    try {
      final Response samlResponse = this.responseFactory.createSuccessResponse(simulatedCountry, request, user,
          Objects.requireNonNull(form.getLoa()), attributes);
      simulatorSession.removeRequest(id);
      log.info("{}: Sending response for '{}' to {} [loa: {}, attributes: {}]", simulatedCountry.getCode(),
          user.getDisplayName(), request.target().assertionConsumerServiceUrl(), form.getLoa(),
          attributes.stream().map(Attribute::getFriendlyName).toList());
      return postResponse(samlResponse, request.target());
    }
    catch (final ResponseCreationException e) {
      return this.errorPage(response, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Creates the authentication page.
   *
   * @param country the simulated country
   * @param request the request
   * @param users the users to choose from
   * @param form the form
   * @param problems problems with the submitted form, or {@code null}
   * @return the page
   */
  private @NonNull ModelAndView authnPage(final @NonNull SimulatedCountry country,
      final @NonNull ProcessedRequest request, final @NonNull List<SimulatedUser> users, final @NonNull AuthnForm form,
      final @Nullable List<String> problems) {

    final Map<String, Boolean> fields = new LinkedHashMap<>();
    for (final String name : this.attributeReleaser.getReleasableAttributes(country, request)) {
      fields.put(name.substring(name.lastIndexOf('/') + 1), request.isRequired(name));
    }
    final List<String> ignored = request.requestedAttributes().stream()
        .map(ProcessedRequest.RequestedAttribute::name)
        .filter(n -> !this.attributeReleaser.getReleasableAttributes(country, request).contains(n))
        .map(n -> n.substring(n.lastIndexOf('/') + 1))
        .toList();

    final ModelAndView mav = new ModelAndView("authn");
    mav.addObject("country", country);
    mav.addObject("request", request);
    mav.addObject("connectorName", getConnectorName(request.target()));
    mav.addObject("users", users);
    mav.addObject("form", form);
    mav.addObject("fields", fields);
    mav.addObject("ignoredAttributes", ignored);
    mav.addObject("personIdentifierPrefix",
        "%s/%s/".formatted(country.getCode(), request.destinationCountry()));
    mav.addObject("errorOptions", ErrorResponseOption.values());
    mav.addObject("problems", problems);
    mav.addObject("newUser", NEW_USER);
    return mav;
  }

  /**
   * Sends an error response to the connector.
   *
   * @param country the simulated country
   * @param target where the response is sent
   * @param status the status
   * @param response the HTTP response
   * @return a page posting the response, or an error page if the response cannot be created
   */
  private @NonNull ModelAndView sendErrorResponse(final @NonNull SimulatedCountry country,
      final @NonNull ResponseTarget target, final @NonNull EidasStatus status,
      final @NonNull HttpServletResponse response) {
    try {
      return postResponse(this.responseFactory.createErrorResponse(country, target, status), target);
    }
    catch (final ResponseCreationException e) {
      return this.errorPage(response, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Creates the page that posts a response to the connector.
   *
   * @param samlResponse the response
   * @param target where the response is sent
   * @return the page
   */
  private static @NonNull ModelAndView postResponse(final @NonNull Response samlResponse,
      final @NonNull ResponseTarget target) {
    final String xml;
    try {
      xml = SerializeSupport.nodeToString(XMLObjectSupport.marshall(samlResponse));
    }
    catch (final MarshallingException e) {
      throw new IllegalStateException("Failed to marshall response", e);
    }
    final ModelAndView mav = new ModelAndView("post-response");
    mav.addObject("action", target.assertionConsumerServiceUrl());
    mav.addObject("SAMLResponse", Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8)));
    mav.addObject("RelayState", target.relayState());
    return mav;
  }

  /**
   * Creates an error page.
   *
   * @param response the HTTP response
   * @param status the HTTP status
   * @param message the error message
   * @return the page
   */
  private @NonNull ModelAndView errorPage(final @NonNull HttpServletResponse response,
      final @NonNull HttpStatus status, final @Nullable String message) {
    response.setStatus(status.value());
    final ModelAndView mav = new ModelAndView("simulator-error");
    mav.setStatus(status);
    mav.addObject("message", message);
    return mav;
  }

  /**
   * Gets the users of a country: the fixed users followed by the users added by the tester.
   *
   * @param country the simulated country
   * @param request the HTTP request
   * @return the users
   */
  private @NonNull List<SimulatedUser> getUsers(final @NonNull SimulatedCountry country,
      final @NonNull HttpServletRequest request) {
    final List<SimulatedUser> users = new ArrayList<>(country.getUsers());
    this.savedUsersCookie.getUsers(country.getCode(), request).stream()
        .filter(u -> country.getUser(u.getId()) == null)
        .forEach(users::add);
    return users;
  }

  /**
   * Gets a display name for the connector, from its metadata.
   *
   * @param target the response target
   * @return the display name
   */
  private static @NonNull String getConnectorName(final @NonNull ResponseTarget target) {
    return Optional.ofNullable(target.connectorMetadata().getOrganization())
        .map(Organization::getDisplayNames)
        .flatMap(names -> names.stream().findFirst())
        .map(OrganizationDisplayName::getValue)
        .orElse(target.getConnectorEntityId());
  }

}
