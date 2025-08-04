package org.sunbird.cb.hubservices.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTemplateTest {

    @Test
    void testDefaultValues() {
        NotificationTemplate template = new NotificationTemplate();
        
        assertNotNull(template.getParams());
        assertTrue(template.getParams().isEmpty());
    }

    @Test
    void testGettersAndSetters() {
        NotificationTemplate template = new NotificationTemplate();
        Map<String, String> params = new HashMap<>();
        params.put("key1", "value1");
        
        template.setId("template123");
        template.setParams(params);
        
        assertEquals("template123", template.getId());
        assertEquals(params, template.getParams());
    }

    @Test
    void testSerializable() {
        NotificationTemplate template = new NotificationTemplate();
        assertNotNull(template);
        assertTrue(template instanceof java.io.Serializable);
    }
}