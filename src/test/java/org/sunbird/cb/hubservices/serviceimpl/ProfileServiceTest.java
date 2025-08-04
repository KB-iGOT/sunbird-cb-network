package org.sunbird.cb.hubservices.serviceimpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.sunbird.cb.hubservices.cache.RedisCacheMgr;
import org.sunbird.cb.hubservices.cassandra.CassandraOperation;
import org.sunbird.cb.hubservices.common.auth.AccessTokenValidator;
import org.sunbird.cb.hubservices.exception.ApplicationException;
import org.sunbird.cb.hubservices.model.MultiSearch;
import org.sunbird.cb.hubservices.model.Response;
import org.sunbird.cb.hubservices.model.SBApiResponse;
import org.sunbird.cb.hubservices.service.IConnectionService;
import org.sunbird.cb.hubservices.service.INodeService;
import org.sunbird.cb.hubservices.service.IUserUtility;
import org.sunbird.cb.hubservices.util.Constants;
import org.sunbird.cb.hubservices.util.NetworkServerProperties;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@RunWith(MockitoJUnitRunner.class)
class ProfileServiceTest {

    @InjectMocks
    private ProfileService profileService;

    @Mock
    private IConnectionService connectionService;

    @Mock
    private IUserUtility iUserUtility;

    @Mock
    private AccessTokenValidator accessTokenValidator;

    @Mock
    private RedisCacheMgr redisCacheMgr;

    @Mock
    private NetworkServerProperties networkServerProperties;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private INodeService nodeService;

    @Mock
    private UserUtilityService userUtilityService;

    @Mock
    private CassandraOperation cassandraOperation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testFindCommonProfileV2() {
        // Arrange
        String userId = "user1";
        int offset = 0;
        int limit = 10;
        Response expectedResponse = new Response();
        expectedResponse.put("status", "success");

        when(connectionService.findSuggestedConnectionsV2(userId, offset, limit))
            .thenReturn(expectedResponse);

        // Act
        Response result = profileService.findCommonProfileV2(userId, offset, limit);

        // Assert
        assertEquals(expectedResponse, result);
        verify(connectionService).findSuggestedConnectionsV2(userId, offset, limit);
    }

    @Test
    void testFindProfilesV2() {
        // Arrange
        String userId = "user1";
        int offset = 0;
        int limit = 10;
        Response expectedResponse = new Response();
        expectedResponse.put("status", "success");

        when(connectionService.findAllConnectionsIdsByStatusV2(userId, Constants.Status.APPROVED, offset, limit))
            .thenReturn(expectedResponse);

        // Act
        Response result = profileService.findProfilesV2(userId, offset, limit);

        // Assert
        assertEquals(expectedResponse, result);
        verify(connectionService).findAllConnectionsIdsByStatusV2(userId, Constants.Status.APPROVED, offset, limit);
    }

    @Test
    void testFindProfileRequestedV2() {
        // Arrange
        String userId = "user1";
        int offset = 0;
        int limit = 10;
        Constants.DIRECTION direction = Constants.DIRECTION.OUT;
        Response expectedResponse = new Response();
        expectedResponse.put("status", "success");

        when(connectionService.findConnectionsRequestedV2(userId, offset, limit, direction))
            .thenReturn(expectedResponse);

        // Act
        Response result = profileService.findProfileRequestedV2(userId, offset, limit, direction);

        // Assert
        assertEquals(expectedResponse, result);
        verify(connectionService).findConnectionsRequestedV2(userId, offset, limit, direction);
    }

    @Test
    void testMultiSearchProfiles_Exception() throws Exception {
        // Arrange
        String userId = "user1";
        MultiSearch mSearchRequest = new MultiSearch();
        String[] sourceFields = {"name", "email"};

        when(connectionService.findUserConnectionsV2(userId, Constants.Status.APPROVED))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ApplicationException.class, () -> {
            profileService.multiSearchProfiles(userId, mSearchRequest, sourceFields);
        });
    }

    @Test
    void testGetRelationshipBetweenUsers_Success() {
        // Arrange
        String toUserId = "user2";
        String authToken = "auth-token";
        String fromUserId = "user1";

        Map<String, String> relationship = Map.of("status", "approved");

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(fromUserId);
        when(connectionService.getRelationshipBetweenUsers(fromUserId, toUserId))
            .thenReturn(relationship);

        // Act
        SBApiResponse result = profileService.getRelationshipBetweenUsers(toUserId, authToken);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getResponseCode());
        assertEquals(Constants.OK, result.getParams().getStatus());
        assertEquals(relationship, result.getResult().get("response"));
    }

    @Test
    void testGetRelationshipBetweenUsers_EmptyUserId() {
        // Arrange
        String toUserId = "user2";
        String authToken = "auth-token";

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn("");

        // Act
        SBApiResponse result = profileService.getRelationshipBetweenUsers(toUserId, authToken);

        // Assert
        assertNotNull(result);
        // Should return early without processing
    }

    @Test
    void testGetRelationshipBetweenUsers_Exception() {
        // Arrange
        String toUserId = "user2";
        String authToken = "auth-token";
        String fromUserId = "user1";

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(fromUserId);
        when(connectionService.getRelationshipBetweenUsers(fromUserId, toUserId))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        SBApiResponse result = profileService.getRelationshipBetweenUsers(toUserId, authToken);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getResponseCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.toString(), result.getParams().getStatus());
    }

//    @Test
//    void testFindRecommendations_Success() {
//        // Arrange
//        String authToken = "auth-token";
//        String userId = "user1";
//        Map<String, Object> request = Map.of("offset", 0, "size", 10);
//
//        List<Map<String, String>> recommendationUsersList = Arrays.asList(
//            Map.of("userId", "user2", "score", "0.8"),
//            Map.of("userId", "user3", "score", "0.6")
//        );
//
//        ArrayNode enrichedUserMap = mock(ArrayNode.class);
//        when(enrichedUserMap.size()).thenReturn(2);
//
//        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
//            .thenReturn(userId);
//        when(connectionService.getCountForRecommendedUsers(userId))
//            .thenReturn(2);
//        when(connectionService.findRecommendationForUser(userId, request))
//            .thenReturn(recommendationUsersList);
//        when(redisCacheMgr.getCache(anyString()))
//            .thenReturn(null);
//        when(iUserUtility.getUserInfoFromRedisV2(any(MultiSearch.class), anyList(), anyMap()))
//            .thenReturn(enrichedUserMap);
//
//        // Act
//        SBApiResponse result = profileService.findRecommendations(authToken, request);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(HttpStatus.OK, result.getResponseCode());
//        assertEquals(Constants.OK, result.getParams().getStatus());
//        assertEquals(2, result.getResult().get(Constants.COUNT));
//    }

    @Test
    void testFindRecommendations_EmptyUserId() {
        // Arrange
        String authToken = "auth-token";
        Map<String, Object> request = Map.of("offset", 0, "size", 10);

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn("");

        // Act
        SBApiResponse result = profileService.findRecommendations(authToken, request);

        // Assert
        assertNotNull(result);
        // Should return early without processing
    }

    @Test
    void testFindRecommendations_EmptyRequest() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = new HashMap<>();

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);

        // Act
        SBApiResponse result = profileService.findRecommendations(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getResponseCode());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), result.getParams().getStatus());
    }

    @Test
    void testFindRecommendations_MissingOffset() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = Map.of("size", 10);

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);

        // Act
        SBApiResponse result = profileService.findRecommendations(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getResponseCode());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), result.getParams().getStatus());
    }

    @Test
    void testFindRecommendations_MissingSize() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0);

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);

        // Act
        SBApiResponse result = profileService.findRecommendations(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getResponseCode());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), result.getParams().getStatus());
    }

    @Test
    void testFindRecommendations_InvalidOffsetType() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", "invalid", "size", 10);

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);

        // Act
        SBApiResponse result = profileService.findRecommendations(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getResponseCode());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), result.getParams().getStatus());
    }

    @Test
    void testFindRecommendations_InvalidSizeType() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0, "size", "invalid");

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);

        // Act
        SBApiResponse result = profileService.findRecommendations(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getResponseCode());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), result.getParams().getStatus());
    }

//    @Test
//    void testFindRecommendations_ZeroCount() {
//        // Arrange
//        String authToken = "auth-token";
//        String userId = "user1";
//        Map<String, Object> request = Map.of("offset", 0, "size", 10);
//
//        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
//            .thenReturn(userId);
//        when(connectionService.getCountForRecommendedUsers(userId))
//            .thenReturn(0);
//
//        // Act
//        SBApiResponse result = profileService.findRecommendations(authToken, request);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(HttpStatus.OK, result.getResponseCode());
//        assertEquals(HttpStatus.OK.toString(), result.getParams().getStatus());
//        assertEquals("Recommended users count is empty", result.getResult().get(Constants.MESSAGE));
//    }

//    @Test
//    void testFindRecommendations_EmptyList() {
//        // Arrange
//        String authToken = "auth-token";
//        String userId = "user1";
//        Map<String, Object> request = Map.of("offset", 0, "size", 10);
//
//        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
//            .thenReturn(userId);
//        when(connectionService.getCountForRecommendedUsers(userId))
//            .thenReturn(2);
//        when(connectionService.findRecommendationForUser(userId, request))
//            .thenReturn(new ArrayList<>());
//
//        // Act
//        SBApiResponse result = profileService.findRecommendations(authToken, request);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(HttpStatus.OK, result.getResponseCode());
//        assertEquals(HttpStatus.OK.toString(), result.getParams().getStatus());
//        assertEquals("Recommended users list is empty", result.getResult().get(Constants.MESSAGE));
//    }

    @Test
    void testFindRecommendations_Exception() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0, "size", 10);

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);
        when(connectionService.getCountForRecommendedUsers(userId))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        SBApiResponse result = profileService.findRecommendations(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getResponseCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.toString(), result.getParams().getStatus());
    }

    @Test
    void testFindRecommendedMentors_Success() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0, "size", 10);

        List<Map<String, String>> recommendationMentorsList = Arrays.asList(
            Map.of("userId", "mentor1", "score", "0.9"),
            Map.of("userId", "mentor2", "score", "0.7")
        );

        ArrayNode enrichedUserMap = mock(ArrayNode.class);
        when(enrichedUserMap.size()).thenReturn(2);

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);
        when(connectionService.getCountForRecommendedMentors(userId))
            .thenReturn(2);
        when(connectionService.findRecommendationForMentors(userId, request))
            .thenReturn(recommendationMentorsList);
        when(redisCacheMgr.getCache(anyString()))
            .thenReturn(null);
        when(iUserUtility.getUserInfoFromRedisV2(any(MultiSearch.class), anyList(), anyMap()))
            .thenReturn(enrichedUserMap);

        // Act
        SBApiResponse result = profileService.findRecommendedMentors(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getResponseCode());
        assertEquals(Constants.OK, result.getParams().getStatus());
        assertEquals(2, result.getResult().get(Constants.COUNT));
    }

    @Test
    void testFindRecommendedMentors_ZeroCount() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0, "size", 10);

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);
        when(connectionService.getCountForRecommendedMentors(userId))
            .thenReturn(0);

        // Act
        SBApiResponse result = profileService.findRecommendedMentors(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getResponseCode());
        assertEquals(HttpStatus.OK.toString(), result.getParams().getStatus());
        assertEquals("Recommended Mentors count is empty", result.getResult().get(Constants.MESSAGE));
    }

    @Test
    void testFindRecommendedMentors_EmptyList() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0, "size", 10);

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);
        when(connectionService.getCountForRecommendedMentors(userId))
            .thenReturn(2);
        when(connectionService.findRecommendationForMentors(userId, request))
            .thenReturn(new ArrayList<>());

        // Act
        SBApiResponse result = profileService.findRecommendedMentors(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getResponseCode());
        assertEquals(HttpStatus.OK.toString(), result.getParams().getStatus());
        assertEquals("No recommendations found for the user", result.getResult().get(Constants.MESSAGE));
    }

    @Test
    void testFindBlockedUsers_Success() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0, "size", 10);

        List<Map<String, String>> blockedUsersList = Arrays.asList(
            Map.of("userId", "blocked1", "reason", "spam"),
            Map.of("userId", "blocked2", "reason", "inappropriate")
        );

        Map<String, Integer> userCount = Map.of(Constants.COUNT, 2);

        ArrayNode enrichedUserMap = mock(ArrayNode.class);
        when(enrichedUserMap.size()).thenReturn(2);

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);
        when(connectionService.findBlockedUsers(userId, request))
            .thenReturn(blockedUsersList);
        when(nodeService.getConnectionsCountByStatus(userId, Constants.Status.BLOCKED, Constants.DIRECTION.OUT))
            .thenReturn(userCount);
        when(redisCacheMgr.getCache(anyString()))
            .thenReturn(null);
        when(iUserUtility.getUserInfoFromRedisV2(any(MultiSearch.class), anyList(), anyMap()))
            .thenReturn(enrichedUserMap);

        // Act
        SBApiResponse result = profileService.findBlockedUsers(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getResponseCode());
        assertEquals(Constants.OK, result.getParams().getStatus());
        assertEquals(2, result.get(Constants.COUNT));
    }

    @Test
    void testFindBlockedUsers_EmptyList() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0, "size", 10);

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);
        when(connectionService.findBlockedUsers(userId, request))
            .thenReturn(new ArrayList<>());

        // Act
        SBApiResponse result = profileService.findBlockedUsers(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getResponseCode());
        assertEquals(HttpStatus.OK.toString(), result.getParams().getStatus());
        assertEquals("Blocked users list is empty", result.getResult().get(Constants.MESSAGE));
    }

    @Test
    void testFindBlockedUsers_Exception() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = Map.of("offset", 0, "size", 10);

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);
        when(connectionService.findBlockedUsers(userId, request))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        SBApiResponse result = profileService.findBlockedUsers(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getResponseCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.toString(), result.getParams().getStatus());
    }

    @Test
    void testFetchTotalConnectionsCountByStatus_Success() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = Map.of(
            Constants.REQUEST, Map.of(
                Constants.FILTER, Map.of(Constants.STATUS, Arrays.asList("pending", "approved")),
                Constants.FACETS, Arrays.asList(Constants.STATUS)
            )
        );

        List<Map<String, Object>> expectedList = Arrays.asList(
            Map.of("status", "pending", "count", 5),
            Map.of("status", "approved", "count", 10)
        );

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);
        when(connectionService.getTotalCountForUsersBasedOnStatus(userId, Arrays.asList("pending", "approved"), Constants.STATUS))
            .thenReturn(expectedList);

        // Act
        SBApiResponse result = profileService.fetchTotalConnectionsCountByStatus(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(expectedList, result.get(Constants.FACETS));
    }

    @Test
    void testFetchTotalConnectionsCountByStatus_EmptyRequest() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = new HashMap<>();

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);

        // Act
        SBApiResponse result = profileService.fetchTotalConnectionsCountByStatus(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getResponseCode());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), result.getParams().getStatus());
    }

    @Test
    void testFetchTotalConnectionsCountByStatus_EmptyFilter() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = Map.of(
            Constants.REQUEST, Map.of(
                Constants.FACETS, Arrays.asList(Constants.STATUS)
            )
        );

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);

        // Act
        SBApiResponse result = profileService.fetchTotalConnectionsCountByStatus(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getResponseCode());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), result.getParams().getStatus());
    }

    @Test
    void testFetchTotalConnectionsCountByStatus_EmptyStatus() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = Map.of(
            Constants.REQUEST, Map.of(
                Constants.FILTER, new HashMap<>(),
                Constants.FACETS, Arrays.asList(Constants.STATUS)
            )
        );

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);

        // Act
        SBApiResponse result = profileService.fetchTotalConnectionsCountByStatus(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getResponseCode());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), result.getParams().getStatus());
    }

    @Test
    void testFetchTotalConnectionsCountByStatus_EmptyFacets() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = Map.of(
            Constants.REQUEST, Map.of(
                Constants.FILTER, Map.of(Constants.STATUS, Arrays.asList("pending", "approved"))
            )
        );

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);

        // Act
        SBApiResponse result = profileService.fetchTotalConnectionsCountByStatus(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getResponseCode());
        assertEquals(HttpStatus.BAD_REQUEST.toString(), result.getParams().getStatus());
    }

    @Test
    void testFetchTotalConnectionsCountByStatus_Exception() {
        // Arrange
        String authToken = "auth-token";
        String userId = "user1";
        Map<String, Object> request = Map.of(
            Constants.REQUEST, Map.of(
                Constants.FILTER, Map.of(Constants.STATUS, Arrays.asList("pending", "approved")),
                Constants.FACETS, Arrays.asList(Constants.STATUS)
            )
        );

        when(accessTokenValidator.fetchUserIdFromAccessToken(any(), any(SBApiResponse.class)))
            .thenReturn(userId);
        when(connectionService.getTotalCountForUsersBasedOnStatus(anyString(), anyList(), anyString()))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        SBApiResponse result = profileService.fetchTotalConnectionsCountByStatus(authToken, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getResponseCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.toString(), result.getParams().getStatus());
    }

    @Test
    void testOnboardNetworkHubUser_Success() {
        // Arrange
        String userId = "user1";
        Map<String, Object> userProfile = Map.of(
            Constants.ROOT_ORG_ID, "org123",
            Constants.PROFILE_DETAILS_KEY, Map.of(
                Constants.PROFESSIONAL_DETAILS, Arrays.asList(
                    Map.of(Constants.DESIGNATION, "Teacher")
                )
            )
        );

        List<Map<String, Object>> userRoleRecords = Arrays.asList(
            Map.of(Constants.ROLE, "teacher", Constants.SCOPE, Arrays.asList(
                Map.of(Constants.ORGANISATION_ID, "org123")
            ))
        );

        when(userUtilityService.readUserDataFromDB(userId))
            .thenReturn(userProfile);
        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), anyList()))
            .thenReturn(userRoleRecords);
        when(connectionService.updateUserProfileInNeo4j(any()))
            .thenReturn(true);

        // Act
        SBApiResponse result = profileService.upsertUserInformation(userId);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getResponseCode());
        assertEquals(HttpStatus.OK.toString(), result.getParams().getStatus());
        assertEquals(Constants.USER_ONBOARDED_NETWORK_HUB, result.getResult().get(Constants.MESSAGE));
    }

    @Test
    void testOnboardNetworkHubUser_UserProfileNotFound() {
        // Arrange
        String userId = "user1";

        when(userUtilityService.readUserDataFromDB(userId))
            .thenReturn(new HashMap<>());

        // Act
        SBApiResponse result = profileService.upsertUserInformation(userId);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.NOT_FOUND, result.getResponseCode());
        assertEquals(HttpStatus.NOT_FOUND.toString(), result.getParams().getStatus());
        assertEquals("User profile not found", result.getParams().getErrmsg());
    }

    @Test
    void testOnboardNetworkHubUser_ProfileDetailsNotFound() {
        // Arrange
        String userId = "user1";
        Map<String, Object> userProfile = Map.of(
            Constants.ROOT_ORG_ID, "org123"
        );

        when(userUtilityService.readUserDataFromDB(userId))
            .thenReturn(userProfile);

        // Act
        SBApiResponse result = profileService.upsertUserInformation(userId);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.NOT_FOUND, result.getResponseCode());
        assertEquals(HttpStatus.NOT_FOUND.toString(), result.getParams().getStatus());
        assertEquals("Profile details not found", result.getParams().getErrmsg());
    }

    @Test
    void testOnboardNetworkHubUser_ProfessionalDetailsNotFound() {
        // Arrange
        String userId = "user1";
        Map<String, Object> userProfile = Map.of(
            Constants.ROOT_ORG_ID, "org123",
            Constants.PROFILE_DETAILS_KEY, new HashMap<>()
        );

        when(userUtilityService.readUserDataFromDB(userId))
            .thenReturn(userProfile);

        // Act
        SBApiResponse result = profileService.upsertUserInformation(userId);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.NOT_FOUND, result.getResponseCode());
        assertEquals(HttpStatus.NOT_FOUND.toString(), result.getParams().getStatus());
        assertEquals("Profile details not found", result.getParams().getErrmsg());
    }

    @Test
    void testOnboardNetworkHubUser_UserRolesNotFound() {
        // Arrange
        String userId = "user1";
        Map<String, Object> userProfile = Map.of(
            Constants.ROOT_ORG_ID, "org123",
            Constants.PROFILE_DETAILS_KEY, Map.of(
                Constants.PROFESSIONAL_DETAILS, Arrays.asList(
                    Map.of(Constants.DESIGNATION, "Teacher")
                )
            )
        );

        when(userUtilityService.readUserDataFromDB(userId))
            .thenReturn(userProfile);
        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), anyList()))
            .thenReturn(new ArrayList<>());

        // Act
        SBApiResponse result = profileService.upsertUserInformation(userId);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.NOT_FOUND, result.getResponseCode());
        assertEquals(HttpStatus.NOT_FOUND.toString(), result.getParams().getStatus());
        assertEquals("User roles not found", result.getParams().getErrmsg());
    }

    @Test
    void testOnboardNetworkHubUser_UpdateProfileFails() {
        // Arrange
        String userId = "user1";
        Map<String, Object> userProfile = Map.of(
            Constants.ROOT_ORG_ID, "org123",
            Constants.PROFILE_DETAILS_KEY, Map.of(
                Constants.PROFESSIONAL_DETAILS, Arrays.asList(
                    Map.of(Constants.DESIGNATION, "Teacher")
                )
            )
        );

        List<Map<String, Object>> userRoleRecords = Arrays.asList(
            Map.of(Constants.ROLE, "teacher", Constants.SCOPE, Arrays.asList(
                Map.of(Constants.ORGANISATION_ID, "org123")
            ))
        );

        when(userUtilityService.readUserDataFromDB(userId))
            .thenReturn(userProfile);
        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), anyList()))
            .thenReturn(userRoleRecords);
        when(connectionService.updateUserProfileInNeo4j(any()))
            .thenReturn(false);

        // Act
        SBApiResponse result = profileService.upsertUserInformation(userId);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getResponseCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.toString(), result.getParams().getStatus());
        assertEquals("Failed to onboard user in network hub", result.getParams().getErrmsg());
    }

    @Test
    void testOnboardNetworkHubUser_Exception() {
        // Arrange
        String userId = "user1";

        when(userUtilityService.readUserDataFromDB(userId))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        SBApiResponse result = profileService.upsertUserInformation(userId);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getResponseCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.toString(), result.getParams().getStatus());
        assertEquals("Error while onboarding user in network hub", result.getParams().getErrmsg());
    }

    @Test
    void testGetUserRoles_Success() {
        // Arrange
        String userId = "user1";
        String rootOrgId = "org123";

        List<Map<String, Object>> userRoleRecords = Arrays.asList(
            Map.of(Constants.ROLE, "teacher", Constants.SCOPE, Arrays.asList(
                Map.of(Constants.ORGANISATION_ID, "org123")
            )),
            Map.of(Constants.ROLE, "mentor", Constants.SCOPE, Arrays.asList(
                Map.of(Constants.ORGANISATION_ID, "org123")
            ))
        );

        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), anyList()))
            .thenReturn(userRoleRecords);

        // Act
        List<String> result = profileService.getUserRoles(userId, rootOrgId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("teacher"));
        assertTrue(result.contains("mentor"));
    }

    @Test
    void testGetUserRoles_ScopeAsNull() {
        // Arrange
        String userId = "user1";
        String rootOrgId = "org123";

        List<Map<String, Object>> userRoleRecords = Arrays.asList(
            Map.of(Constants.ROLE, "teacher", Constants.SCOPE, "")
        );

        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), anyList()))
            .thenReturn(userRoleRecords);

        // Act
        List<String> result = profileService.getUserRoles(userId, rootOrgId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetUserRoles_ScopeMismatch() {
        // Arrange
        String userId = "user1";
        String rootOrgId = "org123";

        List<Map<String, Object>> userRoleRecords = Arrays.asList(
            Map.of(Constants.ROLE, "teacher", Constants.SCOPE, Arrays.asList(
                Map.of(Constants.ORGANISATION_ID, "different-org")
            ))
        );

        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), anyList()))
            .thenReturn(userRoleRecords);

        // Act
        List<String> result = profileService.getUserRoles(userId, rootOrgId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}