package com.smooth.drivecast_service.driving.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TraitResponseDto(
        @JsonProperty("userId")
        Long userId,
        
        @JsonProperty("character")
        String character
) {

    public boolean hasCharacter() {
        return character != null && !character.isBlank();
    }
}