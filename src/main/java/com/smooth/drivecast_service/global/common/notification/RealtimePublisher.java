package com.smooth.drivecast_service.global.common.notification;

/**
 * 실시간 메세지 전송 인터페이스
 * 도메인에서 전송 경로를 결정하고, 실제 전송은 이 인터페이스로 위임
 **/
public interface RealtimePublisher {

    /**
     * 특정 사용자에게 메시지 전송
     * @param userId 수신자 ID
     * @param destination 목적지 경로 (예: /user/queue/alert)
     * @param payload 전송할 데이터 (보통 RealtimeEnvelope)
     **/
    void toUser(String userId, String destination, Object payload);
}
