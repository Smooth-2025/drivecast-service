package com.smooth.drivecast_service.incident.service.mapper;

import com.smooth.drivecast_service.incident.dto.IncidentEvent;
import lombok.Builder;
import lombok.Value;

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
