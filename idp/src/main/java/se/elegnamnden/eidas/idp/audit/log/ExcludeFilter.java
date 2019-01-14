/*
 * Copyright 2017-2019 Sweden Connect
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
package se.elegnamnden.eidas.idp.audit.log;

import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.MatchingFilter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Filter for excluding log entries.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class ExcludeFilter extends MatchingFilter {

  /** Text contents to match on. */
  private String contents;

  /** Exception to match on. */
  private String exception;

  /** Log level to match on. */
  private Level level;

  /** The logger to match on. */
  private String logger;

  /**
   * Constructor.
   */
  public ExcludeFilter() {

    // If everything matches we should exclude the entry -> DENY.
    this.setOnMatch("DENY");

    // If we don't have a match we just let the entry through -> NEUTRAL.
    this.setOnMismatch("NEUTRAL");
  }

  /** {@inheritDoc} */
  @Override
  public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
    if (!this.isStarted()) {
      return FilterReply.NEUTRAL;
    }

    FilterReply reply = this.onMismatch;

    if (this.level != null) {
      if (!this.level.equals(level)) {
        return this.onMismatch;
      }
      reply = this.onMatch;
    }
    
    if (this.logger != null && logger != null) {
      if (!this.logger.equals(logger.getName())) {
        return this.onMismatch;
      }
      reply = this.onMatch;
    }

    if (this.exception != null) {
      if (t != null && t.getClass().getName().equals(this.exception)) {
        reply = this.onMatch;
      }
      else {
        return this.onMismatch;
      }
    }

    if (this.contents != null) {
      if (format != null && format.contains(this.contents)) {
        return this.onMatch;
      }
      if (params != null) {
        for (Object o : params) {
          if (String.class.isAssignableFrom(o.getClass())) {
            if (String.class.cast(o).contains(this.contents)) {
              return this.onMatch;
            }
          }
        }
      }
      if (t != null && t.getMessage() != null) {
        if (t.getMessage().contains(this.contents)) {
          return this.onMatch;
        }
      }
      if (t != null && t.getCause() != null && t.getCause().getMessage() != null) {
        if (t.getCause().getMessage().contains(this.contents)) {
          return this.onMatch;
        }
      }
      return this.onMismatch;
    }

    return reply;
  }

  /** {@inheritDoc} */
  @Override
  public void start() {
    if (this.contents != null || this.exception != null || this.logger != null) {
      if (this.getName() == null) {
        this.setName("custom-exclude-filter");
      }
      super.start();
    }
    else {
      this.addError("The contents or exception property must be set for filter" + this.getName());
    }
  }

  /**
   * Assigns the text contents to match on.
   * 
   * @param contents
   *          text contents
   */
  public void setContents(String contents) {
    this.contents = contents;
  }

  /**
   * Assigns the name of the exception to match on.
   * 
   * @param exception
   *          the exception name
   */
  public void setException(String exception) {
    this.exception = exception;
  }

  /**
   * Assigns the log level to match on.
   * 
   * @param level
   *          log level
   */
  public void setLevel(String level) {
    this.level = Level.toLevel(level, null);
  }

  /**
   * Assigns the logger to match on.
   * 
   * @param logger
   *          the logger
   */
  public void setLogger(String logger) {
    this.logger = logger;
  }

}
