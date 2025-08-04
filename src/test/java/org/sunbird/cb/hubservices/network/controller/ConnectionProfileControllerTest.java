package org.sunbird.cb.hubservices.network.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.sunbird.cb.hubservices.model.MultiSearch;
import org.sunbird.cb.hubservices.model.Response;
import org.sunbird.cb.hubservices.model.SBApiResponse;
import org.sunbird.cb.hubservices.serviceimpl.ProfileService;
import org.sunbird.cb.hubservices.util.Constants;

class ConnectionProfileControllerTest {

    @InjectMocks
    private ConnectionProfileController controller;

    @Mock
    private ProfileService profileService;

    @Mock
    private Response mockResponse;

    @Mock
    private SBApiResponse mockSBApiResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindRecommendedConnections() {
        String userId = "user123";
        String[] includeSources = {"source1", "source2"};
        MultiSearch multiSearch = new MultiSearch();
        
        when(profileService.multiSearchProfiles(userId, multiSearch, includeSources)).thenReturn(mockResponse);

        ResponseEntity<Response> result = controller.findRecommendedConnections(userId, includeSources, multiSearch);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
        verify(profileService).multiSearchProfiles(userId, multiSearch, includeSources);
    }

    @Test
    void testFindSuggests() {
        String org = "org123";
        String userId = "user123";
        int pageSize = 25;
        int pageNo = 1;
        
        when(profileService.findCommonProfileV2(userId, pageNo, pageSize)).thenReturn(mockResponse);

        ResponseEntity<Response> result = controller.findSuggests(org, userId, pageSize, pageNo);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
        verify(profileService).findCommonProfileV2(userId, pageNo, pageSize);
    }

    @Test
    void testFindRequests() {
        String org = "org123";
        String userId = "user123";
        int pageSize = 25;
        int pageNo = 1;
        
        when(profileService.findProfileRequestedV2(userId, pageNo, pageSize, Constants.DIRECTION.OUT)).thenReturn(mockResponse);

        ResponseEntity<Response> result = controller.findRequests(org, userId, pageSize, pageNo);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
        verify(profileService).findProfileRequestedV2(userId, pageNo, pageSize, Constants.DIRECTION.OUT);
    }

    @Test
    void testFindRequestsRecieved() {
        String org = "org123";
        String userId = "user123";
        int pageSize = 25;
        int pageNo = 1;
        
        when(profileService.findProfileRequestedV2(userId, pageNo, pageSize, Constants.DIRECTION.IN)).thenReturn(mockResponse);

        ResponseEntity<Response> result = controller.findRequestsRecieved(org, userId, pageSize, pageNo);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
        verify(profileService).findProfileRequestedV2(userId, pageNo, pageSize, Constants.DIRECTION.IN);
    }

    @Test
    void testFindEstablished() {
        String org = "org123";
        String userId = "user123";
        int pageSize = 25;
        int pageNo = 1;
        
        when(profileService.findProfilesV2(userId, pageNo, pageSize)).thenReturn(mockResponse);

        ResponseEntity<Response> result = controller.findEstablished(org, userId, pageSize, pageNo);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
        verify(profileService).findProfilesV2(userId, pageNo, pageSize);
    }

    @Test
    void testGetRelationshipBetweenUsers() {
        String toUserId = "user456";
        String authToken = "token123";
        
        when(mockSBApiResponse.getResponseCode()).thenReturn(HttpStatus.OK);
        when(profileService.getRelationshipBetweenUsers(toUserId, authToken)).thenReturn(mockSBApiResponse);

        ResponseEntity<SBApiResponse> result = controller.getRelationshipBetweenUsers(toUserId, authToken);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockSBApiResponse, result.getBody());
        verify(profileService).getRelationshipBetweenUsers(toUserId, authToken);
    }

    @Test
    void testFindRecommendedConnectionsV2() {
        String authToken = "token123";
        Map<String, Object> request = new HashMap<>();
        request.put("size", 10);
        
        when(mockSBApiResponse.getResponseCode()).thenReturn(HttpStatus.OK);
        when(profileService.findRecommendations(authToken, request)).thenReturn(mockSBApiResponse);

        ResponseEntity<SBApiResponse> result = controller.findRecommendedConnectionsV2(authToken, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockSBApiResponse, result.getBody());
        verify(profileService).findRecommendations(authToken, request);
    }

    @Test
    void testFindRecommendedMentors() {
        String authToken = "token123";
        Map<String, Object> request = new HashMap<>();
        request.put("size", 10);
        
        when(mockSBApiResponse.getResponseCode()).thenReturn(HttpStatus.OK);
        when(profileService.findRecommendedMentors(authToken, request)).thenReturn(mockSBApiResponse);

        ResponseEntity<SBApiResponse> result = controller.findRecommendedMentors(authToken, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockSBApiResponse, result.getBody());
        verify(profileService).findRecommendedMentors(authToken, request);
    }

    @Test
    void testFindBlockedUsers() {
        String authToken = "token123";
        Map<String, Object> request = new HashMap<>();
        request.put("size", 10);
        
        when(mockSBApiResponse.getResponseCode()).thenReturn(HttpStatus.OK);
        when(profileService.findBlockedUsers(authToken, request)).thenReturn(mockSBApiResponse);

        ResponseEntity<SBApiResponse> result = controller.findBlockedUsers(authToken, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockSBApiResponse, result.getBody());
        verify(profileService).findBlockedUsers(authToken, request);
    }

    @Test
    void testFetchTotalConnectionsCountByStatus() {
        String authToken = "token123";
        Map<String, Object> request = new HashMap<>();
        request.put("status", "pending");
        
        when(mockSBApiResponse.getResponseCode()).thenReturn(HttpStatus.OK);
        when(profileService.fetchTotalConnectionsCountByStatus(authToken, request)).thenReturn(mockSBApiResponse);

        ResponseEntity<SBApiResponse> result = controller.fetchTotalConnectionsCountByStatus(authToken, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockSBApiResponse, result.getBody());
        verify(profileService).fetchTotalConnectionsCountByStatus(authToken, request);
    }

}