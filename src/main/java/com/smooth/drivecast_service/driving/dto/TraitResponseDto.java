package com.smooth.drivecast_service.driving.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 성향 조회 응답 DTO
 * 외부 API 응답: { "userId": "31", "character": "DOLPHIN" }
 */
public record TraitResponseDto(
        @JsonProperty("userId")
        Long userId,
        
        @JsonProperty("character")
        String character
) {

    /**
     * 성향이 있는지 확인
     **/
    public boolean hasCharacter() {
        return character != null && !character.isBlank();
    }
}