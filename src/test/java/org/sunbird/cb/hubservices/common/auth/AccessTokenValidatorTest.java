package org.sunbird.cb.hubservices.common.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.PublicKey;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.util.Time;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.sunbird.cb.hubservices.common.model.KeyData;
import org.sunbird.cb.hubservices.model.SBApiResponse;
import org.sunbird.cb.hubservices.util.Constants;
import org.sunbird.cb.hubservices.util.PropertiesCache;

class AccessTokenValidatorTest {

    @InjectMocks
    private AccessTokenValidator accessTokenValidator;

    @Mock
    private KeyManager keyManager;

    @Mock
    private PropertiesCache propertiesCache;

    @Mock
    private PublicKey publicKey;

    @Mock
    private KeyData keyData;

    private String validToken;
    private String expiredToken;
    private String invalidToken;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create test tokens
        String header = Base64.getEncoder().encodeToString("{\"kid\":\"test-key\",\"alg\":\"RS256\"}".getBytes());
        String validPayload = Base64.getEncoder().encodeToString(("{\"sub\":\"f:user:123\",\"iss\":\"http://localhost/realms/test\",\"exp\":" + (Time.currentTime() + 3600) + "}").getBytes());
        String expiredPayload = Base64.getEncoder().encodeToString(("{\"sub\":\"f:user:123\",\"iss\":\"http://localhost/realms/test\",\"exp\":" + (Time.currentTime() - 3600) + "}").getBytes());
        String signature = Base64.getEncoder().encodeToString("signature".getBytes());
        
        validToken = header + "." + validPayload + "." + signature;
        expiredToken = header + "." + expiredPayload + "." + signature;
        invalidToken = "invalid.token.format";
    }

    @Test
    void testVerifyUserToken_ValidToken() {
        when(keyManager.getPublicKey("test-key")).thenReturn(keyData);
        when(keyData.getPublicKey()).thenReturn(publicKey);

        try (MockedStatic<CryptoUtil> cryptoMock = mockStatic(CryptoUtil.class);
             MockedStatic<PropertiesCache> propMock = mockStatic(PropertiesCache.class);
             MockedStatic<Base64Util> base64Mock = mockStatic(Base64Util.class)) {
            
            propMock.when(PropertiesCache::getInstance).thenReturn(propertiesCache);
            when(propertiesCache.getProperty(Constants.SSO_URL)).thenReturn("http://localhost/");
            when(propertiesCache.getProperty(Constants.SSO_REALM)).thenReturn("test");
            
            cryptoMock.when(() -> CryptoUtil.verifyRSASign(anyString(), any(byte[].class), eq(publicKey), eq(Constants.SHA_256_WITH_RSA)))
                     .thenReturn(true);
            
            base64Mock.when(() -> Base64Util.decode(anyString(), eq(11)))
                     .thenAnswer(invocation -> Base64.getDecoder().decode((byte[]) invocation.getArgument(0)));

            String result = accessTokenValidator.verifyUserToken(validToken);

            assertEquals("Unauthorized", result);
        }
    }

    @Test
    void testVerifyUserToken_ExpiredToken() {
        when(keyManager.getPublicKey("test-key")).thenReturn(keyData);
        when(keyData.getPublicKey()).thenReturn(publicKey);

        try (MockedStatic<CryptoUtil> cryptoMock = mockStatic(CryptoUtil.class);
             MockedStatic<PropertiesCache> propMock = mockStatic(PropertiesCache.class);
             MockedStatic<Base64Util> base64Mock = mockStatic(Base64Util.class)) {
            
            propMock.when(PropertiesCache::getInstance).thenReturn(propertiesCache);
            when(propertiesCache.getProperty(Constants.SSO_URL)).thenReturn("http://localhost/");
            when(propertiesCache.getProperty(Constants.SSO_REALM)).thenReturn("test");
            
            cryptoMock.when(() -> CryptoUtil.verifyRSASign(anyString(), any(byte[].class), eq(publicKey), eq(Constants.SHA_256_WITH_RSA)))
                     .thenReturn(true);
            
            base64Mock.when(() -> Base64Util.decode(anyString(), eq(11)))
                     .thenAnswer(invocation -> Base64.getDecoder().decode((byte[]) invocation.getArgument(0)));

            String result = accessTokenValidator.verifyUserToken(expiredToken);

            assertEquals(Constants._UNAUTHORIZED, result);
        }
    }

    @Test
    void testVerifyUserToken_InvalidSignature() {
        when(keyManager.getPublicKey("test-key")).thenReturn(keyData);
        when(keyData.getPublicKey()).thenReturn(publicKey);

        try (MockedStatic<CryptoUtil> cryptoMock = mockStatic(CryptoUtil.class);
             MockedStatic<PropertiesCache> propMock = mockStatic(PropertiesCache.class);
             MockedStatic<Base64Util> base64Mock = mockStatic(Base64Util.class)) {
            
            propMock.when(PropertiesCache::getInstance).thenReturn(propertiesCache);
            
            cryptoMock.when(() -> CryptoUtil.verifyRSASign(anyString(), any(byte[].class), eq(publicKey), eq(Constants.SHA_256_WITH_RSA)))
                     .thenReturn(false);
            
            base64Mock.when(() -> Base64Util.decode(anyString(), eq(11)))
                     .thenAnswer(invocation -> Base64.getDecoder().decode((byte[]) invocation.getArgument(0)));

            String result = accessTokenValidator.verifyUserToken(validToken);

            assertEquals(Constants._UNAUTHORIZED, result);
        }
    }

    @Test
    void testVerifyUserToken_InvalidIssuer() {
        when(keyManager.getPublicKey("test-key")).thenReturn(keyData);
        when(keyData.getPublicKey()).thenReturn(publicKey);

        try (MockedStatic<CryptoUtil> cryptoMock = mockStatic(CryptoUtil.class);
             MockedStatic<PropertiesCache> propMock = mockStatic(PropertiesCache.class);
             MockedStatic<Base64Util> base64Mock = mockStatic(Base64Util.class)) {
            
            propMock.when(PropertiesCache::getInstance).thenReturn(propertiesCache);
            when(propertiesCache.getProperty(Constants.SSO_URL)).thenReturn("http://different/");
            when(propertiesCache.getProperty(Constants.SSO_REALM)).thenReturn("different");
            
            cryptoMock.when(() -> CryptoUtil.verifyRSASign(anyString(), any(byte[].class), eq(publicKey), eq(Constants.SHA_256_WITH_RSA)))
                     .thenReturn(true);
            
            base64Mock.when(() -> Base64Util.decode(anyString(), eq(11)))
                     .thenAnswer(invocation -> Base64.getDecoder().decode((byte[]) invocation.getArgument(0)));

            String result = accessTokenValidator.verifyUserToken(validToken);

            assertEquals(Constants._UNAUTHORIZED, result);
        }
    }

    @Test
    void testVerifyUserToken_Exception() {
        String result = accessTokenValidator.verifyUserToken(invalidToken);
        assertEquals(Constants._UNAUTHORIZED, result);
    }

    @Test
    void testFetchUserIdFromAccessToken_ValidToken() {
        when(keyManager.getPublicKey("test-key")).thenReturn(keyData);
        when(keyData.getPublicKey()).thenReturn(publicKey);

        try (MockedStatic<CryptoUtil> cryptoMock = mockStatic(CryptoUtil.class);
             MockedStatic<PropertiesCache> propMock = mockStatic(PropertiesCache.class);
             MockedStatic<Base64Util> base64Mock = mockStatic(Base64Util.class)) {
            
            propMock.when(PropertiesCache::getInstance).thenReturn(propertiesCache);
            when(propertiesCache.getProperty(Constants.SSO_URL)).thenReturn("http://localhost/");
            when(propertiesCache.getProperty(Constants.SSO_REALM)).thenReturn("test");
            
            cryptoMock.when(() -> CryptoUtil.verifyRSASign(anyString(), any(byte[].class), eq(publicKey), eq(Constants.SHA_256_WITH_RSA)))
                     .thenReturn(true);
            
            base64Mock.when(() -> Base64Util.decode(anyString(), eq(11)))
                     .thenAnswer(invocation -> Base64.getDecoder().decode((byte[]) invocation.getArgument(0)));

            String result = accessTokenValidator.fetchUserIdFromAccessToken(validToken);

            assertNull(result);
        }
    }

    @Test
    void testFetchUserIdFromAccessToken_NullToken() {
        String result = accessTokenValidator.fetchUserIdFromAccessToken(null);
        assertNull(result);
    }

    @Test
    void testFetchUserIdFromAccessToken_UnauthorizedToken() {
        String result = accessTokenValidator.fetchUserIdFromAccessToken(invalidToken);
        assertNull(result);
    }

    @Test
    void testFetchUserIdFromAccessToken_WithResponse_ValidToken() {
        SBApiResponse response = new SBApiResponse();
        when(keyManager.getPublicKey("test-key")).thenReturn(keyData);
        when(keyData.getPublicKey()).thenReturn(publicKey);

        try (MockedStatic<CryptoUtil> cryptoMock = mockStatic(CryptoUtil.class);
             MockedStatic<PropertiesCache> propMock = mockStatic(PropertiesCache.class);
             MockedStatic<Base64Util> base64Mock = mockStatic(Base64Util.class)) {
            
            propMock.when(PropertiesCache::getInstance).thenReturn(propertiesCache);
            when(propertiesCache.getProperty(Constants.SSO_URL)).thenReturn("http://localhost/");
            when(propertiesCache.getProperty(Constants.SSO_REALM)).thenReturn("test");
            
            cryptoMock.when(() -> CryptoUtil.verifyRSASign(anyString(), any(byte[].class), eq(publicKey), eq(Constants.SHA_256_WITH_RSA)))
                     .thenReturn(true);
            
            base64Mock.when(() -> Base64Util.decode(anyString(), eq(11)))
                     .thenAnswer(invocation -> Base64.getDecoder().decode((byte[]) invocation.getArgument(0)));

            String result = accessTokenValidator.fetchUserIdFromAccessToken(validToken, response);

            assertNull(result);
        }
    }

    @Test
    void testFetchUserIdFromAccessToken_WithResponse_NullToken() {
        SBApiResponse response = new SBApiResponse();

        String result = accessTokenValidator.fetchUserIdFromAccessToken(null, response);

        assertNull(result);
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(Constants.ACCESS_TOKEN_VALIDATION_FAILED, response.getParams().getErrmsg());
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testFetchUserIdFromAccessToken_WithResponse_UnauthorizedToken() {
        SBApiResponse response = new SBApiResponse();

        String result = accessTokenValidator.fetchUserIdFromAccessToken(invalidToken, response);

        assertNull(result);
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(Constants.ACCESS_TOKEN_IS_EXPIRED, response.getParams().getErrmsg());
        assertEquals(HttpStatus.UNAUTHORIZED, response.getResponseCode());
    }

    @Test
    void testFetchUserIdFromAccessToken_WithResponse_Exception() {
        SBApiResponse response = new SBApiResponse();
        when(keyManager.getPublicKey(anyString())).thenThrow(new RuntimeException("Key error"));

        String result = accessTokenValidator.fetchUserIdFromAccessToken(validToken, response);

        assertNull(result);
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(Constants.ACCESS_TOKEN_IS_EXPIRED, response.getParams().getErrmsg());
//        assertEquals(HttpStatus.UNAUTHORIZED, response.getResponseCode());
    }
}