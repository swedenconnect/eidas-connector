/*
 * Copyright 2017-2020 Sweden Connect
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
package se.elegnamnden.eidas.idp.statistics;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;
import org.opensaml.profile.context.ProfileRequestContext;

/**
 * Lookup function for {@code StatisticsContext} objects.
 * 
 * @author Martin Lindström (martin@litsec.se)
 */
@SuppressWarnings("rawtypes")
public class StatisticsContextLookup implements ContextDataLookupFunction<ProfileRequestContext, StatisticsContext> {

  /** {@inheritDoc} */
  @Override
  public @Nullable StatisticsContext apply(@Nullable ProfileRequestContext input) {
    return input != null ? input.getSubcontext(StatisticsContext.class, false) : null;
  }

}
