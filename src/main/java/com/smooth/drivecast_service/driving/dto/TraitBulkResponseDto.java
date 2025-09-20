package com.smooth.drivecast_service.driving.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record TraitBulkResponseDto(
        @JsonProperty("data")
        List<TraitResponseDto> data,

        @JsonProperty("generatedAtUtc")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant generatedAtUtc
) {

    public boolean hasData() {
        return data != null && !data.isEmpty();
    }
}
