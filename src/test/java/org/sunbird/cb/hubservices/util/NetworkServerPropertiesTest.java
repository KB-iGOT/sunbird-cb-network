package org.sunbird.cb.hubservices.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NetworkServerPropertiesTest {

    private NetworkServerProperties networkServerProperties;

    @BeforeEach
    void setUp() {
        networkServerProperties = new NetworkServerProperties();
    }

    @Test
    void testAllGettersAndSetters() {
        networkServerProperties.setRedisHostName("localhost");
        assertEquals("localhost", networkServerProperties.getRedisHostName());

        networkServerProperties.setRedisPort("6379");
        assertEquals("6379", networkServerProperties.getRedisPort());

        networkServerProperties.setRedisTimeout("5000");
        assertEquals("5000", networkServerProperties.getRedisTimeout());

        networkServerProperties.setRedisUserListReadTimeOut(3000);
        assertEquals(3000, networkServerProperties.getRedisUserListReadTimeOut().intValue());

        networkServerProperties.setDefaultLimit(10);
        assertEquals(10, networkServerProperties.getDefaultLimit().intValue());

        networkServerProperties.setMaxLimit(100);
        assertEquals(100, networkServerProperties.getMaxLimit().intValue());
    }
}