package com.hr_service.dto;

public class AttendanceReport {
    private Long userId;
    private String username;
    private int totalDays;
    private int lateCount;
    private double totalHours;
    private double avgHours;

    public AttendanceReport(Long userId, String username, int totalDays, int lateCount, double totalHours, double avgHours) {
        this.userId = userId;
        this.username = username;
        this.totalDays = totalDays;
        this.lateCount = lateCount;
        this.totalHours = totalHours;
        this.avgHours = avgHours;
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }

    public int getLateCount() { return lateCount; }
    public void setLateCount(int lateCount) { this.lateCount = lateCount; }

    public double getTotalHours() { return totalHours; }
    public void setTotalHours(double totalHours) { this.totalHours = totalHours; }

    public double getAvgHours() { return avgHours; }
    public void setAvgHours(double avgHours) { this.avgHours = avgHours; }
}
