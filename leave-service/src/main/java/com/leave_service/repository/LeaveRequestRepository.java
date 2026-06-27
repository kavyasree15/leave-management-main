package com.leave_service.repository;

import com.leave_service.model.LeaveRequest;
import com.leave_service.model.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByUserId(Long userId);
    List<LeaveRequest> findByManagerId(Long managerId);
    List<LeaveRequest> findByStatus(LeaveStatus status);
    List<LeaveRequest> findByManagerIdAndStatus(Long managerId, LeaveStatus status);
}
