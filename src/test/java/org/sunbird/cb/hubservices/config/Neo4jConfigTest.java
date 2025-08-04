package org.sunbird.cb.hubservices.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.exceptions.AuthenticationException;
import org.neo4j.driver.v1.exceptions.ServiceUnavailableException;
import org.sunbird.cb.hubservices.exception.GraphException;
import org.sunbird.cb.hubservices.util.Constants;
import org.sunbird.cb.hubservices.util.PropertiesCache;

class Neo4jConfigTest {

    private Neo4jConfig neo4jConfig;
    private PropertiesCache propertiesCache;
    private Driver mockDriver;

    @BeforeEach
    void setUp() {
        neo4jConfig = new Neo4jConfig();
        propertiesCache = mock(PropertiesCache.class);
        mockDriver = mock(Driver.class);
    }

    @Test
    void testNeo4jDriver_WithAuth() {
        when(propertiesCache.getProperty(Constants.NEO4J_AUTH_ENABLED)).thenReturn("true");
        when(propertiesCache.getProperty(Constants.NEO4J_HOST_URL)).thenReturn("bolt://localhost:7687");
        when(propertiesCache.getProperty(Constants.NEO4J_USER_NAME)).thenReturn("neo4j");
        when(propertiesCache.getProperty(Constants.NEO4J_PASSWORD)).thenReturn("password");

        try (MockedStatic<PropertiesCache> propMock = mockStatic(PropertiesCache.class);
             MockedStatic<GraphDatabase> graphMock = mockStatic(GraphDatabase.class);
             MockedStatic<AuthTokens> authMock = mockStatic(AuthTokens.class)) {
            
            propMock.when(PropertiesCache::getInstance).thenReturn(propertiesCache);
            authMock.when(() -> AuthTokens.basic("neo4j", "password")).thenReturn(null);
            graphMock.when(() -> GraphDatabase.driver("bolt://localhost:7687", (org.neo4j.driver.v1.AuthToken) null))
                    .thenReturn(mockDriver);

            Driver result = neo4jConfig.Neo4jDriver();

            assertEquals(mockDriver, result);
        }
    }

    @Test
    void testNeo4jDriver_AuthenticationException() {
        when(propertiesCache.getProperty(Constants.NEO4J_AUTH_ENABLED)).thenReturn("true");
        when(propertiesCache.getProperty(Constants.NEO4J_HOST_URL)).thenReturn("bolt://localhost:7687");
        when(propertiesCache.getProperty(Constants.NEO4J_USER_NAME)).thenReturn("neo4j");
        when(propertiesCache.getProperty(Constants.NEO4J_PASSWORD)).thenReturn("wrong_password");

        try (MockedStatic<PropertiesCache> propMock = mockStatic(PropertiesCache.class);
             MockedStatic<GraphDatabase> graphMock = mockStatic(GraphDatabase.class);
             MockedStatic<AuthTokens> authMock = mockStatic(AuthTokens.class)) {
            
            propMock.when(PropertiesCache::getInstance).thenReturn(propertiesCache);
            authMock.when(() -> AuthTokens.basic("neo4j", "wrong_password")).thenReturn(null);
            
            AuthenticationException authException = mock(AuthenticationException.class);
            when(authException.code()).thenReturn("Neo.ClientError.Security.Unauthorized");
            when(authException.getMessage()).thenReturn("Authentication failed");
            
            graphMock.when(() -> GraphDatabase.driver("bolt://localhost:7687", (org.neo4j.driver.v1.AuthToken) null))
                    .thenThrow(authException);

            GraphException exception = assertThrows(GraphException.class, () -> neo4jConfig.Neo4jDriver());
            
            assertEquals("Neo.ClientError.Security.Unauthorized", exception.getErrCode());
            assertEquals("Authentication failed", exception.getMessage());
        }
    }

    @Test
    void testNeo4jDriver_WithoutAuth_ElseBranch() {
        when(propertiesCache.getProperty(Constants.NEO4J_AUTH_ENABLED)).thenReturn("false");
        when(propertiesCache.getProperty(Constants.NEO$J_TIMEOUT)).thenReturn("30");
        when(propertiesCache.getProperty(Constants.NEO4J_HOST_URL)).thenReturn("bolt://localhost:7687");

        try (MockedStatic<PropertiesCache> propMock = mockStatic(PropertiesCache.class);
             MockedStatic<GraphDatabase> graphMock = mockStatic(GraphDatabase.class)) {

            propMock.when(PropertiesCache::getInstance).thenReturn(propertiesCache);

            // Mock GraphDatabase.driver(host, config) to return mock driver
            graphMock.when(() -> GraphDatabase.driver(eq("bolt://localhost:7687"), any(Config.class)))
                    .thenReturn(mockDriver);

            Driver result = neo4jConfig.Neo4jDriver();

            assertNotNull(result);
            assertEquals(mockDriver, result);
        }
    }

}