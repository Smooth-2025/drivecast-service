package com.smooth.drivecast_service.global.common.messaging;

import lombok.Builder;
import lombok.Value;

/**
 * 실시간 메세지 표준 Envelope (서버 내부 처리용)
 * 
 * ⚠️ 현재 미사용 - 향후 메시지 메타데이터 추가 시 사용 예정
 * 
 * 현재: payload 직접 전송
 * 향후: RealtimeEnvelope로 래핑하여 메타데이터 포함 전송
 * 
 * == 서버 내부 메타데이터 ==
 * messageCategory: INCIDENT_ALERT, DRIVING_DATA 등 (서버 분류용)
 * version: v1 (서버 버전 관리용)
 * correlationId: 수신자 (라우팅용)
 * userId: 메시지 추적용 ID
 * serverTimestamp: 서버 처리 시각
 * == 클라이언트 전송 데이터 ==
 * payload: 클라이언트가 받을 실제 메시지
 **/
@Value
@Builder
public class RealtimeEnvelope<T> {

    // == 서버 내부 메타데이터 ==
    String messageCategory;
    String version;
    String correlationId;
    String userId;
    String serverTimestamp;

    // == 클라이언트 전송 데이터 ==
    T payload;
}
