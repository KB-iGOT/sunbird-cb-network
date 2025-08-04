package org.sunbird.cb.hubservices.profile.handler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConsumerConfigurationTest {

    @InjectMocks
    private ConsumerConfiguration consumerConfiguration;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(consumerConfiguration, "kafkabootstrapAddress", "localhost:9092");
        ReflectionTestUtils.setField(consumerConfiguration, "kafkaOffsetResetValue", "earliest");
        ReflectionTestUtils.setField(consumerConfiguration, "kafkaMaxPollInterval", 300000);
        ReflectionTestUtils.setField(consumerConfiguration, "kafkaMaxPollRecords", 500);
        ReflectionTestUtils.setField(consumerConfiguration, "kafkaAutoCommitInterval", 1000);
    }

    @Test
    void testKafkaListenerContainerFactory() {
        KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> factory = 
            consumerConfiguration.kafkaListenerContainerFactory();
        
        assertNotNull(factory);
        assertTrue(factory instanceof ConcurrentKafkaListenerContainerFactory);
        
        ConcurrentKafkaListenerContainerFactory<String, String> concurrentFactory = 
            (ConcurrentKafkaListenerContainerFactory<String, String>) factory;
        assertEquals(3000, concurrentFactory.getContainerProperties().getPollTimeout());
    }

    @Test
    void testConsumerFactory() {
        ConsumerFactory<String, String> factory = consumerConfiguration.consumerFactory();
        
        assertNotNull(factory);
        assertTrue(factory instanceof DefaultKafkaConsumerFactory);
    }

    @Test
    void testConsumerConfigs() {
        Map<String, Object> configs = consumerConfiguration.consumerConfigs();
        
        assertNotNull(configs);
        assertEquals("localhost:9092", configs.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals(true, configs.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
        assertEquals("1000", configs.get(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG));
        assertEquals(1000, configs.get(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG));
        assertEquals("15000", configs.get(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG));
        assertEquals(StringDeserializer.class, configs.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        assertEquals(StringDeserializer.class, configs.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
        assertEquals("earliest", configs.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
        assertEquals(300000, configs.get(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG));
        assertEquals(500, configs.get(ConsumerConfig.MAX_POLL_RECORDS_CONFIG));
    }
}