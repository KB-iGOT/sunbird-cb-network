package org.sunbird.cb.hubservices.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ConnectionPropertiesTest {

    private ConnectionProperties connectionProperties;

    @BeforeEach
    void setUp() {
        connectionProperties = new ConnectionProperties();
    }

    @Test
    void testAllGettersAndSetters() {
        connectionProperties.setMaxNodeSize(100);
        assertEquals(100, connectionProperties.getMaxNodeSize());

        connectionProperties.setEsHost("localhost");
        assertEquals("localhost", connectionProperties.getEsHost());

        connectionProperties.setEsPort("9200");
        assertEquals("9200", connectionProperties.getEsPort());

        connectionProperties.setEsUser("elastic");
        assertEquals("elastic", connectionProperties.getEsUser());

        connectionProperties.setEsPassword("password");
        assertEquals("password", connectionProperties.getEsPassword());

        connectionProperties.setEsProfileIndex("profile");
        assertEquals("profile", connectionProperties.getEsProfileIndex());

        connectionProperties.setEsProfileIndexType("_doc");
        assertEquals("_doc", connectionProperties.getEsProfileIndexType());

        String[] fields = {"field1", "field2"};
        connectionProperties.setEsProfileSourceFields(fields);
        assertArrayEquals(fields, connectionProperties.getEsProfileSourceFields());

        connectionProperties.setNotificationIp("http://localhost");
        assertEquals("http://localhost", connectionProperties.getNotificationIp());

        connectionProperties.setNotificationEventEndpoint("/notify");
        assertEquals("/notify", connectionProperties.getNotificationEventEndpoint());

        connectionProperties.setNotificationTemplateTargetUrl("targetUrl");
        assertEquals("targetUrl", connectionProperties.getNotificationTemplateTargetUrl());

        connectionProperties.setNotificationTemplateTargetUrlValue("http://example.com");
        assertEquals("http://example.com", connectionProperties.getNotificationTemplateTargetUrlValue());

        connectionProperties.setNotificationTemplateSender("sender");
        assertEquals("sender", connectionProperties.getNotificationTemplateSender());

        connectionProperties.setNotificationTemplateReciepient("recipient");
        assertEquals("recipient", connectionProperties.getNotificationTemplateReciepient());

        connectionProperties.setNotificationTemplateRequest("request");
        assertEquals("request", connectionProperties.getNotificationTemplateRequest());

        connectionProperties.setNotificationTemplateResponse("response");
        assertEquals("response", connectionProperties.getNotificationTemplateResponse());

        connectionProperties.setNotificationTemplateStatus("status");
        assertEquals("status", connectionProperties.getNotificationTemplateStatus());

        connectionProperties.setNotificationv2Sender("v2sender");
        assertEquals("v2sender", connectionProperties.getNotificationv2Sender());

        connectionProperties.setNotificationv2Id("v2id");
        assertEquals("v2id", connectionProperties.getNotificationv2Id());

        connectionProperties.setNotificationv2DeliveryType("email");
        assertEquals("email", connectionProperties.getNotificationv2DeliveryType());

        connectionProperties.setNotificationv2Mode("async");
        assertEquals("async", connectionProperties.getNotificationv2Mode());

        connectionProperties.setNotificationv2RequestBody("request body");
        assertEquals("request body", connectionProperties.getNotificationv2RequestBody());

        connectionProperties.setNotificationv2ResponseBody("response body");
        assertEquals("response body", connectionProperties.getNotificationv2ResponseBody());

        connectionProperties.setNotificationEnabled(true);
        assertTrue(connectionProperties.isNotificationEnabled());

        connectionProperties.setLearnerServiceHost("http://learner");
        assertEquals("http://learner", connectionProperties.getLearnerServiceHost());

        connectionProperties.setUserSearchEndPoint("/search");
        assertEquals("/search", connectionProperties.getUserSearchEndPoint());

        connectionProperties.setUserUpdateEndPoint("/update");
        assertEquals("/update", connectionProperties.getUserUpdateEndPoint());

        connectionProperties.setUserReadEndPoint("/read");
        assertEquals("/read", connectionProperties.getUserReadEndPoint());

        connectionProperties.setUserLabelV3("v3");
        assertEquals("v3", connectionProperties.getUserLabelV3());

        connectionProperties.setRedisUserConnectionRecievedTimeOut(300);
        assertEquals(300, connectionProperties.getRedisUserConnectionRecievedTimeOut().intValue());

        connectionProperties.setRedisUserConnectionRequestedTimeOut(400);
        assertEquals(400, connectionProperties.getRedisUserConnectionRequestedTimeOut().intValue());

        connectionProperties.setRedisUserConnectionEstablishedTimeOut(500);
        assertEquals(500, connectionProperties.getRedisUserConnectionEstablishedTimeOut().intValue());

        connectionProperties.setUserReadV5("v5");
        assertEquals("v5", connectionProperties.getUserReadV5());

        connectionProperties.setClientHttpRequestFactoryTimeout(5000);
        assertEquals(5000, connectionProperties.getClientHttpRequestFactoryTimeout().intValue());

        connectionProperties.setClientHttpRequestFactoryPoolingMaxTotalConnections(200);
        assertEquals(200, connectionProperties.getClientHttpRequestFactoryPoolingMaxTotalConnections().intValue());

        connectionProperties.setClientHttpRequestFactoryPoolingDefaultMaxPerRoute(20);
        assertEquals(20, connectionProperties.getClientHttpRequestFactoryPoolingDefaultMaxPerRoute().intValue());
    }
}