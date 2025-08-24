package com.smooth.drivecast_service.driving.service.mapper;

import com.smooth.drivecast_service.model.AlertEvent;
import lombok.Builder;
import lombok.Value;

/**
 * 주행 메시지 매핑 컨텍스트
 * 기존 AlertEvent를 사용하여 호환성 유지
 * ====
 * event: 기존 AlertEvent
 **/
@Value
@Builder
public class DrivingMappingContext {

    AlertEvent event;

    public static DrivingMappingContext of(AlertEvent event) {
        return DrivingMappingContext.builder()
                .event(event)
                .build();
    }
}
