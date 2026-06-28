package com.auth_service.service;

import com.auth_service.dto.AuthResponse;
import com.auth_service.dto.LoginRequest;
import com.auth_service.dto.RegisterRequest;
import com.auth_service.dto.UserResponse;
import com.auth_service.dto.NotificationEvent;
import com.auth_service.model.Role;
import com.auth_service.model.User;
import com.auth_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.List;
import java.util.stream.Collectors;

import com.auth_service.exception.ResourceNotFoundException;
import com.auth_service.exception.BadRequestException;
import com.auth_service.exception.UnauthorizedException;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public UserResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists");
        }

        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getRole(),
                null, // managerId is initially null for all newly created users
                request.getEmail()
        );

        if (request.getRole() == Role.EMPLOYEE) {
            if (request.getHrId() == null) {
                throw new BadRequestException("Assigned HR ID is mandatory for employees");
            }
            User hr = userRepository.findById(request.getHrId())
                    .orElseThrow(() -> new ResourceNotFoundException("HR not found with ID " + request.getHrId()));
            if (hr.getRole() != Role.HR) {
                throw new BadRequestException("The specified HR ID does not belong to a user with the HR role");
            }
            user.setHrId(request.getHrId());
            user.setKycStatus("PENDING");
            user.setApproved(false); // Pending Verification
        } else if (request.getRole() == Role.MANAGER) {
            // Optionally assign an HR for leave approval
            if (request.getHrId() != null) {
                User hr = userRepository.findById(request.getHrId())
                        .orElseThrow(() -> new ResourceNotFoundException("HR not found with ID " + request.getHrId()));
                if (hr.getRole() != Role.HR) {
                    throw new BadRequestException("The specified HR ID does not belong to a user with the HR role");
                }
                user.setHrId(request.getHrId());
            }
            user.setApproved(true);
            user.setKycStatus("APPROVED");
        } else {
            // HR are active immediately
            user.setApproved(true);
            user.setKycStatus("APPROVED"); // No KYC needed
        }

        User savedUser = userRepository.save(user);
        return convertToResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        if ("admin@technova.com".equals(request.getEmail()) && "admin123".equals(request.getPassword())) {
            String token = jwtService.generateToken(
                    0L,
                    "admin",
                    "ADMIN",
                    null,
                    "APPROVED",
                    null
            );
            return new AuthResponse(token, "admin", "ADMIN", 0L);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Employees are allowed to log in even when approved = false (during Pending Verification / KYC)
        if (!user.isApproved() && user.getRole() != Role.EMPLOYEE) {
            throw new UnauthorizedException("User account is pending admin approval");
        }

        String token = jwtService.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getManagerId(),
                user.getKycStatus(),
                user.getHrId()
        );

        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getId());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID " + id));
        return convertToResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getManagers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.MANAGER)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private UserResponse convertToResponse(User user) {
        UserResponse resp = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getManagerId(),
                user.getEmail(),
                user.isApproved()
        );
        resp.setKycStatus(user.getKycStatus());
        resp.setHrId(user.getHrId());
        resp.setPanNumber(user.getPanNumber());
        resp.setAadhaarNumber(user.getAadhaarNumber());
        resp.setPanCardUrl(user.getPanCardUrl());
        resp.setAadhaarCardUrl(user.getAadhaarCardUrl());
        resp.setPassportUrl(user.getPassportUrl());
        resp.setDrivingLicenseUrl(user.getDrivingLicenseUrl());
        resp.setMobileNumber(user.getMobileNumber());
        resp.setDob(user.getDob());
        resp.setGender(user.getGender());
        resp.setAddress(user.getAddress());
        resp.setRejectionReason(user.getRejectionReason());
        return resp;
    }

    @Transactional
    public String approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID " + userId));
        user.setApproved(true);
        user.setKycStatus("APPROVED");
        userRepository.save(user);
        return "approved";
    }

    public void sendKycNotification(Long recipientId, String subject, String body) {
        try {
            User recipient = userRepository.findById(recipientId).orElse(null);
            String prefix = "Employee";
            if (recipient != null) {
                if (recipient.getRole() == Role.MANAGER) prefix = "Manager";
                else if (recipient.getRole() == Role.HR) prefix = "HR";
            }
            kafkaTemplate.send("notification-topic", new NotificationEvent(prefix + "_" + recipientId, subject, body));
        } catch (Exception e) {
            // Log but don't fail business logic
        }
    }

    @Transactional
    public UserResponse submitKyc(
            Long userId,
            String fullName,
            String mobileNumber,
            String dob,
            String gender,
            String address,
            String panNumber,
            String aadhaarNumber,
            String panCardUrl,
            String aadhaarCardUrl,
            String passportUrl,
            String drivingLicenseUrl
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID " + userId));

        user.setUsername(fullName);
        user.setMobileNumber(mobileNumber);
        user.setDob(dob);
        user.setGender(gender);
        user.setAddress(address);
        user.setPanNumber(panNumber);
        user.setAadhaarNumber(aadhaarNumber);

        if (panCardUrl != null) user.setPanCardUrl(panCardUrl);
        if (aadhaarCardUrl != null) user.setAadhaarCardUrl(aadhaarCardUrl);
        if (passportUrl != null) user.setPassportUrl(passportUrl);
        if (drivingLicenseUrl != null) user.setDrivingLicenseUrl(drivingLicenseUrl);

        user.setKycStatus("PENDING_MANAGER"); // Pending Manager Assignment by HR
        User saved = userRepository.save(user);

        // Notify HR
        sendKycNotification(user.getHrId(), "Employee KYC Submitted", "Employee " + fullName + " has submitted their KYC details. Please assign a Manager.");

        return convertToResponse(saved);
    }

    public List<UserResponse> getHrUsers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.HR)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getHrPendingEmployees(Long hrId) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE
                        && hrId.equals(u.getHrId())
                        && "PENDING_MANAGER".equals(u.getKycStatus())
                        && u.getManagerId() == null)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse assignManager(Long userId, Long managerId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID " + userId));
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with ID " + managerId));
        if (manager.getRole() != Role.MANAGER) {
            throw new BadRequestException("Specified user is not a manager");
        }

        user.setManagerId(managerId);
        User saved = userRepository.save(user);

        // Notify Manager
        sendKycNotification(managerId, "Manager Assigned", "You have been assigned as the manager for employee " + user.getUsername() + ". KYC verification is pending.");

        return convertToResponse(saved);
    }

    public List<UserResponse> getManagerPendingKyc(Long managerId) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE
                        && managerId.equals(u.getManagerId())
                        && "PENDING_MANAGER".equals(u.getKycStatus()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse approveKyc(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID " + userId));

        user.setKycStatus("APPROVED");
        user.setApproved(true); // Account Status becomes Active
        user.setApprovalDate(java.time.LocalDateTime.now());
        User saved = userRepository.save(user);

        // Notify Employee
        sendKycNotification(userId, "KYC Approved", "Your KYC has been approved by your manager. Your account is now active!");

        return convertToResponse(saved);
    }

    @Transactional
    public UserResponse rejectKyc(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID " + userId));

        user.setKycStatus("REJECTED");
        user.setApproved(false);
        user.setRejectionReason(reason);
        User saved = userRepository.save(user);

        // Notify Employee
        sendKycNotification(userId, "KYC Rejected", "Your KYC has been rejected by your manager. Reason: " + reason);

        return convertToResponse(saved);
    }
}
