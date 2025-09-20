package com.smooth.drivecast_service.global.common.messaging;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RealtimeMessage {
    private String userId;
    private String destination;
    private Object payload;
    private String sourcePodId;
    
    public RealtimeMessage(String userId, String destination, Object payload, String sourcePodId) {
        this.userId = userId;
        this.destination = destination;
        this.payload = payload;
        this.sourcePodId = sourcePodId;
    }
}