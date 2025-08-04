package org.sunbird.cb.hubservices.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionRequestTest {

    @Test
    void testGettersAndSetters() {
        ConnectionRequest request = new ConnectionRequest();
        
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");
        request.setStatus("PENDING");
        request.setCreatedAt("2023-01-01");
        request.setUpdatedAt("2023-01-02");
        request.setConnectionId("conn123");
        
        assertEquals("user1", request.getUserIdFrom());
        assertEquals("user2", request.getUserIdTo());
        assertEquals("PENDING", request.getStatus());
        assertEquals("2023-01-01", request.getCreatedAt());
        assertEquals("2023-01-02", request.getUpdatedAt());
        assertEquals("conn123", request.getConnectionId());
    }
}