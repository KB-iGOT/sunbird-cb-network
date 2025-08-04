package org.sunbird.cb.hubservices.profile.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

class ProfileUpdateConsumerTest {

    @InjectMocks
    private ProfileUpdateConsumer profileUpdateConsumer;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private ConsumerRecord<String, String> consumerRecord;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUserProfileUpdateConsumer_ValidData() throws IOException {
        String validJson = "{\"userId\":\"user123\",\"name\":\"John Doe\"}";
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", "user123");
        userData.put("name", "John Doe");

        when(consumerRecord.topic()).thenReturn("user-profile-update");
        when(consumerRecord.value()).thenReturn(validJson);
        when(mapper.readValue(eq(validJson), any(TypeReference.class))).thenReturn(userData);

        assertDoesNotThrow(() -> profileUpdateConsumer.userProfileUpdateConsumer(consumerRecord));

        verify(mapper).readValue(eq(validJson), any(TypeReference.class));
    }

    @Test
    void testUserProfileUpdateConsumer_EmptyUserData() throws IOException {
        String validJson = "{}";
        Map<String, Object> emptyUserData = new HashMap<>();

        when(consumerRecord.topic()).thenReturn("user-profile-update");
        when(consumerRecord.value()).thenReturn(validJson);
        when(mapper.readValue(eq(validJson), any(TypeReference.class))).thenReturn(emptyUserData);

        assertDoesNotThrow(() -> profileUpdateConsumer.userProfileUpdateConsumer(consumerRecord));

        verify(mapper).readValue(eq(validJson), any(TypeReference.class));
    }

    @Test
    void testUserProfileUpdateConsumer_NullValue() throws IOException {
        when(consumerRecord.topic()).thenReturn("user-profile-update");
        when(consumerRecord.value()).thenReturn(null);

        assertDoesNotThrow(() -> profileUpdateConsumer.userProfileUpdateConsumer(consumerRecord));

        verify(mapper, never()).readValue(anyString(), any(TypeReference.class));
    }

    @Test
    void testUserProfileUpdateConsumer_EmptyValue() throws IOException {
        when(consumerRecord.topic()).thenReturn("user-profile-update");
        when(consumerRecord.value()).thenReturn("");

        assertDoesNotThrow(() -> profileUpdateConsumer.userProfileUpdateConsumer(consumerRecord));

        verify(mapper, never()).readValue(anyString(), any(TypeReference.class));
    }

    @Test
    void testUserProfileUpdateConsumer_BlankValue() throws IOException {
        when(consumerRecord.topic()).thenReturn("user-profile-update");
        when(consumerRecord.value()).thenReturn("   ");

        assertDoesNotThrow(() -> profileUpdateConsumer.userProfileUpdateConsumer(consumerRecord));

        verify(mapper, never()).readValue(anyString(), any(TypeReference.class));
    }

    @Test
    void testUserProfileUpdateConsumer_JsonParsingException() throws IOException {
        String invalidJson = "{invalid json}";

        when(consumerRecord.topic()).thenReturn("user-profile-update");
        when(consumerRecord.value()).thenReturn(invalidJson);
        when(mapper.readValue(eq(invalidJson), any(TypeReference.class)))
                .thenThrow(new IOException("Invalid JSON"));

        assertDoesNotThrow(() -> profileUpdateConsumer.userProfileUpdateConsumer(consumerRecord));

        verify(mapper).readValue(eq(invalidJson), any(TypeReference.class));
    }

    @Test
    void testUserProfileUpdateConsumer_RuntimeException() throws IOException {
        String validJson = "{\"userId\":\"user123\"}";

        when(consumerRecord.topic()).thenReturn("user-profile-update");
        when(consumerRecord.value()).thenReturn(validJson);
        when(mapper.readValue(eq(validJson), any(TypeReference.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        assertDoesNotThrow(() -> profileUpdateConsumer.userProfileUpdateConsumer(consumerRecord));

        verify(mapper).readValue(eq(validJson), any(TypeReference.class));
    }
}