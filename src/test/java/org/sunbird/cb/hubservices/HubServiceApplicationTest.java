package org.sunbird.cb.hubservices;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.web.client.RestTemplate;
import org.sunbird.cb.hubservices.util.ConnectionProperties;

@ExtendWith(MockitoExtension.class)
class HubServiceApplicationTest {

    @Mock
    private ConnectionProperties connectionProperties;

    private HubServiceApplication hubServiceApplication;

    @BeforeEach
    void setUp() {

        hubServiceApplication = new HubServiceApplication(connectionProperties);
    }

    @Test
    void testConstructor() {
        assertNotNull(hubServiceApplication);
    }

    @Test
    void testRestTemplateBean() {
        when(connectionProperties.getClientHttpRequestFactoryTimeout()).thenReturn(5000);
        when(connectionProperties.getClientHttpRequestFactoryPoolingMaxTotalConnections()).thenReturn(200);

        RestTemplate restTemplate = hubServiceApplication.restTemplate();
        
        assertNotNull(restTemplate);
        assertNotNull(restTemplate.getRequestFactory());
    }

    @Test
    void testMain() {
        try (MockedStatic<SpringApplication> mockedSpringApplication = mockStatic(SpringApplication.class)) {
            String[] args = {"--spring.profiles.active=test"};
            
            HubServiceApplication.main(args);
            
            mockedSpringApplication.verify(() -> SpringApplication.run(HubServiceApplication.class, args));
        }
    }
}