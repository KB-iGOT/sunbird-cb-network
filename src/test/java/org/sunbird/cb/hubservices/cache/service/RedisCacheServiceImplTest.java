package org.sunbird.cb.hubservices.cache.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.sunbird.cb.hubservices.cache.RedisCacheMgr;
import org.sunbird.cb.hubservices.model.SBApiResponse;
import org.sunbird.cb.hubservices.util.Constants;

class RedisCacheServiceImplTest {

    @InjectMocks
    private RedisCacheServiceImpl redisCacheService;

    @Mock
    private RedisCacheMgr redisCache;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDeleteCache_Success() throws Exception {
        when(redisCache.deleteAllCBExtKey()).thenReturn(true);

        SBApiResponse response = redisCacheService.deleteCache();

        assertEquals(Constants.API_REDIS_DELETE, response.getId());
        assertEquals(Constants.SUCCESSFUL, response.getParams().getStatus());
        assertEquals(HttpStatus.OK, response.getResponseCode());
        verify(redisCache).deleteAllCBExtKey();
    }

    @Test
    void testDeleteCache_NoKeysFound() throws Exception {
        when(redisCache.deleteAllCBExtKey()).thenReturn(false);

        SBApiResponse response = redisCacheService.deleteCache();

        assertEquals(Constants.API_REDIS_DELETE, response.getId());
        assertEquals("No Keys found, Redis cache is empty", response.getParams().getErrmsg());
        assertEquals(HttpStatus.NOT_FOUND, response.getResponseCode());
        verify(redisCache).deleteAllCBExtKey();
    }

    @Test
    void testGetKeys_Success() throws Exception {
        Set<String> keys = new HashSet<>(Arrays.asList("key1", "key2"));
        when(redisCache.getAllKeyNames()).thenReturn(keys);

        SBApiResponse response = redisCacheService.getKeys();

        assertEquals(Constants.API_REDIS_GET_KEYS, response.getId());
        assertEquals(Constants.SUCCESSFUL, response.getParams().getStatus());
        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals(keys, response.get(Constants.RESPONSE));
        verify(redisCache).getAllKeyNames();
    }

    @Test
    void testGetKeys_EmptyKeys() throws Exception {
        when(redisCache.getAllKeyNames()).thenReturn(Collections.emptySet());

        SBApiResponse response = redisCacheService.getKeys();

        assertEquals(Constants.API_REDIS_GET_KEYS, response.getId());
        assertEquals("No Keys found, Redis cache is empty", response.getParams().getErrmsg());
        assertEquals(HttpStatus.NOT_FOUND, response.getResponseCode());
        verify(redisCache).getAllKeyNames();
    }

    @Test
    void testGetKeysAndValues_Success() throws Exception {
        Map<String, Object> keyValueMap = new HashMap<>();
        keyValueMap.put("key1", "value1");
        List<Map<String, Object>> result = Arrays.asList(keyValueMap);
        when(redisCache.getAllKeysAndValues()).thenReturn(result);

        SBApiResponse response = redisCacheService.getKeysAndValues();

        assertEquals(Constants.API_REDIS_GET_KEYS_VALUE_SET, response.getId());
        assertEquals(Constants.SUCCESSFUL, response.getParams().getStatus());
        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals(result, response.get(Constants.RESPONSE));
        verify(redisCache).getAllKeysAndValues();
    }

    @Test
    void testGetKeysAndValues_EmptyResult() throws Exception {
        when(redisCache.getAllKeysAndValues()).thenReturn(Collections.emptyList());

        SBApiResponse response = redisCacheService.getKeysAndValues();

        assertEquals(Constants.API_REDIS_GET_KEYS_VALUE_SET, response.getId());
        assertEquals("No Keys found, Redis cache is empty", response.getParams().getErrmsg());
        assertEquals(HttpStatus.NOT_FOUND, response.getResponseCode());
        verify(redisCache).getAllKeysAndValues();
    }
}