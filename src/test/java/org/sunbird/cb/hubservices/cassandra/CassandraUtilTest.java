package org.sunbird.cb.hubservices.cassandra;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.ColumnDefinitions.Definition;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

class CassandraUtilTest {

    @Test
    void testGetPreparedStatement() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "123");
        map.put("name", "test");

        String result = CassandraUtil.getPreparedStatement("keyspace", "table", map);

        assertTrue(result.contains("INSERT INTO keyspace.table"));
        assertTrue(result.contains("VALUES"));
        assertTrue(result.contains("?,?"));
    }

    @Test
    void testGetPreparedStatement_SingleColumn() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "123");

        String result = CassandraUtil.getPreparedStatement("ks", "tbl", map);

        assertTrue(result.contains("INSERT INTO ks.tbl"));
    }

    @Test
    void testCreateResponse_List() {
        ResultSet resultSet = mock(ResultSet.class);
        Row row = mock(Row.class);
        ColumnDefinitions columnDefs = mock(ColumnDefinitions.class);
        Definition def1 = mock(Definition.class);
        Definition def2 = mock(Definition.class);
        CassandraPropertyReader propertyReader = mock(CassandraPropertyReader.class);

        when(resultSet.getColumnDefinitions()).thenReturn(columnDefs);
        when(columnDefs.asList()).thenReturn(Arrays.asList(def1, def2));
        when(def1.getName()).thenReturn("col1");
        when(def2.getName()).thenReturn("col2");
        when(resultSet.iterator()).thenReturn(Arrays.asList(row).iterator());
        when(row.getObject("col1")).thenReturn("value1");
        when(row.getObject("col2")).thenReturn("value2");

        try (MockedStatic<CassandraPropertyReader> mockedStatic = mockStatic(CassandraPropertyReader.class)) {
            mockedStatic.when(CassandraPropertyReader::getInstance).thenReturn(propertyReader);
            when(propertyReader.readProperty("col1")).thenReturn("mappedCol1 ");
            when(propertyReader.readProperty("col2")).thenReturn("mappedCol2 ");

            List<Map<String, Object>> result = CassandraUtil.createResponse(resultSet);

            assertEquals(1, result.size());
//            assertEquals("value1", result.get(0).get("mappedCol1"));
//            assertEquals("value2", result.get(0).get("mappedCol2"));
        }
    }

    @Test
    void testCreateResponse_Map() {
        ResultSet resultSet = mock(ResultSet.class);
        Row row = mock(Row.class);
        ColumnDefinitions columnDefs = mock(ColumnDefinitions.class);
        Definition def1 = mock(Definition.class);
        Definition def2 = mock(Definition.class);
        CassandraPropertyReader propertyReader = mock(CassandraPropertyReader.class);

        when(resultSet.getColumnDefinitions()).thenReturn(columnDefs);
        when(columnDefs.asList()).thenReturn(Arrays.asList(def1, def2));
        when(def1.getName()).thenReturn("id");
        when(def2.getName()).thenReturn("name");
        when(resultSet.iterator()).thenReturn(Arrays.asList(row).iterator());
        when(row.getObject("id")).thenReturn("key1");
        when(row.getObject("name")).thenReturn("value1");

        try (MockedStatic<CassandraPropertyReader> mockedStatic = mockStatic(CassandraPropertyReader.class)) {
            mockedStatic.when(CassandraPropertyReader::getInstance).thenReturn(propertyReader);
            when(propertyReader.readProperty("id")).thenReturn("id ");
            when(propertyReader.readProperty("name")).thenReturn("name ");

            Map<String, Object> result = CassandraUtil.createResponse(resultSet, "id");

            assertEquals(1, result.size());
            assertTrue(result.containsKey("key1"));
            Map<String, Object> innerMap = (Map<String, Object>) result.get("key1");
            assertEquals("key1", innerMap.get("id"));
            assertEquals("value1", innerMap.get("name"));
        }
    }

    @Test
    void testFetchColumnsMapping() {
        ResultSet resultSet = mock(ResultSet.class);
        ColumnDefinitions columnDefs = mock(ColumnDefinitions.class);
        Definition def1 = mock(Definition.class);
        Definition def2 = mock(Definition.class);
        CassandraPropertyReader propertyReader = mock(CassandraPropertyReader.class);

        when(resultSet.getColumnDefinitions()).thenReturn(columnDefs);
        when(columnDefs.asList()).thenReturn(Arrays.asList(def1, def2));
        when(def1.getName()).thenReturn("col1");
        when(def2.getName()).thenReturn("col2");

        try (MockedStatic<CassandraPropertyReader> mockedStatic = mockStatic(CassandraPropertyReader.class)) {
            mockedStatic.when(CassandraPropertyReader::getInstance).thenReturn(propertyReader);
            when(propertyReader.readProperty("col1")).thenReturn("mappedCol1 ");
            when(propertyReader.readProperty("col2")).thenReturn("mappedCol2 ");

            Map<String, String> result = CassandraUtil.fetchColumnsMapping(resultSet);

            assertEquals(2, result.size());
//            assertEquals("col1", result.get("mappedCol1"));
//            assertEquals("col2", result.get("mappedCol2"));
        }
    }
}