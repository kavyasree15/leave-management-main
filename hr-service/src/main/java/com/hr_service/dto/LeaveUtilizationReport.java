package com.hr_service.dto;

public class LeaveUtilizationReport {
    private Long userId;
    private String username;
    
    private int casualTaken;
    private int casualRemaining;
    
    private int medicalTaken;
    private int medicalRemaining;
    
    private int paidTaken;
    private int paidRemaining;
    
    private int unpaidTaken;

    public LeaveUtilizationReport(Long userId, String username, int casualTaken, int casualRemaining, int medicalTaken, int medicalRemaining, int paidTaken, int paidRemaining, int unpaidTaken) {
        this.userId = userId;
        this.username = username;
        this.casualTaken = casualTaken;
        this.casualRemaining = casualRemaining;
        this.medicalTaken = medicalTaken;
        this.medicalRemaining = medicalRemaining;
        this.paidTaken = paidTaken;
        this.paidRemaining = paidRemaining;
        this.unpaidTaken = unpaidTaken;
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getCasualTaken() { return casualTaken; }
    public void setCasualTaken(int casualTaken) { this.casualTaken = casualTaken; }

    public int getCasualRemaining() { return casualRemaining; }
    public void setCasualRemaining(int casualRemaining) { this.casualRemaining = casualRemaining; }

    public int getMedicalTaken() { return medicalTaken; }
    public void setMedicalTaken(int medicalTaken) { this.medicalTaken = medicalTaken; }

    public int getMedicalRemaining() { return medicalRemaining; }
    public void setMedicalRemaining(int medicalRemaining) { this.medicalRemaining = medicalRemaining; }

    public int getPaidTaken() { return paidTaken; }
    public void setPaidTaken(int paidTaken) { this.paidTaken = paidTaken; }

    public int getPaidRemaining() { return paidRemaining; }
    public void setPaidRemaining(int paidRemaining) { this.paidRemaining = paidRemaining; }

    public int getUnpaidTaken() { return unpaidTaken; }
    public void setUnpaidTaken(int unpaidTaken) { this.unpaidTaken = unpaidTaken; }
}
