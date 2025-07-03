package org.sunbird.cb.hubservices.util;

import lombok.Getter;
import lombok.Setter;
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

}
