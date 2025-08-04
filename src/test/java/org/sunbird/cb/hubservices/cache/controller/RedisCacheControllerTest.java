package org.sunbird.cb.hubservices.cache.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.sunbird.cb.hubservices.cache.service.RedisCacheService;
import org.sunbird.cb.hubservices.model.SBApiResponse;

class RedisCacheControllerTest {

    @InjectMocks
    private RedisCacheController redisCacheController;

    @Mock
    private RedisCacheService redisCacheService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDeleteCache() throws Exception {
        SBApiResponse mockResponse = new SBApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        when(redisCacheService.deleteCache()).thenReturn(mockResponse);

        ResponseEntity<?> response = redisCacheController.deleteCache();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(redisCacheService).deleteCache();
    }

    @Test
    void testGetKeys() throws Exception {
        SBApiResponse mockResponse = new SBApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        when(redisCacheService.getKeys()).thenReturn(mockResponse);

        ResponseEntity<?> response = redisCacheController.getKeys();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(redisCacheService).getKeys();
    }

    @Test
    void testGetKeysAndValues() throws Exception {
        SBApiResponse mockResponse = new SBApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        when(redisCacheService.getKeysAndValues()).thenReturn(mockResponse);

        ResponseEntity<?> response = redisCacheController.getKeysAndValues();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(redisCacheService).getKeysAndValues();
    }
}