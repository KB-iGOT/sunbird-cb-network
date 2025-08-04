package org.sunbird.cb.hubservices.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchTest {

    @Test
    void testGettersAndSetters() {
        Search search = new Search();
        List<Object> values = Arrays.asList("value1", "value2", 123);
        
        search.setField("testField");
        search.setValues(values);
        
        assertEquals("testField", search.getField());
        assertEquals(values, search.getValues());
    }
}