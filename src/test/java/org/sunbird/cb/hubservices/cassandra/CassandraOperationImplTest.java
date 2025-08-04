package org.sunbird.cb.hubservices.cassandra;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Select;

class CassandraOperationImplTest {

    @InjectMocks
    private CassandraOperationImpl cassandraOperation;

    @Mock
    private CassandraConnectionManager connectionManager;

    @Mock
    private Session session;

    @Mock
    private ResultSet resultSet;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetRecordsByProperties_Success() {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("id", "123");
        List<String> fields = Arrays.asList("id", "name");
        List<Map<String, Object>> expectedResponse = Arrays.asList(Map.of("id", "123", "name", "test"));

        when(connectionManager.getSession("keyspace")).thenReturn(session);
        when(session.execute(any(Select.class))).thenReturn(resultSet);

        try (MockedStatic<CassandraUtil> mockedUtil = mockStatic(CassandraUtil.class)) {
            mockedUtil.when(() -> CassandraUtil.createResponse(resultSet)).thenReturn(expectedResponse);

            List<Map<String, Object>> result = cassandraOperation.getRecordsByProperties("keyspace", "table", propertyMap, fields);

            assertEquals(expectedResponse, result);
            verify(connectionManager).getSession("keyspace");
            verify(session).execute(any(Select.class));
        }
    }

    @Test
    void testGetRecordsByProperties_EmptyFields() {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("id", "123");
        List<Map<String, Object>> expectedResponse = Arrays.asList(Map.of("id", "123"));

        when(connectionManager.getSession("keyspace")).thenReturn(session);
        when(session.execute(any(Select.class))).thenReturn(resultSet);

        try (MockedStatic<CassandraUtil> mockedUtil = mockStatic(CassandraUtil.class)) {
            mockedUtil.when(() -> CassandraUtil.createResponse(resultSet)).thenReturn(expectedResponse);

            List<Map<String, Object>> result = cassandraOperation.getRecordsByProperties("keyspace", "table", propertyMap, null);

            assertEquals(expectedResponse, result);
        }
    }

    @Test
    void testGetRecordsByProperties_EmptyPropertyMap() {
        List<String> fields = Arrays.asList("id", "name");
        List<Map<String, Object>> expectedResponse = Arrays.asList(Map.of("id", "123"));

        when(connectionManager.getSession("keyspace")).thenReturn(session);
        when(session.execute(any(Select.class))).thenReturn(resultSet);

        try (MockedStatic<CassandraUtil> mockedUtil = mockStatic(CassandraUtil.class)) {
            mockedUtil.when(() -> CassandraUtil.createResponse(resultSet)).thenReturn(expectedResponse);

            List<Map<String, Object>> result = cassandraOperation.getRecordsByProperties("keyspace", "table", null, fields);

            assertEquals(expectedResponse, result);
        }
    }

    @Test
    void testGetRecordsByProperties_ListProperty() {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("ids", Arrays.asList("1", "2", "3"));
        List<String> fields = Arrays.asList("id");
        List<Map<String, Object>> expectedResponse = Arrays.asList(Map.of("id", "1"));

        when(connectionManager.getSession("keyspace")).thenReturn(session);
        when(session.execute(any(Select.class))).thenReturn(resultSet);

        try (MockedStatic<CassandraUtil> mockedUtil = mockStatic(CassandraUtil.class)) {
            mockedUtil.when(() -> CassandraUtil.createResponse(resultSet)).thenReturn(expectedResponse);

            List<Map<String, Object>> result = cassandraOperation.getRecordsByProperties("keyspace", "table", propertyMap, fields);

            assertEquals(expectedResponse, result);
        }
    }

    @Test
    void testGetRecordsByProperties_NullListProperty() {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("ids", null);
        List<String> fields = Arrays.asList("id");
        List<Map<String, Object>> expectedResponse = Arrays.asList(Map.of("id", "1"));

        when(connectionManager.getSession("keyspace")).thenReturn(session);
        when(session.execute(any(Select.class))).thenReturn(resultSet);

        try (MockedStatic<CassandraUtil> mockedUtil = mockStatic(CassandraUtil.class)) {
            mockedUtil.when(() -> CassandraUtil.createResponse(resultSet)).thenReturn(expectedResponse);

            List<Map<String, Object>> result = cassandraOperation.getRecordsByProperties("keyspace", "table", propertyMap, fields);

            assertEquals(expectedResponse, result);
        }
    }

    @Test
    void testGetRecordsByProperties_Exception() {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("id", "123");
        List<String> fields = Arrays.asList("id");

        when(connectionManager.getSession("keyspace")).thenThrow(new RuntimeException("Connection error"));

        List<Map<String, Object>> result = cassandraOperation.getRecordsByProperties("keyspace", "table", propertyMap, fields);

        assertTrue(result.isEmpty());
        verify(connectionManager).getSession("keyspace");
    }

    @Test
    void testGetRecordsByProperties_MixedProperties() {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("id", "123");
        propertyMap.put("status", Arrays.asList("active", "pending"));
        propertyMap.put("type", "user");
        List<String> fields = Arrays.asList("id", "status");
        List<Map<String, Object>> expectedResponse = Arrays.asList(Map.of("id", "123"));

        when(connectionManager.getSession("keyspace")).thenReturn(session);
        when(session.execute(any(Select.class))).thenReturn(resultSet);

        try (MockedStatic<CassandraUtil> mockedUtil = mockStatic(CassandraUtil.class)) {
            mockedUtil.when(() -> CassandraUtil.createResponse(resultSet)).thenReturn(expectedResponse);

            List<Map<String, Object>> result = cassandraOperation.getRecordsByProperties("keyspace", "table", propertyMap, fields);

            assertEquals(expectedResponse, result);
        }
    }
}