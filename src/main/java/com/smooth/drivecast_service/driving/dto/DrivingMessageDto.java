package com.smooth.drivecast_service.driving.dto;

import java.util.Map;

/**
 * 주행 도메인 전용 메시지 DTO
 * - 출력 전용 DTO (검증된 데이터로 생성)
 * - 클라이언트 응답용
 **/
public record DrivingMessageDto(
        String type,
        Map<String, Object> payload
) {}
