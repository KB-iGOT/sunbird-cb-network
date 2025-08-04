package org.sunbird.cb.hubservices.serviceimpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.sunbird.cb.hubservices.cache.RedisCacheMgr;
import org.sunbird.cb.hubservices.cassandra.CassandraOperation;
import org.sunbird.cb.hubservices.exception.ValidationException;
import org.sunbird.cb.hubservices.model.*;
import org.sunbird.cb.hubservices.service.INodeService;
import org.sunbird.cb.hubservices.util.ConnectionProperties;
import org.sunbird.cb.hubservices.util.Constants;
import org.sunbird.cb.hubservices.util.RequestHandlerServiceImpl;
import org.sunbird.cb.hubservices.util.notificationUtill.HelperMethodService;
import org.sunbird.cb.hubservices.util.notificationUtill.NotificationTriggerService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
class ConnectionServiceTest {

    @InjectMocks
    private ConnectionService connectionService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ConnectionProperties connectionProperties;

    @Mock
    private INodeService nodeService;

    @Mock
    private CassandraOperation cassandraOperation;

    @Mock
    private HelperMethodService helperMethodService;

    @Mock
    private NotificationTriggerService notificationTriggerService;

    @Mock
    private RedisCacheMgr redisCacheMgr;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RequestHandlerServiceImpl requestHandlerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testBlockUser_Success() throws Exception {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");
        request.setConnectionId("conn123");
        request.setStatus("Blocked");

        String authToken = "auth-token";

        Map<String, Object> mockReadData = new HashMap<>();
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> responseMap = new HashMap<>();
        
        List<Map<String, Object>> roles = Arrays.asList(
            Map.of("role", "teacher"),
            Map.of("role", "mentor")
        );
        responseMap.put(Constants.ROLES, roles);
        responseMap.put("rootOrgId", "org123");
        
        Map<String, Object> profileDetails = new HashMap<>();
        List<Map<String, Object>> professionalDetails = Arrays.asList(
            Map.of(Constants.DESIGNATION, "Senior Teacher")
        );
        profileDetails.put(Constants.PROFESSIONAL_DETAILS, professionalDetails);
        responseMap.put(Constants.PROFILE_DETAILS_KEY, profileDetails);
        
        resultMap.put(Constants.RESPONSE, responseMap);
        mockReadData.put(Constants.RESULT, resultMap);

        when(requestHandlerService.fetchUsingGetWithHeadersProfile(anyString(), anyMap()))
            .thenReturn(mockReadData);
        when(nodeService.connect(any(Node.class), any(Node.class), anyMap()))
            .thenReturn(true);

        // Act
        SBApiResponse response = connectionService.blockUser(request, authToken);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.get(Constants.ResponseStatus.STATUS));
        assertEquals(Constants.ResponseStatus.SUCCESSFUL, response.get(Constants.ResponseStatus.MESSAGE));
        
        verify(requestHandlerService, times(2)).fetchUsingGetWithHeadersProfile(anyString(), anyMap());
        verify(nodeService).connect(any(Node.class), any(Node.class), anyMap());
    }

    @Test
    void testBlockUser_NodeConnectionFails() throws Exception {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");
        request.setConnectionId("conn123");
        request.setStatus("Blocked");

        String authToken = "auth-token";

        Map<String, Object> mockReadData = new HashMap<>();
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put(Constants.ROLES, new ArrayList<>());
        responseMap.put("rootOrgId", "org123");
        resultMap.put(Constants.RESPONSE, responseMap);
        mockReadData.put(Constants.RESULT, resultMap);

        when(requestHandlerService.fetchUsingGetWithHeadersProfile(anyString(), anyMap()))
            .thenReturn(mockReadData);
        when(nodeService.connect(any(Node.class), any(Node.class), anyMap()))
            .thenReturn(false);

        // Act
        SBApiResponse response = connectionService.blockUser(request, authToken);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.get(Constants.ResponseStatus.STATUS));
    }

    @Test
    void testBlockUser_ExceptionOccurs() throws Exception {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");
        request.setConnectionId("conn123");
        request.setStatus("Blocked");

        String authToken = "auth-token";

        Map<String, Object> mockReadData = new HashMap<>();
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put(Constants.ROLES, new ArrayList<>());
        responseMap.put("rootOrgId", "org123");
        resultMap.put(Constants.RESPONSE, responseMap);
        mockReadData.put(Constants.RESULT, resultMap);

        when(requestHandlerService.fetchUsingGetWithHeadersProfile(anyString(), anyMap()))
            .thenReturn(mockReadData);
        when(nodeService.connect(any(Node.class), any(Node.class), anyMap()))
            .thenThrow(new RuntimeException("Connection failed"));

        // Act
        SBApiResponse response = connectionService.blockUser(request, authToken);

        // Assert
        assertNotNull(response);
        // Should still return a response even when exception occurs
    }

    @Test
    void testUpsert_ValidRequest_Success() throws Exception {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");
        request.setConnectionId("conn123");
        request.setStatus(Constants.Status.PENDING);
        request.setCreatedAt("2023-01-01");
        request.setUpdatedAt("2023-01-02");

        when(nodeService.connect(any(Node.class), any(Node.class), anyMap()))
            .thenReturn(true);
        when(helperMethodService.fetchUserFirstName(anyString()))
            .thenReturn("John");
        when(connectionProperties.isNotificationEnabled())
            .thenReturn(false);

        // Act
        Response response = connectionService.upsert(request, Constants.ADD_OPERATION);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.get(Constants.ResponseStatus.STATUS));
        assertEquals(Constants.ResponseStatus.SUCCESSFUL, response.get(Constants.ResponseStatus.MESSAGE));
        
        verify(notificationTriggerService).triggerNotification(
            eq(Constants.SEND_CONNECTION_REQUEST), 
            eq(Constants.ALERT), 
            anyList(), 
            eq("John"), 
            anyMap()
        );
    }

    @Test
    void testUpsert_UpdateOperation() throws Exception {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");
        request.setConnectionId("conn123");
        request.setStatus(Constants.Status.APPROVED);

        when(nodeService.connect(any(Node.class), any(Node.class), anyMap()))
            .thenReturn(true);
        when(helperMethodService.fetchUserFirstName(anyString()))
            .thenReturn("John");
        when(connectionProperties.isNotificationEnabled())
            .thenReturn(false);

        // Act
        Response response = connectionService.upsert(request, Constants.UPDATE_OPERATION);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.get(Constants.ResponseStatus.STATUS));
        
        verify(notificationTriggerService).triggerNotification(
            eq(Constants.ACCEPTED_CONNECTION_REQUEST), 
            eq(Constants.ALERT), 
            anyList(), 
            eq("John"), 
            anyMap()
        );
    }

    @Test
    void testUpsert_InvalidRequest() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("");
        request.setUserIdTo("user2");

        // Act
        Response response = connectionService.upsert(request, Constants.ADD_OPERATION);

        // Assert
        assertNotNull(response);
        // Should not process invalid request
    }

    @Test
    void testUpsert_NodeConnectionFails() throws Exception {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");
        request.setConnectionId("conn123");
        request.setStatus(Constants.Status.PENDING);

        when(nodeService.connect(any(Node.class), any(Node.class), anyMap()))
            .thenReturn(false);

        // Act
        Response response = connectionService.upsert(request, Constants.ADD_OPERATION);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.get(Constants.ResponseStatus.STATUS));
    }

    @Test
    void testUpsert_ValidationException() throws Exception {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");
        request.setConnectionId("conn123");
        request.setStatus(Constants.Status.PENDING);

        when(nodeService.connect(any(Node.class), any(Node.class), anyMap()))
            .thenThrow(new ValidationException("Validation failed"));

        // Act
        Response response = connectionService.upsert(request, Constants.ADD_OPERATION);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.get(Constants.ResponseStatus.STATUS));
    }

    @Test
    void testUpsert_GeneralException() throws Exception {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");
        request.setConnectionId("conn123");
        request.setStatus(Constants.Status.PENDING);

        when(nodeService.connect(any(Node.class), any(Node.class), anyMap()))
            .thenThrow(new RuntimeException("General error"));

        // Act
        Response response = connectionService.upsert(request, Constants.ADD_OPERATION);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.get(Constants.ResponseStatus.STATUS));
    }

    @Test
    void testUpsert_NotificationEnabled() throws Exception {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");
        request.setConnectionId("conn123");
        request.setStatus(Constants.Status.PENDING);

        when(nodeService.connect(any(Node.class), any(Node.class), anyMap()))
            .thenReturn(true);
        when(helperMethodService.fetchUserFirstName(anyString()))
            .thenReturn("John");
        when(connectionProperties.isNotificationEnabled())
            .thenReturn(true);
        when(connectionProperties.getNotificationTemplateRequest())
            .thenReturn("template123");

        // Act
        Response response = connectionService.upsert(request, Constants.ADD_OPERATION);

        // Assert
        assertNotNull(response);
    }

    @Test
    void testValidateRequest_ValidRequest() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("user2");

        // Act
        boolean result = connectionService.validateRequest(request);

        // Assert
        assertTrue(result);
    }

    @Test
    void testValidateRequest_EmptyUserIdFrom() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("");
        request.setUserIdTo("user2");

        // Act
        boolean result = connectionService.validateRequest(request);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateRequest_EmptyUserIdTo() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("");

        // Act
        boolean result = connectionService.validateRequest(request);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateRequest_SameUserIds() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setUserIdFrom("user1");
        request.setUserIdTo("user1");

        // Act
        boolean result = connectionService.validateRequest(request);

        // Assert
        assertFalse(result);
    }

    @Test
    void testSendNotification() {
        // Arrange
        String eventId = "event123";
        String sender = "user1";
        String recipient = "user2";
        String status = "pending";

        NotificationEvent mockEvent = new NotificationEvent();
        when(notificationService.buildEvent(eventId, sender, recipient, status))
            .thenReturn(mockEvent);

        // Act
        connectionService.sendNotification(eventId, sender, recipient, status);

        // Assert
        verify(notificationService).buildEvent(eventId, sender, recipient, status);
        verify(notificationService).postEvent(mockEvent);
    }

    @Test
    void testFindUserConnectionsV2_Success() throws Exception {
        // Arrange
        String userId = "user1";
        String status = "approved";

        List<Node> mockNodes = Arrays.asList(
            new Node("user2"),
            new Node("user3")
        );

        when(nodeService.getNodes(eq(userId), anyMap(), isNull(), eq(0), anyInt(), anyList()))
            .thenReturn(mockNodes);

        // Act
        List<String> result = connectionService.findUserConnectionsV2(userId, status);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("user2"));
        assertTrue(result.contains("user3"));
    }

    @Test
    void testFindSuggestedConnectionsV2_Success() {
        // Arrange
        String userId = "user1";
        int offset = 0;
        int limit = 10;

        List<Node> mockNodes = Arrays.asList(
            new Node("user2"),
            new Node("user3")
        );

        when(nodeService.getNodeNextLevel(eq(userId), anyMap(), eq(offset), eq(limit)))
            .thenReturn(mockNodes);
        when(nodeService.getNodes(eq(userId), anyMap(), isNull(), eq(0), anyInt(), anyList()))
            .thenReturn(Arrays.asList(new Node("user4")));

        // Act
        Response response = connectionService.findSuggestedConnectionsV2(userId, offset, limit);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.get(Constants.ResponseStatus.STATUS));
        assertEquals(Constants.ResponseStatus.SUCCESSFUL, response.get(Constants.ResponseStatus.MESSAGE));
    }

    @Test
    void testFindAllConnectionsIdsByStatusV2_Success() throws Exception {
        // Arrange
        String userId = "user1";
        String status = "approved";
        int offset = 0;
        int limit = 10;

        List<Node> mockNodes = Arrays.asList(
            new Node("user2"),
            new Node("user3")
        );

        Map<String, Integer> userCount = Map.of(Constants.COUNT, 2);

        when(nodeService.getNodes(eq(userId), anyMap(), isNull(), eq(offset), eq(limit), isNull()))
            .thenReturn(mockNodes);
        when(nodeService.getConnectionsCountByStatus(eq(userId), eq(Constants.Status.APPROVED), isNull()))
            .thenReturn(userCount);
        when(redisCacheMgr.getCache(anyString()))
            .thenReturn(null);

        // Act
        Response response = connectionService.findAllConnectionsIdsByStatusV2(userId, status, offset, limit);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.get(Constants.ResponseStatus.STATUS));
        assertEquals(Constants.ResponseStatus.SUCCESSFUL, response.get(Constants.ResponseStatus.MESSAGE));
        assertEquals(2, response.get(Constants.COUNT));
    }

    @Test
    void testFindAllConnectionsIdsByStatusV2_WithCache() throws Exception {
        // Arrange
        String userId = "user1";
        String status = "approved";
        int offset = 0;
        int limit = 10;

        List<Node> mockNodes = Arrays.asList(
            new Node("user2"),
            new Node("user3")
        );

        Map<String, Integer> userCount = Map.of(Constants.COUNT, 2);
        Collection<Node> cachedNodes = Arrays.asList(new Node("user4"));

        when(nodeService.getNodes(eq(userId), anyMap(), isNull(), eq(offset), eq(limit), isNull()))
            .thenReturn(mockNodes);
        when(nodeService.getConnectionsCountByStatus(eq(userId), eq(Constants.Status.APPROVED), isNull()))
            .thenReturn(userCount);
        when(redisCacheMgr.getCache(anyString()))
            .thenReturn("cached_data");
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
            .thenReturn(cachedNodes);

        // Act
        Response response = connectionService.findAllConnectionsIdsByStatusV2(userId, status, offset, limit);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.get(Constants.ResponseStatus.STATUS));
    }

    @Test
    void testFindConnectionsRequestedV2_OutDirection() throws Exception {
        // Arrange
        String userId = "user1";
        int offset = 0;
        int limit = 10;
        Constants.DIRECTION direction = Constants.DIRECTION.OUT;

        List<Node> mockNodes = Arrays.asList(
            new Node("user2"),
            new Node("user3")
        );

        Map<String, Integer> userCount = Map.of(Constants.COUNT, 2);

        when(nodeService.getNodes(eq(userId), anyMap(), eq(direction), eq(offset), eq(limit), isNull()))
            .thenReturn(mockNodes);
        when(nodeService.getConnectionsCountByStatus(eq(userId), eq(Constants.Status.PENDING), eq(direction)))
            .thenReturn(userCount);
        when(redisCacheMgr.getCache(anyString()))
            .thenReturn(null);

        // Act
        Response response = connectionService.findConnectionsRequestedV2(userId, offset, limit, direction);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.get(Constants.ResponseStatus.STATUS));
        assertEquals(Constants.ResponseStatus.SUCCESSFUL, response.get(Constants.ResponseStatus.MESSAGE));
    }

    @Test
    void testFindConnectionsRequestedV2_InDirection() throws Exception {
        // Arrange
        String userId = "user1";
        int offset = 0;
        int limit = 10;
        Constants.DIRECTION direction = Constants.DIRECTION.IN;

        List<Node> mockNodes = Arrays.asList(
            new Node("user2"),
            new Node("user3")
        );

        Map<String, Integer> userCount = Map.of(Constants.COUNT, 2);

        when(nodeService.getNodes(eq(userId), anyMap(), eq(direction), eq(offset), eq(limit), isNull()))
            .thenReturn(mockNodes);
        when(nodeService.getConnectionsCountByStatus(eq(userId), eq(Constants.Status.PENDING), eq(direction)))
            .thenReturn(userCount);
        when(redisCacheMgr.getCache(anyString()))
            .thenReturn(null);

        // Act
        Response response = connectionService.findConnectionsRequestedV2(userId, offset, limit, direction);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.get(Constants.ResponseStatus.STATUS));
    }

    @Test
    void testFindConnectionsRequestedV2_WithCache() throws Exception {
        // Arrange
        String userId = "user1";
        int offset = 0;
        int limit = 10;
        Constants.DIRECTION direction = Constants.DIRECTION.OUT;

        List<Node> mockNodes = Arrays.asList(
            new Node("user2"),
            new Node("user3")
        );

        Map<String, Integer> userCount = Map.of(Constants.COUNT, 2);
        Collection<Node> cachedNodes = Arrays.asList(new Node("user4"));

        when(nodeService.getNodes(eq(userId), anyMap(), eq(direction), eq(offset), eq(limit), isNull()))
            .thenReturn(mockNodes);
        when(nodeService.getConnectionsCountByStatus(eq(userId), eq(Constants.Status.PENDING), eq(direction)))
            .thenReturn(userCount);
        when(redisCacheMgr.getCache(anyString()))
            .thenReturn("cached_data");
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
            .thenReturn(cachedNodes);

        // Act
        Response response = connectionService.findConnectionsRequestedV2(userId, offset, limit, direction);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.get(Constants.ResponseStatus.STATUS));
    }

    @Test
    void testSetRelationshipProperties_WithDates() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setConnectionId("conn123");
        request.setStatus("pending");
        request.setCreatedAt("2023-01-01");
        request.setUpdatedAt("2023-01-02");

        Node from = new Node("user1");
        Node to = new Node("user2");

        // Act
        Map<String, String> result = connectionService.setRelationshipProperties(request, from, to);

        // Assert
        assertNotNull(result);
        assertEquals("conn123", result.get(Constants.Graph.CONNECTION_ID.getValue()));
        assertEquals("pending", result.get(Constants.Graph.STATUS.getValue()));
        assertEquals("2023-01-01", result.get(Constants.Graph.CREATED_AT.getValue()));
        assertEquals("2023-01-02", result.get(Constants.Graph.UPDATED_AT.getValue()));
        assertEquals("2023-01-01", from.getCreatedAt());
        assertEquals("2023-01-02", from.getUpdatedAt());
        assertEquals("2023-01-01", to.getCreatedAt());
        assertEquals("2023-01-02", to.getUpdatedAt());
    }

    @Test
    void testSetRelationshipProperties_WithoutDates() {
        // Arrange
        ConnectionRequest request = new ConnectionRequest();
        request.setConnectionId("conn123");
        request.setStatus("pending");

        Node from = new Node("user1");
        Node to = new Node("user2");

        // Act
        Map<String, String> result = connectionService.setRelationshipProperties(request, from, to);

        // Assert
        assertNotNull(result);
        assertEquals("conn123", result.get(Constants.Graph.CONNECTION_ID.getValue()));
        assertEquals("pending", result.get(Constants.Graph.STATUS.getValue()));
        assertNull(result.get(Constants.Graph.CREATED_AT.getValue()));
        assertNull(result.get(Constants.Graph.UPDATED_AT.getValue()));
    }

    @Test
    void testGetRelationshipBetweenUsers_Success() {
        // Arrange
        String fromUserId = "user1";
        String toUserId = "user2";
        Map<String, String> expectedRelationship = Map.of("status", "approved");

        when(nodeService.getRelationshipBetweenUsers(fromUserId, toUserId))
            .thenReturn(expectedRelationship);

        // Act
        Map<String, String> result = connectionService.getRelationshipBetweenUsers(fromUserId, toUserId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedRelationship, result);
    }

    @Test
    void testGetRelationshipBetweenUsers_Exception() {
        // Arrange
        String fromUserId = "user1";
        String toUserId = "user2";

        when(nodeService.getRelationshipBetweenUsers(fromUserId, toUserId))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        Map<String, String> result = connectionService.getRelationshipBetweenUsers(fromUserId, toUserId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindRecommendationForUser_Success() {
        // Arrange
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0, "limit", 10);
        List<Map<String, String>> expectedRecommendations = Arrays.asList(
            Map.of("userId", "user2", "score", "0.8"),
            Map.of("userId", "user3", "score", "0.6")
        );

        when(nodeService.findRecommendationForUser(userId, request))
            .thenReturn(expectedRecommendations);

        // Act
        List<Map<String, String>> result = connectionService.findRecommendationForUser(userId, request);

        // Assert
        assertNotNull(result);
        assertEquals(expectedRecommendations, result);
    }

    @Test
    void testFindRecommendationForUser_Exception() {
        // Arrange
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0, "limit", 10);

        when(nodeService.findRecommendationForUser(userId, request))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        List<Map<String, String>> result = connectionService.findRecommendationForUser(userId, request);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindRecommendationForMentors_Success() {
        // Arrange
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0, "limit", 10);
        List<Map<String, String>> expectedRecommendations = Arrays.asList(
            Map.of("userId", "mentor1", "score", "0.9"),
            Map.of("userId", "mentor2", "score", "0.7")
        );

        when(nodeService.findRecommendationForMentors(userId, request))
            .thenReturn(expectedRecommendations);

        // Act
        List<Map<String, String>> result = connectionService.findRecommendationForMentors(userId, request);

        // Assert
        assertNotNull(result);
        assertEquals(expectedRecommendations, result);
    }

    @Test
    void testFindRecommendationForMentors_Exception() {
        // Arrange
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0, "limit", 10);

        when(nodeService.findRecommendationForMentors(userId, request))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        List<Map<String, String>> result = connectionService.findRecommendationForMentors(userId, request);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindBlockedUsers_Success() {
        // Arrange
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0, "limit", 10);
        List<Map<String, String>> expectedBlockedUsers = Arrays.asList(
            Map.of("userId", "blocked1", "reason", "spam"),
            Map.of("userId", "blocked2", "reason", "inappropriate")
        );

        when(nodeService.findBlockedUsers(userId, request))
            .thenReturn(expectedBlockedUsers);

        // Act
        List<Map<String, String>> result = connectionService.findBlockedUsers(userId, request);

        // Assert
        assertNotNull(result);
        assertEquals(expectedBlockedUsers, result);
    }

    @Test
    void testFindBlockedUsers_Exception() {
        // Arrange
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0, "limit", 10);

        when(nodeService.findBlockedUsers(userId, request))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        List<Map<String, String>> result = connectionService.findBlockedUsers(userId, request);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetCountForRecommendedUsers_Success() {
        // Arrange
        String userId = "user1";
        Integer expectedCount = 5;

        when(nodeService.getCountForRecommendedUsers(userId))
            .thenReturn(expectedCount);

        // Act
        Integer result = connectionService.getCountForRecommendedUsers(userId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedCount, result);
    }

    @Test
    void testGetCountForRecommendedUsers_Exception() {
        // Arrange
        String userId = "user1";

        when(nodeService.getCountForRecommendedUsers(userId))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        Integer result = connectionService.getCountForRecommendedUsers(userId);

        // Assert
        assertNotNull(result);
    }

    @Test
    void testGetCountForRecommendedMentors_Success() {
        // Arrange
        String userId = "user1";
        Integer expectedCount = 3;

        when(nodeService.getCountForRecommendedMentors(userId))
            .thenReturn(expectedCount);

        // Act
        Integer result = connectionService.getCountForRecommendedMentors(userId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedCount, result);
    }

    @Test
    void testGetCountForRecommendedMentors_Exception() {
        // Arrange
        String userId = "user1";

        when(nodeService.getCountForRecommendedMentors(userId))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        Integer result = connectionService.getCountForRecommendedMentors(userId);

        // Assert
        assertNotNull(result);
    }

    @Test
    void testGetTotalCountForUsersBasedOnStatus_Success() {
        // Arrange
        String userId = "user1";
        List<String> statusList = Arrays.asList("pending", "approved");
        String facet = "department";
        List<Map<String, Object>> expectedCounts = Arrays.asList(
            Map.of("status", "pending", "count", 5),
            Map.of("status", "approved", "count", 10)
        );

        when(nodeService.getTotalCountForUsersBasedOnStatus(userId, statusList, facet))
            .thenReturn(expectedCounts);

        // Act
        List<Map<String, Object>> result = connectionService.getTotalCountForUsersBasedOnStatus(userId, statusList, facet);

        // Assert
        assertNotNull(result);
        assertEquals(expectedCounts, result);
    }

    @Test
    void testGetTotalCountForUsersBasedOnStatus_Exception() {
        // Arrange
        String userId = "user1";
        List<String> statusList = Arrays.asList("pending", "approved");
        String facet = "department";

        when(nodeService.getTotalCountForUsersBasedOnStatus(userId, statusList, facet))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        List<Map<String, Object>> result = connectionService.getTotalCountForUsersBasedOnStatus(userId, statusList, facet);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testUpdateUserProfileInNeo4j() {
        // Arrange
        Node node = new Node("user1");
        node.setFullName("John Doe");

        when(nodeService.updateUserProfileInNeo4j(node))
            .thenReturn(true);

        // Act
        boolean result = connectionService.updateUserProfileInNeo4j(node);

        // Assert
        assertTrue(result);
        verify(nodeService).updateUserProfileInNeo4j(node);
    }

    @Test
    void testEnrichUserInfo_WithValidUserData() throws Exception {
        // Arrange
        String userId = "user1";
        int offset = 0;
        int limit = 10;

        List<Node> mockNodes = Arrays.asList(
            new Node("user2"),
            new Node("user3")
        );

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put(Constants.ID, "user2");
        userInfo.put(Constants.STATUS, 1);
        userInfo.put(Constants.FULL_NAME, "John Doe");
        userInfo.put(Constants.CHANNEL, "Department A");
        userInfo.put(Constants.PROFILE_DETAILS, "{\"professionalDetails\":[{\"designation\":\"Teacher\"}],\"employmentDetails\":{\"org\":\"School\"},\"profileImageUrl\":\"image.jpg\",\"profileBannerUrl\":\"banner.jpg\"}");

        when(nodeService.getNodeNextLevel(eq(userId), anyMap(), eq(offset), eq(limit)))
            .thenReturn(mockNodes);
        when(nodeService.getNodes(eq(userId), anyMap(), isNull(), eq(0), anyInt(), anyList()))
            .thenReturn(Arrays.asList(new Node("user4")));
        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), anyList()))
            .thenReturn(Arrays.asList(userInfo));

        JsonNode mockJsonNode = mock(JsonNode.class);
        when(objectMapper.readTree(anyString())).thenReturn(mockJsonNode);
        when(mockJsonNode.hasNonNull(Constants.PROFESSIONAL_DETAILS)).thenReturn(true);
        when(mockJsonNode.hasNonNull(Constants.EMPLOYMENT_DETAILS)).thenReturn(true);
        when(mockJsonNode.hasNonNull(Constants.PROFILE_IMAGE_URL)).thenReturn(true);
        when(mockJsonNode.hasNonNull(Constants.PROFILE_BANNER_URL)).thenReturn(true);
        when(mockJsonNode.get(Constants.PROFESSIONAL_DETAILS)).thenReturn(mock(JsonNode.class));
        when(mockJsonNode.get(Constants.EMPLOYMENT_DETAILS)).thenReturn(mock(JsonNode.class));
        when(mockJsonNode.get(Constants.PROFILE_IMAGE_URL)).thenReturn(mock(JsonNode.class));
        when(mockJsonNode.get(Constants.PROFILE_BANNER_URL)).thenReturn(mock(JsonNode.class));
        when(mockJsonNode.asText()).thenReturn("test");

        // Act
        Response response = connectionService.findSuggestedConnectionsV2(userId, offset, limit);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.get(Constants.ResponseStatus.STATUS));
    }

    @Test
    void testEnrichUserInfo_WithInactiveUser() throws Exception {
        // Arrange
        String userId = "user1";
        int offset = 0;
        int limit = 10;

        List<Node> mockNodes = Arrays.asList(
            new Node("user2"),
            new Node("user3")
        );

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put(Constants.ID, "user2");
        userInfo.put(Constants.STATUS, 0); // Inactive user
        userInfo.put(Constants.FULL_NAME, "John Doe");

        when(nodeService.getNodeNextLevel(eq(userId), anyMap(), eq(offset), eq(limit)))
            .thenReturn(mockNodes);
        when(nodeService.getNodes(eq(userId), anyMap(), isNull(), eq(0), anyInt(), anyList()))
            .thenReturn(Arrays.asList(new Node("user4")));
        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), anyList()))
            .thenReturn(Arrays.asList(userInfo));

        // Act
        Response response = connectionService.findSuggestedConnectionsV2(userId, offset, limit);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.get(Constants.ResponseStatus.STATUS));
    }

    @Test
    void testEnrichUserInfo_ExceptionInCassandraOperation() throws Exception {
        // Arrange
        String userId = "user1";
        int offset = 0;
        int limit = 10;

        List<Node> mockNodes = Arrays.asList(
            new Node("user2"),
            new Node("user3")
        );

        when(nodeService.getNodeNextLevel(eq(userId), anyMap(), eq(offset), eq(limit)))
            .thenReturn(mockNodes);
        when(nodeService.getNodes(eq(userId), anyMap(), isNull(), eq(0), anyInt(), anyList()))
            .thenReturn(Arrays.asList(new Node("user4")));
        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), anyList()))
            .thenThrow(new RuntimeException("Cassandra error"));

        // Act
        Response response = connectionService.findSuggestedConnectionsV2(userId, offset, limit);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.get(Constants.ResponseStatus.STATUS));
    }
} 