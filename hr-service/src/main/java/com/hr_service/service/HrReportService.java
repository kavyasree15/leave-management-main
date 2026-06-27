package com.hr_service.service;

import com.hr_service.client.AttendanceClient;
import com.hr_service.client.AuthClient;
import com.hr_service.client.LeaveClient;
import com.hr_service.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class HrReportService {

    @Autowired
    private AuthClient authClient;

    @Autowired
    private AttendanceClient attendanceClient;

    @Autowired
    private LeaveClient leaveClient;

    @CircuitBreaker(name = "hrBackend", fallbackMethod = "fallbackAttendanceReport")
    @Retry(name = "hrBackend")
    public List<AttendanceReport> getAttendanceReport(Long userId, LocalDate start, LocalDate end) {
        List<UserDto> users = new ArrayList<>();
        if (userId != null) {
            users.add(authClient.getUserById(userId));
        } else {
            users.addAll(authClient.getAllUsers());
        }

        String startStr = start.toString();
        String endStr = end.toString();

        return users.stream().map(user -> {
            List<AttendanceDto> attendanceList = attendanceClient.getUserAttendanceBetween(user.getId(), startStr, endStr);
            int totalDays = attendanceList.size();
            int lateCount = (int) attendanceList.stream().filter(AttendanceDto::isLate).count();
            double totalHours = attendanceList.stream()
                    .mapToDouble(a -> a.getWorkingHours() != null ? a.getWorkingHours() : 0.0)
                    .sum();
            double avgHours = totalDays > 0 ? (totalHours / totalDays) : 0.0;
            // Round to 2 decimal places
            avgHours = Math.round(avgHours * 100.0) / 100.0;
            totalHours = Math.round(totalHours * 100.0) / 100.0;

            return new AttendanceReport(user.getId(), user.getUsername(), totalDays, lateCount, totalHours, avgHours);
        }).collect(Collectors.toList());
    }

    @CircuitBreaker(name = "hrBackend", fallbackMethod = "fallbackLeaveReport")
    @Retry(name = "hrBackend")
    public List<LeaveUtilizationReport> getLeaveUtilizationReport(Long userId) {
        List<UserDto> users = new ArrayList<>();
        if (userId != null) {
            users.add(authClient.getUserById(userId));
        } else {
            users.addAll(authClient.getAllUsers());
        }

        List<LeaveRequestDto> allLeaves = leaveClient.getAllLeaves();

        return users.stream().map(user -> {
            LeaveBalanceDto balance = leaveClient.getUserBalance(user.getId());
            List<LeaveRequestDto> userApprovedLeaves = allLeaves.stream()
                    .filter(l -> l.getUserId().equals(user.getId()) && "APPROVED".equalsIgnoreCase(l.getStatus()))
                    .collect(Collectors.toList());

            int casualTaken = 0;
            int medicalTaken = 0;
            int paidTaken = 0;
            int unpaidTaken = 0;

            for (LeaveRequestDto leave : userApprovedLeaves) {
                long days = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
                switch (leave.getLeaveType().toUpperCase()) {
                    case "CASUAL" -> casualTaken += (int) days;
                    case "MEDICAL" -> medicalTaken += (int) days;
                    case "PAID" -> paidTaken += (int) days;
                    case "UNPAID" -> unpaidTaken += (int) days;
                }
            }

            return new LeaveUtilizationReport(
                    user.getId(),
                    user.getUsername(),
                    casualTaken,
                    balance.getCasualLeave(),
                    medicalTaken,
                    balance.getMedicalLeave(),
                    paidTaken,
                    balance.getPaidLeave(),
                    unpaidTaken
            );
        }).collect(Collectors.toList());
    }

    public List<AttendanceReport> fallbackAttendanceReport(Long userId, LocalDate start, LocalDate end, Throwable t) {
        List<AttendanceReport> fallbackList = new ArrayList<>();
        fallbackList.add(new AttendanceReport(userId != null ? userId : 0L, "DOWNSTREAM SERVICE UNAVAILABLE", 0, 0, 0.0, 0.0));
        return fallbackList;
    }

    public List<LeaveUtilizationReport> fallbackLeaveReport(Long userId, Throwable t) {
        List<LeaveUtilizationReport> fallbackList = new ArrayList<>();
        fallbackList.add(new LeaveUtilizationReport(userId != null ? userId : 0L, "DOWNSTREAM SERVICE UNAVAILABLE", 0, 0, 0, 0, 0, 0, 0));
        return fallbackList;
    }
}
