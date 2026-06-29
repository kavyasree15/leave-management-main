package com.auth_service.controller;

import com.auth_service.dto.AuthResponse;
import com.auth_service.dto.LoginRequest;
import com.auth_service.dto.RegisterRequest;
import com.auth_service.dto.UserResponse;
import com.auth_service.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody RegisterRequest request) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new com.auth_service.exception.UnauthorizedException("Only administrators can create users");
        }
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/managers")
    public ResponseEntity<List<UserResponse>> getManagers() {
        return ResponseEntity.ok(authService.getManagers());
    }

    @GetMapping("/hr-users")
    public ResponseEntity<List<UserResponse>> getHrUsers() {
        return ResponseEntity.ok(authService.getHrUsers());
    }

    @GetMapping("/hr/pending-employees")
    public ResponseEntity<List<UserResponse>> getHrPendingEmployees(@RequestHeader("X-User-Id") Long hrId) {
        return ResponseEntity.ok(authService.getHrPendingEmployees(hrId));
    }

    @PostMapping("/users/{userId}/assign-manager")
    public ResponseEntity<UserResponse> assignManager(
            @PathVariable Long userId,
            @RequestParam("managerId") Long managerId,
            @RequestHeader("X-User-Id") Long callerId,
            @RequestHeader("X-User-Role") String callerRole) {
        if (!"ADMIN".equalsIgnoreCase(callerRole)) {
            UserResponse employee = authService.getUserById(userId);
            if (!callerId.equals(employee.getHrId())) {
                throw new com.auth_service.exception.UnauthorizedException("Only the assigned HR coordinator can map a manager to this employee");
            }
        }
        return ResponseEntity.ok(authService.assignManager(userId, managerId));
    }

    @GetMapping("/manager/pending-kyc")
    public ResponseEntity<List<UserResponse>> getManagerPendingKyc(@RequestHeader("X-User-Id") Long managerId) {
        return ResponseEntity.ok(authService.getManagerPendingKyc(managerId));
    }

    @PostMapping("/users/{userId}/kyc/approve")
    public ResponseEntity<UserResponse> approveKyc(@PathVariable Long userId) {
        return ResponseEntity.ok(authService.approveKyc(userId));
    }

    @PostMapping("/users/{userId}/kyc/reject")
    public ResponseEntity<UserResponse> rejectKyc(
            @PathVariable Long userId,
            @RequestParam("reason") String reason) {
        return ResponseEntity.ok(authService.rejectKyc(userId, reason));
    }

    @PostMapping(value = "/users/{userId}/kyc", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submitKyc(
            @PathVariable Long userId,
            @RequestParam("fullName") String fullName,
            @RequestParam("mobileNumber") String mobileNumber,
            @RequestParam("dob") String dob,
            @RequestParam("gender") String gender,
            @RequestParam("address") String address,
            @RequestParam("panNumber") String panNumber,
            @RequestParam("aadhaarNumber") String aadhaarNumber,
            @RequestParam(value = "panCard", required = false) org.springframework.web.multipart.MultipartFile panCard,
            @RequestParam(value = "aadhaarCard", required = false) org.springframework.web.multipart.MultipartFile aadhaarCard,
            @RequestParam(value = "passport", required = false) org.springframework.web.multipart.MultipartFile passport,
            @RequestParam(value = "drivingLicense", required = false) org.springframework.web.multipart.MultipartFile drivingLicense
    ) {
        try {
            String panCardUrl = saveFile(userId, "panCard", panCard);
            String aadhaarCardUrl = saveFile(userId, "aadhaarCard", aadhaarCard);
            String passportUrl = saveFile(userId, "passport", passport);
            String drivingLicenseUrl = saveFile(userId, "drivingLicense", drivingLicense);

            UserResponse response = authService.submitKyc(
                    userId, fullName, mobileNumber, dob, gender, address, panNumber, aadhaarNumber,
                    panCardUrl, aadhaarCardUrl, passportUrl, drivingLicenseUrl
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private String saveFile(Long userId, String docType, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        if (file == null || file.isEmpty()) return null;
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = userId + "_" + docType + "_" + System.currentTimeMillis() + extension;
        java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads/kyc");
        if (!java.nio.file.Files.exists(uploadDir)) {
            java.nio.file.Files.createDirectories(uploadDir);
        }
        java.nio.file.Path filePath = uploadDir.resolve(filename);
        java.nio.file.Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return "/api/auth/documents/" + filename;
    }

    @GetMapping("/documents/{filename:.+}")
    public ResponseEntity<?> getDocument(@PathVariable String filename) {
        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get("uploads/kyc").resolve(filename);
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                String contentType = java.nio.file.Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/approve/{userId}")
    public ResponseEntity<String> approveUser(
            @PathVariable Long userId,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new com.auth_service.exception.UnauthorizedException("Only administrators can approve user registrations");
        }
        return ResponseEntity.ok(authService.approveUser(userId));
    }
}
