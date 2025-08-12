package com.smooth.alert_service.support.validator;

import com.smooth.alert_service.model.AlertType;
import com.smooth.alert_service.model.AlertEvent;

public class AlertEventValidator {

    public static void validate(AlertEvent event) {
        if (event.type() == null || event.type().isBlank()) {
            throw new IllegalArgumentException("type is required");
        }

        AlertType type = AlertType.from(event.type())
                .orElseThrow(() -> new IllegalArgumentException("invalid alert type: " + event.type()));

        switch (type) {
            case ACCIDENT -> {
                require(event.accidentId(), "accidentId is required for type=accident");
                require(event.userId(), "userId(vehicleId) is required for type=accident");
                require(event.latitude(), "latitude is required for type=accident");
                require(event.longitude(), "longitude is required for type=accident");
                require(event.timestamp(), "timestamp is required for type=accident");
            }
            case OBSTACLE -> {
                require(event.userId(), "userId(vehicleId) is required for type=obstacle");
                require(event.latitude(), "latitude is required for type=obstacle");
                require(event.longitude(), "longitude is required for type=obstacle");
                require(event.timestamp(), "timestamp is required for type=obstacle");
            }
            case POTHOLE -> {
                require(event.latitude(), "latitude is required for type=pothole");
                require(event.longitude(), "longitude is required for type=pothole");
                require(event.timestamp(), "timestamp is required for type=pothole");
            }
            case START, END -> {
                require(event.userId(), "userId(vehicleId) is required for type=" + type);
                require(event.timestamp(), "timestamp is required for type=" + type);
            }
        }
    }

    private static void require(Object value, String message) {
        if (value == null || (value instanceof String str && str.isBlank())) {
            throw new IllegalArgumentException(message);
        }
    }
}
