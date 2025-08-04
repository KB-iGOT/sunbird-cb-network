package org.sunbird.cb.hubservices.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrettyPrintingMapTest {

    @Test
    void testToString_EmptyMap() {
        Map<String, String> emptyMap = new HashMap<>();
        PrettyPrintingMap<String, String> prettyMap = new PrettyPrintingMap<>(emptyMap);
        
        String result = prettyMap.toString();
        
        assertEquals("", result);
    }

    @Test
    void testToString_SingleEntry() {
        Map<String, String> singleMap = new HashMap<>();
        singleMap.put("key1", "value1");
        PrettyPrintingMap<String, String> prettyMap = new PrettyPrintingMap<>(singleMap);
        
        String result = prettyMap.toString();
        
        assertEquals("key1=\"value1\"", result);
    }

    @Test
    void testToString_MultipleEntries() {
        Map<String, String> multiMap = new LinkedHashMap<>();
        multiMap.put("key1", "value1");
        multiMap.put("key2", "value2");
        multiMap.put("key3", "value3");
        PrettyPrintingMap<String, String> prettyMap = new PrettyPrintingMap<>(multiMap);
        
        String result = prettyMap.toString();
        
        assertEquals("key1=\"value1\", key2=\"value2\", key3=\"value3\"", result);
    }

    @Test
    void testToString_IntegerKeyValue() {
        Map<Integer, Integer> intMap = new LinkedHashMap<>();
        intMap.put(1, 100);
        intMap.put(2, 200);
        PrettyPrintingMap<Integer, Integer> prettyMap = new PrettyPrintingMap<>(intMap);
        
        String result = prettyMap.toString();
        
        assertEquals("1=\"100\", 2=\"200\"", result);
    }

    @Test
    void testToString_NullValues() {
        Map<String, String> nullMap = new LinkedHashMap<>();
        nullMap.put("key1", null);
        nullMap.put("key2", "value2");
        PrettyPrintingMap<String, String> prettyMap = new PrettyPrintingMap<>(nullMap);
        
        String result = prettyMap.toString();
        
        assertEquals("key1=\"null\", key2=\"value2\"", result);
    }
}