package org.sunbird.cb.hubservices.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class RequestHandlerServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private RequestHandlerServiceImpl requestHandlerService;

    private Map<String, String> headers;
    private Map<String, Object> requestBody;
    private Map<String, Object> responseBody;

    @BeforeEach
    void setUp() {
        headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");
        
        requestBody = new HashMap<>();
        requestBody.put("key", "value");
        
        responseBody = new HashMap<>();
        responseBody.put("result", "success");
    }

    @Test
    void testFetchResultUsingPost_Success() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseBody);

        Map<String, Object> result = requestHandlerService.fetchResultUsingPost("http://test.com", requestBody, headers);

        assertNotNull(result);
        assertEquals("success", result.get("result"));
        verify(restTemplate).postForObject(anyString(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testFetchResultUsingPost_WithNullHeaders() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseBody);

        Map<String, Object> result = requestHandlerService.fetchResultUsingPost("http://test.com", requestBody, null);

        assertNotNull(result);
        assertEquals("success", result.get("result"));
    }

    @Test
    void testFetchResultUsingPost_HttpClientErrorException() {
        String errorResponse = "{\"error\":\"Bad Request\"}";
        HttpClientErrorException exception = mock(HttpClientErrorException.class);
        when(exception.getResponseBodyAsString()).thenReturn(errorResponse);
        
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(exception);

        Map<String, Object> result = requestHandlerService.fetchResultUsingPost("http://test.com", requestBody, headers);

        assertNotNull(result);
        assertEquals("Bad Request", result.get("error"));
    }

    @Test
    void testFetchResultUsingPost_HttpClientErrorException_InvalidJson() {
        HttpClientErrorException exception = mock(HttpClientErrorException.class);
        when(exception.getResponseBodyAsString()).thenReturn("invalid json");
        
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(exception);

        Map<String, Object> result = requestHandlerService.fetchResultUsingPost("http://test.com", requestBody, headers);

        assertNull(result);
    }

    @Test
    void testFetchUsingGetWithHeadersProfile_Success() {
        ResponseEntity<Map> responseEntity = mock(ResponseEntity.class);
        when(responseEntity.getBody()).thenReturn(responseBody);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        Object result = requestHandlerService.fetchUsingGetWithHeadersProfile("http://test.com", headers);

        assertNotNull(result);
        assertEquals(responseBody, result);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testFetchUsingGetWithHeadersProfile_WithNullHeaders() {
        ResponseEntity<Map> responseEntity = mock(ResponseEntity.class);
        when(responseEntity.getBody()).thenReturn(responseBody);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        Object result = requestHandlerService.fetchUsingGetWithHeadersProfile("http://test.com", null);

        assertNotNull(result);
        assertEquals(responseBody, result);
    }

    @Test
    void testFetchUsingGetWithHeadersProfile_HttpClientErrorException() {
        String errorResponse = "{\"error\":\"Not Found\"}";
        HttpClientErrorException exception = mock(HttpClientErrorException.class);
        when(exception.getResponseBodyAsString()).thenReturn(errorResponse);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(exception);

        Object result = requestHandlerService.fetchUsingGetWithHeadersProfile("http://test.com", headers);

        assertNotNull(result);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals("Not Found", resultMap.get("error"));
    }

    @Test
    void testFetchUsingGetWithHeadersProfile_HttpClientErrorException_InvalidJson() {
        HttpClientErrorException exception = mock(HttpClientErrorException.class);
        when(exception.getResponseBodyAsString()).thenReturn("invalid json");
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(exception);

        Object result = requestHandlerService.fetchUsingGetWithHeadersProfile("http://test.com", headers);

        assertNull(result);
    }

    @Test
    void testFetchUsingGetWithHeadersProfile_GeneralException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection error"));

        Object result = requestHandlerService.fetchUsingGetWithHeadersProfile("http://test.com", headers);

        assertNull(result);
    }
}