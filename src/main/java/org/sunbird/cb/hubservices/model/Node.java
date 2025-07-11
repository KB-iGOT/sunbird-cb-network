package org.sunbird.cb.hubservices.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Node {

	private String userId;
	private String createdAt;
	private String updatedAt;
	private String fullName;
	private String departmentName;
	private String status;
	private List<Map<String, Object>> professionalDetails;
	private Map<String, Object> employmentDetails;
	private String profileImageUrl;
	private String profileBannerUrl;
	private String designation;
	private String organisationId;
	private List<String> roles;

	public Node(String userId) {
		this.userId = userId;
	}

	public Node(String userId,String createdAt,String updatedAt,String status){
		this.userId = userId;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.status = status;
	}
}
