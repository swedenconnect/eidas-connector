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
package se.elegnamnden.eidas.idp.audit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.profile.context.RelyingPartyContext;

/**
 * {@link Function} that returns the attribute IDs and values from an {@link AttributeContext}.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public class AttributeValuesAuditExtractor implements Function<ProfileRequestContext<?, ?>, Collection<String>> {

  public static final String HIDDEN_VALUE = "*hidden*";

  /** Extract the unfiltered attribute list instead of the filtered list. */
  private boolean useUnfiltered;

  /** Tells whether the attribute values should be included in the audit log. */
  private boolean includeAttributeValues = true;

  /** A predicate to control whether attributes should be extracted for logging. */
  @Nullable
  private Predicate<ProfileRequestContext<?,?>> activationCondition;
  
  /** List of attribute ID:s of attributes that are "secret". The logger will not log the value of a secret attribute. */
  @Nullable
  private List<String> secretAttributes;

  /** Lookup strategy for AttributeContext to read from. */
  @Nonnull
  private final Function<ProfileRequestContext<?,?>, AttributeContext> attributeContextLookupStrategy;

  /**
   * Constructor.
   */
  public AttributeValuesAuditExtractor() {
    // Defaults to ProfileRequestContext -> RelyingPartyContext -> AttributeContext.
    this.attributeContextLookupStrategy = Functions.compose(new ChildContextLookup<>(AttributeContext.class),
        new ChildContextLookup<ProfileRequestContext<?,?>, RelyingPartyContext>(RelyingPartyContext.class));
  }

  @Override
  public Collection<String> apply(ProfileRequestContext<?, ?> input) {

    if (this.activationCondition != null && !this.activationCondition.apply(input)) {
      return Collections.emptyList();
    }

    final AttributeContext attributeCtx = this.attributeContextLookupStrategy.apply(input);
    if (attributeCtx != null) {
      Map<String, IdPAttribute> attributes = this.useUnfiltered ? attributeCtx.getUnfilteredIdPAttributes() : attributeCtx.getIdPAttributes();
      if (!this.includeAttributeValues) {
        return attributes.keySet();
      }
      else {
        List<String> result = new ArrayList<String>();
        attributes.values().stream().map(a -> formatAuditEntry(a)).forEach(result::add);
        return result;
      }
    }
    else {
      return Collections.emptyList();
    }
  }
  
  private String formatAuditEntry(IdPAttribute attribute) {
    StringBuffer sb = new StringBuffer();
    for (IdPAttributeValue<?> value : attribute.getValues()) {
      if (sb.length() > 0) {
        sb.append(',');
      }
      if (this.secretAttributes != null && this.secretAttributes.contains(attribute.getId())) {
        sb.append(HIDDEN_VALUE);
      }
      else {
        sb.append(value.getDisplayValue());
      }
    }
    return String.format("%s=[%s]", attribute.getId(), sb);
  }

  /**
   * Set whether to extract the list of unfiltered attributes instead of the filtered attributes.
   * 
   * @param flag
   *          flag to set
   */
  public void setUseUnfiltered(final boolean flag) {
    this.useUnfiltered = flag;
  }

  /**
   * Set whether to include attribute values in the audit log. If set to {@code false} only the attribute IDs will be
   * included.
   * 
   * @param includeAttributeValues
   *          flag to set
   */
  public void setIncludeAttributeValues(boolean includeAttributeValues) {
    this.includeAttributeValues = includeAttributeValues;
  }

  /**
   * Set a condition to evaluate to control whether attributes are extracted for logging.
   * 
   * <p>
   * This is used primarily to prevent logging of attributes for profiles in which attributes may be resolved, but not
   * actually disclosed to a relying party.
   * </p>
   * 
   * @param condition
   *          condition to evaluate
   */
  public void setActivationCondition(@Nullable final Predicate<ProfileRequestContext<?,?>> condition) {
    this.activationCondition = condition;
  }

  /**
   * Assigns the list of attribute ID:s of attributes that are "secret". The logger will not log the value
   * of a secret attribute.
   * 
   * @param secretAttributes 
   *          a list of attribute ID:s
   */
  public void setSecretAttributes(@Nullable final List<String> secretAttributes) {
    this.secretAttributes = secretAttributes;
  }
}
