package com.auth_service.dto;

import com.auth_service.model.Role;

public class UserResponse {
    private Long id;
    private String username;
    private Role role;
    private Long managerId;
    private String email;
    private boolean approved;

    public UserResponse() {}

    public UserResponse(Long id, String username, Role role, Long managerId, String email, boolean approved) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.managerId = managerId;
        this.email = email;
        this.approved = approved;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
}
