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
package se.elegnamnden.eidas.idp.connector.sp.metadata;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import net.shibboleth.utilities.java.support.xml.XMLParserException;
import se.litsec.eidas.opensaml.common.EidasConstants;
import se.litsec.eidas.opensaml.ext.NodeCountry;
import se.litsec.opensaml.saml2.attribute.AttributeTemplate;
import se.litsec.opensaml.saml2.metadata.MetadataUtils;
import se.litsec.opensaml.saml2.metadata.build.spring.SpEntityDescriptorFactoryBean;
import se.litsec.opensaml.utils.ObjectUtils;

/**
 * Extensions to SP metadata specific for eIDAS.
 *
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class EidasSpEntityDescriptorFactoryBean extends SpEntityDescriptorFactoryBean {

  /**
   * The attribute template for the application identifier attribute stored as an attribute in the entity attributes
   * extension.
   */
  public static final AttributeTemplate APPLICATION_IDENTIFIER_TEMPLATE = new AttributeTemplate(
    EidasConstants.EIDAS_APPLICATION_IDENTIFIER_ATTRIBUTE_NAME, null);

  /**
   * The attribute template for the protocol version attribute stored as an attribute in the entity attributes
   * extension.
   */
  public static final AttributeTemplate PROTOCOL_VERSION_TEMPLATE = new AttributeTemplate(
    EidasConstants.EIDAS_PROTOCOL_VERSION_ATTRIBUTE_NAME, null);

  /**
   * See {@link SpEntityDescriptorFactoryBean#SpEntityDescriptorFactoryBean()}.
   */
  public EidasSpEntityDescriptorFactoryBean() {
    super();
  }

  /**
   * See {@link SpEntityDescriptorFactoryBean#SpEntityDescriptorFactoryBean(EntityDescriptor)}.
   * 
   * @param template
   *          the template
   * @throws UnmarshallingException
   *           for unmarshalling errors
   * @throws MarshallingException
   *           for marshalling errors
   */
  public EidasSpEntityDescriptorFactoryBean(EntityDescriptor template) throws UnmarshallingException, MarshallingException {
    super(template);
  }

  /**
   * See {@link SpEntityDescriptorFactoryBean#SpEntityDescriptorFactoryBean(Resource)}.
   * 
   * @param resource
   *          the template resource
   * @throws IOException
   *           if the resource can not be read
   * @throws UnmarshallingException
   *           for unmarshalling errors
   * @throws XMLParserException
   *           for XML parsing errors
   */
  public EidasSpEntityDescriptorFactoryBean(Resource resource) throws XMLParserException, UnmarshallingException, IOException {
    super(resource);
  }

  /**
   * Assigns the {@code NodeCountry} extension to the {@code SPSSODescriptor} element.
   * 
   * @param nodeCountry
   *          the node country (country code).
   */
  public void setNodeCountry(String nodeCountry) {

    SPSSODescriptor ssoDescriptor = this.builder().object().getSPSSODescriptor(SAMLConstants.SAML20P_NS);
    if (ssoDescriptor == null) {
      ssoDescriptor = ObjectUtils.createSamlObject(SPSSODescriptor.class);
      ssoDescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);
      this.builder().object().getRoleDescriptors().add(ssoDescriptor);
    }
    if (ssoDescriptor.getExtensions() == null) {
      if (nodeCountry == null) {
        return;
      }
      ssoDescriptor.setExtensions(ObjectUtils.createSamlObject(Extensions.class));
    }
    Optional<NodeCountry> previousNodeCountry = MetadataUtils.getMetadataExtension(ssoDescriptor.getExtensions(), NodeCountry.class);
    if (previousNodeCountry.isPresent()) {
      ssoDescriptor.getExtensions().getUnknownXMLObjects().remove(previousNodeCountry.get());
      if (nodeCountry == null) {
        if (ssoDescriptor.getExtensions().getUnknownXMLObjects().isEmpty()) {
          ssoDescriptor.setExtensions(null);
        }
        return;
      }
    }
    NodeCountry nc = ObjectUtils.createSamlObject(NodeCountry.class);
    nc.setNodeCountry(nodeCountry);
    ssoDescriptor.getExtensions().getUnknownXMLObjects().add(nc);
  }

  /**
   * Assigns the eIDAS application identifier entity attribute.
   * 
   * @param applicationIdentifier
   *          the application identifier
   */
  public void setApplicationIdentifierEntityAttribute(String applicationIdentifier) {
    if (!StringUtils.hasText(applicationIdentifier)) {
      return;
    }
    this.addEntityAttributeExtension(
      APPLICATION_IDENTIFIER_TEMPLATE.createBuilder().value(applicationIdentifier).build());
  }

  /**
   * Assigns the version, or versions, of the eIDAS specification that this SP supports as an entity attribute.
   * 
   * @param versions
   *          a comma-separated string of versions, e.g., "1.1,1.2"
   */
  public void setProtocolVersionEntityAttributes(String versions) {
    if (!StringUtils.hasText(versions)) {
      return;
    }
    String[] v = versions.split(",");    
    this.addEntityAttributeExtension(
      PROTOCOL_VERSION_TEMPLATE.createBuilder().value(Arrays.asList(v)).build());
  }

  /**
   * Adds an attribute to the {@code mdattr:EntityAttributes} element that is part of the metadata extension element.
   * 
   * @param attribute
   *          the attribute to add
   */
  private void addEntityAttributeExtension(Attribute attribute) {
    if (this.builder().object().getExtensions() == null) {
      if (attribute == null) {
        return;
      }
      this.builder().object().setExtensions(ObjectUtils.createSamlObject(Extensions.class));
    }
    Optional<EntityAttributes> entityAttributes = MetadataUtils.getEntityAttributes(this.builder().object());
    if (!entityAttributes.isPresent()) {
      if (attribute == null) {
        return;
      }
      entityAttributes = Optional.of(ObjectUtils.createSamlObject(EntityAttributes.class));
      this.builder().object().getExtensions().getUnknownXMLObjects().add(entityAttributes.get());
    }
    try {
      entityAttributes.get().getAttributes().add(XMLObjectSupport.cloneXMLObject(attribute));
    }
    catch (MarshallingException | UnmarshallingException e) {
      throw new RuntimeException(e);
    }
  }

}
