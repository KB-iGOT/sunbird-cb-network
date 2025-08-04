package org.sunbird.cb.hubservices.profile.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.cb.hubservices.exception.BadRequestException;
import org.sunbird.cb.hubservices.util.ConnectionProperties;

@ExtendWith(MockitoExtension.class)
class AutoCompleteServiceTest {

    @Mock
    private ConnectionProperties connectionProperties;

    @Mock
    private RestHighLevelClient esClient;

    @Mock
    private SearchResponse searchResponse;

    @Mock
    private SearchHits searchHits;

    @Mock
    private SearchHit searchHit;

    @InjectMocks
    private AutoCompleteService autoCompleteService;

    @Test
    void testGetUserSearchData_EmptySearchTerm() {
        BadRequestException exception = assertThrows(BadRequestException.class, 
            () -> autoCompleteService.getUserSearchData(""));
        assertEquals("Search term should not be empty!", exception.getMessage());
    }

    @Test
    void testGetUserSearchData_NullSearchTerm() {
        BadRequestException exception = assertThrows(BadRequestException.class, 
            () -> autoCompleteService.getUserSearchData(null));
        assertEquals("Search term should not be empty!", exception.getMessage());
    }

    @Test
    void testGetUserSearchData_ValidSearchTerm() throws IOException {
        when(connectionProperties.getEsProfileIndex()).thenReturn("profile");
        when(connectionProperties.getEsProfileIndexType()).thenReturn("_doc");

        Map<String, Object> personalDetails = new HashMap<>();
        personalDetails.put("firstname", "John");
        personalDetails.put("primaryEmail", "john@example.com");

        Map<String, Object> employmentDetails = new HashMap<>();
        employmentDetails.put("departmentName", "IT");

        Map<String, Object> sourceMap = new HashMap<>();
        sourceMap.put("id", "123");
        sourceMap.put("personalDetails", personalDetails);
        sourceMap.put("employmentDetails", employmentDetails);

        when(esClient.search(any(SearchRequest.class), eq(RequestOptions.DEFAULT))).thenReturn(searchResponse);
        when(searchResponse.getHits()).thenReturn(searchHits);
        when(searchHits.iterator()).thenReturn(List.of(searchHit).iterator());
        when(searchHit.getSourceAsMap()).thenReturn(sourceMap);
        when(searchHit.getScore()).thenReturn(1.0f);

        List<Map<String, Object>> result = autoCompleteService.getUserSearchData("john");

        assertNotNull(result);
        assertEquals(1, result.size());
        Map<String, Object> userResult = result.get(0);
        assertEquals("John", userResult.get("first_name"));
        assertEquals("john@example.com", userResult.get("email"));
        assertEquals("123", userResult.get("wid"));
        assertEquals("IT", userResult.get("department_name"));
        assertEquals(1.0f, userResult.get("rank"));
    }

    @Test
    void testGetUserSearchData_EmptyEmploymentDetails() throws IOException {
        when(connectionProperties.getEsProfileIndex()).thenReturn("profile");
        when(connectionProperties.getEsProfileIndexType()).thenReturn("_doc");

        Map<String, Object> personalDetails = new HashMap<>();
        personalDetails.put("firstname", "Jane");
        personalDetails.put("primaryEmail", "jane@example.com");

        Map<String, Object> sourceMap = new HashMap<>();
        sourceMap.put("id", "456");
        sourceMap.put("personalDetails", personalDetails);
        sourceMap.put("employmentDetails", new HashMap<>());

        when(esClient.search(any(SearchRequest.class), eq(RequestOptions.DEFAULT))).thenReturn(searchResponse);
        when(searchResponse.getHits()).thenReturn(searchHits);
        when(searchHits.iterator()).thenReturn(List.of(searchHit).iterator());
        when(searchHit.getSourceAsMap()).thenReturn(sourceMap);
        when(searchHit.getScore()).thenReturn(0.8f);

        List<Map<String, Object>> result = autoCompleteService.getUserSearchData("jane");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("", result.get(0).get("department_name"));
    }

    @Test
    void testGetUserSearchData_NullEmploymentDetails() throws IOException {
        when(connectionProperties.getEsProfileIndex()).thenReturn("profile");
        when(connectionProperties.getEsProfileIndexType()).thenReturn("_doc");


        Map<String, Object> personalDetails = new HashMap<>();
        personalDetails.put("firstname", "Bob");
        personalDetails.put("primaryEmail", "bob@example.com");

        Map<String, Object> sourceMap = new HashMap<>();
        sourceMap.put("id", "789");
        sourceMap.put("personalDetails", personalDetails);
        sourceMap.put("employmentDetails", null);

        when(esClient.search(any(SearchRequest.class), eq(RequestOptions.DEFAULT))).thenReturn(searchResponse);
        when(searchResponse.getHits()).thenReturn(searchHits);
        when(searchHits.iterator()).thenReturn(List.of(searchHit).iterator());
        when(searchHit.getSourceAsMap()).thenReturn(sourceMap);
        when(searchHit.getScore()).thenReturn(0.5f);

        List<Map<String, Object>> result = autoCompleteService.getUserSearchData("bob");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("", result.get(0).get("department_name"));
    }

    @Test
    void testGetUserSearchData_NullDepartmentName() throws IOException {
        when(connectionProperties.getEsProfileIndex()).thenReturn("profile");
        when(connectionProperties.getEsProfileIndexType()).thenReturn("_doc");

        Map<String, Object> personalDetails = new HashMap<>();
        personalDetails.put("firstname", "Alice");
        personalDetails.put("primaryEmail", "alice@example.com");

        Map<String, Object> employmentDetails = new HashMap<>();
        employmentDetails.put("departmentName", null);

        Map<String, Object> sourceMap = new HashMap<>();
        sourceMap.put("id", "101");
        sourceMap.put("personalDetails", personalDetails);
        sourceMap.put("employmentDetails", employmentDetails);

        when(esClient.search(any(SearchRequest.class), eq(RequestOptions.DEFAULT))).thenReturn(searchResponse);
        when(searchResponse.getHits()).thenReturn(searchHits);
        when(searchHits.iterator()).thenReturn(List.of(searchHit).iterator());
        when(searchHit.getSourceAsMap()).thenReturn(sourceMap);
        when(searchHit.getScore()).thenReturn(0.9f);

        List<Map<String, Object>> result = autoCompleteService.getUserSearchData("alice");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("", result.get(0).get("department_name"));
    }
}