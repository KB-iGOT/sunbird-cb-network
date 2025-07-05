package org.sunbird.hubservices.daoimpl;

import static org.neo4j.driver.internal.types.InternalTypeSystem.TYPE_SYSTEM;
import static org.neo4j.driver.v1.Values.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.exceptions.ClientException;
import org.neo4j.driver.v1.exceptions.SessionExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.sunbird.cb.hubservices.exception.ErrorCode;
import org.sunbird.cb.hubservices.exception.GraphException;
import org.sunbird.cb.hubservices.model.Node;
import org.sunbird.cb.hubservices.util.ConnectionProperties;
import org.sunbird.cb.hubservices.util.Constants;
import org.sunbird.hubservices.dao.IGraphDao;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GraphDao implements IGraphDao {

    private final Logger logger = LoggerFactory.getLogger(GraphDao.class);

    @Autowired
    private Driver neo4jDriver;

    private final String label;

    @Autowired
    public GraphDao(String label) {
        this.label = label;
    }

    @Autowired
    ConnectionProperties connectionProperties;

    @Override
    public Boolean upsertNode(Node node) throws Exception {
        try (Session session = neo4jDriver.session();Transaction transaction = session.beginTransaction()) {
            Statement statement = new Statement("MATCH (n:" + label + ") WHERE n.userId=$fromUUID " + "RETURN n", parameters(Constants.FROM_UUID, node.getId()));
            StatementResult result = transaction.run(statement);
            List<Record> existingNodes = result.list();
            result.consume();
            if (!existingNodes.isEmpty()) {
                logger.info("Nodes exists, new node cannot be created! ");
            } else {
                logger.info("Node doesn't exists, new node can be created! ");
                Map<String, Object> params = new HashMap<>();
                params.put(Constants.Graph.PROPS.getValue(), new ObjectMapper().convertValue(node, Map.class));
                StringBuilder queryBuilder = new StringBuilder();
                queryBuilder.append("CREATE (n:").append(label).append(") SET n = $props RETURN n");
                statement = new Statement(queryBuilder.toString(), params);
                result = transaction.run(statement);
                result.consume();
                transaction.commitAsync().toCompletableFuture().get();
                logger.info("user node with id {} created successfully ", node.getId());
            }
        } catch (Exception e) {
            logger.error("user node creation failed : ", e);
            return Boolean.FALSE;

        }
        return Boolean.TRUE;
    }

    @Override
    public Boolean upsertRelation(Node nodeFrom, Node nodeTo, Map<String, String> relationProperties) throws Exception {
        boolean isUpserted = Boolean.FALSE;
        try (Session session = neo4jDriver.session(); Transaction transaction = session.beginTransaction()) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(Constants.FROM_UUID, nodeFrom.getId());
            parameters.put(Constants.TO_UUID, nodeTo.getId());
            parameters.put(Constants.Graph.PROPS.getValue(), relationProperties);

            String queryNodeExistWithReverseEdge = "MATCH (n:" + label + ")<-[r:connect]-(n1:" +
                    label + ") WHERE n.userId = $fromUUID AND n1.userId = $toUUID " + "RETURN n,n1";

            Statement statement = new Statement(queryNodeExistWithReverseEdge, parameters);
            StatementResult result = transaction.run(statement);
            int recordSize = result.list().size();
            result.consume();
            if (recordSize != 0) {
                if (logger.isDebugEnabled())
                    logger.debug("updating user relation with fromUUID {} and toUUID {} ", nodeFrom.getId(), nodeTo.getId());
                isUpserted = updateRelationshipBetweenTwoNodes(nodeFrom, nodeTo, statement, result, transaction, recordSize, relationProperties);
                transaction.commitAsync().toCompletableFuture().get();
            } else {
                String query = "MATCH (n:" + label + ")-[r:connect]->(n1:" + label +
                        ") WHERE n.userId = $fromUUID AND n1.userId = $toUUID " + "RETURN n,n1";

                statement = new Statement(query, parameters);
                result = transaction.run(statement);
                recordSize = result.list().size();
                result.consume();
                if (recordSize == 0) { // nodes relation doesn't exists
                    isUpserted = createRelationshipBetweenTwoNodes(nodeFrom, nodeTo, transaction, parameters);
                } else {
                    if (logger.isDebugEnabled())
                        logger.debug(nodeTo.getId(), nodeFrom.getId());
                    isUpserted = updateRelationshipBetweenTwoNodes(nodeTo, nodeFrom, statement, result, transaction, recordSize, relationProperties);
                }
                transaction.commitAsync().toCompletableFuture().get();
            }
        } catch (ClientException e) {
            logger.error("user relation creation failed : ", e);
            return Boolean.FALSE;

        }
        return isUpserted;
    }

    private Boolean updateRelationshipBetweenTwoNodes(Node nodeTo, Node nodeFrom, Statement statement, StatementResult result, Transaction transaction, int recordSize, Map<String, String> relationProperties) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(Constants.FROM_UUID, nodeFrom.getId());
        parameters.put(Constants.TO_UUID, nodeTo.getId());
        parameters.put(Constants.Graph.PROPS.getValue(), relationProperties);
        String updateQuery = "MATCH (n:" + label + ")-[r:connect]->(n1:" + label +
                ") WHERE n.userId = $fromUUID AND n1.userId = $toUUID " + "SET r" + " += " +
                "$props " + "RETURN n,n1";

        statement = new Statement(updateQuery, parameters);
        result = transaction.run(statement);
        recordSize = result.list().size();
        result.consume();
        if (recordSize == 0) {
            logger.info("user relation with toUUID {} and fromUUID {} in updateRelationshipBetweenTwoNodes failed to update ", nodeTo.getId(),
                    nodeFrom.getId());
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private Boolean createRelationshipBetweenTwoNodes(Node nodeFrom, Node nodeTo, Transaction transaction, Map<String, Object> parameters) {
        int recordSize;
        StatementResult result;
        StringBuilder query;
        Statement statement;
        query = new StringBuilder();
        query.append("MATCH (n:").append(label).append("), (n1:").append(label)
                .append(") WHERE n.userId = $fromUUID AND n1.userId = $toUUID ")
                .append("CREATE (n)-[r:connect]->(n1) ").append("SET r").append(" += ").append("$props ")
                .append("RETURN n,n1");

        statement = new Statement(query.toString(), parameters);
        result = transaction.run(statement);
        recordSize = result.list().size();
        result.consume();
        if (recordSize == 0) {
            logger.info("user relation with toUUID {} and fromUUID {} in createRelationshipBetweenTwoNodes failed to create ", nodeTo.getId(),
                    nodeFrom.getId());
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    @Override
    public int getNeighboursCount(String UUID, Map<String, String> relationProperties, Constants.DIRECTION direction) {
        int count;

        try (Session session = neo4jDriver.session()) {
            try (Transaction transaction = session.beginTransaction()) {

                Map<String, Object> parameters = new HashMap<>();
                parameters.put(Constants.Graph.UUID.getValue(), UUID);
                parameters.put(Constants.Graph.PROPS.getValue(), relationProperties);

                StringBuilder query = new StringBuilder();

                if (direction == Constants.DIRECTION.OUT) {
                    query.append("MATCH (n:").append(label).append(")-[r:connect]->(n1:").append(label)
                            .append(") WHERE n.userId = $UUID ");
                } else if (direction == Constants.DIRECTION.IN) {
                    query.append("MATCH (n:").append(label).append(")<-[r:connect]-(n1:").append(label)
                            .append(") WHERE n.userId = $UUID ");
                } else {
                    query.append("MATCH (n:").append(label).append(")-[r:connect]-(n1:").append(label)
                            .append(") WHERE n.userId = $UUID ");
                }

                relationProperties.forEach((key, value) -> query.append(" AND r.").append(key).append(" = ").append("'").append(value).append("'"));
                query.append(" RETURN count(*)");
                Statement statement = new Statement(query.toString(), parameters);

                StatementResult result = transaction.run(statement);
                List<Record> records = result.list();
                result.consume();
                count = records.get(0).get("count(*)").asInt();
                logger.info("{} nodes count.", count);

            } catch (ClientException e) {
                throw new GraphException(ErrorCode.GRAPH_TRANSACTIONAL_ERROR.name(), e.getMessage());

            }
        } catch (SessionExpiredException se) {
            throw new GraphException(ErrorCode.GRAPH_SESSION_EXPIRED_ERROR.name(), se.getMessage());
        }
        return count;

    }

    private List<Node> getNodes(List<Record> records) {
        List<Node> nodes = new ArrayList<>();
        if (records.size() > 0) {
            for (Record record : records) {

                // TODO: optimise

                String id = null;
                String createdAt = null;
                String updatedAt = null;
                String status = null;
                for (String k : record.keys()) {
                    org.neo4j.driver.v1.types.Type t = record.get(k).type();
                    if (t.equals(TYPE_SYSTEM.NODE())) {
                        org.neo4j.driver.v1.types.Node node = record.get(k).asNode();
                        if (node.get(Constants.Graph.USER_ID.getValue()) == null)
                            throw new GraphException(ErrorCode.MISSING_PROPERTY_ERROR.name(),
                                    "Missing {id} mandatory field");
                        id = node.get(Constants.Graph.USER_ID.getValue()).asString();
                    } else if (t.equals(TYPE_SYSTEM.STRING()) && k.contains(Constants.Graph.ID.getValue())) {
                        id = record.get(k).asString();

                    } else if( t.equals(TYPE_SYSTEM.RELATIONSHIP())){
                        org.neo4j.driver.v1.types.Relationship node =  record.get(k).asRelationship();
                        createdAt = node.get("createdAt") != null ? node.get("createdAt").asString() : null;
                        updatedAt = node.get("updatedAt") != null ? node.get("updatedAt").asString() : null;
                        status = node.get("status") != null ? node.get("status").asString() : null;
                    } else {
                        throw new GraphException(ErrorCode.MISSING_PROPERTY_ERROR.name(),
                                "Missing {id} mandatory field");
                    }

                }
                Node nodePojo = new Node(id, createdAt, updatedAt, status);
                nodes.add(nodePojo);

            }
        }
        return nodes;
    }

    public List<Node> getNeighbours(String UUID, Map<String, String> relationProperties, Constants.DIRECTION direction,
                                    int level, int offset, int limit, List<String> attributes) {
        try (Session session = neo4jDriver.session()) {
            Transaction transaction = session.beginTransaction();
            try {

                if (level == 0)
                    throw new GraphException(ErrorCode.RECORD_NOT_FOUND_ERROR.name(), "Oth level have no neighbours ");

                Map<String, Object> parameters = new HashMap<>();
                parameters.put(Constants.UUID, UUID);
                parameters.put(Constants.PROPS, relationProperties);

                StringBuilder linkNthLevel = new StringBuilder();
                for (int i = 0; i <= level; i++) {
                    if (direction == Constants.DIRECTION.OUT)
                        linkNthLevel.append("(n").append(i).append(":").append(label).append(")").append("-[r")
                                .append(i).append(":connect]->");
                    if (direction == Constants.DIRECTION.IN)
                        linkNthLevel.append("(n").append(i).append(":").append(label).append(")").append("<-[r")
                                .append(i).append(":connect]-");
                    if (direction == null)
                        linkNthLevel.append("(n").append(i).append(":").append(label).append(")").append("-[r")
                                .append(i).append(":connect]-");
                }
                String s = (direction == Constants.DIRECTION.IN)
                        ? linkNthLevel.substring(0, linkNthLevel.lastIndexOf("[") - 2)
                        : linkNthLevel.substring(0, linkNthLevel.lastIndexOf("[") - 1);

                StringBuilder query = new StringBuilder();
                query.append("MATCH ").append(s).append(" WHERE n0.userId = $UUID ");

                relationProperties.forEach((key, value) -> query.append(" AND r").append(level - 1).append(".")
                        .append(key).append(" = ").append("'").append(value).append("'"));
                query.append(" RETURN ");
                StringBuilder sb = new StringBuilder();
                if (!CollectionUtils.isEmpty(attributes)) {
                    attributes.forEach(
                            attribute -> sb.append("n").append(level).append(".").append(attribute).append(","));
                    sb.append("r").append(level - 1).append(",");
                    sb.deleteCharAt(sb.length() - 1);
                } else {
                    sb.append("r").append(level - 1).append(",");
                    sb.append("n").append(level);
                }
                query.append(sb).append(" Skip ").append(offset).append(" limit ").append(limit);

                Statement statement = new Statement(query.toString(), parameters);

                StatementResult result = transaction.run(statement);
                List<Record> records = result.list();

                transaction.commitAsync().toCompletableFuture().get();
                logger.info("Neighbour users for UUID {} found successfully ", UUID);
                return getNodes(records);

            } catch (ClientException e) {
                transaction.rollbackAsync().toCompletableFuture();
                throw new GraphException(ErrorCode.GRAPH_TRANSACTIONAL_ERROR.name(), e.getMessage());

            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                transaction.close();
            }
        } catch (SessionExpiredException se) {
            throw new GraphException(ErrorCode.GRAPH_SESSION_EXPIRED_ERROR.name(), se.getMessage());
        }

    }


    @Override
    public Map<String, String> getRelationshipBetweenUsers(String fromUser, String toUser) {
        Map<String, String> relationshipProps = new HashMap<>();
        String query = "MATCH (a:" + label + ")-[r:connect]-(b:" + label + ") " +
                "WHERE a.userId = $fromUser AND b.userId = $toUser RETURN r LIMIT 1";
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.FROM_USER, fromUser);
        params.put(Constants.TO_USER, toUser);

        try (Session session = neo4jDriver.session()) {
            Statement statement = new Statement(query, params);
            StatementResult result = session.run(statement);
            if (result.hasNext()) {
                Record record = result.next();
                org.neo4j.driver.v1.types.Relationship rel = record.get("r").asRelationship();
                rel.asMap().forEach((k, v) -> relationshipProps.put(k, v != null ? v.toString() : null));
            }
        } catch (Exception e) {
            logger.error(String.format("Error fetching relationship between %s and %s : %s", fromUser, toUser, e));
        }
        return relationshipProps;
    }

    /**
     * Finds recommendations for a user based on the provided request parameters.
     *
     * @param userId  The ID of the user for whom recommendations are to be found.
     * @param request A map containing request parameters for finding recommendations.
     * @return A list of maps, each representing a recommendation with relevant details.
     */
    @Override
    public List<Map<String, String>> findRecommendationForUser(String userId, Map<String, Object> request) {
        Map<String, String> recommendationData;
        List<Map<String, String>> recommendationList = null;
        try (Session session = neo4jDriver.session()) {
            List<Record> recordsComplete = fetchRecommendationBasedOnOrgAndDesignation(userId, request, session);
            if (!CollectionUtils.isEmpty(recordsComplete)) {
                recommendationList = new ArrayList<>();
                for (Record record : recordsComplete) {
                    recommendationData = new HashMap<>();
                    recommendationData.put(Constants.USER_ID, record.get(Constants.USER_ID).asString());
                    recommendationData.put(Constants.ORGANISATION_ID, record.get(Constants.ORGANISATION_ID).asString());
                    recommendationData.put(Constants.DESIGNATION, record.get(Constants.DESIGNATION).asString());
                    if (!record.get(Constants.ROLE).isNull()) {
                        List<String> rolesList = record.get(Constants.ROLE).asList(Value::asString);
                        String rolesString = String.join(",", rolesList);
                        recommendationData.put(Constants.ROLE, rolesString);
                    } else {
                        recommendationData.put(Constants.ROLE, "");
                    }
                    recommendationList.add(recommendationData);
                }
                logger.info("Recommendations for user {} fetched successfully. Found {} recommendations",
                        userId, recommendationList.size());
            }
        }
        return recommendationList;
    }

    /**
     * Fetches recommendations for a user based on the same organization and designation.
     *
     * @param userId  The ID of the user for whom recommendations are to be fetched.
     * @param request A map containing request parameters such as size and offset.
     * @param session The Neo4j session to use for the query.
     * @return A list of records containing recommendation data.
     */
    private List<Record> fetchRecommendationBasedOnOrgAndDesignation(String userId, Map<String, Object> request, Session session) {
        try (Transaction transaction = session.beginTransaction()) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(Constants.USER_ID, userId);
            int size = (Integer) request.get(Constants.SIZE);
            int offset = Math.max(0, (Integer) request.get(Constants.OFFSET));
            parameters.put(Constants.SIZE, size);
            if (offset != 0) {
                offset = (offset * size) + 1;
            }
            parameters.put(Constants.OFFSET, offset);
            Statement statement = getStatementForRecommendationFromSameOrg(parameters);
            StatementResult result = transaction.run(statement);
            List<Record> recordsFromSameOrg = result.list();
            result.consume();
            return recordsFromSameOrg;
        } catch (Exception e) {
            logger.error("Error finding recommendations for user {}: {}", userId, e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Constructs a Neo4j statement to find recommendations for a user from the same organization.
     *
     * @param parameters A map containing userId, offset, and size for pagination.
     * @return A Neo4j Statement object.
     */
    private Statement getStatementForRecommendationFromSameOrg(Map<String, Object> parameters) {
        String orgQuery = "MATCH (u1:" + connectionProperties.getUserLabelV3() + " {userId: $userId}) " +
                "WITH u1 " +
                "MATCH (u2:" + connectionProperties.getUserLabelV3() + ") " +
                "WHERE ( " +
                "    (u2.organisationId = u1.organisationId AND u2.userId <> u1.userId) " +
                "    OR " +
                "    (u2.designation = u1.designation AND u2.organisationId <> u1.organisationId AND u2.userId <> u1.userId) " +
                ") " +
                "OPTIONAL MATCH (u1)-[r]-(u2) " +
                "WHERE r IS NULL OR (NOT r.status IN ['Approved','Pending', 'Blocked']) " +
                "RETURN u2.userId AS userId, " +
                "       u2.organisationId AS organisationId, " +
                "       u2.designation AS designation, " +
                "       u2.role AS role " +
                "SKIP $offset LIMIT $size";
        return new Statement(orgQuery, parameters);
    }

    /**
     * Finds recommendations for mentors based on the provided request parameters.
     *
     * @param userId  The ID of the user for whom mentor recommendations are to be found.
     * @param request A map containing request parameters for finding mentor recommendations.
     * @return A list of maps, each representing a mentor recommendation with relevant details.
     */
    @Override
    public List<Map<String, String>> findRecommendationForMentors(String userId, Map<String, Object> request) {
        Map<String, String> recommendationData;
        List<Map<String, String>> recommendationList = null;
        try (Session session = neo4jDriver.session(); Transaction transaction = session.beginTransaction()) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(Constants.USER_ID, userId);
            int size = (Integer) request.get(Constants.SIZE);
            int offset = Math.max(0, (Integer) request.get(Constants.OFFSET));
            if (offset != 0) {
                offset = (offset * size) + 1;
            }
            parameters.put(Constants.SIZE, size);
            parameters.put(Constants.OFFSET, offset);
            Statement statement = getStatementForRecommendedMentorsInSameOrg(parameters);
            StatementResult result = transaction.run(statement);
            List<Record> recordsForRecommendedMentors = result.list();
            result.consume();
            if (!CollectionUtils.isEmpty(recordsForRecommendedMentors)) {
                recommendationList = new ArrayList<>();
                for (Record record : recordsForRecommendedMentors) {
                    recommendationData = new HashMap<>();
                    recommendationData.put(Constants.USER_ID, record.get(Constants.USER_ID).asString());
                    recommendationData.put(Constants.ORGANISATION_ID, record.get(Constants.ORGANISATION_ID).asString());
                    recommendationData.put(Constants.DESIGNATION, record.get(Constants.DESIGNATION).asString());
                    if (!record.get(Constants.ROLE).isNull()) {
                        List<String> rolesList = record.get(Constants.ROLE).asList(Value::asString);
                        String rolesString = String.join(",", rolesList);
                        recommendationData.put(Constants.ROLE, rolesString);
                    } else {
                        recommendationData.put(Constants.ROLE, "");
                    }
                    recommendationList.add(recommendationData);
                }
                logger.info("Recommendations for user {} fetched successfully. Found {} recommendations",
                        userId, recommendationList.size());
            }
            return recommendationList;
        }
    }

    /**
     * Constructs a Neo4j statement to find recommended mentors in the same organization.
     *
     * @param parameters A map containing userId, offset, and size for pagination.
     * @return A Neo4j Statement object.
     */
    private Statement getStatementForRecommendedMentorsInSameOrg(Map<String, Object> parameters) {
        String recommendedMentorsQuery = "MATCH (u1:" + connectionProperties.getUserLabelV3() + " {userId: $userId}) " +
                "MATCH (u2:" + connectionProperties.getUserLabelV3() + ") " +
                "WHERE u2.organisationId = u1.organisationId " +
                "AND u2.userId <> u1.userId " +
                "AND NOT (u1)--(u2) " +
                "AND 'MENTOR' IN u2.role " +
                "RETURN u2.userId as userId, u2.organisationId as organisationId, " +
                "u2.designation as designation, " +
                "u2.role as role " +
                "SKIP $offset LIMIT $size";
        return new Statement(recommendedMentorsQuery, parameters);
    }


    /**
     * Finds blocked users based on the provided request parameters.
     *
     * @param userId  The ID of the user for whom blocked users are to be found.
     * @param request A map containing request parameters for finding blocked users.
     * @return A list of maps, each representing a blocked user with relevant details.
     */
    @Override
    public List<Map<String, String>> findBlockedUsers(String userId, Map<String, Object> request) {
        Map<String, String> blockedUsersData;
        List<Map<String, String>> blockedUsersList = null;
        try (Session session = neo4jDriver.session(); Transaction transaction = session.beginTransaction()) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(Constants.USER_ID, userId);
            int size = (Integer) request.get(Constants.SIZE);
            int offset = Math.max(0, (Integer) request.get(Constants.OFFSET));
            if (offset != 0) {
                offset = (offset * size) + 1;
            }
            parameters.put(Constants.SIZE, size);
            parameters.put(Constants.OFFSET, offset);
            Statement statement = getStatementForBlockedUsers(parameters);
            StatementResult result = transaction.run(statement);
            List<Record> recordsForRecommendedMentors = result.list();
            result.consume();
            if (!CollectionUtils.isEmpty(recordsForRecommendedMentors)) {
                blockedUsersList = new ArrayList<>();
                for (Record record : recordsForRecommendedMentors) {
                    blockedUsersData = new HashMap<>();
                    blockedUsersData.put(Constants.USER_ID, record.get("blockedUserId").asString());
                    blockedUsersData.put(Constants.DESIGNATION, record.get("blockedUserDesignation").asString());
                    blockedUsersData.put(Constants.ORGANISATION_ID, record.get("blockedOrganisationId").asString());
                    blockedUsersList.add(blockedUsersData);
                }
                logger.info("Blocked users for user {} fetched successfully. Found {} blocked users",
                        userId, blockedUsersList.size());
            }
        }catch (Exception e){
            logger.error("Error finding blocked users for user {}: {}", userId, e.getMessage());
            }
        return blockedUsersList;
    }

    /**
     * Constructs a Neo4j statement to find blocked users for a given user.
     *
     * @param parameters A map containing userId, offset, and size for pagination.
     * @return A Neo4j Statement object.
     */
    private Statement getStatementForBlockedUsers(Map<String, Object> parameters) {
        String blockedUsersQuery = "MATCH (u:" + connectionProperties.getUserLabelV3() + ")-[r:CONNECTS_TO]->(blocked:" +
                connectionProperties.getUserLabelV3() + ") " +
                "WHERE u.userId = $userId " +
                "AND r.status IN ['blocked','Blocked'] " +
                "RETURN " +
                "u.userId AS userId, " +
                "u.designation AS designation, " +
                "u.organisationId AS organisationId, " +
                "blocked.userId AS blockedUserId, " +
                "blocked.designation AS blockedUserDesignation, " +
                "blocked.organisationId AS blockedOrganisationId, " +
                "r.status AS connectionStatus " +
                "SKIP $offset LIMIT $size";
        return new Statement(blockedUsersQuery, parameters);
    }

    /**
     * Gets the count of connections for a user based on the specified status and direction.
     *
     * @param userId   The ID of the user for whom connections count is to be fetched.
     * @param status   The status of the connections (e.g., 'connected', 'blocked').
     * @param direction The direction of the connections (OUT, IN, or BOTH).
     * @return A map containing the count of connections.
     */
    @Override
    public Map<String, Integer> getConnectionsCountByStatus(String userId, String status, Constants.DIRECTION direction) {
        try (Session session = neo4jDriver.session(); Transaction transaction = session.beginTransaction()) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(Constants.USER_ID, userId);
            parameters.put(Constants.STATUS, status);

            StringBuilder countQuery;
            if (direction ==Constants.DIRECTION.OUT) {
                // Count outgoing connections (user → other)
                countQuery = new StringBuilder("MATCH (u:" + connectionProperties.getUserLabelV3() + ")-[r:connect]->(other:" +
                        connectionProperties.getUserLabelV3() + ") " +
                        "WHERE u.userId = $userId AND r.status = $status " +
                        "RETURN COUNT(r) AS count");
            } else if (direction == Constants.DIRECTION.IN) {
                // Count incoming connections (other → user)
                countQuery = new StringBuilder("MATCH (other:" + connectionProperties.getUserLabelV3() + ")-[r:connect]->(u:" +
                        connectionProperties.getUserLabelV3() + ") " +
                        "WHERE u.userId = $userId AND r.status = $status " +
                        "RETURN COUNT(r) AS count");
            } else {
                // Count connections in both directions
                countQuery = new StringBuilder("MATCH (u:" + connectionProperties.getUserLabelV3() + ")-[r:connect]-(other:" +
                        connectionProperties.getUserLabelV3() + ") " +
                        "WHERE u.userId = $userId AND r.status = $status " +
                        "RETURN COUNT(r) AS count");
            }
            Statement statement = new Statement(countQuery.toString(), parameters);
            StatementResult result = transaction.run(statement);
            Record record = result.single();
            result.consume();
            int count = record.get(Constants.COUNT).asInt();
            Map<String, Integer> resultMap = new HashMap<>();
            resultMap.put(Constants.COUNT, count);
            return resultMap;
        } catch (Exception e) {
            logger.error(String.format("Error fetching connections count by status for user %s: %s", userId, e));
        }
        return new HashMap<>();
    }
}
