package com.smooth.drivecast_service.emergency.controller;

import com.smooth.drivecast_service.emergency.dto.EmergencyReportResult;
import com.smooth.drivecast_service.emergency.dto.EmergencyRequestDto;
import com.smooth.drivecast_service.emergency.service.EmergencyReportService;
import com.smooth.drivecast_service.global.common.ApiResponse;
import com.smooth.drivecast_service.global.security.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/api/drivecast/emergency")
@RequiredArgsConstructor
public class EmergencyReportController {

    private final EmergencyReportService emergencyReportService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/decision")
    public ApiResponse<EmergencyReportResult> receiveEmergencyDecision(@Valid @RequestBody EmergencyRequestDto req, 
                                                                       HttpServletRequest request, 
                                                                       Principal principal) {
        // Principal이 있으면 우선 사용, 없으면 JWT에서 추출
        String userId = null;
        
        if (principal != null) {
            userId = principal.getName();
            log.info("Principal에서 userId 추출: {}", userId);
        } else {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                userId = jwtTokenProvider.getUserId(token);
                log.info("JWT에서 userId 추출: {}", userId);
            }
        }
        
        EmergencyReportResult result = emergencyReportService.processEmergencyDecision(req, userId);
        return ApiResponse.success("응급신고 처리 완료", result);
    }
}
