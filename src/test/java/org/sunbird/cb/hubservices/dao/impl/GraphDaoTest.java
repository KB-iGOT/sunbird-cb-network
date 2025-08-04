package org.sunbird.cb.hubservices.dao.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.exceptions.ClientException;
import org.neo4j.driver.v1.exceptions.SessionExpiredException;
import org.springframework.test.util.ReflectionTestUtils;
import org.sunbird.cb.hubservices.exception.GraphException;
import org.sunbird.cb.hubservices.model.Node;
import org.sunbird.cb.hubservices.util.ConnectionProperties;
import org.sunbird.cb.hubservices.util.Constants;

class GraphDaoTest {

    private GraphDao graphDao;

    @Mock
    private Driver neo4jDriver;

    @Mock
    private Session session;

    @Mock
    private Transaction transaction;

    @Mock
    private StatementResult statementResult;

    @Mock
    private Record record;

    @Mock
    private ConnectionProperties connectionProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        graphDao = new GraphDao("testLabel");
        ReflectionTestUtils.setField(graphDao, "neo4jDriver", neo4jDriver);
        ReflectionTestUtils.setField(graphDao, "connectionProperties", connectionProperties);
        
        // Mock all ConnectionProperties query methods
        when(connectionProperties.getRelationshipBetweenUsersQuery()).thenReturn("MATCH (n:User)-[r:connect]-(m:User) WHERE n.userId = $fromUser AND m.userId = $toUser RETURN r.status, r.createdAt, r.updatedAt");
        when(connectionProperties.getRecommendationUsersDesignationQuery()).thenReturn("MATCH (n:User) WHERE n.userId <> $userId RETURN n.userId, n.organisationId, n.designation, n.role SKIP $offset LIMIT $size");
        when(connectionProperties.getRecommendationMentorsQuery()).thenReturn("MATCH (n:User) WHERE n.userId <> $userId RETURN n.userId, n.organisationId, n.designation, n.role SKIP $offset LIMIT $size");
        when(connectionProperties.getBlockedUsersQuery()).thenReturn("MATCH (n:User)-[r:connect {status:'Blocked'}]-(m:User) WHERE n.userId = $userId RETURN m.userId as blockedUserId, m.designation as blockedUserDesignation, m.organisationId as blockedOrganisationId, r.createdAt, r.updatedAt SKIP $offset LIMIT $size");
        when(connectionProperties.getRecommendedUsersCountQuery()).thenReturn("MATCH (n:User) WHERE n.userId <> $userId RETURN count(*) as totalCount");
        when(connectionProperties.getRecommendedMentorsCountQuery()).thenReturn("MATCH (n:User) WHERE n.userId <> $userId RETURN count(*) as totalCount");
        when(connectionProperties.getConnectionsStatusCountQuery()).thenReturn("MATCH (n:User)-[r:connect]-(m:User) WHERE n.userId = $userId AND r.status IN $statusValue RETURN r.status, count(*) as count");
        when(connectionProperties.getConnectionsOutgoingCountQuery()).thenReturn("MATCH (n:User)-[r:connect]->(m:User) WHERE n.userId = $userId AND r.status = $status RETURN count(*) as count");
        when(connectionProperties.getConnectionsIncomingCountQuery()).thenReturn("MATCH (n:User)<-[r:connect]-(m:User) WHERE n.userId = $userId AND r.status = $status RETURN count(*) as count");
        when(connectionProperties.getConnectionsBothCountQuery()).thenReturn("MATCH (n:User)-[r:connect]-(m:User) WHERE n.userId = $userId AND r.status = $status RETURN count(*) as count");
        when(connectionProperties.getUserLabelV3()).thenReturn("User");
    }

    @Test
    void testUpsertNode_NodeExists() throws Exception {
        Node node = new Node("user123");
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(anyString(), any(Value.class))).thenReturn(statementResult);
        when(statementResult.hasNext()).thenReturn(true);

        Boolean result = graphDao.upsertNode(node);

        assertTrue(result);
        verify(transaction, never()).commitAsync();
    }

    @Test
    void testUpsertNode_NodeDoesNotExist() throws Exception {
        Node node = new Node("user123");
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(anyString(), any(Value.class))).thenReturn(statementResult);
        when(statementResult.hasNext()).thenReturn(false);
        when(transaction.run(anyString(), any(Map.class))).thenReturn(statementResult);
        when(transaction.commitAsync()).thenReturn(future);

        Boolean result = graphDao.upsertNode(node);

        assertTrue(result);
        verify(transaction).commitAsync();
    }

    @Test
    void testUpsertNode_Exception() throws Exception {
        Node node = new Node("user123");
        when(neo4jDriver.session()).thenThrow(new RuntimeException("Connection error"));

        Boolean result = graphDao.upsertNode(node);

        assertFalse(result);
    }

    @Test
    void testUpsertRelation_ReverseEdgeExists() throws Exception {
        Node nodeFrom = new Node("user1");
        Node nodeTo = new Node("user2");
        Map<String, String> relationProperties = Map.of("status", "pending");
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.list()).thenReturn(Arrays.asList(record));
        when(record.get(Constants.STATUS)).thenReturn(Values.value("pending"));
        when(transaction.commitAsync()).thenReturn(future);

        Boolean result = graphDao.upsertRelation(nodeFrom, nodeTo, relationProperties);

        assertTrue(result);
    }

    @Test
    void testUpsertRelation_ClientException() throws Exception {
        Node nodeFrom = new Node("user1");
        Node nodeTo = new Node("user2");
        Map<String, String> relationProperties = Map.of("status", "pending");

        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenThrow(new ClientException("Neo4j error"));

        Boolean result = graphDao.upsertRelation(nodeFrom, nodeTo, relationProperties);

        assertFalse(result);
    }

    @Test
    void testGetNeighboursCount_OutDirection() {
        String uuid = "user123";
        Map<String, String> relationProperties = Map.of("status", "approved");
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.list()).thenReturn(Arrays.asList(record));
        when(record.get("count(*)")).thenReturn(Values.value(5));

        int result = graphDao.getNeighboursCount(uuid, relationProperties, Constants.DIRECTION.OUT);

        assertEquals(5, result);
    }

    @Test
    void testGetNeighboursCount_SessionExpiredException() {
        String uuid = "user123";
        Map<String, String> relationProperties = Map.of("status", "approved");
        
        when(neo4jDriver.session()).thenThrow(new SessionExpiredException("Session expired"));

        assertThrows(GraphException.class, () -> 
            graphDao.getNeighboursCount(uuid, relationProperties, Constants.DIRECTION.OUT));
    }

    @Test
    void testGetNeighbours_Success() throws Exception {
        String uuid = "user123";
        Map<String, String> relationProperties = Map.of("status", "approved");
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.list()).thenReturn(new ArrayList<>());
        when(transaction.commitAsync()).thenReturn(future);
        doNothing().when(transaction).close();

        List<Node> result = graphDao.getNeighbours(uuid, relationProperties, Constants.DIRECTION.OUT, 1, 0, 10, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetNeighbours_ZeroLevel() {
        String uuid = "user123";
        Map<String, String> relationProperties = Map.of("status", "approved");
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);

        assertThrows(GraphException.class, () -> 
            graphDao.getNeighbours(uuid, relationProperties, Constants.DIRECTION.OUT, 0, 0, 10, null));
    }

    @Test
    void testGetRelationshipBetweenUsers_Success() {
        String fromUser = "user1";
        String toUser = "user2";
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.hasNext()).thenReturn(true);
        when(statementResult.next()).thenReturn(record);
        
        when(record.get(Constants.STATUS)).thenReturn(Values.value("Approved"));
        when(record.get(Constants.CREATED_AT)).thenReturn(Values.value("2023-01-01"));
        when(record.get(Constants.UPDATED_AT)).thenReturn(Values.value("2023-01-02"));

        Map<String, String> result = graphDao.getRelationshipBetweenUsers(fromUser, toUser);

        assertEquals("Approved", result.get(Constants.STATUS));
        assertEquals("2023-01-01", result.get(Constants.CREATED_AT));
        assertEquals("2023-01-02", result.get(Constants.UPDATED_AT));
    }

    @Test
    void testFindRecommendationForUser_Success() {
        String userId = "user123";
        Map<String, Object> request = Map.of("size", 10, "offset", 0);
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.list()).thenReturn(Arrays.asList(record));
        
        when(record.get(Constants.USER_ID)).thenReturn(Values.value("user456"));
        when(record.get(Constants.ORGANISATION_ID)).thenReturn(Values.value("org123"));
        when(record.get(Constants.DESIGNATION)).thenReturn(Values.value("Manager"));
        when(record.get(Constants.ROLE)).thenReturn(Values.value(Arrays.asList("ADMIN")));

        List<Map<String, String>> result = graphDao.findRecommendationForUser(userId, request);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("user456", result.get(0).get(Constants.USER_ID));
    }

    @Test
    void testGetConnectionsCountByStatus_OutDirection() {
        String userId = "user123";
        String status = "Approved";
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.single()).thenReturn(record);
        when(record.get(Constants.COUNT)).thenReturn(Values.value(5));

        Map<String, Integer> result = graphDao.getConnectionsCountByStatus(userId, status, Constants.DIRECTION.OUT);

        assertEquals(5, result.get(Constants.COUNT).intValue());
    }

    @Test
    void testGetCountForRecommendedUsers_Success() {
        String userId = "user123";
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.single()).thenReturn(record);
        when(record.get(Constants.TOTAL_COUNT)).thenReturn(Values.value(10));

        Integer result = graphDao.getCountForRecommendedUsers(userId);

        assertEquals(10, result.intValue());
    }

    @Test
    void testGetTotalCountForUsersBasedOnStatus_Success() {
        String userId = "user123";
        List<String> statusValue = Arrays.asList("Pending", "Approved");
        String facetsAttribute = "status";
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.list()).thenReturn(Arrays.asList(record));
        when(record.get(Constants.STATUS)).thenReturn(Values.value("Pending"));
        when(record.get(Constants.COUNT)).thenReturn(Values.value(3));

        List<Map<String, Object>> result = graphDao.getTotalCountForUsersBasedOnStatus(userId, statusValue, facetsAttribute);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("status", result.get(0).get(Constants.NAME));
    }

    @Test
    void testFindRecommendationForMentors_Success() {
        String userId = "user123";
        Map<String, Object> request = Map.of("size", 5, "offset", 0);
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.list()).thenReturn(Arrays.asList(record));
        
        when(record.get(Constants.USER_ID)).thenReturn(Values.value("u123"));
        when(record.get(Constants.ORGANISATION_ID)).thenReturn(Values.value("org1"));
        when(record.get(Constants.DESIGNATION)).thenReturn(Values.value("Lead"));
        when(record.get(Constants.ROLE)).thenReturn(Values.value(Arrays.asList("MENTOR")));

        List<Map<String, String>> result = graphDao.findRecommendationForMentors(userId, request);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("u123", result.get(0).get(Constants.USER_ID));
        assertEquals("MENTOR", result.get(0).get(Constants.ROLE));
    }

    @Test
    void testFindBlockedUsers_Success() {
        String userId = "user123";
        Map<String, Object> request = Map.of("size", 2, "offset", 0);
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.list()).thenReturn(Arrays.asList(record));
        
        when(record.get("blockedUserId")).thenReturn(Values.value("block1"));
        when(record.get("blockedUserDesignation")).thenReturn(Values.value("Tester"));
        when(record.get("blockedOrganisationId")).thenReturn(Values.value("orgB"));
        when(record.get(Constants.CREATED_AT)).thenReturn(Values.value("2025-07-31"));
        when(record.get(Constants.UPDATED_AT)).thenReturn(Values.value("2025-08-01"));

        List<Map<String, String>> result = graphDao.findBlockedUsers(userId, request);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("block1", result.get(0).get(Constants.USER_ID));
    }

    @Test
    void testGetCountForRecommendedMentors_Success() {
        String userId = "userCount";
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.single()).thenReturn(record);
        when(record.get(Constants.TOTAL_COUNT)).thenReturn(Values.value(5));

        Integer result = graphDao.getCountForRecommendedMentors(userId);

        assertEquals(5, result.intValue());
    }

    @Test
    void testUpsertRelation_WithStatusDeletion() throws Exception {
        Node nodeFrom = new Node("user1");
        Node nodeTo = new Node("user2");
        Map<String, String> relationProperties = Map.of("status", "pending");
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.list()).thenReturn(Arrays.asList(record));
        when(record.get(Constants.STATUS)).thenReturn(Values.value("Approved"));
        when(transaction.commitAsync()).thenReturn(future);

        Boolean result = graphDao.upsertRelation(nodeFrom, nodeTo, relationProperties);

        assertTrue(result);
    }

    @Test
    void testGetNeighbours_WithAttributes() throws Exception {
        String uuid = "user123";
        Map<String, String> relationProperties = Map.of("status", "approved");
        List<String> attributes = Arrays.asList("userId", "name");
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.list()).thenReturn(new ArrayList<>());
        when(transaction.commitAsync()).thenReturn(future);
        doNothing().when(transaction).close();

        List<Node> result = graphDao.getNeighbours(uuid, relationProperties, Constants.DIRECTION.IN, 1, 0, 10, attributes);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCreateRelationshipBetweenTwoNodes_Success() throws Exception {
        Node nodeFrom = mock(Node.class);
        Node nodeTo = mock(Node.class);
        Transaction transaction = mock(Transaction.class);
        StatementResult result = mock(StatementResult.class);

        Map<String, Object> parameters = new HashMap<>();
        when(transaction.run(any(Statement.class))).thenReturn(result);
        when(result.list()).thenReturn(Arrays.asList(mock(Record.class)));

        Method method = GraphDao.class.getDeclaredMethod("createRelationshipBetweenTwoNodes",
                Node.class, Node.class, Transaction.class, Map.class);
        method.setAccessible(true);

        Boolean success = (Boolean) method.invoke(graphDao, nodeFrom, nodeTo, transaction, parameters);

        assertTrue(success);
    }

    @Test
    void testGetNeighboursCount_InDirection() {
        String uuid = "user123";
        Map<String, String> relationProperties = Map.of("status", "approved");
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.list()).thenReturn(Arrays.asList(record));
        when(record.get("count(*)")).thenReturn(Values.value(3));

        int result = graphDao.getNeighboursCount(uuid, relationProperties, Constants.DIRECTION.IN);

        assertEquals(3, result);
    }

    @Test
    void testGetConnectionsCountByStatus_InDirection() {
        String userId = "user123";
        String status = "Pending";
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.single()).thenReturn(record);
        when(record.get(Constants.COUNT)).thenReturn(Values.value(2));

        Map<String, Integer> result = graphDao.getConnectionsCountByStatus(userId, status, Constants.DIRECTION.IN);

        assertEquals(2, result.get(Constants.COUNT).intValue());
    }

    @Test
    void testFindRecommendationForUser_EmptyResult() {
        String userId = "user123";
        Map<String, Object> request = Map.of("size", 10, "offset", 0);
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.list()).thenReturn(new ArrayList<>());

        List<Map<String, String>> result = graphDao.findRecommendationForUser(userId, request);

        assertNull(result);
    }

    @Test
    void testFindRecommendationForMentors_EmptyResult() {
        String userId = "user123";
        Map<String, Object> request = Map.of("size", 5, "offset", 0);
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.list()).thenReturn(new ArrayList<>());

        List<Map<String, String>> result = graphDao.findRecommendationForMentors(userId, request);

        assertNull(result);
    }

    @Test
    void testFindBlockedUsers_EmptyResult() {
        String userId = "user123";
        Map<String, Object> request = Map.of("size", 2, "offset", 0);
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.list()).thenReturn(new ArrayList<>());

        List<Map<String, String>> result = graphDao.findBlockedUsers(userId, request);

        assertNull(result);
    }

    @Test
    void testGetCountForRecommendedUsers_NullValue() {
        String userId = "user123";
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.single()).thenReturn(record);
        
        Value nullValue = mock(Value.class);
        when(nullValue.isNull()).thenReturn(true);
        when(record.get(Constants.TOTAL_COUNT)).thenReturn(nullValue);

        Integer result = graphDao.getCountForRecommendedUsers(userId);

        assertEquals(0, result.intValue());
    }

    @Test
    void testGetCountForRecommendedMentors_NullValue() {
        String userId = "user123";
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.single()).thenReturn(record);
        
        Value nullValue = mock(Value.class);
        when(nullValue.isNull()).thenReturn(true);
        when(record.get(Constants.TOTAL_COUNT)).thenReturn(nullValue);

        Integer result = graphDao.getCountForRecommendedMentors(userId);

        assertEquals(0, result.intValue());
    }

    @Test
    void testFindRecommendationForUser_WithNullRole() {
        String userId = "user123";
        Map<String, Object> request = Map.of("size", 10, "offset", 0);
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.list()).thenReturn(Arrays.asList(record));
        
        when(record.get(Constants.USER_ID)).thenReturn(Values.value("user456"));
        when(record.get(Constants.ORGANISATION_ID)).thenReturn(Values.value("org123"));
        when(record.get(Constants.DESIGNATION)).thenReturn(Values.value("Manager"));
        
        Value nullRole = mock(Value.class);
        when(nullRole.isNull()).thenReturn(true);
        when(record.get(Constants.ROLE)).thenReturn(nullRole);

        List<Map<String, String>> result = graphDao.findRecommendationForUser(userId, request);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("", result.get(0).get(Constants.ROLE));
    }

    @Test
    void testFindRecommendationForMentors_WithNullRole() {
        String userId = "user123";
        Map<String, Object> request = Map.of("size", 5, "offset", 0);
        
        when(neo4jDriver.session()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.run(any(Statement.class))).thenReturn(statementResult);
        when(statementResult.list()).thenReturn(Arrays.asList(record));
        
        when(record.get(Constants.USER_ID)).thenReturn(Values.value("u123"));
        when(record.get(Constants.ORGANISATION_ID)).thenReturn(Values.value("org1"));
        when(record.get(Constants.DESIGNATION)).thenReturn(Values.value("Lead"));
        
        Value nullRole = mock(Value.class);
        when(nullRole.isNull()).thenReturn(true);
        when(record.get(Constants.ROLE)).thenReturn(nullRole);

        List<Map<String, String>> result = graphDao.findRecommendationForMentors(userId, request);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("", result.get(0).get(Constants.ROLE));
    }
}