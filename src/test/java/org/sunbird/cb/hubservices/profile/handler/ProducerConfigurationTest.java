package org.sunbird.cb.hubservices.profile.handler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import org.apache.kafka.common.serialization.StringSerializer;

@ExtendWith(MockitoExtension.class)
class ProducerConfigurationTest {

    @InjectMocks
    private ProducerConfiguration producerConfiguration;

    private static final String BOOTSTRAP_ADDRESS = "localhost:9092";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producerConfiguration, "bootstrapAddress", BOOTSTRAP_ADDRESS);
    }

    @Test
    void testProducerFactory() {
        ProducerFactory<String, String> factory = producerConfiguration.producerFactory();

        assertNotNull(factory);
        assertTrue(factory instanceof DefaultKafkaProducerFactory);

        Map<String, Object> configs = ((DefaultKafkaProducerFactory<String, String>) factory).getConfigurationProperties();
        assertEquals(BOOTSTRAP_ADDRESS, configs.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
    }

    @Test
    void testKafkaTemplate() {
        KafkaTemplate<String, String> template = producerConfiguration.kafkaTemplate();

        assertNotNull(template);
        assertNotNull(template.getProducerFactory());
    }

    @Test
    void testUserProducerFactory() {
        ProducerFactory<String, Object> factory = producerConfiguration.userProducerFactory();

        assertNotNull(factory);
        assertTrue(factory instanceof DefaultKafkaProducerFactory);

        Map<String, Object> configs = ((DefaultKafkaProducerFactory<String, Object>) factory).getConfigurationProperties();
        assertEquals(BOOTSTRAP_ADDRESS, configs.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals(JsonSerializer.class, configs.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
    }

    @Test
    void testCustomKafkaTemplate() {
        KafkaTemplate<String, Object> template = producerConfiguration.customKafkaTemplate();

        assertNotNull(template);
        assertNotNull(template.getProducerFactory());
    }
}