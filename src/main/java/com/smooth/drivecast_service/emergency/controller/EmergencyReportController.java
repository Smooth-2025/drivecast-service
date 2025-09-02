package com.smooth.drivecast_service.emergency.controller;

import com.smooth.drivecast_service.emergency.feign.dto.EmergencyReportDto;
import com.smooth.drivecast_service.emergency.dto.EmergencyReportResult;
import com.smooth.drivecast_service.emergency.dto.EmergencyRequestDto;
import com.smooth.drivecast_service.emergency.service.EmergencyReportService;
import com.smooth.drivecast_service.global.util.AuthenticationUtils;

import java.util.List;
import com.smooth.drivecast_service.global.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/drivecast/emergency")
@RequiredArgsConstructor
public class EmergencyReportController {

    private final EmergencyReportService emergencyReportService;

    @PostMapping("/decision")
    public ApiResponse<EmergencyReportResult> receiveEmergencyDecision(@Valid @RequestBody EmergencyRequestDto req) {
        Long userId = AuthenticationUtils.getCurrentUserIdOrThrow();
        String userIdStr = userId.toString();
        
        log.info("응급신고 처리 요청 - userId: {}", userIdStr);
        
        EmergencyReportResult result = emergencyReportService.processEmergencyDecision(req, userIdStr);
        return ApiResponse.success("응급신고 처리 완료", result);
    }

    @GetMapping("/history")
    public ApiResponse<List<EmergencyReportDto>> getEmergencyHistory() {
        Long userId = AuthenticationUtils.getCurrentUserIdOrThrow();
        String userIdStr = userId.toString();
        
        log.info("신고 내역 조회 요청 - userId: {}", userIdStr);
        
        List<EmergencyReportDto> history = emergencyReportService.getUserAccidents(userIdStr);
        return ApiResponse.success("신고 내역 조회 완료", history);
    }
}
