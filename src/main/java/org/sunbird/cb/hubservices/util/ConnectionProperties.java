package org.sunbird.cb.hubservices.util;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class ConnectionProperties {

	@Value("${max.node.size}")
	private int maxNodeSize;

	@Value("${es.host}")
	private String esHost;

	@Value("${es.port}")
	private String esPort;

	@Value("${es.username}")
	private String esUser;

	@Value("${es.password}")
	private String esPassword;

	@Value("${es.profile.index}")
	private String esProfileIndex;

	@Value("${es.profile.index.type}")
	private String esProfileIndexType;

	@Value("${es.profile.source.fields}")
	private String[] esProfileSourceFields;

	@Value("${notification.service.host}")
	private String notificationIp;

	@Value("${notification.event.endpoint}")
	private String notificationEventEndpoint;

	@Value("${notification.template.targetUrl}")
	private String notificationTemplateTargetUrl;

	@Value("${notification.template.targetUrl.value}")
	private String notificationTemplateTargetUrlValue;

	@Value("${notification.template.sender}")
	private String notificationTemplateSender;

	@Value("${notification.template.reciepient}")
	private String notificationTemplateReciepient;

	@Value("${notification.template.request}")
	private String notificationTemplateRequest;

	@Value("${notification.template.response}")
	private String notificationTemplateResponse;

	@Value("${notification.template.status}")
	private String notificationTemplateStatus;

	@Value("${notification.template.v2.sender}")
	private String notificationv2Sender;

	@Value("${notification.template.v2.id}")
	private String notificationv2Id;

	@Value("${notification.template.v2.delivery.type}")
	private String notificationv2DeliveryType;

	@Value("${notification.template.v2.mode}")
	private String notificationv2Mode;

	@Value("${notification.template.v2.request.body}")
	private String notificationv2RequestBody;

	@Value("${notification.template.v2.response.body}")
	private String notificationv2ResponseBody;

	@Value("${notification.enabled}")
	private boolean notificationEnabled;

	@Value("${sunbird.learner.service.host}")
	private String learnerServiceHost;

	@Value("${sunbird.user.search.endpoint}")
	private String userSearchEndPoint;

	@Value("${sunbird.user.update.endpoint}")
	private String userUpdateEndPoint;

	@Value("${sunbird.user.read.endpoint}")
	private String userReadEndPoint;

	@Value("${user.label.v3}")
	private String userLabelV3;

	@Value("${redis.user.connectionRecieved.timeout}")
	private Integer redisUserConnectionRecievedTimeOut;

	@Value("${redis.user.connectionRequested.timeout}")
	private Integer redisUserConnectionRequestedTimeOut;

	@Value(("${redis.user.connectionEstablished.timeout}"))
	private Integer redisUserConnectionEstablishedTimeOut;

	@Value("${user.read.v5}")
	private String userReadV5;

	@Value("${client.http.request.factory.timeout}")
	private Integer clientHttpRequestFactoryTimeout;

	@Value("${client.http.request.factory.pooling.max.total.connections}")
	private Integer clientHttpRequestFactoryPoolingMaxTotalConnections;

	@Value("${client.http.request.factory.pooling.default.max.per.route}")
	private Integer clientHttpRequestFactoryPoolingDefaultMaxPerRoute;

	@Value("${user.recommendation.cache.limit}")
	private Integer userRecommendationCacheLimit;

	@Value("${user.redis.count.timeout}")
	private Integer redisUserCountTimeOut;

	@Value("${relationship.between.users.query}")
	private String relationshipBetweenUsersQuery;

	@Value("${recommendation.users.designation.query}")
	private String recommendationUsersDesignationQuery;

	@Value("${recommendation.mentors.query}")
	private String recommendationMentorsQuery;

	@Value("${blocked.users.query}")
	private String blockedUsersQuery;

	@Value("${recommended.users.count.query}")
	private String recommendedUsersCountQuery;

	@Value("${recommended.mentors.count.query}")
	private String recommendedMentorsCountQuery;

	@Value("${connections.status.count.query}")
	private String connectionsStatusCountQuery;

	@Value("${connections.outgoing.count.query}")
	private String connectionsOutgoingCountQuery;

	@Value("${connections.incoming.count.query}")
	private String connectionsIncomingCountQuery;

	@Value("${connections.both.count.query}")
	private String connectionsBothCountQuery;

	@Value("${basic.profile.fields}")
    private String basicProfileFields;

    @Value("${http.pooling.client.cm.max.total.connections}")
    private int maxTotalConnections;

    @Value("${http.pooling.client.cm.default.max.per.route}")
    private int maxConnectionsPerRoute;

	public List<String> getBasicProfileFields() {
        return Arrays.asList(basicProfileFields.split(","));
    }
}
