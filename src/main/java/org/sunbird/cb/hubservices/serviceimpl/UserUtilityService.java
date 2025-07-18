package org.sunbird.cb.hubservices.serviceimpl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.sunbird.cb.hubservices.cache.RedisCacheMgr;
import org.sunbird.cb.hubservices.cassandra.CassandraOperation;
import org.sunbird.cb.hubservices.model.MultiSearch;
import org.sunbird.cb.hubservices.model.Node;
import org.sunbird.cb.hubservices.model.Request;
import org.sunbird.cb.hubservices.model.Search;
import org.sunbird.cb.hubservices.profile.handler.ProfileUtils;
import org.sunbird.cb.hubservices.service.IConnectionService;
import org.sunbird.cb.hubservices.service.IUserUtility;
import org.sunbird.cb.hubservices.util.ConnectionProperties;
import org.sunbird.cb.hubservices.util.Constants;
import org.sunbird.cb.hubservices.util.NetworkServerProperties;
import org.sunbird.cb.hubservices.util.PrettyPrintingMap;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserUtilityService implements IUserUtility {

    @Autowired
    RedisCacheMgr redisCacheMgr;

    @Autowired
    ConnectionProperties connectionProperties;

    @Autowired
    IConnectionService connectionService;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    NetworkServerProperties networkServerProperties;

    @Autowired
    private CassandraOperation cassandraOperation;

    private Logger logger = LoggerFactory.getLogger(UserUtilityService.class);

    @Override
    public Map<String, Object> getUserInfoFromRedish(MultiSearch multiSearch, String[] sourceFields, List<String> connectionIdsToExclude) {
        String departmentName = "";
        List<String> includeFields = sourceFields != null && !Arrays.asList(sourceFields).isEmpty() ? Arrays.asList(sourceFields) : ProfileUtils.getUserDefaultFields();
        Map<String, Object> tagRes = new HashMap<>();
        for (Search sRequest : multiSearch.getSearch()) {
            departmentName = (String) sRequest.getValues().get(0);
            departmentName = departmentName.trim().replaceAll(" ", "");
            String userInformation = redisCacheMgr.getCache(Constants.USER_LIST + Constants.UNDER_SCORE + departmentName);
            if (ObjectUtils.isEmpty(userInformation)) {
                ArrayNode arrayRes = getUserInfoFromSearch(sRequest, multiSearch, includeFields, connectionIdsToExclude);
                tagRes.put(sRequest.getField(), arrayRes);
                if (arrayRes.size() > 1)
                    redisCacheMgr.putCache(Constants.USER_LIST + Constants.UNDER_SCORE + departmentName, arrayRes, networkServerProperties.getRedisUserListReadTimeOut().intValue());

            } else {
                getUserNodeInfoFromRedish(userInformation, sRequest, multiSearch, connectionIdsToExclude, tagRes);
            }
        }
        logger.info("user search result :: {}", new PrettyPrintingMap<>(tagRes));
        return tagRes;
    }

    private int getLimitRequest(int requestSize) {
        Integer limit = networkServerProperties.getDefaultLimit();
        Integer maxLimit = networkServerProperties.getMaxLimit();
        if (requestSize == 0) {
            return limit;
        } else if (requestSize < maxLimit.intValue()) {
            return requestSize;
        }
        return maxLimit.intValue();
    }

    private ArrayNode getUserInfoFromSearch(Search sRequest, MultiSearch multiSearch, List<String> includeFields, List<String> connectionIdsToExclude) {
        ArrayNode arrayRes = JsonNodeFactory.instance.arrayNode();
        try {
            List<String> tags = new ArrayList<>();
            StringBuilder searchPath = new StringBuilder();
            searchPath.append(ProfileUtils.Profile.PROFILE_DETAILS).append(".").append(sRequest.getField());
            // Prepare of SearchDTO
            Request request = new Request();
            Map<String, Object> searchQueryMap = new HashMap<>();
            Map<String, Object> additionalProperties = new HashMap<>();
            additionalProperties.put(searchPath.toString(), sRequest.getValues().get(0));
            additionalProperties.put("status", 1);
            searchQueryMap.put("query", "");
            searchQueryMap.put("filters", additionalProperties);
            searchQueryMap.put("offset", multiSearch.getOffset());
            searchQueryMap.put("limit", getLimitRequest(multiSearch.getSize()));
            searchQueryMap.put("fields", includeFields);
            request.setRequest(searchQueryMap);
            tags.add(sRequest.getField());
            fetchUserDetailsFromLearnerService(connectionIdsToExclude, request, arrayRes, new HashMap<>());
        } catch (Exception e) {
            logger.error(String.format("Error while connecting the nodes! error : %s", e));
        }
        return arrayRes;
    }

    private Map<String, Object> getUserNodeInfoFromRedish(String userInformation, Search sRequest, MultiSearch multiSearch, List<String> connectionIdsToExclude, Map<String, Object> tagRes) {
        ArrayNode filteredArrayNode = mapper.createArrayNode();
        try {
            JsonNode jsonNode = mapper.readTree(userInformation);
            ArrayNode arrayNode = (ArrayNode) jsonNode;

            for (JsonNode element : arrayNode) {
                if (!connectionIdsToExclude.contains(element.get(ProfileUtils.Profile.USER_ID).asText())) {
                    filteredArrayNode.add(element);
                }
            }

            if (multiSearch.getSize() >= filteredArrayNode.size()) {
                tagRes.put(sRequest.getField(), filteredArrayNode);
            } else if (multiSearch.getSize() < filteredArrayNode.size()) {
                Field innerArrayNode = ArrayNode.class.getDeclaredField("_children");
                innerArrayNode.setAccessible(true);
                List<JsonNode> innerArrayNodeChildNodes = (List<JsonNode>) innerArrayNode.get(filteredArrayNode);
                List<JsonNode> limitedChildNodes = innerArrayNodeChildNodes.subList(0, multiSearch.getSize());
                innerArrayNode.set(filteredArrayNode, limitedChildNodes);
                tagRes.put(sRequest.getField(), filteredArrayNode);
            }
        } catch (Exception e) {
            tagRes.put(Constants.ResponseStatus.STATUS, HttpStatus.OK);
            tagRes.put(sRequest.getField(), filteredArrayNode);
            logger.error(String.format("Error while connecting the nodes! error : %s", e));
        }
        return tagRes;
    }

    @Override
    public ArrayNode getUserInfoFromRedisV2(MultiSearch multiSearch, List<String> connectionUserIds, Map<String, Map<String, Object>> userInfoMap) {
        List<String> includeFields = ProfileUtils.getUserDefaultFields();
        Map<String, Object> tagRes = new HashMap<>();
        ArrayNode arrayRes = getUserInfoFromSearchBasedOnUserIds(includeFields, connectionUserIds, userInfoMap);
        logger.info("user search result :: {}", new PrettyPrintingMap<>(tagRes));
        return sortArrayNodesByDate(arrayRes);
    }

    private ArrayNode getUserInfoFromSearchBasedOnUserIds(List<String> includeFields, List<String> connectionUserIds, Map<String, Map<String, Object>> userInfoMap) {
        ArrayNode arrayRes = JsonNodeFactory.instance.arrayNode();
        try {
            Request request = new Request();
            Map<String, Object> searchQueryMap = new HashMap<>();
            Map<String, Object> additionalProperties = new HashMap<>();
            additionalProperties.put(Constants.USER_ID, connectionUserIds);
            additionalProperties.put(Constants.STATUS, 1);
            searchQueryMap.put(Constants.QUERY, "");
            searchQueryMap.put(Constants.FILTERS, additionalProperties);
            searchQueryMap.put(Constants.FIELDS, includeFields);
            request.setRequest(searchQueryMap);
            fetchUserDetailsFromLearnerService(connectionUserIds, request, arrayRes,userInfoMap);
        } catch (Exception e) {
            logger.error(String.format("Error while connecting the nodes! error : %s", e));
        }
        return arrayRes;
    }

    /**
     * Fetches user details from the learner service and populates the response array.
     *
     * @param connectionUserIds the list of user IDs to fetch details for
     * @param request           the request object containing search parameters
     * @param arrayRes          the array node to populate with user details
     * @param userInfoMap       a map containing additional user information
     */
    private void fetchUserDetailsFromLearnerService(List<String> connectionUserIds, Request request, ArrayNode arrayRes, Map<String, Map<String, Object>> userInfoMap) {
        ResponseEntity<?> responseEntity = ProfileUtils.getResponseEntity(connectionProperties.getLearnerServiceHost(), connectionProperties.getUserSearchEndPoint(), request);
        JsonNode node = mapper.convertValue(responseEntity.getBody(), JsonNode.class);
        ArrayNode nodes = (ArrayNode) node.get(Constants.RESULT).get(Constants.RESPONSE).get(Constants.CONTENT);
        for (JsonNode n : nodes) {
            String userId = n.get(ProfileUtils.Profile.USER_ID).asText();
            if (!connectionUserIds.contains(userId)) continue;
            JsonNode profileDetails = n.get(ProfileUtils.Profile.PROFILE_DETAILS);
            if (profileDetails != null && profileDetails.isObject()) {
                sanitizePersonalDetails(profileDetails);
                populateProfileDetails((ObjectNode) profileDetails, n, userInfoMap.get(userId));
            }
            arrayRes.add(profileDetails);
        }
    }

    public Map<String, Object> readUserDataFromDB(String userId) {        
        String cacheKey = Constants.USER + ":basicProfile:" + userId;
        Map<String, Object> queryParams = Map.of(Constants.ID, userId);
        List<Map<String, Object>> userList = cassandraOperation.getRecordsByProperties(
                Constants.KEYSPACE_SUNBIRD, Constants.USER, queryParams, null);

        if (CollectionUtils.isEmpty(userList)) { 
            return Map.of();
        }
        Map<String, Object> userObj = userList.get(0);
        String profileDetailsJson = (String) userObj.get(Constants.PROFILE_DETAILS);

        try {
            if (StringUtils.isNotBlank(profileDetailsJson)) {
                Map<String, Object> profileDetailsMap = mapper.readValue(profileDetailsJson, new TypeReference<Map<String, Object>>() {
                });
                userObj.put(Constants.PROFILE_DETAILS_KEY, profileDetailsMap);
            } else {
                userObj.put(Constants.PROFILE_DETAILS_KEY, Map.of());
            }
        } catch (IOException e) {
            logger.error("Invalid profileDetails JSON for userId: {}", userId, e);
            userObj.put(Constants.PROFILE_DETAILS_KEY, Map.of());
        }

        return userObj;
    }

    /**
     * Sanitizes personal details by removing sensitive information like mobile and primary email.
     *
     * @param profileDetails the profile details node containing personal information
     */
    private void sanitizePersonalDetails(JsonNode profileDetails) {
        JsonNode personalDetails = profileDetails.get(Constants.PERSONAL_DETAILS);
        if (personalDetails != null && personalDetails.isObject()) {
            ((ObjectNode) personalDetails).remove(Constants.MOBILE);
            ((ObjectNode) personalDetails).remove(Constants.PRIMARY_EMAIL);
        }
    }

    /**
     * Populates profile details with necessary fields and user information.
     *
     * @param profileDetails the profile details node to be populated
     * @param n              the JsonNode containing user data
     * @param userInfo       a map containing additional user information
     */
    private void populateProfileDetails(ObjectNode profileDetails, JsonNode n, Map<String, Object> userInfo) {
        profileDetails.put(Constants.VERIFIED_KARMAYOGI, !ObjectUtils.isEmpty(profileDetails.get(Constants.VERIFIED_KARMAYOGI)) ?
                profileDetails.get(Constants.VERIFIED_KARMAYOGI).asBoolean() : Boolean.FALSE);
        String userId = n.get(ProfileUtils.Profile.USER_ID).asText();
        profileDetails.put(ProfileUtils.Profile.USER_ID, userId);
        profileDetails.put(ProfileUtils.Profile.ID, userId);
        profileDetails.put(ProfileUtils.Profile.AT_ID, userId);
        profileDetails.put(ProfileUtils.Profile.PROFILE_IMAGE_URL, getNodeText(profileDetails, Constants.PROFILE_IMAGE_URL));
        profileDetails.put(ProfileUtils.Profile.PROFILE_DETAILS_PROFILE_BANNER_IMAGE_URL, getNodeText(profileDetails, Constants.PROFILE_BANNER_IMAGE_URL));
        if (userInfo != null) {
            profileDetails.put(Constants.ROLE, mapper.valueToTree(userInfo.get(Constants.ROLE)));
            profileDetails.put(Constants.ROOT_ORG_ID, (String) userInfo.get(Constants.ORGANISATION_ID));
            profileDetails.put(Constants.DESIGNATION, (String) userInfo.get(Constants.DESIGNATION));
            profileDetails.put(Constants.CREATED_AT, (String) userInfo.get(Constants.CREATED_AT));
            profileDetails.put(Constants.UPDATED_AT, (String) userInfo.get(Constants.UPDATED_AT));
        }
    }

    /**
     * Retrieves the text value of a specified field from a JsonNode.
     *
     * @param node  the JsonNode from which to retrieve the field value
     * @param field the name of the field to retrieve
     * @return the text value of the field, or an empty string if the field is null or does not exist
     */
    private String getNodeText(JsonNode node, String field) {
        JsonNode valueNode = node.get(field);
        return (valueNode != null && !valueNode.isNull()) ? valueNode.asText() : "";
    }

    /**
     * Sorts an ArrayNode of JsonNodes by the latest date found in the createdAt or updatedAt fields.
     *
     * @param nodeArray the ArrayNode containing JsonNodes to be sorted
     * @param mapper    the ObjectMapper used to create the sorted ArrayNode
     * @return a new ArrayNode sorted by the latest date in descending order
     */
    public ArrayNode sortArrayNodesByDate(ArrayNode nodeArray) {
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_SORTING, Locale.ENGLISH);
        List<JsonNode> nodeList = new ArrayList<>();
        nodeArray.forEach(nodeList::add);

        nodeList.sort((n1, n2) -> {
            Date date1 = getLatestDate(n1, sdf);
            Date date2 = getLatestDate(n2, sdf);
            if (date1 == null && date2 == null) return 0;
            if (date1 == null) return 1;
            if (date2 == null) return -1;
            return date2.compareTo(date1); // descending order
        });

        ArrayNode sortedArray = mapper.createArrayNode();
        nodeList.forEach(sortedArray::add);
        return sortedArray;
    }

    /**
     * Retrieves the latest date from a JsonNode based on the created_at and updated_at fields.
     *
     * @param node the JsonNode containing date fields
     * @param sdf  the SimpleDateFormat to parse date strings
     * @return the latest date found in the node, or null if neither date is present
     */
    private Date getLatestDate(JsonNode node, SimpleDateFormat sdf) {
        Date createdDate = null;
        Date updatedDate = null;
        String createdAt = node.hasNonNull(Constants.CREATED_AT) ? node.get(Constants.CREATED_AT).asText() : null;
        String updatedAt = node.hasNonNull(Constants.UPDATED_AT) ? node.get(Constants.UPDATED_AT).asText() : null;

        try {
            if (StringUtils.isNotEmpty(createdAt)) {
                createdDate = sdf.parse(createdAt);
            }
        } catch (Exception e) {
            logger.error("Error parsing createdAt: {}", e.getMessage());
        }
        try {
            if (StringUtils.isNotEmpty(updatedAt)) {
                updatedDate = sdf.parse(updatedAt);
            }
        } catch (Exception e) {
            logger.error("Error parsing updatedAt: {}", e.getMessage());
        }
        if (createdDate == null) return updatedDate;
        if (updatedDate == null) return createdDate;
        return createdDate.after(updatedDate) ? createdDate : updatedDate;
    }
}
