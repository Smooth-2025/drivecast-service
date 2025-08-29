package com.smooth.drivecast_service.emergency.feign;

import com.smooth.drivecast_service.emergency.feign.dto.EmergencyReportDto;
import com.smooth.drivecast_service.emergency.service.EmergencyReportService;
import com.smooth.drivecast_service.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/internal/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class InternalController {
    private final EmergencyReportService emergencyReportService;

    @GetMapping(value = "/users/{userId}/accidents")
    public ApiResponse<List<EmergencyReportDto>> getUserAccidents(@PathVariable("userId") String userId) {
        List<EmergencyReportDto> accidents = emergencyReportService.getUserAccidents(userId);
        return ApiResponse.success("유저 사고 이력 조회 완료", accidents);
    }
}