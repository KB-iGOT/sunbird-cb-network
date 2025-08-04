package org.sunbird.cb.hubservices.cassandra;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.sunbird.cb.hubservices.util.Constants;
import org.sunbird.cb.hubservices.util.PropertiesCache;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.Session;

class CassandraConnectionManagerImplTest {

    private CassandraConnectionManagerImpl connectionManager;
    private PropertiesCache propertiesCache;
    private Cluster cluster;
    private Session session;

    @BeforeEach
    void setUp() {
        connectionManager = new CassandraConnectionManagerImpl();
        propertiesCache = mock(PropertiesCache.class);
        cluster = mock(Cluster.class);
        session = mock(Session.class);
    }

    @Test
    void testGetSession_ExistingSession() {
        ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();
        sessionMap.put("keyspace1", session);
        ReflectionTestUtils.setField(connectionManager, "cassandraSessionMap", sessionMap);

        Session result = connectionManager.getSession("keyspace1");

        assertEquals(session, result);
    }

    @Test
    void testGetSession_NewSession() {
        ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(connectionManager, "cassandraSessionMap", sessionMap);
        ReflectionTestUtils.setField(connectionManager, "cluster", cluster);
        
        when(cluster.connect("keyspace2")).thenReturn(session);

        Session result = connectionManager.getSession("keyspace2");

        assertEquals(session, result);
        assertEquals(session, sessionMap.get("keyspace2"));
        verify(cluster).connect("keyspace2");
    }

    @Test
    void testGetConsistencyLevel_ValidLevel() {
        when(propertiesCache.readProperty(Constants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL)).thenReturn("QUORUM");

        try (MockedStatic<PropertiesCache> mockedCache = mockStatic(PropertiesCache.class)) {
            mockedCache.when(PropertiesCache::getInstance).thenReturn(propertiesCache);

            ConsistencyLevel result = ReflectionTestUtils.invokeMethod(connectionManager, "getConsistencyLevel");

            assertEquals(ConsistencyLevel.QUORUM, result);
        }
    }

    @Test
    void testGetConsistencyLevel_BlankLevel() {
        when(propertiesCache.readProperty(Constants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL)).thenReturn("");

        try (MockedStatic<PropertiesCache> mockedCache = mockStatic(PropertiesCache.class)) {
            mockedCache.when(PropertiesCache::getInstance).thenReturn(propertiesCache);

            ConsistencyLevel result = ReflectionTestUtils.invokeMethod(connectionManager, "getConsistencyLevel");

            assertNull(result);
        }
    }

    @Test
    void testGetConsistencyLevel_InvalidLevel() {
        when(propertiesCache.readProperty(Constants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL)).thenReturn("INVALID");

        try (MockedStatic<PropertiesCache> mockedCache = mockStatic(PropertiesCache.class)) {
            mockedCache.when(PropertiesCache::getInstance).thenReturn(propertiesCache);

            ConsistencyLevel result = ReflectionTestUtils.invokeMethod(connectionManager, "getConsistencyLevel");

            assertNull(result);
        }
    }

    @Test
    void testResourceCleanUp() {
        ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();
        sessionMap.put("keyspace1", session);
        ReflectionTestUtils.setField(connectionManager, "cassandraSessionMap", sessionMap);
        ReflectionTestUtils.setField(connectionManager, "cluster", cluster);

        CassandraConnectionManagerImpl.ResourceCleanUp cleanUp = connectionManager.new ResourceCleanUp();

        assertDoesNotThrow(() -> cleanUp.run());

        verify(session).close();
        verify(cluster).close();
    }

    @Test
    void testResourceCleanUp_Exception() {
        ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();
        sessionMap.put("keyspace1", session);
        ReflectionTestUtils.setField(connectionManager, "cassandraSessionMap", sessionMap);
        ReflectionTestUtils.setField(connectionManager, "cluster", cluster);

        CassandraConnectionManagerImpl.ResourceCleanUp cleanUp = connectionManager.new ResourceCleanUp();

        assertDoesNotThrow(() -> cleanUp.run());
    }
}