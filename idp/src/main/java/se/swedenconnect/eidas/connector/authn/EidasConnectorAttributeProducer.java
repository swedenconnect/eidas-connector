/*
 * Copyright 2023-2024 Sweden Connect
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
package se.swedenconnect.eidas.connector.authn;

import java.util.List;

import org.opensaml.saml.saml2.core.Attribute;

import se.swedenconnect.spring.saml.idp.attributes.release.AttributeProducer;
import se.swedenconnect.spring.saml.idp.authentication.Saml2UserAuthentication;

/**
 * Attribute producer for the Swedish eIDAS Connector.
 *
 * @author Martin Lindström
 */
public class EidasConnectorAttributeProducer implements AttributeProducer {

  public EidasConnectorAttributeProducer() {
  }

  @Override
  public List<Attribute> releaseAttributes(final Saml2UserAuthentication userAuthentication) {

    // TODO

    return null;
  }

}
