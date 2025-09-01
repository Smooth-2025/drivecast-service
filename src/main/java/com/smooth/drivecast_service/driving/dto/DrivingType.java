package com.smooth.drivecast_service.driving.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.smooth.drivecast_service.driving.exception.DrivingErrorCode;
import com.smooth.drivecast_service.global.exception.BusinessException;

import java.util.Arrays;
import java.util.Optional;

public enum DrivingType {
    START("start"),
    END("end");

    private final String value;

    DrivingType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DrivingType fromValue(String value) {
        return from(value)
                .orElseThrow(() -> new BusinessException(DrivingErrorCode.INVALID_DRIVING_TYPE, value));
    }

    public static Optional<DrivingType> from(String value) {
        return Arrays.stream(values())
                .filter(t -> t.value.equalsIgnoreCase(value))
                .findFirst();
    }

    @Override
    public String toString() {
        return value;
    }
}
