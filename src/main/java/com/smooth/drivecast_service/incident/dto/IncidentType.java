package com.smooth.drivecast_service.incident.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.smooth.drivecast_service.global.exception.BusinessException;
import com.smooth.drivecast_service.incident.exception.IncidentErrorCode;

import java.util.Arrays;
import java.util.Optional;

public enum IncidentType {
    ACCIDENT("accident", 300),
    OBSTACLE("obstacle", 100);

    private final String value;
    private final int radiusMeters;

    IncidentType(String value, int radiusMeters) {
        this.value = value;
        this.radiusMeters = radiusMeters;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public int getRadiusMeters() {
        return radiusMeters;
    }

    @JsonCreator
    public static IncidentType fromValue(String value) {
        return from(value)
                .orElseThrow(() -> new BusinessException(IncidentErrorCode.INVALID_INCIDENT_TYPE, value));
    }

    public static Optional<IncidentType> from(String value) {
        return Arrays.stream(values())
                .filter(t -> t.value.equalsIgnoreCase(value))
                .findFirst();
    }

    @Override
    public String toString() {
        return value;
    }
}
