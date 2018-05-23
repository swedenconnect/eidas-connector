/*
 * Copyright 2018 Litsec AB
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
package se.elegnamnden.eidas.storage.redis;

import org.opensaml.storage.StorageCapabilitiesEx;

/**
 * Capabilities for the Redis storage service.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class RedisStorageCapabilities implements StorageCapabilitiesEx {
  
  /** Redis supports up to 512 MB big values. But, let's set the size to 16MB. Should be more than enough. */
  public static final int MAX_VALUE_SIZE = 16 * 1024 * 1024;
  
  /** Redis supports up to 512 MB big key. But, let's set the size to 16MB. Should be more than enough. */
  public static final int MAX_KEY_SIZE = 16 * 1024 * 1024;

  /**
   * Returns {@value #MAX_KEY_SIZE} / 2.
   */
  @Override
  public int getContextSize() {
    return MAX_KEY_SIZE / 2;
  }

  /**
   * Returns {@value #MAX_KEY_SIZE} / 2.
   */  
  @Override
  public int getKeySize() {
    return MAX_KEY_SIZE / 2;
  }

  /**
   * Returns {@link #MAX_VALUE_SIZE}.
   */
  @Override
  public long getValueSize() {
    return MAX_VALUE_SIZE;
  }

  /**
   * Always returns {@code true}.
   */
  @Override
  public boolean isServerSide() {
    return true;
  }

  /**
   * Always returns {@code true}.
   */  
  @Override
  public boolean isClustered() {
    return true;
  }

}
