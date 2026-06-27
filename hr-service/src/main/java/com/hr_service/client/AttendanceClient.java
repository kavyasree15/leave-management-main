package com.hr_service.client;

import com.hr_service.dto.AttendanceDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(name = "attendance-service")
public interface AttendanceClient {
    @GetMapping("/api/attendance/user/{userId}/range")
    List<AttendanceDto> getUserAttendanceBetween(
            @PathVariable("userId") Long userId,
            @RequestParam("start") String start,
            @RequestParam("end") String end
    );
}
