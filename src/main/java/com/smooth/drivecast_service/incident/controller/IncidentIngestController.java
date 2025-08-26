package com.smooth.drivecast_service.incident.controller;

import com.smooth.drivecast_service.global.common.ApiResponse;
import com.smooth.drivecast_service.incident.dto.IncidentEvent;
import com.smooth.drivecast_service.incident.service.IncidentEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/incident")
@RequiredArgsConstructor
public class IncidentIngestController {

    private final IncidentEventHandler incidentEventHandler;

    @PostMapping("/ingest")
    public ApiResponse<Void> ingestIncidentEvent(@RequestBody IncidentEvent event) {
        log.info("사고 이벤트 수신: type={}, lat={}, lng={}", event.type(), event.latitude(), event.longitude());
        incidentEventHandler.handle(event);
        return ApiResponse.success("사고 이벤트 수신 성공");
    }
}
