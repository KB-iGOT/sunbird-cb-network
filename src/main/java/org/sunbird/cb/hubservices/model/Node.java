package org.sunbird.cb.hubservices.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Node {

	private String id;
	private String createdAt;
	private String updatedAt;
	private String fullName;
	private String departmentName;
	private String status;

	public Node(String id) {
		this.id = id;
	}

	public Node(String id,String createdAt,String updatedAt,String status){
		this.id = id;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.status = status;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	//@JsonIgnore
	public String getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(String updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getDepartmentName() {
		return departmentName;
	}

	public void setDepartmentName(String departmentName) {
		this.departmentName = departmentName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
