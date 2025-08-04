package org.sunbird.cb.hubservices.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationConfigTest {

    @Test
    void testGettersAndSetters() {
        NotificationConfig config = new NotificationConfig();
        
        config.setSender("test@example.com");
        config.setSubject("Test Subject");
        
        assertEquals("test@example.com", config.getSender());
        assertEquals("Test Subject", config.getSubject());
    }

    @Test
    void testSerializable() {
        NotificationConfig config = new NotificationConfig();
        assertNotNull(config);
        assertTrue(config instanceof java.io.Serializable);
    }
}