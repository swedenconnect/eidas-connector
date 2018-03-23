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
package se.elegnamnden.eidas.idp.connector.sp;

import lombok.Data;
import net.shibboleth.utilities.java.support.annotation.Duration;

/**
 * Configuration settings for response and assertion validation.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
@Data
public class ResponseValidationSettings {
  
  /** The default allowed clock skew (in milliseconds) - 30 seconds. */
  public static final long DEFAULT_ALLOWED_CLOCK_SKEW = 30000L;
  
  /** The default age for a response message that we allow (in milliseconds) - 3 minutes. */
  public static final long DEFAULT_MAX_AGE_RESPONSE = 180000L;
  
  /** Default max session age (in milliseconds) - 1 hour. */
  public static final long DEFAULT_MAX_SESSION_AGE = 3600000L;

  /** The allowed clock skew (in milliseconds). */
  @Duration
  private long allowedClockSkew = DEFAULT_ALLOWED_CLOCK_SKEW;
  
  /** Maximum allowed "age" of a response message (in milliseconds). */
  @Duration
  private long maxAgeResponse;
  
  /** Maximum session age allowed for SSO. */
  @Duration
  private long maxSessionAge;
  
  /** Should validation be strict? Default is false. */
  private boolean strictValidation = false;
  
  /** Is signed assertions required? */
  private boolean requireSignedAssertions = false;
  
}
