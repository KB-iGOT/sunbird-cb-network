package org.sunbird.cb.hubservices.profile.handler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.collections4.MapUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.sunbird.cb.hubservices.serviceimpl.ProfileService;
import org.sunbird.cb.hubservices.util.Constants;

@Component
@Slf4j
public class ProfileUpdateConsumer {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ProfileService profileService;

    @KafkaListener(topics = "${kafka.topic.name.user.profile.update}", groupId = "${kafka.group.name.user.profile.update}")
    public void userProfileUpdateConsumer(ConsumerRecord<String, String> data) throws IOException {
        try {
            log.info("ProfileUpdateConsumer::userProfileUpdated:topic name: {} and recievedData: {}", data.topic(),
                    data.value());
            if (StringUtils.hasText(data.value())) {
                Map<String, Object> userData = mapper.readValue(data.value(), new TypeReference<Map<String, Object>>() {
                });
                if (MapUtils.isNotEmpty(userData)) {
                    // Fetch user data from DB and update cache
                    CompletableFuture.runAsync(() -> profileService.onboardNetworkHubUser((String) userData.get(Constants.USER_ID)));
                    // Get Group and Designation value from the user object and update the same
                    // in the Neo4J
                } else {
                    log.error("Error in userProfileUpdated: Invalid userData in Kafka Msg");
                }
            } else {
                log.error("Error in userProfileUpdated: Invalid Kafka Msg");
            }
        } catch (Exception e) {
            log.error("Error while processing the kafka event {} : ", data, e);
        }
    }
}
