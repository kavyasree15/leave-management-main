package com.hr_service.controller;

import com.hr_service.dto.AttendanceReport;
import com.hr_service.dto.LeaveUtilizationReport;
import com.hr_service.service.HrReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hr")
public class HrController {

    @Autowired
    private HrReportService hrReportService;

    @GetMapping("/reports/attendance")
    public ResponseEntity<?> getAttendanceReport(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        
        if (!"HR".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Requires HR or ADMIN role");
        }

        try {
            List<AttendanceReport> report = hrReportService.getAttendanceReport(userId, start, end);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/reports/leaves")
    public ResponseEntity<?> getLeaveReport(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(value = "userId", required = false) Long userId) {

        if (!"HR".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Requires HR or ADMIN role");
        }

        try {
            List<LeaveUtilizationReport> report = hrReportService.getLeaveUtilizationReport(userId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
