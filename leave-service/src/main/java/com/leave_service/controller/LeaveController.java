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
            @RequestBody LeaveRequest request) {
        try {
            return ResponseEntity.ok(leaveService.applyLeave(userId, request));
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
            if ("MANAGER".equalsIgnoreCase(role)) {
                return ResponseEntity.ok(leaveService.approveManager(requestId, userId));
            } else if ("HR".equalsIgnoreCase(role)) {
                return ResponseEntity.ok(leaveService.approveHR(requestId, userId));
            } else {
                return ResponseEntity.badRequest().body("Only Manager or HR can approve leave requests");
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
            if ("MANAGER".equalsIgnoreCase(role)) {
                return ResponseEntity.ok(leaveService.rejectManager(requestId, userId));
            } else if ("HR".equalsIgnoreCase(role)) {
                return ResponseEntity.ok(leaveService.rejectHR(requestId, userId));
            } else {
                return ResponseEntity.badRequest().body("Only Manager or HR can reject leave requests");
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
    public ResponseEntity<LeaveBalance> getMyBalance(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(leaveService.getOrCreateBalance(userId));
    }

    @GetMapping("/balance/{userId}")
    public ResponseEntity<LeaveBalance> getUserBalance(@PathVariable Long userId) {
        return ResponseEntity.ok(leaveService.getOrCreateBalance(userId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<LeaveRequest>> getMyLeaves(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(leaveService.getUserLeaves(userId));
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRequests(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        if ("MANAGER".equalsIgnoreCase(role)) {
            return ResponseEntity.ok(leaveService.getPendingManagerApprovals(userId));
        } else if ("HR".equalsIgnoreCase(role)) {
            return ResponseEntity.ok(leaveService.getPendingHRApprovals());
        } else {
            return ResponseEntity.badRequest().body("Only Managers and HR have pending approval dashboards");
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
