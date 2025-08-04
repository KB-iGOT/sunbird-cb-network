package org.sunbird.cb.hubservices.common.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.sunbird.cb.hubservices.common.model.KeyData;
import org.sunbird.cb.hubservices.util.Constants;
import org.sunbird.cb.hubservices.util.PropertiesCache;

class KeyManagerTest {

    private KeyManager keyManager;
    private PropertiesCache propertiesCache;

    @BeforeEach
    void setUp() {
        keyManager = new KeyManager();
        propertiesCache = mock(PropertiesCache.class);
        ReflectionTestUtils.setField(KeyManager.class, "keyMap", new java.util.HashMap<>());
    }

    @Test
    void testInit_Success() throws Exception {
        String testKeyContent = "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4f5wg5l2hKsTeNem/V41fGnJm6gOdrj8ym3rFkEjWT2btf02uSxUyDpllVOI5g1PrlIVfYCL5q/a1AuqadjTkJI=\n-----END PUBLIC KEY-----";
        Path mockPath = mock(Path.class);
        Path mockFileName = mock(Path.class);
        
        when(propertiesCache.getProperty(Constants.ACCESS_TOKEN_PUBLICKEY_BASEPATH)).thenReturn("/test/path");
        when(mockPath.getFileName()).thenReturn(mockFileName);
        when(mockFileName.toString()).thenReturn("test-key");

        try (MockedStatic<PropertiesCache> propMock = mockStatic(PropertiesCache.class);
             MockedStatic<Files> filesMock = mockStatic(Files.class);
             MockedStatic<Paths> pathsMock = mockStatic(Paths.class);
             MockedStatic<Base64Util> base64Mock = mockStatic(Base64Util.class)) {
            
            propMock.when(PropertiesCache::getInstance).thenReturn(propertiesCache);
            pathsMock.when(() -> Paths.get("/test/path")).thenReturn(mockPath);
            pathsMock.when(() -> Paths.get("file1")).thenReturn(mockPath);
            
            Stream<Path> mockStream = Arrays.asList(mockPath).stream();
            filesMock.when(() -> Files.walk(mockPath)).thenReturn(mockStream);
            filesMock.when(() -> Files.isRegularFile(mockPath)).thenReturn(true);
            filesMock.when(() -> Files.lines(mockPath, java.nio.charset.StandardCharsets.UTF_8))
                     .thenReturn(Arrays.asList(testKeyContent).stream());
            
            base64Mock.when(() -> Base64Util.decode(any(byte[].class), eq(Base64Util.DEFAULT)))
                     .thenReturn("test".getBytes());

            keyManager.init();

            // Verify no exception thrown
            assertDoesNotThrow(() -> keyManager.init());
        }
    }

    @Test
    void testInit_FileWalkException() {
        when(propertiesCache.getProperty(Constants.ACCESS_TOKEN_PUBLICKEY_BASEPATH)).thenReturn("/invalid/path");

        try (MockedStatic<PropertiesCache> propMock = mockStatic(PropertiesCache.class);
             MockedStatic<Files> filesMock = mockStatic(Files.class);
             MockedStatic<Paths> pathsMock = mockStatic(Paths.class)) {
            
            propMock.when(PropertiesCache::getInstance).thenReturn(propertiesCache);
            Path mockPath = mock(Path.class);
            pathsMock.when(() -> Paths.get("/invalid/path")).thenReturn(mockPath);
            filesMock.when(() -> Files.walk(mockPath)).thenThrow(new RuntimeException("Path not found"));

            assertDoesNotThrow(() -> keyManager.init());
        }
    }

    @Test
    void testInit_KeyLoadException() throws Exception {
        Path mockPath = mock(Path.class);
        Path mockFileName = mock(Path.class);
        
        when(propertiesCache.getProperty(Constants.ACCESS_TOKEN_PUBLICKEY_BASEPATH)).thenReturn("/test/path");
        when(mockPath.getFileName()).thenReturn(mockFileName);
        when(mockFileName.toString()).thenReturn("test-key");

        try (MockedStatic<PropertiesCache> propMock = mockStatic(PropertiesCache.class);
             MockedStatic<Files> filesMock = mockStatic(Files.class);
             MockedStatic<Paths> pathsMock = mockStatic(Paths.class)) {
            
            propMock.when(PropertiesCache::getInstance).thenReturn(propertiesCache);
            pathsMock.when(() -> Paths.get("/test/path")).thenReturn(mockPath);
            pathsMock.when(() -> Paths.get("file1")).thenReturn(mockPath);
            
            Stream<Path> mockStream = Arrays.asList(mockPath).stream();
            filesMock.when(() -> Files.walk(mockPath)).thenReturn(mockStream);
            filesMock.when(() -> Files.isRegularFile(mockPath)).thenReturn(true);
            filesMock.when(() -> Files.lines(mockPath, java.nio.charset.StandardCharsets.UTF_8))
                     .thenThrow(new RuntimeException("File read error"));

            assertDoesNotThrow(() -> keyManager.init());
        }
    }

    @Test
    void testGetPublicKey_ExistingKey() {
        java.util.Map<String, KeyData> keyMap = new java.util.HashMap<>();
        KeyData testKeyData = new KeyData("test-key", mock(PublicKey.class));
        keyMap.put("test-key", testKeyData);
        ReflectionTestUtils.setField(KeyManager.class, "keyMap", keyMap);

        KeyData result = keyManager.getPublicKey("test-key");

        assertEquals(testKeyData, result);
    }

    @Test
    void testGetPublicKey_NonExistingKey() {
        KeyData result = keyManager.getPublicKey("non-existing-key");

        assertNull(result);
    }

//    @Test
//    void testLoadPublicKey_ValidKey() throws Exception {
//        String validKey = "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4f5wg5l2hKsTeNem/V41fGnJm6gOdrj8ym3rFkEjWT2btf02uSxUyDpllVOI5g1PrlIVfYCL5q/a1AuqadjTkJI=\n-----END PUBLIC KEY-----";
//
//        try (MockedStatic<Base64Util> base64Mock = mockStatic(Base64Util.class)) {
//            base64Mock.when(() -> Base64Util.decode(any(byte[].class), eq(Base64Util.DEFAULT)))
//                     .thenReturn(new byte[294]); // Valid RSA key length
//
//            PublicKey result = KeyManager.loadPublicKey(validKey);
//
//            assertNotNull(result);
//        }
//    }

    @Test
    void testLoadPublicKey_InvalidKey() {
        String invalidKey = "invalid key content";
        
        try (MockedStatic<Base64Util> base64Mock = mockStatic(Base64Util.class)) {
            base64Mock.when(() -> Base64Util.decode(any(byte[].class), eq(Base64Util.DEFAULT)))
                     .thenReturn("invalid".getBytes());

            assertThrows(Exception.class, () -> KeyManager.loadPublicKey(invalidKey));
        }
    }

    @Test
    void testLoadPublicKey_Base64Exception() {
        String validKey = "-----BEGIN PUBLIC KEY-----\ntest\n-----END PUBLIC KEY-----";
        
        try (MockedStatic<Base64Util> base64Mock = mockStatic(Base64Util.class)) {
            base64Mock.when(() -> Base64Util.decode(any(byte[].class), eq(Base64Util.DEFAULT)))
                     .thenThrow(new IllegalArgumentException("Invalid Base64"));

            assertThrows(Exception.class, () -> KeyManager.loadPublicKey(validKey));
        }
    }
}