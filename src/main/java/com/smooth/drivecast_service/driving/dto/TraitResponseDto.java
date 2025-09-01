package com.smooth.drivecast_service.driving.dto;

/**
 * 성향 조회 응답 DTO
 * 외부 API 응답: { "userId": "31", "character": "DOLPHIN" }
 */
public record TraitResponseDto(
        String userId,
        String character
) {

    /**
     * 성향이 있는지 확인
     **/
    public boolean hasCharacter() {
        return character != null && !character.isBlank();
    }

    /**
     * 성향 없음을 나타내는 인스턴스 생성
     **/
    public static TraitResponseDto withoutCharacter(String userId) {
        return new TraitResponseDto(userId, null);
    }
}