package com.auth_service.dto;

import com.auth_service.model.Role;

public class UserResponse {
    private Long id;
    private String username;
    private Role role;
    private Long managerId;
    private String email;
    private boolean approved;
    private String kycStatus;
    private Long hrId;
    private String panNumber;
    private String aadhaarNumber;
    private String panCardUrl;
    private String aadhaarCardUrl;
    private String passportUrl;
    private String drivingLicenseUrl;
    private String mobileNumber;
    private String dob;
    private String gender;
    private String address;
    private String rejectionReason;

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

    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }

    public Long getHrId() { return hrId; }
    public void setHrId(Long hrId) { this.hrId = hrId; }

    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }

    public String getAadhaarNumber() { return aadhaarNumber; }
    public void setAadhaarNumber(String aadhaarNumber) { this.aadhaarNumber = aadhaarNumber; }

    public String getPanCardUrl() { return panCardUrl; }
    public void setPanCardUrl(String panCardUrl) { this.panCardUrl = panCardUrl; }

    public String getAadhaarCardUrl() { return aadhaarCardUrl; }
    public void setAadhaarCardUrl(String aadhaarCardUrl) { this.aadhaarCardUrl = aadhaarCardUrl; }

    public String getPassportUrl() { return passportUrl; }
    public void setPassportUrl(String passportUrl) { this.passportUrl = passportUrl; }

    public String getDrivingLicenseUrl() { return drivingLicenseUrl; }
    public void setDrivingLicenseUrl(String drivingLicenseUrl) { this.drivingLicenseUrl = drivingLicenseUrl; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
