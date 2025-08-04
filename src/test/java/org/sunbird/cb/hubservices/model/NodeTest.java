package org.sunbird.cb.hubservices.model;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {

    @Test
    void testNoArgsConstructor() {
        Node node = new Node();
        assertNotNull(node);
    }

    @Test
    void testSingleParamConstructor() {
        Node node = new Node("user123");
        assertEquals("user123", node.getUserId());
    }

    @Test
    void testFourParamConstructor() {
        Node node = new Node("user123", "2023-01-01", "2023-01-02", "ACTIVE");
        
        assertEquals("user123", node.getUserId());
        assertEquals("2023-01-01", node.getCreatedAt());
        assertEquals("2023-01-02", node.getUpdatedAt());
        assertEquals("ACTIVE", node.getStatus());
    }

    @Test
    void testFiveParamConstructor() {
        List<String> roles = Arrays.asList("ADMIN", "USER");
        Node node = new Node("Manager", "user123", roles, "org456", "2023-01-02");
        
        assertEquals("user123", node.getUserId());
        assertEquals("Manager", node.getDesignation());
        assertEquals(roles, node.getRoles());
        assertEquals("org456", node.getOrganisationId());
        assertEquals("2023-01-02", node.getUpdatedAt());
    }

    @Test
    void testGettersAndSetters() {
        Node node = new Node();
        List<Map<String, Object>> professionalDetails = new ArrayList<>();
        Map<String, Object> employmentDetails = new HashMap<>();
        List<String> roles = Arrays.asList("USER");
        
        node.setUserId("user123");
        node.setId("id123");
        node.setCreatedAt("2023-01-01");
        node.setUpdatedAt("2023-01-02");
        node.setFullName("John Doe");
        node.setDepartmentName("IT");
        node.setStatus("ACTIVE");
        node.setProfessionalDetails(professionalDetails);
        node.setEmploymentDetails(employmentDetails);
        node.setProfileImageUrl("http://image.url");
        node.setProfileBannerUrl("http://banner.url");
        node.setDesignation("Developer");
        node.setOrganisationId("org123");
        node.setRoles(roles);
        
        assertEquals("user123", node.getUserId());
        assertEquals("id123", node.getId());
        assertEquals("2023-01-01", node.getCreatedAt());
        assertEquals("2023-01-02", node.getUpdatedAt());
        assertEquals("John Doe", node.getFullName());
        assertEquals("IT", node.getDepartmentName());
        assertEquals("ACTIVE", node.getStatus());
        assertEquals(professionalDetails, node.getProfessionalDetails());
        assertEquals(employmentDetails, node.getEmploymentDetails());
        assertEquals("http://image.url", node.getProfileImageUrl());
        assertEquals("http://banner.url", node.getProfileBannerUrl());
        assertEquals("Developer", node.getDesignation());
        assertEquals("org123", node.getOrganisationId());
        assertEquals(roles, node.getRoles());
    }
}