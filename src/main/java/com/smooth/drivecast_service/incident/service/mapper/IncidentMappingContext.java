package com.smooth.drivecast_service.incident.service.mapper;

import com.smooth.drivecast_service.incident.dto.IncidentEvent;
import lombok.Builder;
import lombok.Value;

/**
 * 사고 메시지 매핑 컨텍스트
 * IncidentEvent 로 변경
 * ====
 * event: IncidentEvent
 * recipientUserId: 수신자 ID
 * isSelfIncident: 본인 사고 여부
 **/
@Value
@Builder
public class IncidentMappingContext {

    IncidentEvent event;
    String recipientUserId;
    boolean isSelfIncident;

    public static IncidentMappingContext of(IncidentEvent event, String recipientUserId) {
        boolean isSelf = event.userId() != null && event.userId().equals(recipientUserId);
        return IncidentMappingContext.builder()
                .event(event)
                .recipientUserId(recipientUserId)
                .isSelfIncident(isSelf)
                .build();
    }

    public static IncidentMappingContext of(IncidentEvent event) {
        return IncidentMappingContext.builder()
                .event(event)
                .recipientUserId(null)
                .isSelfIncident(false)
                .build();
    }
}
