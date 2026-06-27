package com.hr_service.dto;

public class LeaveBalanceDto {
    private Long id;
    private Long userId;
    private Integer casualLeave;
    private Integer medicalLeave;
    private Integer paidLeave;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getCasualLeave() { return casualLeave; }
    public void setCasualLeave(Integer casualLeave) { this.casualLeave = casualLeave; }

    public Integer getMedicalLeave() { return medicalLeave; }
    public void setMedicalLeave(Integer medicalLeave) { this.medicalLeave = medicalLeave; }

    public Integer getPaidLeave() { return paidLeave; }
    public void setPaidLeave(Integer paidLeave) { this.paidLeave = paidLeave; }
}
