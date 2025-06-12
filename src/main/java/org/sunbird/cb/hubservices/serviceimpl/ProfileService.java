package org.sunbird.cb.hubservices.serviceimpl;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.sunbird.cb.hubservices.exception.ApplicationException;
import org.sunbird.cb.hubservices.model.MultiSearch;
import org.sunbird.cb.hubservices.model.Response;
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
	public Response getRelationshipBetweenUsers(Map<String, Object> requestBody) {
		Response response = new Response();
		String fromUserId = null;
		String toUserId = null;
		if (!MapUtils.isEmpty(requestBody)) {
			if (StringUtils.isEmpty(requestBody.get("fromUserId"))) {
				response.put(Constants.ResponseStatus.MESSAGE, "fromUserId cannot be empty");
				response.put(Constants.ResponseStatus.STATUS, HttpStatus.BAD_REQUEST);
				response.put("responseCode", HttpStatus.BAD_REQUEST.value());
				return response;
			} else if (StringUtils.isEmpty(requestBody.get("toUserId"))) {
				response.put(Constants.ResponseStatus.MESSAGE, "toUserId cannot be empty");
				response.put(Constants.ResponseStatus.STATUS, HttpStatus.BAD_REQUEST);
				response.put("responseCode", HttpStatus.BAD_REQUEST.value());
				return response;
			}
			fromUserId = (String) requestBody.get("fromUserId");
			toUserId = (String) requestBody.get("toUserId");
		} else {
			response.put(Constants.ResponseStatus.MESSAGE, "Request body cannot be empty");
			response.put(Constants.ResponseStatus.STATUS, HttpStatus.BAD_REQUEST);
			response.put("responseCode", HttpStatus.BAD_REQUEST.value());
			return response;
		}
		try {
			Map<String, String> map = connectionService.getRelationshipBetweenUsers(fromUserId,
					toUserId);
			response.put(Constants.ResponseStatus.MESSAGE, Constants.ResponseStatus.SUCCESSFUL);
			response.put(Constants.ResponseStatus.DATA, map);
			response.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);
			return response;
		} catch (Exception e) {
			logger.error("Error fetching relationship between {} and {}: {}", fromUserId, toUserId, e.getMessage());
			response.put(Constants.ResponseStatus.MESSAGE, "Error fetching relationship");
			response.put(Constants.ResponseStatus.STATUS, HttpStatus.INTERNAL_SERVER_ERROR);
			return response;
		}
	}
}
