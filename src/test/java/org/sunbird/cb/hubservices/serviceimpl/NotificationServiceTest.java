package org.sunbird.cb.hubservices.serviceimpl;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.cb.hubservices.model.NotificationEvent;
import org.sunbird.cb.hubservices.util.ConnectionProperties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private ObjectMapper mapper;
    @Mock private ConnectionProperties connectionProperties;
    @Mock private ProfileService profileService;

    @Mock private JsonNode dataNode;
    @Mock private JsonNode personalDetailsNode;

    @InjectMocks
    private NotificationService notificationService;
    @Test
    void testBuildEvent_NullParameters_ReturnsNullMode() {
        NotificationEvent result = notificationService.buildEvent(null, "sender", "recipient", "status");
        assertNotNull(result);
        assertNull(result.getMode());
    }

    @Test
    void testBuildEvent_NullSender_ReturnsNullMode() {
        NotificationEvent result = notificationService.buildEvent("request", null, "recipient", "status");
        assertNotNull(result);
        assertNull(result.getMode());
    }

    @Test
    void testBuildEvent_NullRecipient_ReturnsNullMode() {
        NotificationEvent result = notificationService.buildEvent("request", "sender", null, "status");
        assertNotNull(result);
        assertNull(result.getMode());
    }
}
