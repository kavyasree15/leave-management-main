package com.leave_service.model;

import jakarta.persistence.*;

@Entity
@Table(name = "leave_balances")
public class LeaveBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    private Integer casualLeave = 12;
    private Integer medicalLeave = 15;
    private Integer paidLeave = 18;

    // Constructors
    public LeaveBalance() {}

    public LeaveBalance(Long userId) {
        this.userId = userId;
    }

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
