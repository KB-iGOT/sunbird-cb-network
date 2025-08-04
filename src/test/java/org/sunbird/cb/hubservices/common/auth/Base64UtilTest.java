package org.sunbird.cb.hubservices.common.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class Base64UtilTest {

    private final String testString = "Hello World!";
    private final byte[] testBytes = testString.getBytes();
    private final String base64Standard = "SGVsbG8gV29ybGQh";
    private final String base64UrlSafe = "SGVsbG8gV29ybGQh";

    @Test
    void testDecodeString_Default() {
        byte[] result = Base64Util.decode(base64Standard, Base64Util.DEFAULT);
        assertArrayEquals(testBytes, result);
    }

    @Test
    void testDecodeString_UrlSafe() {
        String urlSafeInput = "SGVsbG8tV29ybGQh"; // with - instead of +
        byte[] result = Base64Util.decode(urlSafeInput, Base64Util.URL_SAFE);
        assertNotNull(result);
    }

    @Test
    void testDecodeByteArray_Default() {
        byte[] result = Base64Util.decode(base64Standard.getBytes(), Base64Util.DEFAULT);
        assertArrayEquals(testBytes, result);
    }

    @Test
    void testDecodeByteArray_WithOffset() {
        String paddedInput = "XX" + base64Standard;
        byte[] result = Base64Util.decode(paddedInput.getBytes(), 2, base64Standard.length(), Base64Util.DEFAULT);
        assertArrayEquals(testBytes, result);
    }

    @Test
    void testDecodeInvalidBase64() {
        assertThrows(IllegalArgumentException.class, () -> {
            Base64Util.decode("Invalid@Base64!", Base64Util.DEFAULT);
        });
    }

    @Test
    void testEncodeToString_Default() {
        String result = Base64Util.encodeToString(testBytes, Base64Util.DEFAULT);
        assertTrue(result.contains("SGVsbG8gV29ybGQh"));
    }

    @Test
    void testEncodeToString_NoPadding() {
        String result = Base64Util.encodeToString(testBytes, Base64Util.NO_PADDING);
        assertFalse(result.endsWith("="));
    }

    @Test
    void testEncodeToString_NoWrap() {
        byte[] longData = new byte[100];
        for (int i = 0; i < longData.length; i++) {
            longData[i] = (byte) i;
        }
        String result = Base64Util.encodeToString(longData, Base64Util.NO_WRAP);
        assertFalse(result.contains("\n"));
    }

    @Test
    void testEncodeToString_WithCRLF() {
        byte[] longData = new byte[100];
        String result = Base64Util.encodeToString(longData, Base64Util.CRLF);
        assertTrue(result.contains("\r\n") || !result.contains("\n"));
    }

    @Test
    void testEncodeToString_UrlSafe() {
        byte[] data = {-1, -2, -3}; // bytes that would produce + and / in standard encoding
        String result = Base64Util.encodeToString(data, Base64Util.URL_SAFE);
        assertFalse(result.contains("+"));
        assertFalse(result.contains("/"));
    }

    @Test
    void testEncodeToString_WithOffset() {
        byte[] paddedData = new byte[testBytes.length + 4];
        System.arraycopy(testBytes, 0, paddedData, 2, testBytes.length);
        String result = Base64Util.encodeToString(paddedData, 2, testBytes.length, Base64Util.DEFAULT);
        assertTrue(result.contains("SGVsbG8gV29ybGQh"));
    }

    @Test
    void testEncode_Default() {
        byte[] result = Base64Util.encode(testBytes, Base64Util.DEFAULT);
        String resultString = new String(result);
        assertTrue(resultString.contains("SGVsbG8gV29ybGQh"));
    }

    @Test
    void testEncode_WithOffset() {
        byte[] paddedData = new byte[testBytes.length + 4];
        System.arraycopy(testBytes, 0, paddedData, 2, testBytes.length);
        byte[] result = Base64Util.encode(paddedData, 2, testBytes.length, Base64Util.DEFAULT);
        String resultString = new String(result);
        assertTrue(resultString.contains("SGVsbG8gV29ybGQh"));
    }

    @Test
    void testEncode_NoPadding_Length1() {
        byte[] singleByte = {65}; // 'A'
        byte[] result = Base64Util.encode(singleByte, Base64Util.NO_PADDING);
        String resultString = new String(result);
        assertFalse(resultString.contains("="));
    }

    @Test
    void testEncode_NoPadding_Length2() {
        byte[] twoBytes = {65, 66}; // 'AB'
        byte[] result = Base64Util.encode(twoBytes, Base64Util.NO_PADDING);
        String resultString = new String(result);
        assertFalse(resultString.contains("="));
    }

    @Test
    void testEncode_WithNewlines() {
        byte[] longData = new byte[200];
        for (int i = 0; i < longData.length; i++) {
            longData[i] = (byte) (i % 256);
        }
        byte[] result = Base64Util.encode(longData, Base64Util.DEFAULT);
        String resultString = new String(result);
        assertTrue(resultString.contains("\n"));
    }

    @Test
    void testRoundTrip_Default() {
        byte[] encoded = Base64Util.encode(testBytes, Base64Util.DEFAULT);
        byte[] decoded = Base64Util.decode(encoded, Base64Util.DEFAULT);
        assertArrayEquals(testBytes, decoded);
    }

    @Test
    void testRoundTrip_UrlSafe() {
        byte[] data = {-1, -2, -3, 65, 66, 67};
        byte[] encoded = Base64Util.encode(data, Base64Util.URL_SAFE);
        byte[] decoded = Base64Util.decode(encoded, Base64Util.URL_SAFE);
        assertArrayEquals(data, decoded);
    }

    @Test
    void testRoundTrip_NoPadding() {
        byte[] encoded = Base64Util.encode(testBytes, Base64Util.NO_PADDING);
        byte[] decoded = Base64Util.decode(encoded, Base64Util.NO_PADDING);
        assertArrayEquals(testBytes, decoded);
    }

    @Test
    void testDecodeWithPadding() {
        String withPadding = "SGVsbG8="; // "Hello" with padding
        byte[] result = Base64Util.decode(withPadding, Base64Util.DEFAULT);
        assertEquals("Hello", new String(result));
    }

    @Test
    void testDecodeWithDoublePadding() {
        String withDoublePadding = "SGVsbA=="; // "Hell" with double padding
        byte[] result = Base64Util.decode(withDoublePadding, Base64Util.DEFAULT);
        assertEquals("Hell", new String(result));
    }

    @Test
    void testEmptyInput() {
        byte[] result = Base64Util.decode("", Base64Util.DEFAULT);
        assertEquals(0, result.length);
        
        byte[] encoded = Base64Util.encode(new byte[0], Base64Util.DEFAULT);
        assertEquals(0, encoded.length);
    }

    @Test
    void testDecoderMaxOutputSize() {
        Base64Util.Decoder decoder = new Base64Util.Decoder(Base64Util.DEFAULT, new byte[100]);
        int maxSize = decoder.maxOutputSize(12);
        assertTrue(maxSize > 0);
    }

    @Test
    void testEncoderMaxOutputSize() {
        Base64Util.Encoder encoder = new Base64Util.Encoder(Base64Util.DEFAULT, null);
        int maxSize = encoder.maxOutputSize(12);
        assertTrue(maxSize > 0);
    }
}