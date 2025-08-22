package com.smooth.drivecast_service.model;

import java.util.Arrays;
import java.util.Optional;

public enum EventType {
    ACCIDENT("accident", 300),
    OBSTACLE("obstacle", 100),
    START("start", 0),
    END("end", 0);

    private final String type;
    private final int radiusMeters;

    EventType(String type, int radiusMeters) {
        this.type = type;
        this.radiusMeters = radiusMeters;
    }

    public String getType() {
        return type;
    }

    public int getRadiusMeters() {
        return radiusMeters;
    }

    public static Optional<EventType> from(String type) {
        return Arrays.stream(values())
                .filter(t -> t.type.equalsIgnoreCase(type))
                .findFirst();
    }

    @Override
    public String toString() {
        return type;
    }
}