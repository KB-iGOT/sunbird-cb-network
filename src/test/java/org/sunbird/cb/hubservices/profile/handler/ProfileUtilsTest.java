package org.sunbird.cb.hubservices.profile.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.sunbird.cb.hubservices.util.ConnectionProperties;
import org.sunbird.cb.hubservices.util.Constants;

@RunWith(MockitoJUnitRunner.class)
class ProfileUtilsTest {

    @InjectMocks
    private ProfileUtils profileUtils;

    @Mock
    private ConnectionProperties connectionProperties;

    @Mock
    private RestTemplate restTemplate;

    @Test
    void testGetUserDefaultFields() {
        // Act
        List<String> result = ProfileUtils.getUserDefaultFields();

        // Assert
        assertNotNull(result);
        assertEquals(8, result.size());
        assertTrue(result.contains(Constants.PROFILE_DETAILS_PROFESSIOANAL_DETAILS));
        assertTrue(result.contains(Constants.PROFILE_DETAILS_EMPLOYMENT_DETAILS));
        assertTrue(result.contains(Constants.PROFILE_DETAILS_PERSONAL_DETAILS));
        assertTrue(result.contains(Constants.PROFILE_DETAILS_VERIFIED_KARMAYOGI));
        assertTrue(result.contains(Constants.USER_ID));
        assertTrue(result.contains(Constants.PROFILE_DETAILS_PROFILE_IMAGE_URL));
        assertTrue(result.contains(Constants.ORGANISATIONS));
        assertTrue(result.contains(Constants.PROFILE_DETAILS_PROFILE_BANNER_IMAGE_URL));
    }

    @Test
    void testURLEnum() {
        // Test all enum values
        assertEquals("/add", ProfileUtils.URL.CREATE.getValue());
        assertEquals("/read", ProfileUtils.URL.READ.getValue());
        assertEquals("/search", ProfileUtils.URL.SEARCH.getValue());
        assertEquals("/update", ProfileUtils.URL.UPDATE.getValue());
    }

    @Test
    void testProfileClass() {
        // Test static constants
        assertEquals("UserProfile", ProfileUtils.Profile.USER_PROFILE);
        assertEquals("id", ProfileUtils.Profile.ID);
        assertEquals("@id", ProfileUtils.Profile.AT_ID);
        assertEquals("userId", ProfileUtils.Profile.USER_ID);
        assertEquals("osid", ProfileUtils.Profile.OSID);
        assertEquals("filters", ProfileUtils.Profile.FILTERS);
        assertEquals("request", ProfileUtils.Profile.REQUEST);
        assertEquals("entityType", ProfileUtils.Profile.ENTITY_TYPE);
        assertEquals("profileDetails", ProfileUtils.Profile.PROFILE_DETAILS);
        assertEquals("professionalDetails", ProfileUtils.Profile.PROFESSIONAL_DETAILS);
        assertEquals("profileImageUrl", ProfileUtils.Profile.PROFILE_IMAGE_URL);
        assertEquals("profileBannerUrl", ProfileUtils.Profile.PROFILE_DETAILS_PROFILE_BANNER_IMAGE_URL);
    }

    @Test
    void testMerge_SimpleMaps() {
        // Arrange
        Map<String, Object> mapLeft = new HashMap<>();
        mapLeft.put("key1", "value1");
        mapLeft.put("key2", "value2");

        Map<String, Object> mapRight = new HashMap<>();
        mapRight.put("key2", "newValue2");
        mapRight.put("key3", "value3");

        // Act
        ProfileUtils.merge(mapLeft, mapRight);

        // Assert
        assertEquals("value1", mapLeft.get("key1"));
        assertEquals("newValue2", mapLeft.get("key2"));
        assertEquals("value3", mapLeft.get("key3"));
        assertEquals(3, mapLeft.size());
    }

    @Test
    void testMerge_WithNestedMaps() {
        // Arrange
        Map<String, Object> nestedLeft = new HashMap<>();
        nestedLeft.put("nestedKey1", "nestedValue1");

        Map<String, Object> mapLeft = new HashMap<>();
        mapLeft.put("key1", "value1");
        mapLeft.put("nested", nestedLeft);

        Map<String, Object> nestedRight = new HashMap<>();
        nestedRight.put("nestedKey2", "nestedValue2");

        Map<String, Object> mapRight = new HashMap<>();
        mapRight.put("nested", nestedRight);

        // Act
        ProfileUtils.merge(mapLeft, mapRight);

        // Assert
        assertEquals("value1", mapLeft.get("key1"));
        Map<String, Object> resultNested = (Map<String, Object>) mapLeft.get("nested");
        assertEquals("nestedValue1", resultNested.get("nestedKey1"));
        assertEquals("nestedValue2", resultNested.get("nestedKey2"));
    }

    @Test
    void testMerge_WithNonMapValue() {
        // Arrange
        Map<String, Object> mapLeft = new HashMap<>();
        mapLeft.put("key1", "value1");

        Map<String, Object> mapRight = new HashMap<>();
        mapRight.put("key1", "newValue1");

        // Act
        ProfileUtils.merge(mapLeft, mapRight);

        // Assert
        assertEquals("newValue1", mapLeft.get("key1"));
    }

    @Test
    void testMerge_EmptyRightMap() {
        // Arrange
        Map<String, Object> mapLeft = new HashMap<>();
        mapLeft.put("key1", "value1");

        Map<String, Object> mapRight = new HashMap<>();

        // Act
        ProfileUtils.merge(mapLeft, mapRight);

        // Assert
        assertEquals("value1", mapLeft.get("key1"));
        assertEquals(1, mapLeft.size());
    }

    @Test
    void testMergeLeaf_WithExistingLeafKey() {
        // Arrange
        Map<String, Object> mapLeft = new HashMap<>();
        List<Map<String, Object>> existingList = new ArrayList<>();
        Map<String, Object> existingItem = new HashMap<>();
        existingItem.put("existingKey", "existingValue");
        existingList.add(existingItem);
        mapLeft.put(ProfileUtils.Profile.PROFESSIONAL_DETAILS, existingList);

        Map<String, Object> mapRight = new HashMap<>();
        mapRight.put("newKey", "newValue");

        // Act
        ProfileUtils.mergeLeaf(mapLeft, mapRight, ProfileUtils.Profile.PROFESSIONAL_DETAILS, "testId");

        // Assert
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) mapLeft.get(ProfileUtils.Profile.PROFESSIONAL_DETAILS);
        Map<String, Object> resultItem = resultList.get(0);
        assertEquals("existingValue", resultItem.get("existingKey"));
        assertEquals("newValue", resultItem.get("newKey"));
    }

    @Test
    void testMergeLeaf_WithHashMapValue() {
        // Arrange
        Map<String, Object> mapLeft = new HashMap<>();
        Map<String, Object> existingMap = new HashMap<>();
        existingMap.put("existingKey", "existingValue");
        mapLeft.put("testKey", existingMap);

        Map<String, Object> mapRight = new HashMap<>();
        mapRight.put("newKey", "newValue");

        // Act
        ProfileUtils.mergeLeaf(mapLeft, mapRight, "testKey", "testId");

        // Assert
        Map<String, Object> resultMap = (Map<String, Object>) mapLeft.get("testKey");
        assertEquals("newValue", resultMap.get("newKey"));
    }

    @Test
    void testMergeLeaf_WithoutExistingLeafKey() {
        // Arrange
        Map<String, Object> mapLeft = new HashMap<>();
        Map<String, Object> mapRight = new HashMap<>();
        mapRight.put("designation", "Teacher");
        mapRight.put("department", "Engineering");

        // Act
        ProfileUtils.mergeLeaf(mapLeft, mapRight, ProfileUtils.Profile.PROFESSIONAL_DETAILS, "testId");

        // Assert
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) mapLeft.get(ProfileUtils.Profile.PROFESSIONAL_DETAILS);
        assertNotNull(resultList);
        assertEquals(1, resultList.size());

        Map<String, Object> resultItem = resultList.get(0);
        assertEquals("Teacher", resultItem.get("designation"));
        assertEquals("Engineering", resultItem.get("department"));
    }

    @Test
    void testMergeLeaf_WithNonProfessionalDetailsKey() {
        // Arrange
        Map<String, Object> mapLeft = new HashMap<>();
        Map<String, Object> mapRight = new HashMap<>();
        mapRight.put("key1", "value1");

        // Act
        ProfileUtils.mergeLeaf(mapLeft, mapRight, "otherKey", "testId");

        // Assert
        assertFalse(mapLeft.containsKey("otherKey"));
    }

    @Test
    void testMerge_WithNullValues() {
        // Arrange
        Map<String, Object> mapLeft = new HashMap<>();
        mapLeft.put("key1", null);

        Map<String, Object> mapRight = new HashMap<>();
        mapRight.put("key1", "newValue");

        // Act
        ProfileUtils.merge(mapLeft, mapRight);

        // Assert
        assertEquals("newValue", mapLeft.get("key1"));
    }

    @Test
    void testMerge_WithEmptyMaps() {
        // Arrange
        Map<String, Object> mapLeft = new HashMap<>();
        Map<String, Object> mapRight = new HashMap<>();

        // Act
        ProfileUtils.merge(mapLeft, mapRight);

        // Assert
        assertTrue(mapLeft.isEmpty());
    }

    @Test
    void testMergeLeaf_WithEmptyRightMap() {
        // Arrange
        Map<String, Object> mapLeft = new HashMap<>();
        Map<String, Object> mapRight = new HashMap<>();

        // Act
        ProfileUtils.mergeLeaf(mapLeft, mapRight, ProfileUtils.Profile.PROFESSIONAL_DETAILS, "testId");

        // Assert
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) mapLeft.get(ProfileUtils.Profile.PROFESSIONAL_DETAILS);
        assertNotNull(resultList);
        assertEquals(1, resultList.size());
        assertTrue(resultList.get(0).isEmpty());
    }

    @Test
    void testMergeLeaf_WithNonArrayListValue() {
        // Arrange
        Map<String, Object> mapLeft = new HashMap<>();
        mapLeft.put("testKey", "notAList");

        Map<String, Object> mapRight = new HashMap<>();
        mapRight.put("newKey", "newValue");

        // Act
        ProfileUtils.mergeLeaf(mapLeft, mapRight, "testKey", "testId");

        // Assert
        // Should not throw exception and should not modify the map
        assertEquals("notAList", mapLeft.get("testKey"));
    }

    private void injectMockRestTemplate() throws Exception {
        MockitoAnnotations.openMocks(this);
        Field field = ProfileUtils.class.getDeclaredField("restTemplate");
        field.setAccessible(true);

        // Remove final modifier
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, restTemplate);
    }

    @Test
    void testReadUserProfiles_Success() throws Exception {
        injectMockRestTemplate();
        String userId = "user1";
        String url = "http://localhost:8080/api/read/" + userId;

        Map<String, Object> profileDetails = Map.of("name", "John Doe", "email", "john@example.com");
        Map<String, Object> response = Map.of(ProfileUtils.Profile.PROFILE_DETAILS, profileDetails);
        Map<String, Object> result = Map.of(Constants.RESPONSE, response);
        Map<String, Object> profileResponse = Map.of("responseCode", "OK", "result", result);

        ResponseEntity<Map> mockResponse = new ResponseEntity<>(profileResponse, HttpStatus.OK);

        when(connectionProperties.getLearnerServiceHost()).thenReturn("http://localhost:8080");
        when(connectionProperties.getUserReadEndPoint()).thenReturn("/api/read/");
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponse);

        Map<String, Object> result1 = profileUtils.readUserProfiles(userId);

        assertNotNull(result1);
        assertEquals(profileDetails, result1);
    }

    @Test
    void testReadUserProfiles_ResponseCodeNotOk() throws Exception {
        injectMockRestTemplate();
        String userId = "user1";
        String url = "http://localhost:8080/api/read/" + userId;

        Map<String, Object> profileResponse = Map.of("responseCode", "ERROR");
        ResponseEntity<Map> mockResponse = new ResponseEntity<>(profileResponse, HttpStatus.OK);

        when(connectionProperties.getLearnerServiceHost()).thenReturn("http://localhost:8080");
        when(connectionProperties.getUserReadEndPoint()).thenReturn("/api/read/");
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponse);

        Map<String, Object> result1 = profileUtils.readUserProfiles(userId);
        assertTrue(result1.isEmpty());
    }

    @Test
    void testReadUserProfiles_NullBody() throws Exception {
        injectMockRestTemplate();
        String userId = "user1";
        String url = "http://localhost:8080/api/read/" + userId;

        ResponseEntity<Map> mockResponse = new ResponseEntity<>(null, HttpStatus.OK);

        when(connectionProperties.getLearnerServiceHost()).thenReturn("http://localhost:8080");
        when(connectionProperties.getUserReadEndPoint()).thenReturn("/api/read/");
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponse);

        Map<String, Object> result1 = profileUtils.readUserProfiles(userId);
        assertTrue(result1.isEmpty());
    }

    @Test
    void testReadUserProfiles_ResultWithoutResponseKey() throws Exception {
        injectMockRestTemplate();
        String userId = "user1";
        String url = "http://localhost:8080/api/read/" + userId;

        Map<String, Object> result = Collections.emptyMap();
        Map<String, Object> profileResponse = Map.of("responseCode", "OK", "result", result);
        ResponseEntity<Map> mockResponse = new ResponseEntity<>(profileResponse, HttpStatus.OK);

        when(connectionProperties.getLearnerServiceHost()).thenReturn("http://localhost:8080");
        when(connectionProperties.getUserReadEndPoint()).thenReturn("/api/read/");
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponse);

        Map<String, Object> result1 = profileUtils.readUserProfiles(userId);
        assertTrue(result1.isEmpty());
    }
}