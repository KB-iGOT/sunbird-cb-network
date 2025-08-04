package org.sunbird.cb.hubservices.network.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.sunbird.cb.hubservices.cache.RedisCacheMgr;
import org.sunbird.cb.hubservices.common.auth.AccessTokenValidator;
import org.sunbird.cb.hubservices.model.ConnectionRequest;
import org.sunbird.cb.hubservices.model.Response;
import org.sunbird.cb.hubservices.model.SBApiResponse;
import org.sunbird.cb.hubservices.serviceimpl.ConnectionService;
import org.sunbird.cb.hubservices.util.Constants;

@RunWith(MockitoJUnitRunner.class)
class UserConnectionServiceImplTest {

    @InjectMocks
    private UserConnectionServiceImpl userConnectionService;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private AccessTokenValidator accessTokenValidator;

    @Mock
    private RedisCacheMgr redisCacheMgr;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testBlockUser_Success() {
        // Arrange
        String authToken = "valid-auth-token";
        ConnectionRequest request = new ConnectionRequest();
        request.setConnectionId("conn123");
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");

        SBApiResponse mockResponse = new SBApiResponse();
        mockResponse.setResponseCode(HttpStatus.CREATED);

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn("user1");
        when(connectionService.blockUser(any(ConnectionRequest.class), eq(authToken)))
            .thenReturn(mockResponse);

        // Act
        SBApiResponse result = userConnectionService.blockUser(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getResponseCode());
        assertEquals(Constants.Status.BLOCKED, request.getStatus());
        assertNotNull(request.getCreatedAt());
        
        verify(accessTokenValidator).fetchUserIdFromAccessToken(any(), any(SBApiResponse.class));
        verify(connectionService).blockUser(request, authToken);
    }

    @Test
    void testBlockUser_EmptyUserId() {
        // Arrange
        String authToken = "valid-auth-token";
        ConnectionRequest request = new ConnectionRequest();
        request.setConnectionId("conn123");
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn("");

        // Act
        SBApiResponse result = userConnectionService.blockUser(authToken, request);

        // Assert
        assertNotNull(result);
        verify(accessTokenValidator).fetchUserIdFromAccessToken(any(), any(SBApiResponse.class));
        verify(connectionService, never()).blockUser(any(), any());
    }

    @Test
    void testBlockUser_NullUserId() {
        // Arrange
        String authToken = "valid-auth-token";
        ConnectionRequest request = new ConnectionRequest();
        request.setConnectionId("conn123");
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(null);

        // Act
        SBApiResponse result = userConnectionService.blockUser(authToken, request);

        // Assert
        assertNotNull(result);
        verify(accessTokenValidator).fetchUserIdFromAccessToken(any(), any(SBApiResponse.class));
        verify(connectionService, never()).blockUser(any(), any());
    }

    @Test
    void testBlockUser_EmptyConnectionId() {
        // Arrange
        String authToken = "valid-auth-token";
        ConnectionRequest request = new ConnectionRequest();
        request.setConnectionId("");
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn("user1");

        // Act
        SBApiResponse result = userConnectionService.blockUser(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getResponseCode());
        assertEquals("Connection ID is required to block a user", result.getParams().getErrmsg());
        verify(connectionService, never()).blockUser(any(), any());
    }

    @Test
    void testBlockUser_NullConnectionId() {
        // Arrange
        String authToken = "valid-auth-token";
        ConnectionRequest request = new ConnectionRequest();
        request.setConnectionId(null);
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn("user1");

        // Act
        SBApiResponse result = userConnectionService.blockUser(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getResponseCode());
        assertEquals("Connection ID is required to block a user", result.getParams().getErrmsg());
        verify(connectionService, never()).blockUser(any(), any());
    }

    @Test
    void testBlockUser_EmptyUserIdTo() {
        // Arrange
        String authToken = "valid-auth-token";
        ConnectionRequest request = new ConnectionRequest();
        request.setConnectionId("conn123");
        request.setUserIdFrom("user1");
        request.setUserIdTo("");

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn("user1");

        // Act
        SBApiResponse result = userConnectionService.blockUser(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getResponseCode());
        assertEquals("User ID to block is required", result.getParams().getErrmsg());
        verify(connectionService, never()).blockUser(any(), any());
    }

    @Test
    void testBlockUser_NullUserIdTo() {
        // Arrange
        String authToken = "valid-auth-token";
        ConnectionRequest request = new ConnectionRequest();
        request.setConnectionId("conn123");
        request.setUserIdFrom("user1");
        request.setUserIdTo(null);

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn("user1");

        // Act
        SBApiResponse result = userConnectionService.blockUser(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getResponseCode());
        assertEquals("User ID to block is required", result.getParams().getErrmsg());
        verify(connectionService, never()).blockUser(any(), any());
    }

    @Test
    void testBlockUser_EmptyUserIdFrom() {
        // Arrange
        String authToken = "valid-auth-token";
        ConnectionRequest request = new ConnectionRequest();
        request.setConnectionId("conn123");
        request.setUserIdFrom("");
        request.setUserIdTo("user2");

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn("user1");

        // Act
        SBApiResponse result = userConnectionService.blockUser(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getResponseCode());
        assertEquals("User ID from is required", result.getParams().getErrmsg());
        verify(connectionService, never()).blockUser(any(), any());
    }

    @Test
    void testBlockUser_NullUserIdFrom() {
        // Arrange
        String authToken = "valid-auth-token";
        ConnectionRequest request = new ConnectionRequest();
        request.setConnectionId("conn123");
        request.setUserIdFrom(null);
        request.setUserIdTo("user2");

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn("user1");

        // Act
        SBApiResponse result = userConnectionService.blockUser(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getResponseCode());
        assertEquals("User ID from is required", result.getParams().getErrmsg());
        verify(connectionService, never()).blockUser(any(), any());
    }

    @Test
    void testBlockUser_SameUserIds() {
        // Arrange
        String authToken = "valid-auth-token";
        ConnectionRequest request = new ConnectionRequest();
        request.setConnectionId("conn123");
        request.setUserIdFrom("user1");
        request.setUserIdTo("user1");

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn("user1");

        // Act
        SBApiResponse result = userConnectionService.blockUser(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getResponseCode());
        assertEquals("User ID from and User ID to cannot be the same", result.getParams().getErrmsg());
        verify(connectionService, never()).blockUser(any(), any());
    }

    @Test
    void testBlockUser_Exception() {
        // Arrange
        String authToken = "valid-auth-token";
        ConnectionRequest request = new ConnectionRequest();
        request.setConnectionId("conn123");
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn("user1");
        when(connectionService.blockUser(any(ConnectionRequest.class), eq(authToken)))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        SBApiResponse result = userConnectionService.blockUser(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getResponseCode());
        assertEquals("Error while blocking the user", result.getParams().getErrmsg());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.toString(), result.getParams().getStatus());
    }

    @Test
    void testUpdateUserConnection_Approved() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setStatus(Constants.APPROVED);
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");

        Response mockResponse = new Response();
        mockResponse.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);

        when(connectionService.upsert(request, Constants.UPDATE_OPERATION))
            .thenReturn(mockResponse);

        // Act
        Response result = userConnectionService.updateUserConnection(request);

        // Assert
        assertNotNull(result);
        assertNotNull(request.getUpdatedAt());
        verify(connectionService).upsert(request, Constants.UPDATE_OPERATION);
        
        // Verify cache deletions for APPROVED status
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_RECIEVED + Constants.UNDER_SCORE + "user1");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_REQUESTED + Constants.UNDER_SCORE + "user2");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_ESTABLISHED + Constants.UNDER_SCORE + "user1");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_ESTABLISHED + Constants.UNDER_SCORE + "user2");
    }

    @Test
    void testUpdateUserConnection_Rejected() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setStatus(Constants.REJECTED);
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");

        Response mockResponse = new Response();
        mockResponse.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);

        when(connectionService.upsert(request, Constants.UPDATE_OPERATION))
            .thenReturn(mockResponse);

        // Act
        Response result = userConnectionService.updateUserConnection(request);

        // Assert
        assertNotNull(result);
        verify(connectionService).upsert(request, Constants.UPDATE_OPERATION);
        
        // Verify cache deletions for REJECTED status
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_RECIEVED + Constants.UNDER_SCORE + "user1");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_REQUESTED + Constants.UNDER_SCORE + "user2");
    }

    @Test
    void testUpdateUserConnection_Blocked() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setStatus(Constants.BLOCKED);
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");

        Response mockResponse = new Response();
        mockResponse.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);

        when(connectionService.upsert(request, Constants.UPDATE_OPERATION))
            .thenReturn(mockResponse);

        // Act
        Response result = userConnectionService.updateUserConnection(request);

        // Assert
        assertNotNull(result);
        verify(connectionService).upsert(request, Constants.UPDATE_OPERATION);
        
        // Verify cache deletions for BLOCKED status
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_ESTABLISHED + Constants.UNDER_SCORE + "user1");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_ESTABLISHED + Constants.UNDER_SCORE + "user2");
    }

    @Test
    void testUpdateUserConnection_Withdrawn() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setStatus(Constants.WITHDRAWN);
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");

        Response mockResponse = new Response();
        mockResponse.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);

        when(connectionService.upsert(request, Constants.UPDATE_OPERATION))
            .thenReturn(mockResponse);

        // Act
        Response result = userConnectionService.updateUserConnection(request);

        // Assert
        assertNotNull(result);
        verify(connectionService).upsert(request, Constants.UPDATE_OPERATION);
        
        // Verify cache deletions for WITHDRAWN status
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_REQUESTED + Constants.UNDER_SCORE + "user1");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_RECIEVED + Constants.UNDER_SCORE + "user2");
    }

    @Test
    void testUpdateUserConnection_Unblocked() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setStatus(Constants.UNBLOCKED);
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");

        Response mockResponse = new Response();
        mockResponse.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);

        when(connectionService.upsert(request, Constants.UPDATE_OPERATION))
            .thenReturn(mockResponse);

        // Act
        Response result = userConnectionService.updateUserConnection(request);

        // Assert
        assertNotNull(result);
        verify(connectionService).upsert(request, Constants.UPDATE_OPERATION);
        
        // Verify cache deletions for UNBLOCKED status
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS + Constants.UNDER_SCORE + Constants.BLOCKED_USERS + Constants.UNDER_SCORE + "user1");
    }

    @Test
    void testUpdateUserConnection_Removed() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setStatus(Constants.REMOVED);
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");

        Response mockResponse = new Response();
        mockResponse.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);

        when(connectionService.upsert(request, Constants.UPDATE_OPERATION))
            .thenReturn(mockResponse);

        // Act
        Response result = userConnectionService.updateUserConnection(request);

        // Assert
        assertNotNull(result);
        verify(connectionService).upsert(request, Constants.UPDATE_OPERATION);
        
        // Verify cache deletions for REMOVED status
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_ESTABLISHED + Constants.UNDER_SCORE + "user1");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_ESTABLISHED + Constants.UNDER_SCORE + "user2");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS + Constants.UNDER_SCORE + Constants.USERS + Constants.UNDER_SCORE + "user1");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS + Constants.UNDER_SCORE + Constants.MENTORS + Constants.UNDER_SCORE + "user1");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS + Constants.UNDER_SCORE + Constants.USERS + Constants.UNDER_SCORE + "user2");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS + Constants.UNDER_SCORE + Constants.MENTORS + Constants.UNDER_SCORE + "user2");
    }

    @Test
    void testUpdateUserConnection_UnknownStatus() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setStatus("UNKNOWN_STATUS");
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");

        Response mockResponse = new Response();
        mockResponse.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);

        when(connectionService.upsert(request, Constants.UPDATE_OPERATION))
            .thenReturn(mockResponse);

        // Act
        Response result = userConnectionService.updateUserConnection(request);

        // Assert
        assertNotNull(result);
        verify(connectionService).upsert(request, Constants.UPDATE_OPERATION);
        
        // Verify no cache deletions for unknown status
        verify(redisCacheMgr, never()).deleteKeyByName(anyString());
    }

    @Test
    void testUpdateUserConnection_NullStatus() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setStatus(null);
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");

        Response mockResponse = new Response();
        mockResponse.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);

        when(connectionService.upsert(request, Constants.UPDATE_OPERATION))
            .thenReturn(mockResponse);

        // Act
        Response result = userConnectionService.updateUserConnection(request);

        // Assert
        assertNotNull(result);
        verify(connectionService).upsert(request, Constants.UPDATE_OPERATION);
        
        // Verify no cache deletions for null status
        verify(redisCacheMgr, never()).deleteKeyByName(anyString());
    }

    @Test
    void testAddUserConnection() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");
        String addOperation = "add";

        Response mockResponse = new Response();
        mockResponse.put(Constants.ResponseStatus.STATUS, HttpStatus.CREATED);

        when(connectionService.upsert(request, Constants.ADD_OPERATION))
            .thenReturn(mockResponse);

        // Act
        Response result = userConnectionService.addUserConnection(request, addOperation);

        // Assert
        assertNotNull(result);
        assertEquals(Constants.Status.PENDING, request.getStatus());
        assertNotNull(request.getCreatedAt());
        verify(connectionService).upsert(request, Constants.ADD_OPERATION);
        
        // Verify cache deletions for add operation
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS + Constants.UNDER_SCORE + Constants.USERS + Constants.UNDER_SCORE + "user1");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS + Constants.UNDER_SCORE + Constants.MENTORS + Constants.UNDER_SCORE + "user1");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS + Constants.UNDER_SCORE + Constants.USERS + Constants.UNDER_SCORE + "user2");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS + Constants.UNDER_SCORE + Constants.MENTORS + Constants.UNDER_SCORE + "user2");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_REQUESTED + Constants.UNDER_SCORE + "user1");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_RECIEVED + Constants.UNDER_SCORE + "user2");
    }

    @Test
    void testAddUserConnection_WithNullAddOperation() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");
        String addOperation = null;

        Response mockResponse = new Response();
        mockResponse.put(Constants.ResponseStatus.STATUS, HttpStatus.CREATED);

        when(connectionService.upsert(request, Constants.ADD_OPERATION))
            .thenReturn(mockResponse);

        // Act
        Response result = userConnectionService.addUserConnection(request, addOperation);

        // Assert
        assertNotNull(result);
        assertEquals(Constants.Status.PENDING, request.getStatus());
        assertNotNull(request.getCreatedAt());
        verify(connectionService).upsert(request, Constants.ADD_OPERATION);
        
        // Verify cache deletions still occur
        verify(redisCacheMgr, times(6)).deleteKeyByName(anyString());
    }

    @Test
    void testAddUserConnection_WithEmptyAddOperation() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");
        String addOperation = "";

        Response mockResponse = new Response();
        mockResponse.put(Constants.ResponseStatus.STATUS, HttpStatus.CREATED);

        when(connectionService.upsert(request, Constants.ADD_OPERATION))
            .thenReturn(mockResponse);

        // Act
        Response result = userConnectionService.addUserConnection(request, addOperation);

        // Assert
        assertNotNull(result);
        assertEquals(Constants.Status.PENDING, request.getStatus());
        assertNotNull(request.getCreatedAt());
        verify(connectionService).upsert(request, Constants.ADD_OPERATION);
        
        // Verify cache deletions still occur
        verify(redisCacheMgr, times(6)).deleteKeyByName(anyString());
    }

    @Test
    void testUpdateUserConnection_WithNullUserIds() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setStatus(Constants.APPROVED);
        request.setUserIdFrom(null);
        request.setUserIdTo(null);

        Response mockResponse = new Response();
        mockResponse.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);

        when(connectionService.upsert(request, Constants.UPDATE_OPERATION))
            .thenReturn(mockResponse);

        // Act
        Response result = userConnectionService.updateUserConnection(request);

        // Assert
        assertNotNull(result);
        verify(connectionService).upsert(request, Constants.UPDATE_OPERATION);
        
        // Verify cache deletions still occur (with null values)
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_RECIEVED + Constants.UNDER_SCORE + null);
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_REQUESTED + Constants.UNDER_SCORE + null);
//        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_ESTABLISHED + Constants.UNDER_SCORE + null);
    }

    @Test
    void testAddUserConnection_WithNullUserIds() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom(null);
        request.setUserIdTo(null);
        String addOperation = "add";

        Response mockResponse = new Response();
        mockResponse.put(Constants.ResponseStatus.STATUS, HttpStatus.CREATED);

        when(connectionService.upsert(request, Constants.ADD_OPERATION))
            .thenReturn(mockResponse);

        // Act
        Response result = userConnectionService.addUserConnection(request, addOperation);

        // Assert
        assertNotNull(result);
        assertEquals(Constants.Status.PENDING, request.getStatus());
        assertNotNull(request.getCreatedAt());
        verify(connectionService).upsert(request, Constants.ADD_OPERATION);
        
        // Verify cache deletions still occur (with null values)
        verify(redisCacheMgr, times(6)).deleteKeyByName(anyString());
    }

    @Test
    void testUpdateUserConnection_WithEmptyUserIds() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setStatus(Constants.APPROVED);
        request.setUserIdFrom("");
        request.setUserIdTo("");

        Response mockResponse = new Response();
        mockResponse.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);

        when(connectionService.upsert(request, Constants.UPDATE_OPERATION))
            .thenReturn(mockResponse);

        // Act
        Response result = userConnectionService.updateUserConnection(request);

        // Assert
        assertNotNull(result);
        verify(connectionService).upsert(request, Constants.UPDATE_OPERATION);
        
        // Verify cache deletions still occur (with empty values)
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_RECIEVED + Constants.UNDER_SCORE + "");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_REQUESTED + Constants.UNDER_SCORE + "");
//        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_ESTABLISHED + Constants.UNDER_SCORE + "");
    }

    @Test
    void testAddUserConnection_WithEmptyUserIds() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("");
        request.setUserIdTo("");
        String addOperation = "add";

        Response mockResponse = new Response();
        mockResponse.put(Constants.ResponseStatus.STATUS, HttpStatus.CREATED);

        when(connectionService.upsert(request, Constants.ADD_OPERATION))
            .thenReturn(mockResponse);

        // Act
        Response result = userConnectionService.addUserConnection(request, addOperation);

        // Assert
        assertNotNull(result);
        assertEquals(Constants.Status.PENDING, request.getStatus());
        assertNotNull(request.getCreatedAt());
        verify(connectionService).upsert(request, Constants.ADD_OPERATION);
        
        // Verify cache deletions still occur (with empty values)
        verify(redisCacheMgr, times(6)).deleteKeyByName(anyString());
    }

    @Test
    void testUpdateUserConnection_WithCaseInsensitiveStatus() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setStatus("approved"); // lowercase
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");

        Response mockResponse = new Response();
        mockResponse.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);

        when(connectionService.upsert(request, Constants.UPDATE_OPERATION))
            .thenReturn(mockResponse);

        // Act
        Response result = userConnectionService.updateUserConnection(request);

        // Assert
        assertNotNull(result);
        verify(connectionService).upsert(request, Constants.UPDATE_OPERATION);
        
        // Verify cache deletions for APPROVED status (case insensitive)
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_RECIEVED + Constants.UNDER_SCORE + "user1");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_REQUESTED + Constants.UNDER_SCORE + "user2");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_ESTABLISHED + Constants.UNDER_SCORE + "user1");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_ESTABLISHED + Constants.UNDER_SCORE + "user2");
    }

    @Test
    void testUpdateUserConnection_WithMixedCaseStatus() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setStatus("ReJeCtEd"); // mixed case
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");

        Response mockResponse = new Response();
        mockResponse.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);

        when(connectionService.upsert(request, Constants.UPDATE_OPERATION))
            .thenReturn(mockResponse);

        // Act
        Response result = userConnectionService.updateUserConnection(request);

        // Assert
        assertNotNull(result);
        verify(connectionService).upsert(request, Constants.UPDATE_OPERATION);
        
        // Verify cache deletions for REJECTED status (case insensitive)
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_RECIEVED + Constants.UNDER_SCORE + "user1");
        verify(redisCacheMgr).deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_REQUESTED + Constants.UNDER_SCORE + "user2");
    }
} 