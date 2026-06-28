package com.leave_service.controller;

import com.leave_service.model.LeaveBalance;
import com.leave_service.model.LeaveRequest;
import com.leave_service.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @PostMapping("/apply")
    public ResponseEntity<?> applyLeave(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Kyc-Status", required = false) String kycStatus,
            @RequestHeader(value = "X-User-Hr-Id", required = false) Long hrId,
            @RequestBody LeaveRequest request) {
        if ("EMPLOYEE".equalsIgnoreCase(role) && !"APPROVED".equalsIgnoreCase(kycStatus)) {
            return ResponseEntity.badRequest().body("KYC verification is required before applying for leaves");
        }
        try {
            return ResponseEntity.ok(leaveService.applyLeave(userId, role, request, hrId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<?> approveLeave(
            @PathVariable Long requestId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        try {
            if ("ADMIN".equalsIgnoreCase(role)) {
                return ResponseEntity.ok(leaveService.approveAdmin(requestId, userId));
            } else if ("MANAGER".equalsIgnoreCase(role)) {
                return ResponseEntity.ok(leaveService.approveManager(requestId, userId));
            } else if ("HR".equalsIgnoreCase(role)) {
                return ResponseEntity.ok(leaveService.approveHR(requestId, userId));
            } else {
                return ResponseEntity.badRequest().body("Only Manager, HR, or Admin can approve leave requests");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<?> rejectLeave(
            @PathVariable Long requestId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        try {
            if ("ADMIN".equalsIgnoreCase(role)) {
                return ResponseEntity.ok(leaveService.rejectAdmin(requestId, userId));
            } else if ("MANAGER".equalsIgnoreCase(role)) {
                return ResponseEntity.ok(leaveService.rejectManager(requestId, userId));
            } else if ("HR".equalsIgnoreCase(role)) {
                return ResponseEntity.ok(leaveService.rejectHR(requestId, userId));
            } else {
                return ResponseEntity.badRequest().body("Only Manager, HR, or Admin can reject leave requests");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<?> cancelLeave(
            @PathVariable Long requestId,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            return ResponseEntity.ok(leaveService.cancelLeave(requestId, userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getMyBalance(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Kyc-Status", required = false) String kycStatus) {
        if ("EMPLOYEE".equalsIgnoreCase(role) && !"APPROVED".equalsIgnoreCase(kycStatus)) {
            return ResponseEntity.badRequest().body("KYC verification is required before checking leave balance");
        }
        return ResponseEntity.ok(leaveService.getOrCreateBalance(userId));
    }

    @GetMapping("/balance/{userId}")
    public ResponseEntity<LeaveBalance> getUserBalance(@PathVariable Long userId) {
        return ResponseEntity.ok(leaveService.getOrCreateBalance(userId));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyLeaves(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Kyc-Status", required = false) String kycStatus) {
        if ("EMPLOYEE".equalsIgnoreCase(role) && !"APPROVED".equalsIgnoreCase(kycStatus)) {
            return ResponseEntity.badRequest().body("KYC verification is required before checking leave history");
        }
        return ResponseEntity.ok(leaveService.getUserLeaves(userId));
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRequests(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-User-Hr-Id", required = false) Long hrId) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.ok(leaveService.getPendingAdminApprovals());
        } else if ("MANAGER".equalsIgnoreCase(role)) {
            return ResponseEntity.ok(leaveService.getPendingManagerApprovals(userId));
        } else if ("HR".equalsIgnoreCase(role)) {
            return ResponseEntity.ok(leaveService.getPendingHRApprovals(hrId));
        } else {
            return ResponseEntity.badRequest().body("Only Admin, HR, and Managers have pending approval dashboards");
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<LeaveRequest>> getAllLeaves() {
        return ResponseEntity.ok(leaveService.getAllLeaves());
    }

    @PostMapping("/escalate")
    public ResponseEntity<String> escalatePendingLeaves() {
        int escalated = leaveService.escalatePendingLeaves();
        return ResponseEntity.ok("Escalated " + escalated + " pending leave requests to HR");
    }
}
