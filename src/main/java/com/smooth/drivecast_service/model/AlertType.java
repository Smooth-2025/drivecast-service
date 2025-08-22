package com.smooth.drivecast_service.model;

import java.util.Arrays;
import java.util.Optional;

public enum AlertType {
    ACCIDENT("accident"),
    OBSTACLE("obstacle"),
    START("start"),
    END("end");

    private final String value;

    AlertType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<AlertType> from(String value) {
        return Arrays.stream(values())
                .filter(t->t.value.equalsIgnoreCase(value))
                .findFirst();
    }

    @Override
    public String toString() {
        return value;
    }
}
