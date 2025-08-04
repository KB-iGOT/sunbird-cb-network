package org.sunbird.cb.hubservices.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequestTest {

    @Test
    void testDefaultValues() {
        Request request = new Request();
        
        assertNotNull(request.getRequest());
        assertTrue(request.getRequest().isEmpty());
    }

    @Test
    void testGettersAndSetters() {
        Request request = new Request();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("key1", "value1");
        
        request.setId("req123");
        request.setRequest(requestMap);
        
        assertEquals("req123", request.getId());
        assertEquals(requestMap, request.getRequest());
    }
}