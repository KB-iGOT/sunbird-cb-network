package org.sunbird.cb.hubservices.dao.impl;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.SessionExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.sunbird.cb.hubservices.dao.IGraphDao;
import org.sunbird.cb.hubservices.exception.ErrorCode;
import org.sunbird.cb.hubservices.exception.GraphException;
import org.sunbird.cb.hubservices.model.Node;
import org.sunbird.cb.hubservices.util.ConnectionProperties;
import org.sunbird.cb.hubservices.util.Constants;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.neo4j.driver.Values.parameters;

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
        try (Session session = neo4jDriver.session(); Transaction tx = session.beginTransaction()) {
            Result result = tx.run("MATCH (n:" + label + ") WHERE n.userId=$fromUUID RETURN n",
                    parameters(Constants.FROM_UUID, node.getUserId()));
            if (result.hasNext()) {
                result.consume();
                logger.info("Node exists, skipping creation.");
            } else {
                Map<String, Object> props = new ObjectMapper().convertValue(node, Map.class);
                result = tx.run("CREATE (n:" + label + ") SET n = $props RETURN n", Collections.singletonMap("props", props));
                result.consume();
                logger.info("Node created for userId {}", node.getUserId());
            }
            tx.commit();
        } catch (Exception e) {
            logger.error("Error creating node: ", e);
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }


    @Override
    public Boolean upsertRelation(Node nodeFrom, Node nodeTo, Map<String, String> relationProperties) throws Exception {
        boolean isUpserted = Boolean.FALSE;
        try (Session session = neo4jDriver.session(); Transaction transaction = session.beginTransaction()) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(Constants.FROM_UUID, nodeFrom.getUserId());
            parameters.put(Constants.TO_UUID, nodeTo.getUserId());
            parameters.put(Constants.Graph.PROPS.getValue(), relationProperties);

            String queryNodeExistWithReverseEdge = "MATCH (n:" + label + ")<-[r:connect]-(n1:" +
                    label + ") WHERE n.userId = $fromUUID AND n1.userId = $toUUID " + "RETURN n,n1,r.status as status";

            Query statement = new Query(queryNodeExistWithReverseEdge, parameters);
            Result result = transaction.run(statement);
            List<Record> userRecords = result.list();
            int recordSize = 0;
            if (!CollectionUtils.isEmpty(userRecords)) {
                recordSize = userRecords.size();
                for (Record userRecord : userRecords) {
                    if (Constants.Status.REJECTED.equalsIgnoreCase(userRecord.get(Constants.STATUS).asString()) ||
                            Constants.Status.UNBLOCKED.equalsIgnoreCase(userRecord.get(Constants.STATUS).asString()) ||
                            Constants.Status.WITHDRAWN.equalsIgnoreCase(userRecord.get(Constants.STATUS).asString()) ||
                            Constants.Status.APPROVED.equalsIgnoreCase(userRecord.get(Constants.STATUS).asString()) ||
                            Constants.Status.REMOVED.equalsIgnoreCase(userRecord.get(Constants.STATUS).asString())) {
                        String deleteQuery = "MATCH (n:" + label + ")<-[r:connect]-(n1:" + label + ") " +
                                "WHERE n.userId = $fromUUID AND n1.userId = $toUUID DELETE r";
                        transaction.run(deleteQuery, parameters);
                        recordSize = 0;
                    }
                }
            }
            result.consume();
            if (recordSize != 0) {
                if (logger.isDebugEnabled())
                    logger.debug("updating user relation with fromUUID {} and toUUID {} ", nodeFrom.getUserId(), nodeTo.getUserId());
                isUpserted = updateRelationshipBetweenTwoNodes(nodeFrom, nodeTo, statement, result, transaction, recordSize, relationProperties);
            } else {
                String query = "MATCH (n:" + label + ")-[r:connect]->(n1:" + label +
                        ") WHERE n.userId = $fromUUID AND n1.userId = $toUUID " + "RETURN n,n1";

                statement = new Query(query, parameters);
                result = transaction.run(statement);
                recordSize = result.list().size();
                result.consume();
                if (recordSize == 0) { // nodes relation doesn't exists
                    isUpserted = createRelationshipBetweenTwoNodes(nodeFrom, nodeTo, transaction, parameters);
                } else {
                    if (logger.isDebugEnabled())
                        logger.debug(nodeTo.getUserId(), nodeFrom.getUserId());
                    isUpserted = updateRelationshipBetweenTwoNodes(nodeTo, nodeFrom, statement, result, transaction, recordSize, relationProperties);
                }
            }
            transaction.commit();
        } catch (ClientException e) {
            logger.error("user relation creation failed : ", e);
            return Boolean.FALSE;
        }
        return isUpserted;
    }

    private Boolean updateRelationshipBetweenTwoNodes(Node nodeTo, Node nodeFrom, Query statement, Result result, Transaction transaction, int recordSize, Map<String, String> relationProperties) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(Constants.FROM_UUID, nodeFrom.getUserId());
        parameters.put(Constants.TO_UUID, nodeTo.getUserId());
        parameters.put(Constants.Graph.PROPS.getValue(), relationProperties);
        String updateQuery = "MATCH (n:" + label + ")-[r:connect]->(n1:" + label +
                ") WHERE n.userId = $fromUUID AND n1.userId = $toUUID " + "SET r" + " += " +
                "$props " + "RETURN n,n1";

        statement = new Query(updateQuery, parameters);
        result = transaction.run(statement);
        recordSize = result.list().size();
        result.consume();
        if (recordSize == 0) {
            logger.info("user relation with toUUID {} and fromUUID {} in updateRelationshipBetweenTwoNodes failed to update ", nodeTo.getUserId(),
                    nodeFrom.getUserId());
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private Boolean createRelationshipBetweenTwoNodes(Node nodeFrom, Node nodeTo, Transaction transaction, Map<String, Object> parameters) {
        int recordSize;
        Result result;
        StringBuilder query;
        Query statement;
        query = new StringBuilder();
        query.append("MATCH (n:").append(label).append("), (n1:").append(label)
                .append(") WHERE n.userId = $fromUUID AND n1.userId = $toUUID ")
                .append("CREATE (n)-[r:connect]->(n1) ").append("SET r").append(" += ").append("$props ")
                .append("RETURN n,n1");

        statement = new Query(query.toString(), parameters);
        result = transaction.run(statement);
        recordSize = result.list().size();
        result.consume();
        if (recordSize == 0) {
            logger.info("user relation with toUUID {} and fromUUID {} in createRelationshipBetweenTwoNodes failed to create ", nodeTo.getUserId(),
                    nodeFrom.getUserId());
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
                Query statement = new Query(query.toString(), parameters);

                Result result = transaction.run(statement);
                List<Record> records = result.list();
                result.consume();
                count = records.get(0).get("count(*)").asInt();
                logger.info("{} nodes count.", count);
                transaction.commit();
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
                    org.neo4j.driver.types.Type t = record.get(k).type();
                    if (t.equals(org.neo4j.driver.internal.types.InternalTypeSystem.TYPE_SYSTEM.NODE())) {
                        org.neo4j.driver.types.Node node = record.get(k).asNode();
                        if (node.get(Constants.Graph.USER_ID.getValue()) == null)
                            throw new GraphException(ErrorCode.MISSING_PROPERTY_ERROR.name(),
                                    "Missing {id} mandatory field");
                        id = node.get(Constants.Graph.USER_ID.getValue()).asString();
                    } else if (t.equals(org.neo4j.driver.internal.types.InternalTypeSystem.TYPE_SYSTEM.STRING())
                            && k.contains(Constants.Graph.ID.getValue())) {
                        id = record.get(k).asString();

                    } else if (t.equals(org.neo4j.driver.internal.types.InternalTypeSystem.TYPE_SYSTEM.RELATIONSHIP())) {
                        org.neo4j.driver.types.Relationship node =  record.get(k).asRelationship();
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

                Query statement = new Query(query.toString(), parameters);

                Result result = transaction.run(statement);
                List<Record> records = result.list();

                transaction.commit();
                logger.info("Neighbour users for UUID {} found successfully ", UUID);
                return getNodes(records);

            } catch (ClientException e) {
                transaction.rollback();
                throw new GraphException(ErrorCode.GRAPH_TRANSACTIONAL_ERROR.name(), e.getMessage());

            } finally {
                transaction.close();
            }
        } catch (SessionExpiredException se) {
            throw new GraphException(ErrorCode.GRAPH_SESSION_EXPIRED_ERROR.name(), se.getMessage());
        }

    }


    @Override
    public Map<String, String> getRelationshipBetweenUsers(String fromUser, String toUser) {
        final Map<String, String> relationshipProps = new HashMap<>();
        final String query = connectionProperties.getRelationshipBetweenUsersQuery();
        final Map<String, Object> params = new HashMap<>();
        params.put(Constants.FROM_USER, fromUser);
        params.put(Constants.TO_USER, toUser);

        try (Session session = neo4jDriver.session(SessionConfig.builder()
                .withDefaultAccessMode(AccessMode.READ)
                .build())) {
            Record rec = session.readTransaction(tx -> {
                Result rs = tx.run(query, params);
                if (!rs.hasNext()) return null;
                return rs.next();
            });
    
            if (rec != null) {
                relationshipProps.put(Constants.STATUS,
                    rec.get(Constants.STATUS).isNull() ? null : rec.get(Constants.STATUS).asString());
                relationshipProps.put(Constants.CREATED_AT,
                    rec.get(Constants.CREATED_AT).isNull() ? null : rec.get(Constants.CREATED_AT).asString());
                relationshipProps.put(Constants.UPDATED_AT,
                    rec.get(Constants.UPDATED_AT).isNull() ? null : rec.get(Constants.UPDATED_AT).asString());
            }
        } catch (Exception e) {
            logger.error("Error fetching relationship between {} and {} : {}", fromUser, toUser, e.toString());
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
                for (Record userRecommendationRecord : recordsComplete) {
                    recommendationData = new HashMap<>();
                    recommendationData.put(Constants.USER_ID, userRecommendationRecord.get(Constants.USER_ID).asString());
                    recommendationData.put(Constants.ORGANISATION_ID, userRecommendationRecord.get(Constants.ORGANISATION_ID).asString());
                    recommendationData.put(Constants.DESIGNATION, userRecommendationRecord.get(Constants.DESIGNATION).asString());
                    boolean isMentor = userRecommendationRecord.get(Constants.IS_MENTOR).isNull() ?
                            false : userRecommendationRecord.get(Constants.IS_MENTOR).asBoolean();
                    if (isMentor) {
                        recommendationData.put(Constants.ROLE, Constants.MENTOR);
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
            parameters.put(Constants.SIZE, request.get(Constants.SIZE));
            parameters.put(Constants.OFFSET, request.get(Constants.OFFSET));
            Query statement = getStatementForRecommendationFromSameOrg(parameters);
            List<Record> records= transaction.run(statement).list();
            transaction.commit();
            return records;
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
    private Query getStatementForRecommendationFromSameOrg(Map<String, Object> parameters) {
        String orgQuery = connectionProperties.getRecommendationUsersDesignationQuery();
        return new Query(orgQuery, parameters);
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
        List<Map<String, String>> recommendationList = new ArrayList<>();
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
            Query statement = getStatementForRecommendedMentorsInSameOrg(parameters);
            Result result = transaction.run(statement);
            List<Record> recordsForRecommendedMentors = result.list();
            if (!CollectionUtils.isEmpty(recordsForRecommendedMentors)) {
                recommendationList = new ArrayList<>();
                for (Record recommendMentorRecord : recordsForRecommendedMentors) {
                    recommendationData = new HashMap<>();
                    recommendationData.put(Constants.USER_ID, recommendMentorRecord.get(Constants.USER_ID).asString());
                    recommendationData.put(Constants.ORGANISATION_ID, recommendMentorRecord.get(Constants.ORGANISATION_ID).asString());
                    recommendationData.put(Constants.DESIGNATION, recommendMentorRecord.get(Constants.DESIGNATION).asString());
                    if (!recommendMentorRecord.get(Constants.ROLE).isNull()) {
                        List<String> rolesList = recommendMentorRecord.get(Constants.ROLE).asList(Value::asString);
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
            transaction.commit();
            return recommendationList;
        }
    }

    /**
     * Constructs a Neo4j statement to find recommended mentors in the same organization.
     *
     * @param parameters A map containing userId, offset, and size for pagination.
     * @return A Neo4j Statement object.
     */
    private Query getStatementForRecommendedMentorsInSameOrg(Map<String, Object> parameters) {
        String recommendedMentorsQuery = connectionProperties.getRecommendationMentorsQuery();
        return new Query(recommendedMentorsQuery, parameters);
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
        List<Map<String, String>> blockedUsersList = new ArrayList<>();
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
            Query statement = getStatementForBlockedUsers(parameters);
            Result result = transaction.run(statement);
            List<Record> recordsForBlockedUsers = result.list();
            if (!CollectionUtils.isEmpty(recordsForBlockedUsers)) {
                blockedUsersList = new ArrayList<>();
                for (Record blockedUserRecord : recordsForBlockedUsers) {
                    blockedUsersData = new HashMap<>();
                    blockedUsersData.put(Constants.USER_ID, blockedUserRecord.get("blockedUserId").asString());
                    blockedUsersData.put(Constants.DESIGNATION, blockedUserRecord.get("blockedUserDesignation").asString());
                    blockedUsersData.put(Constants.ORGANISATION_ID, blockedUserRecord.get("blockedOrganisationId").asString());
                    blockedUsersData.put(Constants.CREATED_AT, blockedUserRecord.get(Constants.CREATED_AT).asString());
                    blockedUsersData.put(Constants.UPDATED_AT, blockedUserRecord.get(Constants.UPDATED_AT).asString());
                    blockedUsersList.add(blockedUsersData);
                }
                logger.info("Blocked users for user {} fetched successfully. Found {} blocked users",
                        userId, blockedUsersList.size());
            }
            transaction.commit();
        } catch (Exception e) {
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
    private Query getStatementForBlockedUsers(Map<String, Object> parameters) {
        String blockedUsersQuery = connectionProperties.getBlockedUsersQuery();
        return new Query(blockedUsersQuery, parameters);
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
        Map<String, Integer> resultMap = new HashMap<>();
        try (Session session = neo4jDriver.session(); Transaction transaction = session.beginTransaction()) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(Constants.USER_ID, userId);
            parameters.put(Constants.STATUS, status);

            StringBuilder countQuery;
            if (direction ==Constants.DIRECTION.OUT) {
                // Count outgoing connections (user → other)
                countQuery = new StringBuilder(connectionProperties.getConnectionsOutgoingCountQuery());
            } else if (direction == Constants.DIRECTION.IN) {
                // Count incoming connections (other → user)
                countQuery = new StringBuilder(connectionProperties.getConnectionsIncomingCountQuery());
            } else {
                // Count connections in both directions
                countQuery = new StringBuilder(connectionProperties.getConnectionsBothCountQuery());
            }
            Query statement = new Query(countQuery.toString(), parameters);
            Result result = transaction.run(statement);
            int count = 0;
            if (result.hasNext()) {
                Record connectCountRecord = result.next();
                Value countValue = connectCountRecord.get(Constants.COUNT);
                count = countValue.isNull() ? 0 : countValue.asInt();
            }
            resultMap.put(Constants.COUNT, count);
            transaction.commit();
        } catch (Exception e) {
            logger.error(String.format("Error fetching connections count by status for user %s: %s", userId, e));
            resultMap.put(Constants.COUNT, 0);
        }
        return resultMap;
    }

    /**
     * Gets the count of recommended users for a given user based on organization and designation.
     *
     * @param userId The ID of the user for whom the count of recommended users is to be fetched.
     * @return The count of recommended users.
     */
    @Override
    public Integer getCountForRecommendedUsers(String userId) {
        try (Session session = neo4jDriver.session(); Transaction transaction = session.beginTransaction()) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(Constants.USER_ID, userId);
            String countQuery =connectionProperties.getRecommendedUsersCountQuery();
            Query statement = new Query(countQuery, parameters);
            Result result = transaction.run(statement);
            if (!result.hasNext()) {
                transaction.commit();
                return 0;
            }
            Record recommendUsersRecord = result.next();
            Value countValue = recommendUsersRecord.get(Constants.TOTAL_COUNT);
            transaction.commit();
            return countValue.isNull() ? 0 : countValue.asInt();
        } catch (Exception e) {
            logger.error(String.format("Error fetching connections count for recommended user %s: %s", userId, e));
        }
        return 0;
    }

    /**
     * Gets the count of recommended mentors for a given user.
     *
     * @param userId The ID of the user for whom the count of recommended mentors is to be fetched.
     * @return The count of recommended mentors.
     */
    @Override
    public Integer getCountForRecommendedMentors(String userId) {
        try (Session session = neo4jDriver.session(); Transaction transaction = session.beginTransaction()) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(Constants.USER_ID, userId);
            String countQuery =connectionProperties.getRecommendedMentorsCountQuery();
            Query statement = new Query(countQuery, parameters);
            Result result = transaction.run(statement);
            if(!result.hasNext()){
                transaction.commit();
                return 0;
            }
            Record mentorRecommenedRecord = result.next();
            Value countValue = mentorRecommenedRecord.get(Constants.TOTAL_COUNT);
            transaction.commit();
            return countValue.isNull() ? 0 : countValue.asInt();
        }catch (Exception e) {
            logger.error(String.format("Error fetching connections count for recommended mentors %s: %s", userId, e));
        }
        return 0;
    }

    /**
     * Gets the total count of users based on their connection status.
     *
     * @param userId          The ID of the user for whom the count is to be fetched.
     * @param statusValue     A list of connection statuses to filter by.
     * @param facetsAttribute A list of attributes to be used as facets (e.g., "status").
     * @return A list of maps containing the count of users grouped by status.
     */
    @Override
    public List<Map<String, Object>> getTotalCountForUsersBasedOnStatus(String userId, List<String> statusValue, String facetsAttribute) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        String query = connectionProperties.getConnectionsStatusCountQuery();
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.USER_ID, userId);
        params.put(Constants.STATUS_VALUE, statusValue);
        List<Map<String, Object>> facetsList;
        try (Session session = neo4jDriver.session(); Transaction transaction = session.beginTransaction()) {
            Query statement = new Query(query, params);
            Result result = transaction.run(statement);
            List<Record> totalCounBasedOnStatusRecordList = result.list();
            for (Record totalCounBasedOnStatusRecord : totalCounBasedOnStatusRecordList) {
                Map<String, Object> map = new HashMap<>();
                map.put(Constants.NAME, totalCounBasedOnStatusRecord.get(Constants.STATUS).asString());
                map.put(Constants.COUNT, totalCounBasedOnStatusRecord.get(Constants.COUNT).asInt());
                resultList.add(map);
            }
            facetsList = new ArrayList<>();
            Map<String, Object> facetsMap = new HashMap<>();
            facetsMap.put(Constants.NAME, facetsAttribute);
            facetsMap.put(Constants.VALUES, resultList);
            facetsList.add(facetsMap);
            transaction.commit();
            return facetsList;
        } catch (Exception e) {
            logger.error(String.format("Error fetching connections count for recommended mentors %s: %s", userId, e));
        }
        return new ArrayList<>();
    }

}
