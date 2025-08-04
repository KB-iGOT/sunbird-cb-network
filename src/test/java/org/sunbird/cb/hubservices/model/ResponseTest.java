package org.sunbird.cb.hubservices.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTest {

    @Test
    void testDefaultValues() {
        Response response = new Response();
        
        assertNotNull(response.getResult());
        assertTrue(response.getResult().isEmpty());
    }

    @Test
    void testPutAndGet() {
        Response response = new Response();
        
        response.put("key1", "value1");
        response.put("key2", 123);
        
        assertEquals("value1", response.get("key1"));
        assertEquals(123, response.get("key2"));
        assertNull(response.get("nonexistent"));
    }

    @Test
    void testPutAll() {
        Response response = new Response();
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", "value2");
        
        response.putAll(data);
        
        assertEquals("value1", response.get("key1"));
        assertEquals("value2", response.get("key2"));
    }

    @Test
    void testContainsKey() {
        Response response = new Response();
        
        response.put("existingKey", "value");
        
        assertTrue(response.containsKey("existingKey"));
        assertFalse(response.containsKey("nonExistentKey"));
    }

    @Test
    void testSerializable() {
        Response response = new Response();
        assertNotNull(response);
        assertTrue(response instanceof java.io.Serializable);
    }
}