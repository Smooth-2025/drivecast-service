package com.smooth.drivecast_service.incident.service.mapper;

import com.smooth.drivecast_service.model.AlertEvent;
import lombok.Builder;
import lombok.Value;

/**
 * 사고 메시지 매핑 컨텍스트
 * 기존 AlertEvent 를 사용하여 호환성 유지
 * ====
 * event: 기존 AlertEvent
 * recipientUserId: 수신자 ID
 * isSelfIncident: 본인 사고 여부
 **/
@Value
@Builder
public class IncidentMappingContext {

    AlertEvent event;
    String recipientUserId;
    boolean isSelfIncident;

    public static IncidentMappingContext of(AlertEvent event, String recipientUserId) {
        boolean isSelf = event.userId() != null && event.userId().equals(recipientUserId);
        return IncidentMappingContext.builder()
                .event(event)
                .recipientUserId(recipientUserId)
                .isSelfIncident(isSelf)
                .build();
    }
}
