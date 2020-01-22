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

import org.opensaml.messaging.context.BaseContext;

/**
 * Context for storing statistics information during an authentication.
 * 
 * @author Martin Lindström (martin@litsec.se)
 */
public class StatisticsContext extends BaseContext {

  /** The statistics entry. */
  private final StatisticsEntry entry;

  /**
   * Constructor.
   * 
   * @param entry
   *          the statistics entry.
   */
  public StatisticsContext(final StatisticsEntry entry) {
    this.entry = entry;
  }

  /**
   * Returns the statistics entry of the context.
   * 
   * @return the statistics entry
   */
  public StatisticsEntry getEntry() {
    return this.entry;
  }

}
