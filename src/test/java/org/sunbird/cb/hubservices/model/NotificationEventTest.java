package org.sunbird.cb.hubservices.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationEventTest {

    @Test
    void testDefaultValues() {
        NotificationEvent event = new NotificationEvent();
        
        assertNotNull(event.getIds());
        assertTrue(event.getIds().isEmpty());
        assertNotNull(event.getConfig());
        assertNotNull(event.getTemplate());
    }

    @Test
    void testGettersAndSetters() {
        NotificationEvent event = new NotificationEvent();
        List<String> ids = Arrays.asList("user1", "user2");
        NotificationConfig config = new NotificationConfig();
        NotificationTemplate template = new NotificationTemplate();
        
        event.setMode("email");
        event.setDeliveryType("immediate");
        event.setIds(ids);
        event.setConfig(config);
        event.setTemplate(template);
        
        assertEquals("email", event.getMode());
        assertEquals("immediate", event.getDeliveryType());
        assertEquals(ids, event.getIds());
        assertEquals(config, event.getConfig());
        assertEquals(template, event.getTemplate());
    }

    @Test
    void testSerializable() {
        NotificationEvent event = new NotificationEvent();
        assertNotNull(event);
        assertTrue(event instanceof java.io.Serializable);
    }
}