package com.smooth.alert_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AlertEvent(
        String type,
        String accidentId,
        String userId,
        Double latitude,
        Double longitude,
        String timestamp
) {}
