package org.sunbird.cb.hubservices.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiSearchTest {

    @Test
    void testDefaultValues() {
        MultiSearch multiSearch = new MultiSearch();
        
        assertEquals(10, multiSearch.getSize());
        assertEquals(0, multiSearch.getOffset());
        assertNotNull(multiSearch.getSearch());
        assertTrue(multiSearch.getSearch().isEmpty());
    }

    @Test
    void testGettersAndSetters() {
        MultiSearch multiSearch = new MultiSearch();
        List<Search> searchList = new ArrayList<>();
        
        multiSearch.setSize(20);
        multiSearch.setOffset(5);
        multiSearch.setSearch(searchList);
        
        assertEquals(20, multiSearch.getSize());
        assertEquals(5, multiSearch.getOffset());
        assertEquals(searchList, multiSearch.getSearch());
    }

    @Test
    void testToString() {
        MultiSearch multiSearch = new MultiSearch();
        multiSearch.setSize(15);
        multiSearch.setOffset(3);
        
        String result = multiSearch.toString();
        
        assertTrue(result.contains("size=15"));
        assertTrue(result.contains("offset=3"));
        assertTrue(result.contains("search="));
    }
}