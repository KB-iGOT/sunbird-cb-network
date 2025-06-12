
package org.sunbird.cb.hubservices.service;

import org.sunbird.cb.hubservices.model.MultiSearch;
import org.sunbird.cb.hubservices.model.Response;
import org.sunbird.cb.hubservices.util.Constants;

import java.util.Map;

public interface IProfileService {

	public Response multiSearchProfiles(String userId, MultiSearch multiSearchRequest, String[] sourceFields);

	public Response findCommonProfileV2(String userId, int offset, int limit);

	public Response findProfilesV2(String userId, int offset, int limit);

	public Response findProfileRequestedV2(String userId, int offset, int limit, Constants.DIRECTION direction);

	Response getRelationshipBetweenUsers(Map<String, Object> requestBody);
}
