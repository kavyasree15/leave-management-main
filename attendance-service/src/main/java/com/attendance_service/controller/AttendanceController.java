package com.attendance_service.controller;

import com.attendance_service.model.Attendance;
import com.attendance_service.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Kyc-Status", required = false) String kycStatus) {
        if ("EMPLOYEE".equalsIgnoreCase(role) && !"APPROVED".equalsIgnoreCase(kycStatus)) {
            return ResponseEntity.badRequest().body("KYC verification is required before accessing attendance");
        }
        try {
            return ResponseEntity.ok(attendanceService.checkIn(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkOut(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Kyc-Status", required = false) String kycStatus) {
        if ("EMPLOYEE".equalsIgnoreCase(role) && !"APPROVED".equalsIgnoreCase(kycStatus)) {
            return ResponseEntity.badRequest().body("KYC verification is required before accessing attendance");
        }
        try {
            return ResponseEntity.ok(attendanceService.checkOut(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/today")
    public ResponseEntity<?> getTodayRecord(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Kyc-Status", required = false) String kycStatus) {
        if ("EMPLOYEE".equalsIgnoreCase(role) && !"APPROVED".equalsIgnoreCase(kycStatus)) {
            return ResponseEntity.badRequest().body("KYC verification is required before accessing attendance");
        }
        return ResponseEntity.ok(attendanceService.getTodayRecord(userId));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getUserHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Kyc-Status", required = false) String kycStatus) {
        if ("EMPLOYEE".equalsIgnoreCase(role) && !"APPROVED".equalsIgnoreCase(kycStatus)) {
            return ResponseEntity.badRequest().body("KYC verification is required before accessing attendance");
        }
        return ResponseEntity.ok(attendanceService.getUserHistory(userId));
    }

    @GetMapping("/range")
    public ResponseEntity<List<Attendance>> getAttendanceBetween(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(attendanceService.getAttendanceBetween(start, end));
    }

    @GetMapping("/user/{userId}/range")
    public ResponseEntity<List<Attendance>> getUserAttendanceBetween(
            @PathVariable Long userId,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(attendanceService.getUserAttendanceBetween(userId, start, end));
    }
}
