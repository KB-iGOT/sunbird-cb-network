package org.sunbird.cb.hubservices.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

class HubConfigurationTest {

    private HubConfiguration hubConfiguration;

    @BeforeEach
    void setUp() {
        hubConfiguration = new HubConfiguration();
        ReflectionTestUtils.setField(hubConfiguration, "connectionThreadName", "test-thread-");
        ReflectionTestUtils.setField(hubConfiguration, "connectionCorePoolSize", 5);
        ReflectionTestUtils.setField(hubConfiguration, "connectionMaxPoolSize", 10);
        ReflectionTestUtils.setField(hubConfiguration, "connectionQueueCapacity", 25);
    }

    @Test
    void testTaskExecutor() {
        TaskExecutor taskExecutor = hubConfiguration.taskExecutor();

        assertNotNull(taskExecutor);
        assertTrue(taskExecutor instanceof ThreadPoolTaskExecutor);
        
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) taskExecutor;
        assertEquals(5, executor.getCorePoolSize());
        assertEquals(10, executor.getMaxPoolSize());
        assertEquals("test-thread-", executor.getThreadNamePrefix());
    }
}