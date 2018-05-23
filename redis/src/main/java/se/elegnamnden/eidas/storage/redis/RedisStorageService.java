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

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.cryptacular.util.CodecUtil;
import org.cryptacular.util.HashUtil;
import org.opensaml.storage.StorageCapabilities;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageSerializer;
import org.opensaml.storage.StorageService;
import org.opensaml.storage.VersionMismatchException;
import org.opensaml.storage.annotation.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Shibboleth storage service based on Redis.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class RedisStorageService extends AbstractIdentifiableInitializableComponent implements StorageService {

  /** Our max size for keys. Redis has a 512 MB limit. */
  private static final int MAX_KEY_LENGTH = 4096;

  /** Logger instance. */
  private final Logger logger = LoggerFactory.getLogger(RedisStorageService.class);

  /** The Redis connection factory. */
  private RedisConnectionFactory connectionFactory;

  /** The template. */
  private RedisTemplate<String, RedisStorageRecord> template;

  /** Redis value operations. */
  private ValueOperations<String, RedisStorageRecord> valueOps;

  /**
   * Initializes the Redis connection.
   */
  @Override
  protected void doInitialize() throws ComponentInitializationException {
    super.doInitialize();

    if (this.connectionFactory == null) {
      throw new ComponentInitializationException("connectionFactory has not been initialized");
    }

    this.template = new RedisTemplate<>();
    template.setConnectionFactory(this.connectionFactory);
    template.setEnableDefaultSerializer(true);
    template.afterPropertiesSet();

    this.valueOps = this.template.opsForValue();
  }

  /** {@inheritDoc} */
  @Override
  public StorageCapabilities getCapabilities() {
    return new RedisStorageCapabilities();
  }

  /** {@inheritDoc} */
  @Override
  public boolean create(String context, String key, String value, Long expiration) throws IOException {
    Constraint.isNotNull(StringSupport.trimOrNull(context), "Context cannot be null or empty");
    Constraint.isNotNull(StringSupport.trimOrNull(key), "Key cannot be null or empty");
    Constraint.isNotNull(StringSupport.trimOrNull(value), "Value cannot be null or empty");

    Long timeout = timeout(expiration);
    String redisKey = this.redisKey(context, key);

    if (this.template.hasKey(redisKey)) {
      logger.debug("create: {} already exists", redisKey);
      return false;
    }
    if (timeout != null) {
      this.valueOps.set(redisKey, new RedisStorageRecord(value, expiration), timeout, TimeUnit.MILLISECONDS);
      logger.debug("create: Added {} with TTL '{}'", redisKey, timeout);
    }
    else {
      this.valueOps.set(redisKey, new RedisStorageRecord(value));
    }
    return true;
  }

  /** {@inheritDoc} */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public boolean create(String context, String key, Object value, StorageSerializer serializer, Long expiration) throws IOException {
    Constraint.isNotNull(serializer, "Serializer cannot be null");
    return this.create(context, key, serializer.serialize(value), expiration);
  }

  /** {@inheritDoc} */
  @Override
  public boolean create(Object value) throws IOException {
    Constraint.isNotNull(value, "Value cannot be null");
    return this.create(
      AnnotationSupport.getContext(value),
      AnnotationSupport.getKey(value),
      AnnotationSupport.getValue(value),
      AnnotationSupport.getExpiration(value));
  }

  /** {@inheritDoc} */
  @SuppressWarnings("rawtypes")
  @Override
  public StorageRecord read(String context, String key) throws IOException {
    Constraint.isNotNull(StringSupport.trimOrNull(context), "Context cannot be null or empty");
    Constraint.isNotNull(StringSupport.trimOrNull(key), "Key cannot be null or empty");

    final String redisKey = this.redisKey(context, key);
    RedisStorageRecord record = this.valueOps.get(redisKey);
    if (record == null) {
      logger.debug("read: No record found for {}", redisKey);
      return null;
    }
    logger.debug("read: Returning record for {}", redisKey);
    return record.toStorageRecord();
  }

  /** {@inheritDoc} */
  @Override
  public Object read(Object value) throws IOException {
    Constraint.isNotNull(value, "Value cannot be null");
    return this.read(AnnotationSupport.getContext(value), AnnotationSupport.getKey(value));
  }

  /** {@inheritDoc} */
  @SuppressWarnings("rawtypes")
  @Override
  public Pair<Long, StorageRecord> read(String context, String key, long version) throws IOException {
    Constraint.isGreaterThan(0, version, "Version must be positive");
    final StorageRecord record = this.read(context, key);
    if (record == null) {
      logger.debug("read: No record found for {}:{}", context, key);
      return new Pair<>();
    }
    final Pair<Long, StorageRecord> result = new Pair<>(record.getVersion(), null);
    if (version < record.getVersion()) {
      // Only set the record if the stored record is newer than 'version'
      result.setSecond(record);
      logger.debug("read: Found record for {}:{} with version {}", context, key, record.getVersion());
    }
    else {
      logger.debug("read: Found record for {}:{} does not have newer version than {}", context, key, version);
    }
    return result;
  }

  /** {@inheritDoc} */
  @Override
  public boolean update(String context, String key, String value, Long expiration) throws IOException {
    Constraint.isNotNull(StringSupport.trimOrNull(context), "Context cannot be null or empty");
    Constraint.isNotNull(StringSupport.trimOrNull(key), "Key cannot be null or empty");
    Constraint.isNotNull(StringSupport.trimOrNull(value), "Value cannot be null or empty");
    if (expiration != null) {
      Constraint.isGreaterThan(-1, expiration, "Expiration must be null or positive");
    }

    try {
      RedisStorageRecord record = this.updateInternal(context, key, value, expiration, null);
      if (record == null) {
        logger.debug("update: {}:{} does not exists", context, key);
        return false;
      }
      logger.debug("update: {}:{} updated", context, key);
      return true;
    }
    catch (VersionMismatchException e) {
      throw new IOException("Unexpected error", e);
    }
  }

  /** {@inheritDoc} */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public boolean update(String context, String key, Object value, StorageSerializer serializer, Long expiration) throws IOException {
    Constraint.isNotNull(serializer, "Serializer cannot be null");
    return update(context, key, serializer.serialize(value), expiration);
  }

  /** {@inheritDoc} */
  @Override
  public boolean update(Object value) throws IOException {
    Constraint.isNotNull(value, "Value cannot be null");
    return update(
      AnnotationSupport.getContext(value),
      AnnotationSupport.getKey(value),
      AnnotationSupport.getValue(value),
      AnnotationSupport.getExpiration(value));
  }

  /** {@inheritDoc} */
  @Override
  public Long updateWithVersion(long version, String context, String key, String value, Long expiration) throws IOException,
      VersionMismatchException {

    Constraint.isGreaterThan(0, version, "Version must be positive");
    Constraint.isNotNull(StringSupport.trimOrNull(context), "Context cannot be null or empty");
    Constraint.isNotNull(StringSupport.trimOrNull(key), "Key cannot be null or empty");
    Constraint.isNotNull(StringSupport.trimOrNull(value), "Value cannot be null or empty");
    if (expiration != null) {
      Constraint.isGreaterThan(-1, expiration, "Expiration must be null or positive");
    }

    RedisStorageRecord record = this.updateInternal(context, key, value, expiration, version);
    if (record != null) {
      logger.debug("updateWithVersion: Updated {}:{} with version {}", context, key, version);
      return record.getVersion();
    }
    else {
      logger.debug("updateWithVersion: {}:{} not found", context, key);
      return null;
    }
  }

  /** {@inheritDoc} */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public Long updateWithVersion(long version, String context, String key, Object value, StorageSerializer serializer, Long expiration)
      throws IOException, VersionMismatchException {
    Constraint.isNotNull(serializer, "Serializer cannot be null");
    return updateWithVersion(version, context, key, serializer.serialize(value), expiration);
  }

  /** {@inheritDoc} */
  @Override
  public Long updateWithVersion(long version, Object value) throws IOException, VersionMismatchException {
    Constraint.isNotNull(value, "Value cannot be null");
    return updateWithVersion(version,
      AnnotationSupport.getContext(value),
      AnnotationSupport.getKey(value),
      AnnotationSupport.getValue(value),
      AnnotationSupport.getExpiration(value));
  }

  /** {@inheritDoc} */
  @Override
  public boolean updateExpiration(String context, String key, Long expiration) throws IOException {
    Constraint.isNotNull(StringSupport.trimOrNull(context), "Context cannot be null or empty");
    Constraint.isNotNull(StringSupport.trimOrNull(key), "Key cannot be null or empty");
    if (expiration != null) {
      Constraint.isGreaterThan(-1, expiration, "Expiration must be null or positive");
    }

    try {
      RedisStorageRecord record = this.updateInternal(context, key, null, expiration, null);
      if (record != null) {
        logger.debug("updateExpiration: Updated {}:{}", context, key);
        return true;
      }
      else {
        logger.debug("updateExpiration: {}:{} not found", context, key);
        return false;
      }
    }
    catch (VersionMismatchException e) {
      throw new IOException("Unexpected error", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean updateExpiration(Object value) throws IOException {
    Constraint.isNotNull(value, "Value cannot be null");
    return updateExpiration(
      AnnotationSupport.getContext(value),
      AnnotationSupport.getKey(value),
      AnnotationSupport.getExpiration(value));
  }

  /**
   * Internal update method.
   * 
   * @param context
   *          the context
   * @param key
   *          the key
   * @param value
   *          the value (may be {@code null})
   * @param expiration
   *          the expiration (may be {@code null})
   * @param requiredVersion
   *          the required version (may be {@code null})
   * @return the updated record
   * @throws IOException
   *           for IO errors
   * @throws VersionMismatchException
   *           version mismatch
   */
  private RedisStorageRecord updateInternal(String context, String key, String value, Long expiration, Long requiredVersion)
      throws IOException, VersionMismatchException {

    final String redisKey = this.redisKey(context, key);

      RedisStorageRecord record = (RedisStorageRecord) this.valueOps.get(redisKey);
      if (record == null) {
        return null;
      }
      if (requiredVersion != null && record.getVersion() != requiredVersion) {
        throw new VersionMismatchException();
      }
      
      if (value == null) {
        record.updateExpiration(expiration);
      }
      else {
        record.update(value, expiration);
      }

      if (expiration != null) {
        this.valueOps.set(redisKey, record, timeout(expiration), TimeUnit.MILLISECONDS);
      }
      else {
        this.valueOps.set(redisKey, record);
      }
      
      return record;
  }

  /** {@inheritDoc} */
  @Override
  public boolean delete(String context, String key) throws IOException {
    Constraint.isNotNull(StringSupport.trimOrNull(context), "Context cannot be null or empty");
    Constraint.isNotNull(StringSupport.trimOrNull(key), "Key cannot be null or empty");
    
    final String redisKey = redisKey(context, key);
    if (this.template.hasKey(redisKey)) {
      this.template.delete(redisKey);
      logger.debug("delete: {} deleted", redisKey);
      return true;
    }
    
    logger.debug("delete: {} was not found", redisKey);
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean delete(Object value) throws IOException {
    Constraint.isNotNull(value, "Value cannot be null");
    return delete(AnnotationSupport.getContext(value), AnnotationSupport.getKey(value));
  }

  /** {@inheritDoc} */
  @SuppressWarnings("rawtypes")
  @Override
  public boolean deleteWithVersion(long version, String context, String key) throws IOException, VersionMismatchException {
    Constraint.isGreaterThan(0, version, "Version must be positive");
    Constraint.isNotNull(StringSupport.trimOrNull(context), "Context cannot be null or empty");
    Constraint.isNotNull(StringSupport.trimOrNull(key), "Key cannot be null or empty");

    StorageRecord record = this.read(context, key);
    if (record == null) {
      logger.debug("deleteWithVersion: {}:{} was not found", context, key);
      return false;
    }
    if (record.getVersion() != version) {
      logger.debug("deleteWithVersion: {}:{} has a greater version than {}", context, key, version);
      throw new VersionMismatchException();
    }
    return this.delete(context, key);
  }

  /** {@inheritDoc} */
  @Override
  public boolean deleteWithVersion(long version, Object value) throws IOException, VersionMismatchException {
    Constraint.isNotNull(value, "Value cannot be null");
    return deleteWithVersion(version, AnnotationSupport.getContext(value), AnnotationSupport.getKey(value));
  }

  /**
   * Does nothing.
   */
  @Override
  public void reap(String context) throws IOException {
    return;
  }

  /** {@inheritDoc} */
  @Override
  public void updateContextExpiration(String context, Long expiration) throws IOException {
    Constraint.isNotNull(StringSupport.trimOrNull(context), "Context cannot be null or empty");
    if (expiration != null) {
      Constraint.isGreaterThan(-1, expiration, "Expiration must be null or positive");
    }

    Set<String> keys = this.template.keys(this.contextPattern(context));
    for (String key : keys) {
      try {
        String _key = key.substring(context.length() + 1);
        this.updateExpiration(context, _key, expiration);
      }
      catch (Exception e) {
        logger.warn("updateContextExpiration: Failed to update expiration for {}", key);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void deleteContext(String context) throws IOException {
    Constraint.isNotNull(StringSupport.trimOrNull(context), "Context cannot be null or empty");

    Set<String> keys = this.template.keys(this.contextPattern(context));
    if (!keys.isEmpty()) {
      logger.debug("deleteContext: Deleting {}", keys);
      this.template.delete(keys);
    }
  }

  /**
   * Assigns the Redis connection factory.
   * 
   * @param connectionFactory
   *          connection factory
   */
  public void setConnectionFactory(RedisConnectionFactory connectionFactory) {
    ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
    this.connectionFactory = connectionFactory;
  }

  /**
   * Creates a Redis key from a context and a key.
   *
   * @param context
   *          the context
   * @param key
   *          the key
   *
   * @return key comprised of 250 characters or less
   */
  private String redisKey(String context, String key) {
    if (context.length() + key.length() + 1 > MAX_KEY_LENGTH) {
      String hk = CodecUtil.hex(HashUtil.sha1(key));
      if (context.length() + hk.length() + 1 > MAX_KEY_LENGTH) {
        return CodecUtil.hex(HashUtil.sha1(context)) + ":" + hk;
      }
      else {
        return context + ":" + hk;
      }
    }
    else {
      return context + ":" + key;
    }
  }

  private String contextPattern(String context) {
    if (context.length() + 40 + 1 > MAX_KEY_LENGTH) {
      return CodecUtil.hex(HashUtil.sha1(context)) + "\\:*";
    }
    else {
      return context + "\\:*";
    }
  }

  /**
   * Based on a expiration time that is given in millis since epoch, the method calculates the lifetime for an entry.
   * 
   * @param expiration
   *          the expiration time
   * @return the lifetime in millis or {@code null}
   * @throws IOException
   *           if the expiration time already has passed
   */
  private static Long timeout(Long expiration) throws IOException {
    if (expiration == null) {
      return null;
    }
    Constraint.isGreaterThan(-1, expiration, "Expiration must be null or positive");
    long now = System.currentTimeMillis();

    if (now > expiration) {
      throw new IOException("Expiration time is less than current time");
    }
    return expiration - now;
  }

}
