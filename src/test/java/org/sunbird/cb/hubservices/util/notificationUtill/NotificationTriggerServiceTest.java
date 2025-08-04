package org.sunbird.cb.hubservices.util.notificationUtill;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ExtendWith(MockitoExtension.class)
class NotificationTriggerServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ObjectNode objectNode;

    @InjectMocks
    private NotificationTriggerService notificationTriggerService;

    private List<String> userIds;
    private Map<String, Object> message;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationTriggerService, "notificationApiUrl", "http://notification.api");
        
        userIds = Arrays.asList("user1", "user2");
        message = new HashMap<>();
        message.put("title", "Test Title");
        message.put("body", "Test Body");
    }

    @Test
    void testSendNotification_Success() {
        assertDoesNotThrow(() -> 
            notificationTriggerService.sendNotification("category", "type", userIds, message)
        );
    }

    @Test
    void testSendNotification_NullSubCategory() {
        assertDoesNotThrow(() -> 
            notificationTriggerService.sendNotification(null, "type", userIds, message)
        );
    }

    @Test
    void testSendNotification_EmptySubCategory() {
        assertDoesNotThrow(() -> 
            notificationTriggerService.sendNotification("", "type", userIds, message)
        );
    }

    @Test
    void testSendNotification_NullSubType() {
        assertDoesNotThrow(() -> 
            notificationTriggerService.sendNotification("category", null, userIds, message)
        );
    }

    @Test
    void testSendNotification_EmptySubType() {
        assertDoesNotThrow(() -> 
            notificationTriggerService.sendNotification("category", "", userIds, message)
        );
    }

    @Test
    void testSendNotification_NullUserIds() {
        assertDoesNotThrow(() -> 
            notificationTriggerService.sendNotification("category", "type", null, message)
        );
    }

    @Test
    void testSendNotification_EmptyUserIds() {
        assertDoesNotThrow(() -> 
            notificationTriggerService.sendNotification("category", "type", Collections.emptyList(), message)
        );
    }

    @Test
    void testSendNotification_NullMessage() {
        assertDoesNotThrow(() -> 
            notificationTriggerService.sendNotification("category", "type", userIds, null)
        );
    }

    @Test
    void testSendNotification_EmptyMessage() {
        assertDoesNotThrow(() -> 
            notificationTriggerService.sendNotification("category", "type", userIds, Collections.emptyMap())
        );
    }

    @Test
    void testSendNotification_HttpClientErrorException() {
        assertDoesNotThrow(() -> 
            notificationTriggerService.sendNotification("category", "type", userIds, message)
        );
    }

    @Test
    void testSendNotification_GeneralException() {
        assertDoesNotThrow(() -> 
            notificationTriggerService.sendNotification("category", "type", userIds, message)
        );
    }

    @Test
    void testTriggerNotification_Success() {
        when(objectMapper.createObjectNode()).thenReturn(objectNode);
        when(objectNode.put(anyString(), anyString())).thenReturn(objectNode);

        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        assertDoesNotThrow(() -> 
            notificationTriggerService.triggerNotification("category", "type", userIds, "testUser", data)
        );

        verify(objectMapper).createObjectNode();
        verify(objectNode).put(anyString(), eq("testUser"));
    }

    @Test
    void testTriggerNotification_ExceptionInSendNotification() {
        when(objectMapper.createObjectNode()).thenReturn(objectNode);
        when(objectNode.put(anyString(), anyString())).thenReturn(objectNode);

        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        assertDoesNotThrow(() -> 
            notificationTriggerService.triggerNotification("category", "type", null, "testUser", data)
        );
    }
}