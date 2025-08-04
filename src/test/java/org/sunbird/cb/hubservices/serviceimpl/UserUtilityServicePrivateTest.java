package org.sunbird.cb.hubservices.serviceimpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.cb.hubservices.util.ConnectionProperties;
import org.sunbird.cb.hubservices.util.Constants;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.*;

import org.junit.jupiter.api.extension.ExtendWith;
import org.sunbird.cb.hubservices.util.NetworkServerProperties;

@ExtendWith(MockitoExtension.class)
class UserUtilityServicePrivateTest {

    @InjectMocks
    private UserUtilityService userUtilityService;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private NetworkServerProperties networkServerProperties;

    @Mock
    private ConnectionProperties connectionProperties;


    @Test
    void testGetLimitRequest_ZeroSize() throws Exception {

        when(networkServerProperties.getDefaultLimit()).thenReturn(10);
        when(networkServerProperties.getMaxLimit()).thenReturn(50);

        Method method = UserUtilityService.class.getDeclaredMethod("getLimitRequest", int.class);
        method.setAccessible(true);
        int result = (int) method.invoke(userUtilityService, 0);
        assertEquals(10, result);
    }

    @Test
    void testGetLimitRequest_LessThanMax() throws Exception {

        when(networkServerProperties.getDefaultLimit()).thenReturn(10);
        when(networkServerProperties.getMaxLimit()).thenReturn(50);

        Method method = UserUtilityService.class.getDeclaredMethod("getLimitRequest", int.class);
        method.setAccessible(true);
        int result = (int) method.invoke(userUtilityService, 20);
        assertEquals(20, result);
    }

    @Test
    void testGetLimitRequest_GreaterThanMax() throws Exception {

        when(networkServerProperties.getDefaultLimit()).thenReturn(10);
        when(networkServerProperties.getMaxLimit()).thenReturn(50);

        Method method = UserUtilityService.class.getDeclaredMethod("getLimitRequest", int.class);
        method.setAccessible(true);
        int result = (int) method.invoke(userUtilityService, 100);
        assertEquals(50, result);
    }

    @Test
    void testSanitizePersonalDetails_RemovesSensitiveFields() throws Exception {
        ObjectNode personalDetails = JsonNodeFactory.instance.objectNode();
        personalDetails.put(Constants.MOBILE, "9999999999");
        personalDetails.put(Constants.PRIMARY_EMAIL, "test@example.com");
        ObjectNode profileDetails = JsonNodeFactory.instance.objectNode();
        profileDetails.set(Constants.PERSONAL_DETAILS, personalDetails);

        Method method = UserUtilityService.class.getDeclaredMethod("sanitizePersonalDetails", JsonNode.class);
        method.setAccessible(true);
        method.invoke(userUtilityService, profileDetails);

        JsonNode resultNode = profileDetails.get(Constants.PERSONAL_DETAILS);
        assertFalse(resultNode.has(Constants.MOBILE));
        assertFalse(resultNode.has(Constants.PRIMARY_EMAIL));
    }

    @Test
    void testPopulateProfileDetails_WithUserInfo() throws Exception {
        ObjectNode profileDetails = JsonNodeFactory.instance.objectNode();
        profileDetails.put(Constants.VERIFIED_KARMAYOGI, true);

        ObjectNode nNode = JsonNodeFactory.instance.objectNode();
        nNode.put("userId", "user123");

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put(Constants.ROLE, "Admin");
        userInfo.put(Constants.ORGANISATION_ID, "Org1");
        userInfo.put(Constants.DESIGNATION, "Manager");
        userInfo.put(Constants.CREATED_AT, "2025-07-31");
        userInfo.put(Constants.UPDATED_AT, "2025-07-31");

        when(mapper.valueToTree("Admin")).thenReturn(new TextNode("Admin"));

        Method method = UserUtilityService.class.getDeclaredMethod(
                "populateProfileDetails", ObjectNode.class, JsonNode.class, Map.class);
        method.setAccessible(true);
        method.invoke(userUtilityService, profileDetails, nNode, userInfo);

        assertEquals("user123", profileDetails.get("userId").asText());
        assertTrue(profileDetails.has(Constants.ROLE));
        assertEquals("Org1", profileDetails.get(Constants.ROOT_ORG_ID).asText());
    }

    @Test
    void testPopulateProfileDetails_NullUserInfo() throws Exception {
        ObjectNode profileDetails = JsonNodeFactory.instance.objectNode();

        ObjectNode nNode = JsonNodeFactory.instance.objectNode();
        nNode.put("userId", "user123");

        Method method = UserUtilityService.class.getDeclaredMethod(
                "populateProfileDetails", ObjectNode.class, JsonNode.class, Map.class);
        method.setAccessible(true);
        method.invoke(userUtilityService, profileDetails, nNode, null);

        assertEquals("user123", profileDetails.get("userId").asText());
    }
}
