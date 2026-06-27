package com.hr_service.client;

import com.hr_service.dto.LeaveBalanceDto;
import com.hr_service.dto.LeaveRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@FeignClient(name = "leave-service")
public interface LeaveClient {
    @GetMapping("/api/leaves/balance/{userId}")
    LeaveBalanceDto getUserBalance(@PathVariable("userId") Long userId);

    @GetMapping("/api/leaves/all")
    List<LeaveRequestDto> getAllLeaves();

    @GetMapping("/api/leaves/my")
    List<LeaveRequestDto> getMyLeaves();
}
