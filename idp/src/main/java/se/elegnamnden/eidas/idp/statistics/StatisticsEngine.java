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

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Function;

import lombok.extern.slf4j.Slf4j;

/**
 * Engine for producing statistics entries.
 * 
 * @author Martin Lindström (martin@litsec.se)
 */
@Slf4j
public class StatisticsEngine {

  /** Logging instance. */
  private static final Logger statLogger = LoggerFactory.getLogger("EidasStatistics");

  /** Strategy that gives us the StatisticsContext. */
  @SuppressWarnings("rawtypes")
  private static Function<ProfileRequestContext, StatisticsContext> statisticsContextLookupStrategy = new StatisticsContextLookup();

  /** The JSON writer. */
  private ObjectWriter objectWriter;

  /**
   * Constructor.
   */
  public StatisticsEngine() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_NULL);
    this.objectWriter = mapper.writer();
  }

  /**
   * Gets the statistics entry for an event.
   * 
   * @param context
   *          the context
   * @return statistics entry
   */
  public StatisticsEntry event(final ProfileRequestContext<?, ?> context) {
    StatisticsContext statContext = statisticsContextLookupStrategy.apply(context);
    if (statContext == null) {
      statContext = new StatisticsContext(new StatisticsEntry());
      context.addSubcontext(statContext);
    }
    return statContext.getEntry();
  }

  /**
   * Commits the entry.
   * 
   * @param event
   *          the entry
   * @param type
   *          the type of event
   */
  public void commit(final StatisticsEntry event, final StatisticsEventType type) {
    event.type(type).timestamp();
    try {
      String json = this.objectWriter.writeValueAsString(event);
      statLogger.info(json);
    }
    catch (Exception e) {
      log.error("Failed to write statistics entry of {} - {}", event, e.getMessage(), e);
    }
  }

  /**
   * Commits an error event.
   * 
   * @param event
   *          the entry
   * @param type
   *          the type of event
   * @param errorCode
   *          the SAML error code
   * @param errorMessage
   *          the error message
   */
  public void commitError(final StatisticsEntry event, final StatisticsEventType type, final String errorCode, final String errorMessage) {
    event.setError(new StatisticsEntry.ErrorInformation(errorCode, errorMessage));
    this.commit(event, type);
  }

  /**
   * Commits an error event.
   * 
   * @param event
   *          the entry
   * @param type
   *          the type of event
   * @param status
   *          the SAML Status object
   */
  public void commitError(final StatisticsEntry event, final StatisticsEventType type, final Status status) {
    final String errorCode = status.getStatusCode().getStatusCode() != null
        ? status.getStatusCode().getStatusCode().getValue()
        : status.getStatusCode().getValue();

    event.setError(new StatisticsEntry.ErrorInformation(errorCode, status.getStatusMessage().getMessage()));
    this.commit(event, type);
  }

}
