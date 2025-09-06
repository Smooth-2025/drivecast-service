package com.smooth.drivecast_service.emergency.controller;

import com.smooth.drivecast_service.emergency.dto.EmergencyResponseDto;
import com.smooth.drivecast_service.emergency.service.EmergencyReportService;
import com.smooth.drivecast_service.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "/internal/v1/accidents", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AccidentController {
    private final EmergencyReportService emergencyReportService;

    @GetMapping(value = "/{accidentId}/emergency")
    public ResponseEntity<ApiResponse<EmergencyResponseDto>> getAccidentById(@PathVariable("accidentId") String accidentId) {
        EmergencyResponseDto response = emergencyReportService.getAccidentById(accidentId);
        return ResponseEntity.ok(ApiResponse.success("사고 정보 조회 완료", response));
    }
}