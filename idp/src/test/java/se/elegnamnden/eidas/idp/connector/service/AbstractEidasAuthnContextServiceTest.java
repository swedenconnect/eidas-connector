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
package se.elegnamnden.eidas.idp.connector.service;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.RequestedPrincipalContext;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import se.elegnamnden.eidas.idp.connector.TestHelper;
import se.elegnamnden.eidas.mapping.loa.StaticLevelOfAssuranceMappings;
import se.litsec.opensaml.config.OpenSAMLInitializer;
import se.litsec.opensaml.saml2.core.build.AuthnRequestBuilder;
import se.litsec.opensaml.saml2.core.build.RequestedAuthnContextBuilder;
import se.litsec.opensaml.saml2.metadata.build.SpEntityDescriptorBuilder;
import se.litsec.swedisheid.opensaml.saml2.metadata.entitycategory.EntityCategoryConstants;

/**
 * Base class for testing {@link EidasAuthnContextService}.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class AbstractEidasAuthnContextServiceTest {
  
  public static final String SP_ENTITYID = "http://www.example.com/sp";
  
  /** The service being tested. */
  protected EidasAuthnContextServiceImpl service;

  /** The request context */
  protected ProfileRequestContext<AuthnRequest, Response> context;

  /** The authentication context. */
  protected AuthenticationContext authenticationContext;

  /** The inbound message context. */
  protected MessageContext<AuthnRequest> inboundMessageContext;  

  @Before
  public void setup() throws Exception {

    OpenSAMLInitializer.getInstance().initialize();

    this.context = new ProfileRequestContext<>();

    this.inboundMessageContext = new MessageContext<>();
    this.context.setInboundMessageContext(this.inboundMessageContext);

    this.authenticationContext = new AuthenticationContext();
    this.context.addSubcontext(this.authenticationContext);

    // Setup the authentication flow and its supported principals ...
    //
    AuthenticationFlowDescriptor flowDescriptor = new AuthenticationFlowDescriptor();
    flowDescriptor.setSupportedPrincipals(TestHelper.SUPPORTED_PRINCIPALS);
    this.authenticationContext.getAvailableFlows().put(TestHelper.FLOW_NAME, flowDescriptor);

    // Setup the authentication context service ...
    //
    this.service = new EidasAuthnContextServiceImpl();
    this.service.setAuthnContextweightMap(TestHelper.AUTHN_WEIGHT_MAP);
    this.service.setFlowName(TestHelper.FLOW_NAME);
    this.service.setLoaMappings(new StaticLevelOfAssuranceMappings());
    this.service.afterPropertiesSet();
  }
  
  protected void simulateAuthnRequest(List<String> requestedAuthnContextUris, boolean sigservice) {
    
    RequestedAuthnContext requestedAuthnContext = null;
    if (requestedAuthnContextUris != null && !requestedAuthnContextUris.isEmpty()) {
      requestedAuthnContext = RequestedAuthnContextBuilder.builder()
          .comparison(AuthnContextComparisonTypeEnumeration.EXACT)
          .authnContextClassRefs(requestedAuthnContextUris)
          .build();
    }
    
    AuthnRequest authnRequest = AuthnRequestBuilder.builder()
        .id("_123456789")
        .issuer(SP_ENTITYID)
        .requestedAuthnContext(requestedAuthnContext)
        .build();
    
    this.inboundMessageContext.setMessage(authnRequest);
    
    // Requested AuthnContext URI:s ...
    //    
    RequestedPrincipalContext rpc = new RequestedPrincipalContext();
    if (requestedAuthnContextUris != null) {
      rpc.setRequestedPrincipals(requestedAuthnContextUris.stream().map(AuthnContextClassRefPrincipal::new).collect(Collectors.toList()));
    }
    this.authenticationContext.addSubcontext(rpc);

    // Simulate SP metadata ...
    //
    SpEntityDescriptorBuilder spMetadataBuilder = (new SpEntityDescriptorBuilder()).id(SP_ENTITYID);
    if (sigservice) {
      spMetadataBuilder.entityCategories(EntityCategoryConstants.SERVICE_TYPE_CATEGORY_SIGSERVICE.getUri());
    }
    EntityDescriptor spMetadata = spMetadataBuilder.build();

    SAMLPeerEntityContext peerEntityContext = new SAMLPeerEntityContext();
    SAMLMetadataContext samlMetadataContext = new SAMLMetadataContext();
    samlMetadataContext.setEntityDescriptor(spMetadata);
    peerEntityContext.addSubcontext(samlMetadataContext);

    this.inboundMessageContext.addSubcontext(peerEntityContext);    
  }

  
}
