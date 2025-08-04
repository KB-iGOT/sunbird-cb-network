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
import org.sunbird.cb.hubservices.dao.IGraphDao;
import org.sunbird.cb.hubservices.exception.GraphException;
import org.sunbird.cb.hubservices.exception.ValidationException;
import org.sunbird.cb.hubservices.model.Node;
import org.sunbird.cb.hubservices.util.Constants;

@RunWith(MockitoJUnitRunner.class)
class NodeServiceTest {

    @InjectMocks
    private NodeService nodeService;

    @Mock
    private IGraphDao graphDao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testConnect_Success() throws Exception {
        // Arrange
        Node from = new Node("user1");
        Node to = new Node("user2");
        Map<String, String> relationProperties = Map.of("status", "pending");

        when(graphDao.upsertNode(from)).thenReturn(true);
        when(graphDao.upsertNode(to)).thenReturn(true);
        when(graphDao.upsertRelation(from, to, relationProperties)).thenReturn(true);

        // Act
        Boolean result = nodeService.connect(from, to, relationProperties);

        // Assert
        assertTrue(result);
        verify(graphDao).upsertNode(from);
        verify(graphDao).upsertNode(to);
        verify(graphDao).upsertRelation(from, to, relationProperties);
    }

    @Test
    void testConnect_FromNodeNull() throws Exception {
        // Arrange
        Node from = null;
        Node to = new Node("user2");
        Map<String, String> relationProperties = Map.of("status", "pending");

        // Act
        Boolean result = nodeService.connect(from, to, relationProperties);

        // Assert
        assertFalse(result);
        verify(graphDao, never()).upsertNode(any());
        verify(graphDao, never()).upsertRelation(any(), any(), any());
    }

    @Test
    void testConnect_ToNodeNull() throws Exception {
        // Arrange
        Node from = new Node("user1");
        Node to = null;
        Map<String, String> relationProperties = Map.of("status", "pending");

        // Act
        Boolean result = nodeService.connect(from, to, relationProperties);

        // Assert
        assertFalse(result);
        verify(graphDao, never()).upsertNode(any());
        verify(graphDao, never()).upsertRelation(any(), any(), any());
    }

    @Test
    void testConnect_RelationPropertiesEmpty() throws Exception {
        // Arrange
        Node from = new Node("user1");
        Node to = new Node("user2");
        Map<String, String> relationProperties = new HashMap<>();

        // Act
        Boolean result = nodeService.connect(from, to, relationProperties);

        // Assert
        assertFalse(result);
        verify(graphDao, never()).upsertNode(any());
        verify(graphDao, never()).upsertRelation(any(), any(), any());
    }

    @Test
    void testConnect_SameUserIds() throws Exception {
        // Arrange
        Node from = new Node("user1");
        Node to = new Node("user1");
        Map<String, String> relationProperties = Map.of("status", "pending");

        // Act
        Boolean result = nodeService.connect(from, to, relationProperties);

        // Assert
        assertFalse(result);
        verify(graphDao, never()).upsertNode(any());
        verify(graphDao, never()).upsertRelation(any(), any(), any());
    }

    @Test
    void testConnect_FromNodeUpsertFails() throws Exception {
        // Arrange
        Node from = new Node("user1");
        Node to = new Node("user2");
        Map<String, String> relationProperties = Map.of("status", "pending");

        when(graphDao.upsertNode(from)).thenReturn(false);

        // Act
        Boolean result = nodeService.connect(from, to, relationProperties);

        // Assert
        assertFalse(result);
        verify(graphDao).upsertNode(from);
        verify(graphDao, never()).upsertRelation(any(), any(), any());
    }

    @Test
    void testConnect_ToNodeUpsertFails() throws Exception {
        // Arrange
        Node from = new Node("user1");
        Node to = new Node("user2");
        Map<String, String> relationProperties = Map.of("status", "pending");

        when(graphDao.upsertNode(from)).thenReturn(true);
        when(graphDao.upsertNode(to)).thenReturn(false);

        // Act
        Boolean result = nodeService.connect(from, to, relationProperties);

        // Assert
        assertFalse(result);
        verify(graphDao).upsertNode(from);
        verify(graphDao).upsertNode(to);
        verify(graphDao, never()).upsertRelation(any(), any(), any());
    }

    @Test
    void testConnect_RelationUpsertFails() throws Exception {
        // Arrange
        Node from = new Node("user1");
        Node to = new Node("user2");
        Map<String, String> relationProperties = Map.of("status", "pending");

        when(graphDao.upsertNode(from)).thenReturn(true);
        when(graphDao.upsertNode(to)).thenReturn(true);
        when(graphDao.upsertRelation(from, to, relationProperties)).thenReturn(false);

        // Act
        Boolean result = nodeService.connect(from, to, relationProperties);

        // Assert
        assertFalse(result);
        verify(graphDao).upsertNode(from);
        verify(graphDao).upsertNode(to);
        verify(graphDao).upsertRelation(from, to, relationProperties);
    }

    @Test
    void testGetNodes_Success() {
        // Arrange
        String id = "user1";
        Map<String, String> relationProperties = Map.of("status", "approved");
        Constants.DIRECTION direction = Constants.DIRECTION.OUT;
        int offset = 0;
        int size = 10;
        List<String> attributes = Arrays.asList("userId", "name");

        List<Node> expectedNodes = Arrays.asList(
            new Node("user2"),
            new Node("user3")
        );

        when(graphDao.getNeighbours(id, relationProperties, direction, 1, offset, size, attributes))
            .thenReturn(expectedNodes);

        // Act
        List<Node> result = nodeService.getNodes(id, relationProperties, direction, offset, size, attributes);

        // Assert
        assertNotNull(result);
        assertEquals(expectedNodes, result);
        verify(graphDao).getNeighbours(id, relationProperties, direction, 1, offset, size, attributes);
    }

    @Test
    void testGetNodes_EmptyId() {
        // Arrange
        String id = "";
        Map<String, String> relationProperties = Map.of("status", "approved");
        Constants.DIRECTION direction = Constants.DIRECTION.OUT;
        int offset = 0;
        int size = 10;
        List<String> attributes = Arrays.asList("userId", "name");

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            nodeService.getNodes(id, relationProperties, direction, offset, size, attributes);
        });
    }

    @Test
    void testGetNodes_NullId() {
        // Arrange
        String id = null;
        Map<String, String> relationProperties = Map.of("status", "approved");
        Constants.DIRECTION direction = Constants.DIRECTION.OUT;
        int offset = 0;
        int size = 10;
        List<String> attributes = Arrays.asList("userId", "name");

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            nodeService.getNodes(id, relationProperties, direction, offset, size, attributes);
        });
    }

    @Test
    void testGetNodes_EmptyRelationProperties() {
        // Arrange
        String id = "user1";
        Map<String, String> relationProperties = new HashMap<>();
        Constants.DIRECTION direction = Constants.DIRECTION.OUT;
        int offset = 0;
        int size = 10;
        List<String> attributes = Arrays.asList("userId", "name");

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            nodeService.getNodes(id, relationProperties, direction, offset, size, attributes);
        });
    }

    @Test
    void testGetNodes_NullRelationProperties() {
        // Arrange
        String id = "user1";
        Map<String, String> relationProperties = null;
        Constants.DIRECTION direction = Constants.DIRECTION.OUT;
        int offset = 0;
        int size = 10;
        List<String> attributes = Arrays.asList("userId", "name");

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            nodeService.getNodes(id, relationProperties, direction, offset, size, attributes);
        });
    }

    @Test
    void testGetNodesCount_Success() {
        // Arrange
        String id = "user1";
        Map<String, String> relationProperties = Map.of("status", "approved");
        Constants.DIRECTION direction = Constants.DIRECTION.OUT;
        int expectedCount = 5;

        when(graphDao.getNeighboursCount(id, relationProperties, direction))
            .thenReturn(expectedCount);

        // Act
        int result = nodeService.getNodesCount(id, relationProperties, direction);

        // Assert
        assertEquals(expectedCount, result);
        verify(graphDao).getNeighboursCount(id, relationProperties, direction);
    }

    @Test
    void testGetNodesCount_EmptyId() {
        // Arrange
        String id = "";
        Map<String, String> relationProperties = Map.of("status", "approved");
        Constants.DIRECTION direction = Constants.DIRECTION.OUT;

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            nodeService.getNodesCount(id, relationProperties, direction);
        });
    }

    @Test
    void testGetNodesCount_NullId() {
        // Arrange
        String id = null;
        Map<String, String> relationProperties = Map.of("status", "approved");
        Constants.DIRECTION direction = Constants.DIRECTION.OUT;

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            nodeService.getNodesCount(id, relationProperties, direction);
        });
    }

    @Test
    void testGetNodesCount_GraphException() {
        // Arrange
        String id = "user1";
        Map<String, String> relationProperties = Map.of("status", "approved");
        Constants.DIRECTION direction = Constants.DIRECTION.OUT;

        when(graphDao.getNeighboursCount(id, relationProperties, direction))
            .thenThrow(new GraphException("Database error"));

        // Act
        int result = nodeService.getNodesCount(id, relationProperties, direction);

        // Assert
        assertEquals(0, result);
        verify(graphDao).getNeighboursCount(id, relationProperties, direction);
    }

    @Test
    void testGetNodeNextLevel_Success() {
        // Arrange
        String id = "user1";
        Map<String, String> relationProperties = Map.of("status", "approved");
        int offset = 0;
        int size = 10;

        List<Node> expectedNodes = Arrays.asList(
            new Node("user2"),
            new Node("user3")
        );

        when(graphDao.getNeighbours(id, relationProperties, Constants.DIRECTION.OUT, 2, offset, size, 
            Arrays.asList(Constants.Graph.ID.getValue())))
            .thenReturn(expectedNodes);

        // Act
        List<Node> result = nodeService.getNodeNextLevel(id, relationProperties, offset, size);

        // Assert
        assertNotNull(result);
        assertEquals(expectedNodes, result);
        verify(graphDao).getNeighbours(id, relationProperties, Constants.DIRECTION.OUT, 2, offset, size, 
            Arrays.asList(Constants.Graph.ID.getValue()));
    }

    @Test
    void testGetNodeNextLevel_EmptyId() {
        // Arrange
        String id = "";
        Map<String, String> relationProperties = Map.of("status", "approved");
        int offset = 0;
        int size = 10;

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            nodeService.getNodeNextLevel(id, relationProperties, offset, size);
        });
    }

    @Test
    void testGetNodeNextLevel_EmptyRelationProperties() {
        // Arrange
        String id = "user1";
        Map<String, String> relationProperties = new HashMap<>();
        int offset = 0;
        int size = 10;

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            nodeService.getNodeNextLevel(id, relationProperties, offset, size);
        });
    }

    @Test
    void testGetRelationshipBetweenUsers_Success() {
        // Arrange
        String fromUserId = "user1";
        String toUserId = "user2";
        Map<String, String> expectedRelationship = Map.of("status", "approved");

        when(graphDao.getRelationshipBetweenUsers(fromUserId, toUserId))
            .thenReturn(expectedRelationship);

        // Act
        Map<String, String> result = nodeService.getRelationshipBetweenUsers(fromUserId, toUserId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedRelationship, result);
        verify(graphDao).getRelationshipBetweenUsers(fromUserId, toUserId);
    }

    @Test
    void testGetRelationshipBetweenUsers_GraphException() {
        // Arrange
        String fromUserId = "user1";
        String toUserId = "user2";

        when(graphDao.getRelationshipBetweenUsers(fromUserId, toUserId))
            .thenThrow(new GraphException("Database error"));

        // Act
        Map<String, String> result = nodeService.getRelationshipBetweenUsers(fromUserId, toUserId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(graphDao).getRelationshipBetweenUsers(fromUserId, toUserId);
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

        when(graphDao.findRecommendationForUser(userId, request))
            .thenReturn(expectedRecommendations);

        // Act
        List<Map<String, String>> result = nodeService.findRecommendationForUser(userId, request);

        // Assert
        assertNotNull(result);
        assertEquals(expectedRecommendations, result);
        verify(graphDao).findRecommendationForUser(userId, request);
    }

    @Test
    void testFindRecommendationForUser_GraphException() {
        // Arrange
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0, "limit", 10);

        when(graphDao.findRecommendationForUser(userId, request))
            .thenThrow(new GraphException("Database error"));

        // Act
        List<Map<String, String>> result = nodeService.findRecommendationForUser(userId, request);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(graphDao).findRecommendationForUser(userId, request);
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

        when(graphDao.findRecommendationForMentors(userId, request))
            .thenReturn(expectedRecommendations);

        // Act
        List<Map<String, String>> result = nodeService.findRecommendationForMentors(userId, request);

        // Assert
        assertNotNull(result);
        assertEquals(expectedRecommendations, result);
        verify(graphDao).findRecommendationForMentors(userId, request);
    }

    @Test
    void testFindRecommendationForMentors_GraphException() {
        // Arrange
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0, "limit", 10);

        when(graphDao.findRecommendationForMentors(userId, request))
            .thenThrow(new GraphException("Database error"));

        // Act
        List<Map<String, String>> result = nodeService.findRecommendationForMentors(userId, request);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(graphDao).findRecommendationForMentors(userId, request);
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

        when(graphDao.findBlockedUsers(userId, request))
            .thenReturn(expectedBlockedUsers);

        // Act
        List<Map<String, String>> result = nodeService.findBlockedUsers(userId, request);

        // Assert
        assertNotNull(result);
        assertEquals(expectedBlockedUsers, result);
        verify(graphDao).findBlockedUsers(userId, request);
    }

    @Test
    void testFindBlockedUsers_GraphException() {
        // Arrange
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0, "limit", 10);

        when(graphDao.findBlockedUsers(userId, request))
            .thenThrow(new GraphException("Database error"));

        // Act
        List<Map<String, String>> result = nodeService.findBlockedUsers(userId, request);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(graphDao).findBlockedUsers(userId, request);
    }

    @Test
    void testGetConnectionsCountByStatus_Success() {
        // Arrange
        String userId = "user1";
        String status = "pending";
        Constants.DIRECTION direction = Constants.DIRECTION.OUT;
        Map<String, Integer> expectedCount = Map.of("count", 5);

        when(graphDao.getConnectionsCountByStatus(userId, status, direction))
            .thenReturn(expectedCount);

        // Act
        Map<String, Integer> result = nodeService.getConnectionsCountByStatus(userId, status, direction);

        // Assert
        assertNotNull(result);
        assertEquals(expectedCount, result);
        verify(graphDao).getConnectionsCountByStatus(userId, status, direction);
    }

    @Test
    void testGetConnectionsCountByStatus_GraphException() {
        // Arrange
        String userId = "user1";
        String status = "pending";
        Constants.DIRECTION direction = Constants.DIRECTION.OUT;

        when(graphDao.getConnectionsCountByStatus(userId, status, direction))
            .thenThrow(new GraphException("Database error"));

        // Act
        Map<String, Integer> result = nodeService.getConnectionsCountByStatus(userId, status, direction);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(graphDao).getConnectionsCountByStatus(userId, status, direction);
    }

    @Test
    void testGetCountForRecommendedUsers_Success() {
        // Arrange
        String userId = "user1";
        Integer expectedCount = 5;

        when(graphDao.getCountForRecommendedUsers(userId))
            .thenReturn(expectedCount);

        // Act
        Integer result = nodeService.getCountForRecommendedUsers(userId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedCount, result);
        verify(graphDao).getCountForRecommendedUsers(userId);
    }

    @Test
    void testGetCountForRecommendedUsers_GraphException() {
        // Arrange
        String userId = "user1";

        when(graphDao.getCountForRecommendedUsers(userId))
            .thenThrow(new GraphException("Database error"));

        // Act
        Integer result = nodeService.getCountForRecommendedUsers(userId);

        // Assert
        assertNotNull(result);
        verify(graphDao).getCountForRecommendedUsers(userId);
    }

    @Test
    void testGetCountForRecommendedMentors_Success() {
        // Arrange
        String userId = "user1";
        Integer expectedCount = 3;

        when(graphDao.getCountForRecommendedMentors(userId))
            .thenReturn(expectedCount);

        // Act
        Integer result = nodeService.getCountForRecommendedMentors(userId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedCount, result);
        verify(graphDao).getCountForRecommendedMentors(userId);
    }

    @Test
    void testGetCountForRecommendedMentors_GraphException() {
        // Arrange
        String userId = "user1";

        when(graphDao.getCountForRecommendedMentors(userId))
            .thenThrow(new GraphException("Database error"));

        // Act
        Integer result = nodeService.getCountForRecommendedMentors(userId);

        // Assert
        assertNotNull(result);
        verify(graphDao).getCountForRecommendedMentors(userId);
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

        when(graphDao.getTotalCountForUsersBasedOnStatus(userId, statusList, facet))
            .thenReturn(expectedCounts);

        // Act
        List<Map<String, Object>> result = nodeService.getTotalCountForUsersBasedOnStatus(userId, statusList, facet);

        // Assert
        assertNotNull(result);
        assertEquals(expectedCounts, result);
        verify(graphDao).getTotalCountForUsersBasedOnStatus(userId, statusList, facet);
    }

    @Test
    void testGetTotalCountForUsersBasedOnStatus_GraphException() {
        // Arrange
        String userId = "user1";
        List<String> statusList = Arrays.asList("pending", "approved");
        String facet = "department";

        when(graphDao.getTotalCountForUsersBasedOnStatus(userId, statusList, facet))
            .thenThrow(new GraphException("Database error"));

        // Act
        List<Map<String, Object>> result = nodeService.getTotalCountForUsersBasedOnStatus(userId, statusList, facet);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(graphDao).getTotalCountForUsersBasedOnStatus(userId, statusList, facet);
    }

    @Test
    void testUpdateUserProfileInNeo4j_Success() throws Exception {
        // Arrange
        Node node = new Node("user1");
        node.setFullName("John Doe");

        when(graphDao.upsertNode(node))
            .thenReturn(true);

        // Act
        boolean result = nodeService.updateUserProfileInNeo4j(node);

        // Assert
        assertTrue(result);
        verify(graphDao).upsertNode(node);
    }

    @Test
    void testUpdateUserProfileInNeo4j_Exception() throws Exception {
        // Arrange
        Node node = new Node("user1");
        node.setFullName("John Doe");

        when(graphDao.upsertNode(node))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        boolean result = nodeService.updateUserProfileInNeo4j(node);

        // Assert
        assertFalse(result);
        verify(graphDao).upsertNode(node);
    }

    @Test
    void testUpdateUserProfileInNeo4j_GraphException() throws Exception {
        // Arrange
        Node node = new Node("user1");
        node.setFullName("John Doe");

        when(graphDao.upsertNode(node))
            .thenThrow(new GraphException("Database error"));

        // Act
        boolean result = nodeService.updateUserProfileInNeo4j(node);

        // Assert
        assertFalse(result);
        verify(graphDao).upsertNode(node);
    }

    @Test
    void testUpdateUserProfileInNeo4j_UpsertReturnsFalse() throws Exception {
        // Arrange
        Node node = new Node("user1");
        node.setFullName("John Doe");

        when(graphDao.upsertNode(node))
            .thenReturn(false);

        // Act
        boolean result = nodeService.updateUserProfileInNeo4j(node);

        // Assert
        assertFalse(result);
        verify(graphDao).upsertNode(node);
    }
} 