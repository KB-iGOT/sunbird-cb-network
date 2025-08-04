package org.sunbird.cb.hubservices.common.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.*;

import org.junit.jupiter.api.Test;

class CryptoUtilTest {

    @Test
    void testVerifyRSASign_ValidSignature() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        
        String payload = "test payload";
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(payload.getBytes("US-ASCII"));
        byte[] signatureBytes = signature.sign();

        boolean result = CryptoUtil.verifyRSASign(payload, signatureBytes, keyPair.getPublic(), "SHA256withRSA");

        assertTrue(result);
    }

    @Test
    void testVerifyRSASign_InvalidSignature() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        
        String payload = "test payload";
        byte[] invalidSignature = "invalid".getBytes();

        boolean result = CryptoUtil.verifyRSASign(payload, invalidSignature, keyPair.getPublic(), "SHA256withRSA");

        assertFalse(result);
    }

    @Test
    void testVerifyRSASign_NoSuchAlgorithmException() {
        PublicKey mockKey = mock(PublicKey.class);
        
        boolean result = CryptoUtil.verifyRSASign("payload", new byte[0], mockKey, "InvalidAlgorithm");

        assertFalse(result);
    }

    @Test
    void testVerifyRSASign_InvalidKeyException() {
        PublicKey invalidKey = mock(PublicKey.class);
        when(invalidKey.getAlgorithm()).thenReturn("InvalidAlgorithm");
        
        boolean result = CryptoUtil.verifyRSASign("payload", new byte[0], invalidKey, "SHA256withRSA");

        assertFalse(result);
    }

    @Test
    void testVerifyRSASign_SignatureException() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        
        byte[] malformedSignature = new byte[1]; // Too short for RSA signature

        boolean result = CryptoUtil.verifyRSASign("payload", malformedSignature, keyPair.getPublic(), "SHA256withRSA");

        assertFalse(result);
    }
}