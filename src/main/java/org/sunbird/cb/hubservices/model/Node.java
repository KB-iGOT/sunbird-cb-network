package org.sunbird.cb.hubservices.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Node {

	private String id;
	private String createdAt;
	private String updatedAt;
	private String fullName;
	private String departmentName;
	private String status;
	private List<Map<String, Object>> professionalDetails;
	private Map<String, Object> employmentDetails;
	private String profileImageUrl;
	private String profileBannerUrl;
	public Node(String id) {
		this.id = id;
	}

	public Node(String id,String createdAt,String updatedAt,String status){
		this.id = id;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.status = status;
	}
}
