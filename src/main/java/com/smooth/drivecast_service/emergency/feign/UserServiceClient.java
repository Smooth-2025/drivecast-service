package com.smooth.drivecast_service.emergency.feign;

import com.smooth.drivecast_service.emergency.feign.dto.EmergencyInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "http://localhost:8081", path = "internal/v1/users", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {
    @GetMapping("/{userId}/emergency-info")
    EmergencyInfoResponse getUserInfo(@PathVariable("userId") String userId);
}
