package org.sunbird.cb.hubservices.cache;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataCacheMgrTest {

    private DataCacheMgr dataCacheMgr;

    @BeforeEach
    void setUp() {
        dataCacheMgr = new DataCacheMgr();
    }

    @Test
    void testPutStringInCache() {
        dataCacheMgr.putStringInCache("key1", "value1");
        assertEquals("value1", dataCacheMgr.getStringFromCache("key1"));
    }

    @Test
    void testPutStringInCache_NullKey() {
        dataCacheMgr.putStringInCache(null, "value");
        assertEquals("value", dataCacheMgr.getStringFromCache(null));
    }

    @Test
    void testPutStringInCache_NullValue() {
        dataCacheMgr.putStringInCache("key", null);
        assertNull(dataCacheMgr.getStringFromCache("key"));
    }

    @Test
    void testPutObjectInCache() {
        Object testObject = new Object();
        dataCacheMgr.putObjectInCache("objKey", testObject);
        assertEquals(testObject, dataCacheMgr.getObjectFromCache("objKey"));
    }

    @Test
    void testPutObjectInCache_NullKey() {
        Object testObject = new Object();
        dataCacheMgr.putObjectInCache(null, testObject);
        assertEquals(testObject, dataCacheMgr.getObjectFromCache(null));
    }

    @Test
    void testPutObjectInCache_NullValue() {
        dataCacheMgr.putObjectInCache("objKey", null);
        assertNull(dataCacheMgr.getObjectFromCache("objKey"));
    }

    @Test
    void testGetStringFromCache_ExistingKey() {
        dataCacheMgr.putStringInCache("existingKey", "existingValue");
        assertEquals("existingValue", dataCacheMgr.getStringFromCache("existingKey"));
    }

    @Test
    void testGetStringFromCache_NonExistingKey() {
        assertEquals("", dataCacheMgr.getStringFromCache("nonExistingKey"));
    }

    @Test
    void testGetStringFromCache_NullKey() {
        assertEquals("", dataCacheMgr.getStringFromCache(null));
    }

    @Test
    void testGetObjectFromCache_ExistingKey() {
        String testObject = "testString";
        dataCacheMgr.putObjectInCache("objKey", testObject);
        assertEquals(testObject, dataCacheMgr.getObjectFromCache("objKey"));
    }

    @Test
    void testGetObjectFromCache_NonExistingKey() {
        assertNull(dataCacheMgr.getObjectFromCache("nonExistingKey"));
    }

    @Test
    void testGetObjectFromCache_NullKey() {
        assertNull(dataCacheMgr.getObjectFromCache(null));
    }

    @Test
    void testCacheOverwrite() {
        dataCacheMgr.putStringInCache("key", "value1");
        dataCacheMgr.putStringInCache("key", "value2");
        assertEquals("value2", dataCacheMgr.getStringFromCache("key"));
    }

    @Test
    void testObjectCacheOverwrite() {
        Object obj1 = "object1";
        Object obj2 = "object2";
        dataCacheMgr.putObjectInCache("key", obj1);
        dataCacheMgr.putObjectInCache("key", obj2);
        assertEquals(obj2, dataCacheMgr.getObjectFromCache("key"));
    }
}