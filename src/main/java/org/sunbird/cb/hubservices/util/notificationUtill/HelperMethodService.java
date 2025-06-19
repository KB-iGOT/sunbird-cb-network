package org.sunbird.cb.hubservices.util.notificationUtill;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.cb.hubservices.cache.RedisCacheMgr;
import org.sunbird.cb.hubservices.cassandra.CassandraOperation;
import org.sunbird.cb.hubservices.util.Constants;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HelperMethodService {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CassandraOperation cassandraOperation;
    @Autowired
    private RedisCacheMgr cacheService;

    public Object fetchDataForKey(String key) {
        try {
            String value = cacheService.getCache(key);
            if (value != null) {
                // Replace Object.class with your specific type, e.g., User.class
                return objectMapper.readValue(value, Object.class);
            } else {
                log.warn("No value found in cache for key: {}", key);
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to deserialize JSON for key: {}", key, e);
            return null;
        }
    }

    public List<Object> fetchUserFromPrimary(String userId) {
        log.info("DiscussionServiceImpl::fetchUserFromPrimary: Fetching user data from Cassandra");
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.ID, userId);

        List<Map<String, Object>> userInfoList = cassandraOperation.getRecordsByProperties(
                Constants.KEYSPACE_SUNBIRD,
                Constants.TABLE_USER,
                propertyMap,
                Arrays.asList(Constants.FIRST_NAME, Constants.ID)
        );
        List<Object> userList = new ArrayList<>();
        for (Map<String, Object> userInfo : userInfoList) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put(Constants.USER_ID_KEY, userInfo.get(Constants.ID));
            userMap.put(Constants.FIRST_NAME_KEY, userInfo.get(Constants.FULL_NAME));
            userList.add(userMap);
        }
        return userList;
    }

    public String fetchUserFirstName(String userId) {
        Object redisResult = fetchDataForKey(Constants.USER_PREFIX + userId);
        if (redisResult instanceof Map) {
            String name = (String) ((Map<?, ?>) redisResult).get(Constants.FIRST_NAME_KEY);
            if (StringUtils.isNotBlank(name)) {
                log.info("UserName from redis {}",name);
                return name;
            }
        }

        List<Object> cassandraResults = fetchUserFromPrimary(userId);
        if (!cassandraResults.isEmpty() && cassandraResults.get(0) instanceof Map) {
            String name = (String) ((Map<?, ?>) cassandraResults.get(0)).get(Constants.FIRST_NAME_KEY);
            log.info("UserName from database {}",name);
            if (StringUtils.isNotBlank(name)) return name;
        }
        return "User";
    }

}
