package org.sunbird.cb.hubservices.serviceimpl;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.sunbird.cb.hubservices.cache.RedisCacheMgr;
import org.sunbird.cb.hubservices.cassandra.CassandraOperation;
import org.sunbird.cb.hubservices.common.util.ProjectUtil;
import org.sunbird.cb.hubservices.exception.ApplicationException;
import org.sunbird.cb.hubservices.exception.BadRequestException;
import org.sunbird.cb.hubservices.exception.ValidationException;
import org.sunbird.cb.hubservices.model.*;
import org.sunbird.cb.hubservices.service.IConnectionService;
import org.sunbird.cb.hubservices.service.INodeService;
import org.sunbird.cb.hubservices.service.IProfileService;
import org.sunbird.cb.hubservices.util.ConnectionProperties;
import org.sunbird.cb.hubservices.util.Constants;
import org.sunbird.cb.hubservices.util.RequestHandlerServiceImpl;
import org.sunbird.cb.hubservices.util.notificationUtill.HelperMethodService;
import org.sunbird.cb.hubservices.util.notificationUtill.NotificationTriggerService;

@Service
public class ConnectionService implements IConnectionService {
	private Logger logger = LoggerFactory.getLogger(ConnectionService.class);
	@Autowired
	private NotificationService notificationService;

	@Autowired
	private ConnectionProperties connectionProperties;

	@Autowired
	INodeService nodeService;

	@Autowired
	CassandraOperation cassandraOperation;

	@Autowired
	HelperMethodService helperMethodService;

	@Autowired
	NotificationTriggerService notificationTriggerService;

	@Autowired
	RedisCacheMgr redisCacheMgr;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	RequestHandlerServiceImpl requestHandlerService;

	@Autowired
	IProfileService profileService;

	/**
	 * This method is used to block a user.
	 *
	 * @param connectionRequest the connection request containing details of the user to be blocked
	 * @param authToken         the authentication token of the user
	 * @return SBApiResponse containing the status and message of the operation
	 */
	@Override
	public SBApiResponse blockUser(ConnectionRequest connectionRequest, String authToken) {
		SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_BLOCK_USER);
		Node from = new Node(connectionRequest.getUserIdFrom());
		Node to = new Node(connectionRequest.getUserIdTo());
		Map<String, String> propertyMap = new HashMap<>();
		propertyMap.put(Constants.X_AUTH_TOKEN, authToken);
		List<String> userIds = Arrays.asList(connectionRequest.getUserIdTo(), connectionRequest.getUserIdFrom());
		for (String userId : userIds) {
			Map<String, Object> readData = (Map<String, Object>) requestHandlerService
					.fetchUsingGetWithHeadersProfile(connectionProperties.getLearnerServiceHost() + connectionProperties.getUserReadV5() + userId,
							propertyMap);
			Map<String, Object> resultMap = (Map<String, Object>) readData.get(Constants.RESULT);
			String designation = "";
			List<String> roleList = new ArrayList<>();
			Map<String, Object> responseMap = new HashMap<>();
			if (MapUtils.isNotEmpty(resultMap)) {
				responseMap = (Map<String, Object>) resultMap.get(Constants.RESPONSE);
				List<Map<String, Object>> roles = (List<Map<String, Object>>) responseMap.get(Constants.ROLES);
				roleList = roles.stream()
						.map(roleMap -> (String) roleMap.get(Constants.ROLE))
						.filter(Objects::nonNull)
						.collect(Collectors.toList());
				Map<String, Object> profileDetails = (Map<String, Object>) responseMap.get(Constants.PROFILE_DETAILS_KEY);
				List<Map<String, Object>> professionalDetails = null;
				if (MapUtils.isNotEmpty(profileDetails)) {
					professionalDetails = (List<Map<String, Object>>) profileDetails.get(Constants.PROFESSIONAL_DETAILS);
				}
				if (CollectionUtils.isNotEmpty(professionalDetails)) {
					designation = (String) professionalDetails.get(0).get(Constants.DESIGNATION);
				}
			}
			if (userId.equals(connectionRequest.getUserIdFrom())) {
				from.setUserId(userId);
				from.setDesignation(designation);
				from.setRoles(roleList);
				from.setOrganisationId((String) responseMap.get("rootOrgId"));
			} else {
				to.setUserId(userId);
				to.setDesignation(designation);
				to.setRoles(roleList);
				to.setOrganisationId((String) responseMap.get("rootOrgId"));
			}
		}
		Map<String, String> relationshipProperties = setRelationshipProperties(connectionRequest, from, to);
		try {
			boolean areNodesConnected = nodeService.connect(from, to, relationshipProperties);
			if (areNodesConnected) {
				response.put(Constants.ResponseStatus.MESSAGE, Constants.ResponseStatus.SUCCESSFUL);
				response.put(Constants.ResponseStatus.STATUS, HttpStatus.CREATED);
			} else {
				relationshipProperties.put(Constants.STATUS, Constants.FAILED);
				response.put(Constants.ResponseStatus.STATUS, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (Exception e) {
			logger.error(String.format("Error while blocking the user! error : %s", e.getMessage()));
		}
		return response;
	}

	@Override
	public Response upsert(ConnectionRequest request, String updateOperation) {
		Response response = new Response();
		if (validateRequest(request)) {
			Node from = new Node(request.getUserIdFrom());
			Node to = new Node(request.getUserIdTo());
			if (updateOperation.equalsIgnoreCase(Constants.UPDATE_OPERATION)) {
				to.setUserId(request.getUserIdFrom());
				from.setUserId(request.getUserIdTo());
			}
			Map<String, String> relationshipProperties = setRelationshipProperties(request, from, to);
			try {
				Boolean areNodesConnected = nodeService.connect(from, to, relationshipProperties);
				if (areNodesConnected) {
					String firstName = helperMethodService.fetchUserFirstName(request.getUserIdFrom());
					Map<String, Object> data = new HashMap<>();
					data.put("id", request.getUserIdFrom());
					data.put("user_id",request.getUserIdTo());
					if (request.getStatus().equalsIgnoreCase(Constants.Status.PENDING)) {
						notificationTriggerService.triggerNotification(Constants.SEND_CONNECTION_REQUEST, Constants.ALERT,
								Arrays.asList(request.getUserIdTo()), firstName, data);
					} else if (request.getStatus().equalsIgnoreCase(Constants.Status.APPROVED)) {
						notificationTriggerService.triggerNotification(Constants.ACCEPTED_CONNECTION_REQUEST, Constants.ALERT,
								Arrays.asList(request.getUserIdTo()), firstName, data);

					} else {
						logger.info("No need to send notification.");
					}
					response.put(Constants.ResponseStatus.MESSAGE, Constants.ResponseStatus.SUCCESSFUL);
					response.put(Constants.ResponseStatus.STATUS, HttpStatus.CREATED);
				} else {
					relationshipProperties.put(Constants.STATUS, Constants.FAILED);
					response.put(Constants.ResponseStatus.STATUS, HttpStatus.INTERNAL_SERVER_ERROR);
				}
				if (connectionProperties.isNotificationEnabled()) {
					sendNotification(connectionProperties.getNotificationTemplateRequest(), from.getUserId(), to.getUserId(),
							relationshipProperties.get(Constants.STATUS));
				}
			} catch (ValidationException ve) {
				response.put(Constants.ResponseStatus.STATUS, HttpStatus.BAD_REQUEST);
			} catch (Exception e) {
				response.put(Constants.ResponseStatus.STATUS, HttpStatus.INTERNAL_SERVER_ERROR);
				logger.error(String.format("Error while connecting the nodes! error : %s", e.getMessage()));
			}
		}
		return response;
	}

	@Override
	public boolean validateRequest(ConnectionRequest request) {
		return (!request.getUserIdFrom().isEmpty() && !request.getUserIdTo().isEmpty())
				&& !request.getUserIdFrom().equals(request.getUserIdTo());
	}

	@Override
	public void sendNotification(String eventId, String sender, String reciepient, String status) {
		NotificationEvent event = notificationService.buildEvent(eventId, sender, reciepient, status);
		notificationService.postEvent(event);
	}

	@Override
	public List<String> findUserConnectionsV2(String userId, String status) throws Exception {

		Map<String, String> relationProperties = new HashMap<>();
		relationProperties.put(Constants.Graph.STATUS.getValue(), status);
		return nodeService
				.getNodes(userId, relationProperties, null, 0, connectionProperties.getMaxNodeSize(),
						Arrays.asList(Constants.Graph.ID.getValue()))
				.stream().map(Node::getUserId).collect(Collectors.toList());

	}

	@Override
	public Map<String, String> setRelationshipProperties(ConnectionRequest request, Node from, Node to) {
		Map<String, String> relP = new HashMap<>();
		relP.put(Constants.Graph.CONNECTION_ID.getValue(), request.getConnectionId());
		relP.put(Constants.Graph.STATUS.getValue(), request.getStatus());
		if (request.getCreatedAt() != null) {
			relP.put(Constants.Graph.CREATED_AT.getValue(), request.getCreatedAt());
			from.setCreatedAt(request.getCreatedAt());
			to.setCreatedAt(request.getCreatedAt());
		}
		if (request.getUpdatedAt() != null) {
			relP.put(Constants.Graph.UPDATED_AT.getValue(), request.getUpdatedAt());
			from.setUpdatedAt(request.getUpdatedAt());
			to.setUpdatedAt(request.getUpdatedAt());
		}
		return relP;
	}

	@Override
	public Response findSuggestedConnectionsV2(String userId, int offset, int limit) {

		Response response = new Response();
		try {
			if (userId == null || userId.isEmpty()) {
				throw new BadRequestException(Constants.Message.USER_ID_INVALID);
			}
			Map<String, String> relationProperties = new HashMap<>();
			relationProperties.put(Constants.Graph.STATUS.getValue(), Constants.Status.APPROVED);
			List<Node> nodes = nodeService.getNodeNextLevel(userId, relationProperties, offset, limit);

			List<String> allNodesIds = findUserConnectionsV2(userId, Constants.Status.APPROVED);
			List<Node> detachedNodes = nodes.stream().filter(node -> !allNodesIds.contains(node.getUserId()))
					.collect(Collectors.toList());

			response.put(Constants.ResponseStatus.MESSAGE, Constants.ResponseStatus.SUCCESSFUL);
			response.put(Constants.ResponseStatus.DATA, enrichUserInfo(detachedNodes));
			response.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);

		} catch (Exception e) {
			logger.error("ConnectionService::findSuggestedConnectionsV2 " , e);
			throw new ApplicationException(Constants.Message.FAILED_CONNECTION + e.getMessage());

		}

		return response;
	}

	@Override
	public Response findAllConnectionsIdsByStatusV2(String userId, String status, int offset, int limit) {
		Response response = new Response();

		try {
			if (userId == null || userId.isEmpty()) {
				throw new BadRequestException(Constants.Message.USER_ID_INVALID);
			}
			String nodeCacheKey = Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_ESTABLISHED + Constants.UNDER_SCORE + userId;
			int cacheTtl = connectionProperties.getRedisUserConnectionEstablishedTimeOut();
			String cachedNodesJson = redisCacheMgr.getCache(nodeCacheKey);
			List<Map<String, Object>> cachedNodes;
			Integer cachedCount=0;
			if (StringUtils.isNotEmpty(cachedNodesJson)) {
				cachedNodes = objectMapper.readValue(cachedNodesJson, new TypeReference<List<Map<String, Object>>>() {});
			} else {
				Map<String, String> relationProperties = new HashMap<>();
				relationProperties.put(Constants.Graph.STATUS.getValue(), status);
				List<Node> nodes = nodeService.getNodes(userId, relationProperties, null, offset, limit, null);
				List<Map<String, String>> userList = nodes.stream()
						.map(node -> {
							Map<String, String> map = new HashMap<>();
							map.put(Constants.USER_ID, node.getUserId());
							map.put(Constants.CREATED_AT, node.getCreatedAt());
							map.put(Constants.UPDATED_AT, node.getUpdatedAt());
							map.put(Constants.STATUS, node.getStatus());
							return map;
						})
						.collect(Collectors.toList());
				cachedNodes = profileService.enrichNeo4JDataForRecommendataion(userList);
				redisCacheMgr.putCache(nodeCacheKey, objectMapper.writeValueAsString(cachedNodes), cacheTtl);
			}
			Map<String, Integer> userCount = nodeService.getConnectionsCountByStatus(userId, Constants.Status.PENDING, null);
			cachedCount = userCount.get(Constants.COUNT);
			if (cachedCount == null) {
				cachedCount = 0;
			}
			response.put(Constants.COUNT, cachedCount);
			response.put(Constants.ResponseStatus.PAGENO, offset);
			response.put(Constants.ResponseStatus.MESSAGE, Constants.ResponseStatus.SUCCESSFUL);
			response.put(Constants.ResponseStatus.DATA, cachedNodes);
			response.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);
		} catch (Exception e) {
			logger.error("ConnectionService::findAllConnectionsIdsByStatusV2 " , e);
			throw new ApplicationException(Constants.Message.FAILED_CONNECTION + e.getMessage());
		}

		return response;
	}

	@Override
	public Response findConnectionsRequestedV2(String userId, int offset, int limit, Constants.DIRECTION direction) {
		Response response = new Response();
		logger.info("findConnectionsRequestedV2 called for userId: {}, direction: {}, offset: {}, limit: {}", userId, direction, offset, limit);
		try {
			//Input validation
			if (userId == null || userId.isEmpty()) {
				throw new BadRequestException(Constants.Message.USER_ID_INVALID);
			}
			//Prepare cache keys and TTL based on direction
			String nodeCacheKey;
			String countCacheKey;
			int cacheTtl;
			if (direction == Constants.DIRECTION.OUT) {
				nodeCacheKey = Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_REQUESTED + Constants.UNDER_SCORE + userId;
				cacheTtl = connectionProperties.getRedisUserConnectionRequestedTimeOut();
			} else {
				nodeCacheKey = Constants.USER_LIST + Constants.UNDER_SCORE + Constants.CONNECTION_RECIEVED + Constants.UNDER_SCORE + userId;
				cacheTtl = connectionProperties.getRedisUserConnectionRecievedTimeOut();
			}
			logger.debug("Cache keys - nodeCacheKey: {}", nodeCacheKey);
			//Attempt to fetch node list and count from cache
			String cachedNodesJson = redisCacheMgr.getCache(nodeCacheKey);
			List<Map<String, Object>> cachedNodes;
			Integer cachedCount = 0;
			//If both are cached, use cache
			if (StringUtils.isNotEmpty(cachedNodesJson)) {
				logger.info("Cache hit for userId: {} (direction: {}). Returning cached data.", userId, direction);
				cachedNodes = objectMapper.readValue(cachedNodesJson, new TypeReference<List<Map<String, Object>>>() {
				});
			} else {
				//If cache miss, fetch from DB and cache the results
				logger.info("Cache miss for userId: {} (direction: {}). Fetching from DB.", userId, direction);
				Map<String, String> relationProperties = new HashMap<>();
				relationProperties.put(Constants.Graph.STATUS.getValue(), Constants.Status.PENDING);
				List<Node> nodes = nodeService.getNodes(userId, relationProperties, direction, offset, limit, null);
				List<Map<String, String>> userList = nodes.stream()
						.map(node -> {
							Map<String, String> map = new HashMap<>();
							map.put(Constants.USER_ID, node.getUserId());
							map.put(Constants.CREATED_AT, node.getCreatedAt());
							map.put(Constants.UPDATED_AT, node.getUpdatedAt());
							map.put(Constants.STATUS, node.getStatus());
							return map;
						})
						.collect(Collectors.toList());
				cachedNodes = profileService.enrichNeo4JDataForRecommendataion(userList);
				// Cache the results (including empty/zero)
				redisCacheMgr.putCache(nodeCacheKey, cachedNodes, cacheTtl);
				logger.debug("Caching node list and count for userId: {} (direction: {}) with TTL: {}", userId, direction, cacheTtl);
			}
			Map<String, Integer> userCount = nodeService.getConnectionsCountByStatus(userId, Constants.Status.PENDING, direction);
			cachedCount = userCount.get(Constants.COUNT);
			if (cachedCount == null) {
				cachedCount = 0;
			}
			// Build and return the response
			response.put(Constants.COUNT, cachedCount);
			response.put(Constants.ResponseStatus.MESSAGE, Constants.ResponseStatus.SUCCESSFUL);
			response.put(Constants.ResponseStatus.DATA, cachedNodes);
			response.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);
			//Log method completion
			logger.info("findConnectionsRequestedV2 completed for userId: {} (direction: {})", userId, direction);
		} catch (Exception e) {
			logger.error("ConnectionService::findConnectionsRequestedV2 ", e);
			throw new ApplicationException(Constants.Message.FAILED_CONNECTION + e.getMessage());
		}

		return response;
	}

	private Collection<Node> enrichUserInfo(List<Node> nodes) {
		if(nodes == null || nodes.size() == 0) {
			return nodes;
		}

		logger.info("ConnectionService... enrichUserInfo... node size : " + nodes.size());
		List<String> userIds = nodes.stream().map(Node::getUserId).collect(Collectors.toList());
		Map<String, Node> nodeMap = nodes.stream().collect(Collectors.toMap(Node::getUserId, node -> node));

		List<String> fields = Arrays.asList(Constants.ID, Constants.FIRST_NAME, Constants.STATUS, Constants.CHANNEL,Constants.PROFILE_DETAILS);
		Map<String, Object> propertyMap = new HashMap<>();
		int loopSize = 50;
		for (int i = 0; i < userIds.size(); i += loopSize) {
			List<String> userList = userIds.subList(i, Math.min(userIds.size(), i + loopSize));
			propertyMap.put(Constants.ID, userList);

			try {
				List<Map<String, Object>> userInfoList = cassandraOperation
						.getRecordsByProperties(Constants.KEYSPACE_SUNBIRD, Constants.TABLE_USER, propertyMap, fields);
				for (Map<String, Object> user : userInfoList) {
					String userId = (String) user.get(Constants.ID);
					Integer status = (Integer) user.get(Constants.STATUS);
					if (nodeMap.containsKey(userId)) {
						if (status == 0) {
							nodeMap.remove(userId);
						} else {
							Node node = nodeMap.get(userId);
							node.setFullName((String) user.get(Constants.FULL_NAME));
							node.setDepartmentName((String) user.get(Constants.CHANNEL));
							node.setId(userId);
							JsonNode root = objectMapper.readTree((String) user.get(Constants.PROFILE_DETAILS));
							if (root != null) {
								if (root.hasNonNull(Constants.PROFESSIONAL_DETAILS)) {
									List<Map<String, Object>> professionalDetails = objectMapper.readValue(
											root.get(Constants.PROFESSIONAL_DETAILS).toString(),
											new TypeReference<List<Map<String, Object>>>() {}
									);
									node.setProfessionalDetails(professionalDetails);
								}
								if (root.hasNonNull(Constants.EMPLOYMENT_DETAILS)) {
									Map<String, Object> employmentDetails = objectMapper.readValue(
											root.get(Constants.EMPLOYMENT_DETAILS).toString(),
											new TypeReference<Map<String, Object>>() {}
									);
									node.setEmploymentDetails(employmentDetails);
								}
								if (root.hasNonNull(Constants.PROFILE_IMAGE_URL)) {
									String profileImageUrl = root.get(Constants.PROFILE_IMAGE_URL).asText();
									node.setProfileImageUrl(profileImageUrl);
								}
								if (root.hasNonNull(Constants.PROFILE_BANNER_URL)) {
									String profileBannerUrl = root.get(Constants.PROFILE_BANNER_URL).asText();
									node.setProfileBannerUrl(profileBannerUrl);
								}
							}
						}
					}
				}
			} catch (Exception e) {
				logger.error("Failed to enrich user info... Exception: " + e.getMessage(), e);
			}
		}
		return sortNodesByDate(nodeMap.values());
	}

	@Override
	public Map<String, String> getRelationshipBetweenUsers(String fromUserId, String toUserId) {
		try {
			return nodeService.getRelationshipBetweenUsers(fromUserId, toUserId);
		} catch (Exception e) {
			logger.error(String.format("Error fetching relationship between %s and %s : %s", fromUserId, toUserId, e));
			return new HashMap<>();
		}
	}

	/**
	 * Fetches recommendations for a user based on the provided request parameters.
	 *
	 * @param userId   The ID of the user for whom recommendations are to be fetched.
	 * @param request  A map containing request parameters for fetching recommendations.
	 * @return A list of maps containing recommendation data for the user.
	 */
	@Override
	public  List<Map<String, String>> findRecommendationForUser(String userId, Map<String, Object> request) {
		try {
			return nodeService.findRecommendationForUser(userId,request);
		} catch (Exception e) {
			logger.error(String.format("ConnectionService: findRecommendationForUser:Error fetching Recommendations for user %s %s", userId, e));
			return new ArrayList<>();
		}
	}

    /**
     * Fetches recommendations for mentors based on the provided request parameters.
     *
     * @param userId  The ID of the user for whom mentor recommendations are to be fetched.
     * @param request A map containing request parameters for fetching mentor recommendations.
     * @return A list of maps containing mentor recommendation data for the user.
     */
    @Override
    public List<Map<String, String>> findRecommendationForMentors(String userId, Map<String, Object> request) {
        try {
            return nodeService.findRecommendationForMentors(userId, request);
        } catch (Exception e) {
            logger.error(String.format("ConnectionService:findRecommendationForMentors: Error fetching Mentor Recommendations for user %s %s", userId, e));
            return new ArrayList<>();
        }
    }


	/**
	 * Fetches a list of blocked users for a given user based on the provided request parameters.
	 *
	 * @param userId   The ID of the user for whom blocked users are to be fetched.
	 * @param request  A map containing request parameters for fetching blocked users.
	 * @return A list of maps containing data about blocked users.
	 */
	@Override
	public List<Map<String, String>> findBlockedUsers(String userId, Map<String, Object> request) {
		try {
			return nodeService.findBlockedUsers(userId, request);
		} catch (Exception e) {
			logger.error(String.format("ConnectionService:findBlockedUsers:Error fetching Blocked users data %s %s", userId, e));
			return new ArrayList<>();
		}
	}

	/**
	 * Retrieves the count of recommended users for a given user.
	 *
	 * @param userId The ID of the user for whom the count of recommended users is to be fetched.
	 * @return The count of recommended users for the specified user.
	 */
	@Override
	public Integer getCountForRecommendedUsers(String userId) {
		try {
			return nodeService.getCountForRecommendedUsers(userId);
		} catch (Exception e) {
			logger.error(String.format("ConnectionService:getCountForRecommendedUsers:Error fetching Count for Recommended users data %s %s", userId, e));
			return 0;
		}
	}

	/**
	 * Retrieves the count of recommended mentors for a given user.
	 *
	 * @param userId The ID of the user for whom the count of recommended mentors is to be fetched.
	 * @return The count of recommended mentors for the specified user.
	 */
	@Override
	public Integer getCountForRecommendedMentors(String userId) {
		try {
			return nodeService.getCountForRecommendedMentors(userId);
		} catch (Exception e) {
			logger.error(String.format("ConnectionService:getCountForRecommendedMentors:Error fetching Count for Recommended Mentors data %s %s", userId, e));
			return 0;
		}
	}

	/**
	 * Retrieves the total count of users based on their status for a given user.
	 *
	 * @param userId     The ID of the user for whom the count is to be fetched.
	 * @param statusList A list of statuses to filter the users (e.g., "pending", "accepted").
	 * @return A list of maps containing the total count of users based on their status.
	 */
	@Override
	public List<Map<String, Object>> getTotalCountForUsersBasedOnStatus(String userId, List<String> statusList, String facet) {
		try {
			return nodeService.getTotalCountForUsersBasedOnStatus(userId, statusList, facet);
		} catch (Exception e) {
			logger.error(String.format("ConnectionService:findBlockedUsers:Error fetching Blocked users data %s %s", userId, e));

		}
		return new ArrayList<>();
	}


	/**
	 * Updates the user profile in Neo4j.
	 *
	 * @param node The Node object containing user profile details to be updated.
	 * @return A boolean indicating whether the update was successful or not.
	 */
	@Override
	public boolean updateUserProfileInNeo4j(Node node) {
		return nodeService.updateUserProfileInNeo4j(node);
	}

	/**
	 * Sorts a collection of nodes by their date attributes (updatedAt or createdAt).
	 *
	 * @param nodes The collection of nodes to be sorted.
	 * @return A sorted collection of nodes based on their date attributes.
	 */
	private Collection<Node> sortNodesByDate(Collection<Node> nodes) {
		SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_SORTING, Locale.ENGLISH);
		return nodes.stream()
				.sorted((n1, n2) -> {
					Date date1 = getRelevantDate(n1, sdf);
					Date date2 = getRelevantDate(n2, sdf);
					if (date1 == null && date2 == null) return 0;
					if (date1 == null) return 1;
					if (date2 == null) return -1;
					return date2.compareTo(date1); // descending order
				})
				.collect(Collectors.toList());
	}

	/**
	 * Retrieves the relevant date for a node based on its createdAt and updatedAt attributes.
	 *
	 * @param node The node for which the date is to be retrieved.
	 * @param sdf  The SimpleDateFormat used for parsing date strings.
	 * @return The most relevant date (either createdAt or updatedAt) or null if both are not available.
	 */
	private Date getRelevantDate(Node node, SimpleDateFormat sdf) {
		Date createdDate = null;
		Date updatedDate = null;
		try {
			if (StringUtils.isNotEmpty(node.getCreatedAt())) {
				createdDate = sdf.parse(node.getCreatedAt());
			}
		} catch (Exception e) {
			logger.error(String.format("Error parsing createdAt for node %s: %s", node.getUserId(), e.getMessage()));
		}
		try {
			if (StringUtils.isNotEmpty(node.getUpdatedAt())) {
				updatedDate = sdf.parse(node.getUpdatedAt());
			}
		} catch (Exception e) {
			logger.error(String.format("Error parsing updatedAt for node %s: %s", node.getUserId(), e.getMessage()));
		}
		if (createdDate == null) return updatedDate;
		if (updatedDate == null) return createdDate;
		return createdDate.after(updatedDate) ? createdDate : updatedDate;
	}
}