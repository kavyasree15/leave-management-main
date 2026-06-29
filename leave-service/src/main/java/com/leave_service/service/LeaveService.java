package com.leave_service.service;

import com.leave_service.model.LeaveBalance;
import com.leave_service.model.LeaveRequest;
import com.leave_service.model.LeaveStatus;
import com.leave_service.model.LeaveType;
import com.leave_service.repository.LeaveBalanceRepository;
import com.leave_service.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.kafka.core.KafkaTemplate;
import com.leave_service.dto.NotificationEvent;
import com.leave_service.exception.BadRequestException;
import com.leave_service.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class LeaveService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long getAssignedHrId(Long userId) {
        try {
            return jdbcTemplate.queryForObject("SELECT hr_id FROM users WHERE id = ?", Long.class, userId);
        } catch (Exception e) {
            return null;
        }
    }

    private Long getAssignedManagerId(Long userId) {
        try {
            return jdbcTemplate.queryForObject("SELECT manager_id FROM users WHERE id = ?", Long.class, userId);
        } catch (Exception e) {
            return null;
        }
    }

    public LeaveBalance getOrCreateBalance(Long userId) {
        return leaveBalanceRepository.findByUserId(userId)
                .orElseGet(() -> leaveBalanceRepository.save(new LeaveBalance(userId)));
    }

    public LeaveRequest applyLeave(Long userId, String role, LeaveRequest request, Long hrId) {
        request.setUserId(userId);
        
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be on or after start date");
        }
        
        long duration = calculateBusinessDays(request.getStartDate(), request.getEndDate());
        if (duration <= 0) {
            throw new BadRequestException("Leave duration cannot be 0 business days (selected period falls entirely on weekends)");
        }

        // Validate balance for request before submission
        if (request.getLeaveType() != LeaveType.UNPAID) {
            LeaveBalance balance = getOrCreateBalance(userId);
            int requiredDays = (int) duration;
            boolean hasEnough = switch (request.getLeaveType()) {
                case CASUAL -> balance.getCasualLeave() >= requiredDays;
                case MEDICAL -> balance.getMedicalLeave() >= requiredDays;
                case PAID -> balance.getPaidLeave() >= requiredDays;
                default -> false;
            };
            if (!hasEnough) {
                throw new BadRequestException("Insufficient leave balance");
            }
        }

        // Medical certificate requirement (Anti-Loophole): Contiguous medical leave > 3 days requires certificate
        if (request.getLeaveType() == LeaveType.MEDICAL) {
            long totalContiguousDays = calculateContiguousMedicalDays(userId, request.getStartDate(), request.getEndDate());
            if (totalContiguousDays > 3) {
                if (request.getMedicalCertificate() == null || request.getMedicalCertificate().trim().isEmpty()) {
                    throw new BadRequestException("Medical certificate is mandatory for medical leave (including contiguous blocks) exceeding 3 days");
                }
            }
        }

        // Determine status based on role
        if ("HR".equalsIgnoreCase(role)) {
            request.setStatus(LeaveStatus.PENDING_ADMIN);
            request.setManagerId(0L); // Dummy managerId for schema non-null
        } else if ("MANAGER".equalsIgnoreCase(role)) {
            request.setStatus(LeaveStatus.PENDING_HR);
            request.setManagerId(0L); // Dummy managerId
            // Tag the leave with the manager's assigned HR so only that HR sees it
            Long assignedHrId = getAssignedHrId(userId);
            if (assignedHrId != null) {
                request.setHrId(assignedHrId);
            } else if (hrId != null) {
                request.setHrId(hrId);
            }
        } else {
            // Employee
            request.setStatus(LeaveStatus.PENDING_MANAGER);
            Long assignedManagerId = getAssignedManagerId(userId);
            if (assignedManagerId != null) {
                request.setManagerId(assignedManagerId);
            } else {
                throw new BadRequestException("No reporting manager has been assigned to you by HR yet. Please contact HR to map your manager.");
            }
        }

        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        LeaveRequest saved = leaveRequestRepository.save(request);
        try {
            String subject = "New Leave Request Applied";
            String body = "User ID " + userId + " requested " + request.getLeaveType() + " leave from " + request.getStartDate() + " to " + request.getEndDate();
            String recipient = "Manager_" + request.getManagerId();
            if (saved.getStatus() == LeaveStatus.PENDING_HR) {
                recipient = "HR_all";
            } else if (saved.getStatus() == LeaveStatus.PENDING_ADMIN) {
                recipient = "Admin_0";
            }
            kafkaTemplate.send("notification-topic", new NotificationEvent(recipient, subject, body));
        } catch (Exception e) {
            // Log but don't fail business logic
        }
        return saved;
    }

    @Transactional
    public LeaveRequest approveManager(Long requestId, Long managerId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID " + requestId));

        if (!request.getManagerId().equals(managerId)) {
            throw new BadRequestException("You are not authorized to approve this leave request");
        }

        if (request.getUserId().equals(managerId)) {
            throw new BadRequestException("Self approval is not allowed");
        }

        if (request.getStatus() != LeaveStatus.PENDING_MANAGER) {
            throw new BadRequestException("Request is not pending manager approval");
        }

        long duration = calculateBusinessDays(request.getStartDate(), request.getEndDate());
        if (duration > 10) {
            request.setStatus(LeaveStatus.PENDING_HR);
            Long assignedHrId = getAssignedHrId(request.getUserId());
            if (assignedHrId != null) {
                request.setHrId(assignedHrId);
            }
        } else {
            // Approve and deduct balance
            deductBalance(request.getUserId(), request.getLeaveType(), (int) duration);
            request.setStatus(LeaveStatus.APPROVED);
        }

        request.setUpdatedAt(LocalDateTime.now());
        LeaveRequest saved = leaveRequestRepository.save(request);
        try {
            String subject = saved.getStatus() == LeaveStatus.PENDING_HR ? "Leave Request Approved by Manager (Pending HR)" : "Leave Request Approved";
            String body = "Leave request ID " + requestId + " has been approved by manager ID " + managerId + ". Current status: " + saved.getStatus();
            kafkaTemplate.send("notification-topic", new NotificationEvent("Employee_" + saved.getUserId(), subject, body));
        } catch (Exception e) {}
        return saved;
    }

    public LeaveRequest rejectManager(Long requestId, Long managerId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID " + requestId));

        if (!request.getManagerId().equals(managerId)) {
            throw new BadRequestException("You are not authorized to reject this leave request");
        }

        if (request.getUserId().equals(managerId)) {
            throw new BadRequestException("Self approval/rejection is not allowed");
        }

        if (request.getStatus() != LeaveStatus.PENDING_MANAGER) {
            throw new BadRequestException("Request is not pending manager approval");
        }

        request.setStatus(LeaveStatus.REJECTED);
        request.setUpdatedAt(LocalDateTime.now());
        LeaveRequest saved = leaveRequestRepository.save(request);
        try {
            String subject = "Leave Request Rejected";
            String body = "Leave request ID " + requestId + " has been rejected by manager ID " + managerId;
            kafkaTemplate.send("notification-topic", new NotificationEvent("Employee_" + saved.getUserId(), subject, body));
        } catch (Exception e) {}
        return saved;
    }

    @Transactional
    public LeaveRequest approveHR(Long requestId, Long hrId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID " + requestId));

        if (request.getUserId().equals(hrId)) {
            throw new BadRequestException("Self approval is not allowed");
        }

        if (request.getStatus() != LeaveStatus.PENDING_HR) {
            throw new BadRequestException("Request is not pending HR validation");
        }

        long duration = calculateBusinessDays(request.getStartDate(), request.getEndDate());
        deductBalance(request.getUserId(), request.getLeaveType(), (int) duration);

        request.setHrId(hrId);
        request.setStatus(LeaveStatus.APPROVED);
        request.setUpdatedAt(LocalDateTime.now());
        LeaveRequest saved = leaveRequestRepository.save(request);
        try {
            String subject = "Leave Request Approved by HR";
            String body = "Leave request ID " + requestId + " has been fully approved by HR ID " + hrId;
            kafkaTemplate.send("notification-topic", new NotificationEvent("Employee_" + saved.getUserId(), subject, body));
        } catch (Exception e) {}
        return saved;
    }

    public LeaveRequest rejectHR(Long requestId, Long hrId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID " + requestId));

        if (request.getUserId().equals(hrId)) {
            throw new BadRequestException("Self rejection is not allowed");
        }

        if (request.getStatus() != LeaveStatus.PENDING_HR) {
            throw new BadRequestException("Request is not pending HR validation");
        }

        request.setHrId(hrId);
        request.setStatus(LeaveStatus.REJECTED);
        request.setUpdatedAt(LocalDateTime.now());
        LeaveRequest saved = leaveRequestRepository.save(request);
        try {
            String subject = "Leave Request Rejected by HR";
            String body = "Leave request ID " + requestId + " has been rejected by HR ID " + hrId;
            kafkaTemplate.send("notification-topic", new NotificationEvent("Employee_" + saved.getUserId(), subject, body));
        } catch (Exception e) {}
        return saved;
    }

    @Transactional
    public LeaveRequest cancelLeave(Long requestId, Long userId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID " + requestId));

        if (!request.getUserId().equals(userId)) {
            throw new BadRequestException("You can only cancel your own leave requests");
        }

        if (request.getStatus() == LeaveStatus.CANCELLED || request.getStatus() == LeaveStatus.REJECTED) {
            throw new BadRequestException("Request is already " + request.getStatus());
        }

        // If it was already approved, restore the balance
        if (request.getStatus() == LeaveStatus.APPROVED) {
            long duration = calculateBusinessDays(request.getStartDate(), request.getEndDate());
            restoreBalance(request.getUserId(), request.getLeaveType(), (int) duration);
        }

        request.setStatus(LeaveStatus.CANCELLED);
        request.setUpdatedAt(LocalDateTime.now());
        LeaveRequest saved = leaveRequestRepository.save(request);
        try {
            String subject = "Leave Request Cancelled";
            String body = "Leave request ID " + requestId + " has been cancelled by you.";
            kafkaTemplate.send("notification-topic", new NotificationEvent("Employee_" + saved.getUserId(), subject, body));
        } catch (Exception e) {}
        return saved;
    }

    public List<LeaveRequest> getUserLeaves(Long userId) {
        return leaveRequestRepository.findByUserId(userId);
    }

    public List<LeaveRequest> getPendingManagerApprovals(Long managerId) {
        return leaveRequestRepository.findByManagerIdAndStatus(managerId, LeaveStatus.PENDING_MANAGER);
    }

    public List<LeaveRequest> getPendingHRApprovals(Long hrId) {
        if (hrId != null) {
            // Return only leaves tagged to this specific HR (manager's leave requests)
            // plus any employee long-leave escalations (hrId null on those, so return all PENDING_HR without hrId filter too)
            List<LeaveRequest> hrTagged = leaveRequestRepository.findByHrIdAndStatus(hrId, LeaveStatus.PENDING_HR);
            List<LeaveRequest> untagged = leaveRequestRepository.findByStatus(LeaveStatus.PENDING_HR)
                    .stream().filter(r -> r.getHrId() == null).toList();
            java.util.List<LeaveRequest> combined = new java.util.ArrayList<>(hrTagged);
            combined.addAll(untagged);
            return combined;
        }
        return leaveRequestRepository.findByStatus(LeaveStatus.PENDING_HR);
    }

    public List<LeaveRequest> getPendingAdminApprovals() {
        return leaveRequestRepository.findByStatus(LeaveStatus.PENDING_ADMIN);
    }

    @Transactional
    public LeaveRequest approveAdmin(Long requestId, Long adminId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID " + requestId));

        if (request.getStatus() != LeaveStatus.PENDING_ADMIN) {
            throw new BadRequestException("Request is not pending admin approval");
        }

        long duration = calculateBusinessDays(request.getStartDate(), request.getEndDate());
        deductBalance(request.getUserId(), request.getLeaveType(), (int) duration);

        request.setStatus(LeaveStatus.APPROVED);
        request.setUpdatedAt(LocalDateTime.now());
        LeaveRequest saved = leaveRequestRepository.save(request);
        try {
            String subject = "Leave Request Approved by Admin";
            String body = "Leave request ID " + requestId + " has been approved by Admin";
            kafkaTemplate.send("notification-topic", new NotificationEvent("Employee_" + saved.getUserId(), subject, body));
        } catch (Exception e) {}
        return saved;
    }

    public LeaveRequest rejectAdmin(Long requestId, Long adminId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID " + requestId));

        if (request.getStatus() != LeaveStatus.PENDING_ADMIN) {
            throw new BadRequestException("Request is not pending admin approval");
        }

        request.setStatus(LeaveStatus.REJECTED);
        request.setUpdatedAt(LocalDateTime.now());
        LeaveRequest saved = leaveRequestRepository.save(request);
        try {
            String subject = "Leave Request Rejected by Admin";
            String body = "Leave request ID " + requestId + " has been rejected by Admin";
            kafkaTemplate.send("notification-topic", new NotificationEvent("Employee_" + saved.getUserId(), subject, body));
        } catch (Exception e) {}
        return saved;
    }

    public List<LeaveRequest> getAllLeaves() {
        return leaveRequestRepository.findAll();
    }

    private void deductBalance(Long userId, LeaveType type, int days) {
        if (type == LeaveType.UNPAID) return;
        LeaveBalance balance = getOrCreateBalance(userId);
        switch (type) {
            case CASUAL -> {
                if (balance.getCasualLeave() < days) throw new BadRequestException("Insufficient casual leave balance");
                balance.setCasualLeave(balance.getCasualLeave() - days);
            }
            case MEDICAL -> {
                if (balance.getMedicalLeave() < days) throw new BadRequestException("Insufficient medical leave balance");
                balance.setMedicalLeave(balance.getMedicalLeave() - days);
            }
            case PAID -> {
                if (balance.getPaidLeave() < days) throw new BadRequestException("Insufficient paid leave balance");
                balance.setPaidLeave(balance.getPaidLeave() - days);
            }
        }
        leaveBalanceRepository.save(balance);
    }

    private void restoreBalance(Long userId, LeaveType type, int days) {
        if (type == LeaveType.UNPAID) return;
        LeaveBalance balance = getOrCreateBalance(userId);
        switch (type) {
            case CASUAL -> balance.setCasualLeave(balance.getCasualLeave() + days);
            case MEDICAL -> balance.setMedicalLeave(balance.getMedicalLeave() + days);
            case PAID -> balance.setPaidLeave(balance.getPaidLeave() + days);
        }
        leaveBalanceRepository.save(balance);
    }

    // Process escalations: If any request is in PENDING_MANAGER status and is older than 2 days, auto-escalate it
    // Wait, let's keep it simple: just list escalatable requests or let a scheduled task run it
    @Transactional
    public int escalatePendingLeaves() {
        List<LeaveRequest> pending = leaveRequestRepository.findByStatus(LeaveStatus.PENDING_MANAGER);
        int count = 0;
        LocalDateTime threshold = LocalDateTime.now().minusDays(2);
        for (LeaveRequest request : pending) {
            if (request.getCreatedAt().isBefore(threshold)) {
                // Escalate to HR validation directly
                request.setStatus(LeaveStatus.PENDING_HR);
                request.setUpdatedAt(LocalDateTime.now());
                leaveRequestRepository.save(request);
                count++;
            }
        }
        return count;
    }

    private long calculateBusinessDays(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        long days = 0;
        java.time.LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            java.time.DayOfWeek dow = current.getDayOfWeek();
            if (dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY) {
                days++;
            }
            current = current.plusDays(1);
        }
        return days;
    }

    private long calculateContiguousMedicalDays(Long userId, java.time.LocalDate newStart, java.time.LocalDate newEnd) {
        List<LeaveRequest> existingLeaves = leaveRequestRepository.findByUserId(userId);
        java.util.Set<java.time.LocalDate> medicalDates = new java.util.HashSet<>();
        
        java.time.LocalDate d = newStart;
        while (!d.isAfter(newEnd)) {
            medicalDates.add(d);
            d = d.plusDays(1);
        }
        
        for (LeaveRequest lr : existingLeaves) {
            if (lr.getLeaveType() == LeaveType.MEDICAL && 
                lr.getStatus() != LeaveStatus.REJECTED && 
                lr.getStatus() != LeaveStatus.CANCELLED) {
                java.time.LocalDate curr = lr.getStartDate();
                while (!curr.isAfter(lr.getEndDate())) {
                    medicalDates.add(curr);
                    curr = curr.plusDays(1);
                }
            }
        }
        
        java.time.LocalDate checkBackward = newStart.minusDays(1);
        while (medicalDates.contains(checkBackward)) {
            checkBackward = checkBackward.minusDays(1);
        }
        
        java.time.LocalDate checkForward = newStart;
        while (medicalDates.contains(checkForward)) {
            checkForward = checkForward.plusDays(1);
        }
        
        return calculateBusinessDays(checkBackward.plusDays(1), checkForward.minusDays(1));
    }
}
