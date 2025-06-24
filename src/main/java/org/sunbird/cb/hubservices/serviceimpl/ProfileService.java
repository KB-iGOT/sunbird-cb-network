package org.sunbird.cb.hubservices.serviceimpl;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.sunbird.cb.hubservices.common.auth.AccessTokenValidator;
import org.sunbird.cb.hubservices.common.util.ProjectUtil;
import org.sunbird.cb.hubservices.exception.ApplicationException;
import org.sunbird.cb.hubservices.model.MultiSearch;
import org.sunbird.cb.hubservices.model.Response;
import org.sunbird.cb.hubservices.model.SBApiResponse;
import org.sunbird.cb.hubservices.service.IConnectionService;
import org.sunbird.cb.hubservices.service.IProfileService;
import org.sunbird.cb.hubservices.service.IUserUtility;
import org.sunbird.cb.hubservices.util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProfileService implements IProfileService {

	private Logger logger = LoggerFactory.getLogger(ProfileService.class);

	@Autowired
	IConnectionService connectionService;


	@Autowired
	IUserUtility iUserUtility;

	@Autowired
	AccessTokenValidator accessTokenValidator;

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

	@Override
	public SBApiResponse findRecommendations(String authToken, Map<String, Object> request) {
		SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_USER_RELATIONSHIP);
		String userId = "";
		try {
			userId = accessTokenValidator.fetchUserIdFromAccessToken(authToken, response);
			if (StringUtils.isEmpty(userId)) {
				return response;
			}
			List<Map<String, String>> recommendationUsersList = connectionService.findRecommendationForUser(userId);
			response.getResult().put(Constants.RESPONSE,recommendationUsersList);
			response.getParams().setStatus(Constants.OK);
			response.setResponseCode(HttpStatus.OK);
			return response;
		} catch (Exception e) {
			logger.error(String.format("Error while fetching recommendation for the user %s %s", userId, e));
			response.getParams().setStatus(HttpStatus.INTERNAL_SERVER_ERROR.toString());
			response.getParams().setErrmsg("Error while fetching recommendation for the user");
			response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
			return response;
		}
	}
}
