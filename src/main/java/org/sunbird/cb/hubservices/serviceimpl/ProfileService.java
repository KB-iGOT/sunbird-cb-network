package org.sunbird.cb.hubservices.serviceimpl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.common.recycler.Recycler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.sunbird.cb.hubservices.cache.RedisCacheMgr;
import org.sunbird.cb.hubservices.cassandra.CassandraOperation;
import org.sunbird.cb.hubservices.common.auth.AccessTokenValidator;
import org.sunbird.cb.hubservices.common.util.ProjectUtil;
import org.sunbird.cb.hubservices.exception.ApplicationException;
import org.sunbird.cb.hubservices.model.MultiSearch;
import org.sunbird.cb.hubservices.model.Node;
import org.sunbird.cb.hubservices.model.Response;
import org.sunbird.cb.hubservices.model.SBApiResponse;
import org.sunbird.cb.hubservices.service.IConnectionService;
import org.sunbird.cb.hubservices.service.INodeService;
import org.sunbird.cb.hubservices.service.IProfileService;
import org.sunbird.cb.hubservices.service.IUserUtility;
import org.sunbird.cb.hubservices.util.ConnectionProperties;
import org.sunbird.cb.hubservices.util.Constants;
import org.sunbird.cb.hubservices.util.NetworkServerProperties;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfileService implements IProfileService {

	private Logger logger = LoggerFactory.getLogger(ProfileService.class);

	@Autowired
	IConnectionService connectionService;


	@Autowired
	IUserUtility iUserUtility;

	@Autowired
	AccessTokenValidator accessTokenValidator;

	@Autowired
	RedisCacheMgr redisCacheMgr;

	@Autowired
	NetworkServerProperties networkServerProperties;

	@Autowired
	ObjectMapper mapper;

	@Autowired
	INodeService nodeService;

	@Autowired
	UserUtilityService userUtilityService;

	@Autowired
	private ConnectionProperties connectionProperties;


    @Autowired
    private CassandraOperation cassandraOperation;

	@Override
	public Response findCommonProfileV2(String userId, int offset, int limit) {
		return connectionService.findSuggestedConnectionsV2(userId, offset, limit);
	}

	@Override
	public Response findProfilesV2(String userId, int offset, int limit) {
		return connectionService.findAllConnectionsIdsByStatusV2(userId, Constants.Status.APPROVED, offset, limit);

	}

	@Override
	public Response findProfileRequestedV2(String userId, int offset, int limit, Constants.DIRECTION direction) {
		return connectionService.findConnectionsRequestedV2(userId, offset, limit, direction);

	}

	@Override
	public Response multiSearchProfiles(String userId, MultiSearch mSearchRequest, String[] sourceFields) {

		Response response = new Response();
		try {
			List<String> connectionIdsToExclude = connectionService.findUserConnectionsV2(userId,
					Constants.Status.APPROVED);
			List<String> connectionIdsToExcludeForPending = connectionService.findUserConnectionsV2(userId,
					Constants.Status.PENDING);
			connectionIdsToExclude.add(userId);
			connectionIdsToExclude.addAll(connectionIdsToExcludeForPending);
			logger.info("multi search request :: {}", mSearchRequest.toString());

			Map<String, Object> tagRes = iUserUtility.getUserInfoFromRedish(mSearchRequest, sourceFields, connectionIdsToExclude);
			List<Object> finalRes = new ArrayList<>();
			for (Map.Entry entry : tagRes.entrySet()) {
				Map<String, Object> resObjects = new HashMap<>();
				resObjects.put("field", entry.getKey());
				resObjects.put("results", entry.getValue());
				finalRes.add(resObjects);
			}

			response.put(Constants.ResponseStatus.MESSAGE, Constants.ResponseStatus.SUCCESSFUL);
			response.put(Constants.ResponseStatus.DATA, finalRes);
			response.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);

		} catch (Exception e) {
			logger.error(Constants.Message.CONNECTION_EXCEPTION_OCCURED, e);
			throw new ApplicationException(Constants.Message.FAILED_CONNECTION);

		}

		return response;
	}

	@Override
	public SBApiResponse getRelationshipBetweenUsers(String toUserId, String authToken) {
		SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_USER_RELATIONSHIP);
		String fromUserId = "";
		try {
			fromUserId = accessTokenValidator.fetchUserIdFromAccessToken(authToken, response);
			if (StringUtils.isEmpty(fromUserId)) {
				return response;
			}
			Map<String, String> map = connectionService.getRelationshipBetweenUsers(fromUserId,
					toUserId);
			response.getResult().put("response",map);
			response.getParams().setStatus(Constants.OK);
			response.setResponseCode(HttpStatus.OK);
			return response;
		} catch (Exception e) {
			logger.error(String.format("Error fetching relationship between  in %s %s: %s", fromUserId, toUserId, e));
			response.getParams().setStatus(HttpStatus.INTERNAL_SERVER_ERROR.toString());
			response.getParams().setErrmsg("Error fetching relationship between users");
			response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			return response;
		}
	}

	/**
	 * This method fetches recommendations for the user based on their connections.
	 * It validates the access token, checks pagination parameters, and retrieves
	 * recommended users from the connection service.
	 *
	 * @param authToken The authentication token of the user.
	 * @param request   The request map containing pagination parameters.
	 * @return SBApiResponse containing the list of recommended users or an error message.
	 */
	@Override
	public SBApiResponse findRecommendations(String authToken, Map<String, Object> request) {
		SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_GET_USER_RECOMMENDATIONS_V2);
		String userId = "";
		try {
			userId = accessTokenValidator.fetchUserIdFromAccessToken(authToken, response);
			if (StringUtils.isEmpty(userId)) {
				return response;
			}
			if (validatePaginationParams(request, response)) {
				return response;
			}
			int count = getCountForRecommendedUsers(userId);
			if(count == 0){
				logger.info("ProfileService : findRecommendations : Recommended Users Count is 0 for userId: {}", userId);
				response.getParams().setStatus(HttpStatus.OK.toString());
				response.getResult().put(Constants.MESSAGE,"Recommended users count is empty");
				response.setResponseCode(HttpStatus.OK);
				return response;
			}
			return fetchAndCacheRecommendedUsers(request, userId, response, count);
		} catch (Exception e) {
			logger.error(String.format("ProfileService:findRecommendations:Error while fetching recommendation for the user %s %s", userId, e));
			response.getParams().setStatus(HttpStatus.INTERNAL_SERVER_ERROR.toString());
			response.getParams().setErrmsg("Error while fetching recommendation for the user");
			response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			return response;
		}
	}

	/**
	 * Validates the pagination parameters in the request map.
	 * Checks if the request is not empty, contains 'offset' and 'size',
	 * and ensures they are integers.
	 *
	 * @param request  The request map containing pagination parameters.
	 * @param response The SBApiResponse to set error messages and status.
	 * @return true if validation passes, false otherwise.
	 */
	private boolean validatePaginationParams(Map<String, Object> request, SBApiResponse response) {
		if (MapUtils.isEmpty(request)) {
			response.getParams().setStatus(HttpStatus.BAD_REQUEST.toString());
			response.getParams().setErrmsg("Request body cannot be null");
			response.setResponseCode(HttpStatus.BAD_REQUEST);
			return true;
		}
		if (!request.containsKey("offset") || !request.containsKey("size")) {
			response.getParams().setStatus(HttpStatus.BAD_REQUEST.toString());
			response.getParams().setErrmsg("Missing required parameters: offset and size");
			response.setResponseCode(HttpStatus.BAD_REQUEST);
			return true;
		}
		Object offsetObj = request.get("offset");
		Object sizeObj = request.get("size");
		if (!(offsetObj instanceof Integer) || !(sizeObj instanceof Integer)) {
			response.getParams().setStatus(HttpStatus.BAD_REQUEST.toString());
			response.getParams().setErrmsg("Parameters offset and size must be integers");
			response.setResponseCode(HttpStatus.BAD_REQUEST);
			return true;
		}
		return false;
	}

	/**
	 * This method fetches recommendations for mentors based on the Users Organisation.
	 * It validates the access token, checks pagination parameters, and retrieves
	 * recommended mentors from the connection service.
	 *
	 * @param authToken The authentication token of the user.
	 * @param request   The request map containing pagination parameters.
	 * @return SBApiResponse containing the list of recommended mentors or an error message.
	 */
	@Override
	public SBApiResponse findRecommendedMentors(String authToken, Map<String, Object> request) {
		SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_USER_MENTOR_RECOMMENDATIONS);
		String userId = "";
		try {
			userId = accessTokenValidator.fetchUserIdFromAccessToken(authToken, response);
			if (StringUtils.isEmpty(userId)) {
				return response;
			}
			if (validatePaginationParams(request, response)) {
				return response;
			}
			Integer count = connectionService.getCountForRecommendedMentors(userId);
			if(count == 0){
				logger.info("ProfileService : findRecommendations : Recommended Mentors Count is 0 for userId: {}", userId);
				response.getParams().setStatus(HttpStatus.OK.toString());
				response.getResult().put(Constants.MESSAGE,"Recommended Mentors count is empty");
				response.setResponseCode(HttpStatus.OK);
				return response;
			}
			List<Map<String, String>> recommendationMentorsList = connectionService.findRecommendationForMentors(userId, request);
			if(CollectionUtils.isEmpty(recommendationMentorsList)){
				logger.info("ProfileService : findRecommendedMentors : Recommendation Mentors List is empty for userId: {}", userId);
				response.getParams().setStatus(HttpStatus.OK.toString());
				response.getResult().put(Constants.MESSAGE,"No recommendations found for the user");
				response.setResponseCode(HttpStatus.OK);
				return response;
			}
			enrichUserInformation(recommendationMentorsList, response,userId,Constants.MENTORS);
			response.getResult().put(Constants.COUNT, count);
			return response;
		} catch (Exception e) {
			logger.error(String.format("ProfileService : findRecommendedMentors :Error while fetching recommendation for the user %s %s", userId, e));
			response.getParams().setStatus(HttpStatus.INTERNAL_SERVER_ERROR.toString());
			response.getParams().setErrmsg("Error while fetching recommendation for the user");
			response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			return response;
		}

	}

	/**
	 * Enriches user information by fetching additional details from Redis based on the user IDs
	 * present in the provided userList. It constructs a MultiSearch request and retrieves user info.
	 *
	 * @param userList The list of users to enrich.
	 * @param response The SBApiResponse to populate with enriched user information.
	 * @return SBApiResponse containing enriched user information.
	 */
	private SBApiResponse enrichUserInformation(List<Map<String, String>> userList, SBApiResponse response,String userId, String type) {
		List<String> connectionUserIds = new ArrayList<>();
		ArrayNode enrichedUserMap;
		Map<String,Map<String,Object>> userInfoMap = new HashMap<>();
		extractUserDetails(userList, connectionUserIds, userInfoMap);
		MultiSearch mSearchRequest = new MultiSearch();
		String userInformation = redisCacheMgr.getCache(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS + Constants.UNDER_SCORE + type + Constants.UNDER_SCORE + userId);
		if (!ObjectUtils.isEmpty(userInformation)) {
			JsonNode jsonNode;
			try {
				jsonNode = mapper.readTree(userInformation);
			} catch (IOException e) {
				logger.error("ProfileService :enrichUserInformation: Error reading user information from Redis cache", e);
				response.getParams().setStatus(HttpStatus.INTERNAL_SERVER_ERROR.toString());
				response.getResult().put(Constants.RESPONSE, "Error reading user information from cache");
				response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
				return response;
			}
			enrichedUserMap = fetchUserDataNotAvailableInRedisCache(userId, type, jsonNode, connectionUserIds, mSearchRequest, userInfoMap);
		} else {
			enrichedUserMap = iUserUtility.getUserInfoFromRedisV2(mSearchRequest, connectionUserIds,userInfoMap);
			if (enrichedUserMap.size() > 1)
				redisCacheMgr.putCache(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS + Constants.UNDER_SCORE + type + Constants.UNDER_SCORE + userId, enrichedUserMap, networkServerProperties.getRedisUserListReadTimeOut());
		}
		response.getResult().put(Constants.RESPONSE, enrichedUserMap);
		response.getParams().setStatus(Constants.OK);
		response.setResponseCode(HttpStatus.OK);
		return response;
	}


	/** This method fetches the list of blocked users for the authenticated user.
	 * It validates the access token, checks pagination parameters, and retrieves
	 * blocked users from the connection service.
	 *
	 * @param authToken The authentication token of the user.
	 * @param request   The request map containing pagination parameters.
	 * @return SBApiResponse containing the list of blocked users or an error message.
	 */
	@Override
	public SBApiResponse findBlockedUsers(String authToken, Map<String, Object> request) {
		SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_GET_BLOCKED_USERS);
		String userId = "";
		try {
			userId = accessTokenValidator.fetchUserIdFromAccessToken(authToken, response);
			if (StringUtils.isEmpty(userId)) {
				return response;
			}
			if (validatePaginationParams(request, response)) {
				return response;
			}
			List<Map<String, String>> blockedUsersList = connectionService.findBlockedUsers(userId, request);
			if(CollectionUtils.isEmpty(blockedUsersList)){
				logger.info("ProfileService : findBlockedUsers : Blocked Users List is empty for userId: {}", userId);
				response.getParams().setStatus(HttpStatus.OK.toString());
				response.getResult().put(Constants.MESSAGE,"Blocked users list is empty");
				response.setResponseCode(HttpStatus.OK);
				return response;
			}
			Map<String, Integer> userCount = nodeService.getConnectionsCountByStatus(userId, Constants.Status.BLOCKED, Constants.DIRECTION.OUT);
			response.put(Constants.COUNT, userCount.get(Constants.COUNT));
			return enrichUserInformation(blockedUsersList, response,userId,Constants.BLOCKED_USERS);
		}
		catch (Exception e){
			logger.error(String.format("ProfileService : findBlockedUsers : Error while fetching blocked user %s %s", userId, e));
			response.getParams().setStatus(HttpStatus.INTERNAL_SERVER_ERROR.toString());
			response.getParams().setErrmsg("Error while fetching blocked user data");
			response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			return response;
		}
	}

	/**
	 * Extracts user details from the provided userList and populates the connectionUserIds and userInfoMap.
	 * It processes each user map, extracting the user ID and other relevant details, including roles.
	 *
	 * @param userList          The list of user maps containing user details.
	 * @param connectionUserIds The list to populate with user IDs.
	 * @param userInfoMap       The map to populate with user details excluding the user ID.
	 */
	private static void extractUserDetails(List<Map<String, String>> userList, List<String> connectionUserIds, Map<String, Map<String, Object>> userInfoMap) {
		userList.forEach(userMap -> {
			if (userMap.containsKey(Constants.USER_ID)) {
				connectionUserIds.add(userMap.get(Constants.USER_ID));
				Map<String, Object> userDetails = new HashMap<>();
				userMap.forEach((key, value) -> {
					if (!key.equals(Constants.USER_ID)) {
						if (key.equals(Constants.ROLE) && value != null) {
							List<String> rolesList = Arrays.asList(value.split(","));
							userDetails.put(key, rolesList);
						} else {
							userDetails.put(key, value);
						}
					}
				});
				userInfoMap.put(userMap.get(Constants.USER_ID), userDetails);
			}
		});
	}
	

	/**
	 * Fetches user data that is not available in the Redis cache.
	 * It retrieves the user IDs from the provided JSON node, checks for missing user IDs,
	 * and fetches user information from Redis. If new user data is found, it updates the cache.
	 *
	 * @param userId            The ID of the user for whom recommendations are being fetched.
	 * @param type              The type of users (e.g., recommended, blocked).
	 * @param jsonNode          The JSON node containing user data.
	 * @param connectionUserIds The list of connection user IDs to check against.
	 * @param mSearchRequest    The MultiSearch request object for fetching additional user info.
	 * @param userInfoMap       The map to store user information.
	 * @return ArrayNode containing enriched user data.
	 */
	private ArrayNode fetchUserDataNotAvailableInRedisCache(String userId, String type, JsonNode jsonNode, List<String> connectionUserIds, MultiSearch mSearchRequest, Map<String, Map<String, Object>> userInfoMap) {
		ArrayNode enrichedUserMap;
		enrichedUserMap = (ArrayNode) jsonNode;
		List<String> userIds = new ArrayList<>();
		if (jsonNode != null) {
			for (JsonNode n : jsonNode) {
				userIds.add(n.get(Constants.USER_ID).asText());
			}
		}
		if(connectionUserIds.equals(userIds)){
			logger.info("ProfileService : fetchUserDataNotAvailableInRedisCache : No new user data found in Redis cache for userId: {}", userId);
			return enrichedUserMap;
		}
		ArrayNode userMap = iUserUtility.getUserInfoFromRedisV2(mSearchRequest, connectionUserIds, userInfoMap);
		if (userMap != null && userMap.size() > 0) {
			ArrayNode newEnrichedUserMap = mapper.createArrayNode();
			newEnrichedUserMap.addAll(userMap);
			redisCacheMgr.deleteKeyByName(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS + Constants.UNDER_SCORE + type + Constants.UNDER_SCORE + userId);
			redisCacheMgr.putCache(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS + Constants.UNDER_SCORE + type + Constants.UNDER_SCORE + userId, newEnrichedUserMap, networkServerProperties.getRedisUserListReadTimeOut());
			return newEnrichedUserMap;
		}
		return enrichedUserMap;
	}


	/**
	 * This method fetches the total connections count by status for the authenticated user.
	 * It validates the access token, checks the request parameters, and retrieves the count
	 * of connections based on the specified status.
	 *
	 * @param authToken The authentication token of the user.
	 * @param request   The request map containing filter criteria and facets.
	 * @return SBApiResponse containing the total connections count by status or an error message.
	 */
	@Override
	public SBApiResponse fetchTotalConnectionsCountByStatus(String authToken, Map<String, Object> request) {
		SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_GET_TOTAL_CONNECTIONS_COUNT_BY_STATUS);
		String userId = "";
		try {
			userId = accessTokenValidator.fetchUserIdFromAccessToken(authToken, response);
			if (StringUtils.isEmpty(userId)) {
				return response;
			}
			if (validateTotalConnectionCountByStatusParams(request, response)) {
				return response;
			}
			Map<String, Object> requestBodyMap = (Map<String, Object>) request.get(Constants.REQUEST);
			Map<String, Object> filterMap = (Map<String, Object>) requestBodyMap.get(Constants.FILTER);
			List<String> statusList = (List<String>) filterMap.get(Constants.STATUS);
			List<String> facets = (List<String>) requestBodyMap.get(Constants.FACETS);
			List<Map<String, Object>> list = new ArrayList<>();
			for (String facet : facets) {
				if (Constants.STATUS.equalsIgnoreCase(facet)) {
					list.addAll(connectionService.getTotalCountForUsersBasedOnStatus(userId, statusList, facet));
				}
			}
			response.put(Constants.FACETS, list);
			return response;
		} catch (Exception e) {
			logger.error(String.format("ProfileService : findTotalConnectionsCountByStatus : Error while fetching user connections count by status %s %s", userId, e));
			response.getParams().setStatus(HttpStatus.INTERNAL_SERVER_ERROR.toString());
			response.getParams().setErrmsg("Error while fetching user connections count by status");
			response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			return response;
		}
	}

	/**
	 * Validates the parameters for fetching total connections count by status.
	 * Checks if the request body is not empty, contains a valid filter with status,
	 * and ensures that facets are provided.
	 *
	 * @param request  The request map containing filter criteria and facets.
	 * @param response The SBApiResponse to set error messages and status.
	 * @return true if validation fails, false otherwise.
	 */
	private boolean validateTotalConnectionCountByStatusParams(Map<String, Object> request, SBApiResponse response) {
		Map<String, Object> requestBodyMap = (Map<String, Object>) request.get(Constants.REQUEST);
		if (MapUtils.isEmpty(requestBodyMap)) {
			response.getParams().setStatus(HttpStatus.BAD_REQUEST.toString());
			response.getParams().setErrmsg("Request body cannot be null");
			response.setResponseCode(HttpStatus.BAD_REQUEST);
			return true;
		}
		Map<String, Object> filterMap = (Map<String, Object>) requestBodyMap.get("filter");
		if (MapUtils.isEmpty(filterMap)) {
			response.getParams().setStatus(HttpStatus.BAD_REQUEST.toString());
			response.getParams().setErrmsg("Filter cannot be null");
			response.setResponseCode(HttpStatus.BAD_REQUEST);
			return true;
		}
		List<String> statusList = (List<String>) filterMap.get("status");
		if (CollectionUtils.isEmpty(statusList)) {
			response.getParams().setStatus(HttpStatus.BAD_REQUEST.toString());
			response.getParams().setErrmsg("Status cannot be null");
			response.setResponseCode(HttpStatus.BAD_REQUEST);
			return true;
		}
		List<String> facets = (List<String>) requestBodyMap.get("facets");
		if (CollectionUtils.isEmpty(facets)) {
			response.getParams().setStatus(HttpStatus.BAD_REQUEST.toString());
			response.getParams().setErrmsg("Facets cannot be null");
			response.setResponseCode(HttpStatus.BAD_REQUEST);
			return true;
		}
		return false;
	}

	public SBApiResponse upsertUserInformation(String userId) {
		SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_ONBOARD_NETWORK_HUB_USER);
		try {
			Map<String, Object> userProfile = userUtilityService.readUserDataFromDB(userId);
			if (MapUtils.isEmpty(userProfile)) {
				logger.error("ProfileService : onboardNetworkHubUser : User profile not found for userId: {}", userId);
				response.getParams().setStatus(HttpStatus.NOT_FOUND.toString());
				response.getParams().setErrmsg("User profile not found");
				response.setResponseCode(HttpStatus.NOT_FOUND);
				return response;
			}
			if (MapUtils.isEmpty(userProfile)) {
				logger.error("ProfileService : onboardNetworkHubUser : User profile not found for userId: {}", userId);
				response.getParams().setStatus(HttpStatus.NOT_FOUND.toString());
				response.getParams().setErrmsg("User profile not found");
				response.setResponseCode(HttpStatus.NOT_FOUND);
				return response;
			}

			Map<String, Object> profileDetails = (Map<String, Object>) userProfile.get(Constants.PROFILE_DETAILS_KEY);
			if (MapUtils.isEmpty(profileDetails)) {
				logger.error("ProfileService : onboardNetworkHubUser : Profile details not found for userId: {}", userId);
				response.getParams().setStatus(HttpStatus.NOT_FOUND.toString());
				response.getParams().setErrmsg("Profile details not found");
				response.setResponseCode(HttpStatus.NOT_FOUND);
				return response;
			}

			List<Map<String, Object>> professionalDetails = (List<Map<String, Object>>) profileDetails.get(Constants.PROFESSIONAL_DETAILS);
			if (CollectionUtils.isEmpty(professionalDetails)) {
				logger.error("ProfileService : onboardNetworkHubUser : Professional details not found for userId: {}", userId);
				response.getParams().setStatus(HttpStatus.NOT_FOUND.toString());
				response.getParams().setErrmsg("Professional details not found");
				response.setResponseCode(HttpStatus.NOT_FOUND);
				return response;
			}

			String designation = (String) professionalDetails.get(0).get(Constants.DESIGNATION);
			List<String> role = getUserRoles(userId, (String) userProfile.get(Constants.ROOT_ORG_ID));
			if (CollectionUtils.isEmpty(role)) {
				logger.error("ProfileService : onboardNetworkHubUser : User roles not found for userId: {}", userId);
				response.getParams().setStatus(HttpStatus.NOT_FOUND.toString());
				response.getParams().setErrmsg("User roles not found");
				response.setResponseCode(HttpStatus.NOT_FOUND);
				return response;
			}

			Node node = new Node(designation, userId, role, (String) userProfile.get(Constants.ROOT_ORG_ID), new Date().toString());
			if (connectionService.updateUserProfileInNeo4j(node)) {
				logger.info("ProfileService : onboardNetworkHubUser : User {} onboarded successfully in network hub", userId);
				response.getParams().setStatus(HttpStatus.OK.toString());
				response.getResult().put(Constants.MESSAGE, Constants.USER_ONBOARDED_NETWORK_HUB);
				response.setResponseCode(HttpStatus.OK);
			} else {
				logger.error("ProfileService : onboardNetworkHubUser : Failed to onboard user {} in network hub", userId);
				response.getParams().setStatus(HttpStatus.INTERNAL_SERVER_ERROR.toString());
				response.getParams().setErrmsg("Failed to onboard user in network hub");
				response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			// call graphDao and check user already exists in Neo4j
			// if not, update the details in Neo4J
		} catch(Exception e) {
			logger.error("ProfileService : onboardNetworkHubUser : Error while onboarding user {} in network hub: {}", userId, e);
			response.getParams().setStatus(HttpStatus.INTERNAL_SERVER_ERROR.toString());
			response.getParams().setErrmsg("Error while onboarding user in network hub");
			response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return response;
	}

	public List<String> getUserRoles(String userId, String rootOrgId) {
		logger.info("Fetching user roles for userId: {}, rootOrgId: {}", userId, rootOrgId);
		List<Map<String, Object>> userRoleRecords = cassandraOperation.getRecordsByProperties(
				Constants.KEYSPACE_SUNBIRD, Constants.USER_ROLES,
				Map.of(Constants.USERID_KEY, userId), List.of(Constants.ROLE, Constants.SCOPE)
		);
		return userRoleRecords.stream()
				.map(userRoleMap -> {
					Object scopeObj = userRoleMap.get(Constants.SCOPE);
					List<Map<String, Object>> scopes = new ArrayList<>();
					if (scopeObj instanceof List) {
						scopes = (List<Map<String, Object>>) scopeObj;
					} else if (scopeObj instanceof String) {
						String scopeStr = (String) scopeObj;
						if (!StringUtils.isEmpty(scopeStr)) {
							try {
								scopes = mapper.readValue(scopeStr, new TypeReference<List<Map<String, Object>>>() {
								});
							} catch (Exception e) {
								logger.warn("Failed to parse scope JSON for userId {}: {}", userId, e.getMessage());
								return null;
							}
						}
					}
					if (!scopes.isEmpty() && scopes.stream().allMatch(scope -> rootOrgId.equals(scope.get(Constants.ORGANISATION_ID)))) {
						return (String) userRoleMap.get(Constants.ROLE);
					}
					return null;
				})
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toList());

	}


	/**
	 * Retrieves the count of recommended users for a given user ID.
	 * It first checks the Redis cache for the count and if not found, fetches it from the connection service.
	 * The count is then cached in Redis for future requests.
	 *
	 * @param userId The ID of the user for whom recommendations are being fetched.
	 * @return The count of recommended users.
	 */
	private int getCountForRecommendedUsers(String userId) {
		int count;
		String userRecommendationCount = redisCacheMgr.getCache(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS + Constants.UNDER_SCORE + Constants.USER_COUNT + Constants.UNDER_SCORE + userId);
		if (!StringUtils.isEmpty(userRecommendationCount)) {
			count = Integer.parseInt(userRecommendationCount);
		} else {
			count = connectionService.getCountForRecommendedUsers(userId);
			redisCacheMgr.putCache(Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS + Constants.UNDER_SCORE + Constants.USER_COUNT + Constants.UNDER_SCORE + userId, count, connectionProperties.getRedisUserCountTimeOut());
		}
		return count;
	}


	/**
	 * Retrieves the SBApiResponse for user recommendations based on the request parameters.
	 * It checks if the offset and size are within the cache limits and fetches data from Redis if available.
	 * If not available, it fetches from the connection service and caches the results.
	 *
	 * @param request  The request map containing pagination parameters.
	 * @param userId   The ID of the user for whom recommendations are being fetched.
	 * @param response The SBApiResponse to populate with results.
	 * @param count    The total count of recommended users.
	 * @return SBApiResponse containing the list of recommended users or null if not found in cache.
	 * @throws IOException If there is an error reading from Redis cache.
	 */
	private SBApiResponse fetchAndCacheRecommendedUsers(Map<String, Object> request, String userId, SBApiResponse response, int count) throws IOException {
		int size = (Integer) request.get(Constants.SIZE);
		int offset = Math.max(0, (Integer) request.get(Constants.OFFSET)) * size;

		int cacheLimit = connectionProperties.getUserRecommendationCacheLimit();
		String cacheKey = Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS +
						Constants.UNDER_SCORE + Constants.USERS + Constants.UNDER_SCORE + userId;

		// Check current cache size
		int requiredEndIndex = offset + size;
		List<Map<String, Object>> cachedUsers = null;
		String cachedData = redisCacheMgr.getCache(cacheKey);
		if (!ObjectUtils.isEmpty(cachedData)) {
			cachedUsers = mapper.readValue(cachedData, new TypeReference<List<Map<String, Object>>>() {});
			if (cachedUsers.size() >= requiredEndIndex) {
				List<Map<String, Object>> pagedUsers = cachedUsers.subList(offset, Math.min(requiredEndIndex, cachedUsers.size()));
				response.getResult().put(Constants.RESPONSE, pagedUsers);
				response.getParams().setStatus(Constants.OK);
				response.setResponseCode(HttpStatus.OK);
				response.getResult().put(Constants.COUNT, count);
				return response;
			}
		}

		// If cache is missing or insufficient
		int fetchLimit = ((requiredEndIndex / cacheLimit) + 1) * cacheLimit;
		request.put(Constants.SIZE, fetchLimit);
		List<Map<String, String>> recommendationUsersList = connectionService.findRecommendationForUser(userId, request);
		List<Map<String, Object>> enrichedFullList = enrichNeo4JDataForRecommendataion(recommendationUsersList);

		redisCacheMgr.putCache(cacheKey, enrichedFullList, networkServerProperties.getRedisUserListReadTimeOut());

		if (enrichedFullList.size() >= requiredEndIndex) {
			List<Map<String, Object>> pagedUsers = enrichedFullList.subList(offset, Math.min(requiredEndIndex, enrichedFullList.size()));
			response.getResult().put(Constants.RESPONSE, pagedUsers);
			response.getParams().setStatus(Constants.OK);
			response.setResponseCode(HttpStatus.OK);
			response.getResult().put(Constants.COUNT, count);
			return response;
		}

		// Edge case: still not enough
		response.getResult().put(Constants.RESPONSE, Collections.emptyList());
		response.getParams().setStatus(Constants.OK);
		response.setResponseCode(HttpStatus.OK);
		response.getResult().put(Constants.COUNT, 0);
		return response;
	}

	/**
	 * Fetches user information from the Redis cache if available.
	 * It checks if the cached data is not empty and retrieves paginated user data based on offset and size.
	 * If the cached data is valid, it populates the response with the paged users and returns it.
	 *
	 * @param offset     The starting index for pagination.
	 * @param size       The number of users to fetch.
	 * @param cacheLimit The maximum number of users that can be cached.
	 * @param userId     The ID of the user for whom recommendations are being fetched.
	 * @param response   The SBApiResponse to populate with results.
	 * @param count      The total count of recommended users.
	 * @return SBApiResponse containing the paged users or null if not found in cache.
	 * @throws IOException If there is an error reading from Redis cache.
	 */
	private SBApiResponse fetchUsersInfoFromCacheIfAvailable(int offset, int size, int cacheLimit, String userId, SBApiResponse response, Integer count) throws IOException {
		String cacheKey = Constants.USER_LIST + Constants.UNDER_SCORE + Constants.RECOMMENDED_USERS + Constants.UNDER_SCORE + Constants.USERS + Constants.UNDER_SCORE + userId;
		String cachedData = redisCacheMgr.getCache(cacheKey);
		if (!ObjectUtils.isEmpty(cachedData)) {
			JsonNode cachedUsersNode = mapper.readTree(cachedData);
			ArrayNode cachedUsers = null;
			if (cachedUsersNode != null && cachedUsersNode.isArray()) {
				cachedUsers = (ArrayNode) cachedUsersNode;
			}
			if (cachedUsers.size() <= cacheLimit && offset + size <= cachedUsers.size()) {
				ArrayNode pagedUsers = mapper.createArrayNode();
				for (int i = offset; i < offset + size; i++) {
					pagedUsers.add(cachedUsers.get(i));
				}
				response.getResult().put(Constants.RESPONSE, pagedUsers);
				response.getParams().setStatus(Constants.OK);
				response.setResponseCode(HttpStatus.OK);
				response.getResult().put(Constants.COUNT, count);
				return response;
			}
		}
		return null;
	}

	/**
	 * Only the inital 100 records will be cached in Redis for user recommendations.
	 * Oher records wil be fetched from the database on every call.
	 * Enriches user information for user recommendations by fetching additional details.
	 *
	 * @param userList - The list of users to enrich.
	 * @param response - The SBApiResponse to populate with enriched user information.
	 * @return ArrayNode containing enriched user information.
	 */
	private ArrayNode enrichUserInformationForUserRecommendation(List<Map<String, String>> userList, SBApiResponse response) {
		List<String> connectionUserIds = new ArrayList<>();
		ArrayNode enrichedUserMap;
		Map<String, Map<String, Object>> userInfoMap = new HashMap<>();
		extractUserDetails(userList, connectionUserIds, userInfoMap);
		MultiSearch mSearchRequest = new MultiSearch();
		enrichedUserMap = iUserUtility.getUserInfoFromRedisV2(mSearchRequest, connectionUserIds, userInfoMap);
		response.getResult().put(Constants.RESPONSE, enrichedUserMap);
		response.getParams().setStatus(Constants.OK);
		response.setResponseCode(HttpStatus.OK);
		return enrichedUserMap;
	}

	private List<Map<String, Object>> enrichNeo4JDataForRecommendataion(List<Map<String, String>> userList) {
		if (CollectionUtils.isNotEmpty(userList)) {
			List<Map<String, Object>> enrichedData = new ArrayList<>();
			for (Map<String, String> user : userList) {
				Map<String, Object> enrichedUser = new HashMap<>();
				enrichedUser.put(Constants.USER_ID, user.get(Constants.USER_ID));
				iUserUtility.getUserProfileFromRedis(enrichedUser);
				enrichedData.add(enrichedUser);
			}
			return enrichedData;
		}
		return new ArrayList<>();
	}
}
