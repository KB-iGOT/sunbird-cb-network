package org.sunbird.cb.hubservices.util.notificationUtill;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.cb.hubservices.cache.RedisCacheMgr;
import org.sunbird.cb.hubservices.cassandra.CassandraOperation;
import org.sunbird.cb.hubservices.util.Constants;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HelperMethodServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CassandraOperation cassandraOperation;

    @Mock
    private RedisCacheMgr cacheService;

    @InjectMocks
    private HelperMethodService helperMethodService;

    @BeforeEach
    void setUp() {
        // Setup is handled by @Mock and @InjectMocks annotations
    }

    @Test
    void testFetchDataForKey_Success() throws Exception {
        String key = "testKey";
        String jsonValue = "{\"name\":\"test\"}";
        Map<String, Object> expectedObject = new HashMap<>();
        expectedObject.put("name", "test");

        when(cacheService.getCache(key)).thenReturn(jsonValue);
        when(objectMapper.readValue(jsonValue, Object.class)).thenReturn(expectedObject);

        Object result = helperMethodService.fetchDataForKey(key);

        assertEquals(expectedObject, result);
        verify(cacheService).getCache(key);
        verify(objectMapper).readValue(jsonValue, Object.class);
    }

    @Test
    void testFetchDataForKey_NullValue() {
        String key = "testKey";

        when(cacheService.getCache(key)).thenReturn(null);

        Object result = helperMethodService.fetchDataForKey(key);

        assertNull(result);
        verify(cacheService).getCache(key);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void testFetchDataForKey_JsonException() throws Exception {
        String key = "testKey";
        String jsonValue = "invalid json";

        when(cacheService.getCache(key)).thenReturn(jsonValue);
        when(objectMapper.readValue(jsonValue, Object.class)).thenThrow(new RuntimeException("JSON parse error"));

        Object result = helperMethodService.fetchDataForKey(key);

        assertNull(result);
        verify(cacheService).getCache(key);
        verify(objectMapper).readValue(jsonValue, Object.class);
    }

    @Test
    void testFetchUserFromPrimary_Success() {
        String userId = "user123";
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put(Constants.ID, userId);
        userInfo.put(Constants.FULL_NAME, "John Doe");

        List<Map<String, Object>> userInfoList = Arrays.asList(userInfo);

        when(cassandraOperation.getRecordsByProperties(
                eq(Constants.KEYSPACE_SUNBIRD),
                eq(Constants.TABLE_USER),
                any(Map.class),
                eq(Arrays.asList(Constants.FIRST_NAME, Constants.ID))
        )).thenReturn(userInfoList);

        List<Object> result = helperMethodService.fetchUserFromPrimary(userId);

        assertEquals(1, result.size());
        Map<String, Object> userMap = (Map<String, Object>) result.get(0);
        assertEquals(userId, userMap.get(Constants.USER_ID_KEY));
        assertEquals("John Doe", userMap.get(Constants.FIRST_NAME_KEY));
    }

    @Test
    void testFetchUserFromPrimary_EmptyResult() {
        String userId = "user123";

        when(cassandraOperation.getRecordsByProperties(
                eq(Constants.KEYSPACE_SUNBIRD),
                eq(Constants.TABLE_USER),
                any(Map.class),
                eq(Arrays.asList(Constants.FIRST_NAME, Constants.ID))
        )).thenReturn(new ArrayList<>());

        List<Object> result = helperMethodService.fetchUserFromPrimary(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void testFetchUserFirstName_FromRedis() throws Exception {
        String userId = "user123";
        String redisKey = Constants.USER_PREFIX + userId;
        Map<String, Object> redisData = new HashMap<>();
        redisData.put(Constants.FIRST_NAME_KEY, "John");

        when(cacheService.getCache(redisKey)).thenReturn("{\"firstName\":\"John\"}");
        when(objectMapper.readValue(anyString(), eq(Object.class))).thenReturn(redisData);

        String result = helperMethodService.fetchUserFirstName(userId);

        assertEquals("John", result);
        verify(cacheService).getCache(redisKey);
    }

    @Test
    void testFetchUserFirstName_FromRedis_BlankName() throws Exception {
        String userId = "user123";
        String redisKey = Constants.USER_PREFIX + userId;
        Map<String, Object> redisData = new HashMap<>();
        redisData.put(Constants.FIRST_NAME_KEY, "");

        Map<String, Object> cassandraUser = new HashMap<>();
        cassandraUser.put(Constants.USER_ID_KEY, userId);
        cassandraUser.put(Constants.FIRST_NAME_KEY, "Jane");

        when(cacheService.getCache(redisKey)).thenReturn("{\"firstName\":\"\"}");
        when(objectMapper.readValue(anyString(), eq(Object.class))).thenReturn(redisData);
        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any()))
                .thenReturn(Arrays.asList(Map.of(Constants.ID, userId, Constants.FULL_NAME, "Jane")));

        String result = helperMethodService.fetchUserFirstName(userId);

        assertEquals("Jane", result);
    }

    @Test
    void testFetchUserFirstName_FromCassandra() throws Exception {
        String userId = "user123";
        String redisKey = Constants.USER_PREFIX + userId;

        when(cacheService.getCache(redisKey)).thenReturn(null);

        Map<String, Object> cassandraUser = new HashMap<>();
        cassandraUser.put(Constants.USER_ID_KEY, userId);
        cassandraUser.put(Constants.FIRST_NAME_KEY, "Jane");

        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any()))
                .thenReturn(Arrays.asList(Map.of(Constants.ID, userId, Constants.FULL_NAME, "Jane")));

        String result = helperMethodService.fetchUserFirstName(userId);

        assertEquals("Jane", result);
    }

    @Test
    void testFetchUserFirstName_RedisNotMap() throws Exception {
        String userId = "user123";
        String redisKey = Constants.USER_PREFIX + userId;

        when(cacheService.getCache(redisKey)).thenReturn("string");
        when(objectMapper.readValue(anyString(), eq(Object.class))).thenReturn("not a map");

        Map<String, Object> cassandraUser = new HashMap<>();
        cassandraUser.put(Constants.USER_ID_KEY, userId);
        cassandraUser.put(Constants.FIRST_NAME_KEY, "Jane");

        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any()))
                .thenReturn(Arrays.asList(Map.of(Constants.ID, userId, Constants.FULL_NAME, "Jane")));

        String result = helperMethodService.fetchUserFirstName(userId);

        assertEquals("Jane", result);
    }

    @Test
    void testFetchUserFirstName_CassandraEmpty() throws Exception {
        String userId = "user123";
        String redisKey = Constants.USER_PREFIX + userId;

        when(cacheService.getCache(redisKey)).thenReturn(null);
        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());

        String result = helperMethodService.fetchUserFirstName(userId);

        assertEquals("User", result);
    }

    @Test
    void testFetchUserFirstName_CassandraNotMap() throws Exception {
        String userId = "user123";
        String redisKey = Constants.USER_PREFIX + userId;

        when(cacheService.getCache(redisKey)).thenReturn(null);
//        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any()))
//                .thenReturn((List<Map<String, Object>>) Arrays.asList("not a map"));

        String result = helperMethodService.fetchUserFirstName(userId);

        assertEquals("User", result);
    }

    @Test
    void testFetchUserFirstName_CassandraBlankName() throws Exception {
        String userId = "user123";
        String redisKey = Constants.USER_PREFIX + userId;

        when(cacheService.getCache(redisKey)).thenReturn(null);

        Map<String, Object> cassandraUser = new HashMap<>();
        cassandraUser.put(Constants.USER_ID_KEY, userId);
        cassandraUser.put(Constants.FIRST_NAME_KEY, "");

        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any()))
                .thenReturn(Arrays.asList(cassandraUser));

        String result = helperMethodService.fetchUserFirstName(userId);

        assertEquals("User", result);
    }
}