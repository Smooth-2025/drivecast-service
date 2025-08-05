package com.smooth.alert_service.support.util;

import com.smooth.alert_service.model.AlertEvent;

import java.util.Optional;
import java.util.UUID;

public class AlertIdResolver {

    public static Optional<String> resolve(AlertEvent event) {
        return switch (event.type()) {
            case "accident" -> Optional.ofNullable(event.accidentId());
            case "obstacle" -> Optional.of("obstacle-" + UUID.randomUUID().toString());
            default -> Optional.empty();
        };
    }
}
