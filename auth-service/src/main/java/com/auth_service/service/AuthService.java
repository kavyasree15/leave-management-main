package com.auth_service.service;

import com.auth_service.dto.AuthResponse;
import com.auth_service.dto.LoginRequest;
import com.auth_service.dto.RegisterRequest;
import com.auth_service.dto.UserResponse;
import com.auth_service.model.Role;
import com.auth_service.model.User;
import com.auth_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public UserResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists");
        }
        if (request.getRole() == Role.EMPLOYEE) {
            if (request.getManagerId() == null) {
                throw new BadRequestException("Manager ID is mandatory for employees");
            }
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with ID " + request.getManagerId()));
            if (manager.getRole() != Role.MANAGER) {
                throw new BadRequestException("The specified manager ID does not belong to a user with the MANAGER role");
            }
        } else {
            if (request.getManagerId() != null) {
                throw new BadRequestException("Managers and HR administrators cannot have a manager ID");
            }
        }

        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getRole(),
                request.getManagerId(),
                request.getEmail()
        );
        user.setApproved(false);

        User savedUser = userRepository.save(user);
        return convertToResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        if ("admin@technova.com".equals(request.getEmail()) && "admin123".equals(request.getPassword())) {
            String token = jwtService.generateToken(
                    0L,
                    "admin",
                    "ADMIN",
                    null
            );
            return new AuthResponse(token, "admin", "ADMIN", 0L);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.isApproved()) {
            throw new UnauthorizedException("User account is pending admin approval");
        }

        String token = jwtService.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getManagerId()
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
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getManagerId(),
                user.getEmail(),
                user.isApproved()
        );
    }

    @Transactional
    public String approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID " + userId));
        user.setApproved(true);
        userRepository.save(user);
        return "approved";
    }
}
