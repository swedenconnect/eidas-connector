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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.VersionMismatchException;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.embedded.RedisServer;

/**
 * Test cases for {@code RedisStorageService}.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class RedisStorageServiceTest {

  /** The embedded Redis server. */
  private RedisServer server;

  /** The storage service. */
  private RedisStorageService service;

  @Before
  public void setup() throws IOException, ComponentInitializationException {
    this.server = new RedisServer(Protocol.DEFAULT_PORT);
    this.server.start();

    JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
    jedisPoolConfig.setTestOnBorrow(true);
    jedisPoolConfig.setMaxTotal(10);

    JedisConnectionFactory connectionFactory = new JedisConnectionFactory(jedisPoolConfig);
    connectionFactory.setUsePool(true);
    connectionFactory.setHostName("localhost");
    connectionFactory.setPort(Protocol.DEFAULT_PORT);
    connectionFactory.afterPropertiesSet();

    this.service = new RedisStorageService();
    this.service.setId("redis-storage-service");
    this.service.setConnectionFactory(connectionFactory);
    this.service.initialize();
  }

  @After
  public void teardown() {
    if (this.server != null) {
      this.server.stop();
    }
    if (this.service != null) {
      this.service.destroy();
    }
  }

  @Test
  public void testCreateRead() throws IOException {

    boolean created = this.service.create("context1", "key1", "value1", null);
    Assert.assertTrue(created);

    created = this.service.create("context1", "key2", "value2", null);
    Assert.assertTrue(created);

    created = this.service.create("context1", "key2", "value22", null);
    Assert.assertFalse(created);

    StorageRecord<?> record = this.service.read("context1", "key1");
    Assert.assertNotNull(record);
    Assert.assertEquals("value1", record.getValue());
    Assert.assertEquals(1, record.getVersion());
    Assert.assertNull(record.getExpiration());

    record = this.service.read("context1", "key3");
    Assert.assertNull(record);

    record = this.service.read("context3", "key1");
    Assert.assertNull(record);
  }

  @Test
  public void testCreateExpire() throws IOException, InterruptedException {
    
    this.service.create("context", "key", "value", System.currentTimeMillis() + 10L);
    Thread.sleep(20L);

    StorageRecord<?> record = this.service.read("context", "key");
    Assert.assertNull(record);

    this.service.create("context", "key", "value", System.currentTimeMillis() + 1000L);
    record = this.service.read("context", "key");
    Assert.assertNotNull(record);
  }

  @Test
  public void testUpdate() throws IOException {

    this.service.create("ctx", "K", "V", null);
    boolean updated = this.service.update("ctx", "K", "V2", null);
    Assert.assertTrue(updated);

    StorageRecord<?> record = this.service.read("ctx", "K");
    Assert.assertNotNull(record);
    Assert.assertEquals("V2", record.getValue());
    Assert.assertEquals(2, record.getVersion());
  }

  @Test
  public void testUpdateExpire() throws IOException, InterruptedException {

    this.service.create("ctx", "K", "V", System.currentTimeMillis() + 10000L);
    boolean updated = this.service.update("ctx", "K", "V2", System.currentTimeMillis() + 10L);
    Assert.assertTrue(updated);

    Thread.sleep(20L);

    StorageRecord<?> record = this.service.read("ctx", "K");
    Assert.assertNull(record);
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void testReadVersion() throws IOException {

    this.service.create("ctx", "K", "V", null);
    Pair<Long, StorageRecord> pair = this.service.read("ctx", "K", 1L);
    Assert.assertTrue(1L == pair.getFirst());
    Assert.assertNull(pair.getSecond());

    this.service.update("ctx", "K", "V2", null);
    pair = this.service.read("ctx", "K", 1L);
    Assert.assertTrue(2L == pair.getFirst());
    Assert.assertNotNull(pair.getSecond());
  }
  
  @Test
  public void testUpdateWithVersion() throws IOException, VersionMismatchException {
    this.service.create("ctx", "K", "V", null);
    
    Long version = this.service.updateWithVersion(1L, "ctx", "K", "V2", null);
    Assert.assertTrue(version == 2);
    
    try {
      this.service.updateWithVersion(3L, "ctx", "K", "V2", null);
      Assert.fail("Expected VersionMismatchException");
    }
    catch (VersionMismatchException e) {      
    }
  }
  
  @Test
  public void testUpdateContextExpiration() throws IOException {
    this.service.create("ctx", "K1", "V1", null);
    this.service.create("ctx", "K2", "V2", null);
    this.service.create("ctx", "K3", "V3", null);
    this.service.create("CCC", "K", "V", null);
    
    Long expiration = System.currentTimeMillis() + 1000000L;
    
    this.service.updateContextExpiration("ctx", expiration);
    
    StorageRecord<?> record = this.service.read("ctx", "K1");
    Assert.assertEquals(expiration, record.getExpiration());
    Assert.assertEquals("V1", record.getValue());
    Assert.assertTrue(2 == record.getVersion());
    
    record = this.service.read("ctx", "K2");
    Assert.assertEquals(expiration, record.getExpiration());
    Assert.assertEquals("V2", record.getValue());
    Assert.assertTrue(2 == record.getVersion());
    
    record = this.service.read("ctx", "K3");
    Assert.assertEquals(expiration, record.getExpiration());
    Assert.assertEquals("V3", record.getValue());
    Assert.assertTrue(2 == record.getVersion());
    
    record = this.service.read("CCC", "K");
    Assert.assertNull(record.getExpiration());
    Assert.assertEquals("V", record.getValue());
    Assert.assertTrue(1 == record.getVersion());    
  }
  
  @Test
  public void testDelete() throws IOException {
    this.service.create("ctx", "K1", "V1", null);
    
    boolean result = this.service.delete("ctx", "K1");
    Assert.assertTrue(result);
    
    result = this.service.delete("ctx", "K1");
    Assert.assertFalse(result);
  }
  
  @Test
  public void testDeleteWithVersion() throws IOException, VersionMismatchException {
    this.service.create("ctx", "K1", "V1", null);
    this.service.update("ctx", "K1", "V2", null);
    
    try {
      this.service.deleteWithVersion(1, "ctx", "K1");
      Assert.fail("Expected VersionMismatchException");
    }
    catch (VersionMismatchException e) {
    }
    
    Assert.assertTrue(this.service.deleteWithVersion(2, "ctx", "K1"));
    Assert.assertFalse(this.service.deleteWithVersion(2, "ctx", "K1"));
  }

}
