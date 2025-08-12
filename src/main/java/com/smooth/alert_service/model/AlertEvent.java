package com.smooth.alert_service.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AlertEvent(
        @JsonAlias({"eventType", "type"}) String type,
        String accidentId,
        @JsonAlias({"vehicleId", "userId"}) String userId,
        Double latitude,
        Double longitude,
        String timestamp
) {}
