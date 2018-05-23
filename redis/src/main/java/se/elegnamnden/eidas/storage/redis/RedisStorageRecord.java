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

import java.io.Serializable;

import org.opensaml.storage.StorageRecord;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class RedisStorageRecord implements Serializable {

  /** For serializing. */
  private static final long serialVersionUID = 7755578981399817545L;
  
  /** The value. */
  @Getter
  private String value;
  
  /** Version field. */
  @Getter
  private long version;
  
  /** Expiration field. */
  @Getter
  private Long expiration;
  
  public RedisStorageRecord(String value) {
    this.value = value;
    this.version = 1L;
  }
  
  public RedisStorageRecord(String value, Long expiration) {
    this(value);
    this.expiration = expiration;
  }
  
  public StorageRecord<?> toStorageRecord() {
    return new _StorageRecord<>(this.value, this.version, this.expiration);
  }
  
  public void updateExpiration(Long expiration) {
    this.expiration = expiration;
    this.version++;
  }
  
  public void update(String value, Long expiration) {
    this.value = value;
    this.expiration = expiration;
    this.version++;
  }
  
  private static class _StorageRecord<Type> extends StorageRecord<Type> {

    public _StorageRecord(String val, long version, Long exp) {
      super(val, exp);
      this.setVersion(version);
    }
        
  }
  
}
