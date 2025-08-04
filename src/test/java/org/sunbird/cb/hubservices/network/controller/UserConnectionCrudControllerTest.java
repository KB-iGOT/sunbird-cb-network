package org.sunbird.cb.hubservices.network.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.sunbird.cb.hubservices.model.ConnectionRequest;
import org.sunbird.cb.hubservices.model.Response;
import org.sunbird.cb.hubservices.model.SBApiResponse;
import org.sunbird.cb.hubservices.network.service.UserConnectionService;
import org.sunbird.cb.hubservices.util.Constants;

class UserConnectionCrudControllerTest {

    @InjectMocks
    private UserConnectionCrudController controller;

    @Mock
    private UserConnectionService userConnectionService;

    @Mock
    private Response mockResponse;

    @Mock
    private SBApiResponse mockSBApiResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAdd() {
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");
        request.setStatus("pending");
        
        when(mockResponse.get(Constants.STATUS)).thenReturn(HttpStatus.OK);
        when(userConnectionService.addUserConnection(request, Constants.ADD_OPERATION)).thenReturn(mockResponse);

        ResponseEntity<Response> result = controller.add(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
        verify(userConnectionService).addUserConnection(request, Constants.ADD_OPERATION);
    }

    @Test
    void testUpdate() {
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");
        request.setStatus("approved");
        
        when(mockResponse.get(Constants.STATUS)).thenReturn(HttpStatus.OK);
        when(userConnectionService.updateUserConnection(request)).thenReturn(mockResponse);

        ResponseEntity<Response> result = controller.update(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
        verify(userConnectionService).updateUserConnection(request);
    }

    @Test
    void testBlock() {
        String authToken = "token123";
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");
        request.setStatus("blocked");
        
        when(mockSBApiResponse.getResponseCode()).thenReturn(HttpStatus.OK);
        when(userConnectionService.blockUser(authToken, request)).thenReturn(mockSBApiResponse);

        ResponseEntity<SBApiResponse> result = controller.block(authToken, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockSBApiResponse, result.getBody());
        verify(userConnectionService).blockUser(authToken, request);
    }
}