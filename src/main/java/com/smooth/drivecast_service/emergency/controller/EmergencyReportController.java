package com.smooth.drivecast_service.emergency.controller;

import com.smooth.drivecast_service.emergency.feign.dto.EmergencyReportDto;
import com.smooth.drivecast_service.emergency.dto.EmergencyReportResult;
import com.smooth.drivecast_service.emergency.dto.EmergencyRequestDto;
import com.smooth.drivecast_service.emergency.service.EmergencyReportService;

import java.util.List;
import com.smooth.drivecast_service.global.common.ApiResponse;
import com.smooth.drivecast_service.global.exception.CommonErrorCode;
import com.smooth.drivecast_service.global.security.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
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
        String userId = null;
        
        log.info("Principal: {}", principal);
        
        if (principal != null) {
            userId = principal.getName();
            log.info("Principal에서 userId 추출: {}", userId);
        } else {
            String authHeader = request.getHeader("Authorization");
            log.info("Authorization 헤더: {}", authHeader != null ? "Bearer ***" : "null");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    userId = jwtTokenProvider.getUserId(token);
                    log.info("JWT에서 userId 추출 성공: {}", userId);
                } catch (Exception e) {
                    log.error("JWT 파싱 실패", e);
                }
            } else {
                log.warn("Authorization 헤더가 없거나 Bearer 토큰이 아닙니다");
            }
        }
        
        log.info("최종 userId: {}", userId);
        
        if (userId == null) {
            log.error("인증 정보를 찾을 수 없습니다");
            return ApiResponse.error(CommonErrorCode.UNAUTHORIZED);
        }
        
        EmergencyReportResult result = emergencyReportService.processEmergencyDecision(req, userId);
        return ApiResponse.success("응급신고 처리 완료", result);
    }

    @GetMapping("/history")
    public ApiResponse<List<EmergencyReportDto>> getEmergencyHistory(HttpServletRequest request, Principal principal) {
        String userId = null;
        
        if (principal != null) {
            userId = principal.getName();
        } else {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    userId = jwtTokenProvider.getUserId(token);
                } catch (Exception e) {
                    log.error("JWT 파싱 실패", e);
                }
            }
        }
        
        if (userId == null) {
            return ApiResponse.error(CommonErrorCode.UNAUTHORIZED);
        }
        
        List<EmergencyReportDto> history = emergencyReportService.getUserAccidents(userId);
        return ApiResponse.success("신고 내역 조회 완료", history);
    }
}
