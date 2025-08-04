package org.sunbird.cb.hubservices.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SunbirdApiRespParamTest {

    @Test
    void testDefaultConstructor() {
        SunbirdApiRespParam param = new SunbirdApiRespParam();
        assertNotNull(param);
    }

    @Test
    void testParameterizedConstructor() {
        SunbirdApiRespParam param = new SunbirdApiRespParam("test123");
        
        assertEquals("test123", param.getResmsgid());
        assertEquals("test123", param.getMsgid());
    }

    @Test
    void testGettersAndSetters() {
        SunbirdApiRespParam param = new SunbirdApiRespParam();
        
        param.setResmsgid("resmsg123");
        param.setMsgid("msg123");
        param.setErr("error123");
        param.setStatus("SUCCESS");
        param.setErrmsg("Error message");
        
        assertEquals("resmsg123", param.getResmsgid());
        assertEquals("msg123", param.getMsgid());
        assertEquals("error123", param.getErr());
        assertEquals("SUCCESS", param.getStatus());
        assertEquals("Error message", param.getErrmsg());
    }
}