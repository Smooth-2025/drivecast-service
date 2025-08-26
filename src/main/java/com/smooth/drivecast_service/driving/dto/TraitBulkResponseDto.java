package com.smooth.drivecast_service.driving.dto;

import java.time.Instant;
import java.util.List;

/**
 * 벌크 성향 조회 응답 DTO
 * 외부 API 응답: { "data": [...], "generatedAtUtc": "2025-08-26T19:05:00Z" }
 */
public record TraitBulkResponseDto(
        List<TraitResponseDto> data,
        Instant generatedAtUtc
) {

    /**
     * 데이터가 있는지 확인
     */
    public boolean hasData() {
        return data != null && !data.isEmpty();
    }

    /**
     * 데이터 개수 반환
     */
    public int size() {
        return data != null ? data.size() : 0;
    }
}
