package com.hr_service.client;

import com.hr_service.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@FeignClient(name = "auth-service")
public interface AuthClient {
    @GetMapping("/api/auth/users/{id}")
    UserDto getUserById(@PathVariable("id") Long id);

    @GetMapping("/api/auth/users")
    List<UserDto> getAllUsers();
}
