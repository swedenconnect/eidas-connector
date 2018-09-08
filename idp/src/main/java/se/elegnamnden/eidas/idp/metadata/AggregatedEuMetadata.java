/*
 * Copyright 2017-2018 Sweden Connect
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
package se.elegnamnden.eidas.idp.metadata;

import java.util.Collection;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;

/**
 * An interface for the aggregated EU metadata.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public interface AggregatedEuMetadata {

  /**
   * Returns a collection of country code identifiers for all active eIDAS Proxy Service IdP:s found in the aggregated
   * metadata.
   * 
   * @return a collection of country codes
   */
  Collection<String> getCountries();

  /**
   * Returns the entity descriptor for the Proxy Service IdP for the given country.
   * 
   * @param country
   *          the country code
   * @return the {@code EntityDescriptor}, or {@code null} if no descriptor is found
   */
  EntityDescriptor getProxyServiceIdp(String country);

}
