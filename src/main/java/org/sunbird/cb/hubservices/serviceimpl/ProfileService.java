package org.sunbird.cb.hubservices.serviceimpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.sunbird.cb.hubservices.cache.RedisCacheMgr;
import org.sunbird.cb.hubservices.common.auth.AccessTokenValidator;
import org.sunbird.cb.hubservices.common.util.ProjectUtil;
import org.sunbird.cb.hubservices.exception.ApplicationException;
import org.sunbird.cb.hubservices.model.MultiSearch;
import org.sunbird.cb.hubservices.model.Response;
import org.sunbird.cb.hubservices.model.SBApiResponse;
import org.sunbird.cb.hubservices.service.IConnectionService;
import org.sunbird.cb.hubservices.service.INodeService;
import org.sunbird.cb.hubservices.service.IProfileService;
import org.sunbird.cb.hubservices.service.IUserUtility;
import org.sunbird.cb.hubservices.util.Constants;
import org.sunbird.cb.hubservices.util.NetworkServerProperties;

import java.io.IOException;
import java.util.*;

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
			Integer count = connectionService.getCountForRecommendedUsers(userId);
			if(count == 0){
				logger.info("ProfileService : findRecommendations : Recommended Users Count is 0 for userId: {}", userId);
				response.getParams().setStatus(HttpStatus.OK.toString());
				response.getResult().put(Constants.MESSAGE,"Recommended users count is empty");
				response.setResponseCode(HttpStatus.OK);
				return response;
			}
			List<Map<String, String>> recommendationUsersList = connectionService.findRecommendationForUser(userId, request);
			if(CollectionUtils.isEmpty(recommendationUsersList)){
				logger.info("ProfileService : findRecommendations : Recommended Users List is empty for userId: {}", userId);
				response.getParams().setStatus(HttpStatus.OK.toString());
				response.getResult().put(Constants.MESSAGE,"Recommended users list is empty");
				response.setResponseCode(HttpStatus.OK);
				return response;
			}
            enrichUserInformation(request, recommendationUsersList, response, userId, Constants.USERS);
            response.getResult().put(Constants.COUNT, count);
			return response;
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
			enrichUserInformation(request, recommendationMentorsList, response,userId,Constants.MENTORS);
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
	 * @param request  The request map containing pagination parameters.
	 * @param userList The list of users to enrich.
	 * @param response The SBApiResponse to populate with enriched user information.
	 * @return SBApiResponse containing enriched user information.
	 */
	private SBApiResponse enrichUserInformation(Map<String, Object> request, List<Map<String, String>> userList, SBApiResponse response,String userId, String type) {
		List<String> connectionUserIds = new ArrayList<>();
		ArrayNode enrichedUserMap;
		Map<String,Map<String,Object>> userInfoMap = new HashMap<>();
		extractUserDetails(userList, connectionUserIds, userInfoMap);
		MultiSearch mSearchRequest = new MultiSearch();
		mSearchRequest.setOffset((Integer) request.get(Constants.OFFSET));
		mSearchRequest.setSize((Integer) request.get(Constants.SIZE));
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
		formResponseStructure(response, type, enrichedUserMap);
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
			Map<String, Integer> userCount = nodeService.getConnectionsCountByStatus(userId, Constants.Status.BLOCKED, null);
			response.put(Constants.COUNT, userCount.get(Constants.COUNT));
			return enrichUserInformation(request, blockedUsersList, response,userId,Constants.BLOCKED_USERS);
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
	 * Forms the response structure for the API response.
	 * It shuffles the enriched user map if it is not null and not blocked users,
	 * and sets the response status and code accordingly.
	 *
	 * @param response          The SBApiResponse to populate with the result.
	 * @param type              The type of users (e.g., recommended, blocked).
	 * @param enrichedUserMap   The enriched user map containing user details.
	 */
	private void formResponseStructure(SBApiResponse response, String type, ArrayNode enrichedUserMap) {
		List<JsonNode> nodes = new ArrayList<>();
		if(enrichedUserMap !=null && !Constants.BLOCKED_USERS.equalsIgnoreCase(type)) {
			enrichedUserMap.forEach(nodes::add);
			Collections.shuffle(nodes);
			ArrayNode shuffledArrayNode = mapper.createArrayNode();
			nodes.forEach(shuffledArrayNode::add);
			response.getResult().put(Constants.RESPONSE, shuffledArrayNode);
		}else{
			response.getResult().put(Constants.RESPONSE, enrichedUserMap);
		}
		response.getParams().setStatus(Constants.OK);
		response.setResponseCode(HttpStatus.OK);
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

	public SBApiResponse onboardNetworkHubUser(String userId) {
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

			// Enrich the user profile with id-mapping lookup.

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
}
