package org.sunbird.cb.hubservices.profile.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoCompleteControllerTest {

    @Mock
    private AutoCompleteService autoCompleteService;

    @InjectMocks
    private AutoCompleteController autoCompleteController;

    private List<Map<String, Object>> mockUserData;

    @BeforeEach
    void setUp() {
        mockUserData = new ArrayList<>();
        Map<String, Object> user1 = new HashMap<>();
        user1.put("id", "user1");
        user1.put("name", "John Doe");
        mockUserData.add(user1);
    }

    @Test
    void testGetUserSearchData_Success() throws Exception {
        String searchString = "john";

        when(autoCompleteService.getUserSearchData(searchString)).thenReturn(mockUserData);

        ResponseEntity<List<Map<String, Object>>> response = autoCompleteController.getUserSearchData(searchString);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockUserData, response.getBody());
        verify(autoCompleteService).getUserSearchData(searchString);
    }

    @Test
    void testGetUserSearchData_EmptyResult() throws Exception {
        String searchString = "nonexistent";
        List<Map<String, Object>> emptyList = new ArrayList<>();

        when(autoCompleteService.getUserSearchData(searchString)).thenReturn(emptyList);

        ResponseEntity<List<Map<String, Object>>> response = autoCompleteController.getUserSearchData(searchString);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(emptyList, response.getBody());
        verify(autoCompleteService).getUserSearchData(searchString);
    }

    @Test
    void testGetUserSearchData_ServiceThrowsException() throws Exception {
        String searchString = "error";

        when(autoCompleteService.getUserSearchData(searchString)).thenThrow(new RuntimeException("Service error"));

        assertThrows(RuntimeException.class, () -> {
            autoCompleteController.getUserSearchData(searchString);
        });

        verify(autoCompleteService).getUserSearchData(searchString);
    }
}