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

import org.opensaml.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.util.Assert;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * A Spring factory bean for creating a {@link RedisStorageService}.
 * <p>
 * If {@link #setFallbackService(StorageService)} is called, the factory will fallback to use the supplied service in
 * case the Redis storage service fails to initialize.
 * </p>
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class RedisStorageServiceFactoryBean extends AbstractFactoryBean<StorageService> {

  /** Logger instance. */
  private final Logger logger = LoggerFactory.getLogger(RedisStorageServiceFactoryBean.class);

  /** The Redis connection factory. */
  private RedisConnectionFactory connectionFactory;

  /** The storage service ID. */
  private String serviceId;

  /** The fallback storage service to use if the Redis storage service is not working (can not be initialized). */
  private StorageService fallbackService;

  /** {@inheritDoc} */
  @Override
  public Class<?> getObjectType() {
    return StorageService.class;
  }

  /**
   * Creates a Redis storage service.
   * <p>
   * If this fails and a fallback service has been configured, this service will be returned instead.
   * </p>
   */
  @Override
  protected StorageService createInstance() throws Exception {
    Assert.notNull(this.connectionFactory, "connectionFactory must be assigned");

    RedisStorageService redisStorageService = new RedisStorageService();
    redisStorageService.setConnectionFactory(this.connectionFactory);
    redisStorageService.setId(this.serviceId != null ? this.serviceId : "redis-storage-service");

    try {
      logger.debug("Initializing RedisStorageService ...");
      redisStorageService.initialize();
      logger.debug("RedisStorageService successfully initialized");

      return redisStorageService;
    }
    catch (ComponentInitializationException e) {
      logger.error("ALERT: Redis storage service failed to initialize - {}", e.getMessage());

      if (this.fallbackService != null) {
        logger.warn("Will fallback to use storage service: {}", this.fallbackService.getClass().getSimpleName());
        return this.fallbackService;
      }
      else {
        throw e;
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void destroyInstance(StorageService instance) throws Exception {
    if (instance instanceof RedisStorageService) {
      ((RedisStorageService) instance).destroy();
    }
  }

  /**
   * Assigns the Redis connection factory.
   * 
   * @param connectionFactory
   *          connection factory
   */
  public void setConnectionFactory(RedisConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  /**
   * Assigns the storage service ID to use for the Redis storage service.
   * 
   * @param serviceId
   *          the storage ID
   */
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  /**
   * Assigns an optional "fallback" storage service that is to be used if the Redis storage service cannot be
   * initialized correctly.
   * 
   * @param fallbackService
   *          the fallback storage service
   */
  public void setFallbackService(StorageService fallbackService) {
    this.fallbackService = fallbackService;
  }

}
