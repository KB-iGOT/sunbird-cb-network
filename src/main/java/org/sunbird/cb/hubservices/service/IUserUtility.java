package org.sunbird.cb.hubservices.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.sunbird.cb.hubservices.model.MultiSearch;

import java.util.List;
import java.util.Map;

public interface IUserUtility {

    Map<String, Object> getUserInfoFromRedish(MultiSearch multiSearch, String[] sourceField, List<String> connectionIdsToExclude);

    ArrayNode getUserInfoFromRedisV2(MultiSearch multiSearch, List<String> connectionUserIds, Map<String, Map<String, Object>> userInfoMap);

    void getUserProfileFromRedis(Map<String, Object> userProfile);
}
