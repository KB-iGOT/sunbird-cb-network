package org.sunbird.cb.hubservices.model;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SBApiResponseTest {

    @Test
    void testDefaultConstructor() {
        SBApiResponse response = new SBApiResponse();
        
        assertEquals("v1", response.getVer());
        assertNotNull(response.getTs());
        assertNotNull(response.getParams());
        assertNotNull(response.getResult());
        assertTrue(response.getResult().isEmpty());
    }

    @Test
    void testParameterizedConstructor() {
        SBApiResponse response = new SBApiResponse("test123");
        
        assertEquals("test123", response.getId());
        assertEquals("v1", response.getVer());
        assertNotNull(response.getTs());
        assertNotNull(response.getParams());
    }

    @Test
    void testGettersAndSetters() {
        SBApiResponse response = new SBApiResponse();
        SunbirdApiRespParam params = new SunbirdApiRespParam();
        Map<String, Object> result = new HashMap<>();
        
        response.setId("api123");
        response.setVer("v2");
        response.setTs("2023-01-01");
        response.setParams(params);
        response.setResponseCode(HttpStatus.OK);
        response.setResult(result);
        
        assertEquals("api123", response.getId());
        assertEquals("v2", response.getVer());
        assertEquals("2023-01-01", response.getTs());
        assertEquals(params, response.getParams());
        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals(result, response.getResult());
    }

    @Test
    void testPutAndGet() {
        SBApiResponse response = new SBApiResponse();
        
        response.put("key1", "value1");
        response.put("key2", 123);
        
        assertEquals("value1", response.get("key1"));
        assertEquals(123, response.get("key2"));
        assertNull(response.get("nonexistent"));
    }

    @Test
    void testPutAll() {
        SBApiResponse response = new SBApiResponse();
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", "value2");
        
        response.putAll(data);
        
        assertEquals("value1", response.get("key1"));
        assertEquals("value2", response.get("key2"));
    }

    @Test
    void testContainsKey() {
        SBApiResponse response = new SBApiResponse();
        
        response.put("existingKey", "value");
        
        assertTrue(response.containsKey("existingKey"));
        assertFalse(response.containsKey("nonExistentKey"));
    }
}