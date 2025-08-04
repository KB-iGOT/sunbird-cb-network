package org.sunbird.cb.hubservices.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.sunbird.cb.hubservices.util.NetworkServerProperties;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

class RedisConfigTest {

    @InjectMocks
    private RedisConfig redisConfig;

    @Mock
    private NetworkServerProperties properties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testJedisPool() {
        when(properties.getRedisHostName()).thenReturn("localhost");
        when(properties.getRedisPort()).thenReturn("6379");

        JedisPool jedisPool = redisConfig.jedisPool();

        assertNotNull(jedisPool);
        verify(properties).getRedisHostName();
        verify(properties).getRedisPort();
    }

    @Test
    void testBuildPoolConfig() {
        JedisPoolConfig poolConfig = ReflectionTestUtils.invokeMethod(redisConfig, "buildPoolConfig");

        assertNotNull(poolConfig);
        assertEquals(128, poolConfig.getMaxIdle());
        assertEquals(3000, poolConfig.getMaxTotal());
        assertEquals(100, poolConfig.getMinIdle());
        assertTrue(poolConfig.getTestOnBorrow());
        assertTrue(poolConfig.getTestOnReturn());
        assertTrue(poolConfig.getTestWhileIdle());
        assertEquals(120000, poolConfig.getMinEvictableIdleTimeMillis());
        assertEquals(30000, poolConfig.getTimeBetweenEvictionRunsMillis());
        assertEquals(3, poolConfig.getNumTestsPerEvictionRun());
        assertTrue(poolConfig.getBlockWhenExhausted());
    }
}