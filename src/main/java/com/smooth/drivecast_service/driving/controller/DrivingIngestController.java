package com.smooth.drivecast_service.driving.controller;

import com.smooth.drivecast_service.driving.dto.DrivingEvent;
import com.smooth.drivecast_service.driving.service.DrivingEventHandler;
import com.smooth.drivecast_service.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/drive-status")
@RequiredArgsConstructor
public class DrivingIngestController {

    private final DrivingEventHandler drivingEventHandler;

    @PostMapping("/ingest")
    public ApiResponse<Void> ingestDrivingEvent(@RequestBody DrivingEvent event) {
        log.info("주행 이벤트 수신: type={}, userId={}", event.type(), event.userId());
        drivingEventHandler.handle(event);
        return ApiResponse.success("주행 이벤트 수신 성공");
    }
}
