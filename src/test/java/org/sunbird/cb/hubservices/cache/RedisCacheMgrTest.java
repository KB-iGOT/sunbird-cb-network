package org.sunbird.cb.hubservices.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.sunbird.cb.hubservices.util.Constants;
import org.sunbird.cb.hubservices.util.NetworkServerProperties;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@RunWith(MockitoJUnitRunner.class)
class RedisCacheMgrTest {

    @InjectMocks
    private RedisCacheMgr redisCacheMgr;

    @Mock
    private JedisPool jedisPool;

    @Mock
    private Jedis jedis;

    @Mock
    private NetworkServerProperties properties;

    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(redisCacheMgr, "objectMapper", objectMapper);
    }

    @Test
    void testPostConstruct_WithTimeouts() {
        when(properties.getRedisUserListReadTimeOut()).thenReturn(123);
        when(properties.getRedisTimeout()).thenReturn("456");
        redisCacheMgr.properties = properties;
        redisCacheMgr.postConstruct();
        // No exception means success
    }

    @Test
    void testPostConstruct_WithoutTimeouts() {
        when(properties.getRedisUserListReadTimeOut()).thenReturn(123);
        when(properties.getRedisTimeout()).thenReturn("");
        redisCacheMgr.properties = properties;
        redisCacheMgr.postConstruct();
        // No exception means success
    }

    @Test
    void testPutCache_WithTtl_Success() throws Exception {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(objectMapper.writeValueAsString(any())).thenReturn("json");
//        doNothing().when(jedis).set(anyString(), anyString());
//        doNothing().when(jedis).expire(anyString(), anyInt());
        redisCacheMgr.putCache("key", Map.of("a", 1), 100);
        verify(jedis).set(Constants.REDIS_COMMON_KEY + "key", "json");
        verify(jedis).expire(Constants.REDIS_COMMON_KEY + "key", 100);
    }

    @Test
    void testPutCache_WithTtl_Exception() throws Exception {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("fail"){});
        redisCacheMgr.putCache("key", Map.of("a", 1), 100);
        // Should log error, not throw
    }

    @Test
    void testPutCache_WithoutTtl() throws Exception {
        // Should call putCache(key, object, cache_ttl)
        RedisCacheMgr spyMgr = spy(redisCacheMgr);
        doNothing().when(spyMgr).putCache(anyString(), any(), anyInt());
        spyMgr.putCache("key", Map.of("a", 1));
        verify(spyMgr).putCache(eq("key"), any(), anyInt());
    }

    @Test
    void testPutInQuestionCache_Success() throws Exception {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(objectMapper.writeValueAsString(any())).thenReturn("json");
//        doNothing().when(jedis).set(anyString(), anyString());
//        doNothing().when(jedis).expire(anyString(), anyInt());
        redisCacheMgr.putInQuestionCache("key", Map.of("a", 1));
        verify(jedis).set(Constants.REDIS_COMMON_KEY + "key", "json");
    }

    @Test
    void testPutInQuestionCache_Exception() throws Exception {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("fail"){});
        redisCacheMgr.putInQuestionCache("key", Map.of("a", 1));
        // Should log error, not throw
    }

    @Test
    void testPutStringInCache_WithTtl_Success() {
        when(jedisPool.getResource()).thenReturn(jedis);
//        doNothing().when(jedis).set(anyString(), anyString());
//        doNothing().when(jedis).expire(anyString(), anyInt());
        redisCacheMgr.putStringInCache("key", "value", 100);
        verify(jedis).set(Constants.REDIS_COMMON_KEY + "key", "value");
        verify(jedis).expire(Constants.REDIS_COMMON_KEY + "key", 100);
    }

    @Test
    void testPutStringInCache_WithTtl_Exception() {
        when(jedisPool.getResource()).thenReturn(jedis);
        doThrow(new RuntimeException("fail")).when(jedis).set(anyString(), anyString());
        redisCacheMgr.putStringInCache("key", "value", 100);
        // Should log error, not throw
    }

    @Test
    void testPutStringInCache_WithoutTtl() {
        RedisCacheMgr spyMgr = spy(redisCacheMgr);
        doNothing().when(spyMgr).putStringInCache(anyString(), anyString(), anyInt());
        spyMgr.putStringInCache("key", "value");
        verify(spyMgr).putStringInCache(eq("key"), eq("value"), anyInt());
    }

    @Test
    void testDeleteKeyByName_Success() {
        when(jedisPool.getResource()).thenReturn(jedis);
//        doNothing().when(jedis).del(anyString());
        boolean result = redisCacheMgr.deleteKeyByName("key");
        assertTrue(result);
        verify(jedis).del(Constants.REDIS_COMMON_KEY + "key");
    }

    @Test
    void testDeleteKeyByName_Exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));
        boolean result = redisCacheMgr.deleteKeyByName("key");
        assertFalse(result);
    }

    @Test
    void testDeleteAllCBExtKey_Success() {
        when(jedisPool.getResource()).thenReturn(jedis);
        Set<String> keys = new HashSet<>(Arrays.asList("k1", "k2"));
        when(jedis.keys(anyString())).thenReturn(keys);
//        doNothing().when(jedis).del(anyString());
        boolean result = redisCacheMgr.deleteAllCBExtKey();
        assertTrue(result);
        verify(jedis, times(2)).del(anyString());
    }

    @Test
    void testDeleteAllCBExtKey_Exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));
        boolean result = redisCacheMgr.deleteAllCBExtKey();
        assertFalse(result);
    }

    @Test
    void testGetCache_Success() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.get(anyString())).thenReturn("value");
        String result = redisCacheMgr.getCache("key");
        assertEquals("value", result);
        verify(jedis).get(Constants.REDIS_COMMON_KEY + "key");
    }

    @Test
    void testGetCache_Exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));
        String result = redisCacheMgr.getCache("key");
        assertNull(result);
    }

    @Test
    void testMget_Success() {
        when(jedisPool.getResource()).thenReturn(jedis);
        List<String> fields = Arrays.asList("f1", "f2");
        String[] keys = {Constants.REDIS_COMMON_KEY + Constants.QUESTION_ID + "f1", Constants.REDIS_COMMON_KEY + Constants.QUESTION_ID + "f2"};
        when(jedis.mget(keys)).thenReturn(Arrays.asList("v1", "v2"));
        List<String> result = redisCacheMgr.mget(fields);
        assertEquals(Arrays.asList("v1", "v2"), result);
    }

    @Test
    void testMget_Exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));
        List<String> result = redisCacheMgr.mget(Arrays.asList("f1", "f2"));
        assertNull(result);
    }

    @Test
    void testGetAllKeyNames_Success() {
        when(jedisPool.getResource()).thenReturn(jedis);
        Set<String> keys = new HashSet<>(Arrays.asList("k1", "k2"));
        when(jedis.keys(anyString())).thenReturn(keys);
        Set<String> result = redisCacheMgr.getAllKeyNames();
        assertEquals(keys, result);
    }

    @Test
    void testGetAllKeyNames_Exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));
        Set<String> result = redisCacheMgr.getAllKeyNames();
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllKeysAndValues_Success() {
        when(jedisPool.getResource()).thenReturn(jedis);
        Set<String> keys = new HashSet<>(Arrays.asList("k1", "k2"));
        when(jedis.keys(anyString())).thenReturn(keys);
        when(jedis.get("k1")).thenReturn("v1");
        when(jedis.get("k2")).thenReturn("v2");
        List<Map<String, Object>> result = redisCacheMgr.getAllKeysAndValues();
        assertEquals(1, result.size());
        Map<String, Object> map = result.get(0);
        assertEquals("v1", map.get("k1"));
        assertEquals("v2", map.get("k2"));
    }

    @Test
    void testGetAllKeysAndValues_EmptyKeys() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.keys(anyString())).thenReturn(Collections.emptySet());
        List<Map<String, Object>> result = redisCacheMgr.getAllKeysAndValues();
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllKeysAndValues_Exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("fail"));
        List<Map<String, Object>> result = redisCacheMgr.getAllKeysAndValues();
        assertTrue(result.isEmpty());
    }
}