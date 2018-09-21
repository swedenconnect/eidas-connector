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
package se.elegnamnden.eidas.idp.audit;

import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Function;
import com.google.common.base.Functions;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.profile.context.RelyingPartyContext;

/**
 * Function that reads out the transactionIdentifier attribute value (holding the Assertion ID from the foreign
 * assertion).
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class EidasAssertionIdAuditExtractor implements Function<ProfileRequestContext<?, ?>, String> {
  
  /** ID for the attribute. */
  public static final String TRANSACTION_IDENTIFIER = "transactionIdentifier";

  /** Lookup strategy for AttributeContext to read from. */
  @Nonnull
  private final Function<ProfileRequestContext<?, ?>, AttributeContext> attributeContextLookupStrategy;

  /**
   * Constructor.
   */
  public EidasAssertionIdAuditExtractor() {
    // Defaults to ProfileRequestContext -> RelyingPartyContext -> AttributeContext.
    this.attributeContextLookupStrategy = Functions.compose(new ChildContextLookup<>(AttributeContext.class),
      new ChildContextLookup<ProfileRequestContext<?, ?>, RelyingPartyContext>(RelyingPartyContext.class));
  }

  /** {@inheritDoc} */
  @Override
  public String apply(ProfileRequestContext<?, ?> input) {
    
    final AttributeContext attributeCtx = this.attributeContextLookupStrategy.apply(input);
    if (attributeCtx != null) {
      Map<String, IdPAttribute> attributes = attributeCtx.getIdPAttributes();
      if (attributes != null) {
        IdPAttribute attr = attributes.get(TRANSACTION_IDENTIFIER);
        if (attr != null) {
          return attr.getValues().stream().map(IdPAttributeValue::getDisplayValue).collect(Collectors.joining(","));
        }
      }      
    }
    
    return null;
  }

}
