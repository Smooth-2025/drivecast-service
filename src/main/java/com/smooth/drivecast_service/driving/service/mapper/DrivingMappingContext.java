package com.smooth.drivecast_service.driving.service.mapper;

import com.smooth.drivecast_service.driving.dto.DrivingEvent;
import lombok.Builder;
import lombok.Value;

/**
 * 주행 메시지 매핑 컨텍스트
 * DrivingEvent
 * ====
 * event: DrivingEvent
 **/
@Value
@Builder
public class DrivingMappingContext {

    DrivingEvent event;

    public static DrivingMappingContext of(DrivingEvent event) {
        return DrivingMappingContext.builder()
                .event(event)
                .build();
    }
}
