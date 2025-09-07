package com.smooth.drivecast_service.global.common.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Redis Pub/Sub를 통해 전송되는 실시간 메시지 DTO
 */
@Data
@NoArgsConstructor
public class RealtimeMessage {
    private String userId;
    private String destination;
    private Object payload;
    private String sourcePodId; // 메시지를 발행한 Pod ID
    
    public RealtimeMessage(String userId, String destination, Object payload) {
        this.userId = userId;
        this.destination = destination;
        this.payload = payload;
        // sourcePodId는 StompRealtimePublisher에서 설정
    }
    
    public RealtimeMessage(String userId, String destination, Object payload, String sourcePodId) {
        this.userId = userId;
        this.destination = destination;
        this.payload = payload;
        this.sourcePodId = sourcePodId;
    }
}